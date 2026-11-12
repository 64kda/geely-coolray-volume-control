"""Check preservation and export invariants for the design handoff."""
from pathlib import Path
import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET
from PIL import Image

base=Path(__file__).resolve().parents[2]
design=base/'design'
ns='{http://schemas.android.com/apk/res/android}'
public={p.attrib['id'].lower():p.attrib['name'] for p in ET.parse(base/'analysis/resources-resolved.xml').getroot()}
controls=[e for e in ET.parse(base/'analysis/res/layout/main.xml').getroot().iter()
          if e.tag in ['SeekBar','ImageView','SeekBar','Spinner','EditText','CheckBox']]
names=[public[e.attrib[ns+'id'].replace('@','0x').lower()] for e in controls]
spec=(design/'ДИЗАЙН-ПРОЕКТ.md').read_text(encoding='utf-8')
assert len(names)==23
assert all(name in spec for name in names)
pngs=list(design.glob('**/*.png'))
for file in design.glob('concepts/*.png'):
    assert Image.open(file).size==(1920,640),file
for file in design.glob('**/*.svg'):
    ET.parse(file)
for file in pngs:
    with Image.open(file) as image:
        image.verify()
for density,size in [('ldpi',36),('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    image=Image.open(design/f'icon/android/drawable-{density}/icon.png')
    assert image.size==(size,size)
    assert image.getpixel((0,0))[3]==0
for folder in ['values','values-ru']:
    for filename in ['strings.xml','arrays.xml']:
        ET.parse(design/'localization/res'/folder/filename)
apk_hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [base/'app-v2.7.1.apk',base/'app-second.apk']}
assert set(apk_hashes.values())=={'64b25aeaef78b398b14c6171df93d01e82bae9f34fda78277b7bc14c72f86d1f'}
sdk=Path('C:/Users/XGamerXFuryX/AppData/Local/Android/Sdk')
compile_result=subprocess.run([str(sdk/'build-tools/35.0.0/aapt.exe'),'package','-f',
    '-M',str(design/'validation/AndroidManifest.xml'),'-S',str(design/'localization/res'),
    '-I',str(sdk/'platforms/android-18/android.jar'),'-F',str(design/'validation/resources.ap_')],
    capture_output=True,text=True,check=True)
assert not compile_result.stderr.strip(),compile_result.stderr
report={'original_controls':len(names),'documented_controls':len(names),'missing_controls':[],
        'concepts':3,'concept_size':[1920,640],'png_exports':len(pngs),
        'xml_and_svg_parse':'passed','png_integrity_and_density_sizes':'passed',
        'api18_resource_compilation':'passed using SDK18 android.jar and aapt35; isolated resource draft only',
        'apk_hashes':apk_hashes,'app_build':'not performed','api18_runtime':'not performed'}
(design/'validation/design-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
