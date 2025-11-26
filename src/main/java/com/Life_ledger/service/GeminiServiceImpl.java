package com.Life_ledger.service;

import com.Life_ledger.entity.Transaction;
import com.Life_ledger.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final RestTemplate geminiRestTemplate;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    // Correct endpoint (v1beta supports generateContent)
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    @Override
    public String testModel() {
        try {
            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("contents", List.of(
                    Map.of("parts", List.of(Map.of("text", "Hello from LifeLedger backend! Confirm integration.")))));

            // Force JSON output
            requestBody.put("generationConfig", Map.of(
                    "responseMimeType", "application/json"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = GEMINI_URL + "?key=" + apiKey;
            ResponseEntity<String> response = geminiRestTemplate
                    .postForEntity(url, new HttpEntity<>(requestBody, headers), String.class);

            // Extract clean JSON
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        } catch (Exception e) {
            throw new RuntimeException("Gemini test failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String listModels() {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            return geminiRestTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list models: " + e.getMessage(), e);
        }
    }

    @Override
    public Object analyzeUserTransactions(Long userId) {
        try {
            List<Transaction> txns = transactionRepository.findAllByUserId(userId);

            if (txns.isEmpty()) {
                return Map.of("status", "error", "message", "No transactions found");
            }

            List<Map<String, Object>> cleanTxns = txns.stream().map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("date", t.getDate().toString());
                map.put("amount", t.getAmount());
                map.put("merchant", t.getMerchant());
                map.put("description", t.getNotes());
                return map;
            }).toList();

            // Prepare prompt
            String prompt = buildAnalysisPrompt(cleanTxns);

            // Fetch JSON from Gemini
            String json = callGemini(prompt);

            // Convert string → JSON object
            JsonNode parsed = objectMapper.readTree(json);

            return Map.of(
                    "status", "success",
                    "userId", userId,
                    "analysis", parsed);

        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private String callGemini(String prompt) throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", prompt)))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String fullUrl = GEMINI_URL + "?key=" + apiKey;
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = geminiRestTemplate.postForEntity(fullUrl, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        // (STEP 2) Remove any Markdown formatting
        text = text.trim();
        if (text.startsWith("```")) {
            text = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
        }

        return text;
    }

    private String buildAnalysisPrompt(List<Map<String, Object>> txns) throws Exception {

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(txns);

        return """
                You are a financial intelligence AI.

                Analyze the following transactions and return ONLY a JSON response (no markdown, no text):

                {
                  "categorized": [],
                  "recurring": [],
                  "anomalies": [],
                  "insights": ""
                }

                Rules:
                - No code fences
                - No explanations
                - No extra fields
                - Only valid JSON

                Transactions:
                """ + json;
    }
}
