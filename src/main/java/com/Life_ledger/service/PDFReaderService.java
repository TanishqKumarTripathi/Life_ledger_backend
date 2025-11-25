package com.Life_ledger.service;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PDFReaderService {

    public String extractText(MultipartFile file) {
        return extractText(file, null);
    }

    public String extractText(MultipartFile file, String password) {
        try {
            PDDocument document = (password == null || password.isEmpty()) ? PDDocument.load(file.getInputStream())
                    : PDDocument.load(file.getInputStream(), password);

            try (document) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(document);
            }

        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("PDF is password-protected.");
        } catch (IOException e) {
            throw new RuntimeException("Unable to read PDF", e);
        }
    }
}
