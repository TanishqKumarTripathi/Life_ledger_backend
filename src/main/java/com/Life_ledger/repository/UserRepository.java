package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
