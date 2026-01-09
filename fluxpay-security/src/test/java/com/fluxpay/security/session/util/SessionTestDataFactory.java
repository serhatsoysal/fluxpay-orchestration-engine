package com.fluxpay.security.session.util;

import com.fluxpay.security.session.model.DeviceInfo;
import com.fluxpay.security.session.model.LocationInfo;
import com.fluxpay.security.session.model.SecurityFlags;
import com.fluxpay.security.session.model.SessionData;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class SessionTestDataFactory {

    public static SessionData createSessionData() {
        return SessionData.builder()
                .sessionId(UUID.randomUUID().toString())
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .userId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .role("USER")
                .deviceInfo(createDeviceInfo())
                .locationInfo(createLocationInfo())
                .tokenFingerprint("test-token-fingerprint")
                .deviceFingerprint("test-device-fingerprint")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .securityFlags(createSecurityFlags())
                .createdAt(Instant.now())
                .lastAccess(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .refreshTokenExpiresAt(Instant.now().plusSeconds(86400))
                .requestCount(0)
                .lastRequestTime(Instant.now())
                .metadata(new HashMap<>())
                .build();
    }

    public static SessionData createSessionData(UUID tenantId, UUID userId) {
        return SessionData.builder()
                .sessionId(UUID.randomUUID().toString())
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .userId(userId)
                .tenantId(tenantId)
                .role("USER")
                .deviceInfo(createDeviceInfo())
                .locationInfo(createLocationInfo())
                .tokenFingerprint("test-token-fingerprint")
                .deviceFingerprint("test-device-fingerprint")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .securityFlags(createSecurityFlags())
                .createdAt(Instant.now())
                .lastAccess(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .refreshTokenExpiresAt(Instant.now().plusSeconds(86400))
                .requestCount(0)
                .lastRequestTime(Instant.now())
                .metadata(new HashMap<>())
                .build();
    }

    public static DeviceInfo createDeviceInfo() {
        return DeviceInfo.builder()
                .deviceId("test-device-id")
                .deviceType("desktop")
                .deviceName("Windows PC")
                .os("Windows")
                .osVersion("10")
                .browser("Chrome")
                .browserVersion("120")
                .build();
    }

    public static LocationInfo createLocationInfo() {
        return LocationInfo.builder()
                .country("US")
                .region("California")
                .city("San Francisco")
                .timezone("America/Los_Angeles")
                .latitude(37.7749)
                .longitude(-122.4194)
                .build();
    }

    public static SecurityFlags createSecurityFlags() {
        return SecurityFlags.builder()
                .suspiciousActivity(false)
                .requiresReauth(false)
                .mfaRequired(false)
                .failedAttempts(0)
                .lastSecurityCheck(Instant.now())
                .build();
    }

    public static HttpServletRequest createMockHttpServletRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        when(request.getHeader("Accept-Language")).thenReturn("en-US,en;q=0.9");
        when(request.getHeader("Accept-Encoding")).thenReturn("gzip, deflate, br");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        return request;
    }

    public static HttpServletRequest createMockMobileRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1");
        when(request.getHeader("Accept-Language")).thenReturn("en-US,en;q=0.9");
        when(request.getHeader("Accept-Encoding")).thenReturn("gzip, deflate");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.101");
        return request;
    }

    public static HttpServletRequest createMockTabletRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1");
        when(request.getHeader("Accept-Language")).thenReturn("en-US,en;q=0.9");
        when(request.getHeader("Accept-Encoding")).thenReturn("gzip, deflate");
        when(request.getRemoteAddr()).thenReturn("192.168.1.102");
        return request;
    }
}

