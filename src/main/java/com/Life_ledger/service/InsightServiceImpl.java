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
    public InsightResponseDTO getInsightDTO(Long id) {
        Insight insight = getInsight(id);

        InsightResponseDTO dto = new InsightResponseDTO();
        dto.setId(insight.getId());
        dto.setAiText(insight.getAiText());
        dto.setCreatedAt(insight.getCreatedAt());

        return dto;
    }

    @Override
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

}
