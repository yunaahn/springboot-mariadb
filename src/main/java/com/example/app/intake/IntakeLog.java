package com.example.app.intake;

import jakarta.persistence.*;
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
}
