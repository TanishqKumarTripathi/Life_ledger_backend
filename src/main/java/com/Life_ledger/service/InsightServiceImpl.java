package com.Life_ledger.service;

import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.dto.insight.InsightSummaryResponse;
import com.Life_ledger.dto.insight.InsightType;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.InsightRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private final InsightRepository insightRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Insight createInsight(Insight insight) {
        JsonNode root = insight.getSummaryJson();
        return insightRepository.save(insight);
    }

    @Override
    public Insight getInsight(Long id) {
        return insightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insight not found with id: " + id));
    }

    // 🚀 NEW — correct fetch method
    @Override
    public List<Insight> getInsightsByBankAccountId(Long accountId) {
        return insightRepository.findByBankAccount_Id(accountId);

    }

    @Override
    public List<Insight> getInsightsByUserId(Long userId) {
        return insightRepository.findByBankAccount_User_Id(userId);
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
    public List<InsightResponseDTO> getInsightsByUserIdDTO(Long userId) {
        return insightRepository.findByBankAccount_User_Id(userId)
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
    @Transactional(readOnly = true)
    public InsightResponseDTO getInsightDTO(Long id) {
        Insight insight = getInsight(id);

        InsightResponseDTO dto = new InsightResponseDTO();
        dto.setId(insight.getId());
        dto.setAiText(insight.getAiText());
        dto.setCreatedAt(insight.getCreatedAt());

        return dto;
    }

    // 🚀 NEW — bank-account–based DTO fetch
    @Override
    @Transactional(readOnly = true)
    public List<InsightResponseDTO> getInsightsByBankAccountIdDTO(Long accountId) {
        return insightRepository.findByBankAccount_Id(accountId)
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

    @Transactional(readOnly = true) // ✅ REQUIRED
    @Override
    public List<InsightResponseDTO> getInsightsByAccount(Long accountId) {

        return insightRepository.findByBankAccount_Id(accountId)
                .stream()
                .map(insight -> {
                    InsightResponseDTO dto = new InsightResponseDTO();
                    dto.setId(insight.getId());

                    // ✅ LOB accessed while session is OPEN
                    dto.setAiText(insight.getAiText());

                    dto.setCreatedAt(insight.getCreatedAt());
                    return dto;
                })
                .toList();
    }

    @Override
    public Insight getLatestInsight(Long userId) {
        List<Insight> insights = insightRepository.findByBankAccount_User_Id(userId);
        return insights.isEmpty() ? null : insights.get(insights.size() - 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsightResponseDTO> getInsightsByUser(Long userId) {
        return insightRepository.findByBankAccount_User_Id(userId)
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
    @Transactional(readOnly = true)
    public InsightSummaryResponse getLatestSummaryByAccount(Long accountId) {
        return insightRepository.findTopByBankAccount_IdAndTypeOrderByCreatedAtDesc(accountId, InsightType.SUMMARY)
                .map(this::convertToSummaryResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public InsightSummaryResponse getLatestSummaryByUser(Long userId) {
        List<BankAccount> accounts = bankAccountRepository.findByUserId(userId);
        return accounts.stream()
                .map(acc -> getLatestSummaryByAccount(acc.getId()))
                .filter(summary -> summary != null)
                .findFirst()
                .orElse(null);
    }

    private InsightSummaryResponse convertToSummaryResponse(Insight insight) {
        try {
            JsonNode root = insight.getSummaryJson();
            if (root == null || root.isNull())
                return null;

            InsightSummaryResponse response = new InsightSummaryResponse();
            response.setId(insight.getId());
            response.setCreatedAt(insight.getCreatedAt());
            response.setPeriod(insight.getPeriod());

            // Summary
            JsonNode summaryNode = root.path("summary");
            if (!summaryNode.isMissingNode()) {
                InsightSummaryResponse.SummaryData summary = new InsightSummaryResponse.SummaryData();
                summary.setText(summaryNode.path("text").asText(null));
                summary.setTone(summaryNode.path("tone").asText(null));
                summary.setEmoji(summaryNode.path("emoji").asText(null));
                summary.setColor(summaryNode.path("color").asText(null));
                response.setSummary(summary);
            }

            // Nudges
            JsonNode nudgesNode = root.path("nudges");
            if (nudgesNode.isArray()) {
                List<InsightSummaryResponse.NudgeData> nudges = new java.util.ArrayList<>();
                for (JsonNode nudge : nudgesNode) {
                    InsightSummaryResponse.NudgeData nudgeData = new InsightSummaryResponse.NudgeData();
                    nudgeData.setType(nudge.path("type").asText(null));
                    nudgeData.setText(nudge.path("text").asText(null));
                    nudgeData.setTone(nudge.path("tone").asText(null));
                    nudgeData.setEmoji(nudge.path("emoji").asText(null));
                    nudges.add(nudgeData);
                }
                response.setNudges(nudges);
            }

            return response;
        } catch (Exception e) {
            return null;
        }
    }
}
