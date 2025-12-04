package com.Life_ledger.controller;

import com.Life_ledger.dto.api.ApiError;
import com.Life_ledger.exception.OCRException;
import com.Life_ledger.service.OCRService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OCRController {

    private final OCRService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<?> extract(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        // ------------------------------------------------------
        // 1️⃣ JWT AUTHENTICATION VALIDATION
        // ------------------------------------------------------
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(
                            HttpStatus.UNAUTHORIZED.value(),
                            "UNAUTHORIZED",
                            "Invalid or missing JWT token",
                            "/api/ocr/extract"));
        }

        // ------------------------------------------------------
        // 2️⃣ FILE VALIDATION
        // ------------------------------------------------------
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ApiError(
                            HttpStatus.BAD_REQUEST.value(),
                            "INVALID_FILE",
                            "Please upload a valid image file.",
                            "/api/ocr/extract"));
        }

        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/png") &&
                        !contentType.equals("image/jpeg") &&
                        !contentType.equals("image/jpg"))) {

            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(new ApiError(
                            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                            "INVALID_FILE_TYPE",
                            "Only PNG and JPEG images are allowed.",
                            "/api/ocr/extract"));
        }

        // ------------------------------------------------------
        // 3️⃣ TRY OCR OPERATION
        // ------------------------------------------------------
        try {
            String text = ocrService.extractText(file);

            return ResponseEntity.ok().body(
                    new OCRResponse("Text extracted successfully", text));

        } catch (OCRException ex) {
            // let GlobalExceptionHandler convert to JSON as well
            throw ex;

        } catch (Exception ex) {
            // fallback for unexpected errors
            throw new OCRException("Unexpected error while extracting text", ex);
        }
    }

    // Internal DTO for success response
    static class OCRResponse {
        public String message;
        public String extractedText;

        public OCRResponse(String message, String extractedText) {
            this.message = message;
            this.extractedText = extractedText;
        }
    }
}
