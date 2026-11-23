package com.example.app.reward;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    List<RewardLedger> findByUserIdAndSourceTypeAndSourceIdInOrderById(
            Long userId, RewardSourceType sourceType, Collection<Long> sourceIds);

    List<RewardLedger> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long beforeId, Limit limit);
}
