package rubberbigpepper.VolumeControl.redesign;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import rubberbigpepper.CommonCtrl.ZoomView;

/** Static, inset capsule outline. Never paints a rectangular touch target. */
public final class GuardFeedback {
    private static final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF bounds=new RectF();
    public static void draw(Canvas canvas,ZoomView view){
        if(!view.isActivated())return;
        float w=view.getWidth(),h=view.getHeight(),cross=Math.min(w,h);
        if(cross<4)return;
        float inset=cross*.12f,radius=Math.max(0,(cross-2*inset)/2);
        bounds.set(inset,inset,w-inset,h-inset);
        int color=view.getProceedColor(),alpha=color>>>24;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor((color&0xffffff)|((alpha*28/255)<<24));
        paint.setStrokeWidth(cross*.12f);
        canvas.drawRoundRect(bounds,radius,radius,paint);
        paint.setColor((color&0xffffff)|((alpha*105/255)<<24));
        paint.setStrokeWidth(Math.max(1,cross*.035f));
        canvas.drawRoundRect(bounds,radius,radius,paint);
    }
}
