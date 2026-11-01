param(
    [string]$DataDir = 'data',
    [string]$OutputDir = 'db/backup'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path -LiteralPath $DataDir)) {
    throw "Data directory not found: $DataDir"
}
$running = Get-Process java -ErrorAction SilentlyContinue
if ($running) {
    throw 'Java process detected. Run scripts/stop-demo.ps1 first.'
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$archive = Join-Path $OutputDir "fund-platform-$stamp.zip"
$source = Join-Path $DataDir '*'
Compress-Archive -Path $source -DestinationPath $archive -CompressionLevel Optimal
$size = (Get-Item -LiteralPath $archive).Length
Write-Host "[backup] $archive ($([math]::Round($size / 1MB, 2)) MB)"
