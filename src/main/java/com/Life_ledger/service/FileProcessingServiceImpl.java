// package com.Life_ledger.service;

// import com.Life_ledger.dto.ai.AIExtractionRequest;
// import com.Life_ledger.dto.ai.AIExtractionResponse;
// import com.Life_ledger.entity.FileImport;
// import com.Life_ledger.entity.Transaction;
// import com.Life_ledger.entity.User;
// import com.Life_ledger.repository.FileImportRepository;
// import com.Life_ledger.repository.TransactionRepository;
// import com.Life_ledger.util.PDFParserUtil;

// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// import java.io.File;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.time.Instant;
// import java.util.List;

// @Service
// @RequiredArgsConstructor
// public class FileProcessingServiceImpl implements FileProcessingService {

// private final FileService fileService;
// private final AIService aiService;
// private final FileImportRepository fileImportRepository;
// private final TransactionRepository transactionRepository;

// @Override
// public String extractAndSavePdfText(String fileId, String password) {
// try {
// File pdfFile = fileService.getPdfFile(fileId);

// // extract text
// String extractedText = PDFParserUtil.extractText(pdfFile, password);

// // save text file
// String textPath = fileService.getTextPath(fileId);
// Files.writeString(Paths.get(textPath), extractedText);

// return textPath;

// } catch (Exception e) {
// throw new RuntimeException("Failed to extract PDF text: " + e.getMessage(),
// e);
// }
// }

// @Override
// public Long processParsedText(String fileId, Long userId) {
// try {
// String textPath = fileService.getTextPath(fileId);
// String rawText = Files.readString(Paths.get(textPath));
// AIExtractionRequest aiRequest = new AIExtractionRequest();
// aiRequest.setRawText(rawText);

// // 3. Call AI
// AIExtractionResponse aiResponse = aiService.extract(aiRequest);

// // 4. Create FileImport
// FileImport fileImport = new FileImport();
// fileImport.setFileId(fileId);
// fileImport.setFilename(fileId + ".pdf");
// fileImport.setFiletype("pdf");
// fileImport.setUploadAt(Instant.now());

// User user = new User();
// user.setId(userId);
// fileImport.setUser(user);

// fileImport = fileImportRepository.save(fileImport);

// // 5. Convert AI → Transaction entity objects
// List<Transaction> transactions = aiResponse.toEntityList(fileImport);

// // 6. Save in DB
// transactionRepository.saveAll(transactions);

// return fileImport.getId();

// } catch (Exception e) {
// throw new RuntimeException("Failed to parse extracted text: " +
// e.getMessage(), e);
// }
// }
// }
