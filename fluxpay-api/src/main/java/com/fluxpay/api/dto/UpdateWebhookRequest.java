package com.fluxpay.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UpdateWebhookRequest {
    @NotNull
    @Pattern(regexp = "^https://.*", message = "URL must be HTTPS")
    private String url;

    @NotEmpty
    private List<String> events;

    @NotNull
    private Boolean active;

    private String secret;
}

