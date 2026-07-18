param(
    [string]$DatabaseHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = $env:DB_PASSWORD
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sqlFile = Join-Path $root "database\fruit_mall.sql"

if (-not (Get-Command mysql.exe -ErrorAction SilentlyContinue)) {
    throw "mysql.exe was not found. Install MySQL 8 and add its bin directory to PATH."
}
if (-not (Test-Path -LiteralPath $sqlFile)) {
    throw "Database script not found: $sqlFile"
}
if (-not $Password) {
    $securePassword = Read-Host "Enter the MySQL password for user $User" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$env:MYSQL_PWD = $Password
$oldEncoding = $OutputEncoding
$OutputEncoding = [Text.UTF8Encoding]::new($false)
try {
    Get-Content -Raw -Encoding UTF8 -LiteralPath $sqlFile |
        & mysql.exe --host=$DatabaseHost --port=$Port --user=$User --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) {
        throw "Database initialization failed. mysql.exe exit code: $LASTEXITCODE"
    }
    Write-Host "Database fruit_mall initialized successfully." -ForegroundColor Green
    & (Join-Path $PSScriptRoot "upgrade-database.ps1") -DatabaseHost $DatabaseHost -Port $Port -User $User -Password $Password
} finally {
    $OutputEncoding = $oldEncoding
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
