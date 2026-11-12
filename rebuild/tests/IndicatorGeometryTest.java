import rubberbigpepper.VolumeControl.redesign.IndicatorGeometry;
public class IndicatorGeometryTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        check(IndicatorGeometry.length(600,100)==300,"old maximum");
        check(IndicatorGeometry.length(600,130)==360,"20 percent increase");
        check(IndicatorGeometry.length(600,0)==100,"old minimum");
        for(int max:new int[]{7,15,40,100}){
            check(IndicatorGeometry.active(0,max)==0,"empty");
            check(IndicatorGeometry.active(max,max)==20,"full");
            int previous=-1;
            for(int n=0;n<=max;n++){int active=IndicatorGeometry.active(n,max);check(active>=previous&&active<=20,"monotonic");previous=active;}
        }
        for(int left:new int[]{0,30,900,980})for(int top:new int[]{0,60,300,550}){
            int[] p=IndicatorGeometry.label(left,top,26,300,100,36,0,24,1024,576);
            check(p[0]>=0&&p[0]+100<=1024&&p[1]>=24&&p[1]+36<=576,"label safe");
        }
        check(IndicatorGeometry.label(980,200,26,300,100,36,0,24,1024,576)[1]==156,"above scale");
        check(IndicatorGeometry.label(0,200,26,300,100,36,0,24,1024,576)[1]==156,"left above scale");
        System.out.println("IndicatorGeometry PASS: ranges 7/15/40/100, equal segments, safe label, old/new length");
    }
}
