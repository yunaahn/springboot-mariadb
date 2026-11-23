package com.example.app.supplement;

import com.example.app.product.ProductDtos.NewProduct;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class SupplementDtos {

    private SupplementDtos() {
    }

    /** 순서가 days_of_week 비트 순서(월=1 ... 일=64)다. */
    public enum Day {
        MON, TUE, WED, THU, FRI, SAT, SUN;

        static int toMask(Collection<Day> days) {
            return days.stream().mapToInt(d -> 1 << d.ordinal()).reduce(0, (a, b) -> a | b);
        }

        static List<Day> fromMask(int mask) {
            return Arrays.stream(values()).filter(d -> (mask & (1 << d.ordinal())) != 0).toList();
        }
    }

    public record CreateRequest(Long productId,
                                @Valid NewProduct newProduct,
                                @Size(max = 50) String alias,
                                @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal dosePerIntake,
                                MealTiming mealTiming,
                                @NotEmpty Set<@NotNull Day> daysOfWeek,
                                @NotEmpty @Size(max = 24) Set<@NotNull LocalTime> times,
                                @Positive @Digits(integer = 6, fraction = 2) BigDecimal totalQty) {
    }

    /** null인 항목은 바꾸지 않는다. */
    public record UpdateRequest(@Size(max = 50) String alias,
                                @Positive @Digits(integer = 4, fraction = 2) BigDecimal dosePerIntake,
                                MealTiming mealTiming,
                                @Size(min = 1) Set<@NotNull Day> daysOfWeek,
                                @Size(min = 1, max = 24) Set<@NotNull LocalTime> times,
                                @Positive @Digits(integer = 6, fraction = 2) BigDecimal totalQty,
                                @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal remainingQty) {
    }

    public record StatusRequest(@NotNull SupplementStatus status) {
    }

    public record CreateResponse(Long id, List<String> duplicateNutrients) {
    }

    public record SupplementResponse(Long id, Long productId, String name, String alias, String imageUrl,
                                     String servingUnit, BigDecimal dosePerIntake, MealTiming mealTiming,
                                     List<Day> daysOfWeek,
                                     @JsonFormat(pattern = "HH:mm") List<LocalTime> times,
                                     BigDecimal totalQty, BigDecimal remainingQty, SupplementStatus status,
                                     LocalDate startedAt, LocalDate endedAt) {
    }
}
