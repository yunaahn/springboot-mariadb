package com.example.app.intake;

import com.example.app.supplement.MealTiming;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public final class IntakeDtos {

    private IntakeDtos() {
    }

    public record DayResponse(LocalDate date, Progress progress, List<Slot> slots) {
    }

    public record Progress(int scheduled, int taken) {
    }

    public record Slot(@JsonFormat(pattern = "HH:mm") LocalTime time, List<Item> items) {
    }

    public record Item(Long intakeId, Long supplementId, String name, BigDecimal dose, MealTiming mealTiming,
                       IntakeStatus status, OffsetDateTime takenAt) {
    }

    public record TakeRequest(@NotEmpty @Size(max = 100) Set<@NotNull Long> intakeIds) {
    }

    /** status는 TAKEN, LATE 또는 ALREADY_PROCESSED. */
    public record TakeResult(Long intakeId, String status) {
    }

    /** 경험치·캐릭터·스트릭은 보상 단계에서 추가한다. */
    public record TakeResponse(List<TakeResult> results, boolean dayCompleted) {
    }

    public record StatusResponse(Long intakeId, IntakeStatus status) {
    }
}
