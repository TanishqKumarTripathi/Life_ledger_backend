package com.Life_ledger.entity;

import com.Life_ledger.Enum.NudgeSeverity;
import com.Life_ledger.Enum.NudgeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "nudges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nudge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NudgeType type;

    @Enumerated(EnumType.STRING)
    private NudgeSeverity severity;

    private String category; // optional: goal category context

    private LocalDateTime createdAt;

    private boolean read;

    @ManyToOne
    @JoinColumn(name = "goal_id", foreignKey = @ForeignKey(name = "fk_nudge_goal"))
    @OnDelete(action = OnDeleteAction.CASCADE) // <-- important
    private Goal goal;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Column(precision = 19, scale = 2)
    private BigDecimal spendAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal exceededAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal progressPercent;

}
