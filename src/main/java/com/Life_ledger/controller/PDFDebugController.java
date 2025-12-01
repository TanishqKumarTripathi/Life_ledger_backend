package com.Life_ledger.controller;

import com.Life_ledger.service.PDFReaderService;
import com.Life_ledger.service.EnhancedPDFProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf/debug")
@RequiredArgsConstructor
public class PDFDebugController {

    private final PDFReaderService pdfReaderService;
    private final EnhancedPDFProcessor enhancedPDFProcessor;

    @PostMapping("/extract-text")
    public ResponseEntity<Map<String, Object>> extractTextOnly(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password) {
        
        try {
            String rawText = pdfReaderService.extractText(file, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("textLength", rawText != null ? rawText.length() : 0);
            response.put("rawText", rawText);
            response.put("preview", rawText != null && rawText.length() > 500 ? 
                rawText.substring(0, 500) + "..." : rawText);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/parse-transactions")
    public ResponseEntity<Map<String, Object>> parseTransactionsDebug(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password) {
        
        try {
            // First extract text
            String rawText = pdfReaderService.extractText(file, password);
            
            // Then try to parse transactions
            Map<String, Object> parseResult = enhancedPDFProcessor.processPDF(file, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("textLength", rawText != null ? rawText.length() : 0);
            response.put("textPreview", rawText != null && rawText.length() > 300 ? 
                rawText.substring(0, 300) + "..." : rawText);
            response.put("parseResult", parseResult);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("stackTrace", e.getStackTrace());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}