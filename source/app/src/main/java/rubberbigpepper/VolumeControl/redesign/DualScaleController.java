package rubberbigpepper.VolumeControl.redesign;

import android.content.*;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import rubberbigpepper.CommonCtrl.ZoomView;

/** Adapter for the recovered service. All callbacks and windows belong to its UI thread. */
public final class DualScaleController {
    private static DualScaleController current;
    private final Context context;
    private final ZoomView primary;
    private final WindowManager wm;
    private final SharedPreferences prefs;
    private final AudioManager audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ZoomView[] views=new ZoomView[2];
    private final AutoHideState[] hides=new AutoHideState[2];
    private final TouchGuardState[] guards=new TouchGuardState[2];
    private final TextView[] labels=new TextView[2];
    private final int[] pointers={-1,-1};
    private final Runnable[] tools=new Runnable[2];
    private final float[] downX=new float[2],downY=new float[2];
    private final DualScaleState state;
    private boolean closed,asleep;
    private int stream,range;
    private final VolumeConfirmation confirmation=new VolumeConfirmation();
    private View primaryRoot;
    private int primaryFlags;
    private final Runnable readback=new Runnable(){public void run(){if(!closed&&!asleep){refresh(true);}}};
    private final BroadcastReceiver screen=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){
            asleep=true;cancelInput();handler.removeCallbacksAndMessages(null);state.sleep();setPrimaryTouchable(false);
            for(int n=0;n<2;n++){guards[n].cancel();hides[n].close();views[n].setVisibility(View.GONE);labels[n].setVisibility(View.GONE);}
        }else if(Intent.ACTION_SCREEN_ON.equals(i.getAction())){
            asleep=false;confirmation.reset();setPrimaryTouchable(true);state.wake(3,2,joint(),state.value(),state.maximum());
            for(int n=0;n<2;n++){views[n].setVisibility(View.VISIBLE);labels[n].setVisibility(View.VISIBLE);createHide(n);}
            refresh(false);
        }
    }};
    private static Object field(Object target,String name)throws Exception{
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private static void call(ZoomView v,String name,Class<?> type,Object value)throws Exception{
        v.getClass().getMethod(name,type).invoke(v,value);
    }
    public static void attach(Context service){
        detach();
        if(service.getSharedPreferences("Common",0).getInt("Gravity",0)!=18)return;
        DualScaleController candidate=null;
        try{
            candidate=new DualScaleController(service);
            current=candidate;
            candidate.open();
        }catch(Exception e){
            if(candidate!=null){candidate.close();try{candidate.primary.getClass().getMethod("startAutoHide").invoke(candidate.primary);}catch(Exception ignored){}}current=null;
            android.util.Log.e("VolumeControl","Cannot create second scale",e);
            Toast.makeText(service,"Не удалось открыть вторую шкалу. Выберите одностороннее расположение.",Toast.LENGTH_LONG).show();
        }
    }
    public static void detach(){if(current!=null){DualScaleController old=current;current=null;old.close();}}
    public static boolean active(Context c){return current!=null&&current.context==c;}
    public static boolean owns(ZoomView v){return current!=null&&(current.views[0]==v||current.views[1]==v);}
    public static void interrupted(ZoomView v){if(owns(v))current.cancelInput();}
    public static void removed(ZoomView v){if(owns(v))detach();}
    public static boolean touch(ZoomView v,MotionEvent e){
        if(current==null)return false;
        for(int n=0;n<2;n++)if(current.views[n]==v){current.input(n,e);return true;}
        return false;
    }
    /** Receiver integration; visual updates cannot send a second audio command. */
    public static void external(ZoomView v,int level){
        if(current!=null&&current.primary==v){current.refresh(true);return;}v.setProgress(level);
    }
    private DualScaleController(Context c)throws Exception{
        context=c;prefs=c.getSharedPreferences("Common",0);audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        primary=(ZoomView)field(c,"m_cZoomView");wm=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
        views[1]=primary;
        views[0]=(ZoomView)primary.getClass().getConstructor(Context.class).newInstance(c);
        state=new DualScaleState(3,2,joint(),primary.getMax(),primary.getProgress());
    }
    private boolean joint(){return prefs.getBoolean("Dual scale together",false);}
    private void open()throws Exception{
        primary.getClass().getMethod("closeAutoHide").invoke(primary);
        for(String property:new String[]{"ProceedColor","RemainColor","BarType"}){
            int value=(Integer)primary.getClass().getMethod("get"+property).invoke(primary);
            call(views[0],"set"+property,Integer.TYPE,value);
        }
        call(views[0],"setInverse",Boolean.TYPE,true);
        views[0].getClass().getMethod("enableMarkerAutoHide").invoke(views[0]);
        View root=(View)field(context,"m_cLayout");primaryRoot=root;
        WindowManager.LayoutParams original=(WindowManager.LayoutParams)root.getLayoutParams();primaryFlags=original.flags;
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(original);
        lp.gravity=(lp.gravity&~Gravity.HORIZONTAL_GRAVITY_MASK)|Gravity.LEFT;
        wm.addView(views[0],lp);
        for(int n=0;n<2;n++){
            labels[n]=new TextView(context);labels[n].setTextColor(primary.getProceedColor());labels[n].setTextSize(prefs.getInt("Percent position",0)==1?24:50);
            WindowManager.LayoutParams text=new WindowManager.LayoutParams(-2,-2,original.type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, -3);
            text.gravity=Gravity.CENTER;wm.addView(labels[n],text);
            createHide(n);createGuard(n);
        }
        IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(screen,filter);refresh(false);
        views[0].post(new Runnable(){public void run(){position(0);position(1);}});
    }
    private void createHide(final int n){
        if(n==1&&joint()){hides[1]=hides[0];return;}
        hides[n]=new AutoHideState(new AutoHideState.Clock(){public long now(){return SystemClock.elapsedRealtime();}},
            new AutoHideState.Scheduler(){public void cancel(Runnable r){handler.removeCallbacks(r);}public void schedule(Runnable r,long delay){handler.postDelayed(r,delay);}},
            new AutoHideState.Output(){public void visibility(float alpha){for(int j=0;j<2;j++)if(j==n||joint()){OverlayVisibility.apply(wm,views[j],alpha);if(labels[j]!=null)OverlayVisibility.apply(wm,labels[j],alpha);}}},
            prefs.getBoolean("Auto hide",false),AutoHideController.read(prefs,"Auto hide delay ms",true),AutoHideController.read(prefs,"Auto hide fade ms",false));
    }
    private void createGuard(final int n){
        guards[n]=new TouchGuardState(new TouchGuardState.Clock(){public long now(){return SystemClock.uptimeMillis();}},
            new TouchGuardState.Scheduler(){public void cancel(Runnable r){handler.removeCallbacks(r);}public void post(Runnable r,long delay){handler.postDelayed(r,delay);}},
            new TouchGuardState.Output(){
                public int level(){return state.value();}
                public void write(int v){if(state.owns(1<<n,pointers[n]))command(v);}
                public void ready(boolean on){
                    if(on){if(!state.confirm(1<<n,pointers[n])){guards[n].cancel();return;}show(n,true);}
                    else {if(state.release(1<<n,pointers[n]))releaseHide(n);}
                    views[n].setActivated(on&&TouchGuardController.feedback(prefs));views[n].invalidate();
                }
                public void tools(){primary.guardTools();}
            },TouchGuardController.hold(prefs),ViewConfiguration.get(context).getScaledTouchSlop());
    }
    private void show(int n,boolean held){
        for(int j=0;j<2;j++)if(j==n||joint()){if(held)hides[j].hold();else hides[j].activity();}
    }
    private void releaseHide(int n){for(int j=0;j<2;j++)if(j==n||joint())hides[j].release();}
    private void input(int n,MotionEvent e){
        if(closed||asleep)return;
        int action=e.getActionMasked();ZoomView v=views[n];
        boolean guarded=TouchGuardController.enabled(prefs);
        if(action==MotionEvent.ACTION_DOWN){
            guards[n].cancel();refresh(false);pointers[n]=e.getPointerId(0);
            if(guarded){guards[n].configure(TouchGuardController.hold(prefs));
                guards[n].down(e.getX(),e.getY(),e.getEventTime(),range,v.getHeight(),true,primary.guardToolsEnabled());}
            else if(state.confirm(1<<n,pointers[n])){
                show(n,true);absolute(n,e,0);downX[n]=e.getX();downY[n]=e.getY();
                final int side=n;
                if(primary.guardToolsEnabled()){
                    tools[n]=new Runnable(){public void run(){if(!closed&&!asleep&&state.owns(1<<side,pointers[side])){
                        guards[side].cancel();primary.guardTools();
                    }}};handler.postDelayed(tools[n],3000);
                }
            }
        }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||
            action==MotionEvent.ACTION_POINTER_UP&&e.getPointerId(e.getActionIndex())==pointers[n]){
            if(tools[n]!=null)handler.removeCallbacks(tools[n]);
            boolean owned=state.owns(1<<n,pointers[n]);guards[n].cancel();state.release(1<<n,pointers[n]);if(owned)releaseHide(n);pointers[n]=-1;refresh(false);
        }else if(action==MotionEvent.ACTION_MOVE){
            int index=e.findPointerIndex(pointers[n]);if(index>=0&&tools[n]!=null){float dx=e.getX(index)-downX[n],dy=e.getY(index)-downY[n];float slop=ViewConfiguration.get(context).getScaledTouchSlop();if(dx*dx+dy*dy>slop*slop)handler.removeCallbacks(tools[n]);}
            if(index<0){guards[n].cancel();state.cancel();releaseHide(n);return;}
            if(guarded){for(int h=0;h<e.getHistorySize();h++)guards[n].move(e.getHistoricalX(index,h),e.getHistoricalY(index,h),e.getHistoricalEventTime(h));
                guards[n].move(e.getX(index),e.getY(index),e.getEventTime());}
            else if(state.owns(1<<n,pointers[n]))absolute(n,e,index);
        }
    }
    private void absolute(int n,MotionEvent e,int index){int h=views[n].getHeight();if(h>0)command(Math.round((1f-e.getY(index)/h)*range));}
    private int resolve(){return VolumeMath.displayStream(prefs.getInt("Stream type",3),audio.getMode(),audio.isMusicActive());}
    private void command(int value){
        try{
            if(resolve()!=stream||audio.getStreamMaxVolume(stream)!=range){cancelInput();refresh(false);return;}
            int desired=Math.max(0,Math.min(range,value));confirmation.written(desired,SystemClock.uptimeMillis());
            primary.guardWrite(desired);render(desired,range);
            handler.removeCallbacks(readback);handler.postDelayed(readback,350);
        }catch(RuntimeException e){confirmation.reset();handler.removeCallbacks(readback);cancelInput();refresh(false);}
    }
    private void refresh(boolean external){
        if(closed||asleep)return;
        try{
            int next=resolve(),max=audio.getStreamMaxVolume(next),level=audio.getStreamVolume(next);
            if(next!=stream||max!=range){cancelInput();confirmation.reset();}
            stream=next;range=max;
            int result=confirmation.observe(level,SystemClock.uptimeMillis());
            if(result==VolumeConfirmation.STALE)return;
            if(external&&result==VolumeConfirmation.EXTERNAL){cancelInput();for(AutoHideState h:hides)h.activity();}
            render(level,max);
        }catch(RuntimeException e){android.util.Log.w("VolumeControl","Volume read unavailable",e);}
    }
    private void render(int value,int max){
        state.actual(value,max);
        for(int n=0;n<2;n++){views[n].setMax(max);views[n].setProgress(state.value());
            labels[n].setText(VolumeMath.percent(state.value(),max)+"%");
            position(n);}
    }
    private void position(int n){
        if(closed||labels[n]==null||labels[n].getWindowToken()==null)return;
        WindowManager.LayoutParams lp=(WindowManager.LayoutParams)labels[n].getLayoutParams();
        int[] xy=new int[2];views[n].getLocationOnScreen(xy);
        if(prefs.getInt("Percent position",0)==1){
            lp.gravity=Gravity.TOP|Gravity.LEFT;
            int spec=View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);labels[n].measure(spec,spec);
            android.graphics.Rect safe=new android.graphics.Rect();views[n].getWindowVisibleDisplayFrame(safe);
            int[] pos=IndicatorGeometry.label(xy[0],xy[1],views[n].getWidth(),views[n].getHeight(),labels[n].getMeasuredWidth(),labels[n].getMeasuredHeight(),safe.left,safe.top,safe.right,safe.bottom);
            lp.x=pos[0];lp.y=pos[1];lp.flags|=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        }else{lp.gravity=Gravity.CENTER;lp.x=0;lp.y=0;}
        try{wm.updateViewLayout(labels[n],lp);}catch(IllegalArgumentException ignored){}
    }
    private void setPrimaryTouchable(boolean enabled){
        if(primaryRoot==null||primaryRoot.getWindowToken()==null)return;
        WindowManager.LayoutParams lp=(WindowManager.LayoutParams)primaryRoot.getLayoutParams();
        lp.flags=enabled?primaryFlags:primaryFlags|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        try{wm.updateViewLayout(primaryRoot,lp);}catch(IllegalArgumentException ignored){}
    }
    private void cancelInput(){for(int n=0;n<2;n++)if(guards[n]!=null){if(tools[n]!=null)handler.removeCallbacks(tools[n]);guards[n].cancel();pointers[n]=-1;}state.cancel();}
    private void close(){
        if(closed)return;closed=true;setPrimaryTouchable(true);cancelInput();handler.removeCallbacksAndMessages(null);
        try{context.unregisterReceiver(screen);}catch(IllegalArgumentException ignored){}
        for(int n=0;n<2;n++){
            if(hides[n]!=null)hides[n].close();
            if(labels[n]!=null)try{wm.removeView(labels[n]);}catch(IllegalArgumentException ignored){}
        }
        try{wm.removeView(views[0]);}catch(IllegalArgumentException ignored){}
    }
}
