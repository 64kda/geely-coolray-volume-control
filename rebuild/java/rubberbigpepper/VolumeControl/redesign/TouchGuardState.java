package rubberbigpepper.VolumeControl.redesign;

/** No Android dependencies. All times are uptime milliseconds; one UI thread. */
public final class TouchGuardState {
    public interface Clock { long now(); }
    public interface Scheduler { void cancel(Runnable r); void post(Runnable r,long ms); }
    public interface Output { int level(); void write(int value); void ready(boolean on); void tools(); }
    private final Clock clock;
    private final Scheduler scheduler;
    private final Output output;
    private int hold;
    private final float slop;
    private boolean active,armed,dragging,toolsAllowed;
    private float downX,downY,x,y,anchor;
    private int max,base,last;
    private float length;
    private boolean vertical;
    private long start,generation,confirmed;
    private Runnable pending;
    public static int validHold(int ms){return ms==250||ms==500||ms==1000?ms:250;}
    public void configure(int ms){cancel();hold=validHold(ms);}
    public TouchGuardState(Clock c,Scheduler s,Output o,int ms,float tolerance){
        clock=c;scheduler=s;output=o;hold=validHold(ms);slop=tolerance;
    }
    public void down(float px,float py,long time,int range,float size,boolean upright,boolean tools){
        cancel();active=true;downX=x=px;downY=y=py;start=time;max=range;
        length=size;vertical=upright;toolsAllowed=tools;last=output.level();
        schedule(Math.max(0,start+hold-clock.now()));
    }
    private void schedule(long delay){
        final long token=generation;
        pending=new Runnable(){public void run(){
            if(!active||token!=generation)return;
            pending=null;
            long elapsed=clock.now()-start;
            if(elapsed<hold){schedule(hold-elapsed);return;}
            if(!armed){armed=true;confirmed=clock.now();base=last=output.level();anchor=vertical?y:x;output.ready(true);}
            if(!active)return;
            if(toolsAllowed&&!dragging){
                if(elapsed<hold+3000L)schedule(hold+3000L-elapsed);
                else {cancel();output.tools();}
            }
        }};scheduler.post(pending,delay);
    }
    /** Historical samples also pass here, in event order, before the current sample. */
    public void move(float px,float py,long time){
        if(!active)return;
        x=px;y=py;
        float dx=x-downX,dy=y-downY;
        if(!armed||time<confirmed){
            if(dx*dx+dy*dy>slop*slop){cancel();return;}
            // Only the timer confirms: a late batched MOVE cannot bypass slop.
            return;
        }
        if(dx*dx+dy*dy>slop*slop)toolsAllowed=false;
        float delta=(vertical?y:x)-anchor;
        if(!dragging&&Math.abs(delta)<=slop)return;
        if(length<=0||max<=0){cancel();return;}
        dragging=true;toolsAllowed=false;clearTimer();
        int value=Math.max(0,Math.min(max,Math.round(base+(vertical?-delta:delta)*max/length)));
        if(value!=last){last=value;output.write(value);}
    }
    public void external(int value){if(active&&value!=last)cancel();}
    private void clearTimer(){generation++;if(pending!=null)scheduler.cancel(pending);pending=null;}
    public void cancel(){clearTimer();active=false;armed=false;dragging=false;output.ready(false);}
}
