package com.fluxpay.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshTokenResponse {
    private String token;
    private String refreshToken;
    private String sessionId;
    private long expiresIn;
    private long refreshExpiresIn;
}

