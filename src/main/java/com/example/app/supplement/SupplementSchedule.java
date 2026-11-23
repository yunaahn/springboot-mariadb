package com.example.app.supplement;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supplement_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplementSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userSupplementId;
    private LocalTime intakeTime;
    @Column(name = "is_active")
    private boolean active = true;

    public SupplementSchedule(Long userSupplementId, LocalTime intakeTime) {
        this.userSupplementId = userSupplementId;
        this.intakeTime = intakeTime;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
