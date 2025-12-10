package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.Life_ledger.Enum.CategorySource;
import com.Life_ledger.Enum.TransactionEnum;

@Entity
@Table(name = "transactions", uniqueConstraints = @UniqueConstraint(columnNames = { "fingerprint", "bank_account_id" }))
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
    private String reference;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionEnum typeTransaction;

    @Enumerated(EnumType.STRING)
    private CategorySource categorySource;

    private LocalDate date;
    private String notes;
    private boolean recurring;
    private boolean anomaly;

    @Column(name = "fingerprint", nullable = false, length = 500, unique = true)
    private String fingerprint;

    @ManyToOne
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @ManyToOne
    @JoinColumn(name = "file_import_id")
    private FileImport fileImport;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserCorrection correction;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnomalyRecord> anomalyRecords;
}
