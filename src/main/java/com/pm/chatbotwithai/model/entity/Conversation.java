package com.pm.chatbotwithai.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversation")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Size(max = 100)
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Size(max = 200)
    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    // Constructor
    public Conversation() {}

    public Conversation(String userId) {
        this.userId = userId;
        this.lastActivity = LocalDateTime.now();
    }

    public Conversation(String userId, String title) {
        this.userId = userId;
        this.title = title;
        this.lastActivity = LocalDateTime.now();
    }

    // Business methods
    public void addMessage(Message message) {
        messages.add(message);
        message.setConversation(this);
        updateLastActivity();
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == ConversationStatus.ACTIVE;
    }

    public void archive() {
        this.status = ConversationStatus.ARCHIVED;
        updateLastActivity();
    }

    public void activate() {
        this.status = ConversationStatus.ACTIVE;
        updateLastActivity();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
}
