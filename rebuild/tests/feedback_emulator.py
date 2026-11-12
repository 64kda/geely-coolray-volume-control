"""Real API18 MotionEvents; exclusively the disposable F02_API18 emulator."""
from pathlib import Path
import subprocess,time,re,json,xml.etree.ElementTree as ET,hashlib
BASE=Path(__file__).resolve().parents[2]
ADB='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[ADB,'-s','emulator-5596']
PKG='rubberbigpepper.VolumeControl.redesign'
PREF='/data/data/'+PKG+'/shared_prefs/Common.xml'
OUT=BASE/'output'
def adb(*args,timeout=50):return subprocess.check_output(PREFIX+list(map(str,args)),stderr=subprocess.STDOUT,timeout=timeout)
def shell(*args,timeout=50):return adb('shell',*args,timeout=timeout).decode('utf-8',errors='replace').replace('\r','')
assert shell('getprop','ro.build.version.sdk').strip()=='18'
assert adb('emu','avd','name').decode().splitlines()[0]=='F02_API18'
assert shell('getprop','ro.kernel.qemu').strip()=='1'
APK=BASE/'output/VolumeControl-2.7.1-redesign15-guard-feedback-api18.apk'
assert 'Success' in adb('install','-r',APK,timeout=90).decode()
adb('push',BASE/'rebuild/touch-inject-dex.jar','/data/local/tmp/f02-gesture.jar')
shell('logcat','-c')
shell('input','keyevent','82')
def launch():
    result=shell('am','start','-W','-n',PKG+'/rubberbigpepper.VolumeControl.MainWindow')
    assert 'Error' not in result,result
def configure(on=True,hold=250,eq=False,auto=False,feedback=True,style=0):
    shell('am','force-stop',PKG)
    root=ET.Element('map')
    for key,value in {'Bar type':style,'Touch guard feedback':feedback,'Touch guard enabled':on,'Touch guard hold ms':hold,'Reaction delay':True,
        'Auto hide':auto,'Auto hide delay ms':1000,'Auto hide fade ms':0,'Enable EqMix':eq,
        'Stream type':3,'Gravity':11,'Size':40,'Length':100,'Main color':-14756000,'Background color':-531278507}.items():
        ET.SubElement(root,'boolean' if isinstance(value,bool) else 'int',name=key,value=str(value).lower())
    local=OUT/'f02-runtime-prefs.xml';ET.ElementTree(root).write(local,encoding='utf-8',xml_declaration=True)
    adb('push',local,'/data/local/tmp/f02-prefs.xml')
    shell('sh','-c',"'cat /data/local/tmp/f02-prefs.xml > "+PREF+"'")
    launch()
def frame():
    for block in shell('dumpsys','window','windows').split('Window #'):
        if 'package='+PKG in block and ('ty=2010' in block or 'type=2010' in block):
            m=re.search(r'mFrame=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]',block)
            if m:return list(map(int,m.groups()))
    raise AssertionError('Floating scale absent')
def level():
    text=shell('dumpsys','audio');m=re.search(r'- STREAM_MUSIC:.*?Current: [^:]+: (\d+)',text,re.S)
    assert m,text
    return int(m[1])
def gesture(hold,delta=0,cancel=False):
    f=frame();x=(f[0]+f[2])//2;y=(f[1]+f[3])//2
    args=['CLASSPATH=/data/local/tmp/f02-gesture.jar','app_process','/system/bin','TouchGesture',x,y,hold,delta]
    if cancel:args+=['cancel']
    response=shell(*args);assert 'TouchGesture delivered' in response,response
def screenshot(name):
    shell('screencap','-p','/data/local/tmp/f02-screen.png');adb('pull','/data/local/tmp/f02-screen.png',OUT/(name+'.png'))
def hierarchy():
    shell('uiautomator','dump','/data/local/tmp/f02-ui.xml',timeout=60)
    return ET.fromstring(shell('cat','/data/local/tmp/f02-ui.xml'))
def click(node):
    l,t,r,b=map(int,re.findall(r'\d+',node.get('bounds')));shell('input','tap',(l+r)//2,(t+b)//2)
def app_page():click(next(n for n in hierarchy().iter('node') if n.get('text')=='Работа приложения'))
checks=[]
launch()
for hold in [250,500,1000]:
    configure(hold=hold);before=level()
    gesture(20);assert level()==before
    gesture(20,-80);assert level()==before
    gesture(hold+150);assert level()==before
    gesture(hold+150,0,True);assert level()==before
    gesture(hold+150,60);after=level();assert after<before,(hold,before,after)
    gesture(hold+150,-60);assert level()>after
    checks.append('hold '+str(hold)+': tap/fast swipe/stationary/cancel preserve music; confirmed movement works both ways')
    print(checks[-1],flush=True)
configure(on=False);before=level();gesture(20,0);assert level()!=before
checks.append('Guard off: first touch uses absolute level despite old Reaction delay=true')
configure(eq=True);before=level();gesture(3400);assert level()==before
checks.append('EqMix stationary hold: audio level unchanged')
screenshot('feedback-eqmix')
configure(auto=True);time.sleep(1.3);before=level();gesture(500,60);assert level()<before
checks.append('Hidden scale: first held gesture regulates without second tap')
configure();
before=level();app_page();tree=hierarchy()
assert not any('Задержка перед регулировкой' in n.get('text','') for n in tree.iter('node'))
assert any(n.get('resource-id','').endswith(':id/CheckBoxAutoHide') for n in tree.iter('node'))
guard=next(n for n in tree.iter('node') if n.get('text')=='Защита от случайного касания')
click(guard);tree=hierarchy();spinners=[n for n in tree.iter('node') if n.get('class')=='android.widget.Spinner'];assert all(n.get('enabled')=='false' for n in spinners)
click(next(n for n in tree.iter('node') if n.get('text')=='Защита от случайного касания'));tree=hierarchy()
spinner=next(n for n in tree.iter('node') if n.get('class')=='android.widget.Spinner');assert spinner.get('enabled')=='true';click(spinner)
click(next(n for n in hierarchy().iter('node') if n.get('text')=='500 мс'))
values={n.get('name'):n.get('value') for n in ET.fromstring(shell('cat',PREF))};assert values['Touch guard hold ms']=='500' and values['Touch guard enabled']=='true'
shell('am','force-stop',PKG);launch();app_page();assert level()==before
assert any(n.get('text')=='500 мс' for n in hierarchy().iter('node'))
checks.append('Real settings: old delay removed, Auto hide retained, enabled state, 500ms persistence/restart without volume change')
tree=hierarchy();box=next(n for n in tree.iter('node') if n.get('text')=='Подсветка при удержании');assert box.get('enabled')=='true';click(box)
click(next(n for n in hierarchy().iter('node') if n.get('text')=='Защита от случайного касания'))
box=next(n for n in hierarchy().iter('node') if n.get('text')=='Подсветка при удержании');assert box.get('enabled')=='false' and box.get('checked')=='false'
click(next(n for n in hierarchy().iter('node') if n.get('text')=='Защита от случайного касания'))
box=next(n for n in hierarchy().iter('node') if n.get('text')=='Подсветка при удержании');assert box.get('enabled')=='true' and box.get('checked')=='false'
screenshot('feedback-settings')
for feedback in [True,False]:
    configure(feedback=feedback);before=level();f=frame();x=(f[0]+f[2])//2;y=(f[1]+f[3])//2
    cmd=PREFIX+['shell','CLASSPATH=/data/local/tmp/f02-gesture.jar','app_process','/system/bin','TouchGesture',str(x),str(y),'4000','0']
    proc=subprocess.Popen(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);time.sleep(1.2);screenshot('feedback-held-'+str(feedback));response=proc.communicate(timeout=15)[0];assert proc.returncode==0 and b'TouchGesture delivered' in response,response
    assert level()==before
    gesture(700,60);assert level()<before
checks.append('Feedback toggle: dependency, retained false after guard off/on; held screenshots on/off; relative adjustment works in both modes')
logs=shell('logcat','-d','-v','brief');(OUT/'feedback-runtime-logcat.txt').write_text(logs,encoding='utf-8')
assert 'FATAL EXCEPTION' not in logs and 'VerifyError' not in logs
result={'apk_sha256':hashlib.sha256(APK.read_bytes()).hexdigest(),'environment':'F02_API18 ARM Android4.3, 1920x720; isolated emulator','checks':checks,'not_verified':['physical head unit','real multi-pointer injection (adapter tests only)','precise T-1/T/T+1 on device (state tests only)','all orientations and channel ranges on device']}
(OUT/'feedback-emulator-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
