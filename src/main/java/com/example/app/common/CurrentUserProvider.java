package com.example.app.common;

import java.util.Optional;

/** 소셜 로그인(JWT)이 준비되면 그 구현으로 바꾼다. 구현이 없으면 모든 API가 401이다. */
public interface CurrentUserProvider {

    Optional<Long> currentUserId();
}
