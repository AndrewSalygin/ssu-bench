package com.salygin.ssubench.service;

import com.salygin.ssubench.entity.Role;
import com.salygin.ssubench.entity.User;
import com.salygin.ssubench.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserAccessService accessService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, accessService);
    }

    @Test
    void createUser_encodesPasswordAndPersistsUser() {
        String email = "alpha.user@mail.com";
        String rawPassword = "qwerty!@#456";
        Role role = Role.CUSTOMER;

        when(userRepository.getById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    return Optional.of(new User(id, email, "secured", role.name(), 0, false));
                });

        User result = userService.createUser(email, rawPassword, role);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        verify(userRepository).insertUser(
                any(UUID.class),
                eq(email),
                passwordCaptor.capture(),
                eq(role.name()),
                eq(0L),
                eq(false)
        );

        assertEquals(email, result.getEmail());
        assertEquals(role.name(), result.getRole());
        assertTrue(passwordEncoder.matches(rawPassword, passwordCaptor.getValue()));
    }

    @Test
    void getUser_returnsUserWhenAccessAllowed() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        User user = new User(id, "viewer@mail.net", "hash123", "EXECUTOR", 5, false);

        doNothing().when(accessService).checkAccess(actorId, id);
        when(userRepository.getById(id)).thenReturn(Optional.of(user));

        User result = userService.getUser(id, actorId);

        assertSame(user, result);
    }

    @Test
    void updateUser_updatesFieldsAndPassword() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        String email = "updated.user@mail.net";
        String rawPassword = "newSecurePass!";
        Role role = Role.ADMIN;
        long balance = 42;

        User existing = new User(id, "legacy@mail.net", "oldHash", "CUSTOMER", 15, false);
        User updated = new User(id, email, "freshHash", role.name(), 15, false);

        doNothing().when(accessService).checkAccess(actorId, id);
        when(userRepository.getById(id)).thenReturn(Optional.of(existing), Optional.of(updated));

        User result = userService.updateUser(id, actorId, email, rawPassword, role, balance);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        verify(userRepository).modify(eq(id), eq(email), passwordCaptor.capture(), eq(role.name()), eq(balance));

        assertEquals(email, result.getEmail());
        assertEquals(role.name(), result.getRole());
        assertTrue(passwordEncoder.matches(rawPassword, passwordCaptor.getValue()));
    }

    @Test
    void updateUserBalance_updatesWhenValidValue() {
        UUID id = UUID.randomUUID();

        User existing = new User(id, "wallet@mail.org", "hash", "CUSTOMER", 12, false);
        User updated = new User(id, "wallet@mail.org", "hash", "CUSTOMER", 99, false);

        when(userRepository.getById(id)).thenReturn(Optional.of(existing), Optional.of(updated));

        User result = userService.updateUserBalance(id, 99);

        verify(userRepository).setBalance(id, 99L);
        assertEquals(99, result.getBalance());
    }

    @Test
    void updateUserBalance_throwsWhenInvalidValue() {
        UUID id = UUID.randomUUID();

        when(userRepository.getById(id))
                .thenReturn(Optional.of(new User(id, "fail@mail.org", "hash", "CUSTOMER", 8, false)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserBalance(id, -5));

        assertEquals("Invalid balance.", ex.getMessage());
        verify(userRepository, never()).setBalance(any(), anyLong());
    }

    @Test
    void deleteUser_removesUserWhenAllowed() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(userRepository.getById(id))
                .thenReturn(Optional.of(new User(id, "remove@mail.com", "hash", "CUSTOMER", 0, false)));

        userService.deleteUser(id, actorId);

        verify(userRepository).remove(id);
    }

    @Test
    void getAllUsers_returnsList() {
        List<User> users = List.of(
                new User(UUID.randomUUID(), "one@mail.com", "hash", "CUSTOMER", 1, false),
                new User(UUID.randomUUID(), "two@mail.com", "hash", "ADMIN", 200, false)
        );

        when(userRepository.getBatch(10, 5)).thenReturn(users);

        List<User> result = userService.getAllUsers(10, 5);

        assertEquals(users, result);
    }

    @Test
    void blockAndUnblockUser_executeRepositoryCalls() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(userRepository.getById(userId))
                .thenReturn(Optional.of(new User(userId, "lock@mail.com", "hash", "CUSTOMER", 0, false)))
                .thenReturn(Optional.of(new User(userId, "lock@mail.com", "hash", "CUSTOMER", 0, true)))
                .thenReturn(Optional.of(new User(userId, "lock@mail.com", "hash", "CUSTOMER", 0, false)));

        userService.blockUser(userId, adminId);
        userService.unblockUser(userId);

        verify(userRepository).markBlocked(userId);
        verify(userRepository).markUnblocked(userId);
    }

    @Test
    void blockUser_whenAdminBlocksHimself_throwsException() {
        UUID id = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> userService.blockUser(id, id));
    }

    @Test
    void getUser_throwsWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        doNothing().when(accessService).checkAccess(actorId, id);
        when(userRepository.getById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.getUser(id, actorId));

        assertEquals("User not found.", ex.getMessage());
    }
}

