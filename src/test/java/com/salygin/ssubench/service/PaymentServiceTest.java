package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Task;
import com.salygin.ssubench.entity.TaskStatus;
import com.salygin.ssubench.repository.PaymentRepository;
import com.salygin.ssubench.repository.TaskRepository;
import com.salygin.ssubench.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CurrentUserAccessService accessService;

    private PaymentService paymentService;

    @BeforeEach
    void init() {
        paymentService = new PaymentService(taskRepository, userRepository, paymentRepository, accessService);
    }

    @Test
    void confirmTask_successfullyTransfersFunds() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        Task job = new Task(jobId, "Build UI", "Frontend work", 350, TaskStatus.DONE, clientId, workerId);

        when(taskRepository.getById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.deductIfSufficient(clientId, 350L)).thenReturn(1);

        paymentService.confirmTask(jobId, clientId);

        verify(userRepository).deductIfSufficient(clientId, 350L);
        verify(userRepository).addBalance(workerId, 350L);
        verify(paymentRepository).insertPayment(any(UUID.class), eq(clientId), eq(workerId), eq(350), any(LocalDateTime.class));
        verify(taskRepository).changeStatus(jobId, TaskStatus.CONFIRMED);
    }

    @Test
    void confirmTask_deniedWhenNotOwner() {
        UUID jobId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Task job = new Task(jobId, "Deploy app", "Cloud deploy", 500, TaskStatus.DONE, UUID.randomUUID(), UUID.randomUUID());

        when(taskRepository.getById(jobId)).thenReturn(Optional.of(job));
        doThrow(new IllegalArgumentException("Operation not allowed"))
                .when(accessService)
                .checkAccess(eq(requesterId), any());

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmTask(jobId, requesterId)
        );

        assertEquals("Operation not allowed", ex.getMessage());

        verifyNoInteractions(paymentRepository);
        verify(userRepository, never()).addBalance(any(), anyLong());
        verify(taskRepository, never()).changeStatus(any(), any());
    }

    @Test
    void confirmTask_rejectedIfNotFinished() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Task job = new Task(jobId, "Write tests", "JUnit", 120, TaskStatus.IN_PROGRESS, clientId, UUID.randomUUID());

        when(taskRepository.getById(jobId)).thenReturn(Optional.of(job));

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmTask(jobId, clientId)
        );

        assertEquals("Task is not completed.", ex.getMessage());

        verifyNoInteractions(paymentRepository);
        verify(userRepository, never()).deductIfSufficient(any(), anyLong());
    }

    @Test
    void confirmTask_rejectedIfExecutorAbsent() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Task job = new Task(jobId, "Optimize DB", "Indexes", 200, TaskStatus.DONE, clientId, null);

        when(taskRepository.getById(jobId)).thenReturn(Optional.of(job));

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmTask(jobId, clientId)
        );

        assertEquals("Executor not assigned.", ex.getMessage());

        verify(paymentRepository, never()).insertPayment(any(), any(), any(), any(), any());
        verify(userRepository, never()).addBalance(any(), anyLong());
    }

    @Test
    void confirmTask_rejectedIfBalanceTooLow() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        Task job = new Task(jobId, "Integration", "API work", 600, TaskStatus.DONE, clientId, workerId);

        when(taskRepository.getById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.deductIfSufficient(clientId, 600L)).thenReturn(0);

        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.confirmTask(jobId, clientId)
        );

        assertEquals("Insufficient balance.", ex.getMessage());

        verify(userRepository).deductIfSufficient(clientId, 600L);
        verify(userRepository, never()).addBalance(any(), anyLong());
        verify(paymentRepository, never()).insertPayment(any(), any(), any(), any(), any());
    }
}
