package com.Life_ledger.dto.insight;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InsightSummaryResponse {
    private Long id;
    private SummaryData summary;
    private List<NudgeData> nudges;
    private LocalDateTime createdAt;
    private String period;
    
    @Data
    public static class SummaryData {
        private String text;
        private String tone;
        private String emoji;
        private String color;
    }
    
    @Data
    public static class NudgeData {
        private String type;
        private String text;
        private String tone;
        private String emoji;
    }
}
