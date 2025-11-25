package com.Life_ledger.util;

import com.Life_ledger.entity.Transaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFParserUtil {


    private static final Pattern AMOUNT = Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+\\.\\d{2}|\\d+)");
    private static final Pattern DATE = Pattern.compile("(\\d{1,2}\\s+\\w{3}\\s+\\d{4}|\\d{2}[-/]\\d{2}[-/]\\d{4})");

    private static final DateTimeFormatter[] DATE_PATTERNS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };


    public static String extractTextFromPDF(String filePath, String password) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) throw new Exception("PDF file does not exist: " + filePath);

        try (PDDocument document = (password == null || password.isBlank())
                ? PDDocument.load(file)
                : PDDocument.load(file, password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract pdf content", e);
        }
    }

    public static List<Transaction> parseTransactions(String extractedText) {
        List<Transaction> list = new ArrayList<>();
        if (extractedText == null || extractedText.isBlank()) return list;

        String[] lines = extractedText.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Transaction t = new Transaction();


            Matcher mAmt = AMOUNT.matcher(line);
            if (mAmt.find()) {
                try {
                    String amt = mAmt.group(1).replace(",", "");
                    t.setAmount(new BigDecimal(amt));
                } catch (Exception ignored) {
                }
            }


            Matcher mDate = DATE.matcher(line);
            if (mDate.find()) {
                String dateStr = mDate.group(1);
                LocalDate parsed = tryParseDate(dateStr);
                if (parsed != null) {
                    t.setDate(parsed);
                } else {

                    LocalDate fallbackDate = extractDateFallback(line);
                    t.setDate(fallbackDate);
                }
            } else {
                LocalDate fallbackDate = extractDateFallback(line);
                t.setDate(fallbackDate);
            }


            t.setMerchant(extractMerchant(line));
            t.setNotes(line);


            t.setRecurring(false);
            t.setAnomaly(false);

            if (t.getAmount() != null) {

                if (t.getDate() == null) {
                    t.setDate(LocalDate.now());
                }
                list.add(t);
            }
        }

        return list;
    }

    private static LocalDate tryParseDate(String dateStr) {
        for (DateTimeFormatter fmt : DATE_PATTERNS) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String extractMerchant(String line) {

        String merchant = line;

        merchant = merchant.replaceAll("\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?", "");

        merchant = merchant.replaceAll("\\d{1,2}\\s+\\w{3}\\s+\\d{4}", "");
        merchant = merchant.trim().replaceAll("\\s+", " ");
        String[] words = merchant.split("\\s+");
        if (words.length > 3) {
            return String.join(" ", words[0], words[1], words[2]);
        }
        
        return merchant.isEmpty() ? "Unknown Merchant" : merchant;
    }

    private static LocalDate extractDateFallback(String line) {
        Pattern fallbackPattern = Pattern.compile("(\\d{1,2})\\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\s+(\\d{4})");
        Matcher matcher = fallbackPattern.matcher(line.toUpperCase());
        if (matcher.find()) {
            try {
                String day = matcher.group(1);
                String month = matcher.group(2);
                String year = matcher.group(3);
                String dateStr = day + " " + month + " " + year;
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static String extractAccountNumber(String extracted) {
        if (extracted == null || extracted.isBlank()) return null;
        
        Pattern accountPattern = Pattern.compile("(?:Account\\s+No\\.?|A/C\\s+No\\.?|Account\\s+Number)\\s*:?\\s*(\\d{8,20})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = accountPattern.matcher(extracted);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        Pattern numberPattern = Pattern.compile("\\b(\\d{10,16})\\b");
        Matcher numberMatcher = numberPattern.matcher(extracted);
        if (numberMatcher.find()) {
            return numberMatcher.group(1);
        }
        
        return null;
    }

    public static String extractBankName(String extracted) {
        if (extracted == null || extracted.isBlank()) return null;
        
        String[] commonBanks = {"HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "BOI", "CANARA", "UNION", "INDIAN"};
        
        for (String bank : commonBanks) {
            if (extracted.toUpperCase().contains(bank)) {
                return bank + " Bank";
            }
        }
        
        Pattern bankPattern = Pattern.compile("(\\w+\\s+Bank)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = bankPattern.matcher(extracted);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "Unknown Bank";
    }
    public static String extractText(File file, String password) {
        try {
            PDDocument doc;

            if (password != null && !password.isEmpty()) {
                doc = PDDocument.load(file, password);
            } else {
                doc = PDDocument.load(file);
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            doc.close();
            return text;

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract PDF: " + e.getMessage(), e);
        }
    }
}
