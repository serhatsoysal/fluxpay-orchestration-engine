package com.fluxpay.api.controller;

import com.fluxpay.api.dto.LoginRequest;
import com.fluxpay.api.dto.LoginResponse;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.jwt.JwtTokenProvider;
import com.fluxpay.tenant.entity.User;
import com.fluxpay.tenant.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getUserByEmail(request.getEmail());

        if (!userService.verifyPassword(user, request.getPassword())) {
            throw new ValidationException("Invalid credentials");
        }

        String token = jwtTokenProvider.createToken(
                user.getId(),
                user.getTenantId(),
                user.getRole().name()
        );

        LoginResponse response = new LoginResponse(
                token,
                user.getId().toString(),
                user.getTenantId().toString(),
                user.getRole().name()
        );

        return ResponseEntity.ok(response);
    }
}

