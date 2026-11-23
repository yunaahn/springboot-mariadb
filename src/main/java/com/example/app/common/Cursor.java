package com.example.app.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/** 목록 커서. 마지막 항목의 id를 감싼 값이다. */
public final class Cursor {

    private Cursor() {
    }

    public static long decode(String cursor, long first) {
        if (cursor == null) {
            return first;
        }
        try {
            return Long.parseLong(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("cursor 값이 올바르지 않아요.");
        }
    }

    /** found는 size + 1개까지 조회한 결과. */
    public static <E, T> PageResponse<T> page(List<E> found, int size, Function<E, Long> id, Function<E, T> mapper) {
        boolean hasNext = found.size() > size;
        List<E> page = hasNext ? found.subList(0, size) : found;
        String next = hasNext ? encode(id.apply(page.get(size - 1))) : null;
        return new PageResponse<>(page.stream().map(mapper).toList(), new PageResponse.Page(next, hasNext));
    }

    private static String encode(long id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(id).getBytes(StandardCharsets.UTF_8));
    }
}
