package com.Life_ledger.service;

import com.Life_ledger.exception.OCRException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType; // correct import
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class OCRService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String extractText(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(),
                    file.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    "http://localhost:8001/ocr",
                    request,
                    JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = response.getBody().get("text");
                return node != null ? node.asText() : "";
            } else {
                throw new OCRException("OCR service returned non-OK status: " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new OCRException("Failed to read uploaded file", e);
        } catch (RestClientException e) {
            throw new OCRException("Failed to call OCR service", e);
        }
    }
}
