package com.pm.chatbotwithai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "chatbot")
public class ChatbotProperties {
    private final Ai ai = new Ai();
    private final RateLimit rateLimit = new RateLimit();
    private final Conversation conversation = new Conversation();
    public Ai getAi() { return ai; }
    public RateLimit getRateLimit() { return rateLimit; }
    public Conversation getConversation() { return conversation; }

    public static class Ai {
        private final Model model = new Model();
        private int maxTokens = 512;
        private double confidenceThreshold = 0.5;

        public Model getModel() { return model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }

        public static class Model {
            private String path = "models/chatbot-model";
            private String version = "1.0";

            public String getPath() { return path; }
            public void setPath(String path) { this.path = path; }
            public String getVersion() { return version; }
            public void setVersion(String version) { this.version = version; }
        }
    }

    public static class RateLimit {
        private int requestsPerMinute = 20;
        private int requestsPerHour = 100;
        private int burstLimit = 5;

        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
        public int getRequestsPerHour() { return requestsPerHour; }
        public void setRequestsPerHour(int requestsPerHour) { this.requestsPerHour = requestsPerHour; }
        public int getBurstLimit() { return burstLimit; }
        public void setBurstLimit(int burstLimit) { this.burstLimit = burstLimit; }
    }

    public static class Conversation {
        private int maxMessages = 1000;
        private int contextWindow = 10;

        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
        public int getContextWindow() { return contextWindow; }
        public void setContextWindow(int contextWindow) { this.contextWindow = contextWindow; }
    }
}
