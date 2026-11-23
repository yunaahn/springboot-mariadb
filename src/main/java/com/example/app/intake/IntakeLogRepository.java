package com.example.app.intake;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntakeLogRepository extends JpaRepository<IntakeLog, Long> {

    List<IntakeLog> findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanOrderByScheduledAtAscIdAsc(
            Long userId, Instant from, Instant to);

    List<IntakeLog> findByUserSupplementIdAndStatusAndScheduledAtGreaterThan(
            Long userSupplementId, IntakeStatus status, Instant after);

    List<IntakeLog> findByUserSupplementId(Long userSupplementId);

    List<IntakeLog> findByUserIdAndStatusAndScheduledAtLessThan(Long userId, IntakeStatus status, Instant before);

    List<IntakeLog> findByIdIn(Collection<Long> ids);
}
