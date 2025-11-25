package com.Life_ledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
@Slf4j
public class UniversalBankParser {

    // ------------------------- DATE PATTERNS -------------------------
    private static final List<String> DATE_PATTERNS = List.of(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd MM yyyy",
            "dd/MM/yy", "dd-MM-yy", "dd MM yy",
            "yyyy-MM-dd");

    private static final Pattern DATE_REGEX = Pattern.compile(
            "(\\d{1,2}[-/ ]\\d{1,2}[-/ ]\\d{2,4})");

    private static final Pattern AMOUNT_REGEX = Pattern.compile(
            "(-?\\d{1,3}(,\\d{3})*(\\.\\d+)?|-?\\d+\\.\\d+|-?\\d+)");

    private static final Pattern REF_REGEX = Pattern.compile(
            "(UTR|RRN|REF|CHQ|IMPS|UPI|Txn|Transaction)[-: ]?([A-Za-z0-9]+)");

    // -----------------------------------------------------------------

    public Map<String, Object> parse(String rawText) {

        String normalized = normalize(rawText);
        String[] lines = normalized.split("\n");

        List<Map<String, Object>> txns = new ArrayList<>();
        String bank = detectBank(rawText);

        String currentLine = "";

        for (String line : lines) {
            line = line.trim();

            if (hasDate(line)) {
                // new transaction starts
                if (!currentLine.isEmpty()) {
                    addIfValid(txns, currentLine, bank);
                }
                currentLine = line;
            } else {
                // join multi-line descriptions
                currentLine += " " + line;
            }
        }

        if (!currentLine.isEmpty())
            addIfValid(txns, currentLine, bank);

        Map<String, Object> map = new HashMap<>();
        map.put("bank", bank);
        map.put("transactions", txns);

        return map;
    }

    // -------------------------- HELPERS ------------------------------

    private void addIfValid(List<Map<String, Object>> list, String line, String bank) {
        try {
            Map<String, Object> txn = parseLine(line, bank);
            if (txn != null)
                list.add(txn);
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> parseLine(String line, String bank) {

        Matcher dateM = DATE_REGEX.matcher(line);
        Matcher amountM = AMOUNT_REGEX.matcher(line);

        if (!dateM.find() || !amountM.find())
            return null;

        String date = dateM.group(1);
        String amount = amountM.group(1);

        String description = line.replace(date, "")
                .replace(amount, "").trim();

        String ref = extractReference(line);

        Map<String, Object> txn = new HashMap<>();
        txn.put("date", normalizeDate(date));
        txn.put("amount", parseAmount(amount));
        txn.put("description", description);
        txn.put("reference", ref);
        txn.put("type", (amount.startsWith("-") ? "DEBIT" : "CREDIT"));

        return txn;
    }

    private String extractReference(String line) {
        Matcher m = REF_REGEX.matcher(line);
        if (m.find())
            return m.group(2);
        return "";
    }

    private boolean hasDate(String line) {
        return DATE_REGEX.matcher(line).find();
    }

    private double parseAmount(String amt) {
        return Double.parseDouble(amt.replace(",", "").replace("CR", "").replace("DR", ""));
    }

    private String normalize(String raw) {
        return raw.replace("\r", "").replace("\t", " ").trim();
    }

    private String normalizeDate(String dateStr) {
        dateStr = dateStr.replace("/", "-").replace(" ", "-");

        for (String p : DATE_PATTERNS) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(p)).toString();
            } catch (Exception ignored) {
            }
        }

        return dateStr;
    }

    private String detectBank(String text) {
        if (text.toUpperCase().contains("HDFC"))
            return "HDFC";
        if (text.toUpperCase().contains("STATE BANK"))
            return "SBI";
        if (text.toUpperCase().contains("YES BANK"))
            return "YES BANK";
        if (text.toUpperCase().contains("ICICI"))
            return "ICICI";
        if (text.toUpperCase().contains("AXIS"))
            return "AXIS";
        return "UNKNOWN";
    }
}
