package com.example.app.intake;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "daily_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(DailySummaryId.class)
public class DailySummary {

    @Id
    private Long userId;
    @Id
    private LocalDate summaryDate;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int scheduledCount;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int takenCount;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int ontimeCount;

    public DailySummary(Long userId, LocalDate summaryDate) {
        this.userId = userId;
        this.summaryDate = summaryDate;
    }

    public void update(int scheduled, int taken, int ontime) {
        scheduledCount = scheduled;
        takenCount = taken;
        ontimeCount = ontime;
    }
}
