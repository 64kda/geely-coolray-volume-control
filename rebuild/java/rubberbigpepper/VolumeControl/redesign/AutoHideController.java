package rubberbigpepper.VolumeControl.redesign;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import rubberbigpepper.CommonCtrl.ZoomView;

/** Only the floating scale opts in. No audio calls or persistent animation colors. */
public final class AutoHideController {
    private final AutoHideState state;
    private final Handler handler=new Handler(Looper.getMainLooper());
    public static int read(SharedPreferences prefs,String key,boolean delay){
        int value;
        try{value=prefs.getInt(key,delay?2000:300);}catch(ClassCastException wrongType){value=delay?2000:300;}
        return delay?AutoHideState.validDelay(value):AutoHideState.validFade(value);
    }
    public AutoHideController(final ZoomView view){
        SharedPreferences prefs=view.getContext().getSharedPreferences("Common",0);
        state=new AutoHideState(new AutoHideState.Clock(){public long now(){return SystemClock.elapsedRealtime();}},
            new AutoHideState.Scheduler(){
                public void cancel(Runnable task){handler.removeCallbacks(task);}
                public void schedule(Runnable task,long delay){handler.postDelayed(task,delay);}
            },new AutoHideState.Output(){public void visibility(float value){view.setAlpha(value);}},
            prefs.getBoolean("Auto hide",false),read(prefs,"Auto hide delay ms",true),read(prefs,"Auto hide fade ms",false));
    }
    public void changed(){state.activity();}
    public void touch(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN)state.hold();
        else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)state.release();
    }
    public void close(){state.close();}
}
