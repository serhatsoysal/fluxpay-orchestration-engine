package com.fluxpay.subscription.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.subscription.entity.Customer;
import com.fluxpay.subscription.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(Customer customer) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        if (customerRepository.existsByTenantIdAndEmail(tenantId, customer.getEmail())) {
            throw new ValidationException("Customer with email already exists: " + customer.getEmail());
        }

        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByEmail(String email) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return customerRepository.findByTenantIdAndEmail(tenantId, email)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with email: " + email));
    }

    public Customer updateCustomer(UUID id, Customer updatedCustomer) {
        Customer customer = getCustomerById(id);
        
        customer.setName(updatedCustomer.getName());
        customer.setEmail(updatedCustomer.getEmail());
        customer.setBillingAddress(updatedCustomer.getBillingAddress());
        customer.setTaxId(updatedCustomer.getTaxId());
        customer.setDefaultPaymentMethodId(updatedCustomer.getDefaultPaymentMethodId());
        customer.setMetadata(updatedCustomer.getMetadata());

        return customerRepository.save(customer);
    }

    public void deleteCustomer(UUID id) {
        Customer customer = getCustomerById(id);
        customer.softDelete();
        customerRepository.save(customer);
    }
}

