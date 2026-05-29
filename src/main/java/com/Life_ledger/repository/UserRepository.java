package com.Life_ledger.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // Check if a user exists with given email
    boolean existsByEmail(String email);

    // Find a user by email
    Optional<User> findByEmail(String email);
    void deleteById(Long id);

}
