package com.fluxpay.api.controller;

import com.fluxpay.product.entity.Price;
import com.fluxpay.product.entity.Product;
import com.fluxpay.product.service.PriceService;
import com.fluxpay.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private ProductController productController;

    private Product product;
    private Price price;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setActive(true);

        price = new Price();
        price.setId(UUID.randomUUID());
        price.setProductId(productId);
        price.setUnitAmount(new BigDecimal("1000.00"));
        price.setCurrency("USD");
    }

    @Test
    void createProduct_Success() {
        when(productService.createProduct(any(Product.class))).thenReturn(product);

        ResponseEntity<Product> response = productController.createProduct(product);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(productId);
        verify(productService).createProduct(product);
    }

    @Test
    void getProducts_Success() {
        List<Product> products = List.of(product);
        when(productService.getActiveProducts()).thenReturn(products);

        ResponseEntity<List<Product>> response = productController.getProducts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Test Product");
    }

    @Test
    void getProducts_ReturnsEmptyList() {
        when(productService.getActiveProducts()).thenReturn(Collections.emptyList());

        ResponseEntity<List<Product>> response = productController.getProducts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getProduct_Success() {
        when(productService.getProductById(productId)).thenReturn(product);

        ResponseEntity<Product> response = productController.getProduct(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(productId);
    }

    @Test
    void updateProduct_Success() {
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        when(productService.updateProduct(eq(productId), any(Product.class))).thenReturn(updatedProduct);

        ResponseEntity<Product> response = productController.updateProduct(productId, updatedProduct);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Updated Product");
        verify(productService).updateProduct(productId, updatedProduct);
    }

    @Test
    void createPrice_Success() {
        when(priceService.createPrice(any(Price.class))).thenReturn(price);

        ResponseEntity<Price> response = productController.createPrice(productId, price);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProductId()).isEqualTo(productId);
        verify(priceService).createPrice(price);
    }

    @Test
    void getPrices_Success() {
        List<Price> prices = List.of(price);
        when(priceService.getActivePricesByProduct(productId)).thenReturn(prices);

        ResponseEntity<List<Price>> response = productController.getPrices(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getProductId()).isEqualTo(productId);
    }

    @Test
    void deleteProduct_Success() {
        doNothing().when(productService).deleteProduct(productId);

        ResponseEntity<Void> response = productController.deleteProduct(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productService).deleteProduct(productId);
    }
}

