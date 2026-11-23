package com.example.app.intake;

import java.io.Serializable;
import java.time.LocalDate;

public record DailySummaryId(Long userId, LocalDate summaryDate) implements Serializable {
}
