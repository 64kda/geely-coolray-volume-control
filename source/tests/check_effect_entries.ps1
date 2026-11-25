param([string]$Serial='emulator-5582')
$ErrorActionPreference='Stop'
if($Serial -notmatch '^emulator-\d+$'){throw 'Use a test emulator'}
$adb='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
$pkg='rubberbigpepper.VolumeControl.redesign'
function Device {
    $result=& $adb -s $Serial @args
    if($LASTEXITCODE){throw "ADB failed: $args"}
    $result
}
function Tree {
    Device shell uiautomator dump /data/local/tmp/effect-entry.xml | Out-Null
    $xml=(Device shell cat /data/local/tmp/effect-entry.xml) -join ''
    if(-not $xml.Contains('<hierarchy')){throw 'UI hierarchy missing'}
    ([xml]$xml).SelectNodes('//node')
}
function Tap($node) {
    if(!$node -or $node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){throw 'Clickable node missing'}
    Device shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2)) | Out-Null
    Start-Sleep -Milliseconds 450
}
function EffectsWindows {
    $windows=(Device shell dumpsys window windows) -join [Environment]::NewLine
    @(foreach($block in ($windows -split 'Window #')) {
        if($block.Contains("package=$pkg appop=SYSTEM_ALERT_WINDOW") -and $block -match 'mFrame=\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){
            $left,$top,$right,$bottom=1..4 | ForEach-Object {[int]$Matches[$_]}
            if($right-$left -gt 100 -and $bottom-$top -gt 100){ @{'right'=$right;'top'=$top} }
        }
    })
}
if((Device shell getprop ro.build.version.sdk).Trim() -ne '18'){throw 'Expected API18'}
Device shell am start -W -n "$pkg/rubberbigpepper.VolumeControl.MainWindow" | Out-Null
foreach($case in @(@('Звук','Открыть микшер',$false),@('Эквалайзер','Открыть эквалайзер',$true))){
    $tab,$button,$equalizer=$case
    Tap (Tree | Where-Object {$_.text -ieq $tab -and $_.class -eq 'android.widget.Button'} | Select-Object -First 1)
    if(@(EffectsWindows).Count){throw "Close the existing effects window before testing $button"}
    Tap (Tree | Where-Object {$_.text -ieq $button -and $_.clickable -eq 'true'} | Select-Object -First 1)
    $overlays=@(EffectsWindows)
    if($overlays.Count -ne 1){throw "Expected one effects window for $button"}
    $frame=$overlays[0]
    try {
        $out=New-Item -ItemType Directory -Path output/effect-entries -Force
        $name=if($equalizer){'equalizer'}else{'mixer'}
        Device shell screencap -p /data/local/tmp/effect-entry.png | Out-Null
        Device pull /data/local/tmp/effect-entry.png (Join-Path $out.FullName "$name.png")
        Write-Output "PASS: $tab -> $button"
    } finally {
        Device shell input tap ($frame.right-20) ($frame.top+20) | Out-Null
        Start-Sleep -Milliseconds 350
    }
}
