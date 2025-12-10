package com.Life_ledger.service;

import com.Life_ledger.Enum.GoalStatus;
import com.Life_ledger.Enum.GoalType;
import com.Life_ledger.Enum.NudgeSeverity;
import com.Life_ledger.Enum.NudgeType;
import com.Life_ledger.entity.*;
import com.Life_ledger.repository.NudgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // /**
    // * Evaluate all goals for a user and create nudges based on progress.
    // *
    // * @param user authenticated user
    // * @param goals user goals
    // * @param txns user transactions relevant to goals
    // * @return list of created nudges
    // */
    public List<Nudge> evaluateAndCreateNudges(User user,
            List<Goal> goals,
            List<Transaction> txns) {

        if (goals == null || goals.isEmpty()) {
            return Collections.emptyList();
        }

        List<Nudge> created = new ArrayList<>();

        // Group transactions by category name (for budget goals)
        Map<String, List<Transaction>> txnsByCategory = txns.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName().toLowerCase()));

        LocalDate today = LocalDate.now();

        for (Goal goal : goals) {

            updateGoalStatusIfNeeded(goal, today);

            if (goal.getStatus() == GoalStatus.COMPLETED ||
                    goal.getStatus() == GoalStatus.PAUSED ||
                    goal.getStatus() == GoalStatus.CANCELLED) {
                continue;
            }

            try {
                if (goal.getType() == GoalType.BUDGET) {
                    List<Transaction> catTxns = txnsByCategory.getOrDefault(
                            safeLower(goal.getCategory()),
                            Collections.emptyList());

                    Optional<Nudge> n = evaluateBudgetGoal(user, goal, catTxns, today);
                    n.ifPresent(created::add);

                } else if (goal.getType() == GoalType.SAVING) {
                    Optional<Nudge> n = evaluateSavingGoal(user, goal, today);
                    n.ifPresent(created::add);
                }

            } catch (Exception e) {
                log.error("Failed evaluating goal id={} name={}: {}",
                        goal.getId(), goal.getName(), e.getMessage(), e);
            }
        }

        if (!created.isEmpty()) {
            nudgeRepository.saveAll(created);
            log.info("Created {} nudges for user={}", created.size(), user.getId());
        }

        return created;
    }

    private String safeLower(String s) {
        return s == null ? null : s.toLowerCase();
    }

    /**
     * Budget goal evaluator:
     * - Goal represents a SPENDING CAP on a category.
     * - We compare actual spend vs "expected" spend based on time.
     */
    private Optional<Nudge> evaluateBudgetGoal(User user,
            Goal goal,
            List<Transaction> categoryTxns,
            LocalDate today) {

        if (goal.getStartDate() == null || goal.getDeadline() == null) {
            return Optional.empty();
        }

        LocalDate start = goal.getStartDate();
        LocalDate end = goal.getDeadline();

        if (today.isBefore(start)) {
            return Optional.empty();
        }

        long totalDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
        long elapsedDays = Math.min(totalDays, ChronoUnit.DAYS.between(start, today) + 1);

        BigDecimal target = goal.getTargetAmount() != null
                ? goal.getTargetAmount()
                : BigDecimal.ZERO;

        // sum of DEBIT transactions in this category, within [start, today]
        BigDecimal spent = categoryTxns.stream()
                .filter(t -> t.getDate() != null &&
                        !t.getDate().isBefore(start) &&
                        !t.getDate().isAfter(today))
                .filter(t -> t.getTypeTransaction() != null &&
                        t.getTypeTransaction().name().equalsIgnoreCase("DEBIT"))
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal expected = target
                .multiply(BigDecimal.valueOf(elapsedDays))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);

        if (expected.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal ratio = spent.divide(expected, 2, RoundingMode.HALF_UP);

        double nudgeThreshold = goal.getNudgeThreshold() != null
                ? goal.getNudgeThreshold()
                : 0.7;

        NudgeSeverity severity = null;
        NudgeType type = null;
        String title;
        String message;

        if (spent.compareTo(target) >= 0) {
            // hard cap exceeded
            severity = NudgeSeverity.CRITICAL;
            type = NudgeType.BUDGET_OVERRUN;
            title = "Budget exceeded: " + goal.getCategory();
            message = String.format(
                    "You’ve already spent ₹%.2f of your ₹%.2f %s budget.",
                    spent, target, goal.getCategory());
        } else if (ratio.doubleValue() >= (nudgeThreshold + 0.15)) {
            // significantly ahead of expected spend
            severity = NudgeSeverity.WARNING;
            type = NudgeType.BUDGET_OVERRUN;
            title = "Spending faster than planned";
            message = String.format(
                    "You’ve used ₹%.2f of your ₹%.2f %s budget in %d of %d days.",
                    spent, target, goal.getCategory(), elapsedDays, totalDays);
        } else if (ratio.doubleValue() <= 0.5 && elapsedDays > totalDays * 0.5) {
            // positive nudge: under budget in later part of period
            severity = NudgeSeverity.POSITIVE;
            type = NudgeType.BUDGET_ON_TRACK;
            title = "Nice! You’re under your %s budget".formatted(goal.getCategory());
            message = String.format(
                    "You’ve spent ₹%.2f out of your ₹%.2f %s budget so far. Keep it up!",
                    spent, target, goal.getCategory());
        } else {
            return Optional.empty();
        }

        Nudge nudge = Nudge.builder()
                .user(user)
                .goal(goal)
                .type(type)
                .severity(severity)
                .category(goal.getCategory())
                .title(title)
                .message(message)
                .build();

        return Optional.of(nudge);
    }

    /**
     * Saving goal evaluator:
     * - Goal represents a target amount by a deadline (currentAmount tracks
     * progress).
     */
    private Optional<Nudge> evaluateSavingGoal(User user,
            Goal goal,
            LocalDate today) {

        if (goal.getStartDate() == null || goal.getDeadline() == null) {
            return Optional.empty();
        }

        LocalDate start = goal.getStartDate();
        LocalDate end = goal.getDeadline();

        long totalDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
        long elapsedDays = Math.min(totalDays, ChronoUnit.DAYS.between(start, today) + 1);

        BigDecimal target = goal.getTargetAmount() != null
                ? goal.getTargetAmount()
                : BigDecimal.ZERO;
        BigDecimal current = goal.getCurrentAmount() != null
                ? goal.getCurrentAmount()
                : BigDecimal.ZERO;

        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal progressPct = current
                .multiply(BigDecimal.valueOf(100))
                .divide(target, 2, RoundingMode.HALF_UP);

        BigDecimal expectedPct = BigDecimal.valueOf(elapsedDays)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);

        double tolerance = 0.1; // 10% slack

        NudgeSeverity severity = null;
        NudgeType type = null;
        String title;
        String message;

        if (progressPct.compareTo(BigDecimal.valueOf(100)) >= 0) {
            // this should normally mark the goal complete; we don't nudge here
            return Optional.empty();
        }

        if (progressPct.doubleValue() < expectedPct.doubleValue() * (1 - tolerance)) {
            severity = NudgeSeverity.WARNING;
            type = NudgeType.SAVING_BEHIND;
            title = "You’re behind on your saving goal";
            message = String.format(
                    "You’ve saved %.1f%% of your \"%s\" goal, but by now you should be around %.1f%%.",
                    progressPct.doubleValue(), goal.getName(), expectedPct.doubleValue());
        } else if (progressPct.doubleValue() > expectedPct.doubleValue() * (1 + tolerance)) {
            severity = NudgeSeverity.POSITIVE;
            type = NudgeType.SAVING_AHEAD;
            title = "Great job! You’re ahead of your goal";
            message = String.format(
                    "You’ve saved %.1f%% of your \"%s\" goal, which is ahead of schedule.",
                    progressPct.doubleValue(), goal.getName());
        } else {
            return Optional.empty();
        }

        Nudge nudge = Nudge.builder()
                .user(user)
                .goal(goal)
                .type(type)
                .severity(severity)
                .category(goal.getCategory())
                .title(title)
                .message(message)
                .build();

        return Optional.of(nudge);
    }

    private void updateGoalStatusIfNeeded(Goal goal, LocalDate today) {

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            return;
        }

        boolean deadlinePassed = goal.getDeadline() != null &&
                today.isAfter(goal.getDeadline());

        boolean completed = false;

        if (goal.getType() == GoalType.SAVING) {
            completed = goal.getCurrentAmount() != null &&
                    goal.getTargetAmount() != null &&
                    goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0;
        }

        if (completed) {
            goal.setStatus(GoalStatus.COMPLETED);
            return;
        }

        if (deadlinePassed) {
            goal.setStatus(GoalStatus.OVERDUE);
        }
    }

}
