package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String merchant;

    private BigDecimal amount;

    private String frequency; // Monthly, Weekly, Yearly

    private String reason;

    private LocalDate nextDueDate;

    @ManyToOne
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
}