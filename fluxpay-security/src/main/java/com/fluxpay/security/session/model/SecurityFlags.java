package com.fluxpay.security.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityFlags implements Serializable {
    private boolean suspiciousActivity;
    private boolean requiresReauth;
    private boolean mfaRequired;
    private int failedAttempts;
    private Instant lastSecurityCheck;
}

