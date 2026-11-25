param(
    [switch]$BuildOnly,
    [string]$Serial='',
    [string]$ExpectedAvd='VolumeIcons_API18',
    [string]$Apk='output/VolumeControl-2.7.1-redesign20-verified.apk'
)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskSdk='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk'
$taskJdk='C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin'
$taskOut=Join-Path $taskRoot 'rebuild/user-flow-tests'
$taskApk=(Resolve-Path (Join-Path $taskRoot $Apk) -ErrorAction Stop).Path
New-Item -ItemType Directory -Force -Path "$taskOut/classes","$taskOut/dex","$taskOut/touch-dex" | Out-Null
$taskClasspath="$taskSdk/platforms/android-18/android.jar;$taskSdk/platforms/android-18/uiautomator.jar"
$taskSources=@(
    "$PSScriptRoot/android/LauncherIconUiTest.java",
    "$PSScriptRoot/android/ScenarioRegressionUiTest.java",
    "$PSScriptRoot/android/AutostartBootUiTest.java"
)
& "$taskJdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath $taskClasspath -d "$taskOut/classes" $taskSources
if($LASTEXITCODE -ne 0){throw 'API18 UI test compilation failed'}
& "$taskJdk/jar.exe" cf "$taskOut/ui-input.jar" -C "$taskOut/classes" .
if($LASTEXITCODE -ne 0){throw 'API18 UI class archive failed'}
& "$taskJdk/java.exe" -cp "$taskSdk/build-tools/35.0.0/lib/d8.jar" com.android.tools.r8.D8 --min-api 18 --lib "$taskSdk/platforms/android-18/android.jar" --classpath "$taskSdk/platforms/android-18/uiautomator.jar" --output "$taskOut/dex" "$taskOut/ui-input.jar"
if($LASTEXITCODE -ne 0){throw 'API18 UI test DEX conversion failed'}
& "$taskJdk/jar.exe" cf "$taskOut/user-flow-tests.jar" -C "$taskOut/dex" classes.dex
if($LASTEXITCODE -ne 0){throw 'API18 UI test archive failed'}
$taskHelper=Join-Path $taskOut 'touch-helper'
New-Item -ItemType Directory -Force -Path $taskHelper | Out-Null
& "$taskJdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath "$taskSdk/platforms/android-18/android.jar" -d $taskHelper "$taskRoot/rebuild/tests/TouchGesture.java"
if($LASTEXITCODE -ne 0){throw 'API18 test touch injector compilation failed'}
& "$taskJdk/jar.exe" cf "$taskOut/touch-helper.jar" -C $taskHelper .
& "$taskJdk/java.exe" -cp "$taskSdk/build-tools/35.0.0/lib/d8.jar" com.android.tools.r8.D8 --min-api 18 --lib "$taskSdk/platforms/android-18/android.jar" --output "$taskOut/touch-dex" "$taskOut/touch-helper.jar"
if($LASTEXITCODE -ne 0){throw 'API18 touch injector DEX conversion failed'}
& "$taskJdk/jar.exe" cf "$taskOut/touch-inject.jar" -C "$taskOut/touch-dex" classes.dex
if($LASTEXITCODE -ne 0){throw 'API18 touch injector archive failed'}
if($BuildOnly){Write-Output 'API18 UI tests compiled and packaged; emulator runtime NOT RUN';return}
if(!$Serial){throw 'Pass the serial of the dedicated disposable API18 emulator'}
$taskAdb="$taskSdk/platform-tools/adb.exe"
function Invoke-FlowAdb {
    $taskResult=& $taskAdb -s $Serial @args 2>&1
    if($LASTEXITCODE -ne 0){throw "ADB failed: $taskResult"}
    return ($taskResult -join "`n")
}
$taskAvd=Invoke-FlowAdb emu avd name
if(($taskAvd -split "`n")[0].Trim() -ne $ExpectedAvd){throw 'Refusing runtime tests outside the named disposable AVD'}
if((Invoke-FlowAdb shell getprop ro.build.version.sdk).Trim() -ne '18'){throw 'Expected Android API18'}
if((Invoke-FlowAdb shell getprop ro.kernel.qemu).Trim() -ne '1'){throw 'Runtime tests require an emulator, not a physical device'}
if((Invoke-FlowAdb shell wm density) -notmatch '160'){throw 'Expected mdpi emulator'}
Invoke-FlowAdb install -r $taskApk | Out-Null
Invoke-FlowAdb push "$taskOut/user-flow-tests.jar" /data/local/tmp/user-flow-tests.jar | Out-Null
Invoke-FlowAdb push "$taskOut/touch-inject.jar" /data/local/tmp/touch-inject.jar | Out-Null
function Invoke-FlowTest([string]$Class) {
    $taskLog=Invoke-FlowAdb shell uiautomator runtest /data/local/tmp/user-flow-tests.jar -c $Class
    $taskFile=($Class -replace '[^A-Za-z0-9_-]','_')+'.log'
    $taskLog | Set-Content -Encoding UTF8 (Join-Path $taskRoot "output/$taskFile")
    if($taskLog -notmatch 'OK \(\d+ tests?\)' -or $taskLog -match 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED'){
        throw "UI test failed: $Class`n$taskLog"
    }
    Write-Output "$Class PASS"
}
$taskBootFixture=$false
try {
    Invoke-FlowTest 'LauncherIconUiTest'
    Invoke-FlowTest 'ScenarioRegressionUiTest'
    $taskBootFixture=$true
    Invoke-FlowTest 'AutostartBootUiTest#testEnableBeforeHostReboot'
    Invoke-FlowAdb reboot | Out-Null
    $taskReady=$false
    for($taskAttempt=0;$taskAttempt -lt 90;$taskAttempt++){
        Start-Sleep -Seconds 2
        try{if((Invoke-FlowAdb shell getprop sys.boot_completed).Trim() -eq '1'){$taskReady=$true;break}}catch{}
    }
    if(!$taskReady){throw 'API18 emulator did not complete reboot'}
    $taskServices=Invoke-FlowAdb shell dumpsys activity services rubberbigpepper.VolumeControl.redesign
    if($taskServices -notmatch 'rubberbigpepper.CommonCtrl.VolumeControlService'){
        throw 'Volume service did not start after reboot with autostart enabled'
    }
    Write-Output 'Autostart after real emulator reboot PASS'
} finally {
    if($taskBootFixture){
        try{
            Invoke-FlowTest 'AutostartBootUiTest#testRestorePriorAutostartAfterReboot'
        }catch{Write-Warning "Could not restore the emulator autostart preference: $_"}
    }
}
Write-Output 'User flow tests finished. They changed only the named disposable emulator; inspect output logs and captured screenshots.'
