package com.example.app.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record NewProduct(@NotBlank @Size(max = 200) String name,
                             @Size(max = 100) String manufacturer,
                             @Size(max = 50) List<@Valid @NotNull NewNutrient> nutrients) {
    }

    /** amount는 1회 섭취량당, unit은 성분의 표준 단위. */
    public record NewNutrient(@NotBlank String code,
                              @NotNull @Positive @Digits(integer = 8, fraction = 4) BigDecimal amount,
                              String unit) {
    }

    public record ProductSummary(Long id, String name, String manufacturer, String imageUrl) {
        static ProductSummary of(Product p) {
            return new ProductSummary(p.getId(), p.getName(), p.getManufacturer(), p.getImageUrl());
        }
    }

    public record ProductDetail(Long id, String name, String manufacturer, String imageUrl, String servingUnit,
                                List<NutrientAmount> nutrients) {
    }

    public record NutrientAmount(String code, String name, BigDecimal amount, String unit) {
    }
}
