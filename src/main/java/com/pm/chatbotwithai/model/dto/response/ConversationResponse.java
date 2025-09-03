package com.pm.chatbotwithai.model.dto.response;

import com.pm.chatbotwithai.model.entity.Conversation;
import com.pm.chatbotwithai.model.entity.ConversationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConversationResponse {
    private UUID id;
    private String title;
    private ConversationStatus status;
    private List<MessageResponse> messages = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private int messageCount;

    // Constructors
    public ConversationResponse() {}

    public ConversationResponse(Conversation conversation) {
        this.id = conversation.getId();
        this.title = conversation.getTitle();
        this.status = conversation.getStatus();
        this.createdAt = conversation.getCreatedAt();
        this.lastActivity = conversation.getLastActivity();
        this.messageCount = conversation.getMessages().size();
        this.messages = conversation.getMessages().stream()
                .map(MessageResponse::new)
                .collect(Collectors.toList());
    }

    public ConversationResponse(Conversation conversation, boolean includeMessages) {
        this.id = conversation.getId();
        this.title = conversation.getTitle();
        this.status = conversation.getStatus();
        this.createdAt = conversation.getCreatedAt();
        this.lastActivity = conversation.getLastActivity();
        this.messageCount = conversation.getMessages().size();

        if (includeMessages) {
            this.messages = conversation.getMessages().stream()
                    .map(MessageResponse::new)
                    .collect(Collectors.toList());
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }

    public List<MessageResponse> getMessages() { return messages; }
    public void setMessages(List<MessageResponse> messages) { this.messages = messages; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
}
