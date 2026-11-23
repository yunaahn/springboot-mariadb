package com.example.app.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_nutrients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(ProductNutrientId.class)
public class ProductNutrient {

    @Id
    private Long productId;
    @Id
    private Integer nutrientId;
    @Column(precision = 12, scale = 4)
    private BigDecimal amount;

    public ProductNutrient(Long productId, Integer nutrientId, BigDecimal amount) {
        this.productId = productId;
        this.nutrientId = nutrientId;
        this.amount = amount;
    }
}
