package com.Life_ledger.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CSVReaderService {

    public List<Map<String, Object>> parseCsv(MultipartFile file) {
        List<Map<String, Object>> transactions = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
                CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> rows = csvReader.readAll();
            if (rows.isEmpty())
                return transactions;

            String[] headers = rows.get(0); // first row = header

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                Map<String, Object> txn = new HashMap<>();

                for (int j = 0; j < headers.length; j++) {
                    String header = headers[j].trim().toLowerCase();
                    String value = (j < row.length) ? row[j].trim() : "";

                    switch (header) {
                        case "date":
                            txn.put("date", normalizeDate(value));
                            break;

                        case "amount":
                            txn.put("amount", normalizeAmount(value));
                            break;

                        case "merchant":
                        case "description":
                            txn.put("description", value);
                            break;

                        case "reference":
                            txn.put("reference", value);
                            break;

                        default:
                            txn.put(header, value);
                    }
                }

                // auto generate reference if missing
                txn.putIfAbsent("reference",
                        UUID.nameUUIDFromBytes((txn.toString()).getBytes()).toString());

                // determine type
                BigDecimal amt = (BigDecimal) txn.get("amount");
                if (amt == null)
                    amt = BigDecimal.ZERO;

                txn.put("amount", amt); // ensure saved
                txn.put("type", amt.signum() < 0 ? "DEBIT" : "CREDIT");

                transactions.add(txn);
            }

        } catch (IOException | CsvException e) {
            throw new RuntimeException("Error reading CSV: " + e.getMessage(), e);
        }

        return transactions;
    }

    private String normalizeDate(String date) {
        List<String> formats = List.of("dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy");
        for (String fmt : formats) {
            try {
                return LocalDate.parse(date, DateTimeFormatter.ofPattern(fmt)).toString();
            } catch (Exception ignored) {
            }
        }
        return date; // fallback
    }

    private BigDecimal normalizeAmount(String amt) {
        if (amt == null || amt.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            amt = amt.replace(",", "")
                    .replace("₹", "")
                    .replace("Rs", "")
                    .trim();

            return new BigDecimal(amt);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

}
