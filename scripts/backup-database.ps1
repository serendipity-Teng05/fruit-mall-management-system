param(
    [string]$DatabaseHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = $env:DB_PASSWORD,
    [string]$OutputDirectory
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root "database\backups" }
if (-not (Get-Command mysqldump.exe -ErrorAction SilentlyContinue)) {
    throw "mysqldump.exe was not found. Add the MySQL bin directory to PATH."
}
if (-not $Password) {
    $securePassword = Read-Host "Enter the MySQL password for user $User" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

$resolvedOutput = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null
$backupFile = Join-Path $resolvedOutput ("fruit_mall_{0}.sql" -f (Get-Date -Format "yyyyMMdd_HHmmss"))
$env:MYSQL_PWD = $Password
try {
    & mysqldump.exe --host=$DatabaseHost --port=$Port --user=$User `
        --single-transaction --routines --triggers --events --hex-blob `
        --default-character-set=utf8mb4 --set-gtid-purged=OFF `
        --result-file=$backupFile fruit_mall
    if ($LASTEXITCODE -ne 0) { throw "Database backup failed. mysqldump.exe exit code: $LASTEXITCODE" }
    if (-not (Test-Path -LiteralPath $backupFile) -or (Get-Item $backupFile).Length -lt 100) {
        throw "Database backup did not produce a valid output file."
    }
    Write-Host "Database backup created: $backupFile" -ForegroundColor Green
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

