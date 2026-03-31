package com.salygin.ssubench.security;

import java.util.UUID;

public record JwtPrincipal(
        UUID id,
        String email,
        String role
) {
}