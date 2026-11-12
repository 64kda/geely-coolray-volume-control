"""Verify marker lifecycle hooks and unchanged audio-writing methods in final DEX."""
from pathlib import Path
import sys, zipfile
BASE=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

def dex(file):
    with zipfile.ZipFile(file) as apk:return DEX(apk.read('classes.dex'))
def cls(data,name):return next(c for c in data.get_classes() if c.get_name()==name)
def method(owner,name,descriptor=None):
    return next(m for m in owner.get_methods() if m.get_name()==name and (descriptor is None or m.get_descriptor().replace(' ','')==descriptor))
def code(method):
    # Existing localization predates this fix and does not affect audio control.
    return [(i.get_name(),i.get_output().replace('%.0f дБ','%.0f dB').replace('%.0f Гц','%.0f Hz')) for i in method.get_instructions()]
def calls(owner,name):return [out for op,out in code(method(owner,name)) if op.startswith('invoke-')]

data=dex(Path(sys.argv[1]));original=dex(BASE/'app-v2.7.1.apk')
zoom=cls(data,'Lrubberbigpepper/CommonCtrl/ZoomView;')
serviceName='Lrubberbigpepper/CommonCtrl/VolumeControlService;'
service=cls(data,serviceName)
for name in ['SetStreamVolume','onValueChanged']:
    assert code(method(service,name))==code(method(cls(original,serviceName),name)),name+' audio code changed'
receiver=serviceName[:-1]+'$VolumeChangeReceiver;'
# The feature adds a display-only notification after each original setProgress.
# Compare the assembly outside those exact insertions, preserving stream decisions.
import re
current=(BASE/'rebuild/source/smali/rubberbigpepper/CommonCtrl/VolumeControlService$VolumeChangeReceiver.smali').read_text(encoding='utf-8')
baseline=(BASE/'rebuild/original/smali/rubberbigpepper/CommonCtrl/VolumeControlService$VolumeChangeReceiver.smali').read_text(encoding='utf-8')
insertion='''
    iget-object v3, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService$VolumeChangeReceiver;->this$0:Lrubberbigpepper/CommonCtrl/VolumeControlService;
    invoke-virtual {v3}, Lrubberbigpepper/CommonCtrl/VolumeControlService;->showExternalPercent()V'''
assert current.count(insertion)==5 and current.replace(insertion,'')==baseline,'System receiver audio path changed'
service_path='smali/rubberbigpepper/CommonCtrl/VolumeControlService.smali'
before=(BASE/'rebuild/original'/service_path).read_text(encoding='utf-8')
after=(BASE/'rebuild/source'/service_path).read_text(encoding='utf-8')
def audio_portion(text):
    block=re.search(r'\.method private SetVolume\(I\)V.*?\.end method',text,re.S).group()
    first=block.index('    .line 541') if '    .line 541' in block else block.index('    :cond_4\n')
    last=block.index('    .line 576',first)
    block=block[:first]+block[last:]
    block=re.sub(r'    \.line 629.*?(?=    \.line 633)', '', block, flags=re.S)
    return block.replace('''
    invoke-static {p0, v2}, Lrubberbigpepper/VolumeControl/redesign/PercentPlacement;->systemNotification(Landroid/content/Context;Z)Z
    move-result v2''','')
assert audio_portion(before)==audio_portion(after),'SetVolume outside percentage presentation changed'
progress=code(method(zoom,'setProgress','(IZ)V'))
write=next(i for i,(op,out) in enumerate(progress) if op=='iput' and '->mProgress I' in out)
change=next(i for i,(op,out) in enumerate(progress) if 'MarkerController;->changed(I)V' in out)
refresh=next(i for i,(op,out) in enumerate(progress) if '->refreshProgress()V' in out)
assert write<change<refresh,'Actual change must reach marker before existing invalidate'
assert any('MarkerController;-><init>' in out for out in calls(zoom,'onAttachedToWindow'))
assert any('MarkerController;->close()V' in out for out in calls(zoom,'onDetachedFromWindow'))
assert any('MarkerController;->close()V' in out for out in calls(zoom,'resetMarkerIdle'))
assert sum('->enableMarkerAutoHide()V' in out for m in service.get_methods() for op,out in code(m))==1,'Marker scope must have one opt-in'
assert any('->resetMarkerIdle()V' in out for out in calls(service,'Initialize'))
renderer=cls(data,'Lrubberbigpepper/VolumeControl/redesign/ScaleRenderer;')
assert any('->isMarkerVisible()Z' in out for out in calls(renderer,'draw'))
for name in ['MarkerVisibility','MarkerController']:
    component=cls(data,'Lrubberbigpepper/VolumeControl/redesign/'+name+';')
    assert not any('Landroid/media/' in out for m in component.get_methods() for op,out in code(m))
print('Marker APK PASS: lifecycle and renderer; audio methods unchanged outside display-only percentage hooks')
