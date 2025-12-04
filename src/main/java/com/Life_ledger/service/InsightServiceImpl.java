package com.Life_ledger.service;

import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.repository.InsightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private final InsightRepository insightRepository;

    @Override
    public Insight createInsight(Insight insight) {
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
}
