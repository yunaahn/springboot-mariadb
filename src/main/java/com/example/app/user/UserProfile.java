package com.example.app.user;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    // *Enc 값은 암호화된 바이트만 저장한다.
    @Id
    private Long userId;
    private byte[] genderEnc;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer birthYear;
    private byte[] bodyEnc;
    private byte[] lifestyleEnc;
    private byte[] concernsEnc;
    @UpdateTimestamp
    private Instant updatedAt;

    public UserProfile(Long userId) {
        this.userId = userId;
    }
}
