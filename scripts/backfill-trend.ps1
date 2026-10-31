param(
    [int]$Months = 8,
    [string]$BaseUrl = 'http://localhost:8080/api/v1',
    [string]$Username = 'admin',
    [string]$Password = 'Admin@123'
)

$ErrorActionPreference = 'Stop'

function PostJson($url, $token, $obj) {
    $json = $obj | ConvertTo-Json -Depth 6 -Compress
    return Invoke-RestMethod -Method Post -Uri $url `
        -Headers @{ Authorization = "Bearer $token" } `
        -ContentType 'application/json; charset=utf-8' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($json))
}

$loginJson = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($loginJson))
if ($login.code -ne 0) {
    throw "login failed: $($login.message)"
}
$token = $login.data.token
Write-Host "[backfill] login ok: $Username"

$now = Get-Date
$startMonth = $now.AddMonths(-$Months).ToString('yyyy-MM')
$endMonth = $now.ToString('yyyy-MM')

for ($offset = $Months; $offset -ge 1; $offset--) {
    $month = $now.AddMonths(-$offset)
    $ym = $month.ToString('yyyy-MM')
    $lastDay = [DateTime]::DaysInMonth($month.Year, $month.Month)
    $analysisDate = ('{0}-{1:00}' -f $ym, $lastDay)
    $run = PostJson "$BaseUrl/analysis/runs" $token @{ triggerType = 'FULL'; analysisDate = $analysisDate }
    if ($run.code -ne 0) {
        Write-Host "[backfill] $ym FAILED: $($run.message)"
        continue
    }
    Write-Host ("[backfill] {0} status={1} projects={2} pairs={3} llm={4}" -f `
        $ym, $run.data.status, $run.data.projectCount, $run.data.pairCount, $run.data.llmUsed)
}

$trend = Invoke-RestMethod -Uri "$BaseUrl/insight/trend?startMonth=$startMonth&endMonth=$endMonth" `
    -Headers @{ Authorization = "Bearer $token" }
Write-Host ("[backfill] trend points={0} hasData={1}" -f $trend.data.points.Count, $trend.data.hasData)
if ($trend.data.summary) {
    $trend.data.summary | ForEach-Object { Write-Host "  - $_" }
}
