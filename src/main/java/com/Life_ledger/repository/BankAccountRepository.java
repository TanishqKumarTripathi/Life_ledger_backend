package com.Life_ledger.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    void deleteByUserId(Long userId);



    //List<Long> findIdsByUserId(Long userId);
    //void deleteByIdIn(List<Long> ids);
    @Query("select b.id from BankAccount b where b.user.id = :userId")
    List<Long> findIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from BankAccount b where b.id in :ids")
    void deleteByIdIn(@Param("ids") List<Long> ids);


}