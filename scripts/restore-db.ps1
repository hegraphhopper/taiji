param(
    [Parameter(Mandatory = $true)][string]$Archive,
    [string]$TargetDir = 'data',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path -LiteralPath $Archive)) {
    throw "Archive not found: $Archive"
}
$running = Get-Process java -ErrorAction SilentlyContinue
if ($running) {
    throw 'Java process detected. Run scripts/stop-demo.ps1 first.'
}
if (Test-Path -LiteralPath $TargetDir) {
    if (-not $Force) {
        throw "Target exists: $TargetDir. Use -Force to overwrite."
    }
    Remove-Item -LiteralPath $TargetDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
Expand-Archive -LiteralPath $Archive -DestinationPath $TargetDir -Force
Write-Host "[restore] $Archive -> $TargetDir"
Get-ChildItem -LiteralPath $TargetDir | Select-Object Name, Length | Format-Table -AutoSize
