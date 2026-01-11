package com.fluxpay.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxpay.common.enums.WebhookEventType;
import com.fluxpay.notification.entity.WebhookEndpoint;
import com.fluxpay.notification.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookEndpointRepository webhookEndpointRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    @Test
    void sendWebhook_WithActiveEndpoints_ShouldSendWebhook() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId(UUID.randomUUID());
        endpoint.setTenantId(tenantId);
        endpoint.setUrl("https://example.com/webhook");
        endpoint.setEnabledEvents(List.of(WebhookEventType.SUBSCRIPTION_CREATED));
        endpoint.setActive(true);

        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(any())).thenReturn(List.of(endpoint));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.sendWebhook(tenantId, WebhookEventType.SUBSCRIPTION_CREATED, new HashMap<>());
    }

    @Test
    void sendWebhook_WithNoEndpoints_ShouldNotFail() {
        UUID tenantId = UUID.randomUUID();
        
        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(any())).thenReturn(Collections.emptyList());

        webhookService.sendWebhook(tenantId, WebhookEventType.SUBSCRIPTION_CREATED, new HashMap<>());
    }

    @Test
    void sendWebhook_ShouldSkipWhenEventNotInEnabledEvents() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId(UUID.randomUUID());
        endpoint.setTenantId(tenantId);
        endpoint.setUrl("https://example.com/webhook");
        endpoint.setEnabledEvents(List.of(WebhookEventType.SUBSCRIPTION_CREATED));
        endpoint.setActive(true);

        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(any())).thenReturn(List.of(endpoint));

        webhookService.sendWebhook(tenantId, WebhookEventType.INVOICE_PAID, new HashMap<>());

        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void sendWebhook_ShouldHandleException() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId(UUID.randomUUID());
        endpoint.setTenantId(tenantId);
        endpoint.setUrl("https://example.com/webhook");
        endpoint.setEnabledEvents(List.of(WebhookEventType.SUBSCRIPTION_CREATED));
        endpoint.setActive(true);

        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(any())).thenReturn(List.of(endpoint));
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

        assertThatCode(() -> webhookService.sendWebhook(tenantId, WebhookEventType.SUBSCRIPTION_CREATED, new HashMap<>()))
                .doesNotThrowAnyException();
    }

    @Test
    void sendWebhook_ShouldSendToMultipleEndpoints() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        WebhookEndpoint endpoint1 = new WebhookEndpoint();
        endpoint1.setId(UUID.randomUUID());
        endpoint1.setTenantId(tenantId);
        endpoint1.setUrl("https://example1.com/webhook");
        endpoint1.setEnabledEvents(List.of(WebhookEventType.SUBSCRIPTION_CREATED));
        endpoint1.setActive(true);

        WebhookEndpoint endpoint2 = new WebhookEndpoint();
        endpoint2.setId(UUID.randomUUID());
        endpoint2.setTenantId(tenantId);
        endpoint2.setUrl("https://example2.com/webhook");
        endpoint2.setEnabledEvents(List.of(WebhookEventType.SUBSCRIPTION_CREATED));
        endpoint2.setActive(true);

        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(any())).thenReturn(List.of(endpoint1, endpoint2));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.sendWebhook(tenantId, WebhookEventType.SUBSCRIPTION_CREATED, new HashMap<>());

        verify(objectMapper, times(2)).writeValueAsString(any());
    }

    @Test
    void sendWebhook_ShouldSkipWhenEnabledEventsEmpty() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId(UUID.randomUUID());
        endpoint.setTenantId(tenantId);
        endpoint.setUrl("https://example.com/webhook");
        endpoint.setEnabledEvents(Collections.emptyList());
        endpoint.setActive(true);

        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(any())).thenReturn(List.of(endpoint));

        webhookService.sendWebhook(tenantId, WebhookEventType.SUBSCRIPTION_CREATED, new HashMap<>());

        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void sendWebhook_ShouldHandleMultipleEventsInEndpoint() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId(UUID.randomUUID());
        endpoint.setTenantId(tenantId);
        endpoint.setUrl("https://example.com/webhook");
        endpoint.setEnabledEvents(List.of(
            WebhookEventType.SUBSCRIPTION_CREATED, 
            WebhookEventType.INVOICE_PAID,
            WebhookEventType.PAYMENT_FAILED
        ));
        endpoint.setActive(true);

        when(webhookEndpointRepository.findByTenantIdAndActiveTrue(any())).thenReturn(List.of(endpoint));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.sendWebhook(tenantId, WebhookEventType.INVOICE_PAID, new HashMap<>());

        verify(objectMapper).writeValueAsString(any());
    }
}

