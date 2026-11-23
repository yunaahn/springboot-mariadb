package com.example.app.intake;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "intake_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntakeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long userSupplementId;
    private Long scheduleId;
    private Instant scheduledAt;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private IntakeStatus status = IntakeStatus.PENDING;
    private Instant takenAt;
    @Column(precision = 8, scale = 2)
    private BigDecimal takenQty;
    private Instant notifiedAt;
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int remindCount;
    private Instant nextRemindAt;
    private int cancelCount;

    public IntakeLog(Long userId, Long userSupplementId, Long scheduleId, Instant scheduledAt) {
        this.userId = userId;
        this.userSupplementId = userSupplementId;
        this.scheduleId = scheduleId;
        this.scheduledAt = scheduledAt;
    }

    public void take(IntakeStatus result, Instant at, BigDecimal consumedQty) {
        status = result;
        takenAt = at;
        takenQty = consumedQty;
        nextRemindAt = null;
    }

    public void skip() {
        status = IntakeStatus.SKIPPED;
        nextRemindAt = null;
    }

    public void miss() {
        status = IntakeStatus.MISSED;
        nextRemindAt = null;
    }

    /** 체크·건너뛰기를 예정 상태로 되돌린다. */
    public void cancel() {
        if (isTaken()) {
            cancelCount++;
        }
        status = IntakeStatus.PENDING;
        takenAt = null;
        takenQty = null;
    }

    public boolean isTaken() {
        return status == IntakeStatus.TAKEN || status == IntakeStatus.LATE;
    }
}
