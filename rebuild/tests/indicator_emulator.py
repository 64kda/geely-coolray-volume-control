"""Opt-in API18 emulator check; never targets a physical Android device."""
from pathlib import Path
import subprocess, re, json, time, sys, hashlib, xml.etree.ElementTree as ET
BASE=Path(__file__).resolve().parents[2]
ADB='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[ADB,'-s','emulator-5584']
PKG='rubberbigpepper.VolumeControl.redesign'
PREF='/data/data/'+PKG+'/shared_prefs/Common.xml'
OUT=BASE/'output'
def adb(*args):return subprocess.check_output(PREFIX+list(args),stderr=subprocess.STDOUT,timeout=35)
def shell(*args):return adb('shell',*map(str,args)).decode('utf-8',errors='replace').replace('\r','')
assert shell('getprop','ro.build.version.sdk').strip()=='18'
assert shell('getprop','ro.kernel.qemu').strip()=='1','Physical device forbidden'
assert 'Coolray_API18' in adb('emu','avd','name').decode(),'Use isolated standard AVD'
backup=adb('shell','cat',PREF).replace(b'\r\n',b'\n').replace(b'\r',b'')
recovery=BASE/'.recovery/indicator-20261105/emulator-api18-Common.xml'
if not recovery.exists():recovery.write_bytes(backup)
def parse(data):
    return {n.get('name'):n.get('value') for n in ET.fromstring(data)}
old=parse(backup)
def stop():shell('am','force-stop',PKG)
def start():
    shell('input','keyevent','82')
    result=shell('am','start','-W','-n',PKG+'/rubberbigpepper.VolumeControl.MainWindow')
    assert 'Error' not in result,result
def configure(**settings):
    stop();tree=ET.fromstring(backup)
    for key,value in settings.items():
        key=key.replace('_',' ')
        node=next((n for n in tree if n.get('name')==key),None)
        if node is None:node=ET.SubElement(tree,'boolean' if isinstance(value,bool) else 'int',name=key)
        node.set('value',str(value).lower() if isinstance(value,bool) else str(value))
    local=BASE/'rebuild/indicator-emulator-prefs.xml';ET.ElementTree(tree).write(local,encoding='utf-8',xml_declaration=True)
    adb('push',str(local),'/data/local/tmp/indicator-prefs.xml')
    shell('sh','-c',"'cat /data/local/tmp/indicator-prefs.xml > "+PREF+"'")
    start();time.sleep(.4)
def windows():return shell('dumpsys','window','windows')
def frames(data):
    found=[]
    for block in data.split('Window #'):
        if 'package='+PKG not in block:continue
        m=re.search(r'mFrame=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]',block)
        if m:found.append((list(map(int,m.groups())),block))
    return found
def overlay():
    return next((f for f,b in frames(windows()) if 'ty=2010' in b or 'type=2010' in b),None)
def screen(name):
    shell('screencap','-p','/data/local/tmp/indicator-screen.png')
    adb('pull','/data/local/tmp/indicator-screen.png',str(OUT/(name+'.png')))
def adjust(frame):
    x=(frame[0]+frame[2])//2;y=frame[1]+(frame[3]-frame[1])*3//4
    result=shell('input','touchscreen','swipe',x,y,x,y-80,100)
    assert 'Error:' not in result,result
def percentage_frames(data):return [f for f,b in frames(data) if 'ty=2006' in b or 'type=2006' in b]
def hierarchy():
    name='/data/local/tmp/indicator-ui-'+str(time.time_ns())+'.xml'
    result=shell('uiautomator','dump',name);assert 'dumped' in result,result
    return ET.fromstring(adb('shell','cat',name))
def tap_text(root,text):
    node=next(n for n in root.iter('node') if n.get('text')==text)
    l,t,r,b=map(int,re.findall(r'\d+',node.get('bounds')))
    result=shell('input','touchscreen','tap',(l+r)//2,(t+b)//2);assert 'Error:' not in result,result
report={'environment':'Coolray_API18_ARM emulator-5584; standard API18, system image unchanged; not Coolray firmware','checks':[]}
try:
    stop()
    artifact=OUT/'VolumeControl-2.7.1-redesign11-indicator-api18.apk'
    if '--skip-install' not in sys.argv:
        result=adb('install','--no-streaming','-r',str(artifact)).decode()
        assert 'Success' in result,result
    installed=shell('pm','path',PKG).strip().removeprefix('package:')
    installed_file=BASE/'rebuild/indicator-installed.apk';adb('pull',installed,str(installed_file))
    assert hashlib.sha256(installed_file.read_bytes()).digest()==hashlib.sha256(artifact.read_bytes()).digest(),'Installed APK differs from artifact'
    start();time.sleep(.4)
    current=parse(adb('shell','cat',PREF))
    for key in ['Size','Length','Bar type']:assert current.get(key)==old.get(key),(key,'migration changed saved value')
    assert current.get('Percent position','0')==old.get('Percent position','0'),'Legacy percentage default'
    report['checks'].append('update preserves saved Size, Length, style and default percentage')
    for side in [11,5]:
        configure(Size=26,Length=130,Bar_type=2,Gravity=side,Percent_position=1,Fullscreen=False,
                  Horizontal_offset=0,Vertical_offset=0,Show_notification=False,Stream_type=3,Auto_hide=False)
        frame=overlay();assert frame,windows()
        assert frame[2]-frame[0]==26,frame
        assert 420<=frame[3]-frame[1]<=432,frame
        assert frame[1]>=24 and frame[3]<=720,frame
        adjust(frame);state=windows();active=percentage_frames(state)
        if not active:
            screen('indicator-failure');(OUT/'indicator-failure-windows.txt').write_text(state,encoding='utf-8')
            (OUT/'indicator-failure-logcat.txt').write_bytes(adb('logcat','-d','-t','300'))
        assert len(active)==1,active
        text=active[0];assert text[3]<=frame[1] and text[1]>=24,(frame,text)
        adjust(frame);screen('indicator-'+('right' if side==11 else 'left'))
        time.sleep(1.2);assert not percentage_frames(windows()),'Percentage did not hide'
        report['checks'].append({'side':side,'scale':frame,'percentage':text,'single_window_and_hide':'passed'})
    # The UI must expose the new options and retain values above the old maximum.
    configure(Size=26,Length=130,Bar_type=2,Gravity=11,Percent_position=1,Fullscreen=False,
              Horizontal_offset=0,Vertical_offset=0,Show_notification=False,Stream_type=3)
    assert parse(adb('shell','cat',PREF))['Length']=='130','Length truncated on startup'
    stop();start();assert parse(adb('shell','cat',PREF))['Length']=='130','Length lost on restart'
    report['checks'].append('length130 survives application restart')
    shell('input','touchscreen','tap',260,110)
    for n in range(2):shell('input','touchscreen','swipe',100,650,100,230,300)
    controls=hierarchy()
    assert any(n.get('text')=='Положение процента' for n in controls.iter('node')),'Percentage control missing'
    tap_text(controls,'Рядом со шкалой (рекомендуется)')
    tap_text(hierarchy(),'По центру экрана (совместимость)')
    assert parse(adb('shell','cat',PREF))['Percent position']=='0','UI position not saved'
    tap_text(hierarchy(),'По центру экрана (совместимость)')
    tap_text(hierarchy(),'Рядом со шкалой (рекомендуется)')
    tap_text(hierarchy(),'Равномерные сегменты')
    tap_text(hierarchy(),'Столбики уровня')
    assert parse(adb('shell','cat',PREF))['Bar type']=='1','Legacy style selection failed'
    tap_text(hierarchy(),'Столбики уровня')
    tap_text(hierarchy(),'Равномерные сегменты')
    screen('indicator-options')
    stop();start();saved=parse(adb('shell','cat',PREF))
    for key,value in [('Size','26'),('Length','130'),('Bar type','2'),('Percent position','1')]:assert saved[key]==value,(key,saved[key])
    report['checks'].append('native settings UI selects centre/nearby and legacy/uniform styles; all four geometry/style/position keys survive restart')
    # Hardware-key event through the existing receiver, using the ring stream.
    configure(Size=26,Length=130,Bar_type=2,Gravity=11,Percent_position=1,Fullscreen=False,
              Horizontal_offset=0,Vertical_offset=0,Show_notification=False,Stream_type=2)
    shell('input','keyevent','25')
    assert len(percentage_frames(windows()))==1,'External key did not show percentage'
    shell('input','keyevent','24');screen('indicator-hardware-key')
    report['checks'].append('Android volume key shows adjacent app percentage through existing receiver')
    report['passed']=True
finally:
    stop()
    # Restore emulator preferences, preserving ownership of the existing file.
    local=BASE/'rebuild/indicator-emulator-restore.xml';local.write_bytes(backup)
    adb('push',str(local),'/data/local/tmp/indicator-prefs.xml')
    shell('sh','-c',"'cat /data/local/tmp/indicator-prefs.xml > "+PREF+"'")
    report['emulator_preferences_restored']=parse(adb('shell','cat',PREF))==old
    (OUT/'indicator-emulator.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
