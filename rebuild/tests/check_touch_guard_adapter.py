"""Execute actual Android adapter against event/view/prefs fakes (not device QA)."""
from pathlib import Path
import subprocess,tempfile
BASE=Path(__file__).resolve().parents[2]
JDK=Path('C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin')
sources={
'android/content/SharedPreferences.java':'''package android.content; import java.util.*;
public class SharedPreferences {
 public Map<String,Object> values=new HashMap<String,Object>();
 public interface OnSharedPreferenceChangeListener {void onSharedPreferenceChanged(SharedPreferences p,String key);}
 public Set<OnSharedPreferenceChangeListener> listeners=new HashSet<OnSharedPreferenceChangeListener>();
 public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.add(l);}
 public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.remove(l);}
 public boolean contains(String k){return values.containsKey(k);}
 public boolean getBoolean(String k,boolean d){return contains(k)?(Boolean)values.get(k):d;}
 public int getInt(String k,int d){return contains(k)?(Integer)values.get(k):d;}
 public Editor edit(){return new Editor();}
 public class Editor {Map<String,Object> changes=new HashMap<String,Object>();
 public Editor putBoolean(String k,boolean v){changes.put(k,v);return this;}
 public Editor putInt(String k,int v){changes.put(k,v);return this;}
 public boolean commit(){values.putAll(changes);for(String k:changes.keySet())for(OnSharedPreferenceChangeListener l:new HashSet<OnSharedPreferenceChangeListener>(listeners))l.onSharedPreferenceChanged(SharedPreferences.this,k);return true;}}
}''',
'android/content/Context.java':'''package android.content; public class Context {
 public static final String AUDIO_SERVICE="audio";public android.media.AudioManager audio=new android.media.AudioManager();
 public Object getSystemService(String key){return audio;}
 public SharedPreferences prefs=new SharedPreferences();public SharedPreferences getSharedPreferences(String key,int mode){return prefs;}}''',
'android/media/AudioManager.java':'''package android.media;public class AudioManager {
 public int level=7,mode,max=15;public boolean music;
 public int getMode(){return mode;}public boolean isMusicActive(){return music;}
 public int getStreamMaxVolume(int stream){return max;}public int getStreamVolume(int stream){return level;}}''',
'android/os/SystemClock.java':'''package android.os;public class SystemClock {public static long now;public static long uptimeMillis(){return now;}}''',
'android/view/ViewConfiguration.java':'''package android.view;public class ViewConfiguration {
 public static ViewConfiguration get(android.content.Context c){return new ViewConfiguration();}public int getScaledTouchSlop(){return 8;}}''',
'android/view/MotionEvent.java':'''package android.view;public class MotionEvent {
 public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5,ACTION_POINTER_UP=6;
 public int action,count=1,id=42;public float x=50,y=50;public long time;
 public MotionEvent(int a,float px,float py){action=a;x=px;y=py;time=android.os.SystemClock.now;}
 public int getActionMasked(){return action;}public int getPointerId(int i){return id;}public int getPointerCount(){return count;}
 public int findPointerIndex(int pointer){return pointer==id?0:-1;}public long getEventTime(){return time;}
 public float getX(){return x;}public float getY(){return y;}public float getX(int i){return x;}public float getY(int i){return y;}
 public int getHistorySize(){return 0;}public float getHistoricalX(int i,int h){return x;}public float getHistoricalY(int i,int h){return y;}
 public long getHistoricalEventTime(int h){return time;}}''',
'rubberbigpepper/CommonCtrl/ZoomView.java':'''package rubberbigpepper.CommonCtrl;
import java.util.*;import android.content.*;import rubberbigpepper.VolumeControl.redesign.TouchGuardController;
public class ZoomView {
 public Context context=new Context();public TouchGuardController controller;public int level=7,writes,tools;public boolean ready;
 public Map<Runnable,Long> tasks=new HashMap<Runnable,Long>();
 public Context getContext(){return context;}public int getWidth(){return 26;}public int getHeight(){return 100;}
 public int getMax(){return 15;}public int getProgress(){return level;}public boolean guardToolsEnabled(){return true;}
 public void setMax(int v){}public void setProgress(int v){level=v;controller.changed(v);}
 public void guardWrite(int v){level=v;context.audio.level=v;writes++;controller.changed(v);}
 public void guardTools(){tools++;}public void setActivated(boolean on){ready=on;}public void invalidate(){}
 public boolean removeCallbacks(Runnable r){tasks.remove(r);return true;}
 public boolean postDelayed(Runnable r,long ms){tasks.put(r,android.os.SystemClock.now+ms);return true;}
 public void at(long time){android.os.SystemClock.now=time;for(Map.Entry<Runnable,Long> e:new HashMap<Runnable,Long>(tasks).entrySet())if(e.getValue()<=time){tasks.remove(e.getKey());e.getKey().run();}}
}''',
'AdapterTest.java':'''import android.content.*;import android.view.*;import android.os.*;import rubberbigpepper.CommonCtrl.*;import rubberbigpepper.VolumeControl.redesign.*;
public class AdapterTest {
 static void ok(boolean value){if(!value)throw new AssertionError();}
 static ZoomView make(){SystemClock.now=0;ZoomView v=new ZoomView();v.context.prefs.values.put("Touch guard enabled",true);v.controller=new TouchGuardController(v);return v;}
 static void event(ZoomView v,int a,float y){ok(v.controller.touch(new MotionEvent(a,50,y)));}
 public static void main(String[] args){
 for(int old:new int[]{-1,0,1}){SharedPreferences p=new SharedPreferences();if(old>=0)p.values.put("Reaction delay",old==1);
 ok(TouchGuardController.enabled(p)==(old==1));ok(TouchGuardController.hold(p)==(old==1?1000:250));
 p.values.put("Reaction delay",old!=1);ok(TouchGuardController.enabled(p)==(old==1));}
 SharedPreferences p=new SharedPreferences();p.values.put("Touch guard enabled",false);p.values.put("Reaction delay",true);p.values.put("Touch guard hold ms",123);ok(!TouchGuardController.enabled(p)&&TouchGuardController.hold(p)==250);
 ZoomView v=make();event(v,0,50);event(v,1,50);v.at(4000);ok(v.writes==0&&v.tools==0&&!v.ready);
 v=make();event(v,0,50);v.at(250);ok(v.ready&&v.writes==0);event(v,2,30);ok(v.writes==1&&v.level==10);event(v,1,30);v.at(5000);ok(v.tools==0);
 v=make();event(v,0,50);MotionEvent multi=new MotionEvent(5,50,50);multi.count=2;v.controller.touch(multi);v.at(250);event(v,2,0);event(v,1,0);v.at(5000);ok(v.writes==0&&v.tools==0);
 v=make();event(v,0,50);v.at(250);v.at(3250);ok(v.tools==1&&v.writes==0);v.at(9000);ok(v.tools==1);
 v=make();event(v,0,50);v.at(250);v.level=8;v.controller.changed(8);event(v,2,0);ok(v.writes==0&&!v.ready);
 v=make();v.context.prefs.values.put("Stream type",-2);event(v,0,50);v.at(250);v.context.audio.mode=2;event(v,2,0);ok(v.writes==0&&!v.ready);
 v=make();event(v,0,50);v.context.audio.level=8;v.at(250);ok(v.writes==0&&!v.ready);
 v=make();event(v,0,50);v.context.prefs.edit().putInt("Touch guard hold ms",500).commit();v.at(4000);ok(v.writes==0&&v.tools==0);
 v=make();event(v,0,50);v.context.prefs.edit().putBoolean("Touch guard enabled",false).commit();event(v,2,0);event(v,1,0);ok(v.writes==0);ok(!v.controller.touch(new MotionEvent(0,50,50)));
 v.context.prefs.edit().putBoolean("Touch guard enabled",true).putInt("Touch guard hold ms",500).commit();event(v,1,50);event(v,0,50);v.at(250);ok(!v.ready);v.at(500);ok(v.ready&&v.writes==0);
 v=make();event(v,0,50);Runnable stale=v.tasks.keySet().iterator().next();v.controller.close();v.at(4000);stale.run();ok(!v.ready&&v.context.prefs.listeners.isEmpty());
 v=make();v.context.prefs.edit().putBoolean("Touch guard feedback",false).commit();event(v,0,50);v.at(250);ok(!v.ready&&v.writes==0);event(v,2,30);ok(v.writes==1);event(v,1,30);
 v=new ZoomView();v.controller=new TouchGuardController(v);ok(!v.controller.touch(new MotionEvent(0,50,50)));ok(v.writes==0);
 System.out.println("TouchGuard adapter PASS: migration, legacy precedence, disabled routing, touch/audio counters, multi-touch, EqMix, external/settings/close cancellation");
 }}'''
}
with tempfile.TemporaryDirectory(prefix='touch-guard-') as tmp:
    root=Path(tmp)
    for name,source in sources.items():
        file=root/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(source,encoding='utf-8')
    native=BASE/'rebuild/java/rubberbigpepper/VolumeControl/redesign'
    subprocess.run([str(JDK/'javac.exe'),'-d',str(root),*[str(root/name) for name in sources],str(native/'TouchGuardState.java'),str(native/'TouchGuardController.java'),str(native/'VolumeMath.java')],check=True)
    subprocess.run([str(JDK/'java.exe'),'-cp',str(root),'AdapterTest'],check=True)
