param(
    [int[]]$Ports = @(8080, 5173)
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$stopped = 0

foreach ($port in $Ports) {
    $connections = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
    $processIds = @($connections | Select-Object -ExpandProperty OwningProcess -Unique)
    foreach ($ownerPid in $processIds) {
        $processId = [int]$ownerPid
        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
        if (-not $processInfo) {
            continue
        }

        $commandLine = [string]$processInfo.CommandLine
        if ($commandLine.IndexOf($root, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
            Write-Warning "Port $port belongs to PID $processId, but it was not started from this workspace. It was left running."
            continue
        }

        Stop-Process -Id $processId -Force
        Write-Host "Stopped Fruit Mall process PID $processId on port $port." -ForegroundColor Green
        $stopped++
    }
}

$processFile = Join-Path $root "logs\processes.json"
if (Test-Path -LiteralPath $processFile) {
    Remove-Item -LiteralPath $processFile -Force
}

if ($stopped -eq 0) {
    Write-Host "No running Fruit Mall service was found on ports 8080 or 5173." -ForegroundColor Yellow
}
