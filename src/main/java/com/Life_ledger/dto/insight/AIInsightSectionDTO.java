package com.Life_ledger.dto.insight;

import lombok.Data;
import java.util.List;

@Data
public class AIInsightSectionDTO {
    private String sectionName;
    private Object data;
    private String summary;
    
    public static AIInsightSectionDTO of(String sectionName, Object data, String summary) {
        AIInsightSectionDTO dto = new AIInsightSectionDTO();
        dto.setSectionName(sectionName);
        dto.setData(data);
        dto.setSummary(summary);
        return dto;
    }
}