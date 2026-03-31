package com.salygin.ssubench.controller;

import com.salygin.ssubench.api.auth.AuthResponse;
import com.salygin.ssubench.entity.Role;
import com.salygin.ssubench.service.AuthService;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @Resource(name = "controllerExecutor")
    private Executor controllerExecutor;

    @TimeLimiter(name = "http-controller")
    @PostMapping("/register")
    public CompletableFuture<AuthResponse> register(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password,
            @RequestParam @NotNull Role role
    ) {
        return CompletableFuture.supplyAsync(() -> authService.register(email, password, role), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/login")
    public CompletableFuture<AuthResponse> login(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password
    ) {
        return CompletableFuture.supplyAsync(() -> authService.login(email, password), controllerExecutor);
    }

}