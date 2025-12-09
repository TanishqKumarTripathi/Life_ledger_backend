package com.Life_ledger.util;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import com.Life_ledger.Enum.TransactionEnum;

@Component
public class TransactionFingerprintUtil {

        public String build(
                        LocalDate date,
                        BigDecimal amount,
                        TransactionEnum type,
                        String description,
                        Long accountId) {

                String normalizedDesc = normalize(description);

                String normalizedAmount = amount
                                .stripTrailingZeros()
                                .toPlainString();

                String raw = String.join("|",
                                date.toString(),
                                normalizedAmount,
                                type.name(),
                                normalizedDesc,
                                accountId.toString());

                // ✅ Return HASH, not raw text
                return DigestUtils.sha256Hex(raw);
        }

        private String normalize(String text) {
                if (text == null)
                        return "";

                return text
                                .toUpperCase()
                                .replaceAll("[^A-Z0-9@]", "") // ✅ kills OCR noise: dots, dashes, slashes
                                .trim();
        }
}
