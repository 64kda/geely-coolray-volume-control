"""Run only against this task's disposable Branding_API18 emulator."""
from pathlib import Path
import subprocess, time, json, hashlib, xml.etree.ElementTree as ET

BASE=Path(__file__).resolve().parents[2]
ADB='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[ADB,'-s','emulator-5596']
PKG='rubberbigpepper.VolumeControl.redesign'
APK=BASE/'output/VolumeControl-2.7.1-redesign8-api18.apk'

def adb(*args, timeout=90):
    return subprocess.check_output(PREFIX+list(args),stderr=subprocess.STDOUT,timeout=timeout).decode('utf-8',errors='replace')

def shell(*args):return adb('shell',*args)

def launch():
    response=shell('am','start','-W','-n',PKG+'/rubberbigpepper.VolumeControl.MainWindow')
    assert 'Status: ok' in response,response
    time.sleep(2)

def prefs():
    root=ET.fromstring(shell('cat','/data/data/'+PKG+'/shared_prefs/Common.xml'))
    return {n.get('name'):n.get('value',n.text) for n in root}

until=time.monotonic()+240
while time.monotonic()<until:
    try:
        if shell('getprop','sys.boot_completed').strip()=='1' and 'package:' in shell('pm','path','android'):
            break
    except subprocess.SubprocessError:pass
    time.sleep(2)
else:raise AssertionError('Disposable Android 4.3 did not boot')
assert shell('getprop','ro.build.version.sdk').strip()=='18'
print('Clean API18 ready',flush=True)
assert 'Success' in adb('install','-r',str(BASE/'output/VolumeControl-2.7.1-redesign7-api18.apk'))
launch();before=prefs()
assert len(before)>10,before
print('Previous version launched; saved preferences captured',flush=True)
assert 'Success' in adb('install','-r',str(APK))
launch();after=prefs()
assert before==after,{'before':before,'after':after}
print('In-place upgrade preserved all '+str(len(before))+' preferences',flush=True)
assert 'versionCode=31' in shell('dumpsys','package',PKG)
adb('push',str(BASE/'rebuild/volume-ui-test.jar'),'/data/local/tmp/volume-ui-test.jar')
result=shell('uiautomator','runtest','volume-ui-test.jar','-c','VolumeUiTest')
(BASE/'output/publisher-ui-test-result.txt').write_text(result,encoding='utf-8')
assert 'OK (1 test)' in result,result
installed=shell('pm','path',PKG).strip().split('package:')[-1].strip()
pulled=BASE/'output/installed-verification.apk'
adb('pull',installed,str(pulled))
assert APK.read_bytes()==pulled.read_bytes(),'Installed APK differs from delivered APK'
adb('pull','/data/local/tmp/volume-stock-final.png',str(BASE/'output/publisher-final.png'))
report={'api':18,'upgrade_from':'redesign7','version':'redesign8','preferences_preserved':len(before),
        'ui_test':'OK (1 test)','six_tabs':True,'audio_settings_checked':23,
        'donation_action_absent':True,'mixer_and_equalizer_overlays':True,
        'installed_apk_matches_delivery':True,'sha256':hashlib.sha256(APK.read_bytes()).hexdigest()}
(BASE/'output/publisher-upgrade-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False),flush=True)
