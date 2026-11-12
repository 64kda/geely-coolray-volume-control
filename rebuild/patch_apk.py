from pathlib import Path
import shutil
import re
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parent
SRC=ROOT/'source'
ANDROID='{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android',ANDROID[1:-1])
backup=ROOT/'original';backup.mkdir(exist_ok=True)

def original(relative):
    dest=backup/relative;file=SRC/relative
    if not dest.exists():dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(file,dest)
    return dest.read_text(encoding='utf-8')
def save(relative,text):(SRC/relative).write_text(text,encoding='utf-8')

main='smali/rubberbigpepper/VolumeControl/MainWindow.smali'
s=original(main)
s=s.replace('.implements Landroid/view/View$OnClickListener;',
    '.implements Landroid/view/View$OnClickListener;\n.implements Lrubberbigpepper/VolumeControl/redesign/ResetHost;',1)
# Reset callbacks must not save intermediate controls or start the audio service.
for signature in ['onCheckedChanged(Landroid/widget/CompoundButton;Z)V',
        'onItemSelected(Landroid/widget/AdapterView;Landroid/view/View;IJ)V',
        'onProgressChanged(Landroid/widget/SeekBar;IZ)V']:
    begin=s.index('.method public '+signature);end=s.index('.end method',begin)
    block=s[begin:end]
    block=re.sub(r'\.locals 0\b','.locals 1',block,count=1)
    block=block.replace('    .prologue','''    .prologue
    invoke-static {}, Lrubberbigpepper/VolumeControl/redesign/MainUi;->isResetting()Z
    move-result v0
    if-eqz v0, :reset_continue
    return-void
    :reset_continue''',1)
    s=s[:begin]+block+s[end:]
s+='''
.method public setResetColors(IIII)V
    .locals 0
    iput p1, p0, Lrubberbigpepper/VolumeControl/MainWindow;->m_nColor:I
    iput p2, p0, Lrubberbigpepper/VolumeControl/MainWindow;->m_nBGColor:I
    iput p3, p0, Lrubberbigpepper/VolumeControl/MainWindow;->m_nSelColor:I
    iput p4, p0, Lrubberbigpepper/VolumeControl/MainWindow;->m_nBackColor:I
    return-void
.end method
'''
# The launcher must never depend on switching Free/Pro aliases.
s=re.sub(r'\.method protected SetAppIcon\(\)V.*?\.end method',
    '.method protected SetAppIcon()V\n    .locals 0\n    return-void\n.end method',s,count=1,flags=re.S)
# Keep other subsystems' saved settings when the settings form writes its keys.
s=s.replace('    invoke-interface {v0}, Landroid/content/SharedPreferences$Editor;->clear()Landroid/content/SharedPreferences$Editor;','    # Keep saved equalizer and mixer keys.')
start=s.index('.method public onCreate(Landroid/os/Bundle;)V')
end=s.index('.end method',start)
method=s[start:end]
needle='    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V'
method=method.replace(needle,needle+'\n\n    invoke-static {p0}, Lrubberbigpepper/VolumeControl/redesign/MainUi;->prepare(Landroid/app/Activity;)V',1)
method=method.replace('    return-void','    invoke-static {p0}, Lrubberbigpepper/VolumeControl/redesign/MainUi;->install(Landroid/app/Activity;)V\n\n    return-void',1)
s=s[:start]+method+s[end:];save(main,s)

zoom='smali/rubberbigpepper/CommonCtrl/ZoomView.smali'
s=original(zoom)
s=s.replace('# instance fields','''# instance fields
.field private m_markerAutoHide:Z
.field private m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
''',1)
needle='    iput p1, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->mProgress:I'
assert s.count(needle)==1,'Progress-change hook must have one owner'
s=s.replace(needle,needle+'''
    iget-object v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    if-eqz v0, :marker_change_complete
    invoke-virtual {v0, p1}, Lrubberbigpepper/VolumeControl/redesign/MarkerController;->changed(I)V
    :marker_change_complete
''',1)
s+='''
.method public enableMarkerAutoHide()V
    .locals 1
    const/4 v0, 0x1
    iput-boolean v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_markerAutoHide:Z
    return-void
.end method

.method public isMarkerVisible()Z
    .locals 1
    iget-boolean v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_markerAutoHide:Z
    if-eqz v0, :marker_unmanaged
    iget-object v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    if-eqz v0, :marker_idle
    invoke-virtual {v0}, Lrubberbigpepper/VolumeControl/redesign/MarkerController;->isVisible()Z
    move-result v0
    return v0
    :marker_idle
    const/4 v0, 0x0
    return v0
    :marker_unmanaged
    const/4 v0, 0x1
    return v0
.end method

.method public resetMarkerIdle()V
    .locals 1
    iget-object v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    if-eqz v0, :marker_reset_done
    invoke-virtual {v0}, Lrubberbigpepper/VolumeControl/redesign/MarkerController;->close()V
    new-instance v0, Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    invoke-direct {v0, p0}, Lrubberbigpepper/VolumeControl/redesign/MarkerController;-><init>(Lrubberbigpepper/CommonCtrl/ZoomView;)V
    iput-object v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    :marker_reset_done
    return-void
.end method

.method protected onAttachedToWindow()V
    .locals 1
    invoke-super {p0}, Landroid/view/View;->onAttachedToWindow()V
    iget-boolean v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_markerAutoHide:Z
    if-eqz v0, :marker_attach_done
    new-instance v0, Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    invoke-direct {v0, p0}, Lrubberbigpepper/VolumeControl/redesign/MarkerController;-><init>(Lrubberbigpepper/CommonCtrl/ZoomView;)V
    iput-object v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    :marker_attach_done
    return-void
.end method

.method protected onDetachedFromWindow()V
    .locals 1
    iget-object v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    if-eqz v0, :marker_detach_done
    invoke-virtual {v0}, Lrubberbigpepper/VolumeControl/redesign/MarkerController;->close()V
    const/4 v0, 0x0
    iput-object v0, p0, Lrubberbigpepper/CommonCtrl/ZoomView;->m_marker:Lrubberbigpepper/VolumeControl/redesign/MarkerController;
    :marker_detach_done
    invoke-super {p0}, Landroid/view/View;->onDetachedFromWindow()V
    return-void
.end method
'''
s=re.sub(r'\.method protected onDraw\(Landroid/graphics/Canvas;\)V.*?\.end method',
'''.method protected onDraw(Landroid/graphics/Canvas;)V
    .locals 0
    invoke-super {p0, p1}, Landroid/view/View;->onDraw(Landroid/graphics/Canvas;)V
    invoke-static {p1, p0}, Lrubberbigpepper/VolumeControl/redesign/ScaleRenderer;->draw(Landroid/graphics/Canvas;Lrubberbigpepper/CommonCtrl/ZoomView;)V
    return-void
.end method''',s,count=1,flags=re.S)
save(zoom,s)

service='smali/rubberbigpepper/CommonCtrl/VolumeControlService.smali'
s=original(service)
# Rebuilding an overlay must read live levels, not invoke the user's volume command.
begin=s.index('.method private Initialize()V');end=s.index('.end method',begin)
block=s[begin:end]
first=block.index('    .line 495');last=block.index('    .line 497',first)
block=block[:first]+'''    :cond_1
    move-object/from16 v0, p0
    iget-object v3, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cZoomView:Lrubberbigpepper/CommonCtrl/ZoomView;
    iget-object v4, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cAudioManager:Landroid/media/AudioManager;
    iget v5, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_nStream:I
    invoke-static {v3, v4, v5}, Lrubberbigpepper/VolumeControl/redesign/ScaleState;->sync(Lrubberbigpepper/CommonCtrl/ZoomView;Landroid/media/AudioManager;I)V

'''+block[last:]
s=s[:begin]+block+s[end:]
# Keep restoring EQ effects, but never restore stale saved mixer stream levels.
begin=s.index('.method private ReadEqMixState()V');end=s.index('.end method',begin)
block=s[begin:end]
first=block.index('    .line 1142');last=block.index('    .line 1154',first)
assert block[first:last].count('->setStreamVolume(III)V')==7
block=block[:first]+'    :cond_1\n    # Stream levels remain owned by the live audio system.\n'+block[last:]
s=s[:begin]+block+s[end:]
# Opt in only the floating volume view; mixer/equalizer ZoomViews stay unchanged.
needle='    iput-object v3, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cZoomView:Lrubberbigpepper/CommonCtrl/ZoomView;'
assert s.count(needle)==1,'Floating scale must have one construction site'
s=s.replace(needle,needle+'\n    invoke-virtual {v3}, Lrubberbigpepper/CommonCtrl/ZoomView;->enableMarkerAutoHide()V',1)
begin=s.index('.method private Initialize()V');end=s.index('.end method',begin)
block=s[begin:end].replace('    return-void','''    move-object/from16 v0, p0
    iget-object v3, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cZoomView:Lrubberbigpepper/CommonCtrl/ZoomView;
    invoke-virtual {v3}, Lrubberbigpepper/CommonCtrl/ZoomView;->resetMarkerIdle()V
    return-void''',1)
s=s[:begin]+block+s[end:]
# Track only existing effects; reset never starts the service or restores mixer levels.
begin=s.index('.method private Initialize()V');end=s.index('.end method',begin)
block=s[begin:end]
block=block.replace('    return-void','''    move-object/from16 v0, p0
    iget-object v3, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cEqualizer:Ljava/lang/Object;
    iget-object v4, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cBass:Ljava/lang/Object;
    iget-object v5, v0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cVirtualizer:Ljava/lang/Object;
    invoke-static {v3, v4, v5}, Lrubberbigpepper/VolumeControl/redesign/EffectReset;->setTargets(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
    return-void''',1)
s=s[:begin]+block+s[end:]
# Initialization no longer calls SetVolume (audio must stay read-only), so arm
# its original display-only fade explicitly after the overlay is fully ready.
begin=s.index('.method private Initialize()V');end=s.index('.end method',begin)
block=s[begin:end].replace('    return-void','''    move-object/from16 v0, p0
    invoke-direct {v0}, Lrubberbigpepper/CommonCtrl/VolumeControlService;->startInitialAutoHide()V
    return-void''',1)
s=s[:begin]+block+s[end:]
s+='''
.method private startInitialAutoHide()V
    .locals 6
    const/4 v0, 0x0
    iput v0, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_nAlphaMinus:I
    iget-object v1, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cHandler:Landroid/os/Handler;
    iget-object v2, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cRunnableBlending:Ljava/lang/Runnable;
    invoke-virtual {v1, v2}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V
    iget-boolean v3, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_bAutoHide:Z
    if-eqz v3, :initial_hide_done
    iget v3, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_nColor:I
    invoke-static {v3}, Landroid/graphics/Color;->alpha(I)I
    move-result v3
    if-lez v3, :initial_hide_done
    const-wide/16 v4, 0x64
    invoke-virtual {v1, v2, v4, v5}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
    :initial_hide_done
    return-void
.end method
'''
begin=s.index('.method public onDestroy()V');end=s.index('.end method',begin)
block=s[begin:end].replace('    .prologue','    .prologue\n    invoke-static {}, Lrubberbigpepper/VolumeControl/redesign/EffectReset;->clearTargets()V',1)
s=s[:begin]+block+s[end:]
begin=s.index('.method private ReadEqMixState()V');end=s.index('.end method',begin)
block=s[begin:end];band=block.index('    const-string v6, "Band %d level"')
needle='    invoke-interface {v1, v6, v5}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I'
block=block[:band]+block[band:].replace(needle,'''    invoke-static {v1, v5}, Lrubberbigpepper/VolumeControl/redesign/EffectReset;->bandDefault(Landroid/content/SharedPreferences;I)I
    move-result v5
'''+needle,1)
s=s[:begin]+block+s[end:]
start=s.index('.method public handleCommand(Landroid/content/Intent;)V');position=s.index('    .prologue',start)
injection='''    # Explicit user actions from the new main screen.
    if-eqz p1, :redesign_original
    invoke-virtual {p1}, Landroid/content/Intent;->getAction()Ljava/lang/String;
    move-result-object v0
    const-string v1, "local.volumecontrol.redesign.MIXER"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-nez v1, :redesign_effects
    const-string v2, "local.volumecontrol.redesign.EQUALIZER"
    invoke-virtual {v2, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v2
    if-eqz v2, :redesign_original
    :redesign_effects
    iget-object v0, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cWM:Landroid/view/WindowManager;
    if-nez v0, :redesign_ready
    invoke-direct {p0}, Lrubberbigpepper/CommonCtrl/VolumeControlService;->Initialize()V
    :redesign_ready
    iget-object v0, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cMainEqMixerView:Landroid/view/View;
    if-nez v0, :redesign_visible
    invoke-direct {p0}, Lrubberbigpepper/CommonCtrl/VolumeControlService;->ShowEqMixWindow()V
    :redesign_visible
    iget-object v0, p0, Lrubberbigpepper/CommonCtrl/VolumeControlService;->m_cMainLayout:Landroid/widget/LinearLayout;
    if-eqz v1, :redesign_eq
    invoke-direct {p0, v0}, Lrubberbigpepper/CommonCtrl/VolumeControlService;->CreateAllMixViews(Landroid/widget/LinearLayout;)V
    return-void
    :redesign_eq
    invoke-direct {p0, v0}, Lrubberbigpepper/CommonCtrl/VolumeControlService;->CreateAllEqViews(Landroid/widget/LinearLayout;)V
    return-void
    :redesign_original
'''
s=s[:position]+injection+s[position:]
s=s.replace('%.0f Hz','%.0f Гц').replace('%.0f dB','%.0f дБ')
save(service,s)


helper=ROOT/'helper-smali/smali/rubberbigpepper/VolumeControl/redesign'
shutil.copytree(helper,SRC/'smali/rubberbigpepper/VolumeControl/redesign',dirs_exist_ok=True)

translations=ET.parse(ROOT.parent/'design/localization/res/values-ru/strings.xml').getroot()
arrays=ET.parse(ROOT.parent/'design/localization/res/values-ru/arrays.xml').getroot()
for folder in (SRC/'res').glob('values*'):
    file=folder/'strings.xml'
    if file.exists():
        data=ET.parse(file);root=data.getroot();byname={e.get('name'):e for e in root}
        for element in translations:
            key=element.get('name')
            if key in byname:byname[key].text=element.text
            elif folder.name in ['values','values-ru']:root.append(ET.fromstring(ET.tostring(element)))
        ET.indent(data);data.write(file,encoding='utf-8',xml_declaration=True)
    file=folder/'arrays.xml'
    if file.exists():
        data=ET.parse(file);root=data.getroot();byname={e.get('name'):e for e in root}
        for element in arrays:
            if element.get('name') in byname:
                old=byname[element.get('name')];index=list(root).index(old);root.remove(old);root.insert(index,ET.fromstring(ET.tostring(element)))
        ET.indent(data);data.write(file,encoding='utf-8',xml_declaration=True)

styles='res/values/styles.xml';s=original(styles)
s=s.replace('<style name="MyTheme" parent="@android:style/Theme" />','''<style name="MyTheme" parent="@android:style/Theme.Holo.NoActionBar">
        <item name="android:windowBackground">#121212</item>
        <item name="android:colorBackground">#121212</item>
        <item name="android:textColorPrimary">#F5F5F5</item>
        <item name="android:textColorSecondary">#B3B3B3</item>
        <item name="android:windowNoTitle">true</item>
    </style>''')
save(styles,s)
for file in (SRC/'res').glob('values-v*/styles.xml'):
    text=file.read_text(encoding='utf-8')
    text=text.replace('<style name="MyTheme" parent="@android:style/Theme.Holo" />',
        '<style name="MyTheme" parent="@android:style/Theme.Holo.NoActionBar"><item name="android:windowBackground">#121212</item><item name="android:colorBackground">#121212</item><item name="android:windowNoTitle">true</item></style>')
    file.write_text(text,encoding='utf-8')
# API18 AbsSeekBar centers its thumb using maxHeight, but stretches the track
# to the available bounds. Keep both in the same coordinate frame for tall targets.
for folder in (SRC/'res').glob('values*'):
    file=folder/'styles.xml'
    if not file.exists():continue
    tree=ET.parse(file)
    for style in tree.getroot():
        if style.get('name')=='MyTheme':
            match=next((item for item in style if item.get('name')=='android:seekBarStyle'),None)
            if match is None:match=ET.SubElement(style,'item',name='android:seekBarStyle')
            match.text='@style/RedesignSeekBar'
    ET.indent(tree);tree.write(file,encoding='utf-8',xml_declaration=True)
(SRC/'res/values/redesign.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<resources><style name="RedesignSeekBar" parent="@android:style/Widget.Holo.SeekBar">
<item name="android:minHeight">20dp</item><item name="android:maxHeight">1000dp</item>
</style></resources>''',encoding='utf-8')

icons=ROOT.parent/'design/icon/android'
for file in (SRC/'res').glob('drawable*/icon.png'):
    density=next((d for d in ['xxxhdpi','xxhdpi','xhdpi','hdpi','mdpi','ldpi'] if '-'+d in file.parent.name),'mdpi')
    shutil.copy2(icons/f'drawable-{density}/icon.png',file)
for file in (SRC/'res').glob('drawable*/free_icon.png'):
    density=next((d for d in ['xxxhdpi','xxhdpi','xhdpi','hdpi','mdpi','ldpi'] if '-'+d in file.parent.name),'mdpi')
    shutil.copy2(icons/f'drawable-{density}/icon.png',file)

manifest=ET.fromstring(original('AndroidManifest.xml'))
old='rubberbigpepper.VolumeControl';new=old+'.redesign'
manifest.set('package',new)
manifest.set(ANDROID+'installLocation','internalOnly')
for node in manifest.iter():
    if node.tag in ['activity','service','receiver']:
        name=node.get(ANDROID+'name','')
        if name.startswith('.'):node.set(ANDROID+'name',old+name)
    if node.tag=='activity-alias':
        name=node.get(ANDROID+'name','')
        # MainWindow's existing icon-switch logic uses the current package.
        node.set(ANDROID+'name',new+'.'+name.rsplit('.',1)[-1])
        target=node.get(ANDROID+'targetActivity','')
        if target.startswith('.'):node.set(ANDROID+'targetActivity',old+target)
    if node.get(ANDROID+'name')==old+'.NeverKillVolumeService':node.set(ANDROID+'name',new+'.NeverKillVolumeService')
# Keep old components for existing shortcuts, but expose one permanent launcher.
# MainWindow has never been toggled by SetAppIcon, including on older installs.
app=manifest.find('application')
for alias in app.findall('activity-alias'):
    alias.set(ANDROID+'exported','true')
    for intent_filter in list(alias.findall('intent-filter')):alias.remove(intent_filter)
activity=next(n for n in app.findall('activity') if n.get(ANDROID+'name')==old+'.MainWindow')
activity.set(ANDROID+'enabled','true')
activity.set(ANDROID+'exported','true')
launcher=ET.SubElement(activity,'intent-filter')
ET.SubElement(launcher,'action',{ANDROID+'name':'android.intent.action.MAIN'})
ET.SubElement(launcher,'category',{ANDROID+'name':'android.intent.category.LAUNCHER'})
ET.indent(manifest);ET.ElementTree(manifest).write(SRC/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
for file in (SRC/'smali/rubberbigpepper').rglob('*.smali'):
    text=file.read_text(encoding='utf-8')
    changed=text.replace(old+'.NeverKillVolumeService',new+'.NeverKillVolumeService')
    if text!=changed:file.write_text(changed,encoding='utf-8')
config=original('apktool.yml').replace('minSdkVersion: 3','minSdkVersion: 18').replace('versionCode: 23','versionCode: 38').replace('versionName: 2.7.1','versionName: 2.7.1-redesign15')
save('apktool.yml',config)
from indicator_patch import apply as apply_indicator
apply_indicator(SRC)
from autohide_patch import apply as apply_autohide
apply_autohide(SRC)
from touch_guard_patch import apply as apply_touch_guard
apply_touch_guard(SRC)
from remove_publisher_links import apply
apply(SRC)
print('Patched native UI, scale rendering, all locale resources, icons and isolated test package.')
