package com.fluxpay.tenant.entity;

import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

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
    void prePersist_SetsTenantId_WhenNull() {
        Notification notification = new Notification();
        notification.setUserId(UUID.randomUUID());
        notification.setTitle("Test");
        notification.setMessage("Test message");

        notification.prePersist();

        assertThat(notification.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void prePersist_DoesNotOverrideTenantId_WhenNotNull() {
        UUID existingTenantId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setTenantId(existingTenantId);
        notification.setUserId(UUID.randomUUID());
        notification.setTitle("Test");
        notification.setMessage("Test message");

        notification.prePersist();

        assertThat(notification.getTenantId()).isEqualTo(existingTenantId);
    }

    @Test
    void gettersAndSetters_WorkCorrectly() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String title = "Test Title";
        String message = "Test Message";
        boolean read = true;
        Instant readAt = Instant.now();
        String type = "INFO";

        Notification notification = new Notification();
        notification.setId(id);
        notification.setTenantId(tenantId);
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(read);
        notification.setReadAt(readAt);
        notification.setType(type);

        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getTenantId()).isEqualTo(tenantId);
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getMessage()).isEqualTo(message);
        assertThat(notification.getRead()).isEqualTo(read);
        assertThat(notification.getReadAt()).isEqualTo(readAt);
        assertThat(notification.getType()).isEqualTo(type);
    }
}

