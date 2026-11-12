"""API18 navigation and original-control preservation checks on the test package."""
from pathlib import Path
import subprocess, re, json, xml.etree.ElementTree as ET

BASE=Path(__file__).resolve().parents[2]
OUT=BASE/'output'
ADB='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[ADB,'-s','emulator-5582']
PACKAGE='rubberbigpepper.VolumeControl.redesign'
def adb(*args):return subprocess.check_output(PREFIX+list(args),stderr=subprocess.STDOUT)
def shell(*args):return adb('shell',*args).decode('utf-8',errors='replace')
assert shell('getprop','ro.build.version.sdk').strip()=='18'

def dump(name):
    shell('uiautomator','dump','/data/local/tmp/volume-redesign-current.xml')
    file=OUT/(name+'.xml');adb('pull','/data/local/tmp/volume-redesign-current.xml',str(file))
    root=ET.parse(file)
    if any(n.get('package')=='android' and 'закрыл' in n.get('text','') for n in root.iter('node')):
        raise AssertionError('System crash dialog')
    return root
def point(node):
    x,y,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')));return ((x+x2)//2,(y+y2)//2)
def click(root,text,top=False):
    nodes=[n for n in root.iter('node') if n.get('text')==text and n.get('clickable')=='true']
    if top:nodes=[n for n in nodes if point(n)[1]<200]
    assert nodes,'Missing action '+text
    shell('input','tap',*map(str,point(nodes[0])))
def screenshot(name):
    shell('screencap','-p','/data/local/tmp/volume-redesign-screen.png')
    adb('pull','/data/local/tmp/volume-redesign-screen.png',str(OUT/(name+'.png')))

tabs=['Управление','Шкала','Звук','Эквалайзер','Работа приложения','О приложении']
markers=['SpinnerStreamType','SeekBarSize','CheckBoxShowNotification','CheckBoxEnableEqMix','CheckBoxDelay','ButtonDonation']
shell('input','keyevent','82')
shell('am','start','-W','-n',PACKAGE+'/rubberbigpepper.VolumeControl.MainWindow')
seen=set();root=dump('runtime-start')
for i,title in enumerate(tabs):
    click(root,title,top=True);root=dump('runtime-page-'+str(i))
    if not any(n.get('resource-id','').endswith(':id/'+markers[i]) for n in root.iter('node')):
        click(root,title,top=True);root=dump('runtime-page-'+str(i))
    assert any(n.get('resource-id','').endswith(':id/'+markers[i]) for n in root.iter('node')),'Tab did not open: '+title
    screenshot('runtime-page-'+str(i))
    assert any(n.get('package')==PACKAGE for n in root.iter('node')),title
    for scroll in range(5):
        current={n.get('resource-id','').split(':id/')[-1] for n in root.iter('node') if ':id/' in n.get('resource-id','')}
        previous=len(seen);seen.update(current)
        if i not in [1,3]:break
        # Scroll over the label gutter, never over an actual slider or selector.
        response=shell('input','touchscreen','swipe','40','620','40','240')
        assert 'Error:' not in response,response
        root=dump('runtime-page-'+str(i)+'-scroll-'+str(scroll))
        if len(seen)==previous and scroll>0:break
    print('Visited tab '+str(i+1)+': '+title,flush=True)
public={e.attrib['id'].lower():e.attrib['name'] for e in ET.parse(BASE/'analysis/resources-resolved.xml').getroot()}
ns='{http://schemas.android.com/apk/res/android}'
original=[e for e in ET.parse(BASE/'analysis/res/layout/main.xml').getroot().iter() if e.tag in ['SeekBar','ImageView','Spinner','EditText','CheckBox']]
expected={public[e.get(ns+'id').replace('@','0x').lower()] for e in original}
missing=sorted(expected-seen)
result={'api':18,'tabs_visited':6,'original_controls':23,'controls_observed':len(expected&seen),'missing_controls':missing}
(OUT/'runtime-smoke.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
assert not missing,'Missing original controls: '+str(missing)
click(root,'Управление',top=True);root=dump('runtime-home');screenshot('runtime-home')
click(root,'Микшер');root=dump('runtime-mixer');screenshot('runtime-mixer')
assert any(n.get('text')=='Микшер' for n in root.iter('node')),'Mixer did not open'
windows=shell('dumpsys','window','windows')
frames=[]
for block in windows.split('Window #'):
    if 'package='+PACKAGE+' appop=SYSTEM_ALERT_WINDOW' in block:
        m=re.search(r'mFrame=\[(\d+),(\d+)\]\[(\d+),(\d+)\]',block)
        if m:
            frame=list(map(int,m.groups()))
            if frame[2]-frame[0]>100 and frame[3]-frame[1]>100:frames.append(frame)
assert frames,'Mixer overlay window was not created'
result['mixer_overlay_opened']=True
(OUT/'runtime-smoke.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False),flush=True)
