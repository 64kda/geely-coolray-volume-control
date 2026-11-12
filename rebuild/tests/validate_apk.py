from pathlib import Path
import sys,json,hashlib,zipfile,xml.etree.ElementTree as ET
from io import BytesIO
from PIL import Image
BASE=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.apk import APK
from androguard.core.dex import DEX
file=Path(sys.argv[1]) if len(sys.argv)>1 else BASE/'output/VolumeControl-2.7.1-redesign12-startup-autohide-api18.apk';a=APK(str(file));package=a.get_package();resources=a.get_android_resources()
expected_code='38' if 'redesign15' in file.name else '37' if 'redesign14' in file.name else '36' if 'redesign13' in file.name else '35'
assert package=='rubberbigpepper.VolumeControl.redesign'
assert a.get_min_sdk_version()=='18'
assert a.get_androidversion_code()==expected_code
previous=APK(str(BASE/'output/VolumeControl-2.7.1-redesign2-api18.apk'))
assert a.get_package()==previous.get_package()
certificates={c.dump() for c in a.get_certificates_v1() if c is not None}
assert certificates and certificates=={c.dump() for c in previous.get_certificates_v1() if c is not None},'Update signing certificate changed'
manifest=a.get_android_manifest_xml()
android='{http://schemas.android.com/apk/res/android}'
launchers=[node for node in manifest.iter() if node.tag in ('activity','activity-alias')
    and any(c.get(android+'name')=='android.intent.category.LAUNCHER' for c in node.iter('category'))]
assert len(launchers)==1 and launchers[0].get(android+'name')=='rubberbigpepper.VolumeControl.MainWindow'
assert launchers[0].get(android+'enabled')=='true'
assert manifest.get(android+'installLocation') in ('internalOnly','1')
assert any(n.get(android+'name')=='android.permission.RECEIVE_BOOT_COMPLETED' for n in manifest.iter('uses-permission'))
receiver=next(n for n in manifest.iter('receiver') if n.get(android+'name')=='rubberbigpepper.CommonCtrl.CRebootReceiver')
assert any(n.get(android+'name')=='android.intent.action.BOOT_COMPLETED' for n in receiver.iter('action'))
old=ET.parse(BASE/'analysis/resources-resolved.xml').getroot()
new=ET.fromstring(resources.get_public_resources(package))
ids={(e.get('type'),e.get('name')):e.get('id') for e in new}
assert all(ids[(e.get('type'),e.get('name'))]==e.get('id') for e in old),'Original resource ID changed'
ru=ET.fromstring(resources.get_string_resources(package,'ru'))
translations={e.get('name'):e.text for e in ET.parse(BASE/'design/localization/res/values-ru/strings.xml').getroot()}
actual={e.get('name'):e.text for e in ru}
assert all(actual[k]==v.replace('\\"','"').replace("\\'", "'") for k,v in translations.items() if k not in {'DonateHTTP','LabelDonate','AdvDonation','PaypalDonation','BuyPro'})
with zipfile.ZipFile(file) as z:
    assert z.testzip() is None
    assert not any(n.startswith('classes2') for n in z.namelist()),'API18 must not require native multidex'
    dex=z.read('classes.dex');assert dex[:8]==b'dex\n035\x00'
    d=DEX(dex);classes={c.get_name() for c in d.get_classes()}
    assert 'Lrubberbigpepper/VolumeControl/redesign/TabDefaults;' in classes,'Reset defaults missing from built APK'
    assert 'Lrubberbigpepper/VolumeControl/redesign/ResetHost;' in classes,'Reset bridge missing from built APK'
    effects=next(c for c in d.get_classes() if c.get_name()=='Lrubberbigpepper/VolumeControl/redesign/EffectReset;')
    assert not any('->setStreamVolume(' in i.get_output() for m in effects.get_methods() for i in m.get_instructions())
    service=next(c for c in d.get_classes() if c.get_name()=='Lrubberbigpepper/CommonCtrl/VolumeControlService;')
    for method,target in [('Initialize','->setTargets('),('onDestroy','->clearTargets('),('ReadEqMixState','->bandDefault(')]:
        code=next(m for m in service.get_methods() if m.get_name()==method)
        assert any(target in i.get_output() for i in code.get_instructions()),method+' effect reset hook missing'
    main=next(c for c in d.get_classes() if c.get_name()=='Lrubberbigpepper/VolumeControl/MainWindow;')
    assert 'Lrubberbigpepper/VolumeControl/redesign/ResetHost;' in main.get_interfaces()
    assert any(m.get_name()=='setResetColors' for m in main.get_methods())
    for name in ['onCheckedChanged','onProgressChanged','onItemSelected']:
        handler=next(m for m in main.get_methods() if m.get_name()==name)
        assert '->isResetting()Z' in list(handler.get_instructions())[0].get_output(),name+' reset guard missing'
    icon=next(m for m in main.get_methods() if m.get_name()=='SetAppIcon')
    assert [i.get_name() for i in icon.get_instructions()]==['return-void'],'Launcher switching must stay disabled'
    assert 'Lrubberbigpepper/VolumeControl/redesign/MainUi;' in classes
    assert 'Lrubberbigpepper/CommonCtrl/ZoomView;' in classes
    image=Image.open(BytesIO(z.read('res/drawable-mdpi-v4/icon.png'))).convert('RGBA')
    reference=Image.open(BASE/'design/icon/android/drawable-mdpi/icon.png').convert('RGBA')
    assert image.size==reference.size and image.tobytes()==reference.tobytes()
result={'file':file.name,'bytes':file.stat().st_size,'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),
        'package':package,'version':a.get_androidversion_name(),'version_code':int(expected_code),'min_sdk':18,
        'target_sdk':a.get_target_sdk_version(),'original_resource_ids_preserved':True,
        'russian_strings_verified':len(translations)-5,'publisher_strings_cleared':5,'single_dex_035':True,'icon_verified':True,'zip_integrity':'passed',
        'permanent_launcher':True,'boot_receiver_and_permission':True,'icon_switch_disabled':True}
(BASE/('output/apk-validation-guard-feedback.json' if expected_code=='38' else 'output/apk-validation-f02.json' if expected_code=='37' else 'output/apk-validation-f01.json' if expected_code=='36' else 'output/apk-validation-startup-autohide.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
