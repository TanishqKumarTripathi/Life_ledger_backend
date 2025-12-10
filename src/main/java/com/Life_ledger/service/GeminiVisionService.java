package com.Life_ledger.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiVisionService {

        @Value("${gemini.api.key}")
        private String apiKey;

        private final WebClient.Builder webClientBuilder;

        public String analyzeImage(String prompt, String base64Image) {

                Map<String, Object> payload = Map.of(
                                "contents", List.of(
                                                Map.of(
                                                                "role", "user",
                                                                "parts", List.of(
                                                                                Map.of("text", prompt),
                                                                                Map.of("inline_data", Map.of(
                                                                                                "mime_type",
                                                                                                "image/jpeg",
                                                                                                "data",
                                                                                                base64Image))))));

                WebClient client = webClientBuilder
                                .baseUrl("https://generativelanguage.googleapis.com")
                                .build();

                return client.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/v1beta/models/gemini-flash-latest:generateContent")
                                                .queryParam("key", apiKey)
                                                .build())
                                .bodyValue(payload)
                                .retrieve()
                                .bodyToMono(JsonNode.class)
                                // IMPORTANT: prompt Gemini to return RAW JSON and then
                                // this should be exactly that JSON string
                                .map(json -> json.at("/candidates/0/content/parts/0/text").asText())
                                .block();
        }
}
