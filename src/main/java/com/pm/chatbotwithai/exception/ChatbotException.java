package com.pm.chatbotwithai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public abstract class ChatbotException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotException.class);

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final LocalDateTime timestamp;
    private final String userMessage;
    private final String developerMessage;

    protected ChatbotException(String errorCode, String userMessage, String developerMessage,
                               HttpStatus httpStatus, Throwable cause) {
        super(developerMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.developerMessage = developerMessage;
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();

        logException();
    }

    protected ChatbotException(String errorCode, String userMessage, String developerMessage,
                               HttpStatus httpStatus) {
        this(errorCode, userMessage, developerMessage, httpStatus, null);
    }

    private void logException() {
        logger.error("ChatBot Exception occurred - Code: {}, " +
                        "Status: {}, " +
                        "Developer Message: {}, " +
                        "User Message: {}, " +
                        "Timestamp: {}",
                errorCode, httpStatus, developerMessage, userMessage, timestamp, this);
    }

    // Getters
    public String getErrorCode() { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getUserMessage() { return userMessage; }
    public String getDeveloperMessage() { return developerMessage; }
}
