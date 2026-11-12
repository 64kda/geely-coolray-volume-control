import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;
import java.lang.reflect.*;

/** Shell-only event injector for the isolated API18 emulator. Never in product APK. */
public class TouchGesture {
    static Object manager;static Method inject;static long down;
    static void event(int action,float x,float y)throws Exception{
        MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);
        e.setSource(0x1002);
        try{if(!((Boolean)inject.invoke(manager,e,2)))throw new AssertionError("Injection rejected");}finally{e.recycle();}
    }
    public static void main(String[] args)throws Exception{
        Class<?> type=Class.forName("android.hardware.input.InputManager");
        manager=type.getMethod("getInstance").invoke(null);
        inject=type.getMethod("injectInputEvent",InputEvent.class,int.class);
        float x=Float.parseFloat(args[0]),y=Float.parseFloat(args[1]),delta=Float.parseFloat(args[3]);
        down=SystemClock.uptimeMillis();event(0,x,y);SystemClock.sleep(Integer.parseInt(args[2]));
        if(delta!=0){event(2,x,y+delta);SystemClock.sleep(50);}
        event(args.length>4?3:1,x,y+delta);
        System.out.println("TouchGesture delivered");
    }
}
