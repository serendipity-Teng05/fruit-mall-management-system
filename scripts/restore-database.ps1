param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [string]$DatabaseHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = $env:DB_PASSWORD,
    [switch]$ConfirmRestore
)

$ErrorActionPreference = "Stop"
if (-not $ConfirmRestore) { throw "Restore is destructive. Re-run with -ConfirmRestore after checking the target host." }
if (-not (Test-Path -LiteralPath $BackupFile)) { throw "Backup file not found: $BackupFile" }
if (-not (Get-Command mysql.exe -ErrorAction SilentlyContinue)) { throw "mysql.exe was not found." }
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
    Get-Content -Raw -Encoding UTF8 -LiteralPath $BackupFile |
        & mysql.exe --host=$DatabaseHost --port=$Port --user=$User --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw "Database restore failed. mysql.exe exit code: $LASTEXITCODE" }
    Write-Host "Database restore completed. Run .\scripts\upgrade-database.ps1 and verify-database.ps1 next." -ForegroundColor Green
} finally {
    $OutputEncoding = $oldEncoding
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

