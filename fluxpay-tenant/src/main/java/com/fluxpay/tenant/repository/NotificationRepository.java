package com.fluxpay.tenant.repository;

import com.fluxpay.tenant.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findByTenantIdAndUserId(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.read = false AND n.deletedAt IS NULL")
    long countUnreadByTenantIdAndUserId(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}

