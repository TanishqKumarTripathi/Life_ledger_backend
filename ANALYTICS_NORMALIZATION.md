# Analytics Normalization System

## Overview

This system implements in-memory transaction normalization for accurate analytics without modifying the database or PDF ingestion flow. Raw transactions are converted into standardized `NormalizedTransaction` objects before any analytics calculations.

## Key Components

### 1. NormalizedTransaction DTO
- **signedAmount**: Negative for expenses, positive for income
- **cleanMerchant**: Standardized merchant names (e.g., "ZOMATO UPI" → "Zomato")
- **category**: Auto-categorized or existing category
- **isExpense**: Boolean flag for expense detection
- **isRecurring**: Detected based on merchant frequency (≥3 occurrences)

### 2. TransactionNormalizationService
Handles the core normalization logic:

#### Expense Detection
1. **Primary**: Uses `TransactionEnum.DEBIT/CREDIT` if available
2. **Fallback**: Checks amount sign (negative = expense)
3. **Last resort**: Searches narration for keywords ("debit", "withdrawal", "purchase")

#### Merchant Cleaning
- Removes bank-specific prefixes (UPI, PAYTM, HDFCBANK, etc.)
- Maps common patterns to clean names
- Handles special characters and formatting

#### Auto-Categorization
Maps transactions to categories based on merchant and reference text:
- **Food**: Zomato, Swiggy, restaurants
- **Travel**: IRCTC, Uber, Ola, flights
- **Shopping**: Amazon, Flipkart, malls
- **Utilities**: Bills, recharges, internet
- **Entertainment**: Netflix, movies, games
- **Healthcare**: Hospitals, pharmacies
- **Education**: Schools, courses, books

#### Recurring Detection
- Calculates merchant frequency across all transactions
- Marks transactions as recurring if merchant appears ≥3 times

### 3. Updated AnalyticsServiceImpl
All analytics methods now:
1. Fetch raw transactions from database
2. Normalize using `TransactionNormalizationService`
3. Perform calculations on normalized data

## Benefits

### ✅ Accurate Analytics
- Proper expense/income classification
- Consistent merchant grouping
- Automatic categorization
- Reliable recurring detection

### ✅ No Database Changes
- Raw transactions remain unchanged
- PDF ingestion flow unmodified
- Backward compatibility maintained

### ✅ Flexible & Extensible
- Easy to add new merchant patterns
- Simple category rule updates
- Configurable recurring thresholds

## Usage Examples

### Test Normalization
```bash
GET /api/analytics/normalized/preview?limit=10
Authorization: Bearer <token>
```

### Get Analytics (Now Normalized)
```bash
GET /api/analytics/dashboard?accountId=123
Authorization: Bearer <token>
```

## Configuration

### Adding New Merchant Patterns
```java
// In TransactionNormalizationService
private static final Map<Pattern, String> MERCHANT_PATTERNS = Map.of(
    Pattern.compile(".*MYNEWMERCHANT.*", Pattern.CASE_INSENSITIVE), "My New Merchant"
);
```

### Adding New Categories
```java
private static final Map<String, String> CATEGORY_KEYWORDS = Map.of(
    "NewCategory", "KEYWORD1|KEYWORD2|KEYWORD3"
);
```

### Adjusting Recurring Threshold
```java
// Change from 3 to desired threshold
boolean isRecurring = merchantFrequency.getOrDefault(cleanMerchant, 0L) >= 3;
```

## Analytics Features Now Working

- ✅ **Monthly Spending**: Accurate expense tracking
- ✅ **Category Breakdown**: Auto-categorized transactions
- ✅ **Top Merchants**: Clean, grouped merchant names
- ✅ **Recurring vs One-time**: Frequency-based detection
- ✅ **Burn Rate**: Last 30 days expense calculation
- ✅ **Year-over-Year**: Proper expense comparison
- ✅ **Averages**: Daily/weekly/monthly spending

## Performance Notes

- Normalization happens in-memory per request
- No database writes during analytics
- Merchant frequency calculated once per normalization batch
- Suitable for typical transaction volumes (<10k per user)

## Future Enhancements

1. **Caching**: Cache normalized results for frequently accessed data
2. **ML Categories**: Use machine learning for better categorization
3. **User Rules**: Allow users to define custom merchant/category mappings
4. **Batch Processing**: Pre-normalize transactions for large datasets