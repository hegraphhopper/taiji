param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$targetDir = Join-Path $root 'target'
$backendMatch = [regex]::Escape($root) + '.*fund-platform'
$frontendMatch = 'vite\.js|npm\.cmd.*run dev'
$pidFiles = @(
    @{ File = (Join-Path $targetDir 'app.pid'); Match = $backendMatch },
    @{ File = (Join-Path $targetDir 'vite.pid'); Match = $frontendMatch }
)

function Get-ProcessCommandLine {
    param([int]$ProcessId)
    try {
        $info = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction Stop
        if ($info -and $info.CommandLine) {
            return $info.CommandLine
        }
    } catch {
    }
    return ''
}

function Test-ProjectProcess {
    param([int]$ProcessId, [string]$Match)
    if ($ProcessId -le 0) {
        return $false
    }
    if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        return $false
    }
    $commandLine = Get-ProcessCommandLine -ProcessId $ProcessId
    if ([string]::IsNullOrWhiteSpace($commandLine)) {
        return $false
    }
    return [regex]::IsMatch($commandLine, $Match, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
}

foreach ($entry in $pidFiles) {
    $pidFile = $entry.File
    if (-not (Test-Path $pidFile)) {
        continue
    }
    $value = (Get-Content -LiteralPath $pidFile -Raw).Trim()
    $parsed = 0
    if ([int]::TryParse($value, [ref]$parsed) -and $parsed -gt 0) {
        if (Test-ProjectProcess -ProcessId $parsed -Match $entry.Match) {
            & taskkill.exe /PID $parsed /T /F | Out-Null
            Write-Host "[stop] pid=$parsed"
        } elseif (Get-Process -Id $parsed -ErrorAction SilentlyContinue) {
            Write-Host "[stop] pid=$parsed does not belong to this project, skipped"
        } else {
            Write-Host "[stop] pid=$parsed already exited"
        }
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

$ports = @(
    @{ Port = 8080; Match = $backendMatch },
    @{ Port = 5173; Match = 'vite\.js' }
)
foreach ($item in $ports) {
    $port = $item.Port
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $owner = $connection.OwningProcess
        if (Test-ProjectProcess -ProcessId $owner -Match $item.Match) {
            & taskkill.exe /PID $owner /T /F | Out-Null
            Write-Host "[stop] released port $port (pid=$owner)"
        } else {
            Write-Host "[stop] port $port is held by pid=$owner (not this project), skipped"
        }
    }
}

Write-Host '[stop] data/ directory is preserved.'
Write-Host '[stop] done'
