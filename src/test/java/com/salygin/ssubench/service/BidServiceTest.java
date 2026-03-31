package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Bid;
import com.salygin.ssubench.entity.BidStatus;
import com.salygin.ssubench.entity.Task;
import com.salygin.ssubench.entity.TaskStatus;
import com.salygin.ssubench.repository.BidRepository;
import com.salygin.ssubench.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CurrentUserAccessService accessService;

    private BidService bidService;

    @BeforeEach
    void setup() {
        bidService = new BidService(bidRepository, taskRepository, accessService);
    }

    @Test
    void createBid_success_forActiveTask() {
        UUID tId = UUID.randomUUID();
        UUID execId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();

        Task activeTask = new Task(tId, "Fix bug", "Critical issue", 250, TaskStatus.PUBLISHED, owner, null);
        Bid saved = new Bid(bId, tId, execId, BidStatus.PENDING.name());

        when(taskRepository.getById(tId)).thenReturn(Optional.of(activeTask));
        when(bidRepository.getById(any(UUID.class))).thenReturn(Optional.of(saved));

        Bid result = bidService.createBid(tId, execId);

        verify(bidRepository).insert(any(UUID.class), eq(tId), eq(execId), eq(BidStatus.PENDING.name()));

        assertEquals(execId, result.getExecutorId());
        assertEquals(tId, result.getTaskId());
        assertEquals(BidStatus.PENDING.name(), result.getStatus());
    }

    @Test
    void createBid_fails_whenTaskAbsent() {
        UUID tId = UUID.randomUUID();

        when(taskRepository.getById(tId)).thenReturn(Optional.empty());

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.createBid(tId, UUID.randomUUID())
        );

        assertEquals("Task not found.", ex.getMessage());
    }

    @Test
    void createBid_fails_whenStatusInvalid() {
        UUID tId = UUID.randomUUID();

        Task draft = new Task(tId, "Draft", "Hidden", 300, TaskStatus.CREATED, UUID.randomUUID(), null);

        when(taskRepository.getById(tId)).thenReturn(Optional.of(draft));

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.createBid(tId, UUID.randomUUID())
        );

        assertEquals("Task is not open for bids.", ex.getMessage());
        verify(bidRepository, never()).insert(any(), any(), any(), any());
    }

    @Test
    void markCompleted_updatesStatus_whenExecutorMatches() {
        UUID tId = UUID.randomUUID();
        UUID execId = UUID.randomUUID();

        Task task = new Task(tId, "Build API", "REST", 400, TaskStatus.IN_PROGRESS, UUID.randomUUID(), execId);

        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));

        bidService.markCompleted(tId, execId);

        verify(taskRepository).changeStatus(tId, TaskStatus.DONE);
    }

    @Test
    void markCompleted_fails_whenNoExecutor() {
        UUID tId = UUID.randomUUID();

        Task task = new Task(tId, "Write docs", "Markdown", 50, TaskStatus.IN_PROGRESS, UUID.randomUUID(), null);

        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.markCompleted(tId, UUID.randomUUID())
        );

        assertEquals("Executor not assigned.", ex.getMessage());
    }

    @Test
    void markCompleted_fails_whenWrongStatus() {
        UUID tId = UUID.randomUUID();

        Task task = new Task(tId, "Deploy", "Prod", 500, TaskStatus.PUBLISHED, UUID.randomUUID(), UUID.randomUUID());

        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.markCompleted(tId, UUID.randomUUID())
        );

        assertEquals("Task is not in progress.", ex.getMessage());
    }

    @Test
    void selectBid_assignsExecutor_correctly() {
        UUID tId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID execId = UUID.randomUUID();

        Task task = new Task(tId, "Feature", "New module", 700, TaskStatus.PUBLISHED, owner, null);
        Bid bid = new Bid(bId, tId, execId, BidStatus.PENDING.name());

        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));
        when(bidRepository.getById(bId)).thenReturn(Optional.of(bid));

        bidService.selectBid(bId, owner);

        verify(bidRepository).changeStatus(bId, BidStatus.ACCEPTED);
        verify(bidRepository).rejectOthers(tId, bId);
        verify(taskRepository).setExecutor(tId, execId);
    }

    @Test
    void getBids_returnsCollection() {
        UUID tId = UUID.randomUUID();

        List<Bid> list = List.of(
                new Bid(UUID.randomUUID(), tId, UUID.randomUUID(), BidStatus.PENDING.name()),
                new Bid(UUID.randomUUID(), tId, UUID.randomUUID(), BidStatus.REJECTED.name())
        );

        when(bidRepository.getByTask(tId, 10, 5)).thenReturn(list);

        List<Bid> result = bidService.getBids(tId, 10, 5);

        assertEquals(list, result);
    }

    @Test
    void createBid_fails_whenDuplicateExists() {
        UUID tId = UUID.randomUUID();
        UUID execId = UUID.randomUUID();

        Task task = new Task(tId, "Refactor", "Cleanup", 120, TaskStatus.PUBLISHED, UUID.randomUUID(), null);

        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));
        when(bidRepository.existsByTaskAndExecutor(tId, execId)).thenReturn(2);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.createBid(tId, execId)
        );

        assertEquals("Bid already exists.", ex.getMessage());
    }

    @Test
    void selectBid_fails_whenNotOwner() {
        UUID tId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        UUID owner = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();

        Task task = new Task(tId, "Test", "QA", 80, TaskStatus.PUBLISHED, owner, null);
        Bid bid = new Bid(bId, tId, UUID.randomUUID(), BidStatus.PENDING.name());

        when(bidRepository.getById(bId)).thenReturn(Optional.of(bid));
        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));

        doThrow(new IllegalArgumentException("Only task owner can select bid."))
                .when(accessService)
                .checkAccess(any(), any());

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.selectBid(bId, outsider)
        );

        assertEquals("Only task owner can select bid.", ex.getMessage());
    }

    @Test
    void selectBid_fails_whenBidMissing() {
        UUID bId = UUID.randomUUID();

        when(bidRepository.getById(bId)).thenReturn(Optional.empty());

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.selectBid(bId, UUID.randomUUID())
        );

        assertEquals("Bid not found.", ex.getMessage());
    }

    @Test
    void selectBid_fails_whenTaskMissing() {
        UUID tId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        Bid bid = new Bid(bId, tId, UUID.randomUUID(), BidStatus.PENDING.name());

        when(bidRepository.getById(bId)).thenReturn(Optional.of(bid));
        when(taskRepository.getById(tId)).thenReturn(Optional.empty());

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.selectBid(bId, UUID.randomUUID())
        );

        assertEquals("Task not found.", ex.getMessage());
    }

    @Test
    void selectBid_fails_whenTaskClosed() {
        UUID tId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        Task task = new Task(tId, "Closed", "No bids", 60, TaskStatus.CREATED, UUID.randomUUID(), null);
        Bid bid = new Bid(bId, tId, UUID.randomUUID(), BidStatus.PENDING.name());

        when(bidRepository.getById(bId)).thenReturn(Optional.of(bid));
        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.selectBid(bId, UUID.randomUUID())
        );

        assertEquals("Task is not open for selection.", ex.getMessage());
    }

    @Test
    void selectBid_fails_whenExecutorAlreadyChosen() {
        UUID tId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        Task task = new Task(tId, "Assigned", "Taken", 90, TaskStatus.PUBLISHED, UUID.randomUUID(), UUID.randomUUID());
        Bid bid = new Bid(bId, tId, UUID.randomUUID(), BidStatus.PENDING.name());

        when(bidRepository.getById(bId)).thenReturn(Optional.of(bid));
        when(taskRepository.getById(tId)).thenReturn(Optional.of(task));

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.selectBid(bId, UUID.randomUUID())
        );

        assertEquals("Executor already selected.", ex.getMessage());
    }
}
