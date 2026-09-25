package com.eventsphere.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal client for the Google Gemini REST API (generateContent).
 * Returns Optional.empty() on any failure so callers can fall back gracefully - the app must never
 * break because an external AI service is slow, down, or not configured.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public GeminiClient(@Value("${app.ai.gemini.api-key:}") String apiKey,
                        @Value("${app.ai.gemini.model}") String model,
                        @Value("${app.ai.gemini.base-url}") String baseUrl,
                        @Value("${app.ai.gemini.timeout-seconds}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public String model() {
        return model;
    }

    /**
     * @param prompt   the full prompt
     * @param jsonMode when true Gemini is asked to reply with pure JSON
     */
    public Optional<String> generate(String prompt, boolean jsonMode, double temperature) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        Map<String, Object> generationConfig = jsonMode
                ? Map.of("temperature", temperature, "responseMimeType", "application/json")
                : Map.of("temperature", temperature);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", generationConfig);
        try {
            JsonNode response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = response == null ? null
                    : response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            if (text == null || text.isBlank()) {
                log.warn("Gemini returned an empty response");
                return Optional.empty();
            }
            return Optional.of(text.trim());
        } catch (Exception ex) {
            // Never log the API key; the message is enough to diagnose (401 = bad key, 429 = quota, ...)
            log.warn("Gemini call failed, using fallback: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
