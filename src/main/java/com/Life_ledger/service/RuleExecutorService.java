package com.Life_ledger.service;

import com.Life_ledger.entity.Transaction;

import java.util.List;

public interface RuleExecutorService {
    List<Transaction> applyRulesForFileImport(Long fileId);



}
