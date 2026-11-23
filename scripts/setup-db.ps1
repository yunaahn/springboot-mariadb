# 로컬 MariaDB에 appdb/appuser를 만들고 앱을 실행한다.
# 비밀번호는 사용자가 직접 입력하며 파일에 저장하지 않는다.
$ErrorActionPreference = 'Stop'
$client = 'C:\Program Files\MariaDB 12.3\bin\mariadb.exe'
if (-not (Test-Path $client)) { throw "MariaDB 클라이언트를 찾을 수 없습니다: $client" }

function Read-Secret($prompt) {
    $secure = Read-Host $prompt -AsSecureString
    [System.Net.NetworkCredential]::new('', $secure).Password
}

Write-Host '[1/2] 앱 계정(appuser)이 쓸 새 비밀번호를 정하세요. (영문/숫자/기호 8자 이상)'
while ($true) {
    $password = Read-Secret '새 비밀번호'
    if ($password -notmatch '^[\x21-\x7E]{8,}$') {
        Write-Host '-> 공백과 한글 없이 8자 이상이어야 합니다. 다시 입력하세요.' -ForegroundColor Yellow
    } elseif ($password -cne (Read-Secret '새 비밀번호 다시 입력')) {
        Write-Host '-> 두 비밀번호가 다릅니다. 다시 입력하세요.' -ForegroundColor Yellow
    } else {
        break
    }
}

$escaped = $password.Replace('\', '\\').Replace("'", "''")
$sql = @"
CREATE DATABASE IF NOT EXISTS appdb CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY '$escaped';
ALTER USER 'appuser'@'localhost' IDENTIFIED BY '$escaped';
GRANT ALL PRIVILEGES ON appdb.* TO 'appuser'@'localhost';
"@

$OutputEncoding = [System.Text.Encoding]::ASCII
Write-Host ''
Write-Host '[2/2] MariaDB 설치 때 정한 root 비밀번호를 입력하세요.'
for ($try = 1; $try -le 3; $try++) {
    $sql | & $client -u root -p
    if ($LASTEXITCODE -eq 0) { break }
    if ($try -eq 3) { throw 'DB 설정에 실패했습니다. 위 오류 내용을 확인하세요.' }
    Write-Host '-> 실패했습니다. root 비밀번호를 다시 입력하세요.' -ForegroundColor Yellow
}
Write-Host 'DB 준비 완료: appdb / appuser' -ForegroundColor Green

$env:DB_PASSWORD = $password
$env:SPRING_PROFILES_ACTIVE = 'dev' # 로그인 구현 전: 개발용 사용자 사용
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17' }
Set-Location (Split-Path $PSScriptRoot -Parent)
& .\mvnw.cmd spring-boot:run
