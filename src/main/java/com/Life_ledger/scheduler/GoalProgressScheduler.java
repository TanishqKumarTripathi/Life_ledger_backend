package com.Life_ledger.scheduler;

import com.Life_ledger.Enum.GoalStatus;
import com.Life_ledger.entity.*;
import com.Life_ledger.service.GoalProgressService;
import com.Life_ledger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoalProgressScheduler {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final GoalProgressService goalProgressService;

    /**
     * Runs every 15 minutes
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void recalculateGoalsAndNudges() {

        log.info("🔄 GoalProgressScheduler started");

        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                List<Goal> activeGoals = goalRepository.findByUser_IdAndStatus(
                        user.getId(), GoalStatus.ACTIVE);

                if (activeGoals.isEmpty())
                    continue;

                LocalDate earliestStart = activeGoals.stream()
                        .map(Goal::getStartDate)
                        .filter(d -> d != null)
                        .min(LocalDate::compareTo)
                        .orElse(LocalDate.now());

                List<Transaction> txns = transactionRepository
                        .findByUserAndDateBetween(
                                user.getId(),
                                earliestStart,
                                LocalDate.now());

                goalProgressService.evaluateGoals(user, activeGoals, txns);

            } catch (Exception e) {
                log.error("❌ Error processing goals for user={}", user.getId(), e);
            }
        }

        log.info("✅ GoalProgressScheduler finished");
    }
}
