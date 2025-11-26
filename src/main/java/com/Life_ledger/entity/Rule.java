package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // matching fields
    private String keyword; // plain keyword match (lowercased when saved)
    private String regex; // optional regex

    private Double minAmount;
    private Double maxAmount;

    private Integer priority = 100; // lower = higher priority

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // link to category/subcategory
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;
}
