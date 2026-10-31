$ErrorActionPreference = 'SilentlyContinue'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Show-Service {
    param(
        [string]$Name,
        [int]$Port,
        [string]$Url
    )
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $connection) {
        Write-Host ("{0}: DOWN (port {1})" -f $Name, $Port)
        return
    }
    $pidValue = $connection.OwningProcess
    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    $status = 'UNKNOWN'
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        $status = "HTTP $($response.StatusCode)"
    } catch {
        $status = 'NO RESPONSE'
    }
    $processName = if ($process) { $process.ProcessName } else { 'unknown' }
    Write-Host ("{0}: UP (port {1}, pid {2}, {3}, {4})" -f $Name, $Port, $pidValue, $processName, $status)
}

Show-Service -Name 'backend' -Port 8080 -Url 'http://localhost:8080/api/v1/health'
Show-Service -Name 'frontend' -Port 5173 -Url 'http://127.0.0.1:5173/'

if (Test-Path 'target\app.pid') {
    Write-Host ("backend pid file: {0}" -f (Get-Content 'target\app.pid' -Raw).Trim())
}
if (Test-Path 'target\vite.pid') {
    Write-Host ("frontend pid file: {0}" -f (Get-Content 'target\vite.pid' -Raw).Trim())
}
