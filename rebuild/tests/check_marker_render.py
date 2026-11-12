"""Run the real renderer against a recording Canvas, without an Android install.

This is a drawing-command regression test, not a device screenshot or native UI test.
"""
from pathlib import Path
import json, subprocess, tempfile, sys

BASE=Path(__file__).resolve().parents[2]
JDK=Path('C:/Users/XGamerXFuryX/.jdks/corretto-21.0.12.1/bin')
sources={
'android/graphics/Paint.java': '''package android.graphics; public class Paint {
 public static final int ANTI_ALIAS_FLAG=1; public int color; public Paint(int flags){} public void setColor(int value){color=value;} }''',
'android/graphics/RectF.java': '''package android.graphics; public class RectF {
 public float left,top,right,bottom; public void set(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;} }''',
'android/graphics/Color.java': '''package android.graphics; public class Color {
 public static int alpha(int c){return c>>>24;} public static int argb(int a,int r,int g,int b){return a<<24|r<<16|g<<8|b;} }''',
'android/graphics/Canvas.java': '''package android.graphics; import java.util.*; public class Canvas {
 public final List<String> ops=new ArrayList<String>();
 public void drawRoundRect(RectF r,float rx,float ry,Paint p){ops.add("rect,"+r.left+","+r.top+","+r.right+","+r.bottom+","+rx+","+p.color);}
 public void drawCircle(float x,float y,float radius,Paint p){ops.add("circle,"+x+","+y+","+radius+","+p.color);} }''',
'rubberbigpepper/CommonCtrl/ZoomView.java': '''package rubberbigpepper.CommonCtrl; public class ZoomView {
 public int width=26,height=300,progress=4,type=0; public boolean marker;
 public int getWidth(){return width;} public int getHeight(){return height;} public int getMax(){return 10;}
 public int getProgress(){return progress;} public int getProceedColor(){return 0xff1ed760;} public int getRemainColor(){return 0xe0555555;}
 public int getBarType(){return type;} public boolean getInverse(){return false;} public boolean isMarkerVisible(){return marker;} }''',
'MarkerRenderTest.java': '''import java.util.*; import android.graphics.*;
 import rubberbigpepper.CommonCtrl.ZoomView; import rubberbigpepper.VolumeControl.redesign.ScaleRenderer;
 public class MarkerRenderTest {
 static List<String> draw(ZoomView v,boolean show){v.marker=show;Canvas c=new Canvas();ScaleRenderer.draw(c,v);return c.ops;}
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args){ZoomView v=new ZoomView();
 for(int level:new int[]{0,4,10}){v.progress=level;List<String> idle=draw(v,false),active=draw(v,true),expired=draw(v,false);
 check(idle.size()==(level==0?1:2),"Idle draws only bar segments");
 check(active.size()==idle.size()+1,"Adjustment adds exactly one marker");
 check(active.subList(0,idle.size()).equals(idle)&&expired.equals(idle),"Bar geometry/colors/level changed");}
 v.progress=4;for(String op:draw(v,false))System.out.println("IDLE "+op);for(String op:draw(v,true))System.out.println("ADJUSTING "+op);
 v.type=1;check(draw(v,false).equals(draw(v,true)),"Segmented bar changed");
 v.type=0;v.width=300;v.height=26;check(draw(v,false).equals(draw(v,true)),"Horizontal scale changed");
 v.type=2;
 for(int width:new int[]{10,26,100})for(int length:new int[]{100,300,360})for(int level:new int[]{0,4,10}){
 v.width=width;v.height=length;v.progress=level;List<String> ops=draw(v,false);check(ops.size()==20,"Twenty uniform cells");
 float cw=-1,ch=-1,gap=-1,previous=-1;int active=0;
 for(String op:ops){String[] fields=op.split(",");float l=Float.parseFloat(fields[1]),t=Float.parseFloat(fields[2]),r=Float.parseFloat(fields[3]),b=Float.parseFloat(fields[4]);
 check(l>=0&&r<=width&&t>=0&&b<=length,"Cells inside unchanged width/length");
 if(cw<0){cw=r-l;ch=b-t;}else check(Math.abs(cw-(r-l))<.001&&Math.abs(ch-(b-t))<.001,"Uniform size");
 if(previous>=0){float d=previous-b;if(gap<0)gap=d;else check(Math.abs(gap-d)<.001,"Uniform gap");}previous=t;
 if(Integer.parseInt(fields[6])==0xff1ed760)active++;
 }check(active==level*2,"Fill matches range 0..10");}
 System.out.println("MarkerRender PASS: vertical marker only; bar segments/colors unchanged; min/max; horizontal and segmented unchanged");} }'''
}
with tempfile.TemporaryDirectory(prefix='marker-render-') as tmp:
    root=Path(tmp)
    for name,source in sources.items():
        file=root/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(source,encoding='utf-8')
    renderer=BASE/'rebuild/java/rubberbigpepper/VolumeControl/redesign/ScaleRenderer.java'
    geometry=renderer.with_name('IndicatorGeometry.java')
    subprocess.run([str(JDK/'javac.exe'),'-d',str(root),*[str(root/n) for n in sources],str(renderer),str(geometry)],check=True)
    result=subprocess.check_output([str(JDK/'java.exe'),'-cp',str(root),'MarkerRenderTest'],text=True)
print(result.strip())
ops={name:[line.split(' ',1)[1].split(',') for line in result.splitlines() if line.startswith(name+' ')] for name in ['IDLE','ADJUSTING']}
(BASE/'output/marker-render-validation.json').write_text(json.dumps({'test':'recording Canvas, not Android runtime','passed':True,'ops':ops},indent=2),encoding='utf-8')
sys.path.insert(0,str(BASE/'.analysis-tools'))
from PIL import Image, ImageDraw, ImageFont
preview=Image.new('RGBA',(660,410),(18,18,18,255));font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',17)
for index,(label,state) in enumerate([('Покой','IDLE'),('Регулировка','ADJUSTING'),('После 2000 мс','IDLE')]):
    x=index*220
    ImageDraw.Draw(preview).text((x+25,15),label,font=font,fill='#f5f5f5')
    layer=Image.new('RGBA',preview.size,(0,0,0,0));drawing=ImageDraw.Draw(layer)
    for op in ops[state]:
        values=list(map(float,op[1:-1]));color=int(op[-1])&0xffffffff
        rgba=((color>>16)&255,(color>>8)&255,color&255,(color>>24)&255)
        if op[0]=='rect':
            l,t,r,b,radius=values;drawing.rounded_rectangle((x+85+l,65+t,x+85+r,65+b),radius=radius,fill=rgba)
        else:
            cx,cy,radius=values;drawing.ellipse((x+85+cx-radius,65+cy-radius,x+85+cx+radius,65+cy+radius),fill=rgba)
    preview=Image.alpha_composite(preview,layer)
ImageDraw.Draw(preview).text((20,385),'Модель Canvas-команд, не снимок Android',font=font,fill='#b3b3b3')
preview.convert('RGB').save(BASE/'output/marker-render-preview.png')
