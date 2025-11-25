package com.Life_ledger.service;

import com.Life_ledger.dto.ai.AIExtractionRequest;
import com.Life_ledger.dto.ai.AIExtractionResponse;
import com.Life_ledger.service.AIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";

    @Override
    public AIExtractionResponse extract(AIExtractionRequest request) {

        try {
            ObjectMapper mapper = new ObjectMapper();

            // ----------------------
            // 1) Prompt
            // ----------------------
            String prompt =
                    "Extract all bank transactions from this text.\n" +
                            "Return ONLY JSON in this format:\n" +
                            "{ \"transactions\": [" +
                            "{ \"date\":\"YYYY-MM-DD\", \"merchant\":\"...\", \"amount\":123.45, \"category\":\"...\", \"notes\":\"...\" }" +
                            "]}\n\n" +
                            "TEXT:\n" +
                            request.getRawText();

            // ----------------------
            // 2) Create Body
            // ----------------------
            String requestBody = """
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }]
            }
            """.formatted(prompt.replace("\"", "\\\""));

            // ----------------------
            // 3) HTTP Request
            // ----------------------
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // ----------------------
            // 4) Extract AI JSON Result
            // ----------------------
            JsonNode root = mapper.readTree(response.body());

            String aiText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            JsonNode json = mapper.readTree(aiText);

            // ----------------------
            // 5) Parse into DTO
            // ----------------------
            List<AIExtractionResponse.AITransaction> extracted = new ArrayList<>();

            for (JsonNode t : json.path("transactions")) {

                AIExtractionResponse.AITransaction tx =
                        AIExtractionResponse.AITransaction.builder()
                                .date(LocalDate.parse(t.get("date").asText()))
                                .merchant(t.get("merchant").asText())
                                .amount(new BigDecimal(t.get("amount").asText()))
                                .category(t.get("category").asText())
                                .notes(t.get("notes").asText())
                                .build();

                extracted.add(tx);
            }

            return AIExtractionResponse.builder()
                    .transactions(extracted)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("AI extraction failed: " + e.getMessage(), e);
        }
    }
}
