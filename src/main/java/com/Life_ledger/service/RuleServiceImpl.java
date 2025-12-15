package com.Life_ledger.service;

import com.Life_ledger.dto.rules.RuleRequest;
import com.Life_ledger.dto.rules.RuleResponse;
import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.Rule;
import com.Life_ledger.entity.SubCategory;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.CategoryRepository;
import com.Life_ledger.repository.RuleRepository;
import com.Life_ledger.repository.SubCategoryRepository;
import com.Life_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final UserRepository userRepository;

    /**
     * Create a rule for a user. Will create category / subcategory if they don't
     * exist.
     */
    @Override
    @Transactional
    public RuleResponse createRule(Long userId, RuleRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        // Normalize names
        String categoryName = req.getCategoryName() != null ? req.getCategoryName().trim() : null;
        String subCategoryName = req.getSubCategoryName() != null ? req.getSubCategoryName().trim() : null;

        Category category = null;
        SubCategory subCategory = null;

        if (categoryName != null && !categoryName.isEmpty()) {
            // find existing category for this user (case-insensitive)
            Optional<Category> existingCat = categoryRepository.findByUser_IdAndNameIgnoreCase(userId, categoryName);
            if (existingCat.isPresent()) {
                category = existingCat.get();
            } else {
                Category newCat = new Category();
                newCat.setName(categoryName);
                newCat.setUser(user);
                category = categoryRepository.save(newCat);
            }
        }

        if (subCategoryName != null && !subCategoryName.isEmpty() && category != null) {
            // try to find existing subcategory under this category
            Optional<SubCategory> existingSub = subCategoryRepository
                    .findByCategory_IdAndNameIgnoreCase(category.getId(), subCategoryName);
            if (existingSub.isPresent()) {
                subCategory = existingSub.get();
            } else {
                SubCategory sc = new SubCategory();
                sc.setName(subCategoryName);
                sc.setCategory(category);
                subCategory = subCategoryRepository.save(sc);
            }
        }

        Rule rule = new Rule();
        rule.setKeyword(req.getKeyword() == null ? null : req.getKeyword().trim().toLowerCase());
        rule.setRegex(req.getRegex());
        rule.setMinAmount(req.getMinAmount());
        rule.setMaxAmount(req.getMaxAmount());
        rule.setPriority(req.getPriority() == null ? 100 : req.getPriority());
        rule.setUser(user);
        rule.setCategory(category);
        rule.setSubCategory(subCategory);

        Rule saved = ruleRepository.save(rule);

        RuleResponse resp = new RuleResponse();
        resp.setId(saved.getId());
        resp.setKeyword(saved.getKeyword());
        resp.setRegex(saved.getRegex());
        resp.setMinAmount(saved.getMinAmount());
        resp.setMaxAmount(saved.getMaxAmount());
        resp.setPriority(saved.getPriority());
        resp.setCategoryId(category != null ? category.getId() : null);
        resp.setCategoryName(category != null ? category.getName() : null);
        resp.setSubCategoryId(subCategory != null ? subCategory.getId() : null);
        resp.setSubCategoryName(subCategory != null ? subCategory.getName() : null);
        return resp;
    }

    /**
     * Return all rules for a user sorted by priority asc.
     */
    @Override
    public List<RuleResponse> getAllRules(Long userId) {
        List<Rule> rules = ruleRepository.findByUser_IdOrderByPriorityAsc(userId);
        List<RuleResponse> out = new ArrayList<>(rules.size());

        for (Rule r : rules) {
            RuleResponse rr = new RuleResponse();
            rr.setId(r.getId());
            rr.setKeyword(r.getKeyword());
            rr.setRegex(r.getRegex());
            rr.setMinAmount(r.getMinAmount());
            rr.setMaxAmount(r.getMaxAmount());
            rr.setPriority(r.getPriority());
            rr.setCategoryId(r.getCategory() != null ? r.getCategory().getId() : null);
            rr.setCategoryName(r.getCategory() != null ? r.getCategory().getName() : null);
            rr.setSubCategoryId(r.getSubCategory() != null ? r.getSubCategory().getId() : null);
            rr.setSubCategoryName(r.getSubCategory() != null ? r.getSubCategory().getName() : null);
            out.add(rr);
        }

        return out;
    }

    /**
     * Delete a rule (only if it belongs to the same user)
     */
    @Override
    public void deleteRule(Long userId, Long ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found"));
        if (!rule.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized rule deletion");
        }
        ruleRepository.delete(rule);
    }

    /**
     * Apply user rules to a given description + amount. Returns optional map with
     * category/subcategory ids and names.
     */
    @Override
    public Optional<Map<String, Object>> applyRules(Long userId, String description, Double amount) {
        if (description == null || description.trim().isEmpty()) {
            return Optional.empty();
        }

        String textLower = description.toLowerCase();
        List<Rule> rules = ruleRepository.findByUser_IdOrderByPriorityAsc(userId);

        for (Rule rule : rules) {
            boolean keywordMatch = rule.getKeyword() != null && !rule.getKeyword().isBlank()
                    && textLower.contains(rule.getKeyword().toLowerCase());

            boolean regexMatch = false;
            if (rule.getRegex() != null && !rule.getRegex().isBlank()) {
                try {
                    regexMatch = Pattern.compile(rule.getRegex(), Pattern.CASE_INSENSITIVE)
                            .matcher(description)
                            .find();
                } catch (Exception ignored) {
                    // invalid regex — treat as no match
                }
            }

            boolean amountMatch = (rule.getMinAmount() == null || amount == null || amount >= rule.getMinAmount()) &&
                    (rule.getMaxAmount() == null || amount == null || amount <= rule.getMaxAmount());

            if ((keywordMatch || regexMatch) && amountMatch) {
                Map<String, Object> out = new HashMap<>();
                out.put("categoryId", rule.getCategory() != null ? rule.getCategory().getId() : null);
                out.put("categoryName", rule.getCategory() != null ? rule.getCategory().getName() : null);
                out.put("subCategoryId", rule.getSubCategory() != null ? rule.getSubCategory().getId() : null);
                out.put("subCategoryName", rule.getSubCategory() != null ? rule.getSubCategory().getName() : null);
                out.put("matchedRuleId", rule.getId());
                out.put("confidence", 1.0);
                return Optional.of(out);
            }
        }

        return Optional.empty();
    }
}
