package rubberbigpepper.VolumeControl.redesign;

import android.content.Context;
import android.graphics.Point;
import android.view.Gravity;
import android.view.WindowManager;

/** Bounds only the newly extended range; legacy geometry is left intact. */
public final class IndicatorLayout {
    private IndicatorLayout(){}
    public static void constrain(Context context,WindowManager.LayoutParams lp){
        int length=context.getSharedPreferences("Common",0).getInt("Length",50);
        if(length<=100)return;
        int gravity=context.getSharedPreferences("Common",0).getInt("Gravity",0);
        Point size=new Point();
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
        boolean vertical=gravity==0||gravity==2||(gravity>=4&&gravity<=6)||(gravity>=10&&gravity<=12);
        if(!vertical){
            lp.width=(gravity==1||gravity==3)?size.x:IndicatorGeometry.length(size.x,100);
            return;
        }
        int id=context.getResources().getIdentifier("status_bar_height","dimen","android");
        int status=id==0?0:context.getResources().getDimensionPixelSize(id);
        int available=Math.max(1,size.y-status);
        lp.flags&=~(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        lp.height=Math.min(available,(gravity==0||gravity==2)?size.y:IndicatorGeometry.length(size.y,length));
        int room=available-lp.height,align=lp.gravity&Gravity.VERTICAL_GRAVITY_MASK;
        if(align==Gravity.CENTER_VERTICAL)lp.y=Math.max(-room/2,Math.min(room/2,lp.y));
        else lp.y=Math.max(0,Math.min(room,lp.y));
    }
}
