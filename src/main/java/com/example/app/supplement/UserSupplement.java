package com.example.app.supplement;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_supplements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSupplement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long productId;
    private String alias;
    @Column(precision = 6, scale = 2)
    private BigDecimal dosePerIntake = BigDecimal.ONE;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private MealTiming mealTiming = MealTiming.ANY;
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int daysOfWeek = 127;
    @Column(precision = 8, scale = 2)
    private BigDecimal totalQty;
    @Column(precision = 8, scale = 2)
    private BigDecimal remainingQty;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private SupplementStatus status = SupplementStatus.ACTIVE;
    private LocalDate startedAt;
    private LocalDate endedAt;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    public UserSupplement(Long userId, Long productId, LocalDate startedAt) {
        this.userId = userId;
        this.productId = productId;
        this.startedAt = startedAt;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setDosePerIntake(BigDecimal dosePerIntake) {
        this.dosePerIntake = dosePerIntake;
    }

    public void setMealTiming(MealTiming mealTiming) {
        this.mealTiming = mealTiming;
    }

    public void setDaysOfWeek(int daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public void setTotalQty(BigDecimal totalQty) {
        this.totalQty = totalQty;
    }

    public void setRemainingQty(BigDecimal remainingQty) {
        this.remainingQty = remainingQty;
    }

    public void changeStatus(SupplementStatus status, LocalDate today) {
        this.status = status;
        this.endedAt = status == SupplementStatus.ENDED ? today : null;
    }

    public boolean isScheduledOn(LocalDate date) {
        return status == SupplementStatus.ACTIVE
                && !date.isBefore(startedAt)
                && (daysOfWeek & (1 << (date.getDayOfWeek().getValue() - 1))) != 0;
    }

    /** 1회 복용량만큼 차감하고 실제 차감한 수량을 돌려준다. 수량 미입력이면 null. */
    public BigDecimal consume() {
        if (remainingQty == null) {
            return null;
        }
        BigDecimal consumed = remainingQty.min(dosePerIntake);
        remainingQty = remainingQty.subtract(consumed);
        return consumed;
    }

    public void restore(BigDecimal consumed) {
        if (remainingQty != null && consumed != null) {
            remainingQty = remainingQty.add(consumed);
        }
    }
}
