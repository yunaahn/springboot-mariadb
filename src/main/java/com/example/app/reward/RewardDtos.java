package com.example.app.reward;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class RewardDtos {

    private RewardDtos() {
    }

    public record RewardResponse(Long id, RewardSourceType type, int exp, OffsetDateTime createdAt) {
        static RewardResponse of(RewardLedger r, ZoneId zone) {
            return new RewardResponse(r.getId(), r.getSourceType(), r.getExp(),
                    r.getCreatedAt().atZone(zone).toOffsetDateTime());
        }
    }

    public record Bonus(RewardSourceType type, int exp) {
    }

    public record CharacterState(int level, int exp, int nextLevelExp, int stage, boolean leveledUp,
                                 boolean stageChanged, boolean completed) {
    }

    public record Streak(int current) {
    }
}
