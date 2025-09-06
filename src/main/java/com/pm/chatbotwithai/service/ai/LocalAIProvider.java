package com.pm.chatbotwithai.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class LocalAIProvider implements AIProvider {
    private static final Logger logger = LoggerFactory.getLogger(LocalAIProvider.class);

    private final String endpoint;
    private final String modelPath;
    private final RestTemplate restTemplate;

    public LocalAIProvider(String endpoint, String modelPath) {
        this.endpoint = endpoint;
        this.modelPath = modelPath;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing Local AI Provider with endpoint: {}", endpoint);
        healthCheck();
        logger.info("Local AI Provider initialized successfully");
    }

    @Override
    public GenerationResponse generateResponse(GenerationRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "input", request.getInput(),
                "context", request.getContext() != null ? request.getContext() : "",
                "max_tokens", request.getMaxTokens()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String generatedText = (String) responseBody.get("response");
                Double confidence = (Double) responseBody.getOrDefault("confidence", 0.8);

                return new GenerationResponse(generatedText, confidence, "local-1.0");
            }
            throw new RuntimeException("Invalid response from local AI endpoint");

        } catch (Exception ex) {
            logger.error("Error calling local AI endpoint", ex);
            throw new RuntimeException("Local AI error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void healthCheck() throws Exception {

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    endpoint.replace("/generate", "/health"),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Local AI service is not healthy");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Cannot connect to local AI service: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getModelVersion() {
        return "local-1.0";
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down Local AI Provider");
    }
}
