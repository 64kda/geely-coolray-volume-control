"""Explicitly isolated API18 F01_API18 only. Never uses physical devices."""
from pathlib import Path
import subprocess,time,re,json,xml.etree.ElementTree as ET
from PIL import Image
BASE=Path(__file__).resolve().parents[2]
ADB='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[ADB,'-s','emulator-5594']
PKG='rubberbigpepper.VolumeControl.redesign'
PREF='/data/data/'+PKG+'/shared_prefs/Common.xml'
OUT=BASE/'output'
def adb(*args,timeout=40):return subprocess.check_output(PREFIX+list(map(str,args)),stderr=subprocess.STDOUT,timeout=timeout)
def shell(*args,timeout=40):return adb('shell',*args,timeout=timeout).decode('utf-8',errors='replace').replace('\r','')
assert shell('getprop','ro.build.version.sdk').strip()=='18'
assert shell('getprop','ro.kernel.qemu').strip()=='1'
assert adb('emu','avd','name').decode().splitlines()[0]=='F01_API18'
shell('logcat','-c')
assert 'Success' in adb('install','-r',BASE/'output/VolumeControl-2.7.1-redesign13-configurable-autohide-api18.apk',timeout=90).decode()
shell('input','keyevent','82')
def launch():
    result=shell('am','start','-W','-n',PKG+'/rubberbigpepper.VolumeControl.MainWindow')
    assert 'Error' not in result,result
def stop():shell('am','force-stop',PKG)
launch();time.sleep(1)
backup=shell('cat',PREF)
(OUT/'f01-test-original-Common.xml').write_text(backup,encoding='utf-8')
def configure(delay=5000,fade=300,on=True,style=0,gravity=11,percent=0):
    stop();tree=ET.fromstring(backup)
    for key,value in {'Auto hide':on,'Auto hide delay ms':delay,'Auto hide fade ms':fade,
        'Main color':-14756000,'Background color':-531278507,'Bar type':style,'Gravity':gravity,
        'Percent position':percent,'Stream type':3}.items():
        n=next((n for n in tree if n.get('name')==key),None)
        if n is None:n=ET.SubElement(tree,'boolean' if isinstance(value,bool) else 'int',name=key)
        n.set('value',str(value).lower())
    local=OUT/'f01-test-Common.xml';ET.ElementTree(tree).write(local,encoding='utf-8',xml_declaration=True)
    adb('push',local,'/data/local/tmp/f01-prefs.xml')
    shell('sh','-c',"'cat /data/local/tmp/f01-prefs.xml > "+PREF+"'")
    launch()
def screen(name):
    shell('screencap','-p','/data/local/tmp/f01.png');adb('pull','/data/local/tmp/f01.png',OUT/(name+'.png'))
    return Image.open(OUT/(name+'.png')).convert('RGB')
def frame():
    data=shell('dumpsys','window','windows')
    for block in data.split('Window #'):
        if 'package='+PKG in block and ('ty=2010' in block or 'type=2010' in block):
            m=re.search(r'mFrame=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]',block)
            if m:return list(map(int,m.groups()))
    raise AssertionError('Scale window absent')
def green(image,f):return sum(g>70 and g>r*1.4 and g>b*1.3 for r,g,b in image.crop(tuple(f)).getdata())
def hierarchy():
    result=shell('uiautomator','dump','/data/local/tmp/f01-ui.xml',timeout=60)
    assert 'dumped' in result,result
    return ET.fromstring(shell('cat','/data/local/tmp/f01-ui.xml'))
def click(n):
    l,t,r,b=map(int,re.findall(r'\d+',n.get('bounds')));shell('input','touchscreen','tap',(l+r)//2,(t+b)//2)
checks=[]
configure(delay=10000);f=frame();before=screen('f01-before-hide');assert green(before,f)>0
time.sleep(11);after=screen('f01-after-hide');assert green(after,f)==0
checks.append('Cold initialization without touch: visible then hidden; screenshots')
print(checks[-1],flush=True)
gesture=subprocess.Popen(PREFIX+['shell','input','touchscreen','swipe',str((f[0]+f[2])//2),str((f[1]+f[3])//2),str((f[0]+f[2])//2),str((f[1]+f[3])//2),'15000'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
time.sleep(11);held=screen('f01-during-hold');assert green(held,f)>0
response=gesture.communicate(timeout=30)[0].decode();assert gesture.returncode==0 and 'Error' not in response,response
restored=screen('f01-after-hold-release');assert green(restored,f)>0
checks.append('Stationary 15-second gesture: visible during hold after 11 s and after release, fresh delay')
print(checks[-1],flush=True)
# Real UI: disabled controls, enable, select nondefault options, restart persistence.
configure(on=False)
root=hierarchy();click(next(n for n in root.iter('node') if n.get('text')=='Работа приложения' and n.get('class')=='android.widget.Button'))
root=hierarchy();spinners=[n for n in root.iter('node') if n.get('class')=='android.widget.Spinner']
assert len(spinners)==2 and all(n.get('enabled')=='false' for n in spinners)
box=next(n for n in root.iter('node') if n.get('resource-id','').endswith(':id/CheckBoxAutoHide'))
click(box);root=hierarchy();spinners=[n for n in root.iter('node') if n.get('class')=='android.widget.Spinner']
assert len(spinners)==2 and all(n.get('enabled')=='true' for n in spinners)
click(spinners[0]);root=hierarchy();click(next(n for n in root.iter('node') if n.get('text')=='3 секунды'))
root=hierarchy();spinners=[n for n in root.iter('node') if n.get('class')=='android.widget.Spinner'];click(spinners[1])
root=hierarchy();click(next(n for n in root.iter('node') if n.get('text')=='Мгновенно'))
prefs=ET.fromstring(shell('cat',PREF));values={n.get('name'):n.get('value') for n in prefs}
assert values['Auto hide']=='true' and values['Auto hide delay ms']=='3000' and values['Auto hide fade ms']=='0'
stop();launch();prefs2=ET.fromstring(shell('cat',PREF));assert {n.get('name'):n.get('value') for n in prefs2}==values
checks.append('Real UI: disabled/enabled selectors, 3 s / instant selection, persistent restart')
print(checks[-1],flush=True)
screen('f01-settings')
crashes=shell('logcat','-d','-v','brief')
(OUT/'f01-runtime-logcat.txt').write_text(crashes,encoding='utf-8')
assert 'FATAL EXCEPTION' not in crashes and 'VerifyError' not in crashes
report={'environment':'F01_API18 ARM, Android 4.3/API18, disposable project data; not vehicle','checks':checks,
        'not_verified':['full style/side/percent UI matrix','external vendor event delivery','sleep/wake on vehicle']}
(OUT/'f01-emulator-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
