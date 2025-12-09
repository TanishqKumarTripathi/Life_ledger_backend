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

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final RestTemplate geminiRestTemplate;
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final GoalRepository goalRepository;
    private final InsightRepository insightRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final RecurringPatternRepository recurringPatternRepository;
    private final AnomalyRecordRepository anomalyRecordRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    // MAIN ENTRY
    @Override
    public Object analyzeUserTransactions(Long userId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            // 1) Load transactions
            List<Transaction> txns = transactionRepository.findAllByUserId(userId);
            if (txns == null || txns.isEmpty()) {
                resp.put("status", "error");
                resp.put("message", "No transactions found");
                return resp;
            }

            // Convert transactions to clean JSON-friendly maps
            List<Map<String, Object>> cleanTxns = new ArrayList<>();
            for (Transaction t : txns) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.getId());
                m.put("date", t.getDate() != null ? t.getDate().toString() : null);
                m.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
                m.put("merchant", t.getMerchant() != null ? t.getMerchant() : "");
                m.put("description", t.getNotes() != null ? t.getNotes() : "");
                cleanTxns.add(m);
            }

            // 2) Load rules (if any)
            List<Map<String, Object>> cleanRules = new ArrayList<>();
            List<Rule> rules = ruleRepository.findByUser_IdOrderByPriorityAsc(userId);
            if (rules != null) {
                for (Rule r : rules) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("keyword", r.getKeyword());
                    m.put("regex", r.getRegex());
                    m.put("minAmount", r.getMinAmount());
                    m.put("maxAmount", r.getMaxAmount());
                    m.put("priority", r.getPriority());
                    m.put("category", r.getCategory() != null ? r.getCategory().getName() : null);
                    m.put("subCategory", r.getSubCategory() != null ? r.getSubCategory().getName() : null);
                    cleanRules.add(m);
                }
            }

            // 3) Load goals (if any)
            List<Map<String, Object>> cleanGoals = new ArrayList<>();
            List<Goal> goals = goalRepository.findByUser_Id(userId);
            if (goals != null) {
                for (Goal g : goals) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", g.getName());
                    m.put("category", g.getCategory());
                    m.put("targetAmount", g.getTargetAmount());
                    m.put("currentAmount", g.getCurrentAmount());
                    m.put("deadline", g.getDeadline() != null ? g.getDeadline().toString() : null);
                    m.put("type", g.getType() != null ? g.getType().toString() : null);
                    m.put("status", g.getStatus() != null ? g.getStatus().toString() : null);
                    cleanGoals.add(m);
                }
            }

            // 4) Build prompt (unchanged)
            String prompt = buildAnalysisPrompt(cleanTxns, cleanRules, cleanGoals);

            // 5) Call Gemini
            String jsonResponse = callGemini(prompt);
            if (jsonResponse == null || jsonResponse.isBlank()) {
                resp.put("status", "error");
                resp.put("message", "Empty response from Gemini");
                return resp;
            }

            JsonNode parsed;
            try {
                parsed = objectMapper.readTree(jsonResponse);
            } catch (Exception parseEx) {
                // If model returned text that's not pure JSON, include raw string in response
                resp.put("status", "error");
                resp.put("message", "Unable to parse Gemini JSON: " + parseEx.getMessage());
                resp.put("raw", jsonResponse);
                return resp;
            }

            // 6) Persist AI results to DB (categories, subcategories, recurring, anomalies,
            // insight)
            saveAIResults(userId, parsed);

            // 7) Return final response
            resp.put("status", "success");
            resp.put("userId", userId);
            resp.put("analysis", parsed);
            return resp;
        } catch (Exception e) {
            System.err.println("❌ Gemini Analysis Failed: " + e.getMessage());
            resp.put("status", "error");
            resp.put("message", e.getMessage());
            return resp;
        }
    }

    // ========================================================================================
    // SAVE AI RESULTS (atomic-ish: transactional)
    // ========================================================================================
    @Transactional
    public void saveAIResults(Long userId, JsonNode result) {
        // Keep processing best-effort: errors on one section shouldn't abort the whole
        // method
        try {
            // -------------------------
            // 1) CATEGORIES / SUBCATEGORIES / ASSIGN TO TRANSACTION
            // -------------------------
            if (result.has("categorized") && result.get("categorized").isArray()) {
                for (JsonNode item : result.get("categorized")) {
                    try {
                        long txnId = item.path("id").asLong(0L);
                        if (txnId == 0L) {
                            System.out.println("⚠️ categorized item missing id, skipping: " + item.toString());
                            continue;
                        }

                        String categoryName = item.path("category").asText(null);
                        String subCategoryName = item.path("subCategory").asText(null);

                        Transaction txn = transactionRepository.findById(txnId).orElse(null);
                        if (txn == null) {
                            System.out
                                    .println("⚠️ transaction not found for id " + txnId + ", skipping category assign");
                            continue;
                        }

                        Category category = null;
                        if (categoryName != null && !categoryName.isBlank()) {
                            Optional<Category> catOpt = categoryRepository.findByUser_IdAndNameIgnoreCase(userId,
                                    categoryName);
                            if (catOpt.isPresent()) {
                                category = catOpt.get();
                            } else {
                                // create category
                                category = new Category();
                                category.setName(categoryName);
                                category.setUser(User.builder().id(userId).build());
                                category = categoryRepository.save(category);
                                System.out.println(
                                        "✅ Created category '" + categoryName + "' (id=" + category.getId() + ")");
                            }
                        }

                        SubCategory subCategory = null;
                        if (subCategoryName != null && !subCategoryName.isBlank() && category != null) {
                            Optional<SubCategory> scOpt = subCategoryRepository
                                    .findByCategory_IdAndNameIgnoreCase(category.getId(), subCategoryName);
                            if (scOpt.isPresent()) {
                                subCategory = scOpt.get();
                            } else {
                                subCategory = new SubCategory();
                                subCategory.setName(subCategoryName);
                                subCategory.setCategory(category);
                                subCategory = subCategoryRepository.save(subCategory);
                                System.out.println("✅ Created subcategory '" + subCategoryName + "' (id="
                                        + subCategory.getId() + ")");
                            }
                        }

                        // assign to txn (if category/subCategory exist)
                        if (category != null) {
                            txn.setCategory(category);
                        }
                        if (subCategory != null) {
                            txn.setSubCategory(subCategory);
                        }
                        if (category != null || subCategory != null) {
                            transactionRepository.save(txn);
                            System.out.println("📌 Updated transaction " + txnId + " with category/subcategory");
                        } else {
                            System.out.println(
                                    "ℹ️ No category/subcategory provided for txn " + txnId + ", skipping update");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error processing categorized item: " + e.getMessage());
                        // continue with next item
                    }
                }
            } else {
                System.out.println("ℹ️ No 'categorized' array in AI response or it's not an array");
            }

            // -------------------------
            // 2) RECURRING PATTERNS - Handled by RecurringPatternService
            // -------------------------
            System.out.println("ℹ️ Recurring patterns will be processed by RecurringPatternService");

            // -------------------------
            // 3) ANOMALIES
            // -------------------------
            if (result.has("anomalies") && result.get("anomalies").isArray()) {
                for (JsonNode item : result.get("anomalies")) {
                    try {
                        String date = item.path("date").asText();
                        double amount = item.path("amount").asDouble(0.0);
                        String merchant = item.path("merchant").asText();
                        String reason = item.path("reason").asText();
                        String category = item.path("category").asText();
                        
                        // Find matching transaction by date, amount, and merchant
                        List<Transaction> matchingTxns = transactionRepository.findByMerchantAndAmountRange(
                            userId, merchant, BigDecimal.valueOf(amount), new BigDecimal("10.00")
                        );
                        
                        if (!matchingTxns.isEmpty()) {
                            Transaction txn = matchingTxns.get(0);
                            txn.setAnomaly(true);
                            transactionRepository.save(txn);
                            
                            // Create detailed anomaly record
                            AnomalyRecord anomalyRecord = AnomalyRecord.builder()
                                .transaction(txn)
                                .reason(reason)
                                .confidence(0.85) // Default confidence
                                .anomalyType("AI_DETECTED")
                                .bankAccount(txn.getBankAccount())
                                .createdAt(java.time.LocalDateTime.now())
                                .build();
                                
                            anomalyRecordRepository.save(anomalyRecord);
                            System.out.println("⚠️ Created anomaly record for transaction: " + txn.getMerchant());
                        } else {
                            System.out.println("⚠️ No matching transaction found for anomaly: " + merchant);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error processing anomaly item: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("ℹ️ No 'anomalies' array in AI response");
            }

            // -------------------------
            // 4) INSIGHT (summary)
            // -------------------------
            if (result.has("summary")) {
                try {
                    String summaryText = result.path("summary").path("text").asText("");
                    if (summaryText != null && !summaryText.isBlank()) {
                        Insight insight = new Insight();
                        insight.setAiText(summaryText);
                        // set user reference by id only (avoid fetching)
                        insight.setUser(User.builder().id(userId).build());
                        insightRepository.save(insight);
                        System.out.println("💡 Insight saved for user " + userId);
                    } else {
                        System.out.println("ℹ️ 'summary.text' empty, nothing to save as insight");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error saving insight: " + e.getMessage());
                }
            } else {
                System.out.println("ℹ️ No 'summary' found in AI response");
            }

        } catch (Exception e) {
            // top-level catch inside transactional method
            System.err.println("❌ Failed saving AI results: " + e.getMessage());
            // don't rethrow to avoid breaking caller; you may choose to rethrow if you want
            // transaction to rollback fully
        }
    }

    // ========================================================================================
    // CALL GEMINI (safe parsing)
    // ========================================================================================
    private String callGemini(String prompt) throws Exception {
        // Build request body as plain HashMap -> RestTemplate will convert to JSON
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = GEMINI_URL + "?key=" + apiKey;

        ResponseEntity<String> response = geminiRestTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                String.class);

        if (response == null || response.getBody() == null) {
            throw new RuntimeException("Empty HTTP response from Gemini");
        }

        JsonNode root = objectMapper.readTree(response.getBody());

        // Guard: ensure candidates array exists and has element 0
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.size() == 0) {
            // try to return raw body if structure is unexpected
            throw new RuntimeException("Unexpected Gemini response shape (no candidates). Raw: " + response.getBody());
        }

        JsonNode first = candidates.get(0);
        JsonNode contentNode = first.path("content");
        JsonNode parts = contentNode.path("parts");
        if (!parts.isArray() || parts.size() == 0) {
            throw new RuntimeException("Unexpected Gemini response shape (no parts). Raw: " + response.getBody());
        }

        String text = parts.get(0).path("text").asText("").trim();
        if (text.startsWith("```")) {
            // try to extract JSON inside backticks
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}");
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1);
            } else {
                // fallback: strip the backticks
                text = text.replaceAll("^```+|```+$", "");
            }
        }
        return text;
    }

    // ========================================================================================
    // ORIGINAL PROMPT (unchanged)
    // ========================================================================================
    private String buildAnalysisPrompt(
            List<Map<String, Object>> txns,
            List<Map<String, Object>> rules,
            List<Map<String, Object>> goals) throws Exception {

        String txnJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(txns);
        String ruleJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rules);
        String goalJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(goals);

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

    @Override
    public Object analyzeAccountTransactions(Long accountId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            // Load transactions for specific bank account
            List<Transaction> txns = transactionRepository.findAllByBankAccountId(accountId);
            if (txns == null || txns.isEmpty()) {
                resp.put("status", "error");
                resp.put("message", "No transactions found for this account");
                return resp;
            }

            // Get user ID from first transaction for rules and goals
            Long userId = txns.get(0).getBankAccount().getUser().getId();

            // Convert transactions to clean JSON-friendly maps
            List<Map<String, Object>> cleanTxns = new ArrayList<>();
            for (Transaction t : txns) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.getId());
                m.put("date", t.getDate() != null ? t.getDate().toString() : null);
                m.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
                m.put("merchant", t.getMerchant() != null ? t.getMerchant() : "");
                m.put("description", t.getNotes() != null ? t.getNotes() : "");
                cleanTxns.add(m);
            }

            // Load user rules and goals (same as user analysis)
            List<Map<String, Object>> cleanRules = new ArrayList<>();
            List<Rule> rules = ruleRepository.findByUser_IdOrderByPriorityAsc(userId);
            if (rules != null) {
                for (Rule r : rules) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("keyword", r.getKeyword());
                    m.put("regex", r.getRegex());
                    m.put("minAmount", r.getMinAmount());
                    m.put("maxAmount", r.getMaxAmount());
                    m.put("priority", r.getPriority());
                    m.put("category", r.getCategory() != null ? r.getCategory().getName() : null);
                    m.put("subCategory", r.getSubCategory() != null ? r.getSubCategory().getName() : null);
                    cleanRules.add(m);
                }
            }

            List<Map<String, Object>> cleanGoals = new ArrayList<>();
            List<Goal> goals = goalRepository.findByUser_Id(userId);
            if (goals != null) {
                for (Goal g : goals) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", g.getName());
                    m.put("category", g.getCategory());
                    m.put("targetAmount", g.getTargetAmount());
                    m.put("currentAmount", g.getCurrentAmount());
                    m.put("deadline", g.getDeadline() != null ? g.getDeadline().toString() : null);
                    m.put("type", g.getType() != null ? g.getType().toString() : null);
                    m.put("status", g.getStatus() != null ? g.getStatus().toString() : null);
                    cleanGoals.add(m);
                }
            }

            // Build prompt and call Gemini
            String prompt = buildAnalysisPrompt(cleanTxns, cleanRules, cleanGoals);
            String jsonResponse = callGemini(prompt);
            
            if (jsonResponse == null || jsonResponse.isBlank()) {
                resp.put("status", "error");
                resp.put("message", "Empty response from Gemini");
                return resp;
            }

            JsonNode parsed;
            try {
                parsed = objectMapper.readTree(jsonResponse);
            } catch (Exception parseEx) {
                resp.put("status", "error");
                resp.put("message", "Unable to parse Gemini JSON: " + parseEx.getMessage());
                resp.put("raw", jsonResponse);
                return resp;
            }

            // Save AI results (same as user analysis)
            saveAIResults(userId, parsed);

            // Return response
            resp.put("status", "success");
            resp.put("accountId", accountId);
            resp.put("userId", userId);
            resp.put("analysis", parsed);
            return resp;
            
        } catch (Exception e) {
            System.err.println("❌ Account-specific Gemini Analysis Failed: " + e.getMessage());
            resp.put("status", "error");
            resp.put("message", e.getMessage());
            return resp;
        }
    }
}