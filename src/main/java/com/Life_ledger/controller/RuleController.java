package com.Life_ledger.controller;

import com.Life_ledger.entity.Transaction;
import com.Life_ledger.service.RuleExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class RuleController {
    @Autowired
    private RuleExecutorService ruleExecutorService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyRulesforText(@RequestParam Long fileImportId) throws Exception {
        List<Transaction> updated = ruleExecutorService.applyRulesForFileImport(fileImportId);
        return ResponseEntity.status(201).body(updated);

}
}
