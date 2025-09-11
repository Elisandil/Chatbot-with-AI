package com.pm.chatbotwithai.controller;

import com.pm.chatbotwithai.model.dto.request.MessageRequest;
import com.pm.chatbotwithai.model.dto.response.ChatResponse;
import com.pm.chatbotwithai.model.dto.response.ConversationResponse;
import com.pm.chatbotwithai.service.AIModelService;
import com.pm.chatbotwithai.service.ConversationService;
import com.pm.chatbotwithai.service.RateLimitService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = {"http://localhost:4200", "https://your-frontend-domain.com"}) // Needed for CORS testing
@Validated
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ConversationService conversationService;
    private final AIModelService aiModelService;
    private final RateLimitService rateLimitService;

    @Autowired
    public ChatController(ConversationService conversationService,
                          AIModelService aiModelService,
                          RateLimitService rateLimitService) {
        this.conversationService = conversationService;
        this.aiModelService = aiModelService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/message")
    public CompletableFuture<ResponseEntity<ChatResponse>> sendMessage(
            @Valid
            @RequestBody MessageRequest messageRequest,
            @RequestHeader(value = "X-User-ID", required = false) String userId) {

        if (messageRequest.getUserId() == null && userId != null) {
            messageRequest.setUserId(userId);
        }

        logger.info("Received message from user: {}, conversation: {}",
                messageRequest.getUserId(), messageRequest.getConversationId());

        return conversationService.processMessageAsync(messageRequest)
                .thenApply(chatResponse -> {
                    logger.info("Message processed successfully for user: {}",
                            messageRequest.getUserId());
                    return ResponseEntity.ok(chatResponse);
                })
                .exceptionally(throwable -> {
                    logger.error("Error processing message", throwable);
                    ChatResponse errorResponse = new ChatResponse("PROCESSING_ERROR");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResponse);
                });
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable String conversationId,
            @RequestHeader("X-User-ID") String userId) {

        logger.debug("Retrieving conversation {} for user {}", conversationId, userId);

        ConversationResponse conversation = conversationService.getConversation(conversationId, userId);
        return ResponseEntity.ok(conversation);
    }

    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationResponse>> getUserConversations(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        logger.debug("Retrieving conversations for user: {}, page: {}, size: {}", userId, page, size);

        Page<ConversationResponse> conversations = conversationService.getUserConversations(userId, page, size);
        return ResponseEntity.ok(conversations);
    }

    @PostMapping("/conversation")
    public ResponseEntity<ConversationResponse> createConversation(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(required = false) String title) {

        logger.info("Creating new conversation for user: {}", userId);

        ConversationResponse conversation = conversationService.createConversation(userId, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @PutMapping("/conversation/{conversationId}/archive")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable String conversationId,
            @RequestHeader("X-User-ID") String userId) {

        logger.info("Archiving conversation {} for user {}", conversationId, userId);

        conversationService.archiveConversation(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rate-limit/status")
    public ResponseEntity<RateLimitService.RateLimitStatus> getRateLimitStatus(
            @RequestHeader("X-User-ID") String userId) {

        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(userId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/stats")
    public ResponseEntity<ConversationService.ConversationStats> getConversationStats(
            @RequestHeader("X-User-ID") String userId) {

        logger.debug("Retrieving conversation statistics for user: {}", userId);

        ConversationService.ConversationStats stats = conversationService.getConversationStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        AIModelService.ModelHealthStatus modelStatus = aiModelService.getModelHealth();

        HealthResponse health = new HealthResponse(
                "UP",
                System.currentTimeMillis(),
                modelStatus.isLoaded(),
                modelStatus.getStatus()
        );

        HttpStatus status = modelStatus.isLoaded() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }

    // Inner class for health response
    public static class HealthResponse {
        private final String status;
        private final long timestamp;
        private final boolean aiModelLoaded;
        private final String aiModelStatus;

        public HealthResponse(String status, long timestamp, boolean aiModelLoaded, String aiModelStatus) {
            this.status = status;
            this.timestamp = timestamp;
            this.aiModelLoaded = aiModelLoaded;
            this.aiModelStatus = aiModelStatus;
        }

        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
        public boolean isAiModelLoaded() { return aiModelLoaded; }
        public String getAiModelStatus() { return aiModelStatus; }
    }
}
