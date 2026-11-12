"""Small targeted mutation check, no source files changed and no model calls."""
from pathlib import Path
import subprocess, tempfile

BASE=Path(__file__).resolve().parents[2]
JDK=Path('C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin')
source=(BASE/'rebuild/java/rubberbigpepper/VolumeControl/redesign/MarkerVisibility.java').read_text(encoding='utf-8')
mutations={
    'late timeout':source.replace('TIMEOUT_MS=2000','TIMEOUT_MS=2001'),
    'wrong boundary':source.replace('clock.now()<deadline)return','clock.now()<=deadline)return'),
    'duplicate is activity':source.replace('closed||value==level','closed'),
    'stale callback hides':source.replace('||clock.now()<deadline)return',')return'),
    'detach leaves task':source.replace('closed=true;adjusting=false;scheduler.cancel(hide);','closed=true;adjusting=false;'),
}
for name,mutated in mutations.items():
    assert mutated!=source,name
    with tempfile.TemporaryDirectory(prefix='marker-mutation-') as tmp:
        root=Path(tmp);file=root/'MarkerVisibility.java';file.write_text(mutated,encoding='utf-8')
        subprocess.run([str(JDK/'javac.exe'),'-d',str(root),str(file),str(BASE/'rebuild/tests/MarkerVisibilityTest.java')],check=True,capture_output=True)
        result=subprocess.run([str(JDK/'java.exe'),'-cp',str(root),'MarkerVisibilityTest'],capture_output=True,text=True)
        assert result.returncode!=0,'Surviving mutation: '+name
        assert 'AssertionError' in result.stderr,'Unexpected failure: '+result.stderr
        print('Killed mutation: '+name)
print('Marker timer mutations: 5/5 rejected')
