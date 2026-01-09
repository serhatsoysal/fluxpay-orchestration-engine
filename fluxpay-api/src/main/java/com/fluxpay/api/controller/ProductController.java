package com.fluxpay.api.controller;

import com.fluxpay.product.entity.Price;
import com.fluxpay.product.entity.Product;
import com.fluxpay.product.service.PriceService;
import com.fluxpay.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final PriceService priceService;

    public ProductController(ProductService productService, PriceService priceService) {
        this.productService = productService;
        this.priceService = priceService;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        List<Product> products = productService.getActiveProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product product) {
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }

    @PostMapping("/{productId}/prices")
    public ResponseEntity<Price> createPrice(@PathVariable UUID productId, @Valid @RequestBody Price price) {
        price.setProductId(productId);
        Price createdPrice = priceService.createPrice(price);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPrice);
    }

    @GetMapping("/{productId}/prices")
    public ResponseEntity<List<Price>> getPrices(@PathVariable UUID productId) {
        List<Price> prices = priceService.getActivePricesByProduct(productId);
        return ResponseEntity.ok(prices);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

