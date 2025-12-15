# PDF Transaction Extraction Improvements

## Problem
The original PDF processing system was failing to extract transactions from PDF bank statements due to:
- Rigid regex patterns that only worked for specific HDFC formats
- No fallback mechanisms when primary parsing failed
- Limited support for different PDF layouts and formats

## Solution
Implemented a multi-layered PDF processing system with the following components:

### 1. UniversalTransactionExtractor
- **Location**: `src/main/java/com/Life_ledger/service/UniversalTransactionExtractor.java`
- **Purpose**: Flexible transaction extraction using multiple pattern matching strategies
- **Features**:
  - Multiple date format patterns (DD/MM/YYYY, DD-MM-YYYY, DD MMM YYYY, etc.)
  - Flexible amount detection (with/without commas, different decimal formats)
  - Smart line preprocessing to merge multi-line transactions
  - Automatic transaction type detection (Credit/Debit)

### 2. EnhancedPDFProcessor
- **Location**: `src/main/java/com/Life_ledger/service/EnhancedPDFProcessor.java`
- **Purpose**: Orchestrates multiple parsing strategies and chooses the best result
- **Features**:
  - Tries HDFC parser first (now with universal fallback)
  - Falls back to universal extractor if HDFC parser finds few transactions
  - Line-by-line parsing for difficult PDFs
  - Better error handling and logging

### 3. Updated HdfcStatementParser
- **Changes**: Added UniversalTransactionExtractor as fallback
- **Behavior**: If HDFC-specific parsing finds fewer than 3 transactions, automatically tries universal extraction

### 4. Debug Tools
- **PDFDebugController**: New endpoint `/api/pdf/debug/` for troubleshooting
  - `/extract-text`: Shows raw text extraction from PDF
  - `/parse-transactions`: Shows parsing results with debug info

### 5. Configuration
- **PDFProcessingConfig**: Configurable settings for PDF processing
- **Properties**: Added configuration in `application.properties`

## Usage

### Normal Upload (Enhanced)
```
POST /api/pdf/upload
- Uses EnhancedPDFProcessor automatically
- Tries multiple parsing strategies
- Returns best result with most transactions found
```

### Debug Endpoints
```
POST /api/pdf/debug/extract-text
- Shows raw text extracted from PDF
- Useful for understanding what text is available

POST /api/pdf/debug/parse-transactions  
- Shows parsing results and debug information
- Helps identify why transactions aren't being found
```

### Configuration Options
```properties
# Enable/disable enhanced processing
pdf.processing.enhanced-processing=true

# Minimum transactions for HDFC parser success
pdf.processing.min-transactions-threshold=3

# Enable debug logging
pdf.processing.debug-logging=true

# Maximum file size (MB)
pdf.processing.max-file-size-mb=10
```

## Testing the Fix

1. **Upload a PDF** using the existing `/api/pdf/upload` endpoint
2. **Check logs** for debug information about parsing process
3. **Use debug endpoints** if transactions still aren't found:
   - First try `/api/pdf/debug/extract-text` to see if text extraction works
   - Then try `/api/pdf/debug/parse-transactions` to see parsing results

## Expected Improvements

- **Better Success Rate**: Multiple parsing strategies increase chances of finding transactions
- **More Bank Support**: Universal extractor works with various bank statement formats
- **Better Error Handling**: Clear error messages and fallback mechanisms
- **Debugging Tools**: Easy to troubleshoot when PDFs don't work
- **Configurable**: Can adjust thresholds and enable/disable features

## Backward Compatibility

- All existing endpoints continue to work
- CSV processing unchanged
- HDFC-specific parsing still works for compatible formats
- No breaking changes to API responses