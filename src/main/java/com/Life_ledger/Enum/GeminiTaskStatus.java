package com.Life_ledger.Enum;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class GeminiTaskStatus {

    public enum State {
        PENDING,
        RUNNING,
        DONE,
        ERROR
    }

    private volatile State state = State.PENDING;
    private volatile String message;
    private volatile JsonNode result;
    private volatile LocalDateTime updatedAt = LocalDateTime.now();

    // getters/setters
}
