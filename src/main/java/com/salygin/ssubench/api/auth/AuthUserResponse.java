package com.salygin.ssubench.api.auth;

import com.salygin.ssubench.entity.Role;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        Role role,
        Integer balance,
        Boolean blocked
) {
}