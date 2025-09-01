package com.pm.chatbotwithai.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @NotNull
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "model_confidence")
    private Double modelConfidence;

    @Size(max = 50)
    @Column(name = "model_version")
    private String modelVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    // Constructors
    public Message() {}

    public Message(Conversation conversation, String content) {
        this.conversation = conversation;
        this.content = content;
        this.messageType = MessageType.TEXT;
        this.senderType = SenderType.USER;
    }

    public Message(Conversation conversation, String content, Long processingTimeMs,
                   Double confidence, String modelVersion) {
        this.conversation = conversation;
        this.content = content;
        this.messageType = MessageType.TEXT;
        this.senderType = SenderType.AI;
        this.processingTimeMs = processingTimeMs;
        this.modelConfidence = confidence;
        this.modelVersion = modelVersion;
    }

    // Business methods
    public boolean isFromUser() {
        return senderType == SenderType.USER;
    }

    public boolean isFromAI() {
        return senderType == SenderType.AI;
    }

    public boolean isSystemMessage() {
        return senderType == SenderType.SYSTEM;
    }

    public void updateProcessingMetrics(long processingTime, double confidence, String version) {
        this.processingTimeMs = processingTime;
        this.modelConfidence = confidence;
        this.modelVersion = version;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

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

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
