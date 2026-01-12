package com.fluxpay.api.controller;

import com.fluxpay.notification.entity.WebhookEndpoint;
import com.fluxpay.notification.repository.WebhookEndpointRepository;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private WebhookEndpointRepository webhookEndpointRepository;

    @InjectMocks
    private WebhookController webhookController;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createWebhook_ShouldReturnCreated() {
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setUrl("https://example.com/webhook");
        webhook.setActive(true);

        when(webhookEndpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<WebhookEndpoint> response = webhookController.createWebhook(webhook);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(webhook);
        verify(webhookEndpointRepository).save(webhook);
    }

    @Test
    void getWebhooks_ShouldReturnList() {
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(UUID.randomUUID());
        webhook.setUrl("https://example.com/webhook");
        webhook.setActive(true);

        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of(webhook));

        ResponseEntity<List<WebhookEndpoint>> response = webhookController.getWebhooks();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(webhookEndpointRepository).findByTenantIdAndActiveTrue(tenantId);
    }

    @Test
    void getWebhook_ShouldReturnWebhookWhenExists() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setUrl("https://example.com/webhook");

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));

        ResponseEntity<WebhookEndpoint> response = webhookController.getWebhook(webhookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(webhook);
    }

    @Test
    void getWebhook_ShouldReturnNotFoundWhenNotExists() {
        UUID webhookId = UUID.randomUUID();

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.empty());

        ResponseEntity<WebhookEndpoint> response = webhookController.getWebhook(webhookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getWebhook_ShouldReturnNotFoundWhenDeleted() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setDeletedAt(Instant.now());

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));

        ResponseEntity<WebhookEndpoint> response = webhookController.getWebhook(webhookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteWebhook_ShouldDeactivateWhenExists() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setActive(true);

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));
        when(webhookEndpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Void> response = webhookController.deleteWebhook(webhookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(webhookEndpointRepository).save(argThat(w -> Boolean.FALSE.equals(w.getActive())));
    }

    @Test
    void deleteWebhook_ShouldReturnNotFoundWhenNotExists() {
        UUID webhookId = UUID.randomUUID();

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = webhookController.deleteWebhook(webhookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(webhookEndpointRepository, never()).save(any());
    }
}

