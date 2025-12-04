package com.Life_ledger.service;

import com.Life_ledger.entity.*;
import com.Life_ledger.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Final GeminiServiceImpl — multi-step, account-aware, S2 analytics split
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final GoalRepository goalRepository;
    private final InsightRepository insightRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final RecurringPatternRepository recurringPatternRepository;
    private final AnomalyRecordRepository anomalyRecordRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.connectTimeout:50000}")
    private int connectTimeoutMs;

    @Value("${gemini.api.readTimeout:120000}")
    private int readTimeoutMs;

    // status store keyed by "userId:accountId|all"
    private final Map<String, EnumMap<Step, TaskStatus>> statusStore = new ConcurrentHashMap<>();

    public enum Step {
        CATEGORIZE, RECURRING, ANOMALIES, SUMMARY
    }

    public static class TaskStatus {
        public enum State {
            PENDING, RUNNING, DONE, ERROR
        }

        public volatile State state = State.PENDING;
        public volatile String message = null;
        public volatile JsonNode result = null;
        public volatile LocalDateTime updatedAt = LocalDateTime.now();
    }

    // --------------------
    // Interface implementations
    // --------------------
    @Override
    public String testModel() {
        return "gemini-pro (v1beta) test OK";
    }

    @Override
    public String listModels() {
        return "v1beta gemini-pro";
    }

    @Override
    public void analyzeUserTransactionsAsync(Long userId) {
        analyzeUserTransactionsAsync(userId, null);
    }

    @Override
    @Async
    public void analyzeUserTransactionsAsync(Long userId, Long accountId) {
        String key = statusKey(userId, accountId);
        log.info("Start multi-step analysis for {}", key);
        ensureStatusEntryForKey(key);
        for (Step s : Step.values())
            setStatusForKey(key, s, TaskStatus.State.PENDING, null, null);

        // Run categorize first (so categories can be saved early)
        try {
            processCategorization(userId, accountId);
        } catch (Exception e) {
            log.error("Categorize failed for {}: {}", key, e.getMessage(), e);
        }

        // run others in parallel
        processRecurringAsync(userId, accountId);
        processAnomaliesAsync(userId, accountId);
        processSummaryAsync(userId, accountId);
    }

    @Override
    public void processCategorization(Long userId) {
        processCategorization(userId, null);
    }

    public void processCategorization(Long userId, Long accountId) {
        String key = statusKey(userId, accountId);
        setStatusForKey(key, Step.CATEGORIZE, TaskStatus.State.RUNNING, null, null);
        try {
            List<Transaction> txns = loadTransactions(userId, accountId);
            List<Rule> rules = ruleRepository.findByUser_IdOrderByPriorityAsc(userId);

            String prompt = buildCategorizationPrompt(txns, rules);
            JsonNode res = callGeminiWithRetry(prompt);

            if (res != null) {
                // Save categories (and category totals if provided)
                saveCategorizedResults(userId, res);
            }

            setStatusForKey(key, Step.CATEGORIZE, TaskStatus.State.DONE, null, res);
            log.info("Categorization DONE for {}", key);
        } catch (Exception ex) {
            setStatusForKey(key, Step.CATEGORIZE, TaskStatus.State.ERROR, ex.getMessage(), null);
            log.error("Categorization ERROR for {}: {}", key, ex.getMessage(), ex);
        }
    }

    @Override
    public void processRecurringAsync(Long userId) {
        processRecurringAsync(userId, null);
    }

    @Async
    public void processRecurringAsync(Long userId, Long accountId) {
        String key = statusKey(userId, accountId);
        setStatusForKey(key, Step.RECURRING, TaskStatus.State.RUNNING, null, null);
        try {
            List<Transaction> txns = loadTransactions(userId, accountId);
            String prompt = buildRecurringPrompt(txns);
            JsonNode res = callGeminiWithRetry(prompt);

            if (res != null)
                saveRecurringResults(userId, res);

            setStatusForKey(key, Step.RECURRING, TaskStatus.State.DONE, null, res);
            log.info("Recurring DONE for {}", key);
        } catch (Exception ex) {
            setStatusForKey(key, Step.RECURRING, TaskStatus.State.ERROR, ex.getMessage(), null);
            log.error("Recurring ERROR for {}: {}", key, ex.getMessage(), ex);
        }
    }

    @Override
    public void processAnomaliesAsync(Long userId) {
        processAnomaliesAsync(userId, null);
    }

    @Async
    public void processAnomaliesAsync(Long userId, Long accountId) {
        String key = statusKey(userId, accountId);
        setStatusForKey(key, Step.ANOMALIES, TaskStatus.State.RUNNING, null, null);
        try {
            List<Transaction> txns = loadTransactions(userId, accountId);
            String prompt = buildAnomaliesPrompt(txns);
            JsonNode res = callGeminiWithRetry(prompt);

            if (res != null)
                saveAnomalyResults(userId, res);
            setStatusForKey(key, Step.ANOMALIES, TaskStatus.State.DONE, null, res);
            log.info("Anomalies DONE for {}", key);
        } catch (Exception ex) {
            setStatusForKey(key, Step.ANOMALIES, TaskStatus.State.ERROR, ex.getMessage(), null);
            log.error("Anomalies ERROR for {}: {}", key, ex.getMessage(), ex);
        }
    }

    @Override
    public void processSummaryAsync(Long userId) {
        processSummaryAsync(userId, null);
    }

    @Async
    public void processSummaryAsync(Long userId, Long accountId) {
        String key = statusKey(userId, accountId);
        setStatusForKey(key, Step.SUMMARY, TaskStatus.State.RUNNING, null, null);
        try {
            List<Transaction> txns = loadTransactions(userId, accountId);
            List<Goal> goals = goalRepository.findByUser_Id(userId);

            String prompt = buildSummaryPrompt(txns, goals);
            JsonNode res = callGeminiWithRetry(prompt);

            if (res != null)
                saveSummaryResults(userId, res);
            setStatusForKey(key, Step.SUMMARY, TaskStatus.State.DONE, null, res);
            log.info("Summary DONE for {}", key);
        } catch (Exception ex) {
            setStatusForKey(key, Step.SUMMARY, TaskStatus.State.ERROR, ex.getMessage(), null);
            log.error("Summary ERROR for {}: {}", key, ex.getMessage(), ex);
        }
    }

    // --------------------
    // Status helpers
    // --------------------
    private String statusKey(Long userId, Long accountId) {
        return userId + ":" + (accountId == null ? "all" : accountId.toString());
    }

    private void ensureStatusEntryForKey(String key) {
        statusStore.computeIfAbsent(key, k -> defaultStatusMap());
    }

    private EnumMap<Step, TaskStatus> defaultStatusMap() {
        EnumMap<Step, TaskStatus> map = new EnumMap<>(Step.class);
        for (Step s : Step.values())
            map.put(s, new TaskStatus());
        return map;
    }

    private void setStatusForKey(String key, Step step, TaskStatus.State state, String message, JsonNode result) {
        ensureStatusEntryForKey(key);
        TaskStatus ts = statusStore.get(key).get(step);
        ts.state = state;
        ts.message = message;
        ts.result = result;
        ts.updatedAt = LocalDateTime.now();
    }

    public Map<Step, TaskStatus> getStatusForUserAccount(Long userId, Long accountId) {
        String key = statusKey(userId, accountId);
        return statusStore.getOrDefault(key, defaultStatusMap());
    }

    public TaskStatus getStepStatusForUserAccount(Long userId, Long accountId, Step step) {
        return getStatusForUserAccount(userId, accountId).get(step);
    }

    // --------------------
    // Transaction loader
    // --------------------
    private List<Transaction> loadTransactions(Long userId, Long accountId) {
        if (accountId != null) {
            // ensure repository implements this
            return transactionRepository.findAllByBankAccountId(accountId);
        } else {
            return transactionRepository.findAllByUserId(userId);
        }
    }

    // --------------------
    // Gemini call + retry + extraction
    // --------------------
    private JsonNode callGeminiWithRetry(String prompt) throws Exception {
        int maxRetries = 3;
        int backoff = 3000;
        Exception lastEx = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return callGemini(prompt);
            } catch (Exception ex) {
                lastEx = ex;
                String m = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                if ((m.contains("503") || m.contains("timed out") || m.contains("unavailable") || m.contains("429"))
                        && attempt < maxRetries) {
                    log.warn("Gemini busy (attempt {}), backing off {}ms: {}", attempt, backoff, m);
                    Thread.sleep(backoff);
                    backoff *= 2;
                    continue;
                } else
                    throw ex;
            }
        }
        throw new RuntimeException("Gemini failed after retries", lastEx);
    }

    private JsonNode callGemini(String prompt) throws Exception {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = GEMINI_URL + "?key=" + apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        RestTemplate local = new RestTemplate(requestFactory);

        ResponseEntity<String> resp = local.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        if (resp == null || resp.getBody() == null)
            throw new RuntimeException("Empty response from Gemini");

        JsonNode root = objectMapper.readTree(resp.getBody());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.size() == 0) {
            throw new RuntimeException("Unexpected Gemini response (no candidates). Raw: " + resp.getBody());
        }

        JsonNode first = candidates.get(0);
        if (first == null)
            throw new RuntimeException("No candidate[0] in Gemini response");
        JsonNode parts = first.path("content").path("parts");
        if (!parts.isArray() || parts.size() == 0)
            throw new RuntimeException("No parts in candidate");
        String text = parts.get(0).path("text").asText("").trim();

        String jsonText = extractJsonSnippet(text);
        try {
            return objectMapper.readTree(jsonText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini JSON. Raw: " + text + " Extracted: " + jsonText, e);
        }
    }

    private String extractJsonSnippet(String text) {
        if (text == null)
            return "{}";
        text = text.trim();
        if (text.startsWith("```"))
            text = text.replaceAll("^```+|```+$", "").trim();
        int objStart = text.indexOf("{");
        int arrStart = text.indexOf("[");
        int start = -1;
        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart))
            start = objStart;
        else if (arrStart >= 0)
            start = arrStart;
        if (start == -1)
            return text;
        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open)
                depth++;
            else if (c == close) {
                depth--;
                if (depth == 0)
                    return text.substring(start, i + 1);
            }
        }
        return text.substring(start);
    }

    // --------------------
    // Prompt builders (S2: split analytics)
    // --------------------
    private String buildCategorizationPrompt(List<Transaction> txns, List<Rule> rules) throws Exception {
        // produce categorized + category totals
        List<Map<String, Object>> compact = txns.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("date", t.getDate() != null ? t.getDate().toString() : null);
            m.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
            m.put("merchant", t.getMerchant() == null ? "" : t.getMerchant());
            m.put("notes", t.getNotes() == null ? "" : t.getNotes());
            m.put("type", t.getTypeTransaction() != null ? t.getTypeTransaction().toString().toLowerCase() : "debit");
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> compactRules = (rules == null) ? Collections.emptyList() : rules.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("keyword", r.getKeyword());
            m.put("regex", r.getRegex());
            m.put("minAmount", r.getMinAmount());
            m.put("maxAmount", r.getMaxAmount());
            m.put("priority", r.getPriority());
            m.put("category", r.getCategory() != null ? r.getCategory().getName() : null);
            m.put("subCategory", r.getSubCategory() != null ? r.getSubCategory().getName() : null);
            return m;
        }).collect(Collectors.toList());

        String txJson = objectMapper.writeValueAsString(compact);
        String rulesJson = objectMapper.writeValueAsString(compactRules);

        return """
                You are LifeLedger's Categorization engine. Use the USER RULES first, then AI fallback.
                Output STRICT JSON only with this shape:
                {
                  "categorized": [
                    { "id": 0, "type": "debit|credit", "category":"string", "subCategory":"string","date":"string" }
                  ],
                  "categoryTotals": {
                    "Food": { "total": 0.0, "subCategories": { "Dining": 0.0, "Groceries": 0.0 } },
                    ...
                  }
                }

                USER RULES:
                """ + rulesJson + "\n\nTRANSACTIONS:\n" + txJson;
    }

    private String buildRecurringPrompt(List<Transaction> txns) throws Exception {
        List<Map<String, Object>> compact = txns.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("merchant", t.getMerchant() == null ? "" : t.getMerchant());
            m.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
            m.put("date", t.getDate() != null ? t.getDate().toString() : null);
            m.put("notes", t.getNotes() == null ? "" : t.getNotes());
            return m;
        }).collect(Collectors.toList());

        String txJson = objectMapper.writeValueAsString(compact);

        return """
                You are LifeLedger's Recurring detection engine.
                Detect subscriptions, EMIs, rent, monthly charges and output STRICT JSON:
                {
                  "recurring": [
                    {
                      "merchant":"string",
                      "frequency":"daily|weekly|monthly|yearly",
                      "totalAmount":0.0,
                      "nextDueDate":"YYYY-MM-DD"
                    }
                  ]
                }

                TRANSACTIONS:
                """ + txJson;
    }

    private String buildAnomaliesPrompt(List<Transaction> txns) throws Exception {
        List<Map<String, Object>> compact = txns.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("merchant", t.getMerchant() == null ? "" : t.getMerchant());
            m.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
            m.put("date", t.getDate() != null ? t.getDate().toString() : null);
            m.put("notes", t.getNotes() == null ? "" : t.getNotes());
            return m;
        }).collect(Collectors.toList());

        String txJson = objectMapper.writeValueAsString(compact);

        return """
                You are LifeLedger's Anomaly detector.
                Output STRICT JSON:
                {
                  "anomalies":[ { "id":0, "reason":"string" } ],
                  "highFrequencyMerchants":[ { "merchant":"string", "count":0, "total":0.0 } ]
                }

                TRANSACTIONS:
                """ + txJson;
    }

    private String buildSummaryPrompt(List<Transaction> txns, List<Goal> goals) throws Exception {
        // summary does the heavy analytics: burn-rate, spending vs income, trendlines,
        // nudges, human summary
        List<Map<String, Object>> compact = txns.stream().limit(200).map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("merchant", t.getMerchant() == null ? "" : t.getMerchant());
            m.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
            m.put("date", t.getDate() != null ? t.getDate().toString() : null);
            m.put("type", t.getTypeTransaction() != null ? t.getTypeTransaction().toString().toLowerCase() : "debit");
            m.put("category", t.getCategory() != null ? t.getCategory().getName() : null);
            m.put("subCategory", t.getSubCategory() != null ? t.getSubCategory().getName() : null);
            return m;
        }).collect(Collectors.toList());

        String txJson = objectMapper.writeValueAsString(compact);
        String goalsJson = objectMapper.writeValueAsString(goals == null ? Collections.emptyList() : goals);

        return """
                You are LifeLedger's Financial Insights engine.
                Produce strict JSON with these fields:
                {
                  "summary": { "text":"string", "tone":"positive|warning|negative|neutral", "emoji":"string", "color":"string" },
                  "nudges":[ { "type":"string", "text":"string" } ],
                  "spendingBreakdown":{
                    "totalSpent":0.0,
                    "totalIncome":0.0,
                    "netCashflow":0.0,
                    "burnRate":{"dailyAvg":0.0,"projectedMonthEnd":0.0},
                    "byCategory": { "Food": { "total":0.0, "percentage":0.0, "subCategories": { "Dining":0.0 } } },
                    "highFrequencyMerchants":[ { "merchant":"string","count":0,"total":0.0 } ],
                    "weeklyTrends":[ { "week":"YYYY-W##", "total":0.0 } ],
                    "monthlyTrends":[ { "month":"YYYY-MM", "total":0.0 } ]
                  }
                }

                TRANSACTIONS:
                """
                + txJson + "\nGOALS:\n" + goalsJson;
    }

    // --------------------
    // Save helpers
    // --------------------
    @Transactional
    protected void saveCategorizedResults(Long userId, JsonNode categorizedJson) {
        try {
            if (categorizedJson == null || !categorizedJson.has("categorized"))
                return;
            List<Transaction> toUpdate = new ArrayList<>();

            Map<String, Category> catCache = categoryRepository.findAllByUser_Id(userId).stream()
                    .collect(Collectors.toMap(c -> c.getName().toLowerCase(), c -> c));

            List<Category> newCats = new ArrayList<>();
            List<SubCategory> newSubCats = new ArrayList<>();

            for (JsonNode item : categorizedJson.path("categorized")) {
                if (item == null || !item.isObject())
                    continue;
                long txnId = item.path("id").asLong(0L);
                if (txnId == 0L)
                    continue;
                String catName = item.path("category").asText(null);
                String subName = item.path("subCategory").asText(null);
                String type = item.path("type").asText(null);

                Transaction txn = transactionRepository.findById(txnId).orElse(null);
                if (txn == null)
                    continue;

                Category category = null;
                if (catName != null && !catName.isBlank()) {
                    String key = catName.toLowerCase();
                    if (catCache.containsKey(key))
                        category = catCache.get(key);
                    else {
                        Category c = Category.builder().name(catName).user(User.builder().id(userId).build()).build();
                        newCats.add(c);
                        category = c;
                        catCache.put(key, c);
                    }
                }

                SubCategory sub = null;
                if (subName != null && !subName.isBlank() && category != null) {
                    Long catId = category.getId();
                    if (catId != null) {
                        Optional<SubCategory> scOpt = subCategoryRepository.findByCategory_IdAndNameIgnoreCase(catId,
                                subName);
                        if (scOpt.isPresent())
                            sub = scOpt.get();
                    }
                    if (sub == null) {
                        SubCategory sc = SubCategory.builder().name(subName).category(category).build();
                        newSubCats.add(sc);
                        sub = sc;
                    }
                }

                if (category != null)
                    txn.setCategory(category);
                if (sub != null)
                    txn.setSubCategory(sub);
                if (type != null && !type.isBlank()) {
                    try {
                        txn.setTypeTransaction(com.Life_ledger.Enum.TransactionEnum.valueOf(type.toUpperCase()));
                    } catch (Exception ignored) {
                    }
                }
                toUpdate.add(txn);
            }

            if (!newCats.isEmpty()) {
                List<Category> saved = categoryRepository.saveAll(newCats);
                for (Category c : saved)
                    catCache.put(c.getName().toLowerCase(), c);
            }
            if (!newSubCats.isEmpty())
                subCategoryRepository.saveAll(newSubCats);
            if (!toUpdate.isEmpty()) {
                transactionRepository.saveAll(toUpdate);
                log.info("Saved {} categorized transactions for user={}", toUpdate.size(), userId);
            }

        } catch (Exception e) {
            log.error("Failed saving categorized results: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected void saveRecurringResults(Long userId, JsonNode recurringJson) {
        try {
            if (recurringJson == null || !recurringJson.has("recurring"))
                return;
            List<RecurringPattern> list = new ArrayList<>();
            for (JsonNode item : recurringJson.path("recurring")) {
                if (item == null || !item.isObject())
                    continue;
                String merchant = item.path("merchant").asText("").trim();
                if (merchant.isBlank())
                    continue;
                String frequency = item.path("frequency").asText("monthly").trim();
                double amt = item.path("totalAmount").asDouble(0.0);
                String next = item.path("nextDueDate").asText(null);

                RecurringPattern rp = RecurringPattern.builder()
                        .merchant(merchant)
                        .frequency(frequency)
                        .amount(BigDecimal.valueOf(amt))
                        .reason(null)
                        .build();
                if (next != null && !next.isBlank()) {
                    try {
                        rp.setNextDueDate(LocalDate.parse(next));
                    } catch (Exception ignored) {
                    }
                }
                list.add(rp);
            }
            if (!list.isEmpty()) {
                recurringPatternRepository.saveAll(list);
                log.info("Saved {} recurring patterns for user={}", list.size(), userId);
            }
        } catch (Exception e) {
            log.error("Failed saving recurring results: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected void saveAnomalyResults(Long userId, JsonNode anomaliesJson) {
        try {
            if (anomaliesJson == null || !anomaliesJson.has("anomalies"))
                return;

            List<Transaction> toUpdate = new ArrayList<>();
            List<AnomalyRecord> toSave = new ArrayList<>();

            for (JsonNode item : anomaliesJson.path("anomalies")) {
                long id = item.path("id").asLong(0L);
                if (id == 0L)
                    continue;

                String reason = item.path("reason").asText("No reason provided");
                double confidence = item.path("confidence").asDouble(0.80);

                Transaction txn = transactionRepository.findById(id).orElse(null);
                if (txn == null)
                    continue;

                txn.setAnomaly(true);
                toUpdate.add(txn);

                AnomalyRecord record = AnomalyRecord.builder()
                        .transaction(txn)
                        .bankAccount(txn.getBankAccount()) // 🔥 FIXED
                        .reason(reason)
                        .confidence(confidence)
                        .anomalyType("AI")
                        .createdAt(LocalDateTime.now())
                        .build();

                toSave.add(record);
            }

            if (!toUpdate.isEmpty())
                transactionRepository.saveAll(toUpdate);

            if (!toSave.isEmpty())
                anomalyRecordRepository.saveAll(toSave);

            log.info("Saved {} anomaly records for user={}", toSave.size(), userId);

        } catch (Exception e) {
            log.error("Failed saving anomaly results: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected void saveSummaryResults(Long accountId, JsonNode summaryJson) {
        try {
            if (summaryJson == null || !summaryJson.has("summary"))
                return;
            String text = summaryJson.path("summary").path("text").asText("");
            if (text == null || text.isBlank())
                return;
            Insight insight = Insight.builder()
                    .bankAccount(BankAccount.builder().id(accountId).build())
                    .aiText(text)
                    .build();
            insightRepository.save(insight);
            log.info("Saved insight for user={}", accountId);
        } catch (Exception e) {
            log.error("Failed saving summary results: {}", e.getMessage(), e);
        }
    }
}
