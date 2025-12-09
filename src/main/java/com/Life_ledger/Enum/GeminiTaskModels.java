package com.Life_ledger.Enum;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.EnumMap;

public class GeminiTaskModels {

    public enum Step {
        CATEGORIZE, RECURRING, ANOMALIES, SUMMARY
    }

    public static class TaskStatus {

        public enum State {
            PENDING, RUNNING, DONE, ERROR
        }

        public volatile State state = State.PENDING;
        public volatile String message;
        public volatile JsonNode result;
        public volatile LocalDateTime updatedAt = LocalDateTime.now();
    }

    public static EnumMap<Step, TaskStatus> defaultStatusMap() {
        EnumMap<Step, TaskStatus> map = new EnumMap<>(Step.class);
        for (Step s : Step.values()) {
            map.put(s, new TaskStatus());
        }
        return map;
    }
}
