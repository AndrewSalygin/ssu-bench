package com.salygin.ssubench.service;

import com.salygin.ssubench.api.auth.AuthResponse;
import com.salygin.ssubench.api.auth.AuthUserResponse;
import com.salygin.ssubench.entity.Role;
import com.salygin.ssubench.entity.User;
import com.salygin.ssubench.repository.UserRepository;
import com.salygin.ssubench.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService tokenService;

    public AuthResponse register(String email, String rawPassword, Role role) {

        ensureEmailAvailable(email);

        UUID newUserId = UUID.randomUUID();
        String hashedPassword = encoder.encode(rawPassword);

        userRepo.insertUser(
                newUserId,
                email,
                hashedPassword,
                role.name(),
                0,
                false
        );

        User createdUser = fetchUserOrFail(newUserId);

        return assembleResponse(createdUser);
    }

    public AuthResponse login(String email, String rawPassword) {

        User existingUser = findByEmailOrFail(email);

        validatePassword(rawPassword, existingUser.getPassword());
        checkBlocked(existingUser);

        return assembleResponse(existingUser);
    }

    private void ensureEmailAvailable(String email) {
        if (userRepo.getByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists.");
        }
    }

    private User findByEmailOrFail(String email) {
        return userRepo.getByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private User fetchUserOrFail(UUID id) {
        return userRepo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private void validatePassword(String raw, String encoded) {
        if (!encoder.matches(raw, encoded)) {
            throw new IllegalArgumentException("Invalid credentials.");
        }
    }

    private void checkBlocked(User user) {
        if (Boolean.TRUE.equals(user.getBlocked())) {
            throw new IllegalArgumentException("User is blocked.");
        }
    }

    private AuthResponse assembleResponse(User user) {
        String jwt = tokenService.issueToken(user);

        return new AuthResponse(
                jwt,
                "Bearer",
                mapToDto(user)
        );
    }

    private AuthUserResponse mapToDto(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                Role.valueOf(user.getRole()),
                user.getBalance(),
                user.getBlocked()
        );
    }
}
