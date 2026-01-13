package com.fluxpay.subscription.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.subscription.entity.Customer;
import com.fluxpay.subscription.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private UUID customerId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setName("Test Customer");
        customer.setEmail("test@example.com");
        customer.setDeletedAt(null);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createCustomer_ShouldReturnSavedCustomer() {
        Customer newCustomer = new Customer();
        newCustomer.setTenantId(tenantId);
        newCustomer.setEmail("new@example.com");
        newCustomer.setName("New Customer");

        when(customerRepository.existsByTenantIdAndEmail(tenantId, "new@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        Customer result = customerService.createCustomer(newCustomer);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        verify(customerRepository).existsByTenantIdAndEmail(tenantId, "new@example.com");
        verify(customerRepository).save(newCustomer);
    }

    @Test
    void createCustomer_WhenEmailExists_ShouldThrowException() {
        Customer newCustomer = new Customer();
        newCustomer.setTenantId(tenantId);
        newCustomer.setEmail("existing@example.com");

        when(customerRepository.existsByTenantIdAndEmail(tenantId, "existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(newCustomer))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getCustomerById_ShouldReturnCustomer() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerById(customerId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(customerId);
        verify(customerRepository).findById(customerId);
    }

    @Test
    void getCustomerById_WhenNotFound_ShouldThrowException() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository).findById(customerId);
    }

    @Test
    void getCustomerById_WhenDeleted_ShouldThrowException() {
        customer.setDeletedAt(java.time.Instant.now());
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository).findById(customerId);
    }

    @Test
    void getCustomerByEmail_ShouldReturnCustomer() {
        when(customerRepository.findByTenantIdAndEmail(tenantId, "test@example.com"))
                .thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerByEmail("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(customerRepository).findByTenantIdAndEmail(tenantId, "test@example.com");
    }

    @Test
    void getCustomerByEmail_WhenNotFound_ShouldThrowException() {
        when(customerRepository.findByTenantIdAndEmail(tenantId, "nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository).findByTenantIdAndEmail(tenantId, "nonexistent@example.com");
    }

    @Test
    void getCustomerByEmail_WhenDeleted_ShouldThrowException() {
        customer.setDeletedAt(java.time.Instant.now());
        when(customerRepository.findByTenantIdAndEmail(tenantId, "test@example.com"))
                .thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.getCustomerByEmail("test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository).findByTenantIdAndEmail(tenantId, "test@example.com");
    }

    @Test
    void updateCustomer_ShouldUpdateFields() {
        Customer updatedCustomer = new Customer();
        updatedCustomer.setName("Updated Name");
        updatedCustomer.setEmail("updated@example.com");
        Map<String, Object> billingAddress = new HashMap<>();
        billingAddress.put("street", "123 Main St");
        billingAddress.put("city", "New York");
        updatedCustomer.setBillingAddress(billingAddress);
        updatedCustomer.setTaxId("TAX123");
        updatedCustomer.setDefaultPaymentMethodId("pm_123456");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = customerService.updateCustomer(customerId, updatedCustomer);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getBillingAddress()).isEqualTo(billingAddress);
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomer_WhenNotFound_ShouldThrowException() {
        Customer updatedCustomer = new Customer();
        updatedCustomer.setName("Updated Name");

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(customerId, updatedCustomer))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void deleteCustomer_ShouldSoftDelete() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.deleteCustomer(customerId);

        assertThat(customer.getDeletedAt()).isNotNull();
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(customer);
    }

    @Test
    void deleteCustomer_WhenNotFound_ShouldThrowException() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(customerId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository, never()).save(any());
    }
}

