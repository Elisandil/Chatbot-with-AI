package com.pm.chatbotwithai.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ChatbotException {

    public RateLimitExceededException(String userId) {
        super("RATE_LIMIT_EXCEEDED",
                "Has excedido el límite de mensajes por minuto. Por favor, espera un momento.",
                String.format("Rate limit exceeded for user: %s", userId),
                HttpStatus.TOO_MANY_REQUESTS);
    }
}
