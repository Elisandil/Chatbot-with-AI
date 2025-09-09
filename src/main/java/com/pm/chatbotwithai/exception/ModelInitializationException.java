package com.pm.chatbotwithai.exception;

import org.springframework.http.HttpStatus;

public class ModelInitializationException extends ChatbotException {

    public ModelInitializationException(String message, Throwable cause) {
        super("MODEL_INIT_ERROR",
                "El servicio no está disponible temporalmente. Por favor, inténtalo más tarde.",
                message,
                HttpStatus.SERVICE_UNAVAILABLE,
                cause);
    }
}
