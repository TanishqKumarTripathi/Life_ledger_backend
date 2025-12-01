package com.Life_ledger.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UniversalTransactionExtractor {

    // Multiple date patterns to handle various formats
    private static final Pattern[] DATE_PATTERNS = {
        Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})"),
        Pattern.compile("(\\d{1,2}\\s+\\w{3}\\s+\\d{4})"),
        Pattern.compile("(\\d{2}\\s+\\w{3}\\s+\\d{2})"),
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2})")
    };

    // Amount patterns - more flexible
    private static final Pattern[] AMOUNT_PATTERNS = {
        Pattern.compile("([\\d,]+\\.\\d{2})(?!\\d)"),
        Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?)"),
        Pattern.compile("(\\d+\\.\\d{2})"),
        Pattern.compile("(\\d+)")
    };

    // Reference number patterns
    private static final Pattern[] REFERENCE_PATTERNS = {
        Pattern.compile("(\\d{8,20})"),
        Pattern.compile("REF[:\\s]*(\\w+)"),
        Pattern.compile("TXN[:\\s]*(\\w+)"),
        Pattern.compile("([A-Z0-9]{10,})"),
    };

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("dd-MM-yy"),
        DateTimeFormatter.ofPattern("d MMM yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    public Map<String, Object> extractTransactions(String rawText) {
        Map<String, Object> result = new HashMap<>();
        result.put("bank", detectBankName(rawText));
        result.put("accountNumber", extractAccountNumber(rawText));
        result.put("transactions", parseTransactions(rawText));
        
        return result;
    }

    private List<Map<String, Object>> parseTransactions(String text) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return transactions;
        }

        // Split into lines and process each
        String[] lines = text.split("\\r?\\n");
        List<String> processedLines = preprocessLines(lines);
        
        for (String line : processedLines) {
            Map<String, Object> transaction = extractTransactionFromLine(line);
            if (transaction != null && isValidTransaction(transaction)) {
                transactions.add(transaction);
            }
        }

        // Post-process to determine transaction types
        return determineTransactionTypes(transactions);
    }

    private List<String> preprocessLines(String[] lines) {
        List<String> processed = new ArrayList<>();
        StringBuilder currentTransaction = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Check if this line starts a new transaction (contains date)
            if (containsDate(line) && currentTransaction.length() > 0) {
                processed.add(currentTransaction.toString().trim());
                currentTransaction.setLength(0);
            }
            
            if (currentTransaction.length() > 0) {
                currentTransaction.append(" ");
            }
            currentTransaction.append(line);
        }
        
        // Add the last transaction
        if (currentTransaction.length() > 0) {
            processed.add(currentTransaction.toString().trim());
        }
        
        return processed;
    }

    private boolean containsDate(String line) {
        for (Pattern pattern : DATE_PATTERNS) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> extractTransactionFromLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> transaction = new HashMap<>();
        
        // Extract date
        String date = extractDate(line);
        if (date == null) {
            return null; // Skip lines without dates
        }
        transaction.put("date", date);

        // Extract amount
        Double amount = extractAmount(line);
        if (amount == null || amount == 0.0) {
            return null; // Skip lines without valid amounts
        }
        transaction.put("amount", amount);

        // Extract reference
        String reference = extractReference(line);
        transaction.put("reference", reference != null ? reference : generateReference(line));

        // Extract description (remaining text after removing date, amount, reference)
        String description = extractDescription(line, date, amount.toString(), reference);
        transaction.put("description", description);

        // Default values
        transaction.put("type", "DEBIT"); // Will be determined later
        transaction.put("balance", 0.0);

        return transaction;
    }

    private String extractDate(String line) {
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                return normalizeDate(dateStr);
            }
        }
        return null;
    }

    private String normalizeDate(String dateStr) {
        if (dateStr == null) return null;
        
        // Try to parse and normalize the date
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.toString(); // Returns in yyyy-MM-dd format
            } catch (Exception ignored) {
            }
        }
        
        // If parsing fails, return the original string
        return dateStr;
    }

    private Double extractAmount(String line) {
        for (Pattern pattern : AMOUNT_PATTERNS) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replace(",", "");
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0) {
                        return amount;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private String extractReference(String line) {
        for (Pattern pattern : REFERENCE_PATTERNS) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private String generateReference(String line) {
        // Generate a reference based on line content hash
        return "REF_" + Math.abs(line.hashCode());
    }

    private String extractDescription(String line, String date, String amount, String reference) {
        String description = line;
        
        // Remove date
        if (date != null) {
            for (Pattern pattern : DATE_PATTERNS) {
                description = pattern.matcher(description).replaceAll("");
            }
        }
        
        // Remove amount
        if (amount != null) {
            description = description.replaceAll(amount.replace(".", "\\."), "");
            description = description.replaceAll("[\\d,]+\\.\\d{2}", "");
        }
        
        // Remove reference
        if (reference != null && !reference.startsWith("REF_")) {
            description = description.replace(reference, "");
        }
        
        // Clean up
        description = description.replaceAll("\\s+", " ").trim();
        
        return description.isEmpty() ? "Transaction" : description;
    }

    private boolean isValidTransaction(Map<String, Object> transaction) {
        return transaction != null 
            && transaction.get("date") != null 
            && transaction.get("amount") != null
            && (Double) transaction.get("amount") > 0;
    }

    private List<Map<String, Object>> determineTransactionTypes(List<Map<String, Object>> transactions) {
        // Simple heuristic: look for keywords or patterns to determine credit/debit
        for (Map<String, Object> txn : transactions) {
            String description = (String) txn.get("description");
            if (description != null) {
                String desc = description.toLowerCase();
                if (desc.contains("credit") || desc.contains("deposit") || 
                    desc.contains("salary") || desc.contains("refund")) {
                    txn.put("type", "CREDIT");
                } else {
                    txn.put("type", "DEBIT");
                }
            }
        }
        return transactions;
    }

    private String extractAccountNumber(String text) {
        if (text == null) return "UNKNOWN";
        
        Pattern[] accountPatterns = {
            Pattern.compile("Account\\s+Number\\s*:?\\s*(\\d{6,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A/c\\s+No\\.?\\s*:?\\s*(\\d{6,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Account\\s+No\\.?\\s*:?\\s*(\\d{6,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d{10,16})\\b") // Fallback for long numbers
        };
        
        for (Pattern pattern : accountPatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return "UNKNOWN";
    }

    private String detectBankName(String text) {
        if (text == null) return "UNKNOWN";
        
        String upperText = text.toUpperCase();
        String[] banks = {"HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "BOI", "CANARA"};
        
        for (String bank : banks) {
            if (upperText.contains(bank)) {
                return bank;
            }
        }
        
        return "UNKNOWN";
    }
}