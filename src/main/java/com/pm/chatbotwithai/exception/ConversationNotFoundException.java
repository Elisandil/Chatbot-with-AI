package com.pm.chatbotwithai.exception;

import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends ChatbotException {

    public ConversationNotFoundException(String conversationId) {
        super("CONVERSATION_NOT_FOUND",
                "No se pudo encontrar la conversación solicitada.",
                String.format("Conversation with ID %s not found", conversationId),
                HttpStatus.NOT_FOUND);
    }
}
