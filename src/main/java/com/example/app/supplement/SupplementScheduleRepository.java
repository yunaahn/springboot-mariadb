package com.example.app.supplement;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplementScheduleRepository extends JpaRepository<SupplementSchedule, Long> {

    List<SupplementSchedule> findByUserSupplementIdInOrderByIntakeTime(Collection<Long> userSupplementIds);
}
