package com.projectsync.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiApiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestClient restClient = RestClient.builder().build();

    /** Sends the prompt and returns the model's text. Asks Gemini for JSON output. */
    @SuppressWarnings("unchecked")
    public String generateJson(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json"));

        Map<String, Object> response = restClient.post()
                .uri(apiUrl)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        try {
            var candidate = ((List<Map<String, Object>>) response.get("candidates")).get(0);
            var content = (Map<String, Object>) candidate.get("content");
            var part = ((List<Map<String, Object>>) content.get("parts")).get(0);
            return (String) part.get("text");
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unexpected Gemini response.", e);
        }
    }
}
