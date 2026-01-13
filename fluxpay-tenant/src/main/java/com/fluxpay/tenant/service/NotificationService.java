package com.fluxpay.tenant.service;

import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.tenant.entity.Notification;
import com.fluxpay.tenant.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<Notification> getNotifications(UUID userId, int page, int size) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository.findByTenantIdAndUserId(tenantId, userId, pageable);

        return new PageResponse<>(
                notificationPage.getContent(),
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return notificationRepository.countUnreadByTenantIdAndUserId(tenantId, userId);
    }

    public Notification markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .filter(n -> n.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));

        UUID tenantId = TenantContext.getCurrentTenantId();
        if (!notification.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Notification", id);
        }

        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return notificationRepository.save(notification);
    }

    public long markAllAsRead(UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return notificationRepository.markAllAsRead(tenantId, userId);
    }

    @Transactional(readOnly = true)
    public Notification getNotificationById(UUID id) {
        return notificationRepository.findById(id)
                .filter(n -> n.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }
}

