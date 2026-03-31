package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Task;
import com.salygin.ssubench.entity.TaskStatus;
import com.salygin.ssubench.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CurrentUserAccessService accessService;

    private TaskService taskService;

    @BeforeEach
    void init() {
        taskService = new TaskService(taskRepository, accessService);
    }

    @Test
    void createTask_shouldStoreNewDraftTask() {
        UUID clientId = UUID.randomUUID();
        String name = "Implement feature X";
        String details = "Backend logic";
        Integer price = 220;

        when(taskRepository.getById(any(UUID.class)))
                .thenAnswer(inv -> {
                    UUID genId = inv.getArgument(0);
                    return Optional.of(new Task(genId, name, details, price, TaskStatus.CREATED, clientId, null));
                });

        Task created = taskService.createTask(clientId, name, details, price);

        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);

        verify(taskRepository).insert(
                any(UUID.class),
                eq(name),
                eq(details),
                eq(price),
                stateCaptor.capture(),
                eq(clientId)
        );

        assertEquals(TaskStatus.CREATED, created.getStatus());
        assertEquals(name, created.getTitle());
        assertEquals(price, created.getReward());
        assertEquals(TaskStatus.CREATED.name(), stateCaptor.getValue());
    }

    @Test
    void updateTask_shouldModifyFields_whenEditable() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID editor = UUID.randomUUID();

        String newTitle = "Refactor module";
        String newDesc = "Improve structure";
        Integer newReward = 330;

        Task original = new Task(id, "Legacy", "Old code", 150, TaskStatus.CREATED, owner, null);
        Task modified = new Task(id, newTitle, newDesc, newReward, TaskStatus.CREATED, owner, null);

        when(taskRepository.getById(id)).thenReturn(Optional.of(original), Optional.of(modified));
        doNothing().when(accessService).checkAccess(editor, owner);

        Task result = taskService.updateTask(id, newTitle, newDesc, newReward, TaskStatus.CREATED.name(), editor);

        verify(taskRepository).modify(id, newTitle, newDesc, newReward, TaskStatus.CREATED.name());

        assertEquals(newTitle, result.getTitle());
        assertEquals(newDesc, result.getDescription());
        assertEquals(newReward, result.getReward());
    }

    @Test
    void publishTask_shouldSwitchStatus() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        Task draft = new Task(id, "Draft job", "Prepare", 90, TaskStatus.CREATED, owner, null);
        Task live = new Task(id, "Draft job", "Prepare", 90, TaskStatus.PUBLISHED, owner, null);

        when(taskRepository.getById(id)).thenReturn(Optional.of(draft), Optional.of(live));
        doNothing().when(accessService).checkAccess(actor, owner);

        Task result = taskService.publishTask(id, actor);

        verify(taskRepository).changeStatus(id, TaskStatus.PUBLISHED);
        assertEquals(TaskStatus.PUBLISHED, result.getStatus());
    }

    @Test
    void publishTask_shouldFail_ifWrongState() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        Task already = new Task(id, "Job", "Info", 70, TaskStatus.PUBLISHED, owner, null);

        when(taskRepository.getById(id)).thenReturn(Optional.of(already));
        doNothing().when(accessService).checkAccess(actor, owner);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.publishTask(id, actor)
        );

        assertEquals("Task cannot be published.", ex.getMessage());
        verify(taskRepository, never()).changeStatus(any(), any());
    }

    @Test
    void getTask_shouldReturnExisting() {
        UUID id = UUID.randomUUID();
        Task task = new Task(id, "Analysis", "Research", 40, TaskStatus.PUBLISHED, UUID.randomUUID(), null);

        when(taskRepository.getById(id)).thenReturn(Optional.of(task));

        Task found = taskService.getTask(id);

        assertSame(task, found);
    }

    @Test
    void getTasks_shouldReturnPage() {
        List<Task> data = List.of(
                new Task(UUID.randomUUID(), "One", "Desc1", 15, TaskStatus.CREATED, UUID.randomUUID(), null),
                new Task(UUID.randomUUID(), "Two", "Desc2", 25, TaskStatus.PUBLISHED, UUID.randomUUID(), null)
        );

        when(taskRepository.getPage(5, 0)).thenReturn(data);

        List<Task> result = taskService.getTasks(5, 0);

        assertEquals(data, result);
    }

    @Test
    void cancelTask_shouldRemove_whenValid() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        Task task = new Task(id, "Cleanup", "Remove logs", 60, TaskStatus.PUBLISHED, owner, null);

        when(taskRepository.getById(id)).thenReturn(Optional.of(task));
        doNothing().when(accessService).checkAccess(actor, owner);

        taskService.cancelTask(id, actor);

        verify(taskRepository).markCancelled(id);
    }

    @Test
    void cancelTask_shouldFail_whenUnauthorized() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        Task task = new Task(id, "Cleanup", "Remove logs", 60, TaskStatus.PUBLISHED, owner, null);

        when(taskRepository.getById(id)).thenReturn(Optional.of(task));
        doThrow(new IllegalArgumentException("Forbidden action"))
                .when(accessService)
                .checkAccess(actor, owner);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.cancelTask(id, actor)
        );

        assertEquals("Forbidden action", ex.getMessage());
        verify(taskRepository, never()).markCancelled(any());
    }

    @Test
    void cancelTask_shouldFail_whenAlreadyClosed() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        Task task = new Task(id, "Done job", "Completed", 80, TaskStatus.CONFIRMED, owner, UUID.randomUUID());

        when(taskRepository.getById(id)).thenReturn(Optional.of(task));
        doNothing().when(accessService).checkAccess(actor, owner);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.cancelTask(id, actor)
        );

        assertEquals("Task already completed.", ex.getMessage());
        verify(taskRepository, never()).markCancelled(any());
    }
}
