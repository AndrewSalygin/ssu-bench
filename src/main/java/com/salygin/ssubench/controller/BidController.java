package com.salygin.ssubench.controller;

import com.salygin.ssubench.entity.Bid;
import com.salygin.ssubench.security.CurrentUserId;
import com.salygin.ssubench.service.BidService;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@Validated
public class BidController {

    private final BidService bidService;

    @Resource(name = "controllerExecutor")
    private Executor controllerExecutor;

    @TimeLimiter(name = "http-controller")
    @PostMapping
    public CompletableFuture<Bid> createBid(
            @RequestParam UUID taskId,
            @CurrentUserId UUID executorId
    ) {
        return CompletableFuture.supplyAsync(() -> bidService.createBid(taskId, executorId), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @GetMapping("/task/{taskId}")
    public CompletableFuture<List<Bid>> getBids(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "20") @Positive int limit,
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset
    ) {
        return CompletableFuture.supplyAsync(() -> bidService.getBids(taskId, limit, offset), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/{bidId}/select")
    public CompletableFuture<Void> selectBid(
            @PathVariable UUID bidId,
            @CurrentUserId UUID customerId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            bidService.selectBid(bidId, customerId);
            return null;
        }, controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/task/{taskId}/complete")
    public CompletableFuture<Void> markCompleted(
            @PathVariable UUID taskId,
            @CurrentUserId UUID executorId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            bidService.markCompleted(taskId, executorId);
            return null;
        }, controllerExecutor);
    }
}