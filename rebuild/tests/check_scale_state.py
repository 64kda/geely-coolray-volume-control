"""Execute real ScaleState against live-level fakes; every audio write fails."""
from pathlib import Path
import subprocess,tempfile
BASE=Path(__file__).resolve().parents[2]
JDK=Path('C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin')
sources={
'android/media/AudioManager.java':'''package android.media; public class AudioManager {
 public int mode;public boolean music;public int[] levels={2,4,6,12,1,5,0,0,10};
 public int getMode(){return mode;}public boolean isMusicActive(){return music;}
 public int getStreamMaxVolume(int stream){return stream==3||stream==8?15:7;}
 public int getStreamVolume(int stream){return levels[stream];}
 public void setStreamVolume(int s,int v,int flags){throw new AssertionError("Settings wrote volume");}
 public void setRingerMode(int mode){throw new AssertionError("Settings wrote ringer mode");} }''',
'rubberbigpepper/CommonCtrl/ZoomView.java':'''package rubberbigpepper.CommonCtrl; public class ZoomView {
 public int max,level;public void setMax(int value){max=value;}public void setProgress(int value){level=value;} }''',
'ScaleStateTest.java':'''import java.util.*;import android.media.AudioManager;import rubberbigpepper.CommonCtrl.ZoomView;
 import rubberbigpepper.VolumeControl.redesign.ScaleState; public class ScaleStateTest {
 static void check(boolean condition){if(!condition)throw new AssertionError("Live volume state changed");}
 public static void main(String[] args){AudioManager audio=new AudioManager();ZoomView scale=new ZoomView();int[] before=audio.levels.clone();
 for(int repeat=0;repeat<100;repeat++)for(int stream:new int[]{0,1,2,3,4,5,8}){
 ScaleState.sync(scale,audio,stream);check(scale.level==before[stream]);check(scale.max==audio.getStreamMaxVolume(stream));check(Arrays.equals(before,audio.levels));}
 ScaleState.sync(scale,audio,-1);check(scale.level==12&&scale.max==15);
 audio.music=true;ScaleState.sync(scale,audio,-2);check(scale.level==12);
 audio.music=false;ScaleState.sync(scale,audio,-2);check(scale.level==6&&scale.max==7);
 audio.mode=2;ScaleState.sync(scale,audio,-2);check(scale.level==2);
 audio.mode=3;ScaleState.sync(scale,audio,-2);check(scale.level==2);
 audio.levels[3]=0;ScaleState.sync(scale,audio,3);check(scale.level==0);
 audio.levels[3]=15;ScaleState.sync(scale,audio,3);check(scale.level==15);
 System.out.println("ScaleState PASS: 700 settings refreshes preserve levels; channel ranges, auto/all, live external changes and min/max");} }'''
}
with tempfile.TemporaryDirectory(prefix='scale-state-') as tmp:
    root=Path(tmp)
    for name,source in sources.items():
        file=root/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(source,encoding='utf-8')
    native=BASE/'rebuild/java/rubberbigpepper/VolumeControl/redesign'
    subprocess.run([str(JDK/'javac.exe'),'-d',str(root),*[str(root/name) for name in sources],str(native/'ScaleState.java'),str(native/'VolumeMath.java')],check=True)
    subprocess.run([str(JDK/'java.exe'),'-cp',str(root),'ScaleStateTest'],check=True)
