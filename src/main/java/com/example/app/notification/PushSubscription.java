package com.example.app.notification;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "push_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private PushPlatform platform;
    @Column(length = 1000)
    private String endpoint;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 64)
    private String endpointHash;
    private String p256dh;
    private String authKey;
    private Instant lastSuccessAt;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public PushSubscription(Long userId, PushPlatform platform, String endpoint, String endpointHash) {
        this.userId = userId;
        this.platform = platform;
        this.endpoint = endpoint;
        this.endpointHash = endpointHash;
    }
}
