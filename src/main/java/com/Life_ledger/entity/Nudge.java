package com.Life_ledger.entity;

import com.Life_ledger.Enum.NudgeSeverity;
import com.Life_ledger.Enum.NudgeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "goal_id")
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
}
