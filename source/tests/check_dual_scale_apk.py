"""Final integration boundaries; does not replace device gesture/window tests."""
from pathlib import Path
import sys,zipfile,xml.etree.ElementTree as ET
BASE=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX
with zipfile.ZipFile(sys.argv[1]) as z:
    raw=z.read('classes.dex');assert raw[:8]==b'dex\n035\x00';dex=DEX(raw)
classes={c.get_name():c for c in dex.get_classes()}
H='Lrubberbigpepper/VolumeControl/redesign/'
S='Lrubberbigpepper/CommonCtrl/VolumeControlService;'
Z='Lrubberbigpepper/CommonCtrl/ZoomView;'
def code(owner,name):
    return [i.get_output() for m in classes[owner].get_methods() if m.get_name()==name for i in m.get_instructions()]
for c in ['DualScaleController','DualScaleState','ScalePlacement','VolumeConfirmation','OverlayVisibility']:
    assert H+c+';' in classes
assert any('->attach(' in i for i in code(S,'startInitialAutoHide'))
for m in ['onStart','onDestroy']:assert any('->detach()' in i for i in code(S,m))
touch=code(Z,'onTouchEvent')
assert next(n for n,i in enumerate(touch) if 'DualScaleController;->touch(' in i)<next(n for n,i in enumerate(touch) if 'AutoHideController;->touch(' in i)
controller=[i.get_output() for m in classes[H+'DualScaleController;'].get_methods() for i in m.get_instructions()]
assert not any('->setStreamVolume(' in i or '->startService(' in i for i in controller)
assert sum('->guardWrite(' in i for i in controller)==1
assert any('->registerReceiver(' in i for i in controller)
assert any('->unregisterReceiver(' in i for i in controller)
assert any('->removeCallbacksAndMessages(' in i for i in controller)
refresh=code(H+'DualScaleController;','refresh')
assert any('VolumeConfirmation;->observe(' in i for i in refresh)
assert not any('->guardWrite(' in i for i in refresh)
receiver=code(S[:-1]+'$VolumeChangeReceiver;','onReceive')
assert sum('DualScaleController;->external(' in i for i in receiver)==5
arrays=ET.parse(BASE/'rebuild/source/res/values/arrays.xml')
node=next(n for n in arrays.getroot() if n.get('name')=='Gravity')
assert len(node)==19 and [x.text for x in list(node)[16:]]==['Только слева','Только справа','Слева и справа']
original=ET.parse(BASE/'design/localization/res/values-ru/arrays.xml')
old=next(n for n in original.getroot() if n.get('name')=='Gravity')
assert len(old)==16
assert [x.text for x in list(node)[:16]]==[x.text for x in old]
assert any("->owns(" in i for i in code(Z,"guardResume"))
assert any("->removed(" in i for i in code(Z,"onDetachedFromWindow"))
assert any("->interrupted(" in i for i in code(Z,"onWindowFocusChanged"))
print('Dual-scale APK PASS: packaged DEX035, 19 positions, lifecycle/input/receiver hooks, single existing audio bridge, read-only refresh')

visibility=code(H+"OverlayVisibility;","apply")
assert any("->updateViewLayout(" in i for i in visibility)
assert any("->alpha F" in i for i in visibility)
