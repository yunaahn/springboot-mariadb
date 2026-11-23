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
}
