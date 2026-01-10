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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private UUID tenantId;
    private Product product;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setTenantId(tenantId);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setActive(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createProduct_Success() {
        when(productRepository.existsByTenantIdAndName(tenantId, product.getName())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.createProduct(product);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).save(product);
    }

    @Test
    void createProduct_ThrowsException_WhenNameExists() {
        when(productRepository.existsByTenantIdAndName(tenantId, product.getName())).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(product))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Product with name already exists");

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_Success() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getProductById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductById_ThrowsException_WhenWrongTenant() {
        UUID differentTenantId = UUID.randomUUID();
        product.setTenantId(differentTenantId);
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductById_ThrowsException_WhenDeleted() {
        product.softDelete();
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveProducts_Success() {
        List<Product> products = List.of(product);
        when(productRepository.findByTenantIdAndActive(tenantId, true)).thenReturn(products);

        List<Product> result = productService.getActiveProducts();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Product");
    }

    @Test
    void getActiveProducts_ReturnsEmptyList() {
        when(productRepository.findByTenantIdAndActive(tenantId, true)).thenReturn(Collections.emptyList());

        List<Product> result = productService.getActiveProducts();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getAllProducts_Success() {
        List<Product> products = List.of(product);
        when(productRepository.findByTenantId(tenantId)).thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    void updateProduct_Success() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.existsByTenantIdAndNameExcludingId(tenantId, "Updated Product", id))
                .thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");

        Product result = productService.updateProduct(id, updatedProduct);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_ThrowsException_WhenNameExists() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.existsByTenantIdAndNameExcludingId(tenantId, "Existing Product", id))
                .thenReturn(true);

        Product updatedProduct = new Product();
        updatedProduct.setName("Existing Product");

        assertThatThrownBy(() -> productService.updateProduct(id, updatedProduct))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Product with name already exists");
    }

    @Test
    void updateProduct_DoesNotChangeName_WhenSame() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product updatedProduct = new Product();
        updatedProduct.setName(product.getName());
        updatedProduct.setDescription("Updated Description");

        Product result = productService.updateProduct(id, updatedProduct);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        verify(productRepository, never()).existsByTenantIdAndNameExcludingId(any(), any(), any());
    }

    @Test
    void updateProduct_UpdatesActive() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product updatedProduct = new Product();
        updatedProduct.setActive(false);

        Product result = productService.updateProduct(id, updatedProduct);

        assertThat(result).isNotNull();
        assertThat(result.getActive()).isFalse();
    }

    @Test
    void updateProduct_UpdatesMetadata() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product updatedProduct = new Product();
        updatedProduct.setMetadata(Collections.singletonMap("key", "value"));

        Product result = productService.updateProduct(id, updatedProduct);

        assertThat(result).isNotNull();
        assertThat(result.getMetadata()).containsEntry("key", "value");
    }

    @Test
    void deactivateProduct_Success() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deactivateProduct(id);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_Success() {
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(id);

        assertThat(product.getDeletedAt()).isNotNull();
        verify(productRepository).save(product);
    }
}
