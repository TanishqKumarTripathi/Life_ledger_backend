package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., Monthly Budget, Vacation Savings

    private BigDecimal targetAmount;

    private BigDecimal currentAmount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
