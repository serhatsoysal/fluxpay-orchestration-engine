package com.fluxpay.tenant.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.tenant.entity.User;
import com.fluxpay.tenant.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user, String plainPassword) {
        if (userRepository.existsByTenantIdAndEmail(user.getTenantId(), user.getEmail())) {
            throw new ValidationException("User with email already exists in this tenant: " + user.getEmail());
        }

        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));
    }

    @Transactional(readOnly = true)
    public User getUserByTenantAndEmail(UUID tenantId, String email) {
        return userRepository.findByTenantIdAndEmail(tenantId, email)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));
    }

    public boolean verifyPassword(User user, String plainPassword) {
        return passwordEncoder.matches(plainPassword, user.getPasswordHash());
    }

    public void updateLastLogin(UUID userId, String ipAddress) {
        User user = getUserById(userId);
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        User user = getUserById(id);
        user.softDelete();
        userRepository.save(user);
    }
}

