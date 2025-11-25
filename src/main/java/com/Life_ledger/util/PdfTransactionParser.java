package com.Life_ledger.util;

import com.Life_ledger.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class PdfTransactionParser {

    private static final Pattern AMOUNT = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*(?:\\.\\d{1,2})?)");
    private static final Pattern DATE = Pattern.compile("(\\d{1,2}[\\-/]\\d{1,2}[\\-/]\\d{2,4})");
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uu"),
            DateTimeFormatter.ofPattern("d/M/uu")
    };

    public static List<Transaction> parse(String extractedText) {
        List<Transaction> out = new ArrayList<>();
        if (extractedText == null || extractedText.isBlank()) return out;

        String[] lines = extractedText.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            Transaction t = new Transaction();
            t.setRawText(line);


            Matcher mAmt = AMOUNT.matcher(line.replaceAll("₹|INR|Rs\\.?",""));
            if (mAmt.find()) {
                String s = mAmt.group(1).replaceAll(",", "");
                try {
                    BigDecimal bd = new BigDecimal(s);
                    t.setAmount(bd);
                } catch (Exception ignored) {}
            }


            Matcher mDate = DATE.matcher(line);
            if (mDate.find()) {
                String d = mDate.group(1);
                LocalDate parsed = tryParseDate(d);
                t.setDate(parsed);
            }


            String merchant = extractMerchant(line);
            t.setMerchant(merchant);

            t.setNotes("");
            t.setRecurring(false);
            t.setAnomaly(false);

            out.add(t);
        }
        return out;
    }

    private static LocalDate tryParseDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                String normalized = s.replace('.', '-');
                return LocalDate.parse(normalized, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String extractMerchant(String line) {
        String r = line.replaceAll("\\d{1,2}[\\-/]\\d{1,2}[\\-/]\\d{2,4}", " ");
        r = r.replaceAll("([0-9]{1,3}(?:,[0-9]{3})*(?:\\.\\d{1,2})?)", " ");
        r = r.replaceAll("₹|INR|Rs\\.?"," ");
        r = r.replaceAll("[^A-Za-z0-9 &]", " ").trim();
        if (r.length() > 80) r = r.substring(0, 80);
        return r.isEmpty() ? null : r;
    }
}
