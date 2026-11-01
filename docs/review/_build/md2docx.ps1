param(
    [Parameter(Mandatory = $true)][string]$MarkdownPath,
    [Parameter(Mandatory = $true)][string]$MetaPath
)

$ErrorActionPreference = 'Stop'

function Escape-Html([string]$text) {
    if ($null -eq $text) { return '' }
    $value = $text -replace '&', '&amp;'
    $value = $value -replace '<', '&lt;'
    $value = $value -replace '>', '&gt;'
    return $value
}

function Format-Inline([string]$text) {
    $value = Escape-Html $text
    $value = [regex]::Replace($value, '\*\*(.+?)\*\*', '<strong>$1</strong>')
    $value = [regex]::Replace($value, '`([^`]+)`', '<code>$1</code>')
    return $value
}

function Convert-Markdown([string[]]$lines) {
    $html = New-Object System.Collections.Generic.List[string]
    $index = 0
    $total = $lines.Count
    $listMode = ''
    while ($index -lt $total) {
        $line = $lines[$index]
        if ($line -match '^\s*```') {
            if ($listMode -ne '') { $html.Add("</$listMode>"); $listMode = '' }
            $code = New-Object System.Collections.Generic.List[string]
            $index++
            while ($index -lt $total -and $lines[$index] -notmatch '^\s*```') {
                $code.Add((Escape-Html $lines[$index]))
                $index++
            }
            $index++
            $html.Add('<pre><code>' + ($code -join "`n") + '</code></pre>')
            continue
        }
        if ($line -match '^\s*$') {
            if ($listMode -ne '') { $html.Add("</$listMode>"); $listMode = '' }
            $index++
            continue
        }
        if ($line -match '^(#{1,6})\s+(.*)$') {
            if ($listMode -ne '') { $html.Add("</$listMode>"); $listMode = '' }
            $level = $Matches[1].Length
            $text = Format-Inline $Matches[2]
            $html.Add("<h$level>$text</h$level>")
            $index++
            continue
        }
        if ($line -match '^\s*(-{3,}|\*{3,})\s*$') {
            if ($listMode -ne '') { $html.Add("</$listMode>"); $listMode = '' }
            $html.Add('<hr/>')
            $index++
            continue
        }
        if ($line -match '^\s*\|' -and $index + 1 -lt $total -and $lines[$index + 1] -match '^\s*\|?\s*:?-{3,}') {
            if ($listMode -ne '') { $html.Add("</$listMode>"); $listMode = '' }
            $headerCells = Split-TableRow $line
            $html.Add('<table><thead><tr>')
            foreach ($cell in $headerCells) {
                $html.Add('<th>' + (Format-Inline $cell) + '</th>')
            }
            $html.Add('</tr></thead><tbody>')
            $index += 2
            while ($index -lt $total -and $lines[$index] -match '^\s*\|') {
                $cells = Split-TableRow $lines[$index]
                $html.Add('<tr>')
                foreach ($cell in $cells) {
                    $html.Add('<td>' + (Format-Inline $cell) + '</td>')
                }
                $html.Add('</tr>')
                $index++
            }
            $html.Add('</tbody></table>')
            continue
        }
        if ($line -match '^\s*[-*]\s+(.*)$') {
            if ($listMode -ne 'ul') {
                if ($listMode -ne '') { $html.Add("</$listMode>") }
                $html.Add('<ul>')
                $listMode = 'ul'
            }
            $html.Add('<li>' + (Format-Inline $Matches[1]) + '</li>')
            $index++
            continue
        }
        if ($line -match '^\s*\d+[.)]\s+(.*)$') {
            if ($listMode -ne 'ol') {
                if ($listMode -ne '') { $html.Add("</$listMode>") }
                $html.Add('<ol>')
                $listMode = 'ol'
            }
            $html.Add('<li>' + (Format-Inline $Matches[1]) + '</li>')
            $index++
            continue
        }
        if ($listMode -ne '') { $html.Add("</$listMode>"); $listMode = '' }
        $paragraph = New-Object System.Collections.Generic.List[string]
        while ($index -lt $total) {
            $current = $lines[$index]
            if ($current -match '^\s*$' -or $current -match '^\s*```' -or $current -match '^#{1,6}\s' -or $current -match '^\s*\|' -or $current -match '^\s*[-*]\s+' -or $current -match '^\s*\d+[.)]\s+') { break }
            $paragraph.Add((Format-Inline $current))
            $index++
        }
        $html.Add('<p>' + ($paragraph -join ' ') + '</p>')
    }
    if ($listMode -ne '') { $html.Add("</$listMode>") }
    return ($html -join "`n")
}

function Split-TableRow([string]$line) {
    $trimmed = $line.Trim()
    if ($trimmed.StartsWith('|')) { $trimmed = $trimmed.Substring(1) }
    if ($trimmed.EndsWith('|')) { $trimmed = $trimmed.Substring(0, $trimmed.Length - 1) }
    return ($trimmed -split '\|')
}

$meta = Get-Content -LiteralPath $MetaPath -Raw -Encoding UTF8 | ConvertFrom-Json
$markdown = Get-Content -LiteralPath $MarkdownPath -Raw -Encoding UTF8
$lines = $markdown -split "`r?`n"

$title = $meta.title
$bodyStart = 0
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '^#\s+(.+)$') {
        $title = $Matches[1].Trim()
        $bodyStart = $i + 1
        break
    }
}

$body = Convert-Markdown ($lines[$bodyStart..($lines.Count - 1)])

$css = @'
@page { size: A4; margin: 2.4cm 2.2cm 2.2cm 2.4cm; }
body { font-family: SimSun, "Times New Roman", serif; font-size: 10.5pt; line-height: 1.55; color: #000000; }
p { margin: 0 0 6pt 0; text-align: justify; }
h1 { font-family: SimHei, sans-serif; font-size: 22pt; text-align: center; margin: 0 0 18pt 0; }
h2 { font-family: SimHei, sans-serif; font-size: 16pt; margin: 16pt 0 8pt 0; page-break-after: avoid; }
h3 { font-family: SimHei, sans-serif; font-size: 13.5pt; margin: 12pt 0 6pt 0; page-break-after: avoid; }
h4 { font-family: SimHei, sans-serif; font-size: 12pt; margin: 10pt 0 4pt 0; page-break-after: avoid; }
h5, h6 { font-family: SimHei, sans-serif; font-size: 11pt; margin: 8pt 0 4pt 0; }
ul, ol { margin: 0 0 6pt 0; padding-left: 22pt; }
li { margin: 0 0 3pt 0; }
table { width: 100%; border-collapse: collapse; margin: 6pt 0 10pt 0; font-size: 9pt; }
th { border: 0.75pt solid #666666; background: #EFEFEF; padding: 3pt 4pt; text-align: left; font-family: SimHei, sans-serif; }
td { border: 0.75pt solid #666666; padding: 3pt 4pt; vertical-align: top; }
pre { font-family: Consolas, "Courier New", monospace; font-size: 8.5pt; line-height: 1.35; background: #F5F5F5; border: 0.75pt solid #CCCCCC; padding: 6pt; white-space: pre-wrap; margin: 6pt 0 10pt 0; }
code { font-family: Consolas, "Courier New", monospace; font-size: 9pt; }
hr { border: none; border-top: 0.75pt solid #999999; margin: 10pt 0; }
.cover { text-align: center; }
.cover .org { font-family: SimHei, sans-serif; font-size: 18pt; margin-top: 90pt; letter-spacing: 2pt; }
.cover .doc-title { font-family: SimHei, sans-serif; font-size: 30pt; margin-top: 60pt; line-height: 1.4; }
.cover .doc-sub { font-family: SimHei, sans-serif; font-size: 18pt; margin-top: 16pt; }
.cover .meta { margin-top: 120pt; font-size: 12pt; line-height: 2.0; }
.cover .meta table { width: 60%; margin: 0 auto; font-size: 12pt; }
.cover .meta td { border: none; padding: 2pt 8pt; }
.cover .date { margin-top: 60pt; font-size: 12pt; }
.toc-title { font-family: SimHei, sans-serif; font-size: 18pt; text-align: center; margin: 0 0 12pt 0; }
'@

$metaRows = @(
    @('文档编号', $meta.docNo),
    @('文档版本', $meta.version),
    @('密级', $meta.classification),
    @('编制单位', $meta.org)
)
$metaHtml = ''
foreach ($row in $metaRows) {
    $metaHtml += '<tr><td style="text-align:right;width:35%">' + (Escape-Html $row[0]) + '：</td><td style="text-align:left">' + (Escape-Html $row[1]) + '</td></tr>'
}

$cover = @"
<div class="cover">
  <div class="org">$(Escape-Html $meta.org)</div>
  <div class="doc-title">$(Escape-Html $title)</div>
  <div class="doc-sub">$(Escape-Html $meta.subtitle)</div>
  <div class="meta"><table>$metaHtml</table></div>
  <div class="date">$(Escape-Html $meta.date)</div>
</div>
<div style="page-break-before:always"></div>
<div class="toc-title">$(Escape-Html $meta.tocLabel)</div>
<p>[[TOC]]</p>
<div style="page-break-before:always"></div>
"@

$html = "<!DOCTYPE html><html><head><meta charset=`"utf-8`"><style>$css</style></head><body>$cover$body</body></html>"

$tempHtml = Join-Path $env:TEMP ('md2docx-' + [guid]::NewGuid().ToString('N') + '.html')
[System.IO.File]::WriteAllText($tempHtml, $html, (New-Object System.Text.UTF8Encoding($true)))

if ($env:MD2DOCX_KEEP_HTML -eq '1') {
    $debugDir = Join-Path (Get-Location) 'docs\build'
    $debugName = [System.IO.Path]::GetFileNameWithoutExtension($meta.out) + '.debug.html'
    Copy-Item -LiteralPath $tempHtml -Destination (Join-Path $debugDir $debugName) -Force
}

$outPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $meta.out))
$outDir = [System.IO.Path]::GetDirectoryName($outPath)
if (-not (Test-Path -LiteralPath $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

$word = $null
$doc = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    try { $word.Options.ConfirmConversions = $false } catch { }

    $doc = $word.Documents.Open($tempHtml, $false, $false, $true)

    foreach ($section in $doc.Sections) {
        $section.PageSetup.PaperSize = 7
        $section.PageSetup.TopMargin = 68
        $section.PageSetup.BottomMargin = 62
        $section.PageSetup.LeftMargin = 68
        $section.PageSetup.RightMargin = 62
        $header = $section.Headers.Item(1).Range
        $header.Text = $meta.header
        $header.Font.Name = 'SimSun'
        $header.Font.Size = 9
        $header.ParagraphFormat.Alignment = 1
        $footer = $section.Footers.Item(1).Range
        $footer.Text = ''
        $footer.Fields.Add($footer, 33) | Out-Null
        $footer.Font.Name = 'SimSun'
        $footer.Font.Size = 9
        $footer.ParagraphFormat.Alignment = 1
    }

    $find = $doc.Content.Find
    $find.ClearFormatting()
    $find.Text = '[[TOC]]'
    $find.Forward = $true
    $find.Wrap = 0
    if ($find.Execute()) {
        $range = $find.Parent
        $doc.TablesOfContents.Add($range, $true, 2, 3) | Out-Null
    }

    if ($doc.TablesOfContents.Count -gt 0) {
        $doc.TablesOfContents.Item(1).Update()
    }
    $doc.Fields.Update() | Out-Null

    $doc.SaveAs2($outPath, 16)
    $pages = $doc.ComputeStatistics(2)
    $doc.Close($false)
    $doc = $null
    Write-Output ("OK " + $outPath + " pages=" + $pages)
}
finally {
    if ($doc -ne $null) { $doc.Close($false) }
    if ($word -ne $null) { $word.Quit() }
    if (Test-Path -LiteralPath $tempHtml) { Remove-Item -LiteralPath $tempHtml -Force }
}
