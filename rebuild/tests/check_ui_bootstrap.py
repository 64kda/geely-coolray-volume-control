"""Guard against finding original controls after MainUi detaches the old form.

Checks the compiled APK, including instruction order, rather than the source text.
"""
from pathlib import Path
import sys, zipfile

BASE = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(BASE/'.analysis-tools'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

def check(file):
    with zipfile.ZipFile(file) as apk:
        dex = DEX(apk.read('classes.dex'))
    main = next(c for c in dex.get_classes() if c.get_name() == 'Lrubberbigpepper/VolumeControl/redesign/MainUi;')
    constructor = next(m for m in main.get_methods() if m.get_name() == '<init>')
    detached = False
    lookups = 0
    for instruction in constructor.get_instructions():
        if not instruction.get_name().startswith('invoke-'):
            continue
        target = instruction.get_output()
        if '->removeAllViews()' in target:
            detached = True
        if '->findViewById(I)' in target:
            assert not detached, 'Original control lookup after detaching form: '+target
            lookups += 1
    assert detached and lookups >= 23, 'Bootstrap was not inspected'
    print(str(file)+': UI bootstrap lookup order PASS', flush=True)

if __name__ == '__main__':
    check(Path(sys.argv[1]))
