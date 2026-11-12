"""Verify the shipped DEX and compatibility contract, not only helper sources."""
from pathlib import Path
import sys, zipfile, re, xml.etree.ElementTree as ET
BASE=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX
with zipfile.ZipFile(sys.argv[1]) as apk:data=DEX(apk.read('classes.dex'))
classes={c.get_name():c for c in data.get_classes()}
def methods(owner):return classes[owner].get_methods()
def code(method):return [i.get_output() for i in method.get_instructions()]
service='Lrubberbigpepper/CommonCtrl/VolumeControlService;'
show=next(m for m in methods(service) if m.get_name()=='ShowPercent')
ops=code(show)
assert sum('PercentPlacement;->add(' in o for o in ops)==1
assert sum('PercentPlacement;->update(' in o for o in ops)==1
assert sum('->postDelayed(' in o for o in ops)==1 and any('1000' in o for o in ops),'Existing percentage timeout lost'
assert not any('->setStreamVolume(' in o or '->SetVolume(' in o for o in ops),'Percentage writes audio'
assert any('m_cRunnablePercentOff' in o for o in ops),'Existing timer owner lost'
for name in ['IndicatorGeometry','IndicatorLayout','PercentPlacement']:
    owner='Lrubberbigpepper/VolumeControl/redesign/'+name+';'
    assert owner in classes
    assert not any('->startService(' in o or '->postDelayed(' in o for m in methods(owner) for o in code(m)),'Feature added service or timer'
main=(BASE/'rebuild/source/smali/rubberbigpepper/VolumeControl/MainWindow.smali').read_text(encoding='utf-8')
assert 'SharedPreferences$Editor;->clear()' not in main
saved=re.search(r'\.method private SavePrefs.*?\.end method',main,re.S)
assert saved is not None
size=saved.group().split('const-string v1, "Size"',1)[1].split('    .line 293')[0]
assert '->getProgress()I' in size and '->putInt(' in size and 'add-int' not in size,'Saved width transform changed'
layout=ET.parse(BASE/'rebuild/source/res/layout/main.xml')
android='{http://schemas.android.com/apk/res/android}'
width=next(n for n in layout.iter('SeekBar') if n.get(android+'id')=='@id/SeekBarSize')
length=next(n for n in layout.iter('SeekBar') if n.get(android+'id')=='@id/SeekBarLeng')
assert width.get(android+'max')=='90' and length.get(android+'max')=='130'
arrays=ET.parse(BASE/'rebuild/source/res/values/arrays.xml')
assert len(next(n for n in arrays.getroot() if n.get('name')=='TypeBar'))==3
print('Indicator APK PASS: single original percentage window/timer; new hooks packaged; no new service; width max90 and direct saved progress; length max130; third style')
