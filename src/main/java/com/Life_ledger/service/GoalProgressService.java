package com.Life_ledger.service;

import com.Life_ledger.Enum.GoalStatus;
import com.Life_ledger.dto.nudge.NudgeDto;
import com.Life_ledger.entity.*;
import com.Life_ledger.mapper.NudgeMapper;
import com.Life_ledger.repository.GoalRepository;
import com.Life_ledger.repository.NudgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalProgressService {

    private final NudgeRepository nudgeRepository;
    private final NudgeMapper nudgeMapper;
    private final GoalRepository goalRepository;

    /**
     * Recalculate goals and generate simple nudges
     */
    @Transactional
    public List<NudgeDto> evaluateGoals(
            User user,
            List<Goal> goals,
            List<Transaction> transactions) {

        if (goals == null || goals.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<Nudge> nudges = new ArrayList<>();

        for (Goal goal : goals) {

            if (goal.getStatus() != GoalStatus.ACTIVE)
                continue;
            if (goal.getStartDate() == null || goal.getDeadline() == null)
                continue;

            BigDecimal spent = calculateSpent(goal, transactions);
            BigDecimal target = goal.getTargetAmount();

            goal.setCurrentAmount(spent);

            if (target == null || target.compareTo(BigDecimal.ZERO) <= 0)
                continue;

            BigDecimal progressPercent = spent
                    .multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP);

            // ✅ STATUS UPDATE
            if (spent.compareTo(target) >= 0) {
                goal.setStatus(GoalStatus.COMPLETED);
            } else if (today.isAfter(goal.getDeadline())) {
                goal.setStatus(GoalStatus.OVERDUE);
            }

            goalRepository.save(goal);

            // ✅ NUDGES (simple)
            Optional<Nudge> nudge = createSimpleNudge(
                    user, goal, spent, target, progressPercent, today);

            nudge.ifPresent(nudges::add);
        }

        if (!nudges.isEmpty()) {
            nudgeRepository.saveAll(nudges);
        }

        return nudges.stream()
                .map(nudgeMapper::toDto)
                .collect(Collectors.toList());

    }

    private BigDecimal calculateSpent(Goal goal, List<Transaction> txns) {
        txns.forEach(t -> {
            log.info("TXN: cat={}, type={}, date={}, amount={}",
                    t.getCategory() != null ? t.getCategory().getName() : "NULL",
                    t.getTypeTransaction(),
                    t.getDate(),
                    t.getAmount());
        });

        log.info("GOAL CATEGORY = {}", goal.getCategory());
        log.info("GOAL START = {}", goal.getStartDate());
        log.info("GOAL DEADLINE = {}", goal.getDeadline());

        return txns.stream()
                .filter(t -> t.getCategory() != null)
                .filter(t -> t.getCategory().getName().equalsIgnoreCase(goal.getCategory()))
                .filter(t -> t.getTypeTransaction() != null &&
                        t.getTypeTransaction().name().equalsIgnoreCase("DEBIT"))
                .filter(t -> t.getDate() != null)
                .filter(t -> !t.getDate().isBefore(goal.getStartDate()) &&
                        !t.getDate().isAfter(goal.getDeadline()))
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    private Optional<Nudge> createSimpleNudge(
            User user,
            Goal goal,
            BigDecimal spent,
            BigDecimal target,
            BigDecimal progressPercent,
            LocalDate today) {

        String title;
        String message;

        if (progressPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            title = "Budget exceeded";
            message = "You have exceeded your " + goal.getCategory() + " budget.";
        } else if (progressPercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
            title = "Budget almost used";
            message = "You have used " + progressPercent + "% of your " + goal.getCategory() + " budget.";
        } else if (ChronoUnit.DAYS.between(today, goal.getDeadline()) <= 3) {
            title = "Deadline approaching";
            message = "Only a few days left to stay within your " + goal.getCategory() + " budget.";
        } else {
            return Optional.empty();
        }

        return Optional.of(Nudge.builder()
                .user(user)
                .goal(goal)
                .category(goal.getCategory())
                .title(title)
                .message(message)
                .spendAmount(spent)
                .targetAmount(target)
                .progressPercent(progressPercent)
                .remainingAmount(target.subtract(spent))
                .exceededAmount(spent.compareTo(target) > 0 ? spent.subtract(target) : BigDecimal.ZERO)
                .build());

    }
}
