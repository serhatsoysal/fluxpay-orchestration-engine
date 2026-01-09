package com.fluxpay.product.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.product.entity.Product;
import com.fluxpay.product.repository.ProductRepository;
import com.fluxpay.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private UUID productId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        product = new Product();
        product.setId(productId);
        product.setTenantId(tenantId);
        product.setName("Test Product");
        product.setActive(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createProduct_ShouldSucceed() {
        when(productRepository.existsByTenantIdAndName(tenantId, "Test Product")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.createProduct(product);

        assertThat(result).isEqualTo(product);
        verify(productRepository).save(product);
    }

    @Test
    void createProduct_ShouldThrowException_WhenNameExists() {
        when(productRepository.existsByTenantIdAndName(tenantId, "Test Product")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(product))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getProductById_ShouldReturnProduct() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(productId);

        assertThat(result).isEqualTo(product);
    }

    @Test
    void getActiveProducts_ShouldReturnList() {
        when(productRepository.findByTenantIdAndActive(tenantId, true)).thenReturn(List.of(product));

        List<Product> result = productService.getActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(product);
    }

    @Test
    void updateProduct_ShouldSucceed() {
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        updatedProduct.setActive(false);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.existsByTenantIdAndNameExcludingId(tenantId, "Updated Product", productId))
                .thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.updateProduct(productId, updatedProduct);

        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getActive()).isFalse();
    }

    @Test
    void deactivateProduct_ShouldSucceed() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deactivateProduct(productId);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_ShouldSucceed() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(productId);

        assertThat(product.getDeletedAt()).isNotNull();
        verify(productRepository).save(product);
    }
}

