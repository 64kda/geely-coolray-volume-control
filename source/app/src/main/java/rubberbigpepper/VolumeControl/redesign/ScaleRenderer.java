package rubberbigpepper.VolumeControl.redesign;
import android.graphics.*;
import rubberbigpepper.CommonCtrl.ZoomView;
/** API18 drawing, preserving the original ZoomView gesture and channel handling. */
public final class ScaleRenderer {
    private static final Paint P=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF R=new RectF();
    private static void round(Canvas c,float x,float y,float w,float h,float radius,int color){
        if(w<=0||h<=0)return;P.setColor(color);R.set(x,y,x+w,y+h);c.drawRoundRect(R,radius,radius,P);
    }
    public static void draw(Canvas c,ZoomView v){
        float w=v.getWidth(),h=v.getHeight();if(w<=0||h<=0)return;
        float value=v.getMax()>0?Math.max(0,Math.min(1,(float)v.getProgress()/v.getMax())):0;
        boolean horizontal=w>=h;float length=horizontal?w:h,cross=horizontal?h:w;
        int fill=v.getProceedColor(),back=v.getRemainColor();
        int alpha=Math.max(Color.alpha(fill),Color.alpha(back));
        if(v.getBarType()==2){
            // Existing straight-style inset and width; all twenty cells are equal.
            float thick=Math.max(2,cross*.40f),pad=Math.max(cross*.23f,2);
            float pitch=Math.max(0,length-2*pad)/20f,cell=pitch*.72f;
            int active=IndicatorGeometry.active(v.getProgress(),v.getMax());
            for(int i=0;i<20;i++){
                int color=i<active?Color.argb(Color.alpha(fill),30,215,96):Color.argb(Color.alpha(back),55,55,55);
                float pos=pad+i*pitch;
                if(horizontal)round(c,pos,(h-thick)/2,cell,thick,Math.min(cell,thick)*.2f,color);
                else round(c,(w-thick)/2,length-pos-cell,thick,cell,Math.min(cell,thick)*.2f,color);
            }
            return;
        }
        if(v.getBarType()==1){
            float gap=length/20f,thickness=Math.max(1,gap*.55f);
            for(int i=0;i<20;i++){
                float rise=cross*(.24f+.7f*(i+1)/20f),pos=i*gap+gap*.2f;
                int color=(i+.5f)/20f<=value?fill:back;
                if(horizontal)round(c,pos,v.getInverse()?cross-rise:0,thickness,rise,Math.min(3,thickness/2),color);
                else round(c,v.getInverse()?cross-rise:0,length-pos-thickness,rise,thickness,Math.min(3,thickness/2),color);
            }
            return;
        }
        float thick=Math.max(2,cross*.40f),pad=Math.max(cross*.23f,2),usable=Math.max(0,length-2*pad);
        if(horizontal){
            round(c,pad,(h-thick)/2,usable,thick,thick/2,back);
            round(c,pad,(h-thick)/2,usable*value,thick,thick/2,fill);
            P.setColor(Color.argb(alpha,245,245,245));c.drawCircle(pad+usable*value,h/2,Math.max(thick*.68f,2),P);
        }else{
            round(c,(w-thick)/2,pad,thick,usable,thick/2,back);
            round(c,(w-thick)/2,pad+usable*(1-value),thick,usable*value,thick/2,fill);
            if(v.isMarkerVisible()){P.setColor(Color.argb(alpha,245,245,245));c.drawCircle(w/2,pad+usable*(1-value),Math.max(thick*.68f,2),P);}
        }
    }
}
