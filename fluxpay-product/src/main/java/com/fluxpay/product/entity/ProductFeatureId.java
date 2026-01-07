package com.fluxpay.product.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFeatureId implements Serializable {
    private UUID productId;
    private UUID featureId;
}

