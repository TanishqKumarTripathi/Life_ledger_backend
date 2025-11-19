package com.Life_ledger.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Insight {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private User user;

    @OneToMany
    private List<Transaction> relatedTransactions; // optional

    private String aiText; // AI-generated summary
    private LocalDateTime createdAt;
}
