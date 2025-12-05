package com.Life_ledger.controller;



import com.Life_ledger.service.OpenAIService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIControllerTestEndpoint {
    
    private final OpenAIService openAIService;

    @GetMapping("/test")
    public ResponseEntity<String> testOpenAI() {
        String reply = openAIService.testModel();
        return ResponseEntity.ok(reply);
    }
}
