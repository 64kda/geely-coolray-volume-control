param([string]$Apk = '')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskJdk = 'C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin'
$taskPython = 'C:/Users/XGamerXFuryX/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
$taskOutput = Join-Path $taskRoot 'rebuild/dual-model-tests'
New-Item -ItemType Directory -Force -Path $taskOutput | Out-Null
$taskPackage = Join-Path $taskRoot 'source/app/src/main/java/rubberbigpepper/VolumeControl/redesign'
& "$taskJdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d $taskOutput "$taskPackage/DualScaleState.java" "$taskPackage/ScalePlacement.java" "$taskPackage/VolumeConfirmation.java" "$taskPackage/AutoHideState.java" "$taskPackage/TouchGuardState.java" "$PSScriptRoot/DualScaleStateTest.java" "$PSScriptRoot/ScalePlacementTest.java" "$PSScriptRoot/VolumeConfirmationTest.java" "$PSScriptRoot/AutoHideRegressionTest.java" "$PSScriptRoot/TouchGuardStateTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Model compilation failed' }
foreach ($taskTest in @('DualScaleStateTest','ScalePlacementTest','VolumeConfirmationTest','AutoHideRegressionTest','TouchGuardStateTest')) {
    & "$taskJdk/java.exe" -cp $taskOutput $taskTest
    if ($LASTEXITCODE -ne 0) { throw "$taskTest failed" }
}
& $taskPython "$PSScriptRoot/check_overlay_visibility.py"
if ($LASTEXITCODE -ne 0) { throw 'Overlay visibility regression failed' }
if ($Apk) {
    $taskApk = (Resolve-Path -LiteralPath $Apk -ErrorAction Stop).Path
    foreach ($taskCheck in @('check_dual_scale_apk.py','validate_apk.py')) {
        & $taskPython (Join-Path $PSScriptRoot $taskCheck) $taskApk
        if ($LASTEXITCODE -ne 0) { throw "$taskCheck failed" }
    }
}
$taskIconApk = if ($Apk -and (Split-Path $Apk -Leaf) -match 'redesign(1[89]|20)') { $Apk } else { '' }
& "$PSScriptRoot/run-icons.ps1" -Apk $taskIconApk
if ($LASTEXITCODE -ne 0) { throw 'Launcher icon regression failed' }
