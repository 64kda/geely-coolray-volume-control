"""Extend fresh original patches; preserve all sixteen persisted Gravity indices."""
import re
import xml.etree.ElementTree as ET

def apply(src):
    h='Lrubberbigpepper/VolumeControl/redesign/DualScaleController;'
    service=src/'smali/rubberbigpepper/CommonCtrl/VolumeControlService.smali'
    s=service.read_text(encoding='utf-8')
    needle='    iput v3, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_nOrientation:I'
    assert s.count(needle)==1
    s=s.replace(needle,'    invoke-static {v3}, Lrubberbigpepper/VolumeControl/redesign/ScalePlacement;->geometry(I)I\n    move-result v3\n'+needle)
    start=s.index('.method private startInitialAutoHide()V');end=s.index('.end method',start)
    block=s[start:end].replace('    return-void',f'    invoke-static {{p0}}, {h}->attach(Landroid/content/Context;)V\n    return-void')
    s=s[:start]+block+s[end:]
    # Both reconfiguration and destruction must remove the extra window first.
    for signature in ['.method public onStart(','.method public onDestroy()V']:
        start=s.index(signature);end=s.index('.end method',start)
        block=s[start:end].replace('    .prologue',f'    .prologue\n    invoke-static {{}}, {h}->detach()V',1)
        s=s[:start]+block+s[end:]
    start=s.index('.method private ShowPercent(I)V');end=s.index('.end method',start)
    block=s[start:end].replace('    const/4 v8, 0x1',f'''    invoke-static {{p0}}, {h}->active(Landroid/content/Context;)Z
    move-result v0
    if-eqz v0, :dual_percent_original
    return-void
    :dual_percent_original
    const/4 v8, 0x1''',1)
    s=s[:start]+block+s[end:];service.write_text(s,encoding='utf-8')
    zoom=src/'smali/rubberbigpepper/CommonCtrl/ZoomView.smali'
    s=zoom.read_text(encoding='utf-8');start=s.index('.method public onTouchEvent(');end=s.index('.end method',start)
    block=s[start:end].replace('    .prologue',f'''    .prologue
    invoke-static {{p0, p1}}, {h}->touch(Lrubberbigpepper/CommonCtrl/ZoomView;Landroid/view/MotionEvent;)Z
    move-result v0
    if-eqz v0, :dual_original_touch
    const/4 v0, 0x1
    return v0
    :dual_original_touch''',1)
    s=s[:start]+block+s[end:]
    start=s.index('.method public guardResume()V');end=s.index('.end method',start)
    block=s[start:end].replace('    .locals 1',f'''    .locals 1
    invoke-static {{p0}}, {h}->owns(Lrubberbigpepper/CommonCtrl/ZoomView;)Z
    move-result v0
    if-eqz v0, :dual_guard_resume_original
    return-void
    :dual_guard_resume_original''',1)
    s=s[:start]+block+s[end:]
    start=s.index('.method public onWindowFocusChanged(Z)V');end=s.index('.end method',start)
    block=s[start:end].replace('    .locals 0',f'''    .locals 0
    if-nez p1, :dual_focus_ok
    invoke-static {{p0}}, {h}->interrupted(Lrubberbigpepper/CommonCtrl/ZoomView;)V
    :dual_focus_ok''',1)
    s=s[:start]+block+s[end:]
    start=s.index('.method protected onDetachedFromWindow()V');end=s.index('.end method',start)
    block=s[start:end].replace('    .locals 1',f'    .locals 1\n    invoke-static {{p0}}, {h}->removed(Lrubberbigpepper/CommonCtrl/ZoomView;)V',1)
    s=s[:start]+block+s[end:];zoom.write_text(s,encoding='utf-8')
    receiver=src/'smali/rubberbigpepper/CommonCtrl/VolumeControlService$VolumeChangeReceiver.smali'
    s=receiver.read_text(encoding='utf-8')
    old='invoke-virtual {v3, v4}, Lrubberbigpepper/CommonCtrl/ZoomView;->setProgress(I)V'
    assert s.count(old)==5
    s=s.replace(old,f'invoke-static {{v3, v4}}, {h}->external(Lrubberbigpepper/CommonCtrl/ZoomView;I)V')
    receiver.write_text(s,encoding='utf-8')
    for file in (src/'res').glob('values*/arrays.xml'):
        tree=ET.parse(file)
        for node in tree.getroot():
            if node.get('name')=='Gravity':
                assert len(node)==16
                for label in ['Только слева','Только справа','Слева и справа']:ET.SubElement(node,'item').text=label
        tree.write(file,encoding='utf-8',xml_declaration=True)
