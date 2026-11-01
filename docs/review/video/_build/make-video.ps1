$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech

$root = (Get-Location).Path
$videoDir = Join-Path $root 'docs\review\video'
$slideDir = Join-Path $videoDir 'slides'
$buildDir = Join-Path $videoDir '_build'
$ttsDir = Join-Path $buildDir 'tts'
$clipDir = Join-Path $buildDir 'clips'
$seqDir = Join-Path $buildDir 'seq'
foreach ($d in @($ttsDir, $clipDir, $seqDir)) { if (-not (Test-Path -LiteralPath $d)) { New-Item -ItemType Directory -Path $d | Out-Null } }

$ff = 'D:\SoftWare\tools\tts-vue\tts-vue-main\tts-vue-main\node_modules\@ffmpeg-installer\win32-x64\ffmpeg.exe'
if (-not (Test-Path -LiteralPath $ff)) { throw 'ffmpeg not found' }

$cfg = Get-Content -LiteralPath (Join-Path $buildDir 'timeline.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$slides = $cfg.slides
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Fmt([double]$v) { return [string]::Format([Globalization.CultureInfo]::InvariantCulture, '{0:0.###}', $v) }
function AssTime([double]$s) { $t = [TimeSpan]::FromSeconds($s); return ('{0}:{1:00}:{2:00}.{3:00}' -f [int][Math]::Floor($t.TotalHours), $t.Minutes, $t.Seconds, [int][Math]::Floor($t.Milliseconds / 10)) }
function SrtTime([double]$s) { $t = [TimeSpan]::FromSeconds($s); return ('{0:00}:{1:00}:{2:00},{3:000}' -f [int][Math]::Floor($t.TotalHours), $t.Minutes, $t.Seconds, $t.Milliseconds) }

function WavInfo([string]$path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $pos = 12
    $rate = 0; $ch = 0; $bits = 0; $dataSize = 0
    while ($pos + 8 -le $bytes.Length) {
        $id = [System.Text.Encoding]::ASCII.GetString($bytes, $pos, 4)
        $size = [BitConverter]::ToUInt32($bytes, $pos + 4)
        if ($id -eq 'fmt ') {
            $ch = [BitConverter]::ToUInt16($bytes, $pos + 10)
            $rate = [BitConverter]::ToUInt32($bytes, $pos + 12)
            $bits = [BitConverter]::ToUInt16($bytes, $pos + 22)
        } elseif ($id -eq 'data') {
            $dataSize = $size
            break
        }
        $pos += 8 + $size + ($size % 2)
    }
    $dur = 0.0
    if ($rate -gt 0 -and $ch -gt 0 -and $bits -gt 0) { $dur = $dataSize / ($rate * $ch * ($bits / 8)) }
    return [pscustomobject]@{ Rate = $rate; Channels = $ch; Bits = $bits; Duration = $dur }
}

function SplitChunks([string]$text) {
    $max = 44
    $parts = [regex]::Split($text, '(?<=[\uFF0C\uFF1B\uFF1A])')
    $chunks = New-Object System.Collections.Generic.List[string]
    $current = ''
    foreach ($p in $parts) {
        if ([string]::IsNullOrEmpty($p)) { continue }
        if (($current.Length + $p.Length) -le $max) { $current += $p; continue }
        if ($current.Length -gt 0) { $chunks.Add($current) }
        $rest = $p
        while ($rest.Length -gt $max) { $chunks.Add($rest.Substring(0, $max)); $rest = $rest.Substring($max) }
        $current = $rest
    }
    if ($current.Length -gt 0) { $chunks.Add($current) }
    if ($chunks.Count -eq 0) { $chunks.Add($text) }
    return $chunks
}

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$voice = $cfg.voice
if ($voice) { $synth.SelectVoice($voice) }
$synth.Rate = [int]$cfg.rate
$synth.Volume = 100

$lead = 2.0
$gap = 0.9
$tail = 1.5

$slideIndex = 0
$allSentences = @()
$sceneInfo = @()
$sampleRate = 0
$channels = 1
$sceneStart = 0.0
$total = 0.0

foreach ($s in $slides) {
    $slideIndex++
    $sentenceIndex = 0
    $sceneSentences = @()
    foreach ($text in $s.sentences) {
        $sentenceIndex++
        $wav = Join-Path $ttsDir ('s{0:00}_{1:00}.wav' -f $slideIndex, $sentenceIndex)
        $synth.SetOutputToWaveFile($wav)
        $synth.Speak($text)
        $synth.SetOutputToNull()
        $info = WavInfo $wav
        if ($sampleRate -eq 0) { $sampleRate = $info.Rate; $channels = $info.Channels }
        $sceneSentences += [pscustomobject]@{ File = $wav; Text = $text; Duration = $info.Duration }
    }
    $sum = 0.0
    foreach ($x in $sceneSentences) { $sum += $x.Duration }
    $dur = $lead + $sum + ($gap * ($sceneSentences.Count - 1)) + $tail
    if ($dur -lt 8) { $dur = 8 }
    $sceneInfo += [pscustomobject]@{ Index = $slideIndex; Img = $s.img; Hint = $s.hint; Start = $sceneStart; Duration = $dur; Sentences = $sceneSentences }
    $sceneStart += $dur
    $total = $sceneStart
}
$synth.Dispose()
if ($sampleRate -le 0) { throw 'invalid wav format' }
Write-Output ('TTS done: scenes=' + $sceneInfo.Count + ' total=' + [math]::Round($total, 1) + 's rate=' + $sampleRate + ' ch=' + $channels)

$ass = New-Object System.Collections.Generic.List[string]
$ass.Add('[Script Info]')
$ass.Add('ScriptType: v4.00+')
$ass.Add('PlayResX: 1440')
$ass.Add('PlayResY: 900')
$ass.Add('WrapStyle: 0')
$ass.Add('ScaledBorderAndShadow: yes')
$ass.Add('')
$ass.Add('[V4+ Styles]')
$ass.Add('Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding')
$ass.Add('Style: Sub,Microsoft YaHei,34,&H00FFFFFF,&H00FFFFFF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,3,0,2,60,60,42,1')
$ass.Add('Style: Hint,Microsoft YaHei,26,&H00FFFFFF,&H00FFFFFF,&H00000000,&HAA3D2D1F,1,0,0,0,100,100,1,0,3,0,0,8,60,60,26,1')
$ass.Add('')
$ass.Add('[Events]')
$ass.Add('Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text')

$srt = New-Object System.Collections.Generic.List[string]
$srtIndex = 0
$cueCount = 0

foreach ($scene in $sceneInfo) {
    $hintStart = $scene.Start + 0.2
    $hintEnd = $scene.Start + $scene.Duration - 0.2
    $ass.Add('Dialogue: 0,' + (AssTime $hintStart) + ',' + (AssTime $hintEnd) + ',Hint,,0,0,0,,{\fad(200,200)}' + $scene.Hint)
    $cursor = $scene.Start + $lead
    foreach ($sentence in $scene.Sentences) {
        $chunks = SplitChunks $sentence.Text
        $sumChars = 0
        foreach ($c in $chunks) { $sumChars += $c.Length }
        $chunkStart = $cursor
        foreach ($c in $chunks) {
            $chunkDur = $sentence.Duration * ($c.Length / [double]$sumChars)
            if ($chunkDur -lt 1.2) { $chunkDur = 1.2 }
            $cs = $chunkStart
            $ce = [Math]::Min($chunkStart + $chunkDur + 0.1, $scene.Start + $scene.Duration - 0.15)
            if ($ce -gt $cs) {
                $ass.Add('Dialogue: 0,' + (AssTime $cs) + ',' + (AssTime $ce) + ',Sub,,0,0,0,,{\fad(100,100)}' + $c)
                $srtIndex++
                $srt.Add([string]$srtIndex)
                $srt.Add((SrtTime $cs) + ' --> ' + (SrtTime $ce))
                $srt.Add($c)
                $srt.Add('')
                $cueCount++
            }
            $chunkStart += $chunkDur
        }
        $cursor += $sentence.Duration + $gap
    }
}

[System.IO.File]::WriteAllText((Join-Path $videoDir 'demo.ass'), ($ass -join "`r`n"), $utf8)
[System.IO.File]::WriteAllText((Join-Path $videoDir 'demo.srt'), ($srt -join "`r`n"), $utf8)
Write-Output ('ASS/SRT written: cues=' + $cueCount + ' total=' + [math]::Round($total, 1))

$gapWav = Join-Path $seqDir 'gap.wav'
$leadWav = Join-Path $seqDir 'lead.wav'
$tailWav = Join-Path $seqDir 'tail.wav'
& $ff -y -hide_banner -loglevel error -f lavfi -i ('anullsrc=r=' + $sampleRate + ':cl=' + $(if ($channels -eq 1) { 'mono' } else { 'stereo' })) -t (Fmt $gap) -c:a pcm_s16le $gapWav
& $ff -y -hide_banner -loglevel error -f lavfi -i ('anullsrc=r=' + $sampleRate + ':cl=' + $(if ($channels -eq 1) { 'mono' } else { 'stereo' })) -t (Fmt $lead) -c:a pcm_s16le $leadWav
& $ff -y -hide_banner -loglevel error -f lavfi -i ('anullsrc=r=' + $sampleRate + ':cl=' + $(if ($channels -eq 1) { 'mono' } else { 'stereo' })) -t (Fmt $tail) -c:a pcm_s16le $tailWav

$sceneAudioList = New-Object System.Collections.Generic.List[string]
foreach ($scene in $sceneInfo) {
    $listPath = Join-Path $seqDir ('scene{0:00}.txt' -f $scene.Index)
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("file '" + ($leadWav -replace '\\','/') + "'")
    $i = 0
    foreach ($sentence in $scene.Sentences) {
        $i++
        if ($i -gt 1) { $lines.Add("file '" + ($gapWav -replace '\\','/') + "'") }
        $lines.Add("file '" + ($sentence.File -replace '\\','/') + "'")
    }
    $lines.Add("file '" + ($tailWav -replace '\\','/') + "'")
    [System.IO.File]::WriteAllText($listPath, ($lines -join "`r`n"), $utf8)
    $raw = Join-Path $seqDir ('scene{0:00}-raw.wav' -f $scene.Index)
    & $ff -y -hide_banner -loglevel error -f concat -safe 0 -i $listPath -c:a pcm_s16le $raw
    if ($LASTEXITCODE -ne 0) { throw ('scene audio failed ' + $scene.Index) }
    $fixed = Join-Path $seqDir ('scene{0:00}.wav' -f $scene.Index)
    & $ff -y -hide_banner -loglevel error -i $raw -af apad -t (Fmt $scene.Duration) -ar $sampleRate -ac $channels -c:a pcm_s16le $fixed
    if ($LASTEXITCODE -ne 0) { throw ('scene pad failed ' + $scene.Index) }
    $sceneAudioList.Add("file '" + ($fixed -replace '\\','/') + "'")
}
$audioTxt = Join-Path $seqDir 'scenes.txt'
[System.IO.File]::WriteAllText($audioTxt, ($sceneAudioList -join "`r`n"), $utf8)
$mixWav = Join-Path $seqDir 'mix.wav'
& $ff -y -hide_banner -loglevel error -f concat -safe 0 -i $audioTxt -c:a pcm_s16le -ar 44100 $mixWav
if ($LASTEXITCODE -ne 0) { throw 'audio concat failed' }
Write-Output ('audio done: ' + [math]::Round((WavInfo $mixWav).Duration, 1) + 's')

$list = New-Object System.Collections.Generic.List[string]
foreach ($scene in $sceneInfo) {
    $img = Join-Path $slideDir $scene.Img
    if (-not (Test-Path -LiteralPath $img)) { throw ('missing slide ' + $scene.Img) }
    $clip = Join-Path $clipDir ('clip{0:00}.mp4' -f $scene.Index)
    $dur = $scene.Duration
    $fadeOut = [Math]::Max(0.1, $dur - 0.35)
    $vf = 'scale=1440:900:force_original_aspect_ratio=increase,crop=1440:900,setsar=1,zoompan=z=''min(zoom+0.00035,1.05)'':x=''iw/2-(iw/zoom/2)'':y=''ih/2-(ih/zoom/2)'':d=1:s=1440x900:fps=30,format=yuv420p,fade=t=in:st=0:d=0.3,fade=t=out:st=' + (Fmt $fadeOut) + ':d=0.35'
    & $ff -y -hide_banner -loglevel error -loop 1 -framerate 30 -t (Fmt $dur) -i $img -vf $vf -an -c:v libx264 -preset veryfast -crf 20 -pix_fmt yuv420p $clip
    if ($LASTEXITCODE -ne 0) { throw ('clip failed ' + $scene.Img) }
    $list.Add("file '" + ($clip -replace '\\','/') + "'")
}
[System.IO.File]::WriteAllText((Join-Path $buildDir 'clips.txt'), ($list -join "`r`n"), $utf8)
Write-Output ('clips done: ' + $list.Count)

$outMp4 = Join-Path $videoDir 'demo.mp4'
Push-Location $videoDir
try {
    & $ff -y -hide_banner -loglevel error -f concat -safe 0 -i (Join-Path $buildDir 'clips.txt') -i $mixWav -vf 'ass=demo.ass' -map 0:v -map 1:a -c:v libx264 -preset medium -crf 21 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart $outMp4
    if ($LASTEXITCODE -ne 0) { throw 'final encode failed' }
}
finally {
    Pop-Location
}
Write-Output ('DONE ' + $outMp4 + ' total=' + [math]::Round($total, 1) + 's size=' + (Get-Item -LiteralPath $outMp4).Length)
