package com.Life_ledger.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GoalType {
    SAVING,
    BUDGET,
    SPENDINGCAP;

    @JsonCreator
    public static GoalType fromString(String value) {
        value = value.toUpperCase();

        if (value.equals("SAVINGS"))
            return SAVING;
        if (value.equals("SAVING"))
            return SAVING;

        return GoalType.valueOf(value);
    }
}
