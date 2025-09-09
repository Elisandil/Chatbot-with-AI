package com.pm.chatbotwithai.exception;

import org.springframework.http.HttpStatus;

public class AIModelException extends ChatbotException {

    public AIModelException(String message) {
        super("AI_MODEL_ERROR",
                "Lo siento, hubo un problema procesando tu mensaje. Por favor, inténtalo de nuevo.",
                message,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public AIModelException(String message, Throwable cause) {
        super("AI_MODEL_ERROR",
                "Lo siento, hubo un problema procesando tu mensaje. Por favor, inténtalo de nuevo.",
                message,
                HttpStatus.INTERNAL_SERVER_ERROR,
                cause);
    }
}
