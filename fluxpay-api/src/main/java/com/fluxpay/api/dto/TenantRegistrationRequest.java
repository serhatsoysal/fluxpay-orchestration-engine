package com.fluxpay.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRegistrationRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    @NotBlank
    @Email
    private String billingEmail;

    @NotBlank
    @Email
    private String adminEmail;

    @NotBlank
    private String adminPassword;

    private String adminFirstName;
    private String adminLastName;
}

