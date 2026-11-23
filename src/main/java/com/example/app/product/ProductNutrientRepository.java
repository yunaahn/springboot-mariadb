package com.example.app.product;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductNutrientRepository extends JpaRepository<ProductNutrient, ProductNutrientId> {

    List<ProductNutrient> findByProductIdIn(Collection<Long> productIds);
}
