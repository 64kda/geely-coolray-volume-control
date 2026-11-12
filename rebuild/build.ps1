$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$taskJava = 'C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin/java.exe'
$taskJavac = 'C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin/javac.exe'
$taskPython = 'C:/Users/XGamerXFuryX/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
$taskSdk = 'C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk'
Push-Location $projectRoot
try {
    New-Item -ItemType Directory -Path rebuild/classes,rebuild/stub-classes,rebuild/test-classes,rebuild/helper-dex,rebuild/signing,output -Force | Out-Null
    $helperDecodeTarget = [IO.Path]::GetFullPath((Join-Path $projectRoot 'rebuild/helper-smali'))
    if (-not $helperDecodeTarget.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe generated helper path' }
    if (-not (Test-Path -LiteralPath rebuild/source/apktool.yml)) {
        & $taskJava -jar tools/apktool_3.0.3.jar d app-v2.7.1.apk -o rebuild/source -p rebuild/framework
        if ($LASTEXITCODE -ne 0) { throw 'Original APK decode failed' }
    }
    & $taskJavac -source 8 -target 8 -Xlint:-options -classpath "$taskSdk/platforms/android-18/android.jar" -d rebuild/stub-classes rebuild/stubs/rubberbigpepper/CommonCtrl/ZoomView.java
    if ($LASTEXITCODE -ne 0) { throw 'Compile-only stub failed' }
    $nativeJava = Get-ChildItem rebuild/java -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName
    & $taskJavac -source 8 -target 8 -Xlint:-options -encoding UTF-8 -classpath "rebuild/stub-classes;$taskSdk/platforms/android-18/android.jar" -d rebuild/classes $nativeJava
    if ($LASTEXITCODE -ne 0) { throw 'Native UI compilation failed' }
    & $taskJavac -classpath rebuild/classes -d rebuild/test-classes rebuild/tests/VolumeMathTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    & $taskJava -classpath 'rebuild/classes;rebuild/test-classes' VolumeMathTest
    if ($LASTEXITCODE -ne 0) { throw 'Volume tests failed' }
    & $taskJavac -classpath rebuild/classes -d rebuild/test-classes rebuild/tests/TabDefaultsTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Reset test compilation failed' }
    & $taskJava -classpath 'rebuild/classes;rebuild/test-classes' TabDefaultsTest
    if ($LASTEXITCODE -ne 0) { throw 'Tab reset isolation tests failed' }
    & $taskJavac -classpath rebuild/classes -d rebuild/test-classes rebuild/tests/TouchGuardStateTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Touch guard test compilation failed' }
    & $taskJava -classpath 'rebuild/classes;rebuild/test-classes' TouchGuardStateTest
    if ($LASTEXITCODE -ne 0) { throw 'Touch guard state tests failed' }
    & $taskJavac -classpath rebuild/classes -d rebuild/test-classes rebuild/tests/AutoHideStateTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Auto-hide test compilation failed' }
    & $taskJava -classpath 'rebuild/classes;rebuild/test-classes' AutoHideStateTest
    if ($LASTEXITCODE -ne 0) { throw 'Auto-hide lifecycle tests failed' }
    & $taskJavac -classpath rebuild/classes -d rebuild/test-classes rebuild/tests/MarkerVisibilityTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Marker test compilation failed' }
    & $taskJava -classpath 'rebuild/classes;rebuild/test-classes' MarkerVisibilityTest
    if ($LASTEXITCODE -ne 0) { throw 'Marker timer tests failed' }
    & $taskJavac -classpath rebuild/classes -d rebuild/test-classes rebuild/tests/IndicatorGeometryTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Indicator test compilation failed' }
    & $taskJava -classpath 'rebuild/classes;rebuild/test-classes' IndicatorGeometryTest
    if ($LASTEXITCODE -ne 0) { throw 'Indicator geometry failed' }
    & $taskPython rebuild/tests/check_indicator_layout.py
    if ($LASTEXITCODE -ne 0) { throw 'Indicator layout failed' }
    & $taskPython rebuild/tests/check_marker_render.py
    if ($LASTEXITCODE -ne 0) { throw 'Marker rendering regression failed' }
    & $taskPython rebuild/tests/check_feedback_render.py
    if ($LASTEXITCODE -ne 0) { throw 'Feedback rendering regression failed' }
    & $taskPython rebuild/tests/check_touch_guard_adapter.py
    if ($LASTEXITCODE -ne 0) { throw 'Touch guard adapter tests failed' }
    & $taskPython rebuild/tests/check_scale_state.py
    if ($LASTEXITCODE -ne 0) { throw 'Read-only settings refresh test failed' }
    & $taskPython -c "import zipfile,pathlib; p=pathlib.Path('rebuild/classes'); z=zipfile.ZipFile('rebuild/ui-helper.jar','w',zipfile.ZIP_DEFLATED); [z.write(f,f.relative_to(p).as_posix()) for f in p.rglob('*.class')]; z.close()"
    & $taskJava -cp "$taskSdk/build-tools/35.0.0/lib/d8.jar" com.android.tools.r8.D8 --min-api 18 --lib "$taskSdk/platforms/android-18/android.jar" --classpath rebuild/stub-classes --output rebuild/helper-dex rebuild/ui-helper.jar
    if ($LASTEXITCODE -ne 0) { throw 'DEX compilation failed' }
    & $taskPython -c "import zipfile; z=zipfile.ZipFile('rebuild/helper-wrapper.apk','w',zipfile.ZIP_DEFLATED); original=zipfile.ZipFile('app-v2.7.1.apk'); [z.writestr(n,original.read(n)) for n in ['AndroidManifest.xml','resources.arsc']]; z.write('rebuild/helper-dex/classes.dex','classes.dex'); z.close()"
    & $taskJava -jar tools/apktool_3.0.3.jar d -f -r rebuild/helper-wrapper.apk -o rebuild/helper-smali -p rebuild/framework
    if ($LASTEXITCODE -ne 0) { throw 'DEX disassembly failed' }
    & $taskPython rebuild/patch_apk.py
    if ($LASTEXITCODE -ne 0) { throw 'Patch failed' }
    & $taskJava -jar tools/apktool_3.0.3.jar b rebuild/source -o rebuild/redesign-unsigned.apk -p rebuild/framework
    if ($LASTEXITCODE -ne 0) { throw 'APK build failed' }
    & "$taskSdk/build-tools/35.0.0/zipalign.exe" -f 4 rebuild/redesign-unsigned.apk rebuild/redesign-aligned.apk
    if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }
    if (-not (Test-Path -LiteralPath rebuild/signing/redesign-test.p12)) {
        & 'C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin/keytool.exe' -genkeypair -keystore rebuild/signing/redesign-test.p12 -storepass android -keypass android -alias redesign-test -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Volume Control Redesign Test' -noprompt
        if ($LASTEXITCODE -ne 0) { throw 'Test signing key generation failed' }
    }
    & $taskJava -jar "$taskSdk/build-tools/35.0.0/lib/apksigner.jar" sign --ks rebuild/signing/redesign-test.p12 --ks-key-alias redesign-test --ks-pass pass:android --key-pass pass:android --min-sdk-version 18 --v1-signing-enabled true --v2-signing-enabled true --out output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk rebuild/redesign-aligned.apk
    if ($LASTEXITCODE -ne 0) { throw 'Signing failed' }
    & $taskJava -jar "$taskSdk/build-tools/35.0.0/lib/apksigner.jar" verify --min-sdk-version 18 --verbose output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'Signature verification failed' }
    & $taskPython rebuild/tests/check_ui_bootstrap.py output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'UI bootstrap regression check failed' }
    & $taskPython rebuild/tests/check_publisher_links.py output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'Publisher link removal check failed' }
    & $taskPython rebuild/tests/check_marker_apk.py output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'Marker lifecycle or audio regression check failed' }
    & $taskPython rebuild/tests/check_settings_volume.py output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'Settings volume regression check failed' }
    & $taskPython rebuild/tests/check_startup_autohide.py output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'Startup auto-hide regression failed' }
    & $taskPython rebuild/tests/check_indicator_apk.py output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'Indicator compatibility check failed' }
    & $taskPython rebuild/tests/check_touch_guard_apk.py output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk
    if ($LASTEXITCODE -ne 0) { throw 'Touch guard DEX integration failed' }
} finally { Pop-Location }
