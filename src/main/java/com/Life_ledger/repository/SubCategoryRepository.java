package com.Life_ledger.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.SubCategory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    Optional<SubCategory> findByCategory_IdAndNameIgnoreCase(Long categoryId, String name);
    @Modifying
    @Query("""
    delete from SubCategory sc
    where sc.category.id in (
        select c.id from Category c where c.user.id = :userId
    )
""")
    void deleteByUserId(@Param("userId") Long userId);

}
