param(
    [string]$DatabaseHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = $env:DB_PASSWORD
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$migrations = @(
    @{ File = "upgrade_v2_payment.sql"; Version = "20260712_v2_payment" },
    @{ File = "upgrade_v3_user_role_normalization.sql"; Version = "20260713_v3_user_role_normalization" },
    @{ File = "upgrade_v4_mall_integration.sql"; Version = "20260713_v4_mall_integration" },
    @{ File = "upgrade_v5_hardening.sql"; Version = "20260715_v5_hardening" },
    @{ File = "upgrade_v6_production_readiness.sql"; Version = "20260716_v6_production_readiness" },
    @{ File = "upgrade_v7_audit_reliability.sql"; Version = "20260716_v7_audit_reliability" }
)

if (-not (Get-Command mysql.exe -ErrorAction SilentlyContinue)) {
    throw "mysql.exe was not found. Install MySQL 8 and add its bin directory to PATH."
}
if (-not $Password) {
    $securePassword = Read-Host "Enter the MySQL password for user $User" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

$env:MYSQL_PWD = $Password
$oldEncoding = $OutputEncoding
$OutputEncoding = [Text.UTF8Encoding]::new($false)
try {
    & mysql.exe --host=$DatabaseHost --port=$Port --user=$User --database=fruit_mall `
        --default-character-set=utf8mb4 `
        --execute="CREATE TABLE IF NOT EXISTS sys_schema_version (version varchar(100) NOT NULL, description varchar(255) NOT NULL, installed_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;"
    if ($LASTEXITCODE -ne 0) { throw "Unable to initialize the database version table." }

    foreach ($migration in $migrations) {
        $sqlFile = Join-Path $root ("database\" + $migration.File)
        if (-not (Test-Path -LiteralPath $sqlFile)) { throw "Database upgrade script not found: $sqlFile" }

        $installed = & mysql.exe --host=$DatabaseHost --port=$Port --user=$User --database=fruit_mall `
            --batch --skip-column-names --default-character-set=utf8mb4 `
            --execute="SELECT COUNT(*) FROM sys_schema_version WHERE version = '$($migration.Version)';"
        if ($LASTEXITCODE -ne 0) { throw "Unable to read database version $($migration.Version)." }
        if (($installed | Select-Object -First 1).Trim() -eq "1") {
            Write-Host "Skipping $($migration.File) (already applied)." -ForegroundColor DarkGray
            continue
        }

        Write-Host "Applying $($migration.File)..." -ForegroundColor Cyan
        Get-Content -Raw -Encoding UTF8 -LiteralPath $sqlFile |
            & mysql.exe --host=$DatabaseHost --port=$Port --user=$User --database=fruit_mall --default-character-set=utf8mb4
        if ($LASTEXITCODE -ne 0) {
            throw "Database upgrade failed at $sqlFile. mysql.exe exit code: $LASTEXITCODE"
        }

        $verified = & mysql.exe --host=$DatabaseHost --port=$Port --user=$User --database=fruit_mall `
            --batch --skip-column-names --default-character-set=utf8mb4 `
            --execute="SELECT COUNT(*) FROM sys_schema_version WHERE version = '$($migration.Version)';"
        if ($LASTEXITCODE -ne 0 -or (($verified | Select-Object -First 1).Trim() -ne "1")) {
            throw "Migration $($migration.File) finished without recording version $($migration.Version)."
        }
    }
    Write-Host "Database fruit_mall is up to date through v7 audit reliability." -ForegroundColor Green
} finally {
    $OutputEncoding = $oldEncoding
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
