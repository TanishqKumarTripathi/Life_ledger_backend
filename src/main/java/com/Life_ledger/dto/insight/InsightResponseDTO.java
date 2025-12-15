package com.Life_ledger.dto.insight;

import lombok.Data;
import java.time.LocalDateTime;

import com.Life_ledger.entity.Insight;

@Data
public class InsightResponseDTO {
    private Long id;
    private String aiText;
    private LocalDateTime createdAt;

    public static InsightResponseDTO fromEntity(Insight insight) {
        InsightResponseDTO dto = new InsightResponseDTO();
        dto.setId(insight.getId());
        dto.setAiText(insight.getAiText());
        dto.setCreatedAt(insight.getCreatedAt());
        return dto;
    }
}
