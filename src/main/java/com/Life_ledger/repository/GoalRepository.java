package com.Life_ledger.repository;

import com.Life_ledger.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.Life_ledger.Enum.GoalStatus;
import com.Life_ledger.entity.Goal;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);
    void deleteByUser_Id(Long userId);
    List<Goal> findByUser_Id(Long userId);

    List<Goal> findByBankAccountId(Long bankAccountId);

    List<Goal> findByUser_IdAndStatus(Long userId, GoalStatus status);

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.status = 'ACTIVE'")
    List<Goal> findActiveGoalsByUser(Long userId);

    List<Goal> findByUserId(Long userId);
    
    void deleteByBankAccount_Id(Long bankAccountId);

    // List<Goal> findByBankAccountId(Long accountId);
    @Modifying
    @Query("delete from Goal g where g.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);


}