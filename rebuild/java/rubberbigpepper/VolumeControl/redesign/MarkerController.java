package rubberbigpepper.VolumeControl.redesign;

import android.os.SystemClock;
import rubberbigpepper.CommonCtrl.ZoomView;

/** View callback adapter. Created on attach, cancelled on detach. */
public final class MarkerController {
    private final ZoomView view;
    private final MarkerVisibility state;
    public MarkerController(final ZoomView target){
        view=target;
        state=new MarkerVisibility(target.getProgress(),new MarkerVisibility.Clock(){
            public long now(){return SystemClock.uptimeMillis();}
        },new MarkerVisibility.Scheduler(){
            public void cancel(Runnable task){target.removeCallbacks(task);}
            public void schedule(Runnable task,long delay){target.postDelayed(task,delay);}
        },new Runnable(){public void run(){target.invalidate();}});
    }
    public void changed(int value){if(view.getHeight()>view.getWidth())state.changed(value);}
    public boolean isVisible(){return state.isVisible();}
    public void close(){state.close();}
}
