package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.NormalizedTransaction;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.Enum.TransactionEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TransactionNormalizationService {

    private static final Map<Pattern, String> MERCHANT_PATTERNS = Map.of(
        Pattern.compile(".*ZOMATO.*", Pattern.CASE_INSENSITIVE), "Zomato",
        Pattern.compile(".*SWIGGY.*", Pattern.CASE_INSENSITIVE), "Swiggy",
        Pattern.compile(".*AMAZON.*", Pattern.CASE_INSENSITIVE), "Amazon",
        Pattern.compile(".*FLIPKART.*", Pattern.CASE_INSENSITIVE), "Flipkart",
        Pattern.compile(".*IRCTC.*|.*RAIL.*", Pattern.CASE_INSENSITIVE), "IRCTC",
        Pattern.compile(".*GOOGLE.*|.*GPAY.*|.*PLAYSTORE.*", Pattern.CASE_INSENSITIVE), "Google Pay",
        Pattern.compile(".*PAYTM.*", Pattern.CASE_INSENSITIVE), "Paytm",
        Pattern.compile(".*PHONEPE.*", Pattern.CASE_INSENSITIVE), "PhonePe",
        Pattern.compile(".*UBER.*", Pattern.CASE_INSENSITIVE), "Uber",
        Pattern.compile(".*OLA.*", Pattern.CASE_INSENSITIVE), "Ola"
    );

    private static final Map<String, String> CATEGORY_KEYWORDS = Map.of(
        "Food", "ZOMATO|SWIGGY|RESTAURANT|FOOD|CAFE|PIZZA|BURGER|KFC|MCDONALD",
        "Travel", "IRCTC|UBER|OLA|TAXI|FLIGHT|HOTEL|BOOKING|MAKEMYTRIP|GOIBIBO",
        "Shopping", "AMAZON|FLIPKART|MYNTRA|AJIO|SHOPPING|MALL|STORE",
        "Utilities", "ELECTRICITY|WATER|GAS|INTERNET|BROADBAND|MOBILE|RECHARGE|BILL",
        "Entertainment", "NETFLIX|PRIME|SPOTIFY|MOVIE|CINEMA|GAME|ENTERTAINMENT",
        "Healthcare", "HOSPITAL|MEDICAL|PHARMACY|DOCTOR|HEALTH|MEDICINE",
        "Education", "SCHOOL|COLLEGE|COURSE|BOOK|EDUCATION|TRAINING"
    );

    public List<NormalizedTransaction> normalizeTransactions(List<Transaction> rawTransactions) {
        Map<String, Long> merchantFrequency = calculateMerchantFrequency(rawTransactions);
        
        return rawTransactions.stream()
            .map(t -> normalizeTransaction(t, merchantFrequency))
            .collect(Collectors.toList());
    }

    private NormalizedTransaction normalizeTransaction(Transaction transaction, Map<String, Long> merchantFrequency) {
        String cleanMerchant = cleanMerchantName(transaction.getMerchant());
        boolean isExpense = determineIfExpense(transaction);
        BigDecimal signedAmount = calculateSignedAmount(transaction, isExpense);
        String category = determineCategory(transaction, cleanMerchant);
        boolean isRecurring = merchantFrequency.getOrDefault(cleanMerchant, 0L) >= 3;

        return NormalizedTransaction.builder()
            .id(transaction.getId())
            .cleanMerchant(cleanMerchant)
            .originalMerchant(transaction.getMerchant())
            .signedAmount(signedAmount)
            .category(category)
            .subCategory(transaction.getSubCategory() != null ? transaction.getSubCategory().getName() : null)
            .date(transaction.getDate())
            .isExpense(isExpense)
            .isRecurring(isRecurring)
            .reference(transaction.getReference())
            .notes(transaction.getNotes())
            .build();
    }

    private boolean determineIfExpense(Transaction transaction) {
        // Primary: Use TransactionEnum if available
        if (transaction.getTypeTransaction() != null) {
            return transaction.getTypeTransaction() == TransactionEnum.DEBIT;
        }
        
        // Fallback: Check amount sign (negative = expense)
        if (transaction.getAmount() != null) {
            return transaction.getAmount().compareTo(BigDecimal.ZERO) < 0;
        }
        
        // Last resort: Check narration/reference for keywords
        String text = (transaction.getReference() + " " + transaction.getNotes()).toLowerCase();
        return text.contains("debit") || text.contains("withdrawal") || text.contains("purchase");
    }

    private BigDecimal calculateSignedAmount(Transaction transaction, boolean isExpense) {
        BigDecimal amount = transaction.getAmount();
        if (amount == null) return BigDecimal.ZERO;
        
        // Ensure expenses are negative, income is positive
        if (isExpense && amount.compareTo(BigDecimal.ZERO) > 0) {
            return amount.negate();
        } else if (!isExpense && amount.compareTo(BigDecimal.ZERO) < 0) {
            return amount.abs();
        }
        return amount;
    }

    private String cleanMerchantName(String rawMerchant) {
        if (rawMerchant == null || rawMerchant.trim().isEmpty()) {
            return "Unknown";
        }

        // Check predefined patterns first
        for (Map.Entry<Pattern, String> entry : MERCHANT_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(rawMerchant).matches()) {
                return entry.getValue();
            }
        }

        // Clean the merchant name
        String cleaned = rawMerchant.toUpperCase().trim()
            .replaceAll("UPI[-_ ]?", "")
            .replaceAll("PAYTM|PTYS|PTYBL|PAYAXIS|HDFCBANK|YESB0PTM|YESB0YBL|UTIB|OKAXIS|ICICI|AXISBANK", "")
            .replaceAll("[^A-Z0-9 @.&]", " ")
            .replaceAll("\\s{2,}", " ")
            .trim();

        // Return first meaningful word if too long
        if (cleaned.length() > 20) {
            String[] words = cleaned.split(" ");
            return words.length > 0 ? words[0] : "Unknown";
        }

        return cleaned.isEmpty() ? "Unknown" : cleaned;
    }

    private String determineCategory(Transaction transaction, String cleanMerchant) {
        // Use existing category if available
        if (transaction.getCategory() != null && transaction.getCategory().getName() != null) {
            return transaction.getCategory().getName();
        }

        // Auto-categorize based on merchant and reference
        String searchText = (cleanMerchant + " " + 
                           (transaction.getReference() != null ? transaction.getReference() : "") + " " +
                           (transaction.getNotes() != null ? transaction.getNotes() : "")).toUpperCase();

        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (Pattern.compile(entry.getValue(), Pattern.CASE_INSENSITIVE).matcher(searchText).find()) {
                return entry.getKey();
            }
        }

        return "Uncategorized";
    }

    private Map<String, Long> calculateMerchantFrequency(List<Transaction> transactions) {
        return transactions.stream()
            .map(t -> cleanMerchantName(t.getMerchant()))
            .collect(Collectors.groupingBy(
                merchant -> merchant,
                Collectors.counting()
            ));
    }
}