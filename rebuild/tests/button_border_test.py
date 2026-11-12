"""Pixel and upgrade check on the disposable Border_API18 ARM emulator."""
from pathlib import Path
import subprocess, time, re, json, uuid, xml.etree.ElementTree as ET
from PIL import Image

BASE=Path(__file__).resolve().parents[2]
ADB='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[ADB,'-s','emulator-5596']
PKG='rubberbigpepper.VolumeControl.redesign'

def adb(*args,timeout=90):
    return subprocess.check_output(PREFIX+list(args),stderr=subprocess.STDOUT,timeout=timeout).decode('utf-8',errors='replace')
def shell(*args):return adb('shell',*args)
def launch():
    result=shell('am','start','-W','-n',PKG+'/rubberbigpepper.VolumeControl.MainWindow')
    assert 'Status: ok' in result,result
    shell('input','keyevent','82');time.sleep(1)
def prefs():
    return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(shell('cat','/data/data/'+PKG+'/shared_prefs/Common.xml'))}
def buttons():
    file='/data/local/tmp/border-'+uuid.uuid4().hex+'.xml'
    result=shell('uiautomator','dump',file)
    assert 'dumped' in result,result
    root=ET.fromstring(shell('cat',file))
    return {n.get('text'):tuple(map(int,re.findall(r'\d+',n.get('bounds')))) for n in root.iter('node')
            if n.get('text') in ('Показать шкалу','Скрыть шкалу') and n.get('class')=='android.widget.Button'}
def screen(name):
    remote='/data/local/tmp/border-'+name+'.png';file=BASE/('output/button-border-'+name+'.png')
    shell('screencap','-p',remote);adb('pull',remote,str(file))
    return Image.open(file).convert('RGB')
def border_pixels(image,bounds,color):
    left,top,right,bottom=bounds
    assert bottom-top>=48 and right>left
    middle=(left+right)//2
    # API18 Skia rounds the antialiased green edge by one RGB level.
    return sum(all(abs(actual-expected)<=2 for actual,expected in zip(image.getpixel((x,y)),color))
               for x in range(middle-20,middle+20) for y in range(top,top+3))

until=time.monotonic()+240
while time.monotonic()<until:
    try:
        if shell('getprop','sys.boot_completed').strip()=='1':break
    except subprocess.SubprocessError:pass
    time.sleep(2)
else:raise AssertionError('Disposable ARM API18 did not boot')
assert shell('getprop','ro.build.version.sdk').strip()=='18'
print('ARM API18 ready',flush=True)
assert 'Success' in adb('install','-r','-d',str(BASE/'output/VolumeControl-2.7.1-redesign8-api18.apk'))
launch();before=prefs();bounds=buttons();image=screen('before')
assert border_pixels(image,bounds['Скрыть шкалу'],(104,104,104))==0,'Baseline does not reproduce missing border'
print('Missing inactive border reproduced on redesign8',flush=True)
assert 'Success' in adb('install','-r',str(BASE/'output/VolumeControl-2.7.1-redesign9-api18.apk'))
launch();assert prefs()==before,'Upgrade changed preferences'
results={}
for chosen in ('Показать шкалу','Скрыть шкалу'):
    bounds=buttons();left,top,right,bottom=bounds[chosen]
    shell('input','tap',str((left+right)//2),str((top+bottom)//2));time.sleep(.5)
    image=screen('shown' if chosen=='Показать шкалу' else 'hidden')
    for name,box in bounds.items():
        count=border_pixels(image,box,(30,215,96) if name==chosen else (104,104,104))
        assert count>=30,'Missing border for '+name+' while selected: '+chosen
        results[chosen+' / '+name]=count
assert prefs()==before,'Show/hide changed saved settings'
report={'api':18,'upgrade_from':'redesign8','version':'redesign9','preferences_preserved':len(before),
        'old_missing_border_reproduced':True,'both_selection_states_checked':True,'border_pixels':results}
(BASE/'output/button-border-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False),flush=True)
