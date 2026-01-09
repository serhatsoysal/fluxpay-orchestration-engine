package com.fluxpay.api.dto;

import com.fluxpay.security.session.model.DeviceInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class SessionInfoResponse {
    private String sessionId;
    private DeviceInfo deviceInfo;
    private String ipAddress;
    private Instant createdAt;
    private Instant lastAccess;
}

