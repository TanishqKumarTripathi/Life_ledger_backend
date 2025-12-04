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

    // 🔥 FIXED — now fetch by bank account ID
    @Override
    public List<AnomalyRecord> getAnomaliesByBankAccount(Long bankAccountId) {
        return anomalyRepo.findByBankAccount_Id(bankAccountId);
    }

    @Override
    public AnomalyRecord markResolved(Long id, boolean resolved, String comment) {
        AnomalyRecord anomaly = getAnomaly(id);

        anomaly.setAnomalyType(resolved ? "RESOLVED" : anomaly.getAnomalyType());
        anomaly.setReason(comment != null ? comment : anomaly.getReason());

        return anomalyRepo.save(anomaly);
    }

    @Override
    public void deleteAnomaly(Long id) {
        anomalyRepo.deleteById(id);
    }
}
