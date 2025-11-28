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
        Map<String, Object> result = new HashMap<>();
        result.put("bank", "HDFC");
        result.put("accountNumber", "UNKNOWN");
        result.put("transactions", new ArrayList<>());

        if (rawTextOrCsv == null || rawTextOrCsv.trim().isEmpty()) {
            return result;
        }

        try {
            List<Map<String, Object>> txns = new ArrayList<>();

            if (isCsv) {
                txns = parseCsv(rawTextOrCsv);
            } else {
                List<String> mergedLines = mergeLines(rawTextOrCsv);
                for (String line : mergedLines) {
                    if (line != null && !line.trim().isEmpty()) {
                        Map<String, Object> txn = parseLine(line);
                        if (txn != null) {
                            txns.add(txn);
                        }
                    }
                }
                txns = detectTypes(txns);
            }

            String accountNumber = extractAccountNumber(rawTextOrCsv);
            
            result.put("accountNumber", accountNumber);
            result.put("transactions", txns);
            
        } catch (Exception e) {
            // Return safe default on any error
            result.put("transactions", new ArrayList<>());
        }

        return result;
    }

    public String extractAccountNumber(String text) {
        if (text == null) return "UNKNOWN";
        
        String[] patterns = {
            "Account Number\\s*:?\\s*(\\d{6,20})",
            "A/c No\\.?\\s*:?\\s*(\\d{6,20})",
            "Account No\\.?\\s*:?\\s*(\\d{6,20})"
        };
        
        for (String pattern : patterns) {
            try {
                Matcher m = Pattern.compile(pattern).matcher(text);
                if (m.find()) {
                    return m.group(1);
                }
            } catch (Exception e) {
                // Continue to next pattern
            }
        }
        return "UNKNOWN";
    }

    public List<Map<String, Object>> parseCsv(String csv) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) return transactions;
        
        try {
            String[] lines = csv.split("\\r?\\n");
            if (lines.length < 2) return transactions;

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line == null || line.trim().isEmpty()) continue;

                try {
                    List<String> cols = parseCsvLine(line.trim());
                    if (cols.size() < 7) continue;

                    Map<String, Object> txn = new HashMap<>();
                    txn.put("date", normalizeDate(cols.get(0)));
                    txn.put("valueDate", normalizeDate(cols.get(1)));
                    txn.put("description", cols.get(2) != null ? cols.get(2) : "");
                    txn.put("reference", cols.get(3) != null ? cols.get(3) : "");
                    
                    Double debit = parseAmountSafe(cols.get(4));
                    Double credit = parseAmountSafe(cols.get(5));
                    Double balance = parseAmountSafe(cols.get(6));
                    
                    txn.put("amount", debit != null ? debit : (credit != null ? credit : 0.0));
                    txn.put("balance", balance != null ? balance : 0.0);
                    txn.put("type", debit != null ? "DEBIT" : "CREDIT");

                    transactions.add(txn);
                } catch (Exception e) {
                    // Skip problematic line
                }
            }
        } catch (Exception e) {
            // Return empty list on error
        }

        return transactions;
    }

    private List<String> parseCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        if (line == null) return cols;
        
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
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
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(s.replace(",", "").trim());
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> mergeLines(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) return result;
        
        try {
            String[] lines = raw.split("\\r?\\n");
            StringBuilder current = new StringBuilder();

            for (String line : lines) {
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

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

            if (current.length() > 0) {
                result.add(current.toString().trim());
            }
        } catch (Exception e) {
            // Return empty result on error
        }

        return result;
    }

    private Map<String, Object> parseLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        
        try {
            Matcher matcher = HDFC_PATTERN.matcher(line);
            if (!matcher.find()) return null;

            Map<String, Object> map = new HashMap<>();
            map.put("date", normalizeDate(matcher.group(1)));
            map.put("description", matcher.group(2) != null ? matcher.group(2).trim() : "");
            map.put("reference", matcher.group(3) != null ? matcher.group(3) : "");
            map.put("valueDate", normalizeDate(matcher.group(4)));
            map.put("amount", parseAmount(matcher.group(5)));
            map.put("balance", parseAmount(matcher.group(6)));

            return map;
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseAmount(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        try {
            return Double.valueOf(s.replace(",", "").trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String normalizeDate(String d) {
        if (d == null || d.trim().isEmpty()) return LocalDate.now().toString();
        
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
            // Fall through to return input
        }
        return d;
    }

    private List<Map<String, Object>> detectTypes(List<Map<String, Object>> txns) {
        if (txns == null || txns.isEmpty()) return txns != null ? txns : new ArrayList<>();

        try {
            for (int i = 0; i < txns.size(); i++) {
                Map<String, Object> curr = txns.get(i);
                if (curr == null) continue;

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
        } catch (Exception e) {
            // Return original list on error
        }

        return txns;
    }
}