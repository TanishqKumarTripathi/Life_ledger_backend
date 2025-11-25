package com.Life_ledger.Enum;

public enum AccountStatusEnum {
    MATCH, // matches selected bank account
    DIFFERENT, // belongs to another existing user account
    NEW_ACCOUNT, // not found among user's accounts
    NOT_FOUND_SELECTED
}
