param([string]$Folder = 'docs\review')
$ErrorActionPreference = 'Stop'
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    Get-ChildItem -LiteralPath (Join-Path (Get-Location) $Folder) -Filter '*.docx' | Sort-Object Length -Descending | ForEach-Object {
        $doc = $word.Documents.Open($_.FullName, $false, $true, $false)
        $pages = $doc.ComputeStatistics(2)
        $chars = $doc.ComputeStatistics(3)
        $paragraphs = $doc.Paragraphs.Count
        $tables = $doc.Tables.Count
        $toc = $doc.TablesOfContents.Count
        $tocEntries = 0
        if ($toc -gt 0) { $tocEntries = $doc.TablesOfContents.Item(1).Range.Paragraphs.Count }
        $first = ''
        $last = ''
        foreach ($p in $doc.Paragraphs) {
            $t = $p.Range.Text.Trim()
            if ($t.Length -gt 0) { $first = $t; break }
        }
        $all = $doc.Paragraphs
        for ($i = $all.Count; $i -ge 1; $i--) {
            $t = $all.Item($i).Range.Text.Trim()
            if ($t.Length -gt 0) { $last = $t; break }
        }
        Write-Output ("FILE=" + $_.Name + " PAGES=" + $pages + " CHARS=" + $chars + " PARA=" + $paragraphs + " TABLES=" + $tables + " TOC=" + $toc + " TOC_ENTRIES=" + $tocEntries)
        Write-Output ("FIRST=" + $first)
        Write-Output ("LAST=" + $last)
        $doc.Close($false)
    }
}
finally {
    $word.Quit()
}
