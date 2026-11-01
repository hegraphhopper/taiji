param(
    [switch]$SkipBuild,
    [switch]$SkipBackend,
    [switch]$SkipFrontend
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$targetDir = Join-Path $root 'target'
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir | Out-Null
}

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

function Get-AlivePid {
    param([string]$PidFile, [string]$Match)
    if (-not (Test-Path $PidFile)) {
        return 0
    }
    $value = (Get-Content -LiteralPath $PidFile -Raw).Trim()
    $parsed = 0
    if (-not [int]::TryParse($value, [ref]$parsed)) {
        return 0
    }
    if (-not (Get-Process -Id $parsed -ErrorAction SilentlyContinue)) {
        return 0
    }
    $commandLine = Get-ProcessCommandLine -ProcessId $parsed
    if ([string]::IsNullOrWhiteSpace($commandLine)) {
        return $parsed
    }
    if ([regex]::IsMatch($commandLine, $Match, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
        return $parsed
    }
    return 0
}

function Wait-Http {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return $true
            }
        } catch {
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

$appPidFile = Join-Path $targetDir 'app.pid'
$vitePidFile = Join-Path $targetDir 'vite.pid'
$backendMatch = [regex]::Escape($root) + '.*fund-platform'
$frontendMatch = 'vite\.js|npm\.cmd.*run dev'

if (-not $SkipBackend) {
    $existing = Get-AlivePid $appPidFile -Match $backendMatch
    if ($existing -gt 0) {
        Write-Host "[backend] already running (pid=$existing)"
        if (-not (Wait-Http -Url 'http://localhost:8080/api/v1/health' -TimeoutSeconds 15)) {
            throw 'Backend process exists but health check failed. Run scripts/stop-demo.ps1 and retry.'
        }
        Write-Host '[backend] health OK'
    } else {
        if (-not $SkipBuild) {
            Write-Host '[backend] building jar...'
            & mvn -q -DskipTests package
            if ($LASTEXITCODE -ne 0) {
                throw 'Maven build failed.'
            }
        }
        $jar = Join-Path $targetDir 'fund-platform-1.0.0.jar'
        if (-not (Test-Path $jar)) {
            throw "Jar not found: $jar. Run without -SkipBuild first."
        }
        $javaArgs = @('-Djavax.net.ssl.trustStoreType=Windows-ROOT', '-jar', $jar,
            '--spring.profiles.active=demo')
        $localConfig = Join-Path $root 'application-local.yml'
        if (Test-Path -LiteralPath $localConfig) {
            $javaArgs += '--spring.config.additional-location=optional:file:./application-local.yml'
            Write-Host '[backend] application-local.yml detected'
        }
        $process = Start-Process -FilePath 'java' `
            -ArgumentList $javaArgs `
            -WorkingDirectory $root `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $targetDir 'app.out.log') `
            -RedirectStandardError (Join-Path $targetDir 'app.err.log') `
            -PassThru
        $process.Id | Set-Content -LiteralPath $appPidFile
        Write-Host "[backend] started (pid=$($process.Id))"
        if (-not (Wait-Http -Url 'http://localhost:8080/api/v1/health' -TimeoutSeconds 90)) {
            throw 'Backend health check failed. See target/app.out.log.'
        }
        Write-Host '[backend] health OK'
    }
}

if (-not $SkipFrontend) {
    $existing = Get-AlivePid $vitePidFile -Match $frontendMatch
    if ($existing -gt 0) {
        Write-Host "[frontend] already running (pid=$existing)"
        if (-not (Wait-Http -Url 'http://127.0.0.1:5173/' -TimeoutSeconds 15)) {
            throw 'Frontend process exists but page is not reachable. Run scripts/stop-demo.ps1 and retry.'
        }
        Write-Host '[frontend] ready'
    } else {
        $frontendDir = Join-Path $root 'frontend'
        if (-not (Test-Path $frontendDir)) {
            throw 'frontend directory not found.'
        }
        if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
            Write-Host '[frontend] installing dependencies...'
            Push-Location $frontendDir
            try {
                & npm.cmd install --no-audit --no-fund
                if ($LASTEXITCODE -ne 0) {
                    throw 'npm install failed.'
                }
            } finally {
                Pop-Location
            }
        }
        $process = Start-Process -FilePath 'npm.cmd' `
            -ArgumentList 'run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173' `
            -WorkingDirectory $frontendDir `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $targetDir 'vite.out.log') `
            -RedirectStandardError (Join-Path $targetDir 'vite.err.log') `
            -PassThru
        $process.Id | Set-Content -LiteralPath $vitePidFile
        Write-Host "[frontend] started (pid=$($process.Id))"
        if (-not (Wait-Http -Url 'http://127.0.0.1:5173/' -TimeoutSeconds 60)) {
            throw 'Frontend dev server did not respond. See target/vite.out.log.'
        }
        Write-Host '[frontend] ready'
    }
}

Write-Host ''
Write-Host 'Demo is ready.'
if (-not $SkipBackend) {
    Write-Host '  API:      http://localhost:8080/api/v1/health'
    Write-Host '  Swagger:  http://localhost:8080/swagger-ui.html'
}
if (-not $SkipFrontend) {
    Write-Host '  UI:       http://127.0.0.1:5173'
}
Write-Host '  Accounts: admin / Admin@123, pmo01 / Pmo@12345, user01 / User@12345'
Write-Host '  Stop:     powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-demo.ps1'
