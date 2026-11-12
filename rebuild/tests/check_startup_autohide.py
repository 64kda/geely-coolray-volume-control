"""F-01 final DEX: single timer ownership and audio preservation."""
from pathlib import Path
import sys, zipfile
BASE=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX
with zipfile.ZipFile(sys.argv[1]) as z:
    raw=z.read('classes.dex')
    assert raw[:8]==b'dex\n035\x00'
    data=DEX(raw)
classes={c.get_name():c for c in data.get_classes()}
def method(owner,name):return next(m for m in classes[owner].get_methods() if m.get_name()==name)
def code(owner,name):return [(i.get_name(),i.get_output()) for i in method(owner,name).get_instructions()]
def has(owner,name,target):return any(target in out for _,out in code(owner,name))
service='Lrubberbigpepper/CommonCtrl/VolumeControlService;'
zoom='Lrubberbigpepper/CommonCtrl/ZoomView;'
helper='Lrubberbigpepper/VolumeControl/redesign/'
init=code(service,'Initialize')
assert sum('->startInitialAutoHide()V' in out for _,out in init)==1
hook=next(i for i,(_,out) in enumerate(init) if '->startInitialAutoHide()V' in out)
assert init[hook+1][0]=='return-void'
assert not any('->SetVolume(' in out or '->setStreamVolume(' in out for _,out in init)
assert has(service,'startInitialAutoHide','->startAutoHide()V')
assert has(zoom,'startAutoHide','AutoHideController;-><init>')
assert has(zoom,'onTouchEvent','AutoHideController;->touch(')
assert has(zoom,'setProgress','AutoHideController;->changed()V')
assert has(zoom,'onDetachedFromWindow','->closeAutoHide()V')
assert has(service,'onDestroy','->closeAutoHide()V')
assert code(service[:-1]+'$1;','run')==[('return-void','')]
for m in classes[service].get_methods():
    assert not any('->postDelayed(' in i.get_output() for i in m.get_instructions() if m.get_name() in ['SetVolume','startInitialAutoHide'])
assert has(helper+'AutoHideController$1;','now','->elapsedRealtime()J')
assert has(helper+'AutoHideController$3;','visibility','->setAlpha(F)V')
for name in ['AutoHideController','AutoHideState']:
    owner=helper+name+';'
    assert owner in classes
    assert not any('Landroid/media/' in i.get_output() for m in classes[owner].get_methods() for i in m.get_instructions())
with zipfile.ZipFile(BASE/'output/VolumeControl-2.7.1-redesign12-startup-autohide-api18.apk') as z:old=DEX(z.read('classes.dex'))
oldClasses={c.get_name():c for c in old.get_classes()}
for name in ['SetStreamVolume','onValueChanged']:
    prior=next(m for m in oldClasses[service].get_methods() if m.get_name()==name)
    assert code(service,name)==[(i.get_name(),i.get_output()) for i in prior.get_instructions()]
print('F-01 DEX PASS: startup/touch/change/detach/destroy hooks, inert legacy fade, elapsed clock, no audio writes, DEX035')
