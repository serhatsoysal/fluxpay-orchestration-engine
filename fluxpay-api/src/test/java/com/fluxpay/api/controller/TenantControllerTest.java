package com.fluxpay.api.controller;

import com.fluxpay.api.dto.TenantRegistrationRequest;
import com.fluxpay.common.enums.UserRole;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.tenant.entity.Tenant;
import com.fluxpay.tenant.entity.User;
import com.fluxpay.tenant.service.TenantService;
import com.fluxpay.tenant.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TenantController tenantController;

    private Tenant tenant;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Company");
        tenant.setSlug("test-company");
        tenant.setBillingEmail("billing@test.com");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void register_Success() {
        TenantRegistrationRequest request = new TenantRegistrationRequest();
        request.setName("Test Company");
        request.setSlug("test-company");
        request.setBillingEmail("billing@test.com");
        request.setAdminEmail("admin@test.com");
        request.setAdminFirstName("John");
        request.setAdminLastName("Doe");
        request.setAdminPassword("SecurePass123!");

        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setTenantId(tenantId);
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(UserRole.OWNER);

        when(tenantService.createTenant(any(Tenant.class))).thenReturn(tenant);
        when(userService.createUser(any(User.class), eq("SecurePass123!"))).thenReturn(adminUser);

        ResponseEntity<Tenant> response = tenantController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(tenantId);
        assertThat(response.getBody().getName()).isEqualTo("Test Company");
        assertThat(response.getBody().getSlug()).isEqualTo("test-company");
        
        verify(tenantService).createTenant(argThat(t -> 
            t.getName().equals("Test Company") &&
            t.getSlug().equals("test-company") &&
            t.getBillingEmail().equals("billing@test.com")
        ));
        
        verify(userService).createUser(argThat(u -> 
            u.getTenantId().equals(tenantId) &&
            u.getEmail().equals("admin@test.com") &&
            u.getFirstName().equals("John") &&
            u.getLastName().equals("Doe") &&
            u.getRole() == UserRole.OWNER &&
            u.getEmailVerified() == true
        ), eq("SecurePass123!"));
    }

    @Test
    void register_ShouldSetAndClearTenantContext() {
        TenantRegistrationRequest request = new TenantRegistrationRequest();
        request.setName("Test Company");
        request.setSlug("test-company");
        request.setBillingEmail("billing@test.com");
        request.setAdminEmail("admin@test.com");
        request.setAdminFirstName("John");
        request.setAdminLastName("Doe");
        request.setAdminPassword("SecurePass123!");

        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setTenantId(tenantId);

        when(tenantService.createTenant(any(Tenant.class))).thenReturn(tenant);
        when(userService.createUser(any(User.class), anyString())).thenReturn(adminUser);

        tenantController.register(request);

        verify(userService).createUser(argThat(u -> u.getTenantId().equals(tenantId)), anyString());
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    @Test
    void getTenant_Success() {
        when(tenantService.getTenantById(tenantId)).thenReturn(tenant);

        ResponseEntity<Tenant> response = tenantController.getTenant(tenantId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(tenantId);
        assertThat(response.getBody().getName()).isEqualTo("Test Company");
        verify(tenantService).getTenantById(tenantId);
    }

    @Test
    void getTenant_NotFound_ThrowsException() {
        when(tenantService.getTenantById(tenantId)).thenThrow(new ResourceNotFoundException("Tenant", tenantId));

        assertThatThrownBy(() -> tenantController.getTenant(tenantId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tenantService).getTenantById(tenantId);
    }

    @Test
    void updateTenant_Success() {
        Tenant updatedTenant = new Tenant();
        updatedTenant.setId(tenantId);
        updatedTenant.setName("Updated Company");
        updatedTenant.setSlug("updated-company");
        updatedTenant.setBillingEmail("updated@test.com");

        when(tenantService.updateTenant(eq(tenantId), any(Tenant.class))).thenReturn(updatedTenant);

        ResponseEntity<Tenant> response = tenantController.updateTenant(tenantId, updatedTenant);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(tenantId);
        assertThat(response.getBody().getName()).isEqualTo("Updated Company");
        verify(tenantService).updateTenant(tenantId, updatedTenant);
    }

    @Test
    void updateTenant_NotFound_ThrowsException() {
        Tenant updatedTenant = new Tenant();
        updatedTenant.setName("Updated Company");

        when(tenantService.updateTenant(eq(tenantId), any(Tenant.class)))
                .thenThrow(new ResourceNotFoundException("Tenant", tenantId));

        assertThatThrownBy(() -> tenantController.updateTenant(tenantId, updatedTenant))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tenantService).updateTenant(tenantId, updatedTenant);
    }

    @Test
    void register_ShouldCreateUserWithCorrectRole() {
        TenantRegistrationRequest request = new TenantRegistrationRequest();
        request.setName("Test Company");
        request.setSlug("test-company");
        request.setBillingEmail("billing@test.com");
        request.setAdminEmail("admin@test.com");
        request.setAdminFirstName("John");
        request.setAdminLastName("Doe");
        request.setAdminPassword("password");

        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());

        when(tenantService.createTenant(any(Tenant.class))).thenReturn(tenant);
        when(userService.createUser(any(User.class), anyString())).thenReturn(adminUser);

        tenantController.register(request);

        verify(userService).createUser(argThat(u -> u.getRole() == UserRole.OWNER), anyString());
    }

    @Test
    void register_ShouldSetEmailVerifiedToTrue() {
        TenantRegistrationRequest request = new TenantRegistrationRequest();
        request.setName("Test Company");
        request.setSlug("test-company");
        request.setBillingEmail("billing@test.com");
        request.setAdminEmail("admin@test.com");
        request.setAdminFirstName("John");
        request.setAdminLastName("Doe");
        request.setAdminPassword("password");

        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());

        when(tenantService.createTenant(any(Tenant.class))).thenReturn(tenant);
        when(userService.createUser(any(User.class), anyString())).thenReturn(adminUser);

        tenantController.register(request);

        verify(userService).createUser(argThat(u -> u.getEmailVerified() == true), anyString());
    }
}

