package com.example.app.ocr;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ocr_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OcrJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String imageKey;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private OcrStatus status = OcrStatus.PENDING;
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(columnDefinition = "mediumtext")
    private String rawText;
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String result;
    private Long matchedProductId;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    private Instant finishedAt;

    public OcrJob(Long userId, String imageKey) {
        this.userId = userId;
        this.imageKey = imageKey;
    }
}
