import java.util.*;
import rubberbigpepper.VolumeControl.redesign.AutoHideState;

public class AutoHideStateTest implements AutoHideState.Clock,AutoHideState.Scheduler,AutoHideState.Output {
    long time;float factor;LinkedHashMap<Runnable,Long> tasks=new LinkedHashMap<Runnable,Long>();
    public long now(){return time;}
    public void visibility(float v){factor=v;}
    public void cancel(Runnable r){tasks.remove(r);}
    public void schedule(Runnable r,long delay){tasks.put(r,time+delay);}
    void at(long value){
        for(;;){Runnable next=null;long due=Long.MAX_VALUE;
            for(Map.Entry<Runnable,Long> e:tasks.entrySet())if(e.getValue()<due){next=e.getKey();due=e.getValue();}
            if(next==null||due>value)break;time=due;tasks.remove(next);next.run();
        }time=value;
    }
    void near(float expected){if(Math.abs(factor-expected)>.055f)throw new AssertionError(time+": "+factor+" != "+expected);}
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        for(int delay:new int[]{1000,2000,3000,5000,10000})for(int fade:new int[]{0,150,300,600}){
            AutoHideStateTest t=new AutoHideStateTest();AutoHideState s=new AutoHideState(t,t,t,true,delay,fade);
            t.at(delay-1);t.near(1);t.at(delay);t.near(fade==0?0:1);
            if(fade>0){t.at(delay+fade/2);t.near(.5f);}t.at(delay+fade);t.near(0);check(t.tasks.isEmpty());
            s.hold();t.near(1);t.at(50000);t.near(1);check(t.tasks.isEmpty());s.release();
            t.at(50000+delay-1);t.near(1);t.at(50000+delay+fade);t.near(0);
            s.activity();Runnable stale=t.tasks.keySet().iterator().next();s.activity();stale.run();t.near(1);check(t.tasks.size()==1);
            s.close();stale.run();check(t.tasks.isEmpty());
        }
        AutoHideStateTest t=new AutoHideStateTest();AutoHideState s=new AutoHideState(t,t,t,true,2000,300);
        t.at(2150);t.near(.5f);Runnable stale=t.tasks.keySet().iterator().next();s.activity();stale.run();t.near(1);
        t.at(4149);t.near(1);t.at(4450);t.near(0);
        s.activity();Runnable overdue=t.tasks.keySet().iterator().next();t.tasks.clear();t.time+=100000;overdue.run();t.near(0);check(t.tasks.isEmpty());
        s.close();s.activity();s.hold();s.release();check(t.tasks.isEmpty());
        AutoHideState disabled=new AutoHideState(t,t,t,false,2000,300);t.near(1);check(t.tasks.isEmpty());disabled.hold();disabled.release();check(t.tasks.isEmpty());
        check(AutoHideState.validDelay(-1)==2000&&AutoHideState.validFade(123)==300);
        System.out.println("AutoHideState PASS: 20 timing combinations, fade interruption, hold/release, cancellation generations, late wake, disabled/defaults");
    }
}
