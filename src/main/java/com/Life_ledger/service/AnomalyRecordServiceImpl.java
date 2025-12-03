package com.Life_ledger.service;

import com.Life_ledger.entity.AnomalyRecord;
import com.Life_ledger.repository.AnomalyRecordRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnomalyRecordServiceImpl implements AnomalyRecordService {

    private final AnomalyRecordRepository anomalyRepo;

    @Override
    public AnomalyRecord saveAnomaly(AnomalyRecord anomalyRecord) {
        return anomalyRepo.save(anomalyRecord);
    }

    @Override
    public AnomalyRecord getAnomaly(Long id) {
        return anomalyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Anomaly not found with ID: " + id));
    }

    @Override
    public List<AnomalyRecord> getAnomaliesByUser(Long userId) {
        return anomalyRepo.findByUserId(userId);
    }

    // @Override
    // public AnomalyRecord markResolved(Long id, boolean resolved, String comment)
    // {
    // AnomalyRecord anomaly = getAnomaly(id);
    // anomaly.setResolved(resolved);
    // anomaly.setUserComment(comment);
    // return anomalyRepo.save(anomaly);
    // }

    @Override
    public void deleteAnomaly(Long id) {
        anomalyRepo.deleteById(id);
    }

    @Override
    public AnomalyRecord markResolved(Long id, boolean resolved, String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'markResolved'");
    }
}
