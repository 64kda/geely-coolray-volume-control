"""Check authored assets, manifest entry points and integration in the final APK."""
from pathlib import Path
from io import BytesIO
import json, sys, hashlib, zipfile, xml.etree.ElementTree as ET
from PIL import Image

BASE=Path(__file__).resolve().parents[2]
A='{http://schemas.android.com/apk/res/android}'
PKG='rubberbigpepper.VolumeControl.redesign'
IDS=(7,6,8,10)
def check_manifest(root, resolve=lambda value:value):
    app=root.find('application')
    assert resolve(app.get(A+'icon','')).endswith('/launcher_art_7'),'Application must use large launcher artwork'
    entries=[n for n in app if any(c.get(A+'name')=='android.intent.category.LAUNCHER' for c in n.iter('category'))]
    assert len(entries)==4
    assert {n.get(A+'name') for n in entries}=={PKG+'.LauncherIcon'+str(i) for i in IDS}
    assert [n.get(A+'name') for n in entries if n.get(A+'enabled')=='true']==[PKG+'.LauncherIcon7']
    for n in entries:
        assert n.tag=='activity-alias' and n.get(A+'targetActivity')=='rubberbigpepper.VolumeControl.MainWindow'
        assert n.get(A+'exported')=='true'
        id=n.get(A+'name').rsplit('LauncherIcon',1)[1]
        assert resolve(n.get(A+'icon','')).endswith('/launcher_art_'+id),'Launcher must not use a 48px density export'
    target=next(n for n in app.findall('activity') if n.get(A+'name')=='rubberbigpepper.VolumeControl.MainWindow')
    assert target.get(A+'enabled')=='true'

check_manifest(ET.parse(BASE/'source/app/src/main/AndroidManifest.xml').getroot())
lock=json.loads((BASE/'source/tests/icon-baselines/sha256.json').read_text())
for id in IDS:
    artfile=BASE/f'source/app/src/main/res/drawable-nodpi-v4/launcher_art_{id}.png'
    assert hashlib.sha256(artfile.read_bytes()).hexdigest()==lock[f'nodpi/{id}']
    artwork=Image.open(artfile)
    assert artwork.size==(512,512),'Large launcher artwork must be supplied, not an upscaled 48px PNG'
for density,size in [('ldpi',36),('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    hashes=[]
    for id in IDS:
        file=BASE/f'source/app/src/main/res/drawable-{density}-v4/launcher_icon_{id}.png'
        digest=hashlib.sha256(file.read_bytes()).hexdigest()
        assert digest==lock[f'{density}/{id}'],'Icon differs from reviewed baseline'
        image=Image.open(file).convert('RGBA')
        assert image.size==(size,size) and image.getpixel((0,0))[3]==0
        assert image.getpixel((size//2,size//2))[3]==255
        hashes.append(digest)
    assert len(set(hashes))==4
    default=BASE/f'source/app/src/main/res/drawable-{density}-v4/icon.png'
    assert default.read_bytes()==(default.parent/'launcher_icon_7.png').read_bytes()

if len(sys.argv)>1 and sys.argv[1]:
    sys.path.insert(0,str(BASE/'.analysis-tools'))
    from loguru import logger
    logger.remove()
    from androguard.core.apk import APK
    from androguard.core.dex import DEX
    apk=APK(sys.argv[1])
    def resolve(value):
        if value.startswith('@'):
            try:return apk.get_android_resources().get_resource_xml_name(int(value[1:],16),apk.get_package())
            except ValueError:pass
        return value
    check_manifest(apk.get_android_manifest_xml(),resolve)
    with zipfile.ZipFile(sys.argv[1]) as z:
        for id in IDS:
            name=f'res/drawable-nodpi-v4/launcher_art_{id}.png'
            actual=Image.open(BytesIO(z.read(name))).convert('RGBA')
            expected=Image.open(BASE/'source/tests/icon-baselines'/f'art-{id}.png').convert('RGBA')
            assert actual.size==(512,512) and actual.tobytes()==expected.tobytes(),'Large artwork not packaged: '+name
        for density in ['ldpi','mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']:
            for id in IDS:
                name=f'res/drawable-{density}-v4/launcher_icon_{id}.png'
                actual=Image.open(BytesIO(z.read(name))).convert('RGBA')
                expected=Image.open(BASE/name.replace('res/','source/app/src/main/res/',1)).convert('RGBA')
                assert actual.size==expected.size and actual.tobytes()==expected.tobytes(),name
        dex=DEX(z.read('classes.dex'))
        classes={c.get_name():c for c in dex.get_classes()}
        prefix='Lrubberbigpepper/VolumeControl/redesign/'
        launcher=classes[prefix+'LauncherIcons;']
        assert prefix+'IconSelection;' in classes
        ui=classes[prefix+'MainUi;']
        assert any('->restore()Z' in i.get_output() for m in ui.get_methods() if m.get_name()=='prepare' for i in m.get_instructions())
        backend=classes[prefix+'LauncherIcons$1;']
        assert any('->setComponentEnabledSetting' in i.get_output() for m in backend.get_methods() for i in m.get_instructions())
        assert not any('->setStreamVolume' in i.get_output() for c in [launcher,backend,classes[prefix+'IconSelection;']] for m in c.get_methods() for i in m.get_instructions())
print('Launcher icons: four approved assets, six densities, default07, four aliases / one default entry PASS')
