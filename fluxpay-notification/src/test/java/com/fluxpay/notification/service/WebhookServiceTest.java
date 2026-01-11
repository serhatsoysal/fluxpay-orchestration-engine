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
import static org.mockito.Mockito.when;

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
}

