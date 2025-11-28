package com.Life_ledger.dto.user;

import java.util.List;

import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.dto.category.CategoryResponse;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.dto.transaction.TransactionResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDto {
    private Long id;
    private String name;
    private String email;
    private List<AccountResponse> accounts;
    private List<CategoryResponse> categories;
    // private List<TransactionResponse> transactions;
    private List<GoalResponse> goals;
}
