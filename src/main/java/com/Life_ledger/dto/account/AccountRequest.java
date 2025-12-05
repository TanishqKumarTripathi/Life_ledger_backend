package com.Life_ledger.dto.account;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequest {

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{9,18}", message = "Invalid account number")
    private String accountNumber; // plaintext from user

    private Long userId;

    public void setId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return userId;
    }

    public String getAccountName() {
    return accountName;
}

public void setAccountName(String accountName) {
    this.accountName = accountName;
}

public String getBankName() {
    return bankName;
}

public void setBankName(String bankName) {
    this.bankName = bankName;
}

public String getAccountNumber() {
    return accountNumber;
}

public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
}



}
