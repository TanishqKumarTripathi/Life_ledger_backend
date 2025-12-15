package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.Life_ledger.dto.insight.InsightType;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "insight")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @OneToMany
    @JoinColumn(name = "insight_id")
    private List<Transaction> relatedTransactions;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "TEXT")
    private String aiText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode summaryJson; // summary is saved in json format

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private InsightType type; // SUMMARY

    private String period;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}