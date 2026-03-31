package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Role;
import com.salygin.ssubench.entity.User;
import com.salygin.ssubench.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserAccessService {

    private final UserRepository userRepo;

    public User getExistingUser(UUID id) {
        return userRepo.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public boolean isAdmin(UUID id) {
        return Role.ADMIN.name().equals(extractRole(id));
    }

    public void checkAccess(UUID actorId, UUID ownerId) {

        boolean isOwner = actorId.equals(ownerId);
        boolean hasAdminRights = isAdmin(actorId);

        if (!(isOwner || hasAdminRights)) {
            throw new IllegalArgumentException("Access denied.");
        }
    }

    private String extractRole(UUID userId) {
        User user = getExistingUser(userId);
        return user.getRole();
    }
}
