param([string]$Apk = '')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskJdk='C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin'
$taskPython='C:/Users/XGamerXFuryX/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
$taskClasses=Join-Path $taskRoot 'rebuild/icon-tests'
New-Item -ItemType Directory -Force -Path $taskClasses | Out-Null
& "$taskJdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d $taskClasses "$taskRoot/source/app/src/main/java/rubberbigpepper/VolumeControl/redesign/IconSelection.java" "$PSScriptRoot/IconSelectionTest.java"
if($LASTEXITCODE -ne 0){throw 'Icon test compilation failed'}
& "$taskJdk/java.exe" -cp $taskClasses IconSelectionTest
if($LASTEXITCODE -ne 0){throw 'Icon transitions failed'}
& $taskPython "$PSScriptRoot/check_launcher_icons.py" $Apk
if($LASTEXITCODE -ne 0){throw 'Icon resources/manifest/DEX checks failed'}
