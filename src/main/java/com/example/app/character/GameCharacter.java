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
}
