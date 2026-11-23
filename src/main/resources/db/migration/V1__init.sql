-- 영양제 관리 앱 초기 스키마 (모든 DATETIME은 UTC)
-- [명세 보완] 표시는 노션 ERD와 다른 부분

-- 회원 ----------------------------------------------------------------

CREATE TABLE users (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider          VARCHAR(20)  NOT NULL COMMENT 'KAKAO, GOOGLE',
    provider_user_id  VARCHAR(100) NOT NULL,
    nickname          VARCHAR(50)  NULL,
    timezone          VARCHAR(40)  NOT NULL DEFAULT 'Asia/Seoul',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at        DATETIME(3)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_provider (provider, provider_user_id),
    KEY idx_users_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_profiles (
    user_id        BIGINT UNSIGNED NOT NULL,
    gender_enc     VARBINARY(64)  NULL COMMENT 'AES-256 암호화',
    birth_year     SMALLINT       NULL,
    body_enc       VARBINARY(256) NULL COMMENT '키·체중 JSON 암호화',
    lifestyle_enc  VARBINARY(512) NULL COMMENT '[명세 보완] 생활패턴 JSON 암호화',
    concerns_enc   VARBINARY(512) NULL COMMENT '건강 고민 암호화',
    updated_at     DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_consents (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NOT NULL,
    consent_type  VARCHAR(30) NOT NULL COMMENT 'TERMS, PRIVACY, SENSITIVE, MARKETING',
    version       VARCHAR(10) NOT NULL,
    agreed        TINYINT(1)  NOT NULL,
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_consents_user_type (user_id, consent_type, created_at),
    CONSTRAINT fk_consents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [명세 보완] 로그아웃 시 Refresh Token 무효화용
CREATE TABLE refresh_tokens (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    token_hash  CHAR(64)    NOT NULL COMMENT 'SHA-256(token)',
    expires_at  DATETIME(3) NOT NULL,
    revoked_at  DATETIME(3) NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token (token_hash),
    KEY idx_refresh_user (user_id),
    KEY idx_refresh_expires (expires_at),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [명세 보완] SET-02 알림 설정
CREATE TABLE notification_settings (
    user_id              BIGINT UNSIGNED NOT NULL,
    enabled              TINYINT(1)  NOT NULL DEFAULT 1,
    remind_interval_min  INT         NOT NULL DEFAULT 30 COMMENT '15, 30, 60',
    remind_max_count     INT         NOT NULL DEFAULT 1 COMMENT '0~3',
    refill_enabled       TINYINT(1)  NOT NULL DEFAULT 1,
    updated_at           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE push_subscriptions (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    platform         VARCHAR(10)   NOT NULL COMMENT 'WEB, ANDROID',
    endpoint         VARCHAR(1000) NOT NULL,
    endpoint_hash    CHAR(64)      NOT NULL COMMENT 'SHA-256(endpoint)',
    p256dh           VARCHAR(200)  NULL,
    auth_key         VARCHAR(100)  NULL,
    last_success_at  DATETIME(3)   NULL,
    created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_endpoint (endpoint_hash),
    KEY idx_push_user (user_id),
    CONSTRAINT fk_push_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 제품·성분 -----------------------------------------------------------

CREATE TABLE products (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL,
    manufacturer    VARCHAR(100) NULL,
    mfds_report_no  VARCHAR(30)  NULL COMMENT '식약처 품목제조번호',
    image_url       VARCHAR(500) NULL,
    serving_unit    VARCHAR(10)  NOT NULL DEFAULT '정',
    source          VARCHAR(10)  NOT NULL COMMENT 'MFDS, USER',
    created_by      BIGINT UNSIGNED NULL COMMENT 'source=USER일 때 등록자',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_mfds (mfds_report_no),
    KEY idx_products_name (name),
    KEY idx_products_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE nutrients (
    id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code      VARCHAR(30) NOT NULL COMMENT 'VIT_D, MAGNESIUM ...',
    name_ko   VARCHAR(50) NOT NULL,
    unit      VARCHAR(10) NOT NULL COMMENT '표준 단위 mg, μg ...',
    category  VARCHAR(20) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_nutrients_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_nutrients (
    product_id   BIGINT UNSIGNED NOT NULL,
    nutrient_id  INT UNSIGNED    NOT NULL,
    amount       DECIMAL(12,4)   NOT NULL COMMENT '1회 섭취량당, 표준 단위 환산값',
    PRIMARY KEY (product_id, nutrient_id),
    KEY idx_pn_nutrient (nutrient_id),
    CONSTRAINT fk_pn_product  FOREIGN KEY (product_id)  REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_pn_nutrient FOREIGN KEY (nutrient_id) REFERENCES nutrients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE nutrient_references (
    id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nutrient_id  INT UNSIGNED NOT NULL,
    gender       CHAR(1)  NOT NULL COMMENT 'M, F',
    age_min      TINYINT UNSIGNED NOT NULL,
    age_max      TINYINT UNSIGNED NOT NULL,
    rni          DECIMAL(12,4) NULL COMMENT '권장섭취량',
    ul           DECIMAL(12,4) NULL COMMENT '상한섭취량',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ref (nutrient_id, gender, age_min),
    CONSTRAINT fk_ref_nutrient FOREIGN KEY (nutrient_id) REFERENCES nutrients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ocr_jobs (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    image_key           VARCHAR(300) NOT NULL COMMENT '오브젝트 스토리지 키',
    status              VARCHAR(10)  NOT NULL COMMENT 'PENDING, RUNNING, DONE, FAILED',
    raw_text            MEDIUMTEXT   NULL,
    result              JSON         NULL COMMENT '추출 제품명·성분 목록',
    matched_product_id  BIGINT UNSIGNED NULL,
    created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at         DATETIME(3)  NULL,
    PRIMARY KEY (id),
    KEY idx_ocr_user (user_id, created_at),
    KEY idx_ocr_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 복용 ----------------------------------------------------------------

CREATE TABLE user_supplements (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    product_id       BIGINT UNSIGNED NOT NULL,
    alias            VARCHAR(50)   NULL COMMENT '사용자 별칭',
    dose_per_intake  DECIMAL(6,2)  NOT NULL DEFAULT 1,
    meal_timing      VARCHAR(10)   NOT NULL DEFAULT 'ANY' COMMENT 'BEFORE, AFTER, ANY',
    days_of_week     TINYINT UNSIGNED NOT NULL DEFAULT 127 COMMENT '비트마스크 월=1 ... 일=64',
    total_qty        DECIMAL(8,2)  NULL,
    remaining_qty    DECIMAL(8,2)  NULL,
    status           VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PAUSED, ENDED',
    started_at       DATE          NOT NULL,
    ended_at         DATE          NULL,
    created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_us_user_status (user_id, status),
    KEY idx_us_product (product_id),
    CONSTRAINT fk_us_user    FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_us_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplement_schedules (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_supplement_id  BIGINT UNSIGNED NOT NULL,
    intake_time         TIME NOT NULL COMMENT '사용자 로컬 시각',
    is_active           TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule (user_supplement_id, intake_time),
    CONSTRAINT fk_sch_us FOREIGN KEY (user_supplement_id) REFERENCES user_supplements (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE intake_logs (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL COMMENT '반정규화: 조인 없이 사용자별 조회',
    user_supplement_id  BIGINT UNSIGNED NOT NULL,
    schedule_id         BIGINT UNSIGNED NOT NULL,
    scheduled_at        DATETIME NOT NULL COMMENT 'UTC',
    status              VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, TAKEN, LATE, SKIPPED, MISSED',
    taken_at            DATETIME(3) NULL,
    notified_at         DATETIME(3) NULL,
    remind_count        TINYINT UNSIGNED NOT NULL DEFAULT 0,
    next_remind_at      DATETIME NULL,
    cancel_count        INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '[명세 보완] 취소 횟수 = 다음 보상의 source_seq',
    PRIMARY KEY (id),
    UNIQUE KEY uk_intake_slot (schedule_id, scheduled_at),
    KEY idx_intake_user_time (user_id, scheduled_at),
    KEY idx_intake_dispatch (status, scheduled_at),
    KEY idx_intake_remind (next_remind_at),
    KEY idx_intake_us (user_supplement_id, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 캐릭터·보상 ---------------------------------------------------------

CREATE TABLE characters (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    species_code   VARCHAR(20) NOT NULL,
    name           VARCHAR(30) NOT NULL,
    level          SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    exp            INT UNSIGNED NOT NULL DEFAULT 0,
    stage          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0알 1유아 2성장 3완전체',
    active_marker  TINYINT UNSIGNED NULL DEFAULT 1 COMMENT '진행 중=1, 완료=NULL',
    completed_at   DATETIME(3) NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    version        INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '낙관적 락',
    PRIMARY KEY (id),
    UNIQUE KEY uk_char_active (user_id, active_marker),
    CONSTRAINT fk_char_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_stats (
    user_id           BIGINT UNSIGNED NOT NULL,
    current_streak    INT UNSIGNED NOT NULL DEFAULT 0,
    best_streak       INT UNSIGNED NOT NULL DEFAULT 0,
    last_streak_date  DATE NULL COMMENT '사용자 로컬 날짜',
    shield_count      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    daily_exp_date    DATE NULL,
    daily_exp         INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1일 경험치 상한 체크용',
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_stats_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reward_ledger (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NOT NULL,
    character_id  BIGINT UNSIGNED NOT NULL,
    source_type   VARCHAR(20) NOT NULL COMMENT 'INTAKE, DAY_COMPLETE, STREAK, AD, CANCEL',
    source_id     BIGINT UNSIGNED NOT NULL COMMENT 'intake_log id / 날짜(yyyymmdd) / CANCEL은 취소 대상 reward_ledger id',
    source_seq    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '[명세 보완] 취소 후 재지급 구분',
    exp           INT NOT NULL COMMENT '취소 시 음수',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reward_source (user_id, source_type, source_id, source_seq),
    KEY idx_reward_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE daily_summaries (
    user_id          BIGINT UNSIGNED NOT NULL,
    summary_date     DATE NOT NULL COMMENT '사용자 로컬 날짜',
    scheduled_count  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    taken_count      SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    ontime_count     SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
