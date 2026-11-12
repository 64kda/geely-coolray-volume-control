"""Upgrade and real reboot checks. Run only against the disposable emulator."""
from pathlib import Path
import json, re, subprocess, time, xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parents[2]
ADB = 'C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PKG = 'rubberbigpepper.VolumeControl.redesign'
PREFIX = [ADB, '-s', 'emulator-5590']

def adb(*args, timeout=40):
    return subprocess.check_output(PREFIX + list(args), stderr=subprocess.STDOUT, timeout=timeout).decode('utf-8', errors='replace')

def shell(*args):
    return adb('shell', *args)

def ready():
    until = time.monotonic() + 240
    while time.monotonic() < until:
        try:
            if shell('getprop', 'sys.boot_completed').strip() == '1':
                assert shell('getprop', 'ro.build.version.sdk').strip() == '18'
                return
        except (subprocess.SubprocessError, AssertionError):
            pass
        time.sleep(2)
    raise AssertionError('Android 4.3 did not finish booting')

def dump():
    response = shell('uiautomator', 'dump', '/data/local/tmp/volume-boot.xml')
    assert 'dumped' in response, response
    return ET.fromstring(shell('cat', '/data/local/tmp/volume-boot.xml'))

def click(node):
    x, y, right, bottom = map(int, re.findall(r'\d+', node.get('bounds')))
    shell('input', 'tap', str((x+right)//2), str((y+bottom)//2))

def launch():
    result = shell('am', 'start', '-W', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER', '-p', PKG)
    assert 'Error' not in result and 'Status: ok' in result, result
    return result

def reboot():
    adb('reboot')
    time.sleep(3)
    ready()

ready()
print('Android 4.3 ready', flush=True)
assert 'Success' in adb('install', '-r', str(BASE/'output/VolumeControl-2.7.1-redesign2-api18.apk'))
launch()
print('Previous version installed and launched', flush=True)
assert 'Success' in adb('install', '-r', str(BASE/'output/VolumeControl-2.7.1-redesign5-api18.apk'))
assert 'rubberbigpepper.VolumeControl.MainWindow' in launch()
shell('input', 'keyevent', '82')
root = dump()
click(next(n for n in root.iter('node') if n.get('text') == 'Работа приложения' and n.get('class') == 'android.widget.Button'))
root = dump()
boot = next(n for n in root.iter('node') if n.get('resource-id', '').endswith(':id/CheckBoxRunAfterReboot'))
assert boot.get('text') == 'Автозапуск с магнитолой'
if boot.get('checked') != 'true':
    click(boot)
root = dump()
assert next(n for n in root.iter('node') if n.get('resource-id', '').endswith(':id/CheckBoxRunAfterReboot')).get('checked') == 'true'
prefs = ET.fromstring(shell('cat', '/data/data/'+PKG+'/shared_prefs/Common.xml'))
assert next(n for n in prefs if n.get('name') == 'Start after reboot').get('value') == 'true'
print('Upgrade, launcher and autostart setting passed; rebooting', flush=True)
reboot()
assert 'package:' in shell('pm', 'path', PKG)
until = time.monotonic() + 30
while time.monotonic() < until:
    services = shell('dumpsys', 'activity', 'services', PKG)
    if 'ServiceRecord' in services and 'rubberbigpepper.CommonCtrl.VolumeControlService' in services:
        break
    time.sleep(1)
else:
    raise AssertionError('Volume service absent after real reboot: '+services)
assert 'rubberbigpepper.VolumeControl.MainWindow' in launch()
result = {'api':18, 'upgrade_from_redesign2':True, 'launcher_after_upgrade':True,
          'autostart_label_and_saved_setting':True, 'package_present_after_reboot':True,
          'service_started_after_real_reboot':True, 'launcher_after_reboot':True}
(BASE/'output/boot-upgrade-redesign5.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
print(json.dumps(result), flush=True)
