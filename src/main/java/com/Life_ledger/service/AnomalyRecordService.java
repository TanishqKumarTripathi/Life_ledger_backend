package com.Life_ledger.service;

import com.Life_ledger.entity.AnomalyRecord;
import java.util.List;

public interface AnomalyRecordService {

    AnomalyRecord saveAnomaly(AnomalyRecord anomalyRecord);

    AnomalyRecord getAnomaly(Long id);

    List<AnomalyRecord> getAnomaliesByBankAccount(Long bankAccountId);

    AnomalyRecord markResolved(Long id, boolean resolved, String comment);

    void deleteAnomaly(Long id);
}
