"""Execute the real platform adapters against deterministic Android stubs.

This validates our geometry and window calls, not vendor WindowManager behavior.
"""
from pathlib import Path
import subprocess, tempfile
BASE=Path(__file__).resolve().parents[2]
JDK=Path('C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin')
sources={
'android/content/SharedPreferences.java': '''package android.content; import java.util.*;
public class SharedPreferences { public Map<String,Integer> values=new HashMap<String,Integer>();
 public int getInt(String key,int fallback){Integer v=values.get(key);return v==null?fallback:v;} }''',
'android/content/Context.java': '''package android.content; public class Context {
 public static final String WINDOW_SERVICE="window"; public SharedPreferences prefs=new SharedPreferences();
 public android.view.WindowManager wm=new android.view.WindowManager();
 public SharedPreferences getSharedPreferences(String s,int mode){return prefs;}
 public Object getSystemService(String s){return wm;}
 public android.content.res.Resources getResources(){return new android.content.res.Resources();} }''',
'android/content/res/Resources.java': '''package android.content.res; public class Resources {
 public int getIdentifier(String a,String b,String c){return 1;} public int getDimensionPixelSize(int id){return 24;} }''',
'android/graphics/Point.java': 'package android.graphics; public class Point {public int x,y;}',
'android/graphics/Rect.java': '''package android.graphics; public class Rect {
 public int left,top,right,bottom; public int width(){return right-left;}public int height(){return bottom-top;} }''',
'android/view/Gravity.java': '''package android.view; public class Gravity {
 public static final int TOP=48,LEFT=3,CENTER_VERTICAL=16,VERTICAL_GRAVITY_MASK=112; }''',
'android/view/View.java': '''package android.view; public class View {
 public static class MeasureSpec {public static final int UNSPECIFIED=0;public static int makeMeasureSpec(int a,int b){return 0;}} }''',
'android/view/WindowManager.java': '''package android.view; import android.graphics.Point;
public class WindowManager {
 public int adds,updates;public Object last;
 public static class LayoutParams {public static final int FLAG_LAYOUT_IN_SCREEN=256,FLAG_LAYOUT_NO_LIMITS=512,FLAG_NOT_TOUCHABLE=16;
 public int x,y,width=-2,height=-2,gravity=17,flags=40;}
 public static class Display {public void getSize(Point p){p.x=1024;p.y=576;}}
 public Display getDefaultDisplay(){return new Display();}
 public void addView(android.widget.TextView text,LayoutParams lp){adds++;text.lp=lp;last=text;}
 public void updateViewLayout(android.widget.TextView text,LayoutParams lp){updates++;if(last!=text)throw new AssertionError("Different window");} }''',
'android/widget/TextView.java': '''package android.widget; public class TextView extends android.view.View {
 public String text="";public float size=50;public android.view.WindowManager.LayoutParams lp;
 public void setTextSize(float n){size=n;}public void setText(String s){text=s;}
 public void measure(int a,int b){} public int getMeasuredWidth(){return 80;}public int getMeasuredHeight(){return 32;}
 public Object getLayoutParams(){return lp;} }''',
'rubberbigpepper/CommonCtrl/ZoomView.java': '''package rubberbigpepper.CommonCtrl;
public class ZoomView {public android.content.Context context=new android.content.Context();
 public int x=980,y=150,w=26,h=340,value=11,max=15;
 public android.content.Context getContext(){return context;}public int getProgress(){return value;}public int getMax(){return max;}
 public int getWidth(){return w;}public int getHeight(){return h;}
 public void getLocationOnScreen(int[] xy){xy[0]=x;xy[1]=y;}
 public void getWindowVisibleDisplayFrame(android.graphics.Rect r){r.left=0;r.top=24;r.right=1024;r.bottom=576;} }''',
'IndicatorLayoutTest.java': '''import android.content.*;import android.view.*;import android.widget.*;
import rubberbigpepper.CommonCtrl.ZoomView;import rubberbigpepper.VolumeControl.redesign.*;
public class IndicatorLayoutTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args){
 Context c=new Context();
 for(int gravity:new int[]{0,2,4,5,6,10,11,12})for(int width:new int[]{0,26,90})for(int length:new int[]{0,50,100,130})for(int offset:new int[]{-999,0,999}){
 c.prefs.values.put("Gravity",gravity);c.prefs.values.put("Length",length);
 WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.width=width;lp.height=IndicatorGeometry.length(576,length);lp.y=offset;
 lp.gravity=gravity==5||gravity==11?19:51;int oldHeight=lp.height,oldFlags=lp.flags;
 IndicatorLayout.constrain(c,lp);check(lp.width==width,"Width changed");
 if(length<=100)check(lp.height==oldHeight&&lp.y==offset&&lp.flags==oldFlags,"Legacy geometry changed");
 else check(lp.height<=552&&lp.y<=552-lp.height&&lp.y>=-(552-lp.height)/2,"New range outside usable height");
 }
 for(int g:new int[]{1,3,7,8,9,13,14,15}){c.prefs.values.put("Gravity",g);c.prefs.values.put("Length",130);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.height=26;IndicatorLayout.constrain(c,lp);check(lp.height==26,"Horizontal thickness changed");check(lp.width==(g==1||g==3?1024:512),"Horizontal range changed");}
 ZoomView scale=new ZoomView();WindowManager wm=scale.context.wm;TextView text=new TextView();WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
 PercentPlacement.add(wm,text,lp,scale);check(lp.gravity==17&&text.size==50,"Existing default changed");
 check(PercentPlacement.systemNotification(scale.context,true),"Legacy notification disabled");
 scale.context.prefs.values.put("Percent position",1);PercentPlacement.update(wm,text,scale);
 check(!PercentPlacement.systemNotification(scale.context,true),"Duplicate system UI requested in nearby mode");
 check(wm.adds==1&&wm.updates==1&&text.text.equals("73%"),"Existing window or percentage range changed");
 check(lp.x>=0&&lp.x+lp.width<=1024&&lp.y>=24,"Right label bounds");
 scale.x=0;scale.y=260;scale.h=200;scale.value=0;PercentPlacement.update(wm,text,scale);check(lp.y==220&&text.text.equals("0%"),"Label not relative to moved scale");
 scale.value=15;PercentPlacement.update(wm,text,scale);check(text.text.equals("100%"),"Maximum");
 for(int n=0;n<500;n++){scale.value=n%16;PercentPlacement.update(wm,text,scale);}check(wm.adds==1,"Duplicate percentage windows");
 System.out.println("IndicatorLayout PASS: 288 vertical combinations; legacy geometry; horizontal cap; same percent window; 500 updates; non-100 range");
 } }'''
}
with tempfile.TemporaryDirectory(prefix='layout-',dir=BASE/'rebuild') as tmp:
    root=Path(tmp)
    for name,source in sources.items():
        p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf-8')
    helpers=BASE/'rebuild/java/rubberbigpepper/VolumeControl/redesign'
    subprocess.run([str(JDK/'javac.exe'),'-d',str(root),*[str(root/n) for n in sources],*[str(helpers/n) for n in ['IndicatorGeometry.java','IndicatorLayout.java','PercentPlacement.java','VolumeMath.java']]],check=True)
    subprocess.run([str(JDK/'java.exe'),'-cp',str(root),'IndicatorLayoutTest'],check=True)
