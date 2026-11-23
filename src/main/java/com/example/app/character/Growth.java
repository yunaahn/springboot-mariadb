package com.example.app.character;

/** 성장 규칙. 레벨 n에서 다음 레벨까지 100 + 50(n-1), 완전체까지 총 10,450 경험치. */
final class Growth {

    static final int MAX_LEVEL = 20;

    private Growth() {
    }

    static int nextLevelExp(int level) {
        return level >= MAX_LEVEL ? 0 : 100 + 50 * (level - 1);
    }

    /** 0알 1유아 2성장 3완전체 */
    static int stage(int level) {
        return level >= MAX_LEVEL ? 3 : level >= 10 ? 2 : level >= 5 ? 1 : 0;
    }
}
