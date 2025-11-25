package com.Life_ledger.dto.rules;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRuleDTO {
    private int index;
    private String merchant;
    private BigDecimal amount;
    private String date;
}
