exec(open('analyze_apk.py',encoding='utf-8').read().split('for dex_no, data')[0])
from androguard.core.analysis.analysis import Analysis
from androguard.decompiler.decompiler import DecompilerDAD
dx=Analysis()
ds=[]
for data in a.get_all_dex():
    d=DEX(data); ds.append(d); dx.add(d)
dx.create_xref()
for d in ds:
    d.set_decompiler(DecompilerDAD(d,dx))
    for c in d.get_classes():
        if c.get_name().startswith('Lrubberbigpepper/') and '/R$' not in c.get_name():
            try:
                dest=out/'java'/c.get_name()[1:-1]
                dest=dest.with_suffix('.java'); dest.parent.mkdir(parents=True,exist_ok=True)
                dest.write_text(c.get_source(),encoding='utf-8')
            except Exception as e: print(c.get_name(),str(e))
for p in r.get_packages_names():
    (out/'resources-resolved.xml').write_bytes(r.get_public_resources(p))
    for loc in ['\x00\x00','ru']:
        try: (out/('resources-'+('default' if '\x00' in loc else loc)+'.xml')).write_bytes(r.get_xml(p,loc))
        except Exception as e: print(e)
