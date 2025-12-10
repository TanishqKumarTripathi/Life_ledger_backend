package com.Life_ledger.controller;

import com.Life_ledger.GeminiPrompt.GeminiPrompts;
import com.Life_ledger.dto.OCR.OcrUploadResponseDto;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.service.GeminiVisionService;
import com.Life_ledger.service.ocr.OcrService;

import lombok.RequiredArgsConstructor;

import java.util.Base64;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ocr")
public class OCRController {

        private final GeminiVisionService geminiVisionService;
        private final OcrService ocrService;
        private final UserRepository userRepository;

        @PostMapping("/upload")
        public OcrUploadResponseDto upload(
                        @RequestParam("image") MultipartFile image,
                        @RequestParam("bankAccountId") Long bankAccountId,
                        Authentication authentication) throws Exception {

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String base64Image = Base64.getEncoder().encodeToString(image.getBytes());

                String geminiResponse = geminiVisionService.analyzeImage(
                                GeminiPrompts.TRANSACTION_OCR, // your prompt constant
                                base64Image);

                // geminiResponse should be strict JSON string
                return ocrService.processGeminiText(
                                geminiResponse,
                                bankAccountId,
                                user);
        }
}
