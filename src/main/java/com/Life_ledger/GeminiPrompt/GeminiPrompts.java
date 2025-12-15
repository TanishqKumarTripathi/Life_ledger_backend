package com.Life_ledger.GeminiPrompt;

public class GeminiPrompts {

  public static final String TRANSACTION_OCR = """
                        You are an OCR engine.

      Respond ONLY with valid JSON.
      DO NOT include explanation text.
      DO NOT use trailing commas.
      DO NOT use comments.
      ALL keys and enum values MUST be quoted strings.

      Expected format EXACTLY:

      {
        "transactions": [
          {
            "date": "yyyy-MM-dd",
            "description": "string",
            "amount": 123.45,
            "type": "DEBIT"
          }
        ]
      }

      If no transactions are found, return:
      {
        "transactions": []
      }
      """;
}
