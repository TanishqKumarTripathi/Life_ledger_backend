package com.Life_ledger.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.Life_ledger.Enum.CategorySource;
import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleBasedCategoryService {

    private final CategoryRepository categoryRepo;

    // Keyword → Category name mapping
    private static final Map<String, String> RULES = Map.ofEntries(
            Map.entry("zomato", "Food"),
            Map.entry("swiggy", "Food"),
            Map.entry("restaurant", "Food"),

            Map.entry("amazon", "Shopping"),
            Map.entry("flipkart", "Shopping"),

            Map.entry("uber", "Travel"),
            Map.entry("ola", "Travel"),
            Map.entry("irctc", "Travel"),

            Map.entry("electricity", "Utilities"),
            Map.entry("recharge", "Utilities"),

            Map.entry("salary", "Income"),
            Map.entry("credit interest", "Income"));

    public Optional<Category> categorize(String description, User user) {

        if (description == null || description.isBlank()) {
            return Optional.empty();
        }

        String text = description.toLowerCase();
        log.info("Rule engine checking description: [{}]", text);

        for (Map.Entry<String, String> rule : RULES.entrySet()) {
            if (text.contains(rule.getKey())) {
                log.info("Checking rule keyword: [{}]", rule.getKey());
                return categoryRepo
                        .findByUser_IdAndNameIgnoreCase(user.getId(), rule.getValue())
                        .or(() -> Optional.of(
                                categoryRepo.save(
                                        Category.builder()
                                                .name(rule.getValue())
                                                .user(user)
                                                .categorySource(CategorySource.RULE)
                                                .build())));
            }
        }

        return Optional.empty();
    }

}
