package com.fluxpay.product.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setActive(true);
        product.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createProduct_Success() {
        when(productRepository.existsByTenantIdAndName(tenantId, product.getName())).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.createProduct(product);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).save(product);
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        Product result = productService.getProductById(product.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(product.getId());
    }

    @Test
    void getProductById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveProducts_Success() {
        List<Product> products = Arrays.asList(product);
        when(productRepository.findByTenantIdAndActive(tenantId, true)).thenReturn(products);

        List<Product> result = productService.getActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }
}

