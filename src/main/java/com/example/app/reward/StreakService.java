package com.example.app.reward;

import com.example.app.intake.DailySummaryRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 일별 집계로 스트릭을 계산한다. 일정이 없는 날은 건너뛰고, 오늘은 달성했을 때만 센다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreakService {

    private static final LocalDate BEGINNING = LocalDate.of(2000, 1, 1);

    private final DailySummaryRepository summaries;

    /** start·last는 length가 0이면 null. */
    public record Run(int length, LocalDate start, LocalDate last) {
    }

    public Run run(Long userId, LocalDate today) {
        LocalDate lastFailure = summaries.findLastFailure(userId, today).orElse(BEGINNING);
        DailySummaryRepository.Successes s = summaries.countSuccesses(userId, lastFailure, today);
        return new Run((int) s.getDayCount(), s.getFirstDate(), s.getLastDate());
    }
}
