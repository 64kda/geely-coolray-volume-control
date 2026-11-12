package rubberbigpepper.VolumeControl.redesign;

/** Geometry in physical pixels; never owns or transforms the saved width. */
public final class IndicatorGeometry {
    private IndicatorGeometry(){}
    public static int length(int displayHeight,int setting){
        return (int)(displayHeight*(.5f+Math.max(0,Math.min(130,setting))*.01f)/3f);
    }
    public static int active(int value,int max){
        return max<=0?0:Math.round(20f*Math.max(0,Math.min(max,value))/max);
    }
    private static int clamp(int n,int lo,int hi){return Math.max(lo,Math.min(Math.max(lo,hi),n));}
    public static int[] label(int x,int y,int w,int h,int tw,int th,int l,int t,int r,int b){
        int px,py;
        if(h>w){
            px=x+(w-tw)/2;py=y-th-8;
            if(py<t){px=x+w+8;if(px+tw>r)px=x-tw-8;py=y;}
        }else{
            px=x+w+8;py=y+(h-th)/2;
            if(px+tw>r){px=x+(w-tw)/2;py=y-th-8;}
        }
        return new int[]{clamp(px,l,r-tw),clamp(py,t,b-th)};
    }
}
