package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Bid;
import com.salygin.ssubench.entity.BidStatus;
import com.salygin.ssubench.entity.Task;
import com.salygin.ssubench.entity.TaskStatus;
import com.salygin.ssubench.repository.BidRepository;
import com.salygin.ssubench.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepo;
    private final TaskRepository taskRepo;
    private final CurrentUserAccessService access;

    @PreAuthorize("hasAnyRole('EXECUTOR','ADMIN')")
    public Bid createBid(UUID taskId, UUID performerId) {

        Task task = loadTask(taskId);

        checkCanPlaceBid(task, performerId);

        UUID newBidId = UUID.randomUUID();

        bidRepo.insert(
                newBidId,
                taskId,
                performerId,
                BidStatus.PENDING.name()
        );

        return getBidOrFail(newBidId);
    }

    public List<Bid> getBids(UUID taskId, int size, int offset) {
        return bidRepo.getByTask(taskId, size, offset);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Transactional
    public void selectBid(UUID bidId, UUID actorId) {

        Bid bid = getBidOrFail(bidId);
        Task task = loadTask(bid.getTaskId());

        validateSelectionAccess(actorId, task);
        validateTaskReadyForSelection(task);

        applyBidSelection(task, bid);
    }

    @PreAuthorize("hasAnyRole('EXECUTOR','ADMIN')")
    public void markCompleted(UUID taskId, UUID performerId) {

        Task task = loadTask(taskId);

        ensureExecutorAssigned(task);
        verifyExecutor(task, performerId);
        ensureInProgress(task);

        taskRepo.changeStatus(taskId, TaskStatus.DONE);
    }

    private Task loadTask(UUID id) {
        return taskRepo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private Bid getBidOrFail(UUID id) {
        return bidRepo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found."));
    }

    private void checkCanPlaceBid(Task task, UUID performerId) {

        if (task.getCustomerId().equals(performerId)) {
            throw new IllegalArgumentException("Customer cannot bid on own task.");
        }

        if (!TaskStatus.PUBLISHED.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task is not open for bids.");
        }

        int existing = bidRepo.existsByTaskAndExecutor(task.getId(), performerId);
        if (existing > 0) {
            throw new IllegalArgumentException("Bid already exists.");
        }
    }

    private void validateSelectionAccess(UUID actorId, Task task) {
        access.checkAccess(actorId, task.getCustomerId());
    }

    private void validateTaskReadyForSelection(Task task) {

        if (!TaskStatus.PUBLISHED.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task is not open for selection.");
        }

        if (task.getExecutorId() != null) {
            throw new IllegalArgumentException("Executor already selected.");
        }
    }

    private void applyBidSelection(Task task, Bid bid) {

        bidRepo.changeStatus(bid.getId(), BidStatus.ACCEPTED);
        bidRepo.rejectOthers(task.getId(), bid.getId());
        taskRepo.setExecutor(task.getId(), bid.getExecutorId());
    }

    private void ensureExecutorAssigned(Task task) {
        if (task.getExecutorId() == null) {
            throw new IllegalArgumentException("Executor not assigned.");
        }
    }

    private void verifyExecutor(Task task, UUID performerId) {
        access.checkAccess(performerId, task.getExecutorId());
    }

    private void ensureInProgress(Task task) {
        if (!TaskStatus.IN_PROGRESS.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task is not in progress.");
        }
    }
}

