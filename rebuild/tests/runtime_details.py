from pathlib import Path
import subprocess,xml.etree.ElementTree as ET,re,json
BASE=Path(__file__).resolve().parents[2];OUT=BASE/'output'
ADB='C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk/platform-tools/adb.exe';PKG='rubberbigpepper.VolumeControl.redesign'
def adb(*args):return subprocess.check_output([ADB,'-s','emulator-5582']+list(args),stderr=subprocess.STDOUT)
def shell(*args):return adb('shell',*args).decode('utf-8',errors='replace')
def dump(name):
    shell('uiautomator','dump','/data/local/tmp/volume-details.xml');file=OUT/(name+'.xml');adb('pull','/data/local/tmp/volume-details.xml',str(file));return ET.parse(file)
def point(n):
    x,y,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));return str((x+x2)//2),str((y+y2)//2)
def click(root,text=None,id=None):
    nodes=[n for n in root.iter('node') if (text is None or n.get('text')==text) and (id is None or n.get('resource-id','').endswith(':id/'+id)) and n.get('clickable')=='true']
    assert nodes,text or id
    node=nodes[0]
    if node.get('class')=='android.widget.CheckBox':
        x,y,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
        shell('input','touchscreen','tap',str(x2-30),str((y+y2)//2))
    else:shell('input','touchscreen','tap',*point(node))
def prefs():
    return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(shell('cat','/data/data/'+PKG+'/shared_prefs/Common.xml'))}
def screen(name):
    shell('screencap','-p','/data/local/tmp/volume-details.png');adb('pull','/data/local/tmp/volume-details.png',str(OUT/(name+'.png')))
shell('input','keyevent','82');shell('am','force-stop',PKG);shell('am','start','-W','-n',PKG+'/rubberbigpepper.VolumeControl.MainWindow')
root=dump('details-home');before=prefs()
click(root,'Работа приложения');root=dump('details-work')
node=next(n for n in root.iter('node') if n.get('resource-id','').endswith(':id/CheckBoxAutoHide'))
initial=node.get('checked');click(root,id='CheckBoxAutoHide')
root=dump('details-work-changed')
shell('am','force-stop',PKG);shell('am','start','-W','-n',PKG+'/rubberbigpepper.VolumeControl.MainWindow')
root=dump('details-restarted');click(root,'Работа приложения');root=dump('details-work-restored')
node=next(n for n in root.iter('node') if n.get('resource-id','').endswith(':id/CheckBoxAutoHide'))
assert node.get('checked')!=initial,'Autohide did not persist after process restart'
click(root,id='CheckBoxAutoHide');root=dump('details-after-restore')
assert prefs()['Auto hide']==before['Auto hide'],'Original autohide value was not restored'
click(root,'Шкала');root=dump('details-scale');click(root,id='ImageViewColor');root=dump('details-color')
texts={n.get('text') for n in root.iter('node')}
assert {'Красный','Зелёный','Синий','Применить','Отмена'}<=texts,'RGB dialog translation incomplete'
screen('runtime-color');click(root,'Цветовой круг');root=dump('details-color-circle')
assert any(n.get('text')=='Выбор цвета' for n in root.iter('node'))
shell('input','keyevent','4');root=dump('details-color-return');click(root,'Отмена');root=dump('details-scale-return')
assert prefs()['Main color']==before['Main color'],'Cancel changed the selected color'
for _ in range(5):
    if any(n.get('resource-id','').endswith(':id/SpinnerType') for n in root.iter('node')):break
    shell('input','touchscreen','swipe','40','620','40','240');root=dump('details-scale-bottom')
click(root,id='SpinnerType');root=dump('details-type-options')
texts={n.get('text') for n in root.iter('node')}
assert {'Сплошная полоса','Столбики уровня'}<=texts,'Original bar types missing'
original_type=int(before['Bar type']);click(root,'Столбики уровня' if original_type==0 else 'Сплошная полоса');root=dump('details-type-changed');screen('runtime-alternate-scale')
click(root,id='SpinnerType');root=dump('details-type-restore');click(root,'Сплошная полоса' if original_type==0 else 'Столбики уровня');root=dump('details-type-restored')
assert prefs()['Bar type']==before['Bar type']
shell('am','startservice','-n',PKG+'/rubberbigpepper.CommonCtrl.VolumeControlService','-a','local.volumecontrol.redesign.EQUALIZER')
root=dump('details-equalizer');screen('runtime-equalizer')
windows=shell('dumpsys','window','windows');frames=[]
for block in windows.split('Window #'):
    if 'package='+PKG+' appop=SYSTEM_ALERT_WINDOW' in block:
        m=re.search(r'mFrame=\[(\d+),(\d+)\]\[(\d+),(\d+)\]',block)
        if m:
            frame=list(map(int,m.groups()))
            if frame[2]-frame[0]>100 and frame[3]-frame[1]>100:frames.append(frame)
assert frames,'Equalizer overlay did not open'
x,y,x2,y2=frames[0];shell('input','touchscreen','tap',str(x2-20),str(y+20));dump('details-closed')
after=prefs();keys=['Gravity','Size','Length','Main color','Background color','Active color','Inactive color','Bar type','Auto hide','Stream type']
assert {k:before[k] for k in keys}=={k:after[k] for k in keys},'Read-only navigation changed unrelated settings'
result={'api':18,'autohide_persistence_after_process_restart':'passed','rgb_and_circle_dialogs':'passed','cancel_preserves_color':'passed','both_bar_types_selectable':'passed','equalizer_overlay_opens':'passed','unrelated_settings_unchanged':'passed','tests_restored_modified_values':True}
(OUT/'runtime-details.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result),flush=True)
