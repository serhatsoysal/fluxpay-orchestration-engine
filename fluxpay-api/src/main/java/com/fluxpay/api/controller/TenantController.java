package com.fluxpay.api.controller;

import com.fluxpay.api.dto.TenantRegistrationRequest;
import com.fluxpay.common.enums.UserRole;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.tenant.entity.Tenant;
import com.fluxpay.tenant.entity.User;
import com.fluxpay.tenant.service.TenantService;
import com.fluxpay.tenant.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final UserService userService;

    public TenantController(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Tenant> register(@Valid @RequestBody TenantRegistrationRequest request) {
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setSlug(request.getSlug());
        tenant.setBillingEmail(request.getBillingEmail());

        Tenant createdTenant = tenantService.createTenant(tenant);

        TenantContext.setCurrentTenant(createdTenant.getId());

        User adminUser = new User();
        adminUser.setTenantId(createdTenant.getId());
        adminUser.setEmail(request.getAdminEmail());
        adminUser.setFirstName(request.getAdminFirstName());
        adminUser.setLastName(request.getAdminLastName());
        adminUser.setRole(UserRole.OWNER);
        adminUser.setEmailVerified(true);

        userService.createUser(adminUser, request.getAdminPassword());

        TenantContext.clear();

        return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenant(@PathVariable UUID id) {
        Tenant tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable UUID id, @RequestBody Tenant tenant) {
        Tenant updatedTenant = tenantService.updateTenant(id, tenant);
        return ResponseEntity.ok(updatedTenant);
    }
}

