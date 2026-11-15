package rubberbigpepper.VolumeControl.redesign;

/** Stable persisted indices: never reorder the original sixteen Gravity entries. */
public final class ScalePlacement {
    public static final int LEFT=16, RIGHT=17, BOTH=18;
    private ScalePlacement(){}
    public static int normalize(int saved){return saved>=0&&saved<=BOTH?saved:0;}
    /** Existing service geometry to reuse when creating the primary window. */
    public static int geometry(int saved){
        int mode=normalize(saved);
        return mode==LEFT?5:mode==RIGHT||mode==BOTH?11:mode;
    }
    public static boolean dual(int saved){return saved==BOTH;}
    public static int sides(int saved){
        int mode=normalize(saved);
        if(mode==BOTH)return DualScaleState.BOTH;
        if(mode==RIGHT||mode==2||mode>=10&&mode<=12)return DualScaleState.RIGHT;
        return DualScaleState.LEFT;
    }
    public static boolean jointEnabled(int saved,boolean preference){return dual(saved)&&preference;}
}
