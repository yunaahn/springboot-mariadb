package com.example.app.common;

import java.util.List;

public record PageResponse<T>(List<T> data, Page page) {

    public record Page(String nextCursor, boolean hasNext) {
    }
}
