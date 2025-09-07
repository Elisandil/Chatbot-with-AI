package com.pm.chatbotwithai.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class OpenAIProvider implements AIProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIProvider.class);

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAIProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing OpenAI Provider with model: {}", model);
        // Testing API compatibility
        healthCheck();
        logger.info("OpenAI Provider initialized successfully");
    }

    @Override
    public GenerationResponse generateResponse(GenerationRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", request.getInput())
                ),
                "max_tokens", request.getMaxTokens(),
                "temperature", 0.7
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
                    String content = (String) message.get("content");

                    return new GenerationResponse(content, 0.7, model);
                }
            }
            throw new RuntimeException("Invalid response from OpenAI API");

        } catch (Exception ex) {
            logger.error("Error calling OpenAI API", ex);
            throw new RuntimeException("OpenAI API error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void healthCheck() throws Exception {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
    }

    @Override
    public String getModelVersion() {
        return model;
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down OpenAI Provider");
    }
}
