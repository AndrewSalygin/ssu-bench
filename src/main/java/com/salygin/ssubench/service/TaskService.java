package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Task;
import com.salygin.ssubench.entity.TaskStatus;
import com.salygin.ssubench.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repo;
    private final CurrentUserAccessService access;

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public Task createTask(UUID ownerId, String title, String description, Integer reward) {

        ensureRewardValid(reward);

        UUID taskId = UUID.randomUUID();

        repo.insert(
                taskId,
                title,
                description,
                reward,
                TaskStatus.CREATED.name(),
                ownerId
        );

        return loadTask(taskId);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public Task updateTask(UUID taskId,
                           String title,
                           String description,
                           Integer reward,
                           String newStatus,
                           UUID actorId) {

        Task current = loadTask(taskId);

        access.checkAccess(actorId, current.getCustomerId());

        ensureRewardValid(reward);
        ensureEditable(current);

        repo.modify(
                taskId,
                title,
                description,
                reward,
                newStatus
        );

        return loadTask(taskId);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public Task publishTask(UUID taskId, UUID actorId) {

        Task task = loadTask(taskId);

        access.checkAccess(actorId, task.getCustomerId());
        ensureCanPublish(task);

        repo.changeStatus(taskId, TaskStatus.PUBLISHED);

        return loadTask(taskId);
    }

    public Task getTask(UUID taskId) {
        return loadTask(taskId);
    }

    public List<Task> getTasks(int limit, int offset) {
        return repo.getPage(limit, offset);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public void cancelTask(UUID taskId, UUID actorId) {

        Task task = loadTask(taskId);

        access.checkAccess(actorId, task.getCustomerId());
        ensureNotFinished(task);

        repo.markCancelled(taskId);
    }

    private Task loadTask(UUID id) {
        return repo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private void ensureRewardValid(Integer reward) {
        if (reward == null || reward <= 0) {
            throw new IllegalArgumentException("Invalid reward.");
        }
    }

    private void ensureEditable(Task task) {
        if (!TaskStatus.CREATED.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task cannot be edited.");
        }
    }

    private void ensureCanPublish(Task task) {
        if (!TaskStatus.CREATED.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task cannot be published.");
        }
    }

    private void ensureNotFinished(Task task) {
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CONFIRMED) {
            throw new IllegalArgumentException("Task already completed.");
        }
    }
}
