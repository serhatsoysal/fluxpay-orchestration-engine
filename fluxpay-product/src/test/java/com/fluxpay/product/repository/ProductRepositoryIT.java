package com.fluxpay.product.repository;

import com.fluxpay.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class ProductRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private ProductRepository productRepository;

    private UUID tenantId1;
    private UUID tenantId2;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        tenantId1 = UUID.randomUUID();
        tenantId2 = UUID.randomUUID();

        product1 = createProduct(tenantId1, "Product One", true);
        product2 = createProduct(tenantId1, "Product Two", false);
        product3 = createProduct(tenantId1, "Product Three", true);
        Product product4 = createProduct(tenantId2, "Product Four", true);

        productRepository.saveAll(List.of(product1, product2, product3, product4));
    }

    private Product createProduct(UUID tenantId, String name, Boolean active) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(name);
        product.setActive(active);
        product.setDescription("Test description");
        return product;
    }

    @Test
    void findByTenantIdAndActive_ShouldReturnActiveProducts() {
        List<Product> activeProducts = productRepository.findByTenantIdAndActive(tenantId1, true);

        assertThat(activeProducts).hasSize(2);
        assertThat(activeProducts).allMatch(p -> p.getTenantId().equals(tenantId1));
        assertThat(activeProducts).allMatch(Product::getActive);
    }

    @Test
    void findByTenantId_ShouldReturnAllTenantProducts() {
        List<Product> products = productRepository.findByTenantId(tenantId1);

        assertThat(products).hasSize(3);
        assertThat(products).allMatch(p -> p.getTenantId().equals(tenantId1));
    }

    @Test
    void existsByTenantIdAndName_ShouldReturnTrueWhenExists() {
        boolean exists = productRepository.existsByTenantIdAndName(tenantId1, "Product One");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByTenantIdAndName_ShouldReturnFalseWhenNotExists() {
        boolean exists = productRepository.existsByTenantIdAndName(tenantId1, "Non Existent");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByTenantIdAndNameExcludingId_ShouldExcludeGivenId() {
        boolean exists = productRepository.existsByTenantIdAndNameExcludingId(
                tenantId1, "Product One", product1.getId()
        );

        assertThat(exists).isFalse();
    }

    @Test
    void existsByTenantIdAndNameExcludingId_ShouldReturnTrueForOtherId() {
        boolean exists = productRepository.existsByTenantIdAndNameExcludingId(
                tenantId1, "Product One", product2.getId()
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByTenantIdAndNameExcludingId_ShouldRespectTenantIsolation() {
        boolean exists = productRepository.existsByTenantIdAndNameExcludingId(
                tenantId2, "Product One", UUID.randomUUID()
        );

        assertThat(exists).isFalse();
    }

    @Test
    void softDelete_ShouldExcludeDeletedProducts() {
        product1.setDeletedAt(java.time.Instant.now());
        productRepository.save(product1);

        List<Product> products = productRepository.findByTenantId(tenantId1);

        assertThat(products).hasSize(2);
        assertThat(products).noneMatch(p -> p.getId().equals(product1.getId()));
    }

    @Test
    void existsByTenantIdAndNameExcludingId_ShouldExcludeDeletedProducts() {
        product1.setDeletedAt(java.time.Instant.now());
        productRepository.save(product1);

        boolean exists = productRepository.existsByTenantIdAndNameExcludingId(
                tenantId1, "Product One", UUID.randomUUID()
        );

        assertThat(exists).isFalse();
    }
}

