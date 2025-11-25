package com.Life_ledger.service;

import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.entity.Goal;
import com.Life_ledger.entity.User;
import com.Life_ledger.enums.GoalType;
import com.Life_ledger.mapper.GoalMapper;
import com.Life_ledger.repository.GoalRepository;
import com.Life_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Goal goal = GoalMapper.toEntity(request);
        goal.setUser(user);

        return GoalMapper.toResponse(goalRepository.save(goal));
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long goalId, GoalRequest request) {
        Goal goal = goalRepository.findById(request.getUser_id())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if (request.getName() != null) goal.setName(request.getName());
        if (request.getTargetAmount() != null) goal.setTargetAmount(request.getTargetAmount());
        if (request.getStartDate() != null) goal.setStartDate(request.getStartDate());
        if (request.getDeadline() != null) goal.setDeadline(request.getDeadline());
        if (request.getType() != null) goal.setType(request.getType());
        if (request.getCategory() != null) goal.setCategory(request.getCategory());

        return GoalMapper.toResponse(goalRepository.save(goal));
    }

    @Override
    public GoalResponse getGoalById(Long goalId) {
        return goalRepository.findById(goalId)
                .map(GoalMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
    }

    @Override
    public List<GoalResponse> getAllGoals() {
        return goalRepository.findAll()
                .stream()
                .map(GoalMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoalResponse> getGoalsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Goal> goals = goalRepository.findByUser(user);
        System.out.println("GOALS FROM DB = " + goals);
        List<GoalResponse> mapped = goals.stream()
                .map(GoalMapper::toResponse)
                .collect(Collectors.toList());

        return mapped;
    }
    @Override
    public void deleteGoal(Long goalId) {
        goalRepository.deleteById(goalId);
    }

    @Override
    @Transactional
    public Optional<String> addContribution(Long goalId, BigDecimal amount) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        goalRepository.save(goal);

        return evaluateNudge(goal);
    }

    @Override
    public Optional<String> evaluateNudge(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        return evaluateNudge(goal);
    }

    private Optional<String> evaluateNudge(Goal goal) {

        BigDecimal target = goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount();

        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) return Optional.empty();

        double progress = current
                .divide(target, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .doubleValue();

        LocalDate start = goal.getStartDate();
        LocalDate deadline = goal.getDeadline();

        if (start == null || deadline == null) return Optional.empty();

        long totalDays = start.until(deadline).getDays();
        long passedDays = start.until(LocalDate.now()).getDays();

        if (totalDays <= 0 || passedDays < 0) return Optional.empty();

        double expected = ((double) passedDays / totalDays) * 100;

        if (goal.getType() == GoalType.BUDGET && progress > expected) {
            return Optional.of("You're using your " + goal.getCategory() +
                    " budget faster than expected.");
        }

        if (goal.getType() == GoalType.SAVING && progress < expected) {
            return Optional.of("You're behind your savings goal: " + goal.getName());
        }

        return Optional.empty();
    }
}
