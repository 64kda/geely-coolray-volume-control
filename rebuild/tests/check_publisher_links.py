"""Check the shipped DEX and every resource locale for old commercial links."""
from pathlib import Path
import sys, zipfile, xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.apk import APK
from androguard.core.dex import DEX

def check(file):
    apk = APK(str(file))
    resources = apk.get_android_resources()
    with zipfile.ZipFile(file) as archive:
        dex = DEX(archive.read('classes.dex'))
    strings = dex.get_strings()
    forbidden = ('rubberbigpepper.com', 'rubberbigpepper.VolumeControlPro',
                 'ca-app-pub-3988028866185067')
    for value in strings:
        assert not any(part in value for part in forbidden), 'Old publisher link: '+value
    for locale in resources.get_locales(apk.get_package()):
        xml = resources.get_string_resources(apk.get_package(), locale)
        for node in ET.fromstring(xml):
            assert 'rubberbigpepper.com' not in (node.text or '')
            if node.get('name') in {'DonateHTTP', 'LabelDonate', 'AdvDonation', 'PaypalDonation', 'BuyPro'}:
                assert not node.text, 'Old support text in '+repr(locale)
    main = next(c for c in dex.get_classes() if c.get_name() == 'Lrubberbigpepper/VolumeControl/MainWindow;')
    create = next(m for m in main.get_methods() if m.get_name() == 'onCreate')
    assert not any('Lcom/google/android/gms/ads/AdView;' in i.get_output() for i in create.get_instructions()), 'Publisher advertising still initialized'
    menu = next(m for m in main.get_methods() if m.get_name() == 'onCreateContextMenu')
    assert [i.get_name() for i in menu.get_instructions()] == ['return-void']
    print(str(file)+': publisher links, support menu and advertising initialization absent PASS')

if __name__ == '__main__':
    check(Path(sys.argv[1]))
