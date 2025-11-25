package com.Life_ledger.service;

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
    public List<Insight> getAllInsights() {
        return insightRepository.findAll();
    }

    @Override
    public Insight updateInsight(Long id, Insight insight) {
        Insight existingInsight = getInsight(id);
        existingInsight.setAiText(insight.getAiText());
        existingInsight.setUser(insight.getUser());
        existingInsight.setRelatedTransactions(insight.getRelatedTransactions());
        return insightRepository.save(existingInsight);
    }

    @Override
    public void deleteInsight(Long id) {
        insightRepository.deleteById(id);
    }
}
