$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech

$root = (Get-Location).Path
$videoDir = Join-Path $root 'docs\review\video'
$slideDir = Join-Path $videoDir 'slides'
$buildDir = Join-Path $videoDir '_build'
$ttsDir = Join-Path $buildDir 'tts'
$clipDir = Join-Path $buildDir 'clips'
foreach ($d in @($ttsDir, $clipDir)) { if (-not (Test-Path -LiteralPath $d)) { New-Item -ItemType Directory -Path $d | Out-Null } }

$ff = 'D:\SoftWare\tools\tts-vue\tts-vue-main\tts-vue-main\node_modules\@ffmpeg-installer\win32-x64\ffmpeg.exe'
if (-not (Test-Path -LiteralPath $ff)) { throw 'ffmpeg not found' }

$timeline = Get-Content -LiteralPath (Join-Path $buildDir 'timeline.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$slides = $timeline.slides

function Format-AssTime([double]$seconds) {
    $ts = [TimeSpan]::FromSeconds($seconds)
    return ('{0}:{1:00}:{2:00}.{3:00}' -f [int][Math]::Floor($ts.TotalHours), $ts.Minutes, $ts.Seconds, [int][Math]::Floor($ts.Milliseconds / 10))
}

function Format-SrtTime([double]$seconds) {
    $ts = [TimeSpan]::FromSeconds($seconds)
    return ('{0:00}:{1:00}:{2:00},{3:000}' -f [int][Math]::Floor($ts.TotalHours), $ts.Minutes, $ts.Seconds, $ts.Milliseconds)
}

$ass = New-Object System.Collections.Generic.List[string]
$ass.Add('[Script Info]')
$ass.Add('ScriptType: v4.00+')
$ass.Add('PlayResX: 1440')
$ass.Add('PlayResY: 900')
$ass.Add('WrapStyle: 2')
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
$offset = 0.0
$total = 0.0
foreach ($s in $slides) { $total += [double]$s.dur }

foreach ($s in $slides) {
    $start = $offset
    $end = $offset + [double]$s.dur
    $ass.Add('Dialogue: 0,' + (Format-AssTime $start) + ',' + (Format-AssTime $end) + ',Hint,,0,0,0,,{\fad(200,200)}' + $s.hint)
    foreach ($cue in $s.cues) {
        $cs = $start + [double]$cue.t
        $ce = [Math]::Min($cs + 4.6, $end - 0.15)
        if ($ce -le $cs) { continue }
        $ass.Add('Dialogue: 0,' + (Format-AssTime $cs) + ',' + (Format-AssTime $ce) + ',Sub,,0,0,0,,{\fad(120,120)}' + $cue.text)
        $srtIndex++
        $srt.Add([string]$srtIndex)
        $srt.Add((Format-SrtTime $cs) + ' --> ' + (Format-SrtTime $ce))
        $srt.Add($cue.text)
        $srt.Add('')
    }
    $offset = $end
}

$assPath = Join-Path $videoDir 'demo.ass'
$srtPath = Join-Path $videoDir 'demo.srt'
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($assPath, ($ass -join "`r`n"), $utf8)
[System.IO.File]::WriteAllText($srtPath, ($srt -join "`r`n"), $utf8)
Write-Output ("ASS/SRT written, cues=" + $srtIndex + ", total=" + $total)

function Get-WavDuration([string]$path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $pos = 12
    $sampleRate = 0; $channels = 0; $bits = 0; $dataSize = 0
    while ($pos + 8 -le $bytes.Length) {
        $id = [System.Text.Encoding]::ASCII.GetString($bytes, $pos, 4)
        $size = [BitConverter]::ToUInt32($bytes, $pos + 4)
        if ($id -eq 'fmt ') {
            $channels = [BitConverter]::ToUInt16($bytes, $pos + 10)
            $sampleRate = [BitConverter]::ToUInt32($bytes, $pos + 12)
            $bits = [BitConverter]::ToUInt16($bytes, $pos + 22)
        } elseif ($id -eq 'data') {
            $dataSize = $size
            break
        }
        $pos += 8 + $size + ($size % 2)
    }
    if ($sampleRate -le 0 -or $channels -le 0 -or $bits -le 0) { return 0 }
    return $dataSize / ($sampleRate * $channels * ($bits / 8))
}

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.SelectVoice('Microsoft Huihui Desktop')
$synth.Rate = -1
$synth.Volume = 100
$audioMeta = @()
$index = 0
$offset = 0.0
foreach ($s in $slides) {
    $index++
    $text = ($s.cues | ForEach-Object { $_.text }) -join ([char]0x3002)
    $wav = Join-Path $ttsDir ('slide{0:00}.wav' -f $index)
    $synth.SetOutputToWaveFile($wav)
    $synth.Speak($text)
    $synth.SetOutputToNull()
    $dur = Get-WavDuration $wav
    $avail = [double]$s.dur - 1.2
    $factor = 1.0
    if ($dur -gt $avail -and $avail -gt 0) { $factor = [Math]::Min(1.6, [Math]::Max(1.0, $dur / $avail)) }
    $audioMeta += [pscustomobject]@{ Index = $index; Path = $wav; Start = $offset + 0.6; Factor = [Math]::Round($factor, 3); Dur = [Math]::Round($dur, 2) }
    $offset += [double]$s.dur
}
$synth.Dispose()
Write-Output ('TTS done: ' + (($audioMeta | ForEach-Object { 'S' + $_.Index + '=' + $_.Dur + 's/' + $_.Factor }) -join ' '))

$index = 0
$list = New-Object System.Collections.Generic.List[string]
foreach ($s in $slides) {
    $index++
    $img = Join-Path $slideDir $s.img
    if (-not (Test-Path -LiteralPath $img)) { throw ('missing slide ' + $s.img) }
    $clip = Join-Path $clipDir ('clip{0:00}.mp4' -f $index)
    $dur = [double]$s.dur
    $fadeOut = [Math]::Max(0.1, $dur - 0.35)
    $vf = 'scale=1440:900:force_original_aspect_ratio=decrease,pad=1440:900:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1,zoompan=z=''min(zoom+0.00035,1.05)'':x=''iw/2-(iw/zoom/2)'':y=''ih/2-(ih/zoom/2)'':d=1:s=1440x900:fps=30,format=yuv420p,fade=t=in:st=0:d=0.3,fade=t=out:st=' + $fadeOut + ':d=0.35'
    & $ff -y -hide_banner -loglevel error -loop 1 -framerate 30 -t $dur -i $img -vf $vf -an -c:v libx264 -preset veryfast -crf 20 -pix_fmt yuv420p $clip
    if ($LASTEXITCODE -ne 0) { throw ('clip failed: ' + $s.img) }
    $list.Add("file '" + ($clip -replace '\\','/') + "'")
}
[System.IO.File]::WriteAllText((Join-Path $buildDir 'clips.txt'), ($list -join "`r`n"), $utf8)
Write-Output ('clips done: ' + $list.Count)

$audioList = New-Object System.Collections.Generic.List[string]
$i = 0
$slideOffset = 0.0
foreach ($s in $slides) {
    $i++
    $m = $audioMeta[$i - 1]
    $aud = Join-Path $buildDir ('aud{0:00}.wav' -f $i)
    $delay = [int]($m.Start * 1000)
    $af = ('atempo={0},adelay={1}|{1},apad' -f $m.Factor, $delay)
    & $ff -y -hide_banner -loglevel error -i $m.Path -af $af -t ([double]$s.dur) -ar 22050 -ac 1 -c:a pcm_s16le $aud
    if ($LASTEXITCODE -ne 0) { throw ('audio pad failed: slide ' + $i) }
    $audioList.Add("file '" + ($aud -replace '\\','/') + "'")
    $slideOffset += [double]$s.dur
}
$audioTxt = Join-Path $buildDir 'audio.txt'
[System.IO.File]::WriteAllText($audioTxt, ($audioList -join "`r`n"), $utf8)
$mixWav = Join-Path $buildDir 'mix.wav'
& $ff -y -hide_banner -loglevel error -f concat -safe 0 -i $audioTxt -c:a pcm_s16le -ar 44100 $mixWav
if ($LASTEXITCODE -ne 0) { throw 'audio concat failed' }
Write-Output ('audio mix done, total=' + $slideOffset)

$outMp4 = Join-Path $videoDir 'demo.mp4'
Push-Location $videoDir
try {
    & $ff -y -hide_banner -loglevel error -f concat -safe 0 -i (Join-Path $buildDir 'clips.txt') -i $mixWav -vf 'ass=demo.ass' -map 0:v -map 1:a -c:v libx264 -preset medium -crf 21 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart $outMp4
    if ($LASTEXITCODE -ne 0) { throw 'final encode failed' }
}
finally {
    Pop-Location
}
$size = (Get-Item -LiteralPath $outMp4).Length
Write-Output ('DONE ' + $outMp4 + ' size=' + $size)
