package com.Life_ledger.scheduler;

import com.Life_ledger.entity.Goal;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.GoalRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.service.GoalProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoalNudgeScheduler {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final GoalProgressService goalProgressService;

    /**
     * Run daily at 9 AM server time.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void evaluateAllUsersGoals() {

        List<User> users = userRepository.findAll();

        LocalDate today = LocalDate.now();

        for (User user : users) {
            try {
                List<Goal> goals = goalRepository.findByUser_Id(user.getId());
                if (goals.isEmpty()) {
                    continue;
                }

                // find earliest start date among active goals, fallback to last 30 days
                LocalDate from = goals.stream()
                        .map(Goal::getStartDate)
                        .filter(Objects::nonNull)
                        .min(LocalDate::compareTo)
                        .orElse(today.minusDays(30));

                List<Transaction> txns = transactionRepository
                        .findByBankAccount_User_IdAndDateBetween(user.getId(), from, today);

                goalProgressService.evaluateAndCreateNudges(user, goals, txns);

            } catch (Exception e) {
                log.error("Failed evaluating nudges for user id={}: {}",
                        user.getId(), e.getMessage(), e);
            }
        }
    }
}
