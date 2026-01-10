package com.fluxpay.api.controller;

import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.tenant.entity.Notification;
import com.fluxpay.tenant.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationController notificationController;

    private UUID userId;
    private UUID tenantId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setTenantId(tenantId);
        notification.setTitle("Test Notification");
        notification.setMessage("Test message");
        notification.setRead(false);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void getNotifications_Success() {
        PageResponse<Notification> pageResponse = new PageResponse<>(
                List.of(notification),
                0,
                20,
                1L,
                1,
                true
        );

        when(notificationService.getNotifications(eq(userId), eq(0), eq(20))).thenReturn(pageResponse);

        ResponseEntity<PageResponse<Notification>> response = notificationController.getNotifications(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getTitle()).isEqualTo("Test Notification");
    }

    @Test
    void getNotifications_WithDefaultParameters() {
        PageResponse<Notification> pageResponse = new PageResponse<>(
                Collections.emptyList(),
                0,
                20,
                0L,
                0,
                true
        );

        when(notificationService.getNotifications(eq(userId), eq(0), eq(20))).thenReturn(pageResponse);

        ResponseEntity<PageResponse<Notification>> response = notificationController.getNotifications(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        ResponseEntity<Map<String, Long>> response = notificationController.getUnreadCount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("count")).isEqualTo(5L);
    }

    @Test
    void getUnreadCount_ReturnsZero() {
        when(notificationService.getUnreadCount(userId)).thenReturn(0L);

        ResponseEntity<Map<String, Long>> response = notificationController.getUnreadCount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("count")).isEqualTo(0L);
    }

    @Test
    void markAsRead_Success() {
        Notification updatedNotification = new Notification();
        updatedNotification.setId(notification.getId());
        updatedNotification.setRead(true);
        updatedNotification.setReadAt(Instant.now());

        when(notificationService.markAsRead(notification.getId())).thenReturn(updatedNotification);

        ResponseEntity<Notification> response = notificationController.markAsRead(notification.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isRead()).isTrue();
        assertThat(response.getBody().getReadAt()).isNotNull();
    }

    @Test
    void getNotification_Success() {
        when(notificationService.getNotificationById(notification.getId())).thenReturn(notification);

        ResponseEntity<Notification> response = notificationController.getNotification(notification.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(notification.getId());
        assertThat(response.getBody().getTitle()).isEqualTo("Test Notification");
    }
}

