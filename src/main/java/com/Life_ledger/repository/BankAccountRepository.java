package com.Life_ledger.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    // Find all accounts of a particular user
    List<BankAccount> findByUser(User user);

    // BankAccountRepository.java
    List<BankAccount> findByUserId(Long userId);

    // Find account by id and user
    // Optional<BankAccount> findByIdAndUser(Long id, Long user);

    Optional<BankAccount> findByIdAndUser(Long id, User user);

    Optional<BankAccount> findFirstByEncryptedAccountNumberAndUserId(String encryptedAccountNumber, Long userId);

    Optional<BankAccount> findByUserEmail(String email);

    List<BankAccount> findAllById(Long userId);

    List<BankAccount> findAllByUserId(Long userId);
}