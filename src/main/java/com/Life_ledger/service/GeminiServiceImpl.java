package com.Life_ledger.service;

import com.Life_ledger.entity.*;
import com.Life_ledger.repository.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final RestTemplate geminiRestTemplate;
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final GoalRepository goalRepository;
    private final InsightRepository insightRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    // ============================================================================
    // MAIN FUNCTION
    // ============================================================================
    @Override
    public Object analyzeUserTransactions(Long userId) {
        try {
            // 1️⃣ Load user transactions
            List<Transaction> txns = transactionRepository.findAllByUserId(userId);
            if (txns.isEmpty()) {
                return Map.of("status", "error", "message", "No transactions found");
            }

            // FLEXIBLE MAP BUILDER — corrects type mismatch
            List<Map<String, Object>> cleanTxns = new ArrayList<>();
            for (Transaction t : txns) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("date", t.getDate().toString());
                map.put("amount", t.getAmount().doubleValue());
                map.put("merchant", t.getMerchant());
                map.put("description", t.getNotes());
                cleanTxns.add(map);
            }

            // 2️⃣ Load rules
            List<Rule> rules = ruleRepository.findByUser_IdOrderByPriorityAsc(userId);
            List<Map<String, Object>> cleanRules = new ArrayList<>();
            for (Rule r : rules) {
                Map<String, Object> map = new HashMap<>();
                map.put("keyword", r.getKeyword());
                map.put("regex", r.getRegex());
                map.put("minAmount", r.getMinAmount());
                map.put("maxAmount", r.getMaxAmount());
                map.put("priority", r.getPriority());
                map.put("category", r.getCategory() != null ? r.getCategory().getName() : null);
                map.put("subCategory", r.getSubCategory() != null ? r.getSubCategory().getName() : null);
                cleanRules.add(map);
            }

            // 3️⃣ Load goals
            List<Goal> goals = goalRepository.findByUser_Id(userId);
            List<Map<String, Object>> cleanGoals = new ArrayList<>();
            for (Goal g : goals) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", g.getName());
                map.put("category", g.getCategory());
                map.put("targetAmount", g.getTargetAmount());
                map.put("currentAmount", g.getCurrentAmount());
                map.put("deadline", g.getDeadline() != null ? g.getDeadline().toString() : null);
                map.put("type", g.getType().toString());
                map.put("status", g.getStatus().toString());
                cleanGoals.add(map);
            }

            // 4️⃣ Your ORIGINAL prompt — untouched
            String prompt = buildAnalysisPrompt(cleanTxns, cleanRules, cleanGoals);

            // 5️⃣ Call Gemini API
            String jsonResponse = callGemini(prompt);
            JsonNode parsed = objectMapper.readTree(jsonResponse);

            // 6️⃣ Save summary insight only (we don’t modify categories unless you want)
            saveInsight(userId, parsed);

            return Map.of(
                    "status", "success",
                    "userId", userId,
                    "analysis", parsed);

        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    // ============================================================================
    // SAVE INSIGHT (AI Summary)
    // ============================================================================
    @Transactional
    public void saveInsight(Long userId, JsonNode result) {
        try {
            if (result.has("summary")) {
                String summary = result.get("summary").path("text").asText("");

                if (!summary.isBlank()) {
                    Insight insight = new Insight();
                    insight.setAiText(summary);
                    insight.setUser(User.builder().id(userId).build());
                    insightRepository.save(insight);
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ============================================================================
    // CALL GEMINI
    // ============================================================================
    private String callGemini(String prompt) throws Exception {

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = GEMINI_URL + "?key=" + apiKey;

        ResponseEntity<String> response = geminiRestTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                String.class);

        JsonNode root = objectMapper.readTree(response.getBody());

        String text = root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText()
                .trim();

        // strip ```json
        if (text.startsWith("```"))
            text = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);

        return text;
    }

    // ============================================================================
    // YOUR EXACT PROMPT — UNCHANGED
    // ============================================================================
    private String buildAnalysisPrompt(
            List<Map<String, Object>> txns,
            List<Map<String, Object>> rules,
            List<Map<String, Object>> goals) throws Exception {

        String txnJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(txns);
        String ruleJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rules);
        String goalJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(goals);

        // 🚨 THIS IS EXACTLY YOUR ORIGINAL PROMPT — NOT MODIFIED 🚨
        return """
                You are LifeLedger AI — a smart financial assistant.Note all the transection are in Rupeess

                Your job:
                - Categorize using rules
                - Detect recurring expenses
                - Detect anomalies
                - Evaluate goals
                - Provide nudges with tone (positive/warning/negative/neutral)
                - Output valid JSON only

                EXACT OUTPUT SHAPE:

                {
                  "categorized": [],
                  "recurring": [],
                  "anomalies": [],
                  "goalInsights": [],
                  "spendingBreakdown": {},
                  "nudges": [],
                  "summary": {
                    "text": "",
                    "tone": "",
                    "emoji": "",
                    "color": ""
                  }
                }

                TONE → COLOR → EMOJI:
                - positive → green → 😊🟢
                - warning → yellow → ⚠️
                - negative → red → 🔴
                - neutral → blue → 🔵

                USER RULES:
                """ + ruleJson + """

                USER GOALS:
                """ + goalJson + """

                TRANSACTIONS:
                """ + txnJson;
    }

    @Override
    public String testModel() {
        return "Gemini test OK";
    }

    @Override
    public String listModels() {
        return "gemini-flash-latest";
    }
}
