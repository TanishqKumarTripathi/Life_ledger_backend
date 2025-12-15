package com.Life_ledger.dto.OCR;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrUploadResponseDto {

    private boolean success;
    private String message;

    // ✅ match PDF response
    private int transactionsImported;
    private int transactionsSkipped;

    private List<ParsedTransactionDto> added;
    private List<ParsedTransactionDto> skipped;

    private Long accountId;
}
