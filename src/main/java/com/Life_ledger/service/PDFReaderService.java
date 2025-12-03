package com.Life_ledger.service;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PDFReaderService {

    public String extractText(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {

            if (document.isEncrypted()) {
                throw new IllegalArgumentException("PDF is password protected");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);

        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("PDF is password protected");
        }
    }
}