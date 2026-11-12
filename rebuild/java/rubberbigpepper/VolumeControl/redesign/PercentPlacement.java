package rubberbigpepper.VolumeControl.redesign;

import android.graphics.Rect;
import android.content.Context;
import android.view.*;
import android.widget.TextView;
import rubberbigpepper.CommonCtrl.ZoomView;

/** Positions the existing percentage window. Its owner retains the existing timer. */
public final class PercentPlacement {
    private PercentPlacement(){}
    public static boolean nearby(ZoomView scale){
        return nearby(scale.getContext());
    }
    private static boolean nearby(Context context){return context.getSharedPreferences("Common",0).getInt("Percent position",0)==1;}
    public static boolean systemNotification(Context context,boolean requested){return requested&&!nearby(context);}
    private static void position(TextView text,WindowManager.LayoutParams lp,ZoomView scale){
        if(!nearby(scale))return;
        text.setTextSize(24);
        text.setText(VolumeMath.percent(scale.getProgress(),scale.getMax())+"%");
        Rect safe=new Rect();scale.getWindowVisibleDisplayFrame(safe);
        int[] xy=new int[2];scale.getLocationOnScreen(xy);
        int spec=View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        text.measure(spec,spec);
        int tw=Math.min(text.getMeasuredWidth(),Math.max(1,safe.width()));
        int th=Math.min(text.getMeasuredHeight(),Math.max(1,safe.height()));
        int[] p=IndicatorGeometry.label(xy[0],xy[1],scale.getWidth(),scale.getHeight(),tw,th,safe.left,safe.top,safe.right,safe.bottom);
        lp.width=tw;lp.height=th;lp.gravity=Gravity.TOP|Gravity.LEFT;
        lp.flags|=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        lp.x=p[0];lp.y=p[1];
    }
    public static void add(WindowManager wm,TextView text,WindowManager.LayoutParams lp,ZoomView scale){
        position(text,lp,scale);wm.addView(text,lp);
    }
    public static void update(WindowManager wm,TextView text,ZoomView scale){
        if(!nearby(scale))return;
        WindowManager.LayoutParams lp=(WindowManager.LayoutParams)text.getLayoutParams();
        position(text,lp,scale);wm.updateViewLayout(text,lp);
    }
}
