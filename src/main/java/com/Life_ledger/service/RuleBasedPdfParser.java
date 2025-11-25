package com.Life_ledger.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RuleBasedPdfParser {

    private static final List<Pattern> UNIVERSAL_PATTERNS = List.of(
            Pattern.compile("(?i)(\\d{1,2}[-/ ]\\d{1,2}[-/ ]\\d{2,4})\\s+(.+?)\\s+(-?\\d+[.,]\\d{1,2})\\s*(Cr|Dr)?"),
            Pattern.compile("(?i)(\\d{1,2}[-/ ]\\d{1,2}[-/ ]\\d{2,4}).*?(UPI|IMPS|NEFT).*?\\s(-?\\d+[.,]\\d{1,2})"));

    public Map<String, Object> convertToMap(String rawText) {
        rawText = normalize(rawText);
        Map<String, Object> finalMap = new HashMap<>();
        var txns = new ArrayList<Map<String, Object>>();
        String[] lines = rawText.split("\n");

        for (String line : lines) {
            Map<String, Object> obj = tryParseLine(line);
            if (obj != null)
                txns.add(obj);
        }

        finalMap.put("transactions", txns);
        return finalMap;
    }

    private Map<String, Object> tryParseLine(String line) {
        for (Pattern p : UNIVERSAL_PATTERNS) {
            Matcher m = p.matcher(line);
            if (!m.find())
                continue;

            String date = m.group(1);
            String amountStr = extractAmountGroup(m);
            if (amountStr == null)
                continue;

            String description = extractDescription(m);
            amountStr = amountStr.replace(",", "");
            double amount = Double.parseDouble(amountStr);

            Map<String, Object> obj = new HashMap<>();
            obj.put("date", formatDate(date));
            obj.put("amount", amount);
            obj.put("description", description);

            // Type
            String type = (amount < 0) ? "DEBIT" : "CREDIT";
            if (m.groupCount() >= 4) {
                String crdr = m.group(4);
                if ("Cr".equalsIgnoreCase(crdr))
                    type = "CREDIT";
            }
            obj.put("type", type);

            // Reference
            obj.put("reference", extractReference(description, line));
            return obj;
        }
        return null;
    }

    private String extractDescription(Matcher m) {
        for (int i = 2; i <= m.groupCount(); i++) {
            String g = m.group(i);
            if (g != null && g.matches(".*[A-Za-z].*"))
                return g.trim();
        }
        return "Unknown";
    }

    private String extractReference(String desc, String line) {
        Pattern refPattern = Pattern.compile("(UTR|RRN|REF|CHQ)[-: ]?([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher r = refPattern.matcher(line);
        return r.find() ? r.group(2) : "";
    }

    private String extractAmountGroup(Matcher m) {
        for (int i = 1; i <= m.groupCount(); i++) {
            String g = m.group(i);
            if (g != null && g.matches("-?\\d{1,3}(,\\d{3})*(\\.\\d+)?|-?\\d+(\\.\\d+)?"))
                return g.trim();
        }
        return null;
    }

    private String formatDate(String date) {
        date = date.replace("/", "-").replace(" ", "-");
        String[] formats = { "dd-MM-yyyy", "dd-MM-yy", "MM-dd-yyyy", "yyyy-MM-dd" };
        for (String f : formats) {
            try {
                return LocalDate.parse(date, DateTimeFormatter.ofPattern(f)).toString();
            } catch (Exception ignored) {
            }
        }
        return date;
    }

    private String normalize(String raw) {
        StringBuilder sb = new StringBuilder();
        String[] lines = raw.split("\n");
        String current = "";
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^\\d{1,2}[-/ ]\\d{1,2}[-/ ]\\d{2,4}.*")) {
                if (!current.isEmpty())
                    sb.append(current).append("\n");
                current = line;
            } else
                current += " " + line;
        }
        if (!current.isEmpty())
            sb.append(current);
        return sb.toString();
    }
}
