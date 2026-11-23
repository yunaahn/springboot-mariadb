package com.example.app.intake;

import com.example.app.common.ApiException;
import com.example.app.character.GameCharacter;
import com.example.app.intake.IntakeDtos.CancelResponse;
import com.example.app.intake.IntakeDtos.DayResponse;
import com.example.app.intake.IntakeDtos.Item;
import com.example.app.intake.IntakeDtos.Progress;
import com.example.app.intake.IntakeDtos.Slot;
import com.example.app.intake.IntakeDtos.StatusResponse;
import com.example.app.intake.IntakeDtos.TakeResponse;
import com.example.app.intake.IntakeDtos.TakeResult;
import com.example.app.product.Product;
import com.example.app.product.ProductService;
import com.example.app.reward.RewardDtos;
import com.example.app.reward.RewardService;
import com.example.app.reward.StreakService;
import com.example.app.supplement.UserSupplement;
import com.example.app.supplement.UserSupplementRepository;
import com.example.app.user.User;
import com.example.app.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class IntakeService {

    private static final Duration ON_TIME = Duration.ofMinutes(30);

    private final UserRepository users;
    private final IntakeLogRepository logs;
    private final UserSupplementRepository supplements;
    private final ProductService products;
    private final IntakePlanner planner;
    private final RewardService rewards;
    private final StreakService streaks;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DayResponse day(Long userId, LocalDate requested) {
        User user = users.findByIdAndDeletedAtIsNull(userId).orElseThrow(ApiException::unauthorized);
        LocalDate date = requested == null ? planner.today(user) : requested;
        ZoneId zone = planner.zone(user);
        List<IntakeLog> day = planner.logsOn(user, date);
        Map<Long, UserSupplement> byId = supplementsOf(day);
        Map<Long, Product> productById = products.findAll(
                byId.values().stream().map(UserSupplement::getProductId).collect(Collectors.toSet()));

        Map<LocalTime, List<Item>> slots = new TreeMap<>();
        for (IntakeLog log : day) {
            UserSupplement s = byId.get(log.getUserSupplementId());
            String name = s.getAlias() != null ? s.getAlias() : productById.get(s.getProductId()).getName();
            slots.computeIfAbsent(LocalTime.ofInstant(log.getScheduledAt(), zone), t -> new ArrayList<>())
                    .add(new Item(log.getId(), s.getId(), name, s.getDosePerIntake(), s.getMealTiming(),
                            log.getStatus(), log.getTakenAt() == null ? null
                            : log.getTakenAt().atZone(zone).toOffsetDateTime()));
        }
        int taken = (int) day.stream().filter(IntakeLog::isTaken).count();
        return new DayResponse(date, new Progress(day.size(), taken),
                slots.entrySet().stream().map(e -> new Slot(e.getKey(), e.getValue())).toList());
    }

    public TakeResponse take(Long userId, Set<Long> intakeIds) {
        User user = lockUser(userId);
        List<IntakeLog> found = logs.findByIdIn(intakeIds);
        if (found.size() != intakeIds.size()) {
            throw notFound();
        }
        if (found.stream().anyMatch(l -> !l.getUserId().equals(userId))) {
            throw ApiException.forbidden();
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        LocalDate today = planner.today(user);
        Before before = before(userId);
        Map<Long, UserSupplement> byId = supplementsOf(found);
        List<TakeResult> results = new ArrayList<>();
        boolean changed = false;
        for (IntakeLog log : found.stream().sorted(Comparator.comparing(IntakeLog::getId)).toList()) {
            if (log.getStatus() != IntakeStatus.PENDING) {
                results.add(new TakeResult(log.getId(), "ALREADY_PROCESSED", 0));
                continue;
            }
            checkTodayOrYesterday(user, log);
            boolean past = planner.dateOf(user, log).isBefore(today);
            // 소급 체크는 자정 직후라도 지연 기준으로 보상한다.
            boolean onTime = !past && Duration.between(log.getScheduledAt(), now).abs().compareTo(ON_TIME) <= 0;
            log.take(onTime ? IntakeStatus.TAKEN : IntakeStatus.LATE, now,
                    byId.get(log.getUserSupplementId()).consume());
            int exp = past ? RewardService.PAST_EXP : onTime ? RewardService.ON_TIME_EXP : RewardService.LATE_EXP;
            results.add(new TakeResult(log.getId(), log.getStatus().name(),
                    rewards.grantIntake(userId, log.getId(), exp, today)));
            changed = true;
        }
        summarizeDatesOf(user, found);
        boolean completed = dayCompleted(user, today);
        StreakService.Run run = streaks.run(userId, today);
        List<RewardDtos.Bonus> bonuses = changed
                ? rewards.grantBonuses(userId, today, completed, run) : List.of();
        return new TakeResponse(results, completed, bonuses, after(before), new RewardDtos.Streak(run.length()));
    }

    public CancelResponse cancel(Long userId, Long intakeId) {
        User user = lockUser(userId);
        IntakeLog log = getOwned(userId, intakeId);
        LocalDate today = planner.today(user);
        if (!planner.dateOf(user, log).equals(today) || log.getStatus() == IntakeStatus.MISSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANCEL_NOT_ALLOWED", "오늘 기록만 취소할 수 있어요.");
        }
        Before before = before(userId);
        boolean taken = log.isTaken();
        if (log.getStatus() != IntakeStatus.PENDING) {
            supplements.findById(log.getUserSupplementId()).orElseThrow().restore(log.getTakenQty());
            log.cancel();
            summarizeDatesOf(user, List.of(log));
        }
        StreakService.Run run = streaks.run(userId, today);
        int reclaimed = taken ? rewards.revoke(userId, intakeId, today, dayCompleted(user, today), run) : 0;
        return new CancelResponse(log.getId(), log.getStatus(), reclaimed, after(before),
                new RewardDtos.Streak(run.length()));
    }

    private boolean dayCompleted(User user, LocalDate date) {
        List<IntakeLog> day = planner.logsOn(user, date);
        return !day.isEmpty() && day.stream().allMatch(IntakeLog::isTaken);
    }

    private record Before(GameCharacter character, int level, int stage) {
    }

    private Before before(Long userId) {
        return rewards.activeCharacter(userId).map(c -> new Before(c, c.getLevel(), c.getStage())).orElse(null);
    }

    private RewardDtos.CharacterState after(Before before) {
        if (before == null) {
            return null;
        }
        GameCharacter c = before.character();
        return new RewardDtos.CharacterState(c.getLevel(), c.getExp(), c.nextLevelExp(), c.getStage(),
                c.getLevel() > before.level(), c.getStage() > before.stage(), !c.isActive());
    }

    public StatusResponse skip(Long userId, Long intakeId) {
        User user = lockUser(userId);
        IntakeLog log = getOwned(userId, intakeId);
        if (log.getStatus() == IntakeStatus.PENDING) {
            checkTodayOrYesterday(user, log);
            log.skip();
        } else if (log.getStatus() != IntakeStatus.SKIPPED) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_PROCESSED", "이미 처리된 기록이에요.");
        }
        return new StatusResponse(log.getId(), log.getStatus());
    }

    private void checkTodayOrYesterday(User user, IntakeLog log) {
        LocalDate today = planner.today(user);
        LocalDate date = planner.dateOf(user, log);
        if (date.isAfter(today) || date.isBefore(today.minusDays(1))) {
            throw ApiException.badRequest("오늘과 어제 일정만 처리할 수 있어요.");
        }
    }

    private void summarizeDatesOf(User user, List<IntakeLog> changed) {
        logs.flush();
        planner.summarize(user, changed.stream().map(l -> planner.dateOf(user, l)).collect(Collectors.toSet()));
    }

    private Map<Long, UserSupplement> supplementsOf(List<IntakeLog> list) {
        return supplements.findAllById(list.stream().map(IntakeLog::getUserSupplementId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(UserSupplement::getId, Function.identity()));
    }

    private User lockUser(Long userId) {
        return users.findForUpdate(userId).orElseThrow(ApiException::unauthorized);
    }

    private IntakeLog getOwned(Long userId, Long id) {
        IntakeLog log = logs.findById(id).orElseThrow(this::notFound);
        if (!log.getUserId().equals(userId)) {
            throw ApiException.forbidden();
        }
        return log;
    }

    private ApiException notFound() {
        return ApiException.notFound("복용 일정을 찾을 수 없어요.");
    }
}
