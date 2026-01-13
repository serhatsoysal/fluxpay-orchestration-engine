package com.fluxpay.security.jwt;

import com.fluxpay.security.context.TenantContext;
import com.fluxpay.security.session.model.DeviceInfo;
import com.fluxpay.security.session.model.SecurityFlags;
import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.service.DeviceFingerprintService;
import com.fluxpay.security.session.service.SessionSecurityService;
import com.fluxpay.security.session.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final SessionSecurityService sessionSecurityService;
    private final DeviceFingerprintService deviceFingerprintService;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SessionService sessionService,
            SessionSecurityService sessionSecurityService,
            DeviceFingerprintService deviceFingerprintService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
        this.sessionSecurityService = sessionSecurityService;
        this.deviceFingerprintService = deviceFingerprintService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                if (sessionService.isTokenBlacklisted(token)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UUID userId;
                UUID tenantId;
                String role;
                String sessionId;
                
                try {
                    userId = jwtTokenProvider.getUserId(token);
                    tenantId = jwtTokenProvider.getTenantId(token);
                    role = jwtTokenProvider.getRole(token);
                    sessionId = jwtTokenProvider.getSessionId(token);
                } catch (IllegalArgumentException e) {
                    filterChain.doFilter(request, response);
                    return;
                }

                SessionData session = retrieveOrCreateSession(token, userId, tenantId, role, sessionId, request);

                TenantContext.setCurrentTenant(tenantId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        session,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                try {
                    UUID userId = jwtTokenProvider.getUserId(token);
                    UUID tenantId = jwtTokenProvider.getTenantId(token);
                    String role = jwtTokenProvider.getRole(token);
                    
                    TenantContext.setCurrentTenant(tenantId);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (IllegalArgumentException ex) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
    
    private SessionData buildSessionData(String token, UUID userId, UUID tenantId, String role, 
                                         String sessionId, HttpServletRequest request, String deviceFingerprint) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        
        DeviceInfo deviceInfo = null;
        String ipAddress = null;
        
        try {
            deviceInfo = deviceFingerprintService.extractDeviceInfo(request);
            ipAddress = deviceFingerprintService.getClientIpAddress(request);
        } catch (Exception ignored) {
        }
        
        return SessionData.builder()
                .sessionId(sessionId)
                .accessToken(token)
                .refreshToken(null)
                .userId(userId)
                .tenantId(tenantId)
                .role(role)
                .deviceInfo(deviceInfo)
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .userAgent(request.getHeader("User-Agent"))
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
    }

    private SessionData retrieveOrCreateSession(String token, UUID userId, UUID tenantId, String role, 
                                                String sessionId, HttpServletRequest request) {
        SessionData session = null;
        
        try {
            if (sessionId != null) {
                session = sessionService.getSession(tenantId, userId, sessionId);
            }
        } catch (Exception ignored) {
        }
        
        if (session == null) {
            return createNewSession(token, userId, tenantId, role, sessionId, request);
        } else {
            updateExistingSession(session, request);
            return session;
        }
    }

    private SessionData createNewSession(String token, UUID userId, UUID tenantId, String role, 
                                         String sessionId, HttpServletRequest request) {
        try {
            String deviceFingerprint = deviceFingerprintService.generateFingerprint(request);
            SessionData newSession = buildSessionData(token, userId, tenantId, role, sessionId, request, deviceFingerprint);
            sessionService.createSession(newSession);
            return newSession;
        } catch (Exception ignored) {
            return buildSessionData(token, userId, tenantId, role, sessionId, request, null);
        }
    }

    private void updateExistingSession(SessionData session, HttpServletRequest request) {
        try {
            String deviceFingerprint = deviceFingerprintService.generateFingerprint(request);
            if (!sessionSecurityService.verifyDeviceFingerprint(session, deviceFingerprint)) {
                sessionSecurityService.recordSuspiciousActivity(session, "Device fingerprint mismatch");
            }
            sessionService.updateLastAccess(session);
        } catch (Exception ignored) {
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

