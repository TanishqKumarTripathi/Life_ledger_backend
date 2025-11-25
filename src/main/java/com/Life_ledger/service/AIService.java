package com.Life_ledger.service;

import com.Life_ledger.dto.ai.AIExtractionRequest;
import com.Life_ledger.dto.ai.*;


public interface AIService {
    AIExtractionResponse extract(AIExtractionRequest request);
}
