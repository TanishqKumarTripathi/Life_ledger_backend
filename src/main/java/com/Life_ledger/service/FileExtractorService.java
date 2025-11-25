package com.Life_ledger.service;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class FileExtractorService {

    // TXT → return as plain text
    public String extractTxt(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    // CSV → convert rows to text lines
    public String extractCsv(MultipartFile file) throws IOException {
        Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);

        StringBuilder sb = new StringBuilder();
        for (CSVRecord rec : records) {
            sb.append(String.join(" ", rec)).append("\n");
        }
        return sb.toString();
    }

    // XLSX → NOT SUPPORTED (we removed Apache POI)
    public String extractXlsx(MultipartFile file) {
        throw new UnsupportedOperationException(
                "Excel (.xlsx) extraction is disabled. Please upload PDF, CSV, or TXT.");
    }
}
