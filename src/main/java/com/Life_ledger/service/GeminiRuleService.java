
// package com.Life_ledger.service;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.Life_ledger.dto.rules.TransactionRuleDTO;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import java.net.URI;
// import java.net.http.*;
// import java.time.Duration;
// import java.util.List;

// @Service
// public class GeminiRuleService {

// @Value("${gemini.api.key}")
// private String apiKey;

// @Value("${gemini.endpoint:https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=}")
// private String endpointPrefix;

// private final ObjectMapper mapper = new ObjectMapper();
// private final HttpClient http = HttpClient.newHttpClient();

// private JsonNode callGemini(String prompt) throws Exception {

// String payload = mapper.writeValueAsString(
// java.util.Map.of("contents",
// List.of(java.util.Map.of("parts", List.of(java.util.Map.of("text",
// prompt)))))
// );

// HttpRequest req = HttpRequest.newBuilder()
// .uri(URI.create(endpointPrefix + apiKey))
// .header("Content-Type", "application/json")
// .timeout(Duration.ofSeconds(120))
// .POST(HttpRequest.BodyPublishers.ofString(payload))
// .build();

// HttpResponse<String> resp = null;
// int maxRetries = 3;
// int retryDelay = 2000;

// for (int attempt = 1; attempt <= maxRetries; attempt++) {
// resp = http.send(req, HttpResponse.BodyHandlers.ofString());

// if (resp.statusCode() / 100 == 2) {
// break;
// }

// if (resp.statusCode() == 503 && attempt < maxRetries) {
// System.out.println("Gemini API overloaded, retrying in " + retryDelay + "ms
// (attempt " + attempt + "/" + maxRetries + ")");
// Thread.sleep(retryDelay);
// retryDelay *= 2;
// continue;
// }

// throw new RuntimeException("Gemini non-2xx: " + resp.statusCode() + " body:"
// + resp.body());
// }

// return mapper.readTree(resp.body());
// }

// private String buildInputJson(List<TransactionRuleDTO> list) throws Exception
// {
// return mapper.writeValueAsString(list);
// }

// public JsonNode merchantRule(List<TransactionRuleDTO> list) throws Exception
// {
// String input = buildInputJson(list);
// String prompt = """
// RULE: Merchant categorization
// Map merchants to categories:
// - merchant contains "Starbucks" -> "Coffee"
// - merchant contains "Amazon" -> "Shopping"
// - merchant contains "Netflix" -> "Entertainment"

// Input (JSON array of objects): %s

// Return ONLY JSON:
// { "merchantRule": [ { "index": <int>, "category": <string|null> } ] }
// """.formatted(input);
// return callGemini(prompt);
// }

// public JsonNode amountRule(List<TransactionRuleDTO> list) throws Exception {
// String input = buildInputJson(list);
// String prompt = """
// RULE: Amount categorization
// - If amount < 100 -> category = "Small Expense"
// - Otherwise -> category = null

// Input: %s

// Return ONLY JSON:
// { "amountRule": [ { "index": <int>, "category": <string|null> } ] }
// """.formatted(input);
// return callGemini(prompt);
// }

// public JsonNode recurringRule(List<TransactionRuleDTO> list) throws Exception
// {
// String input = buildInputJson(list);
// String prompt = """
// RULE: Recurring detection
// - If same (merchant + amount) appears 2 or more times -> recurring = true for
// those txns
// - Otherwise recurring = false

// Input: %s

// Return ONLY JSON:
// { "recurringRule": [ { "index": <int>, "recurring": true|false } ] }
// """.formatted(input);
// return callGemini(prompt);
// }

// public JsonNode anomalyRule(List<TransactionRuleDTO> list) throws Exception {
// String input = buildInputJson(list);
// String prompt = """
// RULE: Anomaly detection
// - Compute mean(amount) across transactions
// - If txn.amount > mean * 3 -> anomaly = true, else false

// Input: %s

// Return ONLY JSON:
// { "anomalyRule": [ { "index": <int>, "anomaly": true|false } ] }
// """.formatted(input);

// return callGemini(prompt);
// }
// }
