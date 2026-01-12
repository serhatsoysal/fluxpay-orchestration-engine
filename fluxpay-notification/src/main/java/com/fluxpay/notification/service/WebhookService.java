package com.fluxpay.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxpay.common.enums.WebhookEventType;
import com.fluxpay.notification.entity.WebhookEndpoint;
import com.fluxpay.notification.repository.WebhookEndpointRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final ObjectMapper objectMapper;

    @Value("${WEBHOOK_TIMEOUT_SECONDS:30}")
    private int webhookTimeoutSeconds;

    @Value("${WEBHOOK_CONNECT_TIMEOUT_SECONDS:10}")
    private int webhookConnectTimeoutSeconds;

    private RestTemplate restTemplate;

    public WebhookService(
            WebhookEndpointRepository webhookEndpointRepository,
            ObjectMapper objectMapper) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(webhookConnectTimeoutSeconds).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(webhookTimeoutSeconds).toMillis());
        
        this.restTemplate = new RestTemplate(factory);
    }

    public void sendWebhook(UUID tenantId, WebhookEventType eventType, Map<String, Object> payload) {
        List<WebhookEndpoint> endpoints = webhookEndpointRepository.findByTenantIdAndActiveTrue(tenantId);

        for (WebhookEndpoint endpoint : endpoints) {
            if (endpoint.getEnabledEvents().contains(eventType)) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    
                    if (endpoint.getSecret() != null && !endpoint.getSecret().isEmpty()) {
                        headers.set("X-Webhook-Secret", endpoint.getSecret());
                    }

                    String jsonPayload = objectMapper.writeValueAsString(payload);
                    HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

                    restTemplate.postForEntity(endpoint.getUrl(), request, String.class);
                } catch (Exception e) {
                    // Log error but don't fail
                }
            }
        }
    }
}

