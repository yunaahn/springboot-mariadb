package com.example.app.product;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutrientRepository extends JpaRepository<Nutrient, Integer> {

    List<Nutrient> findByCodeIn(Collection<String> codes);
}
