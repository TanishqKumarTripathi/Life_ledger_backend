package com.Life_ledger.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HdfcStatementService {

    private final PDFReaderService pdfReader;
    private final FileExtractorService fileExtractor;
    private final HdfcStatementParser hdfcParser;

    public HdfcStatementService(PDFReaderService pdfReader,
            FileExtractorService fileExtractor,
            HdfcStatementParser hdfcParser) {
        this.pdfReader = pdfReader;
        this.fileExtractor = fileExtractor;
        this.hdfcParser = hdfcParser;
    }

    public Map<String, Object> parseFile(MultipartFile file) {

        String name = file.getOriginalFilename().toLowerCase();
        boolean isCsv = name.endsWith(".csv");

        try {
            String text;

            if (name.endsWith(".pdf")) {
                text = pdfReader.extractText(file);
            } else if (isCsv) {
                text = fileExtractor.extractCsv(file);
            } else {
                throw new IllegalArgumentException("Unsupported file type");
            }

            // ⬅️ FIX: now we pass (text, isCsv)
            return hdfcParser.parse(text, isCsv);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse HDFC file: " + e.getMessage());
        }
    }
}
