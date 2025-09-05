package com.pm.chatbotwithai.service.ai;

public interface AIProvider {
    void initialize() throws Exception;
    GenerationResponse generateResponse(GenerationRequest request) throws Exception;
    void healthCheck() throws Exception;
    String getModelVersion();
    void shutdown();

    // Request/Response DTOs
    class GenerationRequest {
        private final String input;
        private final String context;
        private final int maxTokens;
        private final int timeoutSeconds;

        private GenerationRequest(Builder builder) {
            this.input = builder.input;
            this.context = builder.context;
            this.maxTokens = builder.maxTokens;
            this.timeoutSeconds = builder.timeoutSeconds;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getInput() { return input; }
        public String getContext() { return context; }
        public int getMaxTokens() { return maxTokens; }
        public int getTimeoutSeconds() { return timeoutSeconds; }

        public static class Builder {
            private String input;
            private String context;
            private int maxTokens = 512;
            private int timeoutSeconds = 30;

            public Builder input(String input) { this.input = input; return this; }
            public Builder context(String context) { this.context = context; return this; }
            public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
            public Builder timeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; return this; }
            public GenerationRequest build() { return new GenerationRequest(this); }
        }
    }

    class GenerationResponse {
        private final String response;
        private final double confidence;
        private final String modelVersion;

        public GenerationResponse(String response, double confidence, String modelVersion) {
            this.response = response;
            this.confidence = confidence;
            this.modelVersion = modelVersion;
        }

        public String getResponse() { return response; }
        public double getConfidence() { return confidence; }
        public String getModelVersion() { return modelVersion; }
    }
}

