param([switch]$BuildOnly,[string]$Serial='', [string]$ExpectedAvd='VolumeIcons_API18')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskSdk='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk'
$taskJdk='C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin'
$taskPython='C:/Users/XGamerXFuryX/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
$taskOut=Join-Path $taskRoot 'rebuild/icon-android-tests'
New-Item -ItemType Directory -Force -Path "$taskOut/classes","$taskOut/ui-classes","$taskOut/dex","$taskOut/ui-dex","$taskOut/assets" | Out-Null
foreach($taskId in @(7,6,8,10)){Copy-Item -LiteralPath "$PSScriptRoot/icon-baselines/art-$taskId.png" -Destination "$taskOut/assets/art-$taskId.png"}
$taskClasspath="$taskSdk/platforms/android-18/android.jar;$taskRoot/rebuild/classes"
& "$taskJdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath $taskClasspath -d "$taskOut/classes" "$PSScriptRoot/android/LauncherIconTest.java"
if($LASTEXITCODE -ne 0){throw 'Instrumentation compilation failed'}
& "$taskJdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath "$taskSdk/platforms/android-18/android.jar;$taskSdk/platforms/android-18/uiautomator.jar" -d "$taskOut/ui-classes" "$PSScriptRoot/android/LauncherIconUiTest.java"
if($LASTEXITCODE -ne 0){throw 'UIAutomator compilation failed'}
& "$taskJdk/jar.exe" cf "$taskOut/tests.jar" -C "$taskOut/classes" .
& "$taskJdk/jar.exe" cf "$taskOut/ui.jar" -C "$taskOut/ui-classes" .
foreach($taskKind in @('tests','ui')){
    $taskDex=if($taskKind -eq 'tests'){"$taskOut/dex"}else{"$taskOut/ui-dex"}
    & "$taskJdk/java.exe" -cp "$taskSdk/build-tools/35.0.0/lib/d8.jar" com.android.tools.r8.D8 --min-api 18 --lib "$taskSdk/platforms/android-18/android.jar" --classpath "$taskSdk/platforms/android-18/uiautomator.jar" --classpath "$taskRoot/rebuild/ui-helper.jar" --output $taskDex "$taskOut/$taskKind.jar"
    if($LASTEXITCODE -ne 0){throw 'Test DEX compilation failed'}
}
& "$taskSdk/build-tools/35.0.0/aapt.exe" package -f -M "$PSScriptRoot/android/AndroidManifest.xml" -I "$taskSdk/platforms/android-18/android.jar" -A "$taskOut/assets" -F "$taskOut/test-unsigned.apk"
if($LASTEXITCODE -ne 0){throw 'Test manifest packaging failed'}
@'
import pathlib,zipfile,sys
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'test-unsigned.apk','a') as z:z.write(p/'dex/classes.dex','classes.dex')
with zipfile.ZipFile(p/'ui-tests.jar','w') as z:z.write(p/'ui-dex/classes.dex','classes.dex')
'@ | & $taskPython - $taskOut
if($LASTEXITCODE -ne 0){throw 'Test archive failed'}
& "$taskJdk/java.exe" -jar "$taskSdk/build-tools/35.0.0/lib/apksigner.jar" sign --ks "$taskRoot/rebuild/signing/redesign-test.p12" --ks-key-alias redesign-test --ks-pass pass:android --key-pass pass:android --min-sdk-version 18 --out "$taskOut/icon-tests.apk" "$taskOut/test-unsigned.apk"
if($LASTEXITCODE -ne 0){throw 'Instrumentation signing failed'}
if($BuildOnly){Write-Output 'Instrumentation and UIAutomator compiled and signed; runtime NOT RUN';return}
if(!$Serial){throw 'Pass the serial of a dedicated disposable API18 mdpi emulator; tests change its settings'}
$taskAdb="$taskSdk/platform-tools/adb.exe"
function Invoke-IconAdb {
    $taskResult=& $taskAdb -s $Serial @args 2>&1
    if($LASTEXITCODE -ne 0){throw "ADB failed: $taskResult"}
    return ($taskResult -join "`n")
}
$taskAvd=Invoke-IconAdb emu avd name
if(($taskAvd -split "`n")[0].Trim() -ne $ExpectedAvd){throw 'Dedicated emulator name does not match'}
if((Invoke-IconAdb shell getprop ro.build.version.sdk).Trim() -ne '18'){throw 'Expected API18'}
if((Invoke-IconAdb shell wm density) -notmatch '160'){throw 'Expected mdpi emulator'}
Invoke-IconAdb install -r "$taskRoot/output/VolumeControl-2.7.1-redesign20-icons-settings-api18.apk"
Invoke-IconAdb install -r "$taskOut/icon-tests.apk"
$taskLog=Invoke-IconAdb shell am instrument -w -e class local.volumecontrol.icontests.LauncherIconTest local.volumecontrol.icontests/android.test.InstrumentationTestRunner
$taskLog | Set-Content -Encoding UTF8 "$taskRoot/output/icon-instrumentation.log"
if($taskLog -notmatch 'OK \(1 test\)' -or $taskLog -match 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED'){throw 'Instrumentation did not pass'}
Invoke-IconAdb push "$taskOut/ui-tests.jar" /data/local/tmp/volume-icon-ui.jar
$taskLog=Invoke-IconAdb shell uiautomator runtest /data/local/tmp/volume-icon-ui.jar -c LauncherIconUiTest
$taskLog | Set-Content -Encoding UTF8 "$taskRoot/output/icon-ui.log"
if($taskLog -notmatch 'OK \(1 test\)' -or $taskLog -match 'FAILURES|INSTRUMENTATION_FAILED'){throw 'Chooser UI/restart tests did not pass'}
foreach($taskId in @(7,6,8,10)){Invoke-IconAdb pull "/data/local/tmp/icon-choice-$taskId.png" "$taskRoot/output/icon-choice-$taskId.png"}
Write-Output 'Real component transitions, resolved artwork, launcher entry start and UI/restart PASS; screenshots require visual review'
