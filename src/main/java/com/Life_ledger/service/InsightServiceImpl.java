package com.Life_ledger.service;

import com.Life_ledger.dto.insight.*;
import com.Life_ledger.dto.recurring.RecurringDetectionResponse;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.repository.InsightRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private final InsightRepository insightRepository;
    private final RecurringPatternService recurringPatternService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Insight createInsight(Insight insight) {
        Insight savedInsight = insightRepository.save(insight);
        
        // Process recurring patterns after insight creation
        recurringPatternService.processInsightPatterns(savedInsight);
        
        return savedInsight;
    }

    @Override
    public Insight getInsight(Long id) {
        return insightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insight not found with id: " + id));
    }

    @Override
    public List<Insight> getInsightsByBankAccountId(Long accountId) {
        return List.of();
    }

    @Override
    public List<Insight> getInsightsByUserId(Long userId) {
        return insightRepository.findByUser_Id(userId);
    }

    @Override
    public Insight updateInsight(Long id, String aiText) {
        Insight existing = getInsight(id);
        existing.setAiText(aiText);
        return insightRepository.save(existing);
    }

    @Override
    public void deleteInsight(Long id) {
        insightRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public InsightResponseDTO getInsightDTO(Long id) {
        Insight insight = getInsight(id);

        InsightResponseDTO dto = new InsightResponseDTO();
        dto.setId(insight.getId());
        dto.setAiText(insight.getAiText());
        dto.setCreatedAt(insight.getCreatedAt());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsightResponseDTO> getInsightsByUserIdDTO(Long userId) {
        return insightRepository.findByUserId(userId)
                .stream()
                .map(insight -> {
                    InsightResponseDTO dto = new InsightResponseDTO();
                    dto.setId(insight.getId());
                    dto.setAiText(insight.getAiText());
                    dto.setCreatedAt(insight.getCreatedAt());
                    return dto;
                })
                .toList();
    }

    @Override
    public Insight getLatestInsight(Long userId) {
        return insightRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getInsightSection(Long userId, String sectionName) {
        Insight latest = getLatestInsight(userId);
        if (latest == null) return null;
        
        try {
            JsonNode rootNode = objectMapper.readTree(latest.getAiText());
            
            switch (sectionName.toLowerCase()) {
                case "health":
                    return parseHealthSection(rootNode);
                case "spending":
                    return parseSpendingSection(rootNode);
                case "patterns":
                    return parsePatternsSection(rootNode);
                case "anomalies":
                    return parseAnomaliesSection(rootNode);
                default:
                    return rootNode.get(sectionName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing insight section: " + e.getMessage());
        }
    }

    @Override
    public boolean hasRecentAnalysis(Long userId, int hours) {
        Insight latest = getLatestInsight(userId);
        if (latest == null) return false;
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return latest.getCreatedAt().isAfter(cutoff);
    }

    @Override
    public Insight getLatestInsightByAccount(Long accountId) {
        return insightRepository.findTopByBankAccountIdOrderByCreatedAtDesc(accountId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getInsightSectionByAccount(Long accountId, String sectionName) {
        Insight latest = getLatestInsightByAccount(accountId);
        if (latest == null) return null;
        
        try {
            JsonNode rootNode = objectMapper.readTree(latest.getAiText());
            
            switch (sectionName.toLowerCase()) {
                case "health":
                    return parseHealthSection(rootNode);
                case "spending":
                    return parseSpendingSection(rootNode);
                case "patterns":
                    return parsePatternsSection(rootNode);
                case "anomalies":
                    return parseAnomaliesSection(rootNode);
                default:
                    return rootNode.get(sectionName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing insight section: " + e.getMessage());
        }
    }

    @Override
    public boolean hasRecentAnalysisByAccount(Long accountId, int hours) {
        Insight latest = getLatestInsightByAccount(accountId);
        if (latest == null) return false;
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return latest.getCreatedAt().isAfter(cutoff);
    }

    private Object parseHealthSection(JsonNode rootNode) {
        JsonNode healthNode = rootNode.get("financialHealth");
        if (healthNode == null) return null;
        
        return AIInsightSectionDTO.of(
            "health",
            healthNode,
            healthNode.has("summary") ? healthNode.get("summary").asText() : "Financial health analysis"
        );
    }

    private Object parseSpendingSection(JsonNode rootNode) {
        JsonNode spendingNode = rootNode.get("spendingBreakdown");
        if (spendingNode == null) return null;
        
        List<CategoryBreakdown> categories = new ArrayList<>();
        if (spendingNode.has("categories")) {
            spendingNode.get("categories").forEach(categoryNode -> {
                CategoryBreakdown breakdown = new CategoryBreakdown();
                breakdown.setCategoryName(categoryNode.get("name").asText());
                breakdown.setAmount(categoryNode.get("amount").decimalValue());
                breakdown.setPercentage(categoryNode.get("percentage").asDouble());
                breakdown.setTransactionCount(categoryNode.get("count").asInt());
                categories.add(breakdown);
            });
        }
        
        return AIInsightSectionDTO.of(
            "spending",
            categories,
            spendingNode.has("summary") ? spendingNode.get("summary").asText() : "Spending breakdown analysis"
        );
    }

    private Object parsePatternsSection(JsonNode rootNode) {
        JsonNode patternsNode = rootNode.get("recurringPatterns");
        if (patternsNode == null) return null;
        
        List<RecurringDetectionResponse> patterns = new ArrayList<>();
        if (patternsNode.has("patterns")) {
            patternsNode.get("patterns").forEach(patternNode -> {
                RecurringDetectionResponse pattern = RecurringDetectionResponse.builder()
                    .merchant(patternNode.get("merchant").asText())
                    .averageAmount(patternNode.get("amount").decimalValue())
                    .frequency(patternNode.get("frequency").asText())
                    .occurrenceCount(patternNode.get("count").asInt())
                    .isRecurring(true)
                    .build();
                patterns.add(pattern);
            });
        }
        
        return AIInsightSectionDTO.of(
            "patterns",
            patterns,
            patternsNode.has("summary") ? patternsNode.get("summary").asText() : "Recurring patterns analysis"
        );
    }

    private Object parseAnomaliesSection(JsonNode rootNode) {
        JsonNode anomaliesNode = rootNode.get("anomalies");
        if (anomaliesNode == null) return null;
        
        List<AnomalyDTO> anomalies = new ArrayList<>();
        if (anomaliesNode.has("detected")) {
            anomaliesNode.get("detected").forEach(anomalyNode -> {
                AnomalyDTO anomaly = new AnomalyDTO();
                anomaly.setTransactionId(anomalyNode.get("transactionId").asLong());
                anomaly.setMerchant(anomalyNode.get("merchant").asText());
                anomaly.setAmount(anomalyNode.get("amount").decimalValue());
                anomaly.setReason(anomalyNode.get("reason").asText());
                anomaly.setAnomalyType(anomalyNode.get("type").asText());
                anomaly.setConfidence(anomalyNode.get("confidence").asDouble());
                anomalies.add(anomaly);
            });
        }
        
        return AIInsightSectionDTO.of(
            "anomalies",
            anomalies,
            anomaliesNode.has("summary") ? anomaliesNode.get("summary").asText() : "Anomaly detection analysis"
        );
    }

    @Transactional(readOnly = true) // :white_tick: REQUIRED
    @Override
    public List<InsightResponseDTO> getInsightsByAccount(Long accountId) {
        return insightRepository.findByBankAccount_Id(accountId)
                .stream()
                .map(insight -> {
                    InsightResponseDTO dto = new InsightResponseDTO();
                    dto.setId(insight.getId());
                    // :white_tick: LOB accessed while session is OPEN
                    dto.setAiText(insight.getAiText());
                    dto.setCreatedAt(insight.getCreatedAt());
                    return dto;
                })
                .toList();
    }
}