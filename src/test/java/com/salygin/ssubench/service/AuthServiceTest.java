package com.salygin.ssubench.service;

import com.salygin.ssubench.api.auth.AuthResponse;
import com.salygin.ssubench.api.auth.AuthUserResponse;
import com.salygin.ssubench.entity.Role;
import com.salygin.ssubench.entity.User;
import com.salygin.ssubench.repository.UserRepository;
import com.salygin.ssubench.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private PasswordEncoder encoder;

    @BeforeEach
    void init() {
        encoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, encoder, jwtService);
    }

    @Test
    void shouldRegisterUserAndStoreEncodedSecret() {
        String email = "alpha.user@mail.com";
        String rawPassword = "P@ssw0rd!";
        Role role = Role.EXECUTOR;

        when(userRepository.getByEmail(email)).thenReturn(Optional.empty());

        when(userRepository.getById(any(UUID.class)))
                .thenAnswer(inv -> {
                    UUID id = inv.getArgument(0);
                    return Optional.of(new User(id, email, "stub", role.name(), 0, false));
                });

        AuthResponse response = authService.register(email, rawPassword, role);
        AuthUserResponse user = response.user();

        ArgumentCaptor<String> passCaptor = ArgumentCaptor.forClass(String.class);

        verify(userRepository).insertUser(
                any(UUID.class),
                eq(email),
                passCaptor.capture(),
                eq(role.name()),
                eq(0L),
                eq(false)
        );

        assertNotNull(user);
        assertEquals(email, user.email());
        assertEquals(role.name(), user.role().name());
        assertTrue(encoder.matches(rawPassword, passCaptor.getValue()));
    }

    @Test
    void shouldRejectRegistrationIfEmailAlreadyTaken() {
        String email = "taken@mail.com";

        when(userRepository.getByEmail(email))
                .thenReturn(Optional.of(new User(UUID.randomUUID(), email, "hash", "EXECUTOR", 0, false)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(email, "AnyPass123", Role.EXECUTOR)
        );

        assertEquals("User with this email already exists.", ex.getMessage());
    }

    @Test
    void shouldLoginSuccessfullyWithCorrectCredentials() {
        String email = "client@mail.com";
        String rawPassword = "MySecurePass1";
        String hashed = encoder.encode(rawPassword);

        User user = new User(UUID.randomUUID(), email, hashed, "CUSTOMER", 25, false);

        when(userRepository.getByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.issueToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(email, rawPassword);

        assertNotNull(response);
        assertNotNull(response.user());
        assertEquals(email, response.user().email());
        assertEquals(Role.CUSTOMER, response.user().role());
        assertEquals(25, response.user().balance());
        assertFalse(response.user().blocked());
    }

    @Test
    void shouldFailLoginIfUserIsBlocked() {
        String email = "blocked@mail.com";
        String rawPassword = "BlockedPass1";

        User user = new User(
                UUID.randomUUID(),
                email,
                encoder.encode(rawPassword),
                "CUSTOMER",
                50,
                true
        );

        when(userRepository.getByEmail(email)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(email, rawPassword)
        );

        assertEquals("User is blocked.", ex.getMessage());
    }

    @Test
    void shouldFailLoginWhenUserDoesNotExist() {
        String email = "missing@mail.com";

        when(userRepository.getByEmail(email)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(email, "nope123")
        );

        assertEquals("User not found.", ex.getMessage());
    }

    @Test
    void shouldFailLoginWhenPasswordDoesNotMatch() {
        String email = "executor@mail.com";
        String correctPassword = "Correct#123";
        String wrongPassword = "Wrong#123";

        User user = new User(
                UUID.randomUUID(),
                email,
                encoder.encode(correctPassword),
                "EXECUTOR",
                15,
                false
        );

        when(userRepository.getByEmail(email)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(email, wrongPassword)
        );

        assertEquals("Invalid credentials.", ex.getMessage());
    }
}

