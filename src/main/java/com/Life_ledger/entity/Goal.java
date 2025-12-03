package com.Life_ledger.entity;

import com.Life_ledger.Enum.GoalStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.Life_ledger.Enum.GoalType;

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

    private String name;

    //@Column(length=500)
    //private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private String category;
    private LocalDate startDate;
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private GoalType type; // SAVING or BUDGET

    @Enumerated(EnumType.STRING)
    private GoalStatus status;

    private Double nudgeThreshold;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();


    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}


