package com.pm.chatbotwithai.service;


import com.pm.chatbotwithai.exception.ConversationNotFoundException;
import com.pm.chatbotwithai.exception.InvalidConversationException;
import com.pm.chatbotwithai.exception.RateLimitExceededException;
import com.pm.chatbotwithai.model.dto.request.MessageRequest;
import com.pm.chatbotwithai.model.dto.response.ChatResponse;
import com.pm.chatbotwithai.model.dto.response.ConversationResponse;
import com.pm.chatbotwithai.model.dto.response.MessageResponse;
import com.pm.chatbotwithai.model.entity.Conversation;
import com.pm.chatbotwithai.model.entity.ConversationStatus;
import com.pm.chatbotwithai.model.entity.Message;
import com.pm.chatbotwithai.repository.ConversationRepository;
import com.pm.chatbotwithai.repository.MessageRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Service
public class ConversationService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AIModelService aiModelService;
    private final RateLimitService rateLimitService;

    @Value("${chatbot.conversation.max-messages:1000}")
    private int maxMessagesPerConversation;

    @Value("${chatbot.conversation.context-window:10}")
    private int contextWindow;

    @Autowired
    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               AIModelService aiModelService,
                               RateLimitService rateLimitService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiModelService = aiModelService;
        this.rateLimitService = rateLimitService;
    }

    @Async
    @Transactional
    public CompletableFuture<ChatResponse> processMessageAsync(MessageRequest messageRequest) {
        logger.debug("Processing message async for user: {}", messageRequest.getUserId());

        return CompletableFuture.supplyAsync(() -> {

            try {

                if (!rateLimitService.isAllowed(messageRequest.getUserId())) {
                    throw new RateLimitExceededException(messageRequest.getUserId());
                }
                if (!aiModelService.isValidInput(messageRequest.getContent())) {
                    throw new InvalidConversationException("Invalid message content");
                }
                Conversation conversation = getOrCreateConversation(
                        messageRequest.getConversationId(),
                        messageRequest.getUserId()
                );
                Message userMessage = saveUserMessage(conversation, messageRequest.getContent());
                String conversationContext = buildConversationContext(conversation);

                return aiModelService.generateResponseAsync(messageRequest.getContent(), conversationContext)
                        .thenApply(aiResponse -> {

                            try {
                                Message aiMessage = saveAIMessage(
                                        conversation,
                                        aiResponse.getResponse(),
                                        aiResponse.getProcessingTimeMs(),
                                        aiResponse.getConfidence(),
                                        aiResponse.getModelVersion()
                                );
                                logger.info("Message processed successfully for conversation: {}",
                                        conversation.getId());

                                return new ChatResponse(
                                        conversation.getId(),
                                        new MessageResponse(userMessage),
                                        new MessageResponse(aiMessage)
                                );

                            } catch (Exception e) {
                                logger.error("Error saving AI response", e);
                                throw new RuntimeException("Error saving AI response", e);
                            }
                        })
                        .join();

            } catch (Exception e) {
                logger.error("Error processing message", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(String conversationId, String userId) {
        UUID uuid = parseConversationId(conversationId);

        Conversation conversation = conversationRepository
                .findByIdAndUserId(uuid, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        logger.debug("Retrieved conversation {} for user {}", conversationId, userId);
        return new ConversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> getUserConversations(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Conversation> conversations = conversationRepository
                .findByUserIdAndStatusOrderByLastActivityDesc(
                        userId,
                        ConversationStatus.ACTIVE,
                        pageable
                );

        logger.debug("Retrieved {} conversations for user {}", conversations.getTotalElements(), userId);

        return conversations.map(conv -> new ConversationResponse(conv, false));
    }

    @Transactional
    public ConversationResponse createConversation(String userId, String title) {
        Conversation conversation = new Conversation(userId, title);
        conversation = conversationRepository.save(conversation);

        logger.info("Created new conversation {} for user {}", conversation.getId(), userId);
        return new ConversationResponse(conversation, false);
    }

    @Transactional
    public void archiveConversation(String conversationId, String userId) {
        UUID uuid = parseConversationId(conversationId);

        Conversation conversation = conversationRepository
                .findByIdAndUserId(uuid, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        conversation.archive();
        conversationRepository.save(conversation);

        logger.info("Archived conversation {} for user {}", conversationId, userId);
    }

    @Transactional
    public void deleteOldConversations(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);

        List<Conversation> oldConversations = conversationRepository
                .findInactiveConversations(cutoffDate, ConversationStatus.ARCHIVED);

        for (Conversation conv : oldConversations) {
            conv.setStatus(ConversationStatus.DELETED);
        }
        conversationRepository.saveAll(oldConversations);
        logger.info("Marked {} old conversations for deletion", oldConversations.size());
    }

    // Private helper methods
    private Conversation getOrCreateConversation(String conversationId, String userId) {

        if (conversationId != null && !conversationId.trim().isEmpty()) {
            UUID uuid = parseConversationId(conversationId);
            return conversationRepository
                    .findByIdAndUserId(uuid, userId)
                    .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        } else {
            String autoTitle = generateConversationTitle();
            Conversation newConversation = new Conversation(userId, autoTitle);
            return conversationRepository.save(newConversation);
        }
    }

    private Message saveUserMessage(Conversation conversation, String content) {

        if (conversation.getMessages().size() >= maxMessagesPerConversation) {
            throw new InvalidConversationException(
                    "Conversation has reached maximum message limit"
            );
        }
        Message userMessage = new Message(conversation, content);
        conversation.addMessage(userMessage);
        Message savedMessage = messageRepository.save(userMessage);
        conversationRepository.save(conversation);

        return savedMessage;
    }

    private Message saveAIMessage(Conversation conversation, String content,
                                  long processingTime, double confidence, String modelVersion) {
        Message aiMessage = new Message(conversation, content, processingTime, confidence, modelVersion);
        conversation.addMessage(aiMessage);

        Message savedMessage = messageRepository.save(aiMessage);
        conversationRepository.save(conversation);

        return savedMessage;
    }

    private String buildConversationContext(Conversation conversation) {
        List<Message> recentMessages = conversation.getMessages()
                .stream()
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                .limit(contextWindow)
                .toList();

        StringBuilder context = new StringBuilder();

        for (Message message : recentMessages) {
            String sender = message.isFromUser() ? "Usuario" : "Asistente";
            context.append(sender).append(": ").append(message.getContent()).append("\n");
        }
        return context.toString();
    }

    private UUID parseConversationId(String conversationId) {
        try {
            return UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            throw new InvalidConversationException("Invalid conversation ID format: " + conversationId);
        }
    }

    private String generateConversationTitle() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("Conversación del %02d/%02d/%d",
                now.getDayOfMonth(),
                now.getMonthValue(),
                now.getYear());
    }

    @Transactional(readOnly = true)
    public ConversationStats getConversationStats(String userId) {
        long totalConversations = conversationRepository
                .countByUserIdAndStatus(userId, ConversationStatus.ACTIVE);

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        long recentMessages = messageRepository
                .countUserMessagesAfter(userId, last24Hours);

        Double avgProcessingTime = messageRepository.getAverageProcessingTime(last24Hours);

        return new ConversationStats(totalConversations, recentMessages,
                avgProcessingTime != null ? avgProcessingTime : 0.0);
    }

    // Inner class for statistics
    public static class ConversationStats {
        private final long totalConversations;
        private final long recentMessages;
        private final double averageProcessingTime;

        public ConversationStats(long totalConversations, long recentMessages, double averageProcessingTime) {
            this.totalConversations = totalConversations;
            this.recentMessages = recentMessages;
            this.averageProcessingTime = averageProcessingTime;
        }

        public long getTotalConversations() { return totalConversations; }
        public long getRecentMessages() { return recentMessages; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
    }
}
