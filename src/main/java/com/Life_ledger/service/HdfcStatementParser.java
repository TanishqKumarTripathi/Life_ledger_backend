package com.Life_ledger.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HdfcStatementParser {

    private static final Pattern HDFC_PATTERN = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{2,4})" + // 1. Transaction Date
                    "\\s+(.+?)\\s+" + // 2. Narration (multi-line merged)
                    "(\\d{8,16})\\s+" + // 3. Reference Number
                    "(\\d{2}/\\d{2}/\\d{2,4})\\s+" + // 4. Value Date
                    "([\\d,]*\\d\\.\\d{2})\\s+" + // 5. Amount
                    "([\\d,]*\\d\\.\\d{2})" // 6. Balance
    );

    public Map<String, Object> parse(String rawTextOrCsv, boolean isCsv) {
        List<Map<String, Object>> txns;

        if (isCsv) {
            txns = parseCsv(rawTextOrCsv);
        } else {
            List<String> mergedLines = mergeLines(rawTextOrCsv);
            txns = new ArrayList<>();

            for (String line : mergedLines) {
                Map<String, Object> txn = parseLine(line);
                if (txn != null)
                    txns.add(txn);
            }

            txns = detectTypes(txns);
        }

        String accountNumber = extractAccountNumber(rawTextOrCsv);

        Map<String, Object> result = new HashMap<>();
        result.put("bank", "HDFC");
        result.put("accountNumber", accountNumber);
        result.put("transactions", txns);

        return result;
    }

    public String extractAccountNumber(String text) {
        // Multiple patterns to catch different formats
        String[] patterns = {
            "Account Number\\s*:?\\s*(\\d{6,20})",
            "A/c No\\.?\\s*:?\\s*(\\d{6,20})",
            "Account No\\.?\\s*:?\\s*(\\d{6,20})",
            "Acc No\\.?\\s*:?\\s*(\\d{6,20})",
            "(?i)account\\s*(?:number|no\\.?)\\s*:?\\s*(\\d{6,20})"
        };
        
        for (String pattern : patterns) {
            Matcher m = Pattern.compile(pattern).matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "UNKNOWN";
    }

    public List<Map<String, Object>> parseCsv(String csv) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        String[] lines = csv.split("\\r?\\n");

        if (lines.length < 2)
            return transactions;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank())
                continue;

            List<String> cols = parseCsvLine(line);
            if (cols.size() < 7)
                continue;

            String date = normalizeDate(cols.get(0));
            String valueDate = normalizeDate(cols.get(1));
            String desc = cols.get(2);
            String ref = cols.get(3);

            Double debit = parseAmountSafe(cols.get(4));
            Double credit = parseAmountSafe(cols.get(5));
            Double balance = parseAmountSafe(cols.get(6));

            Double amount = debit != null ? debit : credit;
            String type = debit != null ? "DEBIT" : "CREDIT";

            Map<String, Object> txn = new HashMap<>();
            txn.put("date", date);
            txn.put("valueDate", valueDate);
            txn.put("description", desc);
            txn.put("reference", ref);
            txn.put("amount", amount);
            txn.put("balance", balance);
            txn.put("type", type);

            transactions.add(txn);
        }

        return transactions;
    }

    private List<String> parseCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (c == ',' && !inQuotes) {
                cols.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        cols.add(sb.toString().trim());
        return cols;
    }

    private Double parseAmountSafe(String s) {
        try {
            if (s == null || s.isBlank())
                return null;
            return Double.parseDouble(s.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> mergeLines(String raw) {
        List<String> result = new ArrayList<>();
        String[] lines = raw.split("\\r?\\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isBlank())
                continue;

            if (line.matches("^\\d{2}/\\d{2}/\\d{2,4}.*")) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                }
                current.setLength(0);
                current.append(line);
            } else {
                current.append(" ").append(line);
            }
        }

        if (current.length() > 0)
            result.add(current.toString().trim());

        return result;
    }

    private Map<String, Object> parseLine(String line) {
        Matcher matcher = HDFC_PATTERN.matcher(line);

        if (!matcher.find()) {
            return null;
        }

        String date = normalizeDate(matcher.group(1));
        String narration = matcher.group(2).trim();
        String reference = matcher.group(3);
        String valueDate = normalizeDate(matcher.group(4));

        Double amount = parseAmount(matcher.group(5));
        Double balance = parseAmount(matcher.group(6));

        Map<String, Object> map = new HashMap<>();
        map.put("date", date);
        map.put("description", narration);
        map.put("reference", reference);
        map.put("valueDate", valueDate);
        map.put("amount", amount);
        map.put("balance", balance);

        return map;
    }

    private Double parseAmount(String s) {
        return Double.valueOf(s.replace(",", "").trim());
    }

    private String normalizeDate(String d) {
        d = d.replace("/", "-").trim();
        String[] formats = { "dd-MM-yyyy", "dd-MM-yy", "MM-dd-yyyy", "yyyy-MM-dd" };
        for (String f : formats) {
            try {
                return LocalDate.parse(d, DateTimeFormatter.ofPattern(f)).toString();
            } catch (Exception ignored) {
            }
        }
        return d;
    }

    private List<Map<String, Object>> detectTypes(List<Map<String, Object>> txns) {
        if (txns.isEmpty())
            return txns;

        // Sort by date to ensure proper balance comparison
        txns.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));

        for (int i = 0; i < txns.size(); i++) {
            Map<String, Object> curr = txns.get(i);

            if (i == 0) {
                // For first transaction, check if amount increases or decreases balance
                curr.put("type", "DEBIT");
                continue;
            }

            double prevBal = (Double) txns.get(i - 1).get("balance");
            double currBal = (Double) curr.get("balance");

            if (currBal > prevBal) {
                curr.put("type", "CREDIT");
            } else {
                curr.put("type", "DEBIT");
            }
        }
        return txns;
    }
}