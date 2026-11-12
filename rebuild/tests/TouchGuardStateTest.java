import java.util.*;
import rubberbigpepper.VolumeControl.redesign.TouchGuardState;

public class TouchGuardStateTest {
    static class Rig implements TouchGuardState.Clock,TouchGuardState.Scheduler,TouchGuardState.Output {
        long now;int level=7,writes,tools;boolean ready;
        Runnable task;TouchGuardState state;
        Rig(int hold){state=new TouchGuardState(this,this,this,hold,8);}
        public long now(){return now;}
        public void cancel(Runnable r){if(task==r)task=null;}
        public void post(Runnable r,long delay){task=r;}
        public int level(){return level;}
        public void write(int v){level=v;writes++;state.external(v);}
        public void ready(boolean v){ready=v;}
        public void tools(){tools++;}
        void down(boolean vertical,int max){state.down(50,50,now,max,100,vertical,true);}
        void fire(long time){now=time;Runnable r=task;task=null;if(r!=null)r.run();}
    }
    static void ok(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        for(int hold:new int[]{250,500,1000}){
            Rig r=new Rig(hold);r.down(true,15);Runnable stale=r.task;
            r.fire(hold-1);ok(!r.ready&&r.writes==0);r.fire(hold);ok(r.ready&&r.writes==0);
            r.fire(hold+1);ok(r.ready&&r.writes==0);
            r.state.move(50,42,hold+1);ok(r.writes==0);
            r.state.move(50,30,hold+2);ok(r.level==10&&r.writes==1);
            r.fire(hold+3000);ok(r.tools==0);r.state.cancel();stale.run();ok(!r.ready);

            r=new Rig(hold);r.down(true,15);r.state.move(59,50,hold-1);r.fire(hold);ok(!r.ready&&r.writes==0);
            r=new Rig(hold);r.down(true,15);r.state.cancel();r.fire(hold+5000);ok(r.writes==0&&r.tools==0);
            r=new Rig(hold);r.down(true,15);r.fire(hold);r.fire(hold+2999);ok(r.tools==0);r.fire(hold+3000);ok(r.tools==1&&r.writes==0);r.fire(9999);ok(r.tools==1);
            r=new Rig(hold);r.down(true,15);r.fire(hold);r.state.external(8);r.state.move(50,0,hold+1);ok(r.writes==0&&!r.ready);
            r=new Rig(hold);r.down(true,15);stale=r.task;r.state.cancel();r.down(true,15);stale.run();ok(!r.ready);
            // Batched excursion before the timer must reject even after timer delivery.
            r=new Rig(hold);r.down(true,15);r.fire(hold);r.state.move(80,50,hold-1);r.state.move(50,0,hold+1);ok(r.writes==0&&!r.ready);
        }
        for(boolean vertical:new boolean[]{false,true})for(int max:new int[]{1,7,15,100}){
            Rig r=new Rig(250);r.level=max/2;r.down(vertical,max);r.fire(250);
            r.state.move(vertical?50:150,vertical?-50:50,251);ok(r.level==max);
            r.state.move(vertical?50:-50,vertical?150:50,252);ok(r.level==0);
            r.state.cancel();int count=r.writes;r.state.move(0,0,300);ok(r.writes==count);
        }
        Rig r=new Rig(250);r.down(true,15);r.fire(250);r.state.move(70,50,251);r.fire(3250);ok(r.tools==0&&r.writes==0);
        ok(TouchGuardState.validHold(123)==250);
        System.out.println("TouchGuard PASS: boundaries, no-write gestures, relative axes/ranges, EqMix, external changes, stale callbacks, history");
    }
}
