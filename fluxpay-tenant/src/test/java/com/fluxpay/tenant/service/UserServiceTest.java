package com.fluxpay.tenant.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.tenant.entity.User;
import com.fluxpay.tenant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded-password");
        user.setDeletedAt(null);
    }

    @Test
    void createUser_ShouldReturnSavedUser() {
        User newUser = new User();
        newUser.setTenantId(tenantId);
        newUser.setEmail("new@example.com");
        String plainPassword = "password123";

        when(userRepository.existsByTenantIdAndEmail(tenantId, "new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(plainPassword)).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        User result = userService.createUser(newUser, plainPassword);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getPasswordHash()).isEqualTo("encoded-password");
        verify(passwordEncoder).encode(plainPassword);
        verify(userRepository).existsByTenantIdAndEmail(tenantId, "new@example.com");
        verify(userRepository).save(newUser);
    }

    @Test
    void createUser_WhenEmailExists_ShouldThrowException() {
        User newUser = new User();
        newUser.setTenantId(tenantId);
        newUser.setEmail("existing@example.com");

        when(userRepository.existsByTenantIdAndEmail(tenantId, "existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(newUser, "password123"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_WhenNotFound_ShouldThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_WhenDeleted_ShouldThrowException() {
        user.setDeletedAt(Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findById(userId);
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getUserByEmail_WhenNotFound_ShouldThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void getUserByEmail_WhenDeleted_ShouldThrowException() {
        user.setDeletedAt(Instant.now());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getUserByEmail("test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getUserByTenantAndEmail_ShouldReturnUser() {
        when(userRepository.findByTenantIdAndEmail(tenantId, "test@example.com"))
                .thenReturn(Optional.of(user));

        User result = userService.getUserByTenantAndEmail(tenantId, "test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        verify(userRepository).findByTenantIdAndEmail(tenantId, "test@example.com");
    }

    @Test
    void getUserByTenantAndEmail_WhenNotFound_ShouldThrowException() {
        when(userRepository.findByTenantIdAndEmail(tenantId, "nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByTenantAndEmail(tenantId, "nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByTenantIdAndEmail(tenantId, "nonexistent@example.com");
    }

    @Test
    void getUserByTenantAndEmail_WhenDeleted_ShouldThrowException() {
        user.setDeletedAt(Instant.now());
        when(userRepository.findByTenantIdAndEmail(tenantId, "test@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getUserByTenantAndEmail(tenantId, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByTenantIdAndEmail(tenantId, "test@example.com");
    }

    @Test
    void verifyPassword_WithCorrectPassword_ShouldReturnTrue() {
        String plainPassword = "password123";

        when(passwordEncoder.matches(plainPassword, "encoded-password")).thenReturn(true);

        boolean result = userService.verifyPassword(user, plainPassword);

        assertThat(result).isTrue();
        verify(passwordEncoder).matches(plainPassword, "encoded-password");
    }

    @Test
    void verifyPassword_WithIncorrectPassword_ShouldReturnFalse() {
        String plainPassword = "wrong-password";

        when(passwordEncoder.matches(plainPassword, "encoded-password")).thenReturn(false);

        boolean result = userService.verifyPassword(user, plainPassword);

        assertThat(result).isFalse();
        verify(passwordEncoder).matches(plainPassword, "encoded-password");
    }

    @Test
    void updateLastLogin_ShouldUpdateFields() {
        String ipAddress = "192.168.1.1";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateLastLogin(userId, ipAddress);

        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(user.getLastLoginIp()).isEqualTo(ipAddress);
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void updateLastLogin_WhenNotFound_ShouldThrowException() {
        String ipAddress = "192.168.1.1";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateLastLogin(userId, ipAddress))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_ShouldSoftDelete() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(userId);

        assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_WhenNotFound_ShouldThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}

