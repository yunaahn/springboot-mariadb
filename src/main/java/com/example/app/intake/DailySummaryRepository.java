package com.example.app.intake;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySummaryRepository extends JpaRepository<DailySummary, DailySummaryId> {
}
