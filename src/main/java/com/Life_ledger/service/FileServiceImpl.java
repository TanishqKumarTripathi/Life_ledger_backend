package com.Life_ledger.service.impl;

import com.Life_ledger.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.text-dir}")
    private String textDir;

    @Override
    public String saveUploadedFile(MultipartFile file) {
        try {
            String fileId = "FILE-" + System.currentTimeMillis();

            Files.createDirectories(Paths.get(uploadDir));
            Path pdfPath = Paths.get(uploadDir, fileId + ".pdf");

            Files.write(pdfPath, file.getBytes());
            return fileId;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public File getPdfFile(String fileId) {
        return new File(uploadDir + "/" + fileId + ".pdf");
    }

    @Override
    public String getPdfPath(String fileId) {
        return uploadDir + "/" + fileId + ".pdf";
    }

    @Override
    public String getTextPath(String fileId) {
        return textDir + "/" + fileId + ".txt";
    }
}
