// HdfcStatementParser (original restored)
package com.Life_ledger.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HdfcStatementParser {

    private static final Pattern HDFC_PATTERN = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{2,4})" +
                    "\\s+(.+?)\\s+" +
                    "(\\d{8,16})\\s+" +
                    "(\\d{2}/\\d{2}/\\d{2,4})\\s+" +
                    "([\\d,]*\\d\\.\\d{2})\\s+" +
                    "([\\d,]*\\d\\.\\d{2})");

    public Map<String, Object> parse(String rawTextOrCsv, boolean isCsv) {
        Map<String, Object> result = new HashMap<>();
        result.put("bank", "HDFC");
        result.put("accountNumber", "UNKNOWN");
        result.put("transactions", new ArrayList<>());
        result.put("parseErrors", new ArrayList<>()); // helpful for debugging

        if (rawTextOrCsv == null || rawTextOrCsv.trim().isEmpty()) {
            return result;
        }

        try {
            List<Map<String, Object>> txns;
            List<String> errors = new ArrayList<>();

            if (isCsv) {
                txns = parseCsv(rawTextOrCsv, errors);
            } else {
                txns = parsePdf(rawTextOrCsv, errors);
            }

            String accountNumber = extractAccountNumber(rawTextOrCsv);
            result.put("accountNumber", accountNumber);
            result.put("transactions", txns);
            result.put("parseErrors", errors);

        } catch (Exception e) {
            // keep safe defaults and return error info
            result.put("transactions", new ArrayList<>());
            ((List<String>) result.get("parseErrors")).add("Unhandled parser exception: " + e.getMessage());
        }

        return result;
    }

    private List<Map<String, Object>> parseCsv(String csv, List<String> errors) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty())
            return transactions;

        String[] lines = csv.split("\\r?\\n");
        if (lines.length == 0)
            return transactions;

        // find header index (first non-empty line)
        int headerIdx = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null && !lines[i].trim().isEmpty()) {
                headerIdx = i;
                break;
            }
        }
        if (headerIdx == -1 || headerIdx >= lines.length) {
            errors.add("No header found in CSV");
            return transactions;
        }

        String headerLine = lines[headerIdx];
        List<String> headerCols = splitCsvLine(headerLine);
        Map<String, Integer> colIndex = detectColumns(headerCols);

        if (colIndex.isEmpty()) {
            colIndex.put("date", 0);
            colIndex.put("description", 1);
            colIndex.put("reference", 2);
            colIndex.put("valueDate", 3);
            colIndex.put("withdrawal", 4);
            colIndex.put("deposit", 5);
            colIndex.put("balance", 6);
        }

        for (int i = headerIdx + 1; i < lines.length; i++) {
            String raw = lines[i];
            if (raw == null || raw.trim().isEmpty())
                continue;

            List<String> cols = splitCsvLine(raw);
            while (cols.size() < Collections.max(colIndex.values()) + 1)
                cols.add("");

            try {
                String rawDate = safeGet(cols, colIndex.getOrDefault("date", 0));
                String desc = safeGet(cols, colIndex.getOrDefault("description", 1));
                String reference = safeGet(cols, colIndex.getOrDefault("reference", 2));
                String rawValueDate = safeGet(cols, colIndex.getOrDefault("valueDate", 3));
                String withdrawal = safeGet(cols, colIndex.getOrDefault("withdrawal", 4));
                String deposit = safeGet(cols, colIndex.getOrDefault("deposit", 5));
                String balance = safeGet(cols, colIndex.getOrDefault("balance", 6));

                withdrawal = cleanField(withdrawal);
                deposit = cleanField(deposit);
                balance = cleanField(balance);

                Double amount = 0.0;
                String type = "DEBIT";

                if (!withdrawal.isBlank()) {
                    amount = parseAmountFlexible(withdrawal);
                    type = "DEBIT";
                } else if (!deposit.isBlank()) {
                    amount = parseAmountFlexible(deposit);
                    type = "CREDIT";
                } else {
                    amount = findAmountInRow(cols);
                    if (amount != null && amount != 0.0) {
                        type = "DEBIT";
                    } else {
                        errors.add("Row " + (i + 1) + " skipped: no amount found -> " + raw);
                        continue;
                    }
                }

                String dateNormalized = normalizeCsvDateFlexible(rawDate);
                String valueDateNormalized = normalizeCsvDateFlexible(rawValueDate);

                Map<String, Object> txn = new HashMap<>();
                txn.put("date", dateNormalized);
                txn.put("valueDate", valueDateNormalized);
                txn.put("description", desc != null ? desc.trim() : "");
                txn.put("reference", reference != null ? reference.trim() : "");
                txn.put("amount", amount);
                txn.put("type", type);
                txn.put("balance", parseAmountFlexible(balance));

                transactions.add(txn);

            } catch (Exception ex) {
                String msg = "Row " + (i + 1) + " parse error: " + ex.getMessage();
                errors.add(msg);
                System.out.println(msg);
            }
        }

        return transactions;
    }

    private List<Map<String, Object>> parsePdf(String raw, List<String> errors) {
        List<Map<String, Object>> txns = new ArrayList<>();
        if (raw == null || raw.isBlank())
            return txns;
        List<String> mergedLines = mergeLines(raw);
        for (String line : mergedLines) {
            try {
                Map<String, Object> m = parseLine(line);
                if (m != null)
                    txns.add(m);
            } catch (Exception e) {
                errors.add("PDF line parse fail: " + e.getMessage());
            }
        }
        return detectTypes(txns);
    }

    private Map<String, Integer> detectColumns(List<String> headerCols) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerCols.size(); i++) {
            String h = headerCols.get(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").trim();
            if (h.contains("date") && !map.containsKey("date"))
                map.put("date", i);
            if ((h.contains("txn") || h.contains("details") || h.contains("description") || h.contains("narration"))
                    && !map.containsKey("description"))
                map.put("description", i);
            if ((h.contains("chq") || h.contains("ref") || h.contains("reference")) && !map.containsKey("reference"))
                map.put("reference", i);
            if ((h.contains("value") && h.contains("date")) && !map.containsKey("valueDate"))
                map.put("valueDate", i);
            if ((h.contains("withdraw") || h.contains("debit")) && !map.containsKey("withdrawal"))
                map.put("withdrawal", i);
            if ((h.contains("deposit") || h.contains("credit")) && !map.containsKey("deposit"))
                map.put("deposit", i);
            if ((h.contains("balance") || h.contains("closing")) && !map.containsKey("balance"))
                map.put("balance", i);
        }
        return map;
    }

    private List<String> splitCsvLine(String line) {
        return splitCsvLineInternal(line);
    }

    private List<String> splitCsvLineInternal(String line) {
        List<String> cols = new ArrayList<>();
        if (line == null)
            return cols;
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int idx = 0; idx < line.length(); idx++) {
            char c = line.charAt(idx);
            if (c == '"') {
                if (inQuotes && idx + 1 < line.length() && line.charAt(idx + 1) == '"') {
                    sb.append('"');
                    idx++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        cols.add(sb.toString());
        return cols;
    }

    private String safeGet(List<String> cols, int idx) {
        if (cols == null)
            return "";
        if (idx < 0 || idx >= cols.size())
            return "";
        return cols.get(idx);
    }

    private String cleanField(String s) {
        if (s == null)
            return "";
        String out = s.trim();
        if (out.startsWith("\"") && out.endsWith("\"") && out.length() >= 2) {
            out = out.substring(1, out.length() - 1).trim();
        }
        return out;
    }

    private Double parseAmountFlexible(String s) {
        if (s == null)
            return 0.0;
        String t = s.trim();
        if (t.isEmpty())
            return 0.0;

        t = t.replaceAll("[^0-9().-]", "");

        boolean negative = false;
        if (t.startsWith("(") && t.endsWith(")")) {
            negative = true;
            t = t.substring(1, t.length() - 1);
        }
        t = t.replace(",", "").trim();
        if (t.isEmpty())
            return 0.0;

        try {
            double val = Double.parseDouble(t);
            return negative ? -Math.abs(val) : val;
        } catch (NumberFormatException ex) {
            Matcher m = Pattern.compile("-?\\d+[\\d,]*\\.?\\d*").matcher(t);
            if (m.find()) {
                String num = m.group().replace(",", "");
                try {
                    return Double.parseDouble(num);
                } catch (Exception ignored) {
                }
            }
        }
        return 0.0;
    }

    private Double findAmountInRow(List<String> cols) {
        for (String c : cols) {
            Double v = tryParseNumeric(c);
            if (v != null && Math.abs(v) > 0.0001)
                return v;
        }
        return 0.0;
    }

    private Double tryParseNumeric(String s) {
        if (s == null)
            return null;
        String t = s.replaceAll("[^0-9().-]", "");
        if (t.isEmpty())
            return null;
        try {
            return Double.parseDouble(t.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeCsvDateFlexible(String d) {
        if (d == null || d.isBlank())
            return LocalDate.now().toString();
        d = d.trim();

        List<String> patterns = Arrays.asList(
                "dd MMM yyyy", "d MMM yyyy",
                "dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy",
                "yyyy-MM-dd", "MM-dd-yyyy");
        for (String pat : patterns) {
            try {
                DateTimeFormatter df = DateTimeFormatter.ofPattern(pat, Locale.ENGLISH);
                LocalDate ld = LocalDate.parse(d, df);
                return ld.toString();
            } catch (DateTimeParseException ignored) {
            }
        }
        return normalizeDate(d);
    }

    private String normalizeDate(String d) {
        if (d == null || d.trim().isEmpty())
            return LocalDate.now().toString();

        try {
            d = d.replace("/", "-").trim();
            String[] formats = { "dd-MM-yyyy", "dd-MM-yy", "MM-dd-yyyy", "yyyy-MM-dd" };
            for (String f : formats) {
                try {
                    return LocalDate.parse(d, DateTimeFormatter.ofPattern(f)).toString();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            return e.getMessage();
        }
        return d;
    }

    private List<String> mergeLines(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null)
            return result;

        String[] lines = raw.split("\\r?\\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line == null)
                continue;
            line = line.trim();
            if (line.isEmpty())
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
        if (line == null || line.trim().isEmpty())
            return null;
        try {
            Matcher matcher = HDFC_PATTERN.matcher(line);
            if (!matcher.find())
                return null;
            Map<String, Object> map = new HashMap<>();
            map.put("date", normalizeDate(matcher.group(1)));
            map.put("description", matcher.group(2) != null ? matcher.group(2).trim() : "");
            map.put("reference", matcher.group(3) != null ? matcher.group(3) : "");
            map.put("valueDate", normalizeDate(matcher.group(4)));
            map.put("amount", parseAmountFlexible(matcher.group(5)));
            map.put("balance", parseAmountFlexible(matcher.group(6)));
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> detectTypes(List<Map<String, Object>> txns) {
        if (txns == null || txns.isEmpty())
            return txns != null ? txns : new ArrayList<>();
        for (int i = 0; i < txns.size(); i++) {
            Map<String, Object> curr = txns.get(i);
            if (curr == null)
                continue;
            if (curr.get("type") != null)
                continue; // already set by CSV parser
            if (i == 0) {
                curr.put("type", "DEBIT");
                continue;
            }
            try {
                Object prevBalObj = txns.get(i - 1).get("balance");
                Object currBalObj = curr.get("balance");
                if (prevBalObj == null || currBalObj == null) {
                    curr.put("type", "DEBIT");
                    continue;
                }
                double prevBal = ((Number) prevBalObj).doubleValue();
                double currBal = ((Number) currBalObj).doubleValue();
                curr.put("type", currBal > prevBal ? "CREDIT" : "DEBIT");
            } catch (Exception e) {
                curr.put("type", "DEBIT");
            }
        }
        return txns;
    }

    public String extractAccountNumber(String text) {
        if (text == null)
            return "UNKNOWN";
        String[] patterns = {
                "Account Number\\s*:?\\s*(\\d{6,20})",
                "A/c No\\.?\\s*:?\\s*(\\d{6,20})",
                "Account No\\.?\\s*:?\\s*(\\d{6,20})",
                "Account\\s*:\\s*(\\d{6,20})"
        };
        for (String pattern : patterns) {
            try {
                Matcher m = Pattern.compile(pattern).matcher(text);
                if (m.find())
                    return m.group(1);
            } catch (Exception ignored) {
            }
        }
        return "UNKNOWN";
    }
}
