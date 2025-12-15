package com.Life_ledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedPDFProcessor {

    private final UniversalTransactionExtractor universalExtractor;
    private final HdfcStatementParser hdfcParser;

    public Map<String, Object> processPDF(MultipartFile file, String password) {
        try {
            // Extract text from PDF
            String rawText = extractTextFromPDF(file, password);
            
            if (rawText == null || rawText.trim().isEmpty()) {
                throw new RuntimeException("No text could be extracted from the PDF");
            }

            log.info("Extracted {} characters from PDF", rawText.length());

            // Try multiple parsing strategies
            Map<String, Object> result = tryMultipleParsers(rawText);
            
            // Validate result
            if (result == null || result.get("transactions") == null) {
                throw new RuntimeException("Failed to parse any transactions from the PDF");
            }

            return result;

        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PDF: " + e.getMessage(), e);
        }
    }

    private String extractTextFromPDF(MultipartFile file, String password) throws IOException {
        try (PDDocument document = loadDocument(file, password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            // Try different extraction strategies
            String text = stripper.getText(document);
            
            // If text is too short, try without position sorting
            if (text.length() < 100) {
                stripper.setSortByPosition(false);
                text = stripper.getText(document);
            }
            
            return text;
        }
    }

    private PDDocument loadDocument(MultipartFile file, String password) throws IOException {
        try {
            if (password != null && !password.trim().isEmpty()) {
                return PDDocument.load(file.getInputStream(), password.trim());
            } else {
                return PDDocument.load(file.getInputStream());
            }
        } catch (IOException e) {
            if (e.getMessage().contains("password") || e.getMessage().contains("encrypted")) {
                throw new RuntimeException("PDF is password protected. Please provide the correct password.");
            }
            throw e;
        }
    }

    private Map<String, Object> tryMultipleParsers(String rawText) {
        Map<String, Object> bestResult = null;
        int maxTransactions = 0;

        // Strategy 1: Try HDFC parser first (it has fallback to universal now)
        try {
            Map<String, Object> hdfcResult = hdfcParser.parse(rawText, false);
            if (hdfcResult != null && hdfcResult.get("transactions") != null) {
                int txnCount = ((java.util.List<?>) hdfcResult.get("transactions")).size();
                if (txnCount > maxTransactions) {
                    maxTransactions = txnCount;
                    bestResult = hdfcResult;
                }
            }
        } catch (Exception e) {
            log.warn("HDFC parser failed: {}", e.getMessage());
        }

        // Strategy 2: Try universal extractor directly
        try {
            Map<String, Object> universalResult = universalExtractor.extractTransactions(rawText);
            if (universalResult != null && universalResult.get("transactions") != null) {
                int txnCount = ((java.util.List<?>) universalResult.get("transactions")).size();
                if (txnCount > maxTransactions) {
                    maxTransactions = txnCount;
                    bestResult = universalResult;
                }
            }
        } catch (Exception e) {
            log.warn("Universal extractor failed: {}", e.getMessage());
        }

        // Strategy 3: Try line-by-line parsing for difficult PDFs
        if (maxTransactions < 2) {
            try {
                Map<String, Object> lineResult = parseLineByLine(rawText);
                if (lineResult != null && lineResult.get("transactions") != null) {
                    int txnCount = ((java.util.List<?>) lineResult.get("transactions")).size();
                    if (txnCount > maxTransactions) {
                        bestResult = lineResult;
                    }
                }
            } catch (Exception e) {
                log.warn("Line-by-line parser failed: {}", e.getMessage());
            }
        }

        return bestResult != null ? bestResult : createEmptyResult();
    }

    private Map<String, Object> parseLineByLine(String rawText) {
        // This is a more aggressive parsing strategy for difficult PDFs
        Map<String, Object> result = new HashMap<>();
        result.put("bank", "UNKNOWN");
        result.put("accountNumber", "UNKNOWN");
        
        java.util.List<Map<String, Object>> transactions = new java.util.ArrayList<>();
        
        String[] lines = rawText.split("\\r?\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.length() < 10) continue; // Skip very short lines
            
            // Look for lines that might contain transaction data
            if (containsPotentialTransaction(line)) {
                Map<String, Object> txn = extractBasicTransaction(line);
                if (txn != null) {
                    transactions.add(txn);
                }
            }
        }
        
        result.put("transactions", transactions);
        return result;
    }

    private boolean containsPotentialTransaction(String line) {
        // Check if line contains both a date-like pattern and an amount-like pattern
        boolean hasDate = line.matches(".*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}.*") ||
                         line.matches(".*\\d{1,2}\\s+\\w{3}\\s+\\d{4}.*");
        
        boolean hasAmount = line.matches(".*\\d+\\.\\d{2}.*") ||
                           line.matches(".*\\d{1,3}(,\\d{3})*\\.\\d{2}.*");
        
        return hasDate && hasAmount;
    }

    private Map<String, Object> extractBasicTransaction(String line) {
        try {
            Map<String, Object> txn = new HashMap<>();
            
            // Extract date
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})");
            java.util.regex.Matcher dateMatcher = datePattern.matcher(line);
            if (dateMatcher.find()) {
                txn.put("date", normalizeDate(dateMatcher.group(1)));
            } else {
                return null;
            }
            
            // Extract amount
            java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("([\\d,]+\\.\\d{2})");
            java.util.regex.Matcher amountMatcher = amountPattern.matcher(line);
            if (amountMatcher.find()) {
                String amountStr = amountMatcher.group(1).replace(",", "");
                txn.put("amount", Double.parseDouble(amountStr));
            } else {
                return null;
            }
            
            // Generate reference
            txn.put("reference", "TXN_" + Math.abs(line.hashCode()));
            
            // Extract description
            String desc = line.replaceAll("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}", "")
                             .replaceAll("[\\d,]+\\.\\d{2}", "")
                             .replaceAll("\\s+", " ")
                             .trim();
            txn.put("description", desc.isEmpty() ? "Transaction" : desc);
            txn.put("type", "DEBIT");
            
            return txn;
            
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeDate(String dateStr) {
        try {
            // Simple date normalization
            if (dateStr.contains("/")) {
                String[] parts = dateStr.split("/");
                if (parts.length == 3) {
                    String day = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
                    String month = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
                    String year = parts[2].length() == 2 ? "20" + parts[2] : parts[2];
                    return year + "-" + month + "-" + day;
                }
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private Map<String, Object> createEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("bank", "UNKNOWN");
        result.put("accountNumber", "UNKNOWN");
        result.put("transactions", new java.util.ArrayList<>());
        return result;
    }
}