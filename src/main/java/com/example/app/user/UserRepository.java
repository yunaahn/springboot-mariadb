package com.example.app.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    /** 같은 사용자의 변경 요청을 한 번에 하나씩 처리하기 위한 잠금. 트랜잭션의 첫 조회여야 한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id and u.deletedAt is null")
    Optional<User> findForUpdate(Long id);
}
