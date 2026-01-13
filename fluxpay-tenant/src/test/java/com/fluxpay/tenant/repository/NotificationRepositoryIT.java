package com.fluxpay.tenant.repository;

import com.fluxpay.tenant.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class NotificationRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID tenantId1;
    private UUID tenantId2;
    private UUID userId1;
    private UUID userId2;
    private Notification notification1;
    private Notification notification2;
    private Notification notification3;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        tenantId1 = UUID.randomUUID();
        tenantId2 = UUID.randomUUID();
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();

        notification1 = createNotification(tenantId1, userId1, false, Instant.now().minusSeconds(3600));
        notification2 = createNotification(tenantId1, userId1, false, Instant.now().minusSeconds(1800));
        notification3 = createNotification(tenantId1, userId1, true, Instant.now());
        Notification notification4 = createNotification(tenantId1, userId2, false, Instant.now());
        Notification notification5 = createNotification(tenantId2, userId1, false, Instant.now());

        notificationRepository.saveAll(List.of(notification1, notification2, notification3, notification4, notification5));
    }

    private Notification createNotification(UUID tenantId, UUID userId, boolean read, Instant createdAt) {
        Notification notification = new Notification();
        notification.setTenantId(tenantId);
        notification.setUserId(userId);
        notification.setRead(read);
        notification.setTitle("Test Notification");
        notification.setMessage("Test message");
        notification.setCreatedAt(createdAt);
        return notification;
    }

    @Test
    void findByTenantIdAndUserId_WithPagination_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 2);

        Page<Notification> result = notificationRepository.findByTenantIdAndUserId(tenantId1, userId1, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getContent()).allMatch(n -> n.getTenantId().equals(tenantId1));
        assertThat(result.getContent()).allMatch(n -> n.getUserId().equals(userId1));
    }

    @Test
    void findByTenantIdAndUserId_ShouldOrderByCreatedAtDesc() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> result = notificationRepository.findByTenantIdAndUserId(tenantId1, userId1, pageable);

        List<Notification> content = result.getContent();
        assertThat(content).hasSize(3);
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getCreatedAt())
                    .isAfterOrEqualTo(content.get(i + 1).getCreatedAt());
        }
    }

    @Test
    void countUnreadByTenantIdAndUserId_ShouldCountUnreadOnly() {
        long unreadCount = notificationRepository.countUnreadByTenantIdAndUserId(tenantId1, userId1);

        assertThat(unreadCount).isEqualTo(2L);
    }

    @Test
    void countUnreadByTenantIdAndUserId_WithAllRead_ShouldReturnZero() {
        notification1.setRead(true);
        notification2.setRead(true);
        notificationRepository.saveAll(List.of(notification1, notification2));

        long unreadCount = notificationRepository.countUnreadByTenantIdAndUserId(tenantId1, userId1);

        assertThat(unreadCount).isZero();
    }

    @Test
    void markAllAsRead_ShouldUpdateUnreadNotifications() {
        long updated = notificationRepository.markAllAsRead(tenantId1, userId1, Instant.now());

        assertThat(updated).isEqualTo(2L);

        long unreadCount = notificationRepository.countUnreadByTenantIdAndUserId(tenantId1, userId1);
        assertThat(unreadCount).isZero();
    }

    @Test
    void markAllAsRead_ShouldNotUpdateAlreadyRead() {
        long updated = notificationRepository.markAllAsRead(tenantId1, userId1, Instant.now());

        assertThat(updated).isEqualTo(2L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> result = notificationRepository.findByTenantIdAndUserId(tenantId1, userId1, pageable);
        assertThat(result.getContent()).allMatch(Notification::getRead);
    }

    @Test
    void markAllAsRead_ShouldRespectTenantIsolation() {
        long updated = notificationRepository.markAllAsRead(tenantId1, userId1, Instant.now());

        assertThat(updated).isEqualTo(2L);

        long tenant2Unread = notificationRepository.countUnreadByTenantIdAndUserId(tenantId2, userId1);
        assertThat(tenant2Unread).isEqualTo(1L);
    }

    @Test
    void markAllAsRead_ShouldRespectUserIsolation() {
        long updated = notificationRepository.markAllAsRead(tenantId1, userId1, Instant.now());

        assertThat(updated).isEqualTo(2L);

        long userId2Unread = notificationRepository.countUnreadByTenantIdAndUserId(tenantId1, userId2);
        assertThat(userId2Unread).isEqualTo(1L);
    }

    @Test
    void softDelete_ShouldExcludeDeletedNotifications() {
        notification1.setDeletedAt(Instant.now());
        notificationRepository.save(notification1);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> result = notificationRepository.findByTenantIdAndUserId(tenantId1, userId1, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).noneMatch(n -> n.getId().equals(notification1.getId()));
    }

    @Test
    void countUnreadByTenantIdAndUserId_ShouldExcludeDeleted() {
        notification1.setDeletedAt(Instant.now());
        notificationRepository.save(notification1);

        long unreadCount = notificationRepository.countUnreadByTenantIdAndUserId(tenantId1, userId1);

        assertThat(unreadCount).isEqualTo(1L);
    }
}

