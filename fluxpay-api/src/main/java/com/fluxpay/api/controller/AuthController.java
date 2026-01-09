package com.fluxpay.api.controller;

import com.fluxpay.api.dto.LoginRequest;
import com.fluxpay.api.dto.LoginResponse;
import com.fluxpay.api.dto.RefreshTokenRequest;
import com.fluxpay.api.dto.RefreshTokenResponse;
import com.fluxpay.api.dto.SessionInfoResponse;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.security.jwt.JwtTokenProvider;
import com.fluxpay.security.session.config.SessionProperties;
import com.fluxpay.security.session.model.DeviceInfo;
import com.fluxpay.security.session.model.SecurityFlags;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.service.DeviceFingerprintService;
import com.fluxpay.security.session.service.SessionService;
import com.fluxpay.tenant.entity.User;
import com.fluxpay.tenant.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final SessionProperties sessionProperties;

    public AuthController(
            UserService userService,
            JwtTokenProvider jwtTokenProvider,
            SessionService sessionService,
            DeviceFingerprintService deviceFingerprintService,
            SessionProperties sessionProperties) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
        this.deviceFingerprintService = deviceFingerprintService;
        this.sessionProperties = sessionProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.getUserByEmail(request.getEmail());

        if (!userService.verifyPassword(user, request.getPassword())) {
            throw new ValidationException("Invalid credentials");
        }

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        
        String accessToken = jwtTokenProvider.createToken(
                user.getId(),
                user.getTenantId(),
                user.getRole().name(),
                sessionId
        );

        DeviceInfo deviceInfo = deviceFingerprintService.extractDeviceInfo(httpRequest);
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
        String ipAddress = deviceFingerprintService.getClientIpAddress(httpRequest);

        SessionData sessionData = SessionData.builder()
                .sessionId(sessionId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .role(user.getRole().name())
                .deviceInfo(deviceInfo)
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .userAgent(httpRequest.getHeader("User-Agent"))
                .securityFlags(SecurityFlags.builder()
                        .suspiciousActivity(false)
                        .requiresReauth(false)
                        .mfaRequired(false)
                        .failedAttempts(0)
                        .lastSecurityCheck(Instant.now())
                        .build())
                .createdAt(Instant.now())
                .lastAccess(Instant.now())
                .requestCount(0)
                .build();

        sessionService.createSession(sessionData);

        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                sessionId,
                user.getId().toString(),
                user.getTenantId().toString(),
                user.getRole().name(),
                sessionProperties.getTtl().getAccessToken().toMillis(),
                sessionProperties.getTtl().getRefreshToken().toMillis()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        UUID userId = getCurrentUserId();
        UUID tenantId = TenantContext.getCurrentTenantId();
        String sessionId = extractSessionId();
        
        if (sessionId != null) {
            sessionService.invalidateSession(tenantId, userId, sessionId);
        }
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        UUID userId = getCurrentUserId();
        UUID tenantId = TenantContext.getCurrentTenantId();
        String currentSessionId = extractSessionId();
        
        sessionService.invalidateAllUserSessions(tenantId, userId, currentSessionId);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
        
        SessionData session = sessionService.refreshSession(request.getRefreshToken(), deviceFingerprint);
        
        RefreshTokenResponse response = new RefreshTokenResponse(
                session.getAccessToken(),
                session.getRefreshToken(),
                session.getSessionId(),
                sessionProperties.getTtl().getAccessToken().toMillis(),
                sessionProperties.getTtl().getRefreshToken().toMillis()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfoResponse>> getSessions() {
        UUID userId = getCurrentUserId();
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        List<SessionData> sessions = sessionService.getUserSessions(tenantId, userId);
        
        List<SessionInfoResponse> response = sessions.stream()
                .map(session -> new SessionInfoResponse(
                        session.getSessionId(),
                        session.getDeviceInfo(),
                        session.getIpAddress(),
                        session.getCreatedAt(),
                        session.getLastAccess()
                ))
                .toList();
        
        return ResponseEntity.ok(response);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) authentication.getPrincipal();
    }

    private String extractSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getCredentials() instanceof SessionData sessionData) {
            return sessionData.getSessionId();
        }
        return null;
    }
}

