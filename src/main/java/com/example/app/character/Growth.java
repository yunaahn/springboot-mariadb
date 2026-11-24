package com.example.app.character;

/** 성장 규칙. 매일 다 챙기면(하루 100) 약 3일에 유아, 2주에 성장, 2달에 완전체가 된다. */
final class Growth {

    static final int MAX_LEVEL = 20;

    /** 레벨 1~19에서 다음 레벨까지. 누적: 유아(5) 250, 성장(10) 1500, 완전체(20) 6000. */
    private static final int[] NEXT = {40, 40, 80, 90, 150, 200, 250, 300, 350,
            450, 450, 450, 450, 450, 450, 450, 450, 450, 450};

    private Growth() {
    }

    static int nextLevelExp(int level) {
        return level >= MAX_LEVEL ? 0 : NEXT[level - 1];
    }

    /** 0알 1유아 2성장 3완전체 */
    static int stage(int level) {
        return level >= MAX_LEVEL ? 3 : level >= 10 ? 2 : level >= 5 ? 1 : 0;
    }
}
