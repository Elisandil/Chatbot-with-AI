package com.pm.chatbotwithai.model.dto.response;

import com.pm.chatbotwithai.model.entity.Message;
import com.pm.chatbotwithai.model.entity.MessageType;
import com.pm.chatbotwithai.model.entity.SenderType;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponse {
    private UUID id;
    private String content;
    private MessageType messageType;
    private SenderType senderType;
    private Long processingTimeMs;
    private Double modelConfidence;
    private LocalDateTime createdAt;

    // Constructors
    public MessageResponse() {}

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.content = message.getContent();
        this.messageType = message.getMessageType();
        this.senderType = message.getSenderType();
        this.processingTimeMs = message.getProcessingTimeMs();
        this.modelConfidence = message.getModelConfidence();
        this.createdAt = message.getCreatedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public SenderType getSenderType() { return senderType; }
    public void setSenderType(SenderType senderType) { this.senderType = senderType; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Double getModelConfidence() { return modelConfidence; }
    public void setModelConfidence(Double modelConfidence) { this.modelConfidence = modelConfidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
