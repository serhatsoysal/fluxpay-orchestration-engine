package com.fluxpay.api.controller;

import com.fluxpay.common.dto.PageResponse;
import com.fluxpay.tenant.entity.Notification;
import com.fluxpay.tenant.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<Notification>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = getCurrentUserId();
        PageResponse<Notification> response = notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UUID userId = getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotification(@PathVariable UUID id) {
        Notification notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(notification);
    }

    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

