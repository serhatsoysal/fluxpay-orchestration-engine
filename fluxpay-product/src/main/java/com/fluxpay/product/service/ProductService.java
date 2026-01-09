package com.fluxpay.product.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.product.entity.Product;
import com.fluxpay.product.repository.ProductRepository;
import com.fluxpay.security.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        if (productRepository.existsByTenantIdAndName(tenantId, product.getName())) {
            throw new ValidationException("Product with name already exists: " + product.getName());
        }
        
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product getProductById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return productRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null && p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public List<Product> getActiveProducts() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return productRepository.findByTenantIdAndActive(tenantId, true);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return productRepository.findByTenantId(tenantId);
    }

    public Product updateProduct(UUID id, Product updatedProduct) {
        Product product = getProductById(id);
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        if (updatedProduct.getName() != null && !product.getName().equals(updatedProduct.getName())) {
            if (productRepository.existsByTenantIdAndNameExcludingId(tenantId, updatedProduct.getName(), id)) {
                throw new ValidationException("Product with name already exists: " + updatedProduct.getName());
            }
            product.setName(updatedProduct.getName());
        }
        if (updatedProduct.getDescription() != null) {
            product.setDescription(updatedProduct.getDescription());
        }
        if (updatedProduct.getActive() != null) {
            product.setActive(updatedProduct.getActive());
        }
        if (updatedProduct.getMetadata() != null) {
            product.setMetadata(updatedProduct.getMetadata());
        }

        return productRepository.save(product);
    }

    public void deactivateProduct(UUID id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        product.softDelete();
        productRepository.save(product);
    }
}

