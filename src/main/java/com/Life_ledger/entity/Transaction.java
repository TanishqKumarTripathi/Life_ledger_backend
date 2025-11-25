package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.Life_ledger.Enum.TransactionEnum;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String merchant;

    @Column(unique = true)
    private String reference;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionEnum typeTransaction;

    private LocalDate date;

    private String notes;

    private boolean recurring;

    private boolean anomaly;

    @ManyToOne
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @ManyToOne
    @JoinColumn(name = "file_import_id")
    private FileImport fileImport;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private UserCorrection correction;
}
