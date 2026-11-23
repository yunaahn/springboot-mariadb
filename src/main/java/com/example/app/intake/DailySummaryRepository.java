package com.example.app.intake;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 하루 복용률 80% 이상이면 스트릭 달성이다. */
public interface DailySummaryRepository extends JpaRepository<DailySummary, DailySummaryId> {

    @Query("""
            select max(d.summaryDate) from DailySummary d
            where d.userId = :userId and d.summaryDate < :today and d.scheduledCount > 0
              and d.takenCount * 100 < d.scheduledCount * 80
            """)
    Optional<LocalDate> findLastFailure(Long userId, LocalDate today);

    @Query("""
            select count(d.summaryDate) as dayCount, min(d.summaryDate) as firstDate, max(d.summaryDate) as lastDate
            from DailySummary d
            where d.userId = :userId and d.summaryDate > :after and d.summaryDate <= :today and d.scheduledCount > 0
              and d.takenCount * 100 >= d.scheduledCount * 80
            """)
    Successes countSuccesses(Long userId, LocalDate after, LocalDate today);

    interface Successes {
        long getDayCount();

        LocalDate getFirstDate();

        LocalDate getLastDate();
    }
}
