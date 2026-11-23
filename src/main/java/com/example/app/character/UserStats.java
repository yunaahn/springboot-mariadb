package com.example.app.character;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStats {

    @Id
    private Long userId;
    private int currentStreak;
    private int bestStreak;
    private LocalDate lastStreakDate;
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int shieldCount;
    private LocalDate dailyExpDate;
    private int dailyExp;
    @UpdateTimestamp
    private Instant updatedAt;

    public UserStats(Long userId) {
        this.userId = userId;
    }
}
