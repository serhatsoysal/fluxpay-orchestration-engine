package com.fluxpay.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product_features")
@IdClass(ProductFeatureId.class)
@Getter
@Setter
public class ProductFeature {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Id
    @Column(name = "feature_id")
    private UUID featureId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", insertable = false, updatable = false)
    private Feature feature;

    @Column
    private Integer value;
}

