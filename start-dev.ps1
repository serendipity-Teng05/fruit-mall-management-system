param(
    [string]$DatabasePassword = $env:DB_PASSWORD,
    [string]$DatabaseHost = "127.0.0.1",
    [int]$DatabasePort = 3306,
    [string]$DatabaseUser = "root",
    [switch]$NoBrowser,
    [switch]$SkipDatabaseUpgrade,
    [switch]$PreflightOnly,
    [int]$StartupTimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$frontend = Join-Path $root "frontend"
$logs = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logs | Out-Null

if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
    throw "Java was not found. Install JDK 17 and add it to PATH."
}
if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
    throw "npm was not found. Install Node.js 18 or newer."
}
if (-not (Get-Command mysql.exe -ErrorAction SilentlyContinue)) {
    throw "mysql.exe was not found. Install MySQL 8 and add its bin directory to PATH."
}

$servicePorts = @(8080, 5173)
$occupiedPorts = @($servicePorts | Where-Object {
    Get-NetTCPConnection -State Listen -LocalPort $_ -ErrorAction SilentlyContinue
})

if ($occupiedPorts.Count -gt 0) {
    Write-Host "Checking existing Fruit Mall processes..." -ForegroundColor Yellow
    & (Join-Path $root "stop-dev.ps1") -Ports $occupiedPorts

    $deadline = (Get-Date).AddSeconds(8)
    do {
        $remaining = @($occupiedPorts | Where-Object {
            Get-NetTCPConnection -State Listen -LocalPort $_ -ErrorAction SilentlyContinue
        })
        if ($remaining.Count -eq 0) { break }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)

    foreach ($port in $remaining) {
        $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
            Select-Object -First 1
        $ownerPid = if ($listener) { [int]$listener.OwningProcess } else { 0 }
        $owner = if ($ownerPid -gt 0) {
            Get-CimInstance Win32_Process -Filter "ProcessId = $ownerPid" -ErrorAction SilentlyContinue
        } else { $null }
        $ownerName = if ($owner) { $owner.Name } else { "unknown" }
        throw "Port $port is occupied by another program (PID $ownerPid, $ownerName). Close that program or change its port, then run .\start-dev.ps1 again."
    }
}

if ($PreflightOnly) {
    Write-Host "Preflight passed: ports 8080 and 5173 are ready for Fruit Mall." -ForegroundColor Green
    return
}

if (-not $DatabasePassword) {
    $securePassword = Read-Host "Enter the local MySQL root password" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $DatabasePassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$env:DB_PASSWORD = $DatabasePassword
$env:DB_USERNAME = $DatabaseUser
if (-not $env:DB_URL) {
    $env:DB_URL = "jdbc:mysql://${DatabaseHost}:$DatabasePort/fruit_mall?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false"
}
if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = "FruitMall-Local-Development-Secret-Change-Before-Production-2026"
}

if (-not $SkipDatabaseUpgrade) {
    $env:MYSQL_PWD = $DatabasePassword
    try {
        $databaseExists = & mysql.exe --host=$DatabaseHost --port=$DatabasePort --user=$DatabaseUser `
            --batch --skip-column-names `
            --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'fruit_mall';"
        if ($LASTEXITCODE -ne 0) {
            throw "Cannot connect to MySQL. Check the host, port, user and password."
        }
    } finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }

    if (($databaseExists | Select-Object -First 1).Trim() -eq "1") {
        Write-Host "Applying safe database upgrades..." -ForegroundColor Cyan
        & (Join-Path $root "scripts\upgrade-database.ps1") `
            -DatabaseHost $DatabaseHost -Port $DatabasePort -User $DatabaseUser -Password $DatabasePassword
    } else {
        Write-Host "fruit_mall does not exist; initializing it now..." -ForegroundColor Cyan
        & (Join-Path $root "scripts\init-database.ps1") `
            -DatabaseHost $DatabaseHost -Port $DatabasePort -User $DatabaseUser -Password $DatabasePassword
    }
}

function Wait-ForService {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    Write-Host "Waiting for $Name to become ready..." -ForegroundColor Yellow

    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) {
            throw "$Name exited before it became ready. Check the log files under $logs."
        }

        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                Write-Host "$Name is ready: $Url" -ForegroundColor Green
                return
            }
        } catch {
            Start-Sleep -Seconds 1
        }
    }

    throw "$Name was not ready within $TimeoutSeconds seconds. Check the log files under $logs and the database configuration."
}

Write-Host "Starting backend: http://127.0.0.1:8080" -ForegroundColor Cyan
$backendProcess = Start-Process -FilePath (Join-Path $root "mvnw.cmd") -ArgumentList "spring-boot:run" -WorkingDirectory $root -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logs "backend.log") -RedirectStandardError (Join-Path $logs "backend-error.log")
Wait-ForService -Name "Backend" -Url "http://127.0.0.1:8080/actuator/health" -Process $backendProcess -TimeoutSeconds $StartupTimeoutSeconds

Write-Host "Starting frontend: http://127.0.0.1:5173" -ForegroundColor Cyan
$frontendProcess = Start-Process -FilePath (Get-Command npm.cmd).Source -ArgumentList "run", "dev" -WorkingDirectory $frontend -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logs "frontend.log") -RedirectStandardError (Join-Path $logs "frontend-error.log")
Wait-ForService -Name "Frontend" -Url "http://127.0.0.1:5173/login" -Process $frontendProcess -TimeoutSeconds 60

@{
    backendLauncherPid = $backendProcess.Id
    frontendLauncherPid = $frontendProcess.Id
    startedAt = (Get-Date).ToString("o")
} | ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $logs "processes.json")

if (-not $NoBrowser) {
    Start-Process "http://127.0.0.1:5173"
}

Write-Host "Fruit Mall is ready. Logs are available in $logs. Versioned database upgrades through v7 are handled automatically." -ForegroundColor Green
