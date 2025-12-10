package com.Life_ledger.repository;

import com.Life_ledger.entity.Nudge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NudgeRepository extends JpaRepository<Nudge, Long> {

    List<Nudge> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Nudge> findByGoal_IdAndCreatedAtAfter(Long goalId, LocalDateTime after);

}
