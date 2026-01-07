package com.fluxpay.tenant.repository;

import com.fluxpay.tenant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByEmail(String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}

