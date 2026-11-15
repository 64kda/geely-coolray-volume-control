$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskJdk = 'C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin'
$taskOutput = Join-Path $taskRoot 'rebuild/dual-model-tests'
New-Item -ItemType Directory -Force -Path $taskOutput | Out-Null
$taskPackage = Join-Path $taskRoot 'source/app/src/main/java/rubberbigpepper/VolumeControl/redesign'
& "$taskJdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d $taskOutput "$taskPackage/DualScaleState.java" "$taskPackage/ScalePlacement.java" "$taskPackage/VolumeConfirmation.java" "$PSScriptRoot/DualScaleStateTest.java" "$PSScriptRoot/ScalePlacementTest.java" "$PSScriptRoot/VolumeConfirmationTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Model compilation failed' }
foreach ($taskTest in @('DualScaleStateTest','ScalePlacementTest','VolumeConfirmationTest')) {
    & "$taskJdk/java.exe" -cp $taskOutput $taskTest
    if ($LASTEXITCODE -ne 0) { throw "$taskTest failed" }
}
