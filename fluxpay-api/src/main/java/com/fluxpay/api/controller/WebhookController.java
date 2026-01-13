package com.fluxpay.api.controller;

import com.fluxpay.api.dto.UpdateWebhookRequest;
import com.fluxpay.common.enums.WebhookEventType;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.notification.entity.WebhookEndpoint;
import com.fluxpay.notification.repository.WebhookEndpointRepository;
import com.fluxpay.security.context.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookEndpointRepository webhookEndpointRepository;

    public WebhookController(WebhookEndpointRepository webhookEndpointRepository) {
        this.webhookEndpointRepository = webhookEndpointRepository;
    }

    @PostMapping
    public ResponseEntity<WebhookEndpoint> createWebhook(@Valid @RequestBody WebhookEndpoint webhook) {
        WebhookEndpoint created = webhookEndpointRepository.save(webhook);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<WebhookEndpoint>> getWebhooks() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<WebhookEndpoint> webhooks = webhookEndpointRepository.findByTenantIdAndActiveTrue(tenantId);
        return ResponseEntity.ok(webhooks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookEndpoint> getWebhook(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return webhookEndpointRepository.findById(id)
                .filter(w -> w.getDeletedAt() == null && w.getTenantId() != null && w.getTenantId().equals(tenantId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookEndpoint> updateWebhook(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWebhookRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        WebhookEndpoint webhook = webhookEndpointRepository.findById(id)
                .filter(w -> w.getDeletedAt() == null && w.getTenantId() != null && w.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", id));
        
        if (request.getSecret() != null && request.getSecret().length() < 16) {
            throw new ValidationException("Secret must be at least 16 characters");
        }
        
        List<WebhookEventType> events = request.getEvents().stream()
                .map(eventName -> {
                    try {
                        return WebhookEventType.valueOf(eventName.toUpperCase().replace(".", "_"));
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Invalid event type: " + eventName);
                    }
                })
                .collect(Collectors.toList());
        
        webhook.setUrl(request.getUrl());
        webhook.setEnabledEvents(events);
        webhook.setActive(request.getActive());
        if (request.getSecret() != null) {
            webhook.setSecret(request.getSecret());
        }
        
        WebhookEndpoint updated = webhookEndpointRepository.save(webhook);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return webhookEndpointRepository.findById(id)
                .filter(w -> w.getTenantId().equals(tenantId))
                .map(webhook -> {
                    webhook.setActive(false);
                    webhookEndpointRepository.save(webhook);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

