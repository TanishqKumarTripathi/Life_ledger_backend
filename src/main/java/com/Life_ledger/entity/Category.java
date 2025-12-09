package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

import com.Life_ledger.Enum.CategorySource;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private CategorySource categorySource;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<SubCategory> subCategories;

    @OneToMany(mappedBy = "category")
    private List<Transaction> transactions;

    public Category(String name) {
        this.name = name;
    }
}
