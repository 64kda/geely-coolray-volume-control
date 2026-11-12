import java.util.*;
import rubberbigpepper.VolumeControl.redesign.MarkerVisibility;

public class MarkerVisibilityTest {
    static final class Harness implements MarkerVisibility.Clock,MarkerVisibility.Scheduler,Runnable {
        long now;int hides;Runnable pending;long due;
        final MarkerVisibility marker=new MarkerVisibility(4,this,this,this);
        public long now(){return now;}
        public void cancel(Runnable task){if(pending==task)pending=null;}
        public void schedule(Runnable task,long delay){pending=task;due=now+delay;}
        public void run(){hides++;}
        void advance(long time){now=time;if(pending!=null&&due<=now){Runnable task=pending;pending=null;task.run();}}
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        Harness h=new Harness();check(!h.marker.isVisible()&&h.pending==null,"Idle at initial level");
        h.marker.changed(4);check(!h.marker.isVisible()&&h.pending==null,"Same value is not adjustment");
        h.marker.changed(5);check(h.marker.isVisible(),"Actual change shows marker");
        h.advance(1999);check(h.marker.isVisible()&&h.hides==0,"Visible through 1999ms");
        h.advance(2000);check(!h.marker.isVisible()&&h.hides==1&&h.pending==null,"Hidden at 2000ms, no polling");
        h=new Harness();h.marker.changed(5);Runnable stale=h.pending;h.advance(1500);h.marker.changed(6);
        h.advance(2000);stale.run();check(h.marker.isVisible()&&h.hides==0,"Stale hide cannot hide recent change");
        h.advance(3499);check(h.marker.isVisible(),"Deadline restarted");
        h.advance(3500);check(!h.marker.isVisible()&&h.hides==1,"New deadline hides once");
        h=new Harness();h.marker.changed(5);h.advance(1000);h.marker.changed(5);h.advance(2000);
        check(!h.marker.isVisible(),"Duplicate events do not restart timer");
        h=new Harness();for(int value=5;value<=15;value++){h.advance(value*20);h.marker.changed(value);check(h.marker.isVisible(),"Rapid change must not flicker");}
        check(h.hides==0,"No intermediate hides");h.advance(2300);check(!h.marker.isVisible(),"Burst expires");
        h=new Harness();h.marker.changed(0);check(h.marker.isVisible(),"Minimum shows marker");h.marker.changed(15);check(h.marker.isVisible(),"Maximum shows marker");
        stale=h.pending;h.marker.close();check(!h.marker.isVisible()&&h.pending==null,"Detach cancels callback");
        h.advance(5000);stale.run();check(h.hides==0&&!h.marker.isVisible(),"Detached task is inert");
        h.marker.changed(6);check(!h.marker.isVisible()&&h.pending==null,"Detached changes ignored");
        Harness recreated=new Harness();check(!recreated.marker.isVisible()&&recreated.pending==null,"Recreated component starts idle");
        System.out.println("MarkerVisibility PASS: idle, 1999/2000ms, duplicate/burst, stale callback, min/max, detach/recreate");
    }
}
