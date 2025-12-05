package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sub_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., Restaurants, FastFood, Airlines

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;




}
