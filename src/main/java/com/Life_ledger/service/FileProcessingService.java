package com.Life_ledger.service;

public interface FileProcessingService {

    String extractAndSavePdfText(String fileId, String password);  // extract → TXT

    Long processParsedText(String fileId, Long userId);           // parse → DB
}
