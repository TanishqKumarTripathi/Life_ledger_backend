package com.Life_ledger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pdf.processing")
@Data
public class PDFProcessingConfig {
    
    /**
     * Enable enhanced PDF processing with multiple parsing strategies
     */
    private boolean enhancedProcessing = true;
    
    /**
     * Minimum number of transactions required for HDFC parser to be considered successful
     */
    private int minTransactionsThreshold = 3;
    
    /**
     * Enable debug logging for PDF processing
     */
    private boolean debugLogging = false;
    
    /**
     * Maximum file size for PDF processing (in MB)
     */
    private int maxFileSizeMB = 10;
}