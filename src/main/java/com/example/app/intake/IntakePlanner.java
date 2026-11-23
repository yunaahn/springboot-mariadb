package com.example.app.intake;

import com.example.app.supplement.SupplementSchedule;
import com.example.app.supplement.SupplementScheduleRepository;
import com.example.app.supplement.SupplementStatus;
import com.example.app.supplement.UserSupplement;
import com.example.app.supplement.UserSupplementRepository;
import com.example.app.user.User;
import com.example.app.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 복용 일정(intake_logs)과 일별 집계를 관리한다. 변경 메서드는 사용자 잠금을 잡은 뒤 호출해야 한다. */
@Component
@RequiredArgsConstructor
@Transactional
public class IntakePlanner {

    private final UserRepository users;
    private final UserSupplementRepository supplements;
    private final SupplementScheduleRepository schedules;
    private final IntakeLogRepository logs;
    private final DailySummaryRepository summaries;
    private final Clock clock;

    /** 스케줄러용: 사용자 한 명의 일정을 생성하고 지난 일정을 미복용 처리한다. */
    public void planUser(Long userId) {
        users.findForUpdate(userId).ifPresent(user -> {
            // 전날까지는 소급 체크할 수 있으므로 그저께 이전만 미복용으로 확정한다.
            Instant yesterday = startOf(user, today(user).minusDays(1));
            logs.findByUserIdAndStatusAndScheduledAtLessThan(userId, IntakeStatus.PENDING, yesterday)
                    .forEach(IntakeLog::miss);
            plan(user);
        });
    }

    /** 오늘 남은 시간과 내일의 일정을 빠진 것만 생성한다. */
    public void plan(User user) {
        Instant now = clock.instant();
        LocalDate today = today(user);
        List<LocalDate> dates = List.of(today, today.plusDays(1));
        Map<Long, UserSupplement> active = supplements
                .findByUserIdAndStatusOrderById(user.getId(), SupplementStatus.ACTIVE).stream()
                .collect(Collectors.toMap(UserSupplement::getId, Function.identity()));
        List<SupplementSchedule> times = schedules.findByUserSupplementIdInOrderByIntakeTime(active.keySet());

        for (LocalDate date : dates) {
            Set<Slot> existing = logsOn(user, date).stream()
                    .map(l -> new Slot(l.getScheduleId(), l.getScheduledAt())).collect(Collectors.toSet());
            for (SupplementSchedule schedule : times) {
                UserSupplement supplement = active.get(schedule.getUserSupplementId());
                Instant at = date.atTime(schedule.getIntakeTime()).atZone(zone(user)).toInstant();
                if (schedule.isActive() && supplement.isScheduledOn(date) && at.isAfter(now)
                        && existing.add(new Slot(schedule.getId(), at))) {
                    logs.save(new IntakeLog(user.getId(), supplement.getId(), schedule.getId(), at));
                }
            }
        }
        summarize(user, dates);
    }

    /** 아직 오지 않은 예정 일정을 지운다. 이어서 plan을 호출한다. */
    public void clearFuture(UserSupplement supplement) {
        logs.deleteAll(logs.findByUserSupplementIdAndStatusAndScheduledAtGreaterThan(
                supplement.getId(), IntakeStatus.PENDING, clock.instant()));
        logs.flush();
    }

    public void deleteAll(User user, UserSupplement supplement) {
        List<IntakeLog> all = logs.findByUserSupplementId(supplement.getId());
        // 지난날 집계는 그대로 두어 스트릭이 바뀌지 않게 한다.
        LocalDate today = today(user);
        Set<LocalDate> dates = all.stream().map(l -> dateOf(user, l)).filter(d -> !d.isBefore(today))
                .collect(Collectors.toSet());
        logs.deleteAll(all);
        logs.flush();
        summarize(user, dates);
    }

    public void summarize(User user, Collection<LocalDate> dates) {
        for (LocalDate date : dates) {
            List<IntakeLog> day = logsOn(user, date);
            DailySummary summary = summaries.findById(new DailySummaryId(user.getId(), date))
                    .orElseGet(() -> summaries.save(new DailySummary(user.getId(), date)));
            summary.update(day.size(), (int) day.stream().filter(IntakeLog::isTaken).count(),
                    (int) day.stream().filter(l -> l.getStatus() == IntakeStatus.TAKEN).count());
        }
    }

    @Transactional(readOnly = true)
    public List<IntakeLog> logsOn(User user, LocalDate date) {
        return logs.findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanOrderByScheduledAtAscIdAsc(
                user.getId(), startOf(user, date), startOf(user, date.plusDays(1)));
    }

    public LocalDate today(User user) {
        return LocalDate.ofInstant(clock.instant(), zone(user));
    }

    public LocalDate dateOf(User user, IntakeLog log) {
        return LocalDate.ofInstant(log.getScheduledAt(), zone(user));
    }

    public ZoneId zone(User user) {
        return ZoneId.of(user.getTimezone());
    }

    private Instant startOf(User user, LocalDate date) {
        return date.atStartOfDay(zone(user)).toInstant();
    }

    private record Slot(Long scheduleId, Instant at) {
    }
}
