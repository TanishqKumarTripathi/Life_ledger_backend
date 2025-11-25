package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    private BigDecimal amount;
    private LocalDate date;
    @Column(length = 1000)
    private String notes;
    private boolean recurring;
    private boolean anomaly;
    @Column(length = 2000)
    private String rawText;
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
