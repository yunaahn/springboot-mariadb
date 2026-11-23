package com.example.app.user;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_consents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ConsentType consentType;
    private String version;
    private boolean agreed;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public UserConsent(Long userId, ConsentType consentType, String version, boolean agreed) {
        this.userId = userId;
        this.consentType = consentType;
        this.version = version;
        this.agreed = agreed;
    }
}
