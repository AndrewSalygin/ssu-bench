package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Task;
import com.salygin.ssubench.entity.TaskStatus;
import com.salygin.ssubench.repository.PaymentRepository;
import com.salygin.ssubench.repository.TaskRepository;
import com.salygin.ssubench.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final PaymentRepository paymentRepo;
    private final CurrentUserAccessService access;

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Transactional
    public void confirmTask(UUID taskId, UUID actorId) {

        Task task = loadTask(taskId);

        validateAccess(actorId, task);
        validateTaskState(task);

        UUID performerId = resolveExecutor(task);
        int amount = task.getReward();

        processDebit(task.getCustomerId(), amount);
        processCredit(performerId, amount);

        recordPayment(task, performerId, amount);

        taskRepo.changeStatus(taskId, TaskStatus.CONFIRMED);
    }

    private Task loadTask(UUID id) {
        return taskRepo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private void validateAccess(UUID actorId, Task task) {
        access.checkAccess(actorId, task.getCustomerId());
    }

    private void validateTaskState(Task task) {
        if (!TaskStatus.DONE.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task is not completed.");
        }
    }

    private UUID resolveExecutor(Task task) {
        UUID executorId = task.getExecutorId();

        if (executorId == null) {
            throw new IllegalArgumentException("Executor not assigned.");
        }

        return executorId;
    }

    private void processDebit(UUID customerId, int amount) {
        int affected = userRepo.deductIfSufficient(customerId, amount);

        if (affected == 0) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
    }

    private void processCredit(UUID executorId, int amount) {
        userRepo.addBalance(executorId, amount);
    }

    private void recordPayment(Task task, UUID executorId, int amount) {
        paymentRepo.insertPayment(
                UUID.randomUUID(),
                task.getCustomerId(),
                executorId,
                amount,
                LocalDateTime.now()
        );
    }
}
