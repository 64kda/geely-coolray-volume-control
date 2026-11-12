package rubberbigpepper.VolumeControl.redesign;

import android.content.SharedPreferences;
import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import rubberbigpepper.CommonCtrl.ZoomView;

/** Opt-in for the floating view only; the disabled path remains in original smali. */
public final class TouchGuardController implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final ZoomView view;
    private final TouchGuardState state;
    private boolean enabled,contact,blockedUntilDown;
    private final SharedPreferences prefs;
    private final AudioManager audio;
    private int stream,range;
    private int pointer=-1;
    private boolean ownWrite,closed;
    public static boolean enabled(SharedPreferences p){
        migrate(p);try{return p.getBoolean("Touch guard enabled",false);}catch(ClassCastException e){return false;}
    }
    public static int hold(SharedPreferences p){
        migrate(p);try{return TouchGuardState.validHold(p.getInt("Touch guard hold ms",250));}catch(ClassCastException e){return 250;}
    }
    public static boolean feedback(SharedPreferences p){
        try{return p.getBoolean("Touch guard feedback",true);}catch(ClassCastException e){return true;}
    }
    private static void migrate(SharedPreferences p){
        if(p.contains("Touch guard enabled"))return;
        boolean old=false;try{old=p.getBoolean("Reaction delay",false);}catch(ClassCastException ignored){}
        p.edit().putBoolean("Touch guard enabled",old).putInt("Touch guard hold ms",old?1000:250).commit();
    }
    public TouchGuardController(final ZoomView target){
        view=target;SharedPreferences p=view.getContext().getSharedPreferences("Common",0);prefs=p;
        audio=(AudioManager)view.getContext().getSystemService(Context.AUDIO_SERVICE);
        enabled=enabled(p);
        state=new TouchGuardState(new TouchGuardState.Clock(){public long now(){return SystemClock.uptimeMillis();}},
            new TouchGuardState.Scheduler(){
                public void cancel(Runnable r){view.removeCallbacks(r);}
                public void post(Runnable r,long ms){view.postDelayed(r,ms);}
            },new TouchGuardState.Output(){
                public int level(){return view.getProgress();}
                public void write(int value){if(!validSnapshot()){cancel();return;}ownWrite=true;try{view.guardWrite(value);}finally{ownWrite=false;}}
                public void ready(boolean on){if(on&&!validSnapshot()){cancel();return;}view.setActivated(on&&feedback(prefs));view.invalidate();}
                public void tools(){if(validSnapshot())view.guardTools();}
            },hold(p),ViewConfiguration.get(view.getContext()).getScaledTouchSlop());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }
    public boolean touch(MotionEvent e){
        if(closed)return true;
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){contact=true;blockedUntilDown=false;}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)contact=false;
        if(blockedUntilDown)return true;
        if(!enabled)return false;
        if(action==MotionEvent.ACTION_DOWN){
            state.cancel();
            try{
                stream=resolveStream();range=audio.getStreamMaxVolume(stream);
                view.setMax(range);view.setProgress(audio.getStreamVolume(stream));
            }catch(RuntimeException unavailable){pointer=-1;return true;}
            pointer=e.getPointerId(0);boolean vertical=view.getWidth()<view.getHeight();
            state.down(e.getX(),e.getY(),e.getEventTime(),view.getMax(),vertical?view.getHeight():view.getWidth(),vertical,view.guardToolsEnabled());
        }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){state.cancel();pointer=-1;}
        else if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP||e.getPointerCount()!=1){state.cancel();pointer=-1;}
        else if(action==MotionEvent.ACTION_MOVE){
            if(!validSnapshot()){cancel();return true;}
            int index=e.findPointerIndex(pointer);
            if(index<0){state.cancel();return true;}
            for(int i=0;i<e.getHistorySize();i++)state.move(e.getHistoricalX(index,i),e.getHistoricalY(index,i),e.getHistoricalEventTime(i));
            state.move(e.getX(index),e.getY(index),e.getEventTime());
        }
        return true;
    }
    private int resolveStream(){return VolumeMath.displayStream(prefs.getInt("Stream type",3),audio.getMode(),audio.isMusicActive());}
    private boolean validSnapshot(){
        try{return resolveStream()==stream&&audio.getStreamMaxVolume(stream)==range&&audio.getStreamVolume(stream)==view.getProgress();}
        catch(RuntimeException unavailable){return false;}
    }
    public void changed(int value){if(!ownWrite)state.external(value);}
    public void cancel(){state.cancel();pointer=-1;}
    public void onSharedPreferenceChanged(SharedPreferences p,String key){
        blockedUntilDown=contact;cancel();enabled=enabled(p);state.configure(hold(p));
    }
    public void close(){closed=true;cancel();prefs.unregisterOnSharedPreferenceChangeListener(this);}
}
