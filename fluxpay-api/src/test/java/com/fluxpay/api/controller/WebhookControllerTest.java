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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        webhook.setTenantId(tenantId);
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
        webhook.setTenantId(tenantId);
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

    @Test
    void updateWebhook_ShouldReturnOkWhenExists() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setTenantId(tenantId);
        webhook.setUrl("https://example.com/old-webhook");
        webhook.setActive(true);
        webhook.setDeletedAt(null);

        com.fluxpay.api.dto.UpdateWebhookRequest request = new com.fluxpay.api.dto.UpdateWebhookRequest();
        request.setUrl("https://example.com/new-webhook");
        request.setEvents(List.of("invoice.created"));
        request.setActive(true);
        request.setSecret("new-secret-key-12345");

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));
        when(webhookEndpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<WebhookEndpoint> response = webhookController.updateWebhook(webhookId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUrl()).isEqualTo("https://example.com/new-webhook");
        verify(webhookEndpointRepository).save(argThat(w -> w.getUrl().equals("https://example.com/new-webhook")));
    }

    @Test
    void updateWebhook_ShouldReturnNotFoundWhenNotExists() {
        UUID webhookId = UUID.randomUUID();
        com.fluxpay.api.dto.UpdateWebhookRequest request = new com.fluxpay.api.dto.UpdateWebhookRequest();
        request.setUrl("https://example.com/webhook");
        request.setEvents(List.of("invoice.created"));

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.empty());

        try {
            webhookController.updateWebhook(webhookId, request);
        } catch (com.fluxpay.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Webhook");
        }

        verify(webhookEndpointRepository, never()).save(any());
    }

    @Test
    void updateWebhook_ShouldReturnNotFoundWhenDeleted() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setTenantId(tenantId);
        webhook.setDeletedAt(Instant.now());

        com.fluxpay.api.dto.UpdateWebhookRequest request = new com.fluxpay.api.dto.UpdateWebhookRequest();
        request.setUrl("https://example.com/webhook");
        request.setEvents(List.of("invoice.created"));

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));

        try {
            webhookController.updateWebhook(webhookId, request);
        } catch (com.fluxpay.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Webhook");
        }

        verify(webhookEndpointRepository, never()).save(any());
    }

    @Test
    void updateWebhook_ShouldReturnNotFoundWhenDifferentTenant() {
        UUID webhookId = UUID.randomUUID();
        UUID differentTenantId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setTenantId(differentTenantId);
        webhook.setDeletedAt(null);

        com.fluxpay.api.dto.UpdateWebhookRequest request = new com.fluxpay.api.dto.UpdateWebhookRequest();
        request.setUrl("https://example.com/webhook");
        request.setEvents(List.of("invoice.created"));

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));

        try {
            webhookController.updateWebhook(webhookId, request);
        } catch (com.fluxpay.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Webhook");
        }

        verify(webhookEndpointRepository, never()).save(any());
    }

    @Test
    void updateWebhook_ShouldUpdateAllFields() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setTenantId(tenantId);
        webhook.setUrl("https://example.com/old-webhook");
        webhook.setActive(false);
        webhook.setDeletedAt(null);

        com.fluxpay.api.dto.UpdateWebhookRequest request = new com.fluxpay.api.dto.UpdateWebhookRequest();
        request.setUrl("https://example.com/new-webhook");
        request.setEvents(List.of("invoice.created", "payment.succeeded"));
        request.setActive(true);
        request.setSecret("new-secret-12345678");

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));
        when(webhookEndpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<WebhookEndpoint> response = webhookController.updateWebhook(webhookId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookEndpointRepository).save(argThat(w ->
                w.getUrl().equals("https://example.com/new-webhook") &&
                w.getActive().equals(true) &&
                w.getSecret().equals("new-secret-12345678")
        ));
    }

    @Test
    void updateWebhook_WithShortSecret_ShouldThrowException() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setTenantId(tenantId);
        webhook.setDeletedAt(null);

        com.fluxpay.api.dto.UpdateWebhookRequest request = new com.fluxpay.api.dto.UpdateWebhookRequest();
        request.setUrl("https://example.com/webhook");
        request.setEvents(List.of("invoice.created"));
        request.setSecret("short");

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));

        assertThatThrownBy(() -> webhookController.updateWebhook(webhookId, request))
                .isInstanceOf(com.fluxpay.common.exception.ValidationException.class)
                .hasMessageContaining("Secret must be at least 16 characters");

        verify(webhookEndpointRepository, never()).save(any());
    }

    @Test
    void updateWebhook_WithInvalidEventType_ShouldThrowException() {
        UUID webhookId = UUID.randomUUID();
        WebhookEndpoint webhook = new WebhookEndpoint();
        webhook.setId(webhookId);
        webhook.setTenantId(tenantId);
        webhook.setDeletedAt(null);

        com.fluxpay.api.dto.UpdateWebhookRequest request = new com.fluxpay.api.dto.UpdateWebhookRequest();
        request.setUrl("https://example.com/webhook");
        request.setEvents(List.of("invalid.event.type"));

        when(webhookEndpointRepository.findById(webhookId)).thenReturn(Optional.of(webhook));

        assertThatThrownBy(() -> webhookController.updateWebhook(webhookId, request))
                .isInstanceOf(com.fluxpay.common.exception.ValidationException.class)
                .hasMessageContaining("Invalid event type");

        verify(webhookEndpointRepository, never()).save(any());
    }
}

