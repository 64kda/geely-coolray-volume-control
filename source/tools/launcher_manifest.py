"""Apply four launcher aliases while keeping the real Activity and legacy shortcuts."""
import xml.etree.ElementTree as ET
from pathlib import Path

A = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', A[1:-1])
PACKAGE = 'rubberbigpepper.VolumeControl.redesign'
TARGET = 'rubberbigpepper.VolumeControl.MainWindow'
IDS = (7, 6, 8, 10)

def apply(file):
    tree = ET.parse(file)
    app = tree.getroot().find('application')
    app.set(A+'icon', '@drawable/launcher_art_7')
    target = next(node for node in app.findall('activity') if node.get(A+'name') == TARGET)
    target.set(A+'enabled', 'true')
    target.set(A+'icon', '@drawable/launcher_art_7')
    for alias in app.findall('activity-alias'):
        if not alias.get(A+'name', '').startswith(PACKAGE+'.LauncherIcon'):
            alias.set(A+'icon', '@drawable/launcher_art_7')
    # Never disable the target Activity: explicit/legacy shortcuts must keep working.
    for node in list(app):
        if node.tag == 'activity-alias' and node.get(A+'name', '').startswith(PACKAGE+'.LauncherIcon'):
            app.remove(node)
    for filt in list(target.findall('intent-filter')):
        if any(node.get(A+'name') == 'android.intent.category.LAUNCHER' for node in filt.findall('category')):
            target.remove(filt)
    for id in IDS:
        alias = ET.SubElement(app, 'activity-alias', {
            A+'name': PACKAGE+'.LauncherIcon'+str(id), A+'targetActivity': TARGET,
            A+'enabled': 'true' if id == 7 else 'false', A+'exported': 'true',
            A+'label': '@string/app_name', A+'icon': '@drawable/launcher_art_'+str(id)})
        filt = ET.SubElement(alias, 'intent-filter')
        ET.SubElement(filt, 'action', {A+'name': 'android.intent.action.MAIN'})
        ET.SubElement(filt, 'category', {A+'name': 'android.intent.category.LAUNCHER'})
    ET.indent(tree)
    tree.write(file, encoding='utf-8', xml_declaration=True)

if __name__ == '__main__':
    import sys
    apply(Path(sys.argv[1]))
