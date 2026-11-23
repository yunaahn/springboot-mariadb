package com.example.app.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductNutrientRepository extends JpaRepository<ProductNutrient, ProductNutrientId> {
}
