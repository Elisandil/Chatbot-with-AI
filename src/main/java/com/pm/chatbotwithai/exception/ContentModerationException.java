package com.pm.chatbotwithai.exception;

import org.springframework.http.HttpStatus;

public class ContentModerationException extends ChatbotException {

    public ContentModerationException(String reason) {
        super("CONTENT_MODERATION",
                "Tu mensaje contiene contenido que no puede ser procesado. Por favor, reformula tu consulta.",
                String.format("Content moderation triggered: %s", reason),
                HttpStatus.BAD_REQUEST);
    }
}
