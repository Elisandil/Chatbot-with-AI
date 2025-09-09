package com.pm.chatbotwithai.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends ChatbotException {

    public AuthenticationException(String message) {
        super("AUTH_ERROR",
                "No tienes permisos para realizar esta acción.",
                message,
                HttpStatus.UNAUTHORIZED);
    }
}
