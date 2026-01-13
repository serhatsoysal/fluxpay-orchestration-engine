package com.fluxpay.security.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionData implements Serializable {
    private String sessionId;
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private UUID tenantId;
    private String role;
    
    private DeviceInfo deviceInfo;
    private LocationInfo locationInfo;
    
    private String tokenFingerprint;
    private String deviceFingerprint;
    private String ipAddress;
    private String userAgent;
    private SecurityFlags securityFlags;
    
    private Instant createdAt;
    private Instant lastAccess;
    private Instant expiresAt;
    private Instant refreshTokenExpiresAt;
    
    private long requestCount;
    private Instant lastRequestTime;
    private transient Map<String, Object> metadata;
}

