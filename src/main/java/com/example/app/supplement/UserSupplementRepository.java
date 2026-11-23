package com.example.app.supplement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserSupplementRepository extends JpaRepository<UserSupplement, Long> {

    List<UserSupplement> findByUserIdOrderById(Long userId);

    List<UserSupplement> findByUserIdAndStatusOrderById(Long userId, SupplementStatus status);

    @Query("""
            select distinct s.userId from UserSupplement s, User u
            where u.id = s.userId and u.deletedAt is null
            """)
    List<Long> findUserIds();
}
