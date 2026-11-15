package rubberbigpepper.VolumeControl.redesign;

/** UI-thread state only. Does not send audio commands or create Android windows. */
public final class DualScaleState {
    public static final int LEFT=1, RIGHT=2, BOTH=3;
    private int sides, owner, pointer=-1, value, maximum;
    private boolean together, suspended;
    private final long[] generation=new long[2];
    private final boolean[] visible=new boolean[2];

    public DualScaleState(int mode, int legacySide, boolean joint, int max, int level) {
        configure(mode,legacySide,joint);
        actual(level,max);
    }
    public static int safeMode(int mode,int legacySide) {
        return mode==LEFT||mode==RIGHT||mode==BOTH?mode:legacySide==RIGHT?RIGHT:LEFT;
    }
    public void configure(int mode,int legacySide,boolean joint) {
        cancel(); sides=safeMode(mode,legacySide); together=joint;
        for(int i=0;i<2;i++){generation[i]++;visible[i]=false;}
    }
    private static int index(int side) {
        if(side!=LEFT&&side!=RIGHT)throw new IllegalArgumentException("Expected one side");
        return side==LEFT?0:1;
    }
    public boolean allowed(int side){index(side);return !suspended&&(sides&side)!=0;}
    /** Called only after the existing touch guard confirms intent. */
    public boolean confirm(int side,int pointerId) {
        if(!allowed(side)||pointerId<0)return false;
        if(owner!=0)return owner==side&&pointer==pointerId;
        owner=side;pointer=pointerId;show(together?sides:side);return true;
    }
    public boolean owns(int side,int pointerId){return owner==side&&pointer==pointerId;}
    public boolean release(int side,int pointerId) {
        if(!owns(side,pointerId))return false;
        int mask=together?sides:owner;cancel();invalidate(mask);return true;
    }
    public void cancel(){owner=0;pointer=-1;}
    public void external(int level,int max){actual(level,max);if(!suspended)show(sides);}
    /** Readback is visual only; this class has no audio-write callback. */
    public void actual(int level,int max){maximum=Math.max(0,max);value=Math.max(0,Math.min(maximum,level));}
    public int value(){return value;}
    public int maximum(){return maximum;}
    private void invalidate(int mask){for(int i=0;i<2;i++)if((mask&(1<<i))!=0)generation[i]++;}
    private void show(int mask){invalidate(mask);for(int i=0;i<2;i++)if((mask&(1<<i))!=0)visible[i]=true;}
    public boolean visible(int side){return visible[index(side)];}
    public long timerToken(int side){return generation[index(side)];}
    public boolean expire(int side,long token) {
        int i=index(side);
        if(token!=generation[i]||!visible[i]||owner==side||(together&&owner!=0))return false;
        if(together){for(int j=0;j<2;j++){visible[j]=false;generation[j]++;}}
        else {visible[i]=false;generation[i]++;}
        return true;
    }
    public void sleep(){suspended=true;cancel();for(int i=0;i<2;i++){visible[i]=false;generation[i]++;}}
    public void wake(int mode,int legacySide,boolean joint,int level,int max){
        configure(mode,legacySide,joint);actual(level,max);suspended=false;
    }
}
