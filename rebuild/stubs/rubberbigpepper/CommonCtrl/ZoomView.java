// Compile-only declarations. Never packaged in the APK.
package rubberbigpepper.CommonCtrl;
public abstract class ZoomView extends android.view.View {
    public ZoomView(android.content.Context c){super(c);}
    public abstract int getBarType();
    public abstract boolean getInverse();
    public abstract int getMax();
    public abstract int getProgress();
    public abstract int getProceedColor();
    public abstract int getRemainColor();
    public abstract boolean isMarkerVisible();
    public abstract void setMax(int value);
    public abstract void setProgress(int value);
    public abstract void guardWrite(int value);
    public abstract void guardTools();
    public abstract boolean guardToolsEnabled();
}
