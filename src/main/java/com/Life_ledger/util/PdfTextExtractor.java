package com.Life_ledger.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class PdfTextExtractor {
    public String extract(MultipartFile file) {
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // improve table extraction

            return stripper.getText(doc);

        } catch (IOException e) {
            throw new RuntimeException("Unable to extract text from PDF: " + e.getMessage(), e);
        }
    }
}