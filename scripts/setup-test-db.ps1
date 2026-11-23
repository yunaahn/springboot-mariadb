# 테스트 전용 appdb_test/apptest를 만들고 접속 비밀번호를 로컬 파일에 저장한다.
# 저장 파일(test-db.local.properties)은 .gitignore 대상이다.
$ErrorActionPreference = 'Stop'
$client = 'C:\Program Files\MariaDB 12.3\bin\mariadb.exe'
if (-not (Test-Path $client)) { throw "MariaDB 클라이언트를 찾을 수 없습니다: $client" }
$file = Join-Path (Split-Path $PSScriptRoot -Parent) 'test-db.local.properties'

$bytes = New-Object byte[] 24
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$password = [System.BitConverter]::ToString($bytes).Replace('-', '').ToLower()

$sql = @"
CREATE DATABASE IF NOT EXISTS appdb_test CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'apptest'@'localhost' IDENTIFIED BY '$password';
ALTER USER 'apptest'@'localhost' IDENTIFIED BY '$password';
GRANT ALL PRIVILEGES ON appdb_test.* TO 'apptest'@'localhost';
"@

$OutputEncoding = [System.Text.Encoding]::ASCII
Write-Host 'MariaDB root 비밀번호를 입력하세요. (테스트 DB 생성에만 사용)'
for ($try = 1; $try -le 3; $try++) {
    $sql | & $client -u root -p
    if ($LASTEXITCODE -eq 0) { break }
    if ($try -eq 3) { throw '테스트 DB 설정에 실패했습니다. 위 오류 내용을 확인하세요.' }
    Write-Host '-> 실패했습니다. root 비밀번호를 다시 입력하세요.' -ForegroundColor Yellow
}

Set-Content -Path $file -Value "TEST_DB_PASSWORD=$password" -Encoding ascii
Write-Host '테스트 DB 준비 완료: appdb_test / apptest' -ForegroundColor Green
