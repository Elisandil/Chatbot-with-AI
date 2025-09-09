package com.pm.chatbotwithai.exception;

import org.springframework.http.HttpStatus;

public class InvalidConversationException extends ChatbotException {

    public InvalidConversationException(String message) {
        super("INVALID_CONVERSATION",
                "El formato del mensaje no es válido.",
                message,
                HttpStatus.BAD_REQUEST);
    }
}
