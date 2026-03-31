package com.salygin.ssubench.controller;

import com.salygin.ssubench.security.CurrentUserId;
import com.salygin.ssubench.service.PaymentService;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @Resource(name = "controllerExecutor")
    private Executor controllerExecutor;

    @TimeLimiter(name = "http-controller")
    @PostMapping("/confirm")
    public CompletableFuture<Void> confirmTask(
            @RequestParam UUID taskId,
            @CurrentUserId UUID customerId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            paymentService.confirmTask(taskId, customerId);
            return null;
        }, controllerExecutor);
    }
}