package com.Life_ledger.Enum;

public enum GoalStatus {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    PAUSED("Paused"),
    CANCELLED("Cancelled"),
    OVERDUE("Overdue");

    private final String displayName;

    GoalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}