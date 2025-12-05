package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Flow.Subscriber;

import com.Life_ledger.Enum.TransactionEnum;

@Entity
// @Table(name = "transactions")
@Table(name = "transactions", uniqueConstraints = @UniqueConstraint(columnNames = { "reference", "bank_account_id" }))
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

    // @Column(unique = true)
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

    @ManyToOne
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private UserCorrection correction;

   

    public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}

public String getMerchant() {
    return merchant;
}

public void setMerchant(String merchant) {
    this.merchant = merchant;
}

public String getReference() {
    return reference;
}

public void setReference(String reference) {
    this.reference = reference;
}

public BigDecimal getAmount() {
    return amount;
}

public void setAmount(BigDecimal amount) {
    this.amount = amount;
}

public TransactionEnum getTypeTransaction() {
    return typeTransaction;
}

public void setTypeTransaction(TransactionEnum typeTransaction) {
    this.typeTransaction = typeTransaction;
}

// Optional agar String se bhi set karna ho
public void setTypeTransaction(String typeTransaction) {
    this.typeTransaction = TransactionEnum.valueOf(typeTransaction);
}

public LocalDate getDate() {
    return date;
}

public void setDate(LocalDate date) {
    this.date = date;
}

public String getNotes() {
    return notes;
}

public void setNotes(String notes) {
    this.notes = notes;
}

public boolean isRecurring() {
    return recurring;
}

public void setRecurring(boolean recurring) {
    this.recurring = recurring;
}

public boolean isAnomaly() {
    return anomaly;
}

public void setAnomaly(boolean anomaly) {
    this.anomaly = anomaly;
}

public BankAccount getBankAccount() {
    return bankAccount;
}

public void setBankAccount(BankAccount bankAccount) {
    this.bankAccount = bankAccount;
}

public FileImport getFileImport() {
    return fileImport;
}

public void setFileImport(FileImport fileImport) {
    this.fileImport = fileImport;
}

public Category getCategory() {
    return category;
}

public void setCategory(Category category) {
    this.category = category;
}

public SubCategory getSubCategory() {
    return subCategory;
}

public void setSubCategory(SubCategory subCategory) {
    this.subCategory = subCategory;
}

public UserCorrection getCorrection() {
    return correction;
}

public void setCorrection(UserCorrection correction) {
    this.correction = correction;
}

    
    
   
}


