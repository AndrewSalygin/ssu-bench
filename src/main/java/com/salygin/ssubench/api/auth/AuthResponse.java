package com.salygin.ssubench.api.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        AuthUserResponse user
) {
}