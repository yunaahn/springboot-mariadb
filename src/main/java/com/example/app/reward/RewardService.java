package com.example.app.reward;

import com.example.app.character.GameCharacter;
import com.example.app.character.GameCharacterRepository;
import com.example.app.character.UserStats;
import com.example.app.character.UserStatsRepository;
import com.example.app.reward.RewardDtos.Bonus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보상 지급·회수. 사용자 잠금을 잡은 뒤 호출해야 한다.
 * 같은 원천에 이미 지급된 순수량을 빼고 지급하므로, 취소로 다 회수하지 못했어도 다시 체크해서 더 받을 수 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RewardService {

    /** 하루 체크 경험치. 그날 일정 수로 나누므로 영양제 개수와 상관없이 같다. */
    private static final int DAILY_INTAKE_EXP = 80;
    private static final int DAY_COMPLETE_EXP = 20;
    private static final Map<Integer, Integer> STREAK_EXP = new TreeMap<>(Map.of(7, 100, 30, 500, 100, 2000));

    private final RewardLedgerRepository ledger;
    private final GameCharacterRepository characters;
    private final UserStatsRepository stats;
    private final Clock clock;

    public Optional<GameCharacter> activeCharacter(Long userId) {
        return characters.findByUserIdAndActiveMarker(userId, 1);
    }

    public enum Timing {
        ON_TIME(100), LATE(50), PAST(20);

        private final int percent;

        Timing(int percent) {
            this.percent = percent;
        }
    }

    /** 복용 체크 보상. 실제 지급량을 돌려준다. 소급분은 오늘의 상한을 쓰지 않는다. */
    public int grantIntake(Long userId, Long intakeId, Timing timing, int scheduledCount, LocalDate today) {
        int exp = (DAILY_INTAKE_EXP * timing.percent + 100 * scheduledCount - 1) / (100 * scheduledCount);
        if (timing == Timing.PAST) {
            return grant(userId, RewardSourceType.INTAKE, intakeId, exp, Integer.MAX_VALUE);
        }
        UserStats s = statsOf(userId);
        int granted = grant(userId, RewardSourceType.INTAKE, intakeId, exp, DAILY_INTAKE_EXP - s.dailyExpOn(today));
        s.addDailyExp(today, granted);
        return granted;
    }

    /** 하루 완료·스트릭 달성 보너스. 1일 상한과 별개다. */
    public List<Bonus> grantBonuses(Long userId, LocalDate today, boolean dayCompleted, StreakService.Run run) {
        List<Bonus> bonuses = new ArrayList<>();
        if (dayCompleted) {
            addBonus(bonuses, RewardSourceType.DAY_COMPLETE,
                    grant(userId, RewardSourceType.DAY_COMPLETE, number(today), DAY_COMPLETE_EXP, Integer.MAX_VALUE));
        }
        STREAK_EXP.forEach((days, exp) -> {
            if (run.length() >= days) {
                addBonus(bonuses, RewardSourceType.STREAK,
                        grant(userId, RewardSourceType.STREAK, streakId(run, days), exp, Integer.MAX_VALUE));
            }
        });
        statsOf(userId).updateStreak(run.length(), run.last());
        return bonuses;
    }

    /** 체크 취소: 체크 보상과 더 이상 조건을 채우지 못하는 보너스를 회수한다. */
    public int revoke(Long userId, Long intakeId, LocalDate today, boolean dayCompleted, StreakService.Run run) {
        int intakeExp = reclaim(userId, RewardSourceType.INTAKE, intakeId);
        UserStats s = statsOf(userId);
        s.addDailyExp(today, -intakeExp);
        int total = intakeExp;
        if (!dayCompleted) {
            total += reclaim(userId, RewardSourceType.DAY_COMPLETE, number(today));
        }
        if (run.length() > 0) {
            for (int days : STREAK_EXP.keySet()) {
                if (run.length() < days) {
                    total += reclaim(userId, RewardSourceType.STREAK, streakId(run, days));
                }
            }
        }
        s.updateStreak(run.length(), run.last());
        return total;
    }

    private int grant(Long userId, RewardSourceType type, Long sourceId, int target, int limit) {
        GameCharacter character = activeCharacter(userId).orElse(null);
        if (character == null) {
            return 0;
        }
        List<RewardLedger> given = find(userId, type, List.of(sourceId));
        int net = sum(given) + sum(cancels(userId, given));
        int amount = Math.min(target - net, limit);
        if (amount <= 0) {
            return 0;
        }
        ledger.save(new RewardLedger(userId, character.getId(), type, sourceId, given.size(), amount));
        character.gain(amount, clock.instant());
        return amount;
    }

    private int reclaim(Long userId, RewardSourceType type, Long sourceId) {
        List<RewardLedger> given = find(userId, type, List.of(sourceId));
        List<RewardLedger> cancels = cancels(userId, given);
        int total = 0;
        for (RewardLedger reward : given) {
            List<RewardLedger> own = cancels.stream().filter(c -> c.getSourceId().equals(reward.getId())).toList();
            int remaining = reward.getExp() + sum(own);
            int amount = remaining <= 0 ? 0
                    : characters.findById(reward.getCharacterId()).map(c -> c.reclaim(remaining)).orElse(0);
            if (amount > 0) {
                ledger.save(new RewardLedger(userId, reward.getCharacterId(), RewardSourceType.CANCEL,
                        reward.getId(), own.size(), -amount));
                total += amount;
            }
        }
        return total;
    }

    private List<RewardLedger> cancels(Long userId, List<RewardLedger> given) {
        return given.isEmpty() ? List.of()
                : find(userId, RewardSourceType.CANCEL, given.stream().map(RewardLedger::getId).toList());
    }

    private List<RewardLedger> find(Long userId, RewardSourceType type, List<Long> sourceIds) {
        return ledger.findByUserIdAndSourceTypeAndSourceIdInOrderById(userId, type, sourceIds);
    }

    private UserStats statsOf(Long userId) {
        return stats.findById(userId).orElseGet(() -> stats.save(new UserStats(userId)));
    }

    private static int sum(List<RewardLedger> list) {
        return list.stream().mapToInt(RewardLedger::getExp).sum();
    }

    private static void addBonus(List<Bonus> bonuses, RewardSourceType type, int exp) {
        if (exp > 0) {
            bonuses.add(new Bonus(type, exp));
        }
    }

    private static long number(LocalDate date) {
        return Long.parseLong(date.format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    /** 스트릭 시작일(yyyymmdd) 뒤에 달성 일수 세 자리. 한 번의 스트릭에서 단계별로 한 번만 지급된다. */
    private static long streakId(StreakService.Run run, int days) {
        return number(run.start()) * 1000 + days;
    }
}
