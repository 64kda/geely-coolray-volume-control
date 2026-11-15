package rubberbigpepper.VolumeControl.redesign;

/** Bounded reconciliation of an audio write with delayed reads; no scheduling or audio I/O. */
public final class VolumeConfirmation {
    public static final int STALE=0, ACK=1, EXTERNAL=2;
    private int expected=-1,last=-1;
    private long deadline;
    public void written(int value,long now){expected=last=value;deadline=now+350;}
    public int observe(int value,long now){
        if(expected>=0&&now<deadline&&value!=expected)return STALE;
        expected=-1;
        if(value==last)return ACK;
        last=-1;return EXTERNAL;
    }
    public void reset(){expected=last=-1;deadline=0;}
}
