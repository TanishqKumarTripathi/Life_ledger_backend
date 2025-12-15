package com.Life_ledger.service;

// import com.Life_ledger.dto.rule.RuleRequest;
import com.Life_ledger.dto.rules.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RuleService {

    RuleResponse createRule(Long userId, RuleRequest request);

    List<RuleResponse> getAllRules(Long userId);

    void deleteRule(Long userId, Long ruleId);

    Optional<Map<String, Object>> applyRules(Long userId, String description, Double amount);
}
