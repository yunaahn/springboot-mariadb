package com.example.app.character;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String speciesCode;
    private String name;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int level = 1;
    private int exp;
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int stage;
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer activeMarker = 1;
    private Instant completedAt;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    @Version
    private int version;

    public GameCharacter(Long userId, String speciesCode, String name) {
        this.userId = userId;
        this.speciesCode = speciesCode;
        this.name = name;
    }

    public void complete(Instant now) {
        this.activeMarker = null;
        this.completedAt = now;
    }

    public boolean isActive() {
        return activeMarker != null;
    }

    public int nextLevelExp() {
        return Growth.nextLevelExp(level);
    }

    public void gain(int amount, Instant now) {
        exp += amount;
        while (level < Growth.MAX_LEVEL && exp >= nextLevelExp()) {
            exp -= nextLevelExp();
            level++;
        }
        stage = Growth.stage(level);
        if (level == Growth.MAX_LEVEL) {
            exp = 0;
            complete(now);
        }
    }

    /** 레벨은 내리지 않고 현재 레벨의 경험치 안에서만 회수한다. 실제 회수량을 돌려준다. */
    public int reclaim(int amount) {
        int reclaimed = isActive() ? Math.min(exp, amount) : 0;
        exp -= reclaimed;
        return reclaimed;
    }
}
