package com.Life_ledger.repository;

import com.Life_ledger.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUser_Id(Long userId);

    Optional<Category> findByUser_IdAndNameIgnoreCase(Long userId, String name);

    boolean existsByIdAndUser_Id(Long categoryId, Long userId);

    Optional<Category> findByBankAccountIdAndNameIgnoreCase(Long bankAccountId, String name);
    @Modifying
    @Query("delete from Category c where c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);


    List<Category> findByBankAccountId(Long bankAccountId);

}
