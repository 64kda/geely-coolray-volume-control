"""Inspect final DEX: settings refresh must not restore or set stream levels."""
from pathlib import Path
import sys,zipfile
BASE=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

with zipfile.ZipFile(sys.argv[1]) as apk:data=DEX(apk.read('classes.dex'))
classes={c.get_name():c for c in data.get_classes()}
service=classes['Lrubberbigpepper/CommonCtrl/VolumeControlService;']
def method(owner,name):return next(m for m in owner.get_methods() if m.get_name()==name)
def calls(method):return [i.get_output() for i in method.get_instructions() if i.get_name().startswith('invoke-')]
init=calls(method(service,'Initialize'))
assert not any('->SetVolume(' in call for call in init),'Initialize writes volume from a settings refresh'
assert not any('->setStreamVolume(' in call for call in calls(method(service,'ReadEqMixState'))),'Settings refresh restores stale mixer levels'
assert any('ScaleState;->sync(' in call for call in init),'Read-only scale initialization missing'
helper=classes['Lrubberbigpepper/VolumeControl/redesign/ScaleState;']
all_calls=[call for m in helper.get_methods() for call in calls(m)]
assert not any('->setStreamVolume(' in call or '->setRingerMode(' in call for call in all_calls)
assert any('->getStreamVolume(' in call for call in all_calls)
assert any('->getStreamMaxVolume(' in call for call in all_calls)
events=calls(method(service,'onValueChanged'))
assert any('->SetVolume(' in call for call in events),'Explicit overlay adjustment disappeared'
assert any('->setStreamVolume(' in call for call in events),'Explicit mixer adjustment disappeared'
print('Settings volume PASS: initialization reads current levels; no stale mixer restore; explicit adjustments retained')
