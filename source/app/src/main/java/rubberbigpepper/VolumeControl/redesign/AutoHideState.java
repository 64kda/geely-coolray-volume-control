package rubberbigpepper.VolumeControl.redesign;

/** UI-thread state machine. Each session owns a distinct cancellable callback. */
public final class AutoHideState {
    public interface Clock { long now(); }
    public interface Scheduler { void cancel(Runnable task); void schedule(Runnable task,long delay); }
    public interface Output { void visibility(float factor); }
    private final Clock clock;
    private final Scheduler scheduler;
    private final Output output;
    private boolean closed,held,enabled;
    private int delay,fade;
    private long deadline,generation;
    private Runnable pending;
    public AutoHideState(Clock c,Scheduler s,Output o,boolean on,int delayMs,int fadeMs){
        clock=c;scheduler=s;output=o;enabled=on;
        delay=validDelay(delayMs);fade=validFade(fadeMs);activity();
    }
    public static int validDelay(int v){return v==1000||v==2000||v==3000||v==5000||v==10000?v:2000;}
    public static int validFade(int v){return v==0||v==150||v==300||v==600?v:300;}
    private void cancel(){generation++;if(pending!=null)scheduler.cancel(pending);pending=null;}
    public void activity(){
        if(closed)return;
        cancel();output.visibility(1f);
        if(!enabled||held)return;
        deadline=clock.now()+delay;schedule(delay);
    }
    public void hold(){if(closed)return;held=true;activity();}
    public void release(){if(closed||!held)return;held=false;activity();}
    private void schedule(long after){
        final long token=generation;
        pending=new Runnable(){public void run(){
            if(closed||token!=generation)return;
            pending=null;
            long elapsed=clock.now()-deadline;
            if(elapsed<0){schedule(-elapsed);return;}
            float factor=fade==0?0f:Math.max(0f,1f-(float)elapsed/fade);
            output.visibility(factor);
            if(factor>0f)schedule(Math.min(10L,fade-elapsed));
        }};
        scheduler.schedule(pending,after);
    }
    public void close(){if(closed)return;closed=true;held=false;cancel();}
}
