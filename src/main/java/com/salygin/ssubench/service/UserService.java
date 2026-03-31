package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Role;
import com.salygin.ssubench.entity.User;
import com.salygin.ssubench.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final CurrentUserAccessService access;

    public User createUser(String email, String rawPassword, Role role) {

        UUID userId = UUID.randomUUID();
        String encoded = encoder.encode(rawPassword);

        repo.insertUser(
                userId,
                email,
                encoded,
                role.name(),
                0,
                false
        );

        return loadUser(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(UUID userId,
                           UUID actorId,
                           String email,
                           String rawPassword,
                           Role role,
                           long balance) {

        access.checkAccess(actorId, userId);

        User current = loadUser(userId);
        String encoded = encoder.encode(rawPassword);

        repo.modify(
                userId,
                email,
                encoded,
                role.name(),
                balance
        );

        return loadUser(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserBalance(UUID userId, long balance) {

        ensureUserExists(userId);
        validateBalance(balance);

        repo.setBalance(userId, balance);

        return loadUser(userId);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public void deleteUser(UUID userId, UUID actorId) {

        access.checkAccess(actorId, userId);
        ensureUserExists(userId);

        repo.remove(userId);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public User getUser(UUID userId, UUID actorId) {

        access.checkAccess(actorId, userId);
        return loadUser(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers(int limit, int offset) {
        return repo.getBatch(limit, offset);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void blockUser(UUID userId, UUID actorId) {
        if (userId.equals(actorId)) {
            throw new IllegalArgumentException("Admin cannot block himself.");
        }

        ensureUserExists(userId);
        repo.markBlocked(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void unblockUser(UUID userId) {
        ensureUserExists(userId);
        repo.markUnblocked(userId);
    }

    private User loadUser(UUID id) {
        return repo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private void ensureUserExists(UUID id) {
        repo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private void validateBalance(long balance) {
        if (balance <= 0) {
            throw new IllegalArgumentException("Invalid balance.");
        }
    }
}
