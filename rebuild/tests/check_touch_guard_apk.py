"""Check the final DEX's opt-in, interception order, lifecycle and audio boundary."""
from pathlib import Path
import sys,zipfile
BASE=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX
with zipfile.ZipFile(sys.argv[1]) as apk:data=DEX(apk.read('classes.dex'))
classes={c.get_name():c for c in data.get_classes()}
Z='Lrubberbigpepper/CommonCtrl/ZoomView;'
S='Lrubberbigpepper/CommonCtrl/VolumeControlService;'
H='Lrubberbigpepper/VolumeControl/redesign/'
def method(owner,name,signature=None):
    return next(m for m in classes[owner].get_methods() if m.get_name()==name and (signature is None or m.get_descriptor().replace(' ','')==signature))
def code(owner,name,signature=None):return [(i.get_name(),i.get_output()) for i in method(owner,name,signature).get_instructions()]
def index(items,fragment):return next(n for n,(_,out) in enumerate(items) if fragment in out)
touch=code(Z,'onTouchEvent')
auto=index(touch,'AutoHideController;->touch(')
guard=index(touch,'TouchGuardController;->touch(')
legacy=index(touch,'->setProgress(I Z)V')
assert auto<guard<legacy
assert any(op=='return' for op,out in touch[guard:legacy]),'Guard must consume before legacy audio/EqMix'
assert any(op=='iput-wide' and '->m_nReactionDelay J' in out for op,out in touch[guard:legacy])
assert any('->setProgress(I Z)V' in out for _,out in code(Z,'guardWrite'))
assert any('->onLongClicked(' in out for _,out in code(Z,'guardTools'))
assert any('TouchGuardController;->changed(I)V' in out for _,out in code(Z,'setProgress','(IZ)V'))
for name in ['onDetachedFromWindow','onWindowFocusChanged']:
    assert any('->closeAutoHide()V' in out for _,out in code(Z,name))
assert any('TouchGuardController;->close()V' in out for _,out in code(Z,'closeAutoHide'))
assert any('->cancelGuard()V' in out for _,out in code(Z,'setMax'))
assert sum('TouchGuardController;-><init>' in i.get_output() for c in data.get_classes() for m in c.get_methods() for i in m.get_instructions())==1
assert sum('->startAutoHide()V' in out for m in classes[S].get_methods() for out in [i.get_output() for i in m.get_instructions()])==1
assert any('->uptimeMillis()J' in out for _,out in code(H+'TouchGuardController$1;','now'))
for name in ['TouchGuardController','TouchGuardState']:
    assert not any('->setStreamVolume(' in i.get_output() or '->setRingerMode(' in i.get_output() for m in classes[H+name+';'].get_methods() for i in m.get_instructions())
controller=[i.get_output() for m in classes[H+'TouchGuardController;'].get_methods() for i in m.get_instructions()]
snapshot=code(H+'TouchGuardController;','validSnapshot')
for call in ['->resolveStream()I','->getStreamVolume(I)I','->getStreamMaxVolume(I)I']:
    assert any(call in out for _,out in snapshot),call
assert any('->configure(I)V' in out for _,out in code(H+'TouchGuardController;','onSharedPreferenceChanged'))
assert any('->getHistoricalEventTime(' in out for out in controller)
assert any('->unregisterOnSharedPreferenceChangeListener(' in out for out in controller)
print('F-02 DEX PASS: F01 before consumed guard, legacy delay bypass, single opt-in, audio bridge, lifecycle/range/prefs, history, uptime')
