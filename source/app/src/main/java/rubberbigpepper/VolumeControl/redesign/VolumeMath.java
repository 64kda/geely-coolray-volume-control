package rubberbigpepper.VolumeControl.redesign;
public final class VolumeMath {
    private static final int[] STREAMS={-2,-1,3,1,2,5,0,4,8};
    private VolumeMath() {}
    public static int streamAt(int position){return STREAMS[Math.max(0,Math.min(8,position))];}
    public static int displayStream(int selected,int mode,boolean musicActive){
        if(selected==-2)return mode==2||mode==3?0:musicActive?3:2;
        return selected==-1?3:selected;
    }
    public static int scale(int value,int sourceMax,int targetMax){
        if(sourceMax<=0||targetMax<=0)return 0;
        return Math.min(targetMax,Math.max(0,value)*targetMax/sourceMax);
    }
    public static int percent(int value,int max){return scale(value,max,100);}
    public static int ringerMode(int value,int action){
        if(action!=1&&action!=2)return -1;
        return value>0?2:action==1?1:0;
    }
}
