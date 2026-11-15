package rubberbigpepper.VolumeControl.redesign;

import android.view.View;
import android.view.WindowManager;

/** Root overlay views need window alpha; child views use ordinary View alpha. */
public final class OverlayVisibility {
    private OverlayVisibility(){}
    public static void apply(WindowManager manager,View view,float alpha){
        float factor=Math.max(0f,Math.min(1f,alpha));
        if(view.getLayoutParams() instanceof WindowManager.LayoutParams){
            WindowManager.LayoutParams params=(WindowManager.LayoutParams)view.getLayoutParams();
            view.setAlpha(1f);
            params.alpha=factor;
            manager.updateViewLayout(view,params);
        }else view.setAlpha(factor);
    }
}
