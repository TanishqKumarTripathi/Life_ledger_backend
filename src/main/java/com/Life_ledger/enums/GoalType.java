package com.Life_ledger.enums;

public enum GoalType {
    SAVING("Saving Goal"),
    BUDGET("Budget Goal"),
    EXPENSE("Expense Limit"),
    INVESTMENT("Investment Target");

    private final String displayName;

    GoalType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}