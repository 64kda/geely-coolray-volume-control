import sys, json, pathlib, hashlib
sys.path.insert(0, str(pathlib.Path('.analysis-tools').resolve()))
from loguru import logger
logger.remove()
from androguard.core.apk import APK
from androguard.core.dex import DEX

out = pathlib.Path('analysis')
out.mkdir(exist_ok=True)
a = APK('app-v2.7.1.apk')
from lxml import etree
(out/'manifest.xml').write_bytes(etree.tostring(a.get_android_manifest_xml(), pretty_print=True, encoding='utf-8'))
summary = dict(package=a.get_package(), version=a.get_androidversion_name(), version_code=a.get_androidversion_code(), min_sdk=a.get_min_sdk_version(), target_sdk=a.get_target_sdk_version(), activities=a.get_activities(), services=a.get_services(), receivers=a.get_receivers(), permissions=a.get_permissions(), main_activity=a.get_main_activity(), sha256=hashlib.sha256(pathlib.Path('app-v2.7.1.apk').read_bytes()).hexdigest())
(out/'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
r = a.get_android_resources()
for package in r.get_packages_names():
    for locale in r.get_locales(package):
        name = locale.replace('\x00','') or 'default'
        (out/f'strings-{name}.xml').write_bytes(r.get_string_resources(package, locale))
from androguard.core.axml import AXMLPrinter
for name in a.get_files():
    if name.startswith('res/') and name.endswith('.xml'):
        dest=out/name
        dest.parent.mkdir(parents=True,exist_ok=True)
        try: dest.write_bytes(AXMLPrinter(a.get_file(name)).get_xml())
        except Exception: pass
for dex_no, data in enumerate(a.get_all_dex()):
    d=DEX(data)
    (out/f'dex-strings-{dex_no}.txt').write_text('\n'.join(d.get_strings()),encoding='utf-8')
    with (out/f'app-code-{dex_no}.txt').open('w',encoding='utf-8') as f:
        for c in d.get_classes():
            if c.get_name().startswith('Lrubberbigpepper/'):
                f.write('\nCLASS '+c.get_name()+' EXTENDS '+str(c.get_superclassname())+'\n')
                for m in c.get_methods():
                    f.write('\nMETHOD '+m.get_name()+m.get_descriptor()+'\n')
                    for ins in m.get_instructions():
                        f.write(ins.get_name()+' '+ins.get_output()+'\n')
print(json.dumps(summary,ensure_ascii=False,indent=2))
