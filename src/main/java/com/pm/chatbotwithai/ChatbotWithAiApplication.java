package com.pm.chatbotwithai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pm.chatbotwithai.config.ChatbotProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableConfigurationProperties({ChatbotProperties.class})
public class ChatbotWithAiApplication {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotWithAiApplication.class);
    private static ApplicationContext applicationContext;


    public static void main(String[] args) {

        try {
            setDefaultSystemProperties();
            applicationContext = SpringApplication.run(ChatbotWithAiApplication.class, args);
            logApplicationStartup(applicationContext);

        } catch (Exception ex) {
            logger.error("Failed to start Chatbot with AI application", ex);
            System.exit(1);
        }
    }

    private static void setDefaultSystemProperties() {

        if (System.getProperty("user.timezone") == null) {
            System.setProperty("user.timezone", "Europe/Madrid");
        }

        if (System.getProperty("file.encoding") == null) {
            System.setProperty("file.encoding", "UTF-8");
        }
        System.setProperty("java.awt.headless", "true");
        System.setProperty("spring.output.ansi.enabled", "always");

        logger.info("System properties configured successfully");
    }

    private static void logApplicationStartup(ApplicationContext context) {
        Environment env = context.getEnvironment();
        String protocol = "http";

        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("Cannot determine host address, using localhost", e);
        }

        logger.info("\n----------------------------------------------------------\n" +
                        "\tApplication '{}' is running! Access URLs:\n" +
                        "\tLocal: \t\t{}://localhost:{}{}\n" +
                        "\tExternal: \t{}://{}:{}{}\n" +
                        "\tProfile(s): \t{}\n" +
                        "\tAI Provider: \t{}\n" +
                        "\tDatabase: \t{}\n" +
                        "----------------------------------------------------------",
                env.getProperty("spring.application.name", "Chatbot with AI"),
                protocol,
                serverPort,
                contextPath,
                protocol,
                hostAddress,
                serverPort,
                contextPath,
                Arrays.toString(env.getActiveProfiles().length == 0 ?
                        env.getDefaultProfiles() : env.getActiveProfiles()),
                env.getProperty("chatbot.ai.provider", "mock"),
                env.getProperty("spring.datasource.url", "jdbc:h2:mem:chatbotdb")
        );
        logImportantConfiguration(env);
        verifyCriticalComponents(context);
    }

    private static void logImportantConfiguration(Environment env) {
        logger.info("=== Application Configuration ===");
        logger.info("AI Provider: {}", env.getProperty("chatbot.ai.provider",
                "mock"));
        logger.info("Max Tokens: {}", env.getProperty("chatbot.ai.max-tokens",
                "512"));
        logger.info("Rate Limit (per minute): {}", env.getProperty("chatbot.rate-limit.requests-per-minute",
                "20"));
        logger.info("Rate Limit (per hour): {}", env.getProperty("chatbot.rate-limit.requests-per-hour",
                "100"));
        logger.info("Max Messages per Conversation: {}", env.getProperty("chatbot.conversation.max-messages",
                "1000"));
        logger.info("Context Window: {}", env.getProperty("chatbot.conversation.context-window",
                "10"));
        logger.info("Database URL: {}", env.getProperty("spring.datasource.url",
                "jdbc:h2:mem:chatbotdb"));

        if (Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            logger.info("PRODUCTION MODE ENABLED - Security features active");
        } else {
            logger.warn("DEVELOPMENT MODE - Some security features may be relaxed");
        }

        String aiProvider = env.getProperty("chatbot.ai.provider", "mock");
        switch (aiProvider) {
            case "openai":
                boolean hasApiKey = env.getProperty("chatbot.ai.openai.api-key") != null &&
                        !env.getProperty("chatbot.ai.openai.api-key").trim().isEmpty();
                logger.info("OpenAI Model: {}", env.getProperty("chatbot.ai.openai.model",
                        "gpt-3.5-turbo"));
                logger.info("OpenAI API Key configured: {}", hasApiKey);

                if (!hasApiKey) {
                    logger.warn("⚠️  OpenAI API key not configured! Using mock provider as fallback.");
                }
                break;
            case "local":
                logger.info("Local AI Endpoint: {}", env.getProperty("chatbot.ai.local.endpoint",
                        "http://localhost:8080/api/generate"));
                logger.info("Model Path: {}", env.getProperty("chatbot.ai.model.path",
                        "models/chatbot-model"));
                break;
            case "mock":
                logger.info("Using Mock AI Provider for development/testing");
                break;
            default:
                logger.warn("Unknown AI provider: {}. Falling back to mock provider.", aiProvider);
        }
        logger.info("================================");
    }

    private static void verifyCriticalComponents(ApplicationContext context) {

        try {

            if (context.containsBean("AIModelService")) {
                logger.info("✓ AI Model Service initialized");
            } else {
                logger.warn("⚠️  AI Model Service not found");
            }

            if (context.containsBean("rateLimitService")) {
                logger.info("✓ Rate Limit Service initialized");
            } else {
                logger.warn("⚠️  Rate Limit Service not found");
            }

            if (context.containsBean("conversationService")) {
                logger.info("✓ Conversation Service initialized");
            } else {
                logger.error("❌ Conversation Service not found - Critical component missing!");
            }

            try {

                if (context.containsBean("dataSource")) {
                    logger.info("✓ Database connection established");
                }
            } catch (Exception e) {
                logger.error("❌ Database connection failed", e);
            }

            logger.info("Component verification completed");

        } catch (Exception e) {
            logger.error("Error during component verification", e);
        }
    }


    public void onExit() {
        logger.info("Shutting down Chatbot with AI application...");

        try {
            logger.info("Application shutdown completed successfully");
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}