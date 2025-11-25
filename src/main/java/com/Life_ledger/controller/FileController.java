package com.Life_ledger.controller;
import com.Life_ledger.dto.upload.FileUploadResponse;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.service.FileProcessingService;
import com.Life_ledger.service.FileService;
import com.Life_ledger.service.RuleExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileProcessingService processingService;
    private final RuleExecutorService ruleExecutor;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        String fileId = fileService.saveUploadedFile(file);
        return ResponseEntity.ok(new FileUploadResponse(fileId, "File uploaded"));
    }

    @PostMapping("/upload/{fileId}/extract")
    public ResponseEntity<?> extract(
            @PathVariable String fileId,
            @RequestParam(required = false) String password
    ) {
        String textPath = processingService.extractAndSavePdfText(fileId, password);
        return ResponseEntity.ok(Map.of(
                "fileId", fileId,
                "textFilePath", textPath,
                "message", "Text extraction complete"
        ));
    }

    @PostMapping("/upload/{fileId}/parse")
    public ResponseEntity<?> parse(
            @PathVariable String fileId,
            @RequestParam Long userId
    ) {
        Long fileImportId = processingService.processParsedText(fileId, userId);
        return ResponseEntity.ok(Map.of(
                "fileId", fileId,
                "fileImportId", fileImportId,
                "message", "Parsing and DB save complete"
        ));
    }

    @PostMapping("/upload/{fileId}/rules")
    public ResponseEntity<?> applyRules(@PathVariable Long fileId) {
        List<Transaction> fileImportId = ruleExecutor.applyRulesForFileImport(fileId);
        return ResponseEntity.ok(Map.of(
                "fileId", fileId,
                "fileImportId", fileImportId,
                "message", "Rules applied"
        ));
    }
}
