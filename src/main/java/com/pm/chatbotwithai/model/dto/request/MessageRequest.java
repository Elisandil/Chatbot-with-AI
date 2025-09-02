package com.pm.chatbotwithai.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MessageRequest {
    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 4000, message = "El mensaje no puede exceder los 4000 caracteres")
    private String content;

    @Size(max = 100, message = "El ID de usuario no puede exceder los 100 caracteres")
    private String userId;

    private String conversationId;

    // Constructors
    public MessageRequest() {}

    public MessageRequest(String content, String userId) {
        this.content = content;
        this.userId = userId;
    }

    // Getters and Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}
