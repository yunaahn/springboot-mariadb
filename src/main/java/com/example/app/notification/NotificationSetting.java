package com.example.app.notification;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Id
    private Long userId;
    private boolean enabled = true;
    private int remindIntervalMin = 30;
    private int remindMaxCount = 1;
    private boolean refillEnabled = true;
    @UpdateTimestamp
    private Instant updatedAt;

    public NotificationSetting(Long userId) {
        this.userId = userId;
    }
}
