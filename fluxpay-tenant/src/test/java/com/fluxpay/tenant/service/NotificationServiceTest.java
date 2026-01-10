package com.fluxpay.tenant.service;

import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.tenant.entity.Notification;
import com.fluxpay.tenant.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID tenantId;
    private UUID userId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setTenantId(tenantId);
        notification.setUserId(userId);
        notification.setTitle("Test Notification");
        notification.setMessage("Test message");
        notification.setRead(false);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Notification result = notificationService.createNotification(notification);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(notification.getId());
        verify(notificationRepository).save(notification);
    }

    @Test
    void getNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> notificationPage = new PageImpl<>(
                List.of(notification),
                pageable,
                1L
        );

        when(notificationRepository.findByTenantIdAndUserId(eq(tenantId), eq(userId), eq(pageable)))
                .thenReturn(notificationPage);

        PageResponse<Notification> result = notificationService.getNotifications(userId, 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getNotifications_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> emptyPage = new PageImpl<>(List.of(), pageable, 0L);

        when(notificationRepository.findByTenantIdAndUserId(eq(tenantId), eq(userId), eq(pageable)))
                .thenReturn(emptyPage);

        PageResponse<Notification> result = notificationService.getNotifications(userId, 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countUnreadByTenantIdAndUserId(tenantId, userId)).thenReturn(5L);

        long result = notificationService.getUnreadCount(userId);

        assertThat(result).isEqualTo(5L);
        verify(notificationRepository).countUnreadByTenantIdAndUserId(tenantId, userId);
    }

    @Test
    void getUnreadCount_ReturnsZero() {
        when(notificationRepository.countUnreadByTenantIdAndUserId(tenantId, userId)).thenReturn(0L);

        long result = notificationService.getUnreadCount(userId);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Notification result = notificationService.markAsRead(notification.getId());

        assertThat(result).isNotNull();
        assertThat(result.getRead()).isTrue();
        assertThat(result.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification");
    }

    @Test
    void markAsRead_ThrowsException_WhenDeleted() {
        notification.setDeletedAt(Instant.now());
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notification.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsRead_ThrowsException_WhenWrongTenant() {
        UUID differentTenantId = UUID.randomUUID();
        notification.setTenantId(differentTenantId);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notification.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getNotificationById_Success() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        Notification result = notificationService.getNotificationById(notification.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(notification.getId());
    }

    @Test
    void getNotificationById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotificationById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getNotificationById_ThrowsException_WhenDeleted() {
        notification.setDeletedAt(Instant.now());
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.getNotificationById(notification.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

