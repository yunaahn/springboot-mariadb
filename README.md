# springboot-mariadb

Spring Boot 4.1 / Java 17 / Maven / MariaDB / JavaScript 기본 프로젝트

## DB 준비

```sql
CREATE DATABASE appdb CHARACTER SET utf8mb4;
CREATE USER 'appuser'@'localhost' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON appdb.* TO 'appuser'@'localhost';
```

접속 정보는 환경 변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 지정합니다.

## 빌드 및 실행 (PowerShell)

```powershell
.\mvnw.cmd clean verify
$env:DB_PASSWORD = '<비밀번호>'
.\mvnw.cmd spring-boot:run
```

http://localhost:8081 으로 접속합니다. 테스트는 별도의 MariaDB(`appdb_test`)를 사용하므로, 빌드 전에 `scripts/setup-test-db.ps1`을 한 번 실행하고 MariaDB를 켜 두세요.

## DB 스키마

스키마는 Flyway(`src/main/resources/db/migration`)로만 변경합니다. 앱이 시작될 때 자동 적용됩니다.
한 번 적용된 파일은 수정하지 말고 `V2__...sql`처럼 새 파일을 추가하세요.

노션 ERD와 다른 점:

- `reward_ledger.source_seq`, `intake_logs.cancel_count` 추가: 취소 후 다시 체크해도 보상 지급 가능. `CANCEL`의 `source_id`는 취소 대상 `reward_ledger.id`
- `notification_settings` 추가: 알림 on/off, 리마인드 간격·횟수
- `refresh_tokens` 추가: 로그아웃 시 토큰 무효화
- `user_profiles.lifestyle` → `lifestyle_enc`: 생활패턴도 암호화 저장
- `users.deleted_at` 인덱스 추가: 탈퇴 배치용. 탈퇴 시 개인·건강정보는 즉시 삭제하고 대용량 기록만 배치로 삭제하는 방침
- `intake_logs.taken_qty` 추가: 체크 때 차감한 수량만큼 취소 시 복원

## 개발용 로그인

소셜 로그인 전까지 `dev` 프로필(`$env:SPRING_PROFILES_ACTIVE = 'dev'`)에서만 개발용 사용자로 API를 쓸 수 있고, 이때는 이 PC에서만 접속됩니다. 프로필이 없으면 API는 401을 응답합니다. `scripts/setup-db.ps1`은 `dev`로 실행합니다. 배포할 때는 `dev`를 쓰지 마세요.
