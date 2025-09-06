package com.pm.chatbotwithai.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MockAIProvider implements AIProvider {
    private static final Logger logger = LoggerFactory.getLogger(MockAIProvider.class);

    private final int maxTokens;
    private final double baseConfidence;
    private final Random random = ThreadLocalRandom.current();
    private volatile boolean initialized = false;

    public MockAIProvider(int maxTokens, double baseConfidence) {
        this.maxTokens = maxTokens;
        this.baseConfidence = baseConfidence;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing Mock AI Provider");
        Thread.sleep(100);
        initialized = true;
        logger.info("Mock AI Provider initialized successfully");
    }

    @Override
    public GenerationResponse generateResponse(GenerationRequest request) throws Exception {

        if (!initialized) {
            throw new IllegalStateException("Mock AI Provider not initialized");
        }
        Thread.sleep(50 + random.nextInt(200));

        String input = request.getInput().toLowerCase();
        String response = generateMockResponse(input);
        double confidence = calculateMockConfidence(input, response);

        logger.debug("Mock AI generated response: {} chars, confidence: {:.2f}",
                response.length(), confidence);

        return new GenerationResponse(response, confidence, "mock-1.0");
    }

    @Override
    public void healthCheck() throws Exception {

        if (!initialized) {
            throw new IllegalStateException("Mock AI Provider not initialized");
        }
    }

    @Override
    public String getModelVersion() {
        return "mock-1.0";
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down Mock AI Provider");
        initialized = false;
    }

    private String generateMockResponse(String input) {

        if (input.contains("hola") || input.contains("hello")) {
            return "¡Hola! Es un placer saludarte. ¿En qué puedo ayudarte hoy?";
        }
        if (input.contains("nombre") || input.contains("name")) {
            return "Soy un asistente de inteligencia artificial creado para ayudarte con tus consultas." +
                    " ¿Qué te gustaría saber?";
        }
        if (input.contains("tiempo") || input.contains("weather")) {
            return "No tengo acceso a información meteorológica en tiempo real, pero puedo ayudarte con muchas otras " +
                    "cosas. ¿Hay algo más en lo que pueda asistirte?";
        }
        if (input.contains("ayuda") || input.contains("help")) {
            return "Por supuesto, estoy aquí para ayudarte. Puedo responder preguntas, explicar conceptos, ayudar con " +
                    "cálculos básicos y mucho más. ¿Qué necesitas específicamente?";
        }
        if (input.contains("adiós") || input.contains("bye")) {
            return "¡Hasta luego! Ha sido un placer ayudarte. No dudes en volver si tienes más preguntas.";
        }

        if (input.length() > 100) {
            return "Veo que tienes una consulta detallada. Aunque soy un modelo de demostración, puedo decirte que " +
                    "entiendo la importancia de tu pregunta. En un sistema real, esto se procesaría con mayor " +
                    "eficiencia.";
        }
        if (input.contains("?")) {
            return "Esa es una buena pregunta. Como soy un modelo de demostración, te puedo decir que en un sistema " +
                    "real se analizaría tu consulta con mayor detalle para darte una respuesta más precisa.";
        }
        // Respuestas genericas
        String[] genericResponses = {
                "Entiendo tu consulta. Como asistente de demostración, puedo ayudarte de manera básica con este " +
                        "tipo de preguntas.",
                "Es interesante lo que mencionas. Aunque soy un modelo simplificado, puedo ofrecerte algunas " +
                        "reflexiones sobre el tema.",
                "Tu consulta es válida. En un sistema completo, esto se analizaría con algoritmos más avanzados, " +
                        "pero puedo darte una respuesta general.",
                "Gracias por tu pregunta. Como sistema de demostración, puedo proporcionarte una respuesta básica " +
                        "sobre este tema."
        };
        return genericResponses[Math.abs(input.hashCode()) % genericResponses.length];
    }

    private double calculateMockConfidence(String input, String response) {
        double confidence = baseConfidence;

        if (input.length() < 5) {
            confidence *= 0.7;
        }
        if (response.length() > 50) {
            confidence += 0.1;
        }
        confidence += (random.nextDouble() - 0.5) * 0.2;

        return Math.max(0.1, Math.min(1.0, confidence));
    }
}
