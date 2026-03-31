package com.salygin.ssubench.controller;

import com.salygin.ssubench.entity.Role;
import com.salygin.ssubench.entity.User;
import com.salygin.ssubench.security.CurrentUserId;
import com.salygin.ssubench.service.UserService;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @Resource(name = "controllerExecutor")
    private Executor controllerExecutor;

    @TimeLimiter(name = "http-controller")
    @GetMapping("/{id}")
    public CompletableFuture<User> getUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        return CompletableFuture.supplyAsync(() -> userService.getUser(id, currentUserId), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @GetMapping
    public CompletableFuture<List<User>> getAllUsers(
            @RequestParam(defaultValue = "20") @Positive int limit,
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset
    ) {
        return CompletableFuture.supplyAsync(() -> userService.getAllUsers(limit, offset), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping
    public CompletableFuture<User> createUser(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank @Size(min = 8, max = 72) String password,
            @RequestParam @NotNull Role role
    ) {
        return CompletableFuture.supplyAsync(() -> userService.createUser(email, password, role), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PutMapping("/{id}")
    public CompletableFuture<User> updateUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId,
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password,
            @RequestParam @NotNull Role role,
            @RequestParam @PositiveOrZero long balance
    ) {
        return CompletableFuture.supplyAsync(
                () -> userService.updateUser(id, currentUserId, email, password, role, balance),
                controllerExecutor
        );
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/{id}/balance")
    public CompletableFuture<User> updateUserBalance(
            @PathVariable UUID id,
            @RequestParam @Positive long balance
    ) {
        return CompletableFuture.supplyAsync(
                () -> userService.updateUserBalance(id, balance),
                controllerExecutor
        );
    }

    @TimeLimiter(name = "http-controller")
    @DeleteMapping("/{id}")
    public CompletableFuture<Void> deleteUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            userService.deleteUser(id, currentUserId);
            return null;
        }, controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/{id}/block")
    public CompletableFuture<Void> blockUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            userService.blockUser(id, currentUserId);
            return null;
        }, controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/{id}/unblock")
    public CompletableFuture<Void> unblockUser(
            @PathVariable UUID id
    ) {
        return CompletableFuture.supplyAsync(() -> {
            userService.unblockUser(id);
            return null;
        }, controllerExecutor);
    }
}