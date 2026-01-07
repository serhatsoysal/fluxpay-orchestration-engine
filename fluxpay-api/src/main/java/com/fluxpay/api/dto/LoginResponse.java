package com.fluxpay.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String userId;
    private String tenantId;
    private String role;
}

