package com.example.app.user;

import com.example.app.common.CurrentUserProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 로그인 구현 전까지 dev 프로필에서만 쓰는 개발용 사용자. */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevUserProvider implements CurrentUserProvider, ApplicationRunner {

    private static final String PROVIDER_USER_ID = "dev-local-user";

    private final UserRepository users;

    @Override
    public void run(ApplicationArguments args) {
        if (currentUserId().isEmpty()) {
            users.save(new User(OAuthProvider.KAKAO, PROVIDER_USER_ID, "개발용 사용자"));
        }
    }

    @Override
    public Optional<Long> currentUserId() {
        return users.findByProviderAndProviderUserId(OAuthProvider.KAKAO, PROVIDER_USER_ID).map(User::getId);
    }
}
