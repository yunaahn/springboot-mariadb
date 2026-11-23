package com.example.app.intake;

import com.example.app.supplement.UserSupplementRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 사용자마다 자정이 달라서 한 시간마다 확인한다. 빠진 일정만 만들므로 여러 번 실행해도 된다. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.intake-scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class IntakeScheduler {

    private final UserSupplementRepository supplements;
    private final IntakePlanner planner;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    void planAll() {
        for (Long userId : supplements.findUserIds()) {
            try {
                planner.planUser(userId);
            } catch (RuntimeException e) {
                log.error("복용 일정 생성 실패: userId={}", userId, e);
            }
        }
    }
}
