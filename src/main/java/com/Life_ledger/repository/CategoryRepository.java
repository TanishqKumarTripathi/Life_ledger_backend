package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
