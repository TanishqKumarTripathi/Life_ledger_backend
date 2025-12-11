package com.Life_ledger.service;

import com.Life_ledger.Enum.GoalStatus;
import com.Life_ledger.Enum.GoalType;
import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Goal;
import com.Life_ledger.entity.User;
import com.Life_ledger.exception.AppException;
import com.Life_ledger.mapper.GoalMapper;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.GoalRepository;
import com.Life_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GoalMapper goalMapper;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public GoalResponse createGoal(GoalRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new AppException("User not found"));

        Goal goal = goalMapper.toEntity(req);
        goal.setUser(user);

        normalizeDates(goal); // ✅ ADD THIS
        updateGoalStatus(goal);

        Goal saved = goalRepository.save(goal);
        return GoalResponse.from(saved);
    }

    @Override
    public GoalResponse createGoalForAccount(GoalRequest request, Long accountId) {
        return null;
    }

    @Override
    public GoalResponse getGoalByIdAndUser(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new AppException("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new AppException("Unauthorized access to goal");
        }

        updateGoalStatus(goal);
        return GoalResponse.from(goal);
    }

    @Override
    public List<GoalResponse> getGoalsByUser(Long userId) {
        List<Goal> goals = goalRepository.findByUser_Id(userId);

        goals.forEach(this::updateGoalStatus);

        return goals.stream()
                .map(GoalResponse::from)
                .toList();
    }

    @Override
    public List<GoalResponse> getGoalsByAccount(Long accountId) {
        return List.of();
    }

    @Override
    public GoalResponse updateGoal(Long goalId, GoalRequest req) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new AppException("Goal not found"));

        if (!goal.getUser().getId().equals(req.getUserId())) {
            throw new AppException("Unauthorized update attempt");
        }

        goalMapper.updateGoalFromRequest(goal, req);

        normalizeDates(goal); // ✅ ADD THIS
        updateGoalStatus(goal);

        Goal updated = goalRepository.save(goal);
        return GoalResponse.from(updated);
    }

    @Override
    public void deleteGoal(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new AppException("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new AppException("Unauthorized delete attempt");
        }

        goalRepository.delete(goal);
    }

    @Override
    public Optional<String> addContribution(Long goalId, BigDecimal amount, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new AppException("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new AppException("Unauthorized contribution attempt");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Contribution amount must be positive");
        }

        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        updateGoalStatus(goal);
        goalRepository.save(goal);

        return evaluateNudge(goalId, userId);
    }

    @Override
    public Optional<String> evaluateNudge(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new AppException("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new AppException("Unauthorized");
        }

        double progress = goal.getCurrentAmount()
                .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                .doubleValue();

        if (goal.getTargetAmount() == null ||
                goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        if (progress >= 1.0) {
            return Optional.of("🎉 Congratulations! Goal completed!");
        }

        double threshold = goal.getNudgeThreshold() != null
                ? goal.getNudgeThreshold()
                : 0.7;

        if (progress >= threshold) {
            return Optional.of("📈 You're " + Math.round(progress * 100) + "% there! Keep it up!");
        }

        if (goal.getDeadline() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
            if (daysLeft <= 7 && daysLeft > 0) {
                return Optional.of("⏰ Only " + daysLeft + " days left to reach your goal!");
            }
        }

        return Optional.empty();
    }

    private void updateGoalStatus(Goal goal) {
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        } else if (goal.getDeadline() != null && LocalDate.now().isAfter(goal.getDeadline())) {
            goal.setStatus(GoalStatus.OVERDUE);
        } else if (goal.getStatus() == GoalStatus.OVERDUE || goal.getStatus() == GoalStatus.COMPLETED) {
            // keep existing
        } else {
            goal.setStatus(GoalStatus.ACTIVE);
        }
    }

    private void normalizeDates(Goal goal) {

        if ((goal.getType() == GoalType.SPENDINGCAP
                || goal.getType() == GoalType.BUDGET)
                && goal.getDeadline() == null) {

            // ✅ Default: current month budget
            goal.setDeadline(
                    goal.getStartDate()
                            .withDayOfMonth(goal.getStartDate().lengthOfMonth()));
        }
    }

    @Override
    public void deleteAllGoalsByUser(Long userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        if (!goals.isEmpty()) {
            goalRepository.deleteAll(goals);
        }
    }

    @Override
    public void deleteAllGoalsByAccount(Long userId, Long accountId) {

        // ensure user owns this account
        BankAccount acc = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!acc.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this account");
        }

        List<Goal> goals = goalRepository.findByBankAccountId(accountId);
        if (!goals.isEmpty()) {
            goalRepository.deleteAll(goals);
        }

    }

}
