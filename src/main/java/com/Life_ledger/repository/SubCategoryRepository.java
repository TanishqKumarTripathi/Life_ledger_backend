package com.Life_ledger.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.SubCategory;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    Optional<SubCategory> findByCategory_IdAndNameIgnoreCase(Long categoryId, String name);
}
