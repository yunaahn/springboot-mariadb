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

http://localhost:8081 으로 접속합니다. 테스트는 H2 인메모리 DB로 실행되어 MariaDB가 필요 없습니다.
