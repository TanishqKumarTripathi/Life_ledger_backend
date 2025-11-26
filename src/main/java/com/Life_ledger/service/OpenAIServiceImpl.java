package com.Life_ledger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// import com.Life_ledger.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OpenAIServiceImpl implements OpenAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // This tries to read OPENAI_API_KEY env var first, then property openai.api.key
    @Value("${OPENAI_API_KEY:${openai.api.key:}}")
    private String apiKey;

    private final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String testModel() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("OpenAI API key not configured. Set OPENAI_API_KEY or openai.api.key");
        }

        try {
            // Build chat-completion request (simple)
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "gpt-3.5-turbo"); // or "gpt-4.1" / "gpt-4o" depending on your access
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", "You are a helpful assistant."));
            messages.add(Map.of("role", "user", "content",
                    "Hello from LifeLedger backend. Reply with a short confirmation."));

            payload.put("messages", messages);
            payload.put("max_tokens", 60);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(CHAT_COMPLETION_URL, request, String.class);

            if (resp.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException(
                        "OpenAI returned status: " + resp.getStatusCode() + " body: " + resp.getBody());
            }

            // Parse the response JSON and return the assistant's first message content
            JsonNode root = objectMapper.readTree(resp.getBody());
            // Chat completions response: choices[0].message.content
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).path("message").path("content");
                return msg.asText();
            }

            return "No message returned from model";

        } catch (Exception e) {
            throw new RuntimeException("OpenAI test call failed: " + e.getMessage(), e);
        }
    }
}
