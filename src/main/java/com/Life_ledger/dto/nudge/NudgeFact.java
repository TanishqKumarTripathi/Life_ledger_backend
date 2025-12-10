package com.Life_ledger.dto.nudge;

import com.Life_ledger.Enum.NudgeSeverity;
import com.Life_ledger.Enum.NudgeType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NudgeFact {
    private Long goalId;
    private String goalName;
    private String category;
    private NudgeType type;
    private NudgeSeverity severity;
    private String rawTitle;
    private String rawMessage;
}
