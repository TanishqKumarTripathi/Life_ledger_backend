package com.Life_ledger.service;

import com.Life_ledger.entity.Insight;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.repository.RecurringPatternRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringPatternServiceImpl implements RecurringPatternService {

    private final RecurringPatternRepository recurringPatternRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RecurringPattern createRecurringPattern(RecurringPattern recurringPattern) {
        // Set bank account from transaction if not already set
        if (recurringPattern.getBankAccount() == null && recurringPattern.getTransaction() != null) {
            recurringPattern.setBankAccount(recurringPattern.getTransaction().getBankAccount());
        }
        return recurringPatternRepository.save(recurringPattern);
    }

    @Override
    public RecurringPattern updateRecurringPattern(Long id, RecurringPattern recurringPattern) {
        RecurringPattern existing = recurringPatternRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring Pattern not found"));

        existing.setMerchant(recurringPattern.getMerchant());
        existing.setAmount(recurringPattern.getAmount());
        existing.setFrequency(recurringPattern.getFrequency());
        existing.setNextDueDate(recurringPattern.getNextDueDate());
        existing.setTransaction(recurringPattern.getTransaction());
        
        // Update bank account from transaction
        if (recurringPattern.getTransaction() != null) {
            existing.setBankAccount(recurringPattern.getTransaction().getBankAccount());
        }

        return recurringPatternRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringPattern getRecurringPattern(Long id) {
        return recurringPatternRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring Pattern not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringPattern> getAllRecurringPatterns() {
        return recurringPatternRepository.findAllWithBankAccount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringPattern> getRecurringPatternsByUserId(Long userId) {
        return recurringPatternRepository.findByUserIdWithBankAccount(userId);
    }

    @Override
    public void deleteRecurringPattern(Long id) {
        recurringPatternRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void processInsightPatterns(Insight insight) {
        try {
            System.out.println("Processing insight patterns for user ID: " + insight.getUser().getId());
            System.out.println("AI Text length: " + (insight.getAiText() != null ? insight.getAiText().length() : 0));
            
            JsonNode rootNode = objectMapper.readTree(insight.getAiText());
            JsonNode recurringNode = rootNode.path("recurring");
            
            System.out.println(" Recurring node exists: " + !recurringNode.isMissingNode());
            System.out.println(" Recurring node is array: " + recurringNode.isArray());
            
            if (recurringNode.isArray()) {
                System.out.println(" Found " + recurringNode.size() + " recurring patterns in AI response");
                Long userId = insight.getUser().getId();
                
                for (JsonNode patternNode : recurringNode) {
                    System.out.println(" Pattern node: " + patternNode.toString());
                    createPatternFromJson(patternNode, userId);
                }
            } else {
                System.out.println(" No recurring array found in AI response");
                System.out.println(" Available keys: " + rootNode.fieldNames());
            }
        } catch (Exception e) {
            System.err.println(" Error processing recurring patterns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createPatternFromJson(JsonNode patternNode, Long userId) {
        String merchant = patternNode.path("merchant").asText();
        double amountValue = patternNode.path("totalAmount").asDouble(0.0);
        if (amountValue == 0.0) {
            amountValue = patternNode.path("amount").asDouble(0.0);
        }
        BigDecimal amount = BigDecimal.valueOf(amountValue);
        String frequency = patternNode.path("frequency").asText("Monthly");
        
        System.out.println(" Processing recurring pattern: merchant=" + merchant + ", amount=" + amount + ", frequency=" + frequency);
        
        if (merchant.isEmpty() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(" Skipping pattern - empty merchant or zero amount");
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
}