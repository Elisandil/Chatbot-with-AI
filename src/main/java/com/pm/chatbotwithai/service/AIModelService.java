package com.pm.chatbotwithai.service;

import com.pm.chatbotwithai.exception.AIModelException;
import com.pm.chatbotwithai.exception.ModelInitializationException;
import com.pm.chatbotwithai.service.ai.AIProvider;
import com.pm.chatbotwithai.service.ai.LocalAIProvider;
import com.pm.chatbotwithai.service.ai.OpenAIProvider;
import com.pm.chatbotwithai.service.ai.MockAIProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AIModelService {
    private static final Logger logger = LoggerFactory.getLogger(AIModelService.class);

    @Value("${chatbot.ai.provider:mock}")
    private String providerType;

    @Value("${chatbot.ai.model.path:models/chatbot-model}")
    private String modelPath;

    @Value("${chatbot.ai.model.version:1.0}")
    private String modelVersion;

    @Value("${chatbot.ai.max-tokens:512}")
    private int maxTokens;

    @Value("${chatbot.ai.confidence-threshold:0.5}")
    private double confidenceThreshold;

    @Value("${chatbot.ai.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${chatbot.ai.openai.api-key:#{null}}")
    private String openAiApiKey;

    @Value("${chatbot.ai.openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    @Value("${chatbot.ai.local.endpoint:http://localhost:8080/api/generate}")
    private String localAiEndpoint;

    private AIProvider aiProvider;
    private final Executor aiExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "AI-Worker");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean modelLoaded = false;
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();
    private static final Pattern INAPPROPRIATE_CONTENT_PATTERN =
            Pattern.compile("(?i)(spam|violencia|odio|discriminaci[óo]n|hack|malware|virus)",
                    Pattern.CASE_INSENSITIVE);

    @PostConstruct
    public void initializeModel() {
        logger.info("Initializing AI provider: {} with model version: {}", providerType, modelVersion);

        try {
            aiProvider = createAIProvider();
            aiProvider.initialize();
            modelLoaded = true;
            lastHealthCheck = LocalDateTime.now();

            logger.info("AI provider initialized successfully: {}", providerType);

        } catch (Exception ex) {
            logger.error("Failed to initialize AI provider: {}", providerType, ex);
            // Fallback to mock provider
            try {
                logger.warn("Falling back to mock AI provider");
                aiProvider = new MockAIProvider(maxTokens, confidenceThreshold);
                aiProvider.initialize();
                modelLoaded = true;
                providerType = "mock";
            } catch (Exception fallbackException) {
                throw new ModelInitializationException(
                        "Failed to initialize both primary and fallback AI providers: " + ex.getMessage(),
                        fallbackException
                );
            }
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<AIResponse> generateResponseAsync(String input, String conversationContext) {

        if (!modelLoaded || aiProvider == null) {
            return CompletableFuture.failedFuture(
                    new AIModelException("AI model is not loaded or available")
            );
        }
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                logger.debug("Generating AI response for input length: {} characters", input.length());

                if (!isValidInput(input)) {
                    throw new AIModelException("Invalid input provided");
                }
                AIResponse response = generateResponse(input, conversationContext);

                logger.debug("AI response generated in {}ms with confidence: {:.2f}",
                        response.getProcessingTimeMs(), response.getConfidence());

                return response;

            } catch (AIModelException ex) {
                logger.error("AI model error during response generation", ex);
                throw new CompletionException(ex);
            } catch (Exception ex) {
                logger.error("Unexpected error during AI response generation", ex);
                throw new CompletionException(new AIModelException("Unexpected error: " + ex.getMessage(), ex));
            }
        }, aiExecutor);
    }

    public AIResponse generateResponse(String input, String conversationContext) throws AIModelException {

        if (!modelLoaded || aiProvider == null) {
            throw new AIModelException("AI provider is not available");
        }
        long startTime = System.currentTimeMillis();

        try {
            String processedInput = preprocessInput(input);
            String fullContext = buildFullContext(processedInput, conversationContext);

            AIProvider.GenerationRequest request = AIProvider.GenerationRequest.builder()
                    .input(processedInput)
                    .context(fullContext)
                    .maxTokens(maxTokens)
                    .timeoutSeconds(timeoutSeconds)
                    .build();

            AIProvider.GenerationResponse providerResponse = aiProvider.generateResponse(request);
            String finalResponse = postProcessResponse(providerResponse.getResponse());
            double confidence = calculateFinalConfidence(
                    providerResponse.getConfidence(),
                    finalResponse,
                    processedInput
            );
            long processingTime = System.currentTimeMillis() - startTime;

            if (confidence < confidenceThreshold) {
                logger.warn("Low confidence response ({:.2f}), using fallback", confidence);
                finalResponse = getFallbackResponse(processedInput);
                confidence = 1.0;
            }
            return new AIResponse(finalResponse, confidence, processingTime, getEffectiveModelVersion());

        } catch (Exception ex) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Error generating AI response after {}ms", processingTime, ex);
            throw new AIModelException("Error during response generation: " + ex.getMessage(), ex);
        }
    }

    public boolean isValidInput(String input) {

        if (input == null || input.trim().isEmpty()) {
            logger.debug("Input validation failed: null or empty");
            return false;
        }
        if (input.length() > 4000) {
            logger.warn("Input validation failed: too long ({} characters)", input.length());
            return false;
        }
        if (containsInappropriateContent(input)) {
            logger.warn("Input validation failed: inappropriate content detected");
            return false;
        }
        return true;
    }

    public ModelHealthStatus getModelHealth() {
        updateHealthStatus();
        String status = determineHealthStatus();
        boolean isHealthy = modelLoaded && aiProvider != null && "HEALTHY".equals(status);

        return new ModelHealthStatus(
                isHealthy,
                getEffectiveModelVersion(),
                status,
                System.currentTimeMillis()
        );
    }

    // Private helper methods
    private AIProvider createAIProvider() {
        return switch (providerType.toLowerCase()) {
            case "openai" -> {

                if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
                    throw new ModelInitializationException("OpenAI API key is required but not provided", null);
                }
                yield new OpenAIProvider(openAiApiKey, openAiModel);
            }
            case "local" -> new LocalAIProvider(localAiEndpoint, modelPath);
            case "mock" -> new MockAIProvider(maxTokens, confidenceThreshold);
            default -> throw new ModelInitializationException("Unknown AI provider type: " + providerType, null);
        };
    }

    private String preprocessInput(String input) {

        if (input == null) {
            return "";
        }
        String processed = input.trim()
                .replaceAll("\\s+", " ") // Normalize whitespace
                .replaceAll("[\\r\\n]+", " "); // Convert line breaks to spaces

        if (processed.length() > 4000) {
            processed = processed.substring(0, 4000) + "...";
        }
        return processed;
    }

    private String buildFullContext(String input, String conversationContext) {
        StringBuilder context = new StringBuilder();

        if (conversationContext != null && !conversationContext.trim().isEmpty()) {
            context.append("Contexto de la conversación:\n")
                    .append(conversationContext.trim())
                    .append("\n\n");
        }
        context.append("Usuario: ").append(input).append("\nAsistente:");

        return context.toString();
    }

    private String postProcessResponse(String rawResponse) {

        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return getFallbackResponse("error");
        }
        String processed = rawResponse.trim()
                .replaceAll("^(Asistente:|AI:|Bot:|Assistant:)\\s*", "")
                .replaceAll("\\s+", " ");

        if (processed.length() > maxTokens * 4) {
            int cutoff = maxTokens * 4;
            int lastSentenceEnd = processed.lastIndexOf('.', cutoff);

            if (lastSentenceEnd > cutoff - 100) {
                processed = processed.substring(0, lastSentenceEnd + 1);
            } else {
                processed = processed.substring(0, cutoff) + "...";
            }
        }
        return processed;
    }

    private double calculateFinalConfidence(double providerConfidence, String response, String input) {
        double confidence = providerConfidence;

        if (response.length() < 10) {
            confidence *= 0.7;
        }
        if (response.toLowerCase().contains("no sé") ||
                response.toLowerCase().contains("no estoy seguro")) {
            confidence *= 0.8;
        }
        if (response.matches(".*[.!?]$")) {
            confidence = Math.min(1.0, confidence + 0.1);
        }
        if (input.length() > 20 && response.length() < 20) {
            confidence *= 0.6;
        }

        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private boolean containsInappropriateContent(String input) {
        return INAPPROPRIATE_CONTENT_PATTERN.matcher(input).find();
    }

    private String getFallbackResponse(String input) {
        String[] fallbackResponses = {
                "Disculpa, no pude procesar completamente tu consulta. ¿Podrías reformularla de manera diferente?",
                "Es una pregunta interesante. ¿Podrías darme un poco más de contexto para ayudarte mejor?",
                "No estoy completamente seguro de cómo responder a eso. ¿Hay algo específico que te gustaría saber?",
                "Me gustaría ayudarte mejor. ¿Podrías ser más específico en tu pregunta?",
                "Parece que necesito más información para darte una respuesta útil. ¿Puedes ampliar tu consulta?"
        };
        int index = Math.abs(input.hashCode()) % fallbackResponses.length;
        return fallbackResponses[index];
    }

    private void updateHealthStatus() {
        LocalDateTime now = LocalDateTime.now();

        if (now.minusMinutes(5).isAfter(lastHealthCheck)) {

            try {

                if (aiProvider != null) {
                    aiProvider.healthCheck();
                }
                lastHealthCheck = now;
            } catch (Exception ex) {
                logger.warn("Health check failed for AI provider: {}", providerType, ex);
            }
        }
    }

    private String determineHealthStatus() {

        if (!modelLoaded || aiProvider == null) {
            return "UNHEALTHY";
        }

        try {
            aiProvider.healthCheck();
            return "HEALTHY";
        } catch (Exception e) {
            logger.debug("Health check failed: {}", e.getMessage());
            return "DEGRADED";
        }
    }

    private String getEffectiveModelVersion() {

        if (aiProvider != null) {
            String providerVersion = aiProvider.getModelVersion();

            if (providerVersion != null && !providerVersion.trim().isEmpty()) {
                return providerVersion;
            }
        }
        return modelVersion;
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up AI model resources");

        try {

            if (aiProvider != null) {
                aiProvider.shutdown();
            }
        } catch (Exception e) {
            logger.warn("Error during AI provider cleanup", e);
        }
        if (aiExecutor instanceof ExecutorService) {
            ((ExecutorService) aiExecutor).shutdown();

            try {

                if (!((ExecutorService) aiExecutor).awaitTermination(10,
                        TimeUnit.SECONDS)) {
                    ((ExecutorService) aiExecutor).shutdownNow();
                }
            } catch (InterruptedException ex) {
                ((ExecutorService) aiExecutor).shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        modelLoaded = false;
        logger.info("AI model resources cleaned up successfully");
    }

    // Inner classes
    public static class AIResponse {
        private final String response;
        private final double confidence;
        private final long processingTimeMs;
        private final String modelVersion;

        public AIResponse(String response, double confidence, long processingTimeMs, String modelVersion) {
            this.response = response;
            this.confidence = confidence;
            this.processingTimeMs = processingTimeMs;
            this.modelVersion = modelVersion;
        }
        // Getters and Setters
        public String getResponse() { return response; }
        public double getConfidence() { return confidence; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public String getModelVersion() { return modelVersion; }

        @Override
        public String toString() {
            return String.format("AIResponse{response='%s', confidence=%.2f, processingTime=%dms, version='%s'}",
                    response.length() > 50 ? response.substring(0, 50) + "..." : response,
                    confidence, processingTimeMs, modelVersion);
        }
    }

    public static class ModelHealthStatus {
        private final boolean loaded;
        private final String version;
        private final String status;
        private final long timestamp;

        public ModelHealthStatus(boolean loaded, String version, String status, long timestamp) {
            this.loaded = loaded;
            this.version = version;
            this.status = status;
            this.timestamp = timestamp;
        }

        // Getters and Setters
        public boolean isLoaded() { return loaded; }
        public String getVersion() { return version; }
        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("ModelHealthStatus{loaded=%b, version='%s', status='%s', timestamp=%d}",
                    loaded, version, status, timestamp);
        }
    }
}