package com.Life_ledger.repository;

import com.Life_ledger.entity.Nudge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NudgeRepository extends JpaRepository<Nudge, Long> {

    List<Nudge> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Nudge> findByGoal_IdAndCreatedAtAfter(Long goalId, LocalDateTime after);

    @Modifying
    @Query("""
    delete from Nudge n
    where n.goal.id in (
        select g.id from Goal g where g.user.id = :userId
    )
""")
    void deleteByUserId(@Param("userId") Long userId);


}
