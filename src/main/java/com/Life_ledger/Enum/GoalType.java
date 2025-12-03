package com.Life_ledger.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GoalType {
    SAVING,
    BUDGET,
    SPENDING_CAP;

    @JsonCreator
    public static GoalType fromString(String value) {
        value = value.toUpperCase();

        if (value.equals("SAVINGS"))
            return SAVING;
        if (value.equals("SAVING"))
            return SAVING;
        if (value.equals("BUDGET"))
            return BUDGET;
        if (value.equals("SPENDING CAP") || value.equals("SPENDING_CAP"))
            return SPENDING_CAP;

        return GoalType.valueOf(value);
    }
}