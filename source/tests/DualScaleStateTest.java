import rubberbigpepper.VolumeControl.redesign.DualScaleState;

public final class DualScaleStateTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        for(int mode=1;mode<=3;mode++)for(boolean joint:new boolean[]{false,true}){
            DualScaleState s=new DualScaleState(mode,1,joint,15,7);
            s.external(8,15);
            check(s.visible(1)==((mode&1)!=0));check(s.visible(2)==((mode&2)!=0));
            s.configure(mode,1,joint);
            int side=mode==2?2:1,other=side==1?2:1;
            check(s.confirm(side,4));check(s.visible(side));
            check(s.visible(other)==(joint&&mode==3));
            check(!s.confirm(other,5));check(!s.release(side,5));check(s.owns(side,4));
            check(!s.expire(side,s.timerToken(side)));
            long before=s.timerToken(side);check(s.release(side,4));check(!s.expire(side,before));
            check(s.expire(side,s.timerToken(side)));
            s.sleep();check(!s.confirm(side,4));check(!s.visible(1)&&!s.visible(2));
            s.wake(mode,1,joint,99,15);check(s.value()==15);check(s.confirm(side,9));
            s.actual(-10,15);check(s.value()==0);
        }
        DualScaleState s=new DualScaleState(3,1,false,15,7);
        s.confirm(1,0);s.release(1,0);long left=s.timerToken(1);
        s.confirm(2,0);s.release(2,0);check(s.visible(1)&&s.visible(2));
        check(s.expire(1,left));check(!s.visible(1)&&s.visible(2));
        long old=s.timerToken(2);s.external(9,15);check(!s.expire(2,old));
        s.configure(1,1,false);check(!s.confirm(2,0));check(!s.visible(2));
        check(DualScaleState.safeMode(99,2)==2);check(DualScaleState.safeMode(99,99)==1);
        // Old timers and fingers must not affect a newly selected placement.
        for(int mode=1;mode<=3;mode++)for(boolean joint:new boolean[]{false,true}){
            DualScaleState changed=new DualScaleState(3,1,true,15,7);
            check(changed.confirm(1,12));long oldLeft=changed.timerToken(1),oldRight=changed.timerToken(2);
            changed.configure(mode,1,joint);
            check(!changed.owns(1,12));check(!changed.release(1,12));
            check(!changed.visible(1)&&!changed.visible(2));
            int side=mode==2?2:1;
            check(changed.confirm(side,13));
            check(!changed.expire(1,oldLeft)&&!changed.expire(2,oldRight));
            check(changed.owns(side,13));
            if(mode==3&&joint)check(!changed.expire(side==1?2:1,changed.timerToken(side==1?2:1)));
            long beforeSleep=changed.timerToken(side);
            changed.sleep();changed.external(14,15);
            check(!changed.visible(1)&&!changed.visible(2));check(!changed.confirm(side,14));
            changed.wake(mode,1,joint,14,15);check(changed.confirm(side,15));
            check(!changed.expire(side,beforeSleep));check(changed.owns(side,15));
        }
        System.out.println("DualScaleState PASS: modes, confirmed ownership, pointer release, visibility, stale timers, sleep, fallback, bounds");
    }
}
