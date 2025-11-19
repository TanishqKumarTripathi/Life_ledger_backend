package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.BankAccount;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

}
