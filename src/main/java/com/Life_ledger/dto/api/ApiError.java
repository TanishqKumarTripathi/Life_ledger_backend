package com.Life_ledger.dto.api;

import java.time.OffsetDateTime;

public class ApiError {
    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public ApiError() {
    }

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // getters / setters (or use Lombok)
    // ...
}
