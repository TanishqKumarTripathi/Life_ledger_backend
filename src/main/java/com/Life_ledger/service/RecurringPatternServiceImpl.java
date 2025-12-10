package com.Life_ledger.service;

import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.repository.RecurringPatternRepository;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringPatternServiceImpl implements RecurringPatternService {

    private final RecurringPatternRepository recurringPatternRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RecurringPattern createRecurringPattern(RecurringPattern recurringPattern, Long userId) {
        BankAccount account = bankAccountRepository.findById(recurringPattern.getBankAccount().getId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized bank account access");
        }
        recurringPattern.setBankAccount(account);
        return recurringPatternRepository.save(recurringPattern);
    }

    @Override
    public RecurringPattern updateRecurringPattern(Long id, RecurringPattern updated, Long userId) {
        RecurringPattern existing = getRecurringPattern(id, userId);

        existing.setMerchant(updated.getMerchant());
        existing.setAmount(updated.getAmount());
        existing.setFrequency(updated.getFrequency());
        existing.setReason(updated.getReason());
        existing.setNextDueDate(updated.getNextDueDate());

        return recurringPatternRepository.save(existing);
    }

    @Override
    public RecurringPattern getRecurringPattern(Long id, Long userId) {
        RecurringPattern pattern = recurringPatternRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring Pattern not found"));

        if (!pattern.getBankAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return pattern;
    }

    @Override
    public List<RecurringPattern> getByBankAccount(Long bankAccountId, Long userId) {
        BankAccount account = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return recurringPatternRepository.findByBankAccount_Id(bankAccountId);
    }

    @Override
    public void deleteRecurringPattern(Long id, Long userId) {
        RecurringPattern pattern = getRecurringPattern(id, userId);
        recurringPatternRepository.delete(pattern);
    }

    @Override
    public List<RecurringPattern> getRecurringPatternsByUserId(Long userId) {
        return List.of();
    }

    @Override
    public List<RecurringPattern> getRecurringPatternsByAccountId(Long accountId) {
        return List.of();
    }

    @Override
    public void deleteRecurringPattern(Long id) {

    }

    @Override
    @Transactional
    public void processInsightPatterns(Insight insight) {
        try {
            System.out.println("Processing insight patterns for user ID: " + insight.getUser().getId());
            System.out.println("AI Text length: " + (insight.getAiText() != null ? insight.getAiText().length() : 0));

            JsonNode rootNode = objectMapper.readTree(insight.getAiText());

            // Debug: Print all available keys
            System.out.println("🔍 Available JSON keys: ");
            rootNode.fieldNames().forEachRemaining(key -> System.out.println("  - " + key));

            // Debug: Print first 500 chars of AI text
            String aiTextPreview = insight.getAiText().length() > 500
                ? insight.getAiText().substring(0, 500) + "..."
                : insight.getAiText();
            System.out.println("🔍 AI Text preview: " + aiTextPreview);

            // Try multiple possible nested paths for recurring data
            JsonNode recurringNode = null;
            String foundPath = "none";

            // Check direct paths first
            String[] directPaths = {"recurring", "patterns", "recurringPatterns"};
            for (String path : directPaths) {
                recurringNode = rootNode.path(path);
                if (!recurringNode.isMissingNode() && recurringNode.isArray()) {
                    foundPath = path;
                    break;
                }
            }

            // Check nested paths under "analysis"
            if (recurringNode == null || recurringNode.isMissingNode()) {
                JsonNode analysisNode = rootNode.path("analysis");
                if (!analysisNode.isMissingNode()) {
                    for (String path : directPaths) {
                        recurringNode = analysisNode.path(path);
                        if (!recurringNode.isMissingNode() && recurringNode.isArray()) {
                            foundPath = "analysis." + path;
                            break;
                        }
                    }
                }
            }

            System.out.println("🔍 Found recurring data at path: " + foundPath);

            System.out.println("🔍 Recurring node exists: " + !recurringNode.isMissingNode());
            System.out.println("🔍 Recurring node is array: " + recurringNode.isArray());
            if (!recurringNode.isMissingNode()) {
                System.out.println("🔍 Recurring node content: " + recurringNode.toString());
            }

            if (recurringNode != null && !recurringNode.isMissingNode() && recurringNode.isArray()) {
                System.out.println("🔍 Found " + recurringNode.size() + " recurring patterns in AI response");
                Long userId = insight.getUser().getId();

                for (JsonNode patternNode : recurringNode) {
                    System.out.println("🔍 Processing pattern node: " + patternNode.toString());
                    try {
                        createPatternFromJson(patternNode, userId);
                        System.out.println("✅ Pattern processed successfully");
                    } catch (Exception e) {
                        System.err.println("❌ Error processing pattern: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("❌ No recurring array found in AI response");
                System.out.println("🔍 RecurringNode is null: " + (recurringNode == null));
                System.out.println("🔍 RecurringNode is missing: " + (recurringNode != null && recurringNode.isMissingNode()));
                System.out.println("🔍 RecurringNode is array: " + (recurringNode != null && recurringNode.isArray()));
                if (recurringNode != null) {
                    System.out.println("🔍 RecurringNode content: " + recurringNode.toString());
                }
            }
        } catch (Exception e) {
            System.err.println(" Error processing recurring patterns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createPatternFromJson(JsonNode patternNode, Long userId) {
        // Handle the actual AI response format
        String description = patternNode.path("description").asText();
        String amountRange = patternNode.path("amount_range").asText();
        String frequency = patternNode.path("frequency").asText("Monthly");
        String category = patternNode.path("category").asText();

        // Extract merchant name from description (first part before parentheses)
        String merchant = extractMerchantFromDescription(description);

        // Extract amount from amount_range (use middle value or first number found)
        BigDecimal amount = extractAmountFromRange(amountRange);

        System.out.println("🔍 Processing recurring pattern:");
        System.out.println("🔍   - Description: " + description);
        System.out.println("🔍   - Amount Range: " + amountRange);
        System.out.println("🔍   - Extracted Merchant: " + merchant);
        System.out.println("🔍   - Extracted Amount: " + amount);
        System.out.println("🔍   - Frequency: " + frequency);

        if (merchant.isEmpty() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("❌ Skipping pattern - empty merchant (" + merchant.isEmpty() + ") or zero amount (" + amount + ")");
            return;
        }

        // Find matching transaction
        List<Transaction> matchingTransactions = transactionRepository.findByMerchantAndAmountRange(
            userId, merchant, amount, new BigDecimal("50.00")
        );

        System.out.println(" Found " + matchingTransactions.size() + " matching transactions for merchant: " + merchant);

        if (!matchingTransactions.isEmpty()) {
            Transaction sourceTransaction = matchingTransactions.get(0);
            System.out.println("🔍 Source transaction ID: " + sourceTransaction.getId() + ", Bank Account ID: " +
                (sourceTransaction.getBankAccount() != null ? sourceTransaction.getBankAccount().getId() : "NULL"));

            if (sourceTransaction.getBankAccount() == null) {
                System.out.println("❌ Transaction has no bank account, skipping pattern creation");
                return;
            }

            // Check if pattern already exists for this merchant and bank account
            boolean patternExists = recurringPatternRepository.findByUserIdWithBankAccount(userId)
                .stream()
                .anyMatch(p -> p.getMerchant().equalsIgnoreCase(merchant) &&
                         p.getBankAccount() != null &&
                         p.getBankAccount().getId().equals(sourceTransaction.getBankAccount().getId()));

            if (!patternExists) {
                RecurringPattern pattern = new RecurringPattern();
                pattern.setMerchant(merchant);
                pattern.setAmount(amount);
                pattern.setFrequency(frequency);
                pattern.setReason("AI Detected");
                pattern.setNextDueDate(calculateNextDueDate(frequency));
                pattern.setTransaction(sourceTransaction);
                pattern.setBankAccount(sourceTransaction.getBankAccount());

                RecurringPattern saved = recurringPatternRepository.save(pattern);
                System.out.println("✅ Saved recurring pattern ID: " + saved.getId() + ", merchant: " + merchant +
                    ", bank account ID: " + (saved.getBankAccount() != null ? saved.getBankAccount().getId() : "NULL"));
            } else {
                System.out.println("ℹ️ Pattern already exists for merchant: " + merchant);
            }
        } else {
            System.out.println("⚠️ No matching transactions found for merchant: " + merchant + ", amount: " + amount);
        }
    }

    private LocalDate calculateNextDueDate(String frequency) {
        LocalDate now = LocalDate.now();
        return switch (frequency.toLowerCase()) {
            case "weekly" -> now.plusWeeks(1);
            case "yearly" -> now.plusYears(1);
            default -> now.plusMonths(1); // Monthly default
        };
    }

    private String extractMerchantFromDescription(String description) {
        // Extract merchant from patterns like "Internet/Fiber Bill (Air Fiber/Jio)"
        if (description.contains("(")) {
            int start = description.indexOf("(") + 1;
            int end = description.indexOf(")");
            if (end > start) {
                String merchant = description.substring(start, end);
                // Take first merchant if multiple separated by /
                return merchant.split("/")[0].trim();
            }
        }
        // Fallback: use first part before parentheses
        return description.split("\\(")[0].trim();
    }

    private BigDecimal extractAmountFromRange(String amountRange) {
        // Handle patterns like "₹1,047.84", "₹3,000 to ₹7,000", "Approx. ₹1,000"
        String cleanRange = amountRange.replaceAll("[₹,Approx.to]", "").trim();

        // Extract first number found
        String[] parts = cleanRange.split("\\s+");
        for (String part : parts) {
            try {
                return new BigDecimal(part);
            } catch (NumberFormatException e) {
                // Continue to next part
            }
        }

        // Fallback: return 0 if no valid number found
        return BigDecimal.ZERO;
    }
}