package rubberbigpepper.VolumeControl.redesign;

/** Event-driven, per-component marker state. The scale level is never changed. */
public final class MarkerVisibility {
    public interface Clock {long now();}
    public interface Scheduler {void cancel(Runnable task);void schedule(Runnable task,long delay);}
    private static final long TIMEOUT_MS=2000;
    private final Clock clock;
    private final Scheduler scheduler;
    private final Runnable hidden;
    private int level;
    private long deadline;
    private boolean adjusting,closed;
    private final Runnable hide=new Runnable(){public void run(){
        if(closed||!adjusting||clock.now()<deadline)return;
        adjusting=false;hidden.run();
    }};
    public MarkerVisibility(int initialLevel,Clock time,Scheduler tasks,Runnable onHidden){
        level=initialLevel;clock=time;scheduler=tasks;hidden=onHidden;
    }
    public void changed(int value){
        if(closed||value==level)return;
        level=value;adjusting=true;deadline=clock.now()+TIMEOUT_MS;
        scheduler.cancel(hide);scheduler.schedule(hide,TIMEOUT_MS);
    }
    public boolean isVisible(){return !closed&&adjusting&&clock.now()<deadline;}
    public void close(){closed=true;adjusting=false;scheduler.cancel(hide);}
}
