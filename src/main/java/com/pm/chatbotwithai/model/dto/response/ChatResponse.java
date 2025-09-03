package com.pm.chatbotwithai.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatResponse {
    private UUID conversationId;
    private MessageResponse userMessage;
    private MessageResponse aiResponse;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorCode;

    // Constructors
    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatResponse(UUID conversationId, MessageResponse userMessage, MessageResponse aiResponse) {
        this();
        this.conversationId = conversationId;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.success = true;
    }

    public ChatResponse(String errorCode) {
        this();
        this.errorCode = errorCode;
        this.success = false;
    }

    // Getters and Setters
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public MessageResponse getUserMessage() { return userMessage; }
    public void setUserMessage(MessageResponse userMessage) { this.userMessage = userMessage; }

    public MessageResponse getAiResponse() { return aiResponse; }
    public void setAiResponse(MessageResponse aiResponse) { this.aiResponse = aiResponse; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
