package com.fluxpay.api.controller;

import com.fluxpay.notification.entity.WebhookEndpoint;
import com.fluxpay.notification.repository.WebhookEndpointRepository;
import com.fluxpay.security.context.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
        return webhookEndpointRepository.findById(id)
                .filter(w -> w.getDeletedAt() == null)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable UUID id) {
        return webhookEndpointRepository.findById(id)
                .map(webhook -> {
                    webhook.setActive(false);
                    webhookEndpointRepository.save(webhook);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

