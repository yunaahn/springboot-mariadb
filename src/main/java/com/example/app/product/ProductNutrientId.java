package com.example.app.product;

import java.io.Serializable;

public record ProductNutrientId(Long productId, Integer nutrientId) implements Serializable {
}
