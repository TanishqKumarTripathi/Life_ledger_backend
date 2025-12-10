package com.Life_ledger.repository;

import com.Life_ledger.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUser_Id(Long userId);

    Optional<Category> findByUser_IdAndNameIgnoreCase(Long userId, String name);

    List<Category> findByBankAccountId(Long bankAccountId);

    Optional<Category> findByBankAccountIdAndNameIgnoreCase(Long bankAccountId, String name);

    boolean existsByIdAndUser_Id(Long categoryId, Long userId);

}
