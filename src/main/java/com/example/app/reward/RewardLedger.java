package com.example.app.reward;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reward_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long characterId;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private RewardSourceType sourceType;
    private Long sourceId;
    private int sourceSeq;
    private int exp;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public RewardLedger(Long userId, Long characterId, RewardSourceType sourceType, Long sourceId, int sourceSeq, int exp) {
        this.userId = userId;
        this.characterId = characterId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceSeq = sourceSeq;
        this.exp = exp;
    }
}
