import com.android.uiautomator.core.*;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;
import android.graphics.Rect;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** One UIAutomation connection, real fresh queries and unique evidence files. */
public class VolumeUiTest extends UiAutomatorTestCase {
    private static final String PKG="rubberbigpepper.VolumeControl.redesign";
    private final String[] titles={"Управление","Шкала","Звук","Эквалайзер","Работа приложения","О приложении"};
    private final String[] markers={"SpinnerStreamType","SeekBarSize","CheckBoxShowNotification","CheckBoxEnableEqMix","CheckBoxDelay","ButtonDonation"};
    private final String[] ids={"SeekBarSize","ImageViewColor","SeekBarTransparent","ImageViewBackgroundColor","SeekBarBackgroundTransparent","SpinnerGravity","SeekBarLeng","CheckBoxFullScreen","EditTextXPos","EditTextYPos","SpinnerType","SpinnerStreamType","SpinnerActionOn0","CheckBoxShowNotification","CheckBoxNotificationSound","CheckBoxDelay","CheckBoxAutoHide","CheckBoxEnableEqMix","ImageViewActiveColor","SeekBarActiveTransparent","ImageViewInactiveColor","SeekBarInactiveTransparent","CheckBoxRunAfterReboot"};
    private UiObject id(String name){return new UiObject(new UiSelector().resourceId(PKG+":id/"+name));}
    private UiObject text(String label){return new UiObject(new UiSelector().text(label));}
    private void tab(int n)throws Exception{
        UiObject button=new UiObject(new UiSelector().text(titles[n]).className("android.widget.Button").instance(0));
        assertTrue("Missing tab "+n,button.waitForExists(2000));button.click();
        if(n==5){
            assertTrue("About page missing",new UiObject(new UiSelector().textStartsWith("Версия 2.7.1")).waitForExists(2000));
            assertFalse("Old donation action still visible",id("ButtonDonation").exists());
            return;
        }
        if(!id(markers[n]).waitForExists(1500))button.click();
        assertTrue("Tab did not open "+n,id(markers[n]).waitForExists(2000));
    }
    private void scrollTo(String name)throws Exception{
        for(int n=0;n<5&&!id(name).exists();n++)getUiDevice().swipe(40,620,40,240,20);
        assertTrue("Missing setting "+name,id(name).exists());
    }
    private Map<String,String> prefs()throws Exception{
        HashMap<String,String> result=new HashMap<String,String>();
        FileInputStream input=new FileInputStream("/data/data/"+PKG+"/shared_prefs/Common.xml");
        try{XmlPullParser p=Xml.newPullParser();p.setInput(input,"UTF-8");for(int type=p.getEventType();type!=XmlPullParser.END_DOCUMENT;type=p.next())
            if(type==XmlPullParser.START_TAG&&p.getAttributeValue(null,"name")!=null)result.put(p.getAttributeValue(null,"name"),p.getAttributeValue(null,"value"));}
        finally{input.close();}return result;
    }
    private String command(String...args)throws Exception{
        Process process=new ProcessBuilder(args).redirectErrorStream(true).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];
        InputStream stream=process.getInputStream();for(int n;(n=stream.read(buf))!=-1;)out.write(buf,0,n);
        assertEquals("Command failed "+Arrays.toString(args),0,process.waitFor());return out.toString("UTF-8");
    }
    private void toggleAutohide()throws Exception{Rect b=id("CheckBoxAutoHide").getBounds();getUiDevice().click(b.right-30,b.centerY());getUiDevice().waitForIdle(300);}
    private void waitPref(String name,String value)throws Exception{
        long until=System.currentTimeMillis()+3000;
        while(System.currentTimeMillis()<until){if(value.equals(prefs().get(name)))return;Thread.sleep(50);}
        assertEquals("Preference not saved: "+name,value,prefs().get(name));
    }
    private void screen(String name){assertTrue("Screenshot failed",getUiDevice().takeScreenshot(new File("/data/local/tmp/volume-stock-"+name+".png")));}
    private int[] overlay()throws Exception{
        for(int attempt=0;attempt<15;attempt++){
        String windows=command("dumpsys","window","windows");
        for(String block:windows.split("Window #"))if(block.contains("package="+PKG+" appop=SYSTEM_ALERT_WINDOW")){
            Matcher m=Pattern.compile("mFrame=\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]").matcher(block);
            if(m.find()){int[] b={Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4))};if(b[2]-b[0]>100&&b[3]-b[1]>100)return b;}
        }Thread.sleep(100);}
        throw new AssertionError("Effects overlay missing");
    }
    public void testApp()throws Exception{
        Configurator.getInstance().setWaitForIdleTimeout(100).setWaitForSelectorTimeout(1000);
        getUiDevice().wakeUp();getUiDevice().pressMenu();
        command("am","start","-W","-n",PKG+"/rubberbigpepper.VolumeControl.MainWindow");
        assertTrue(text("Управление").waitForExists(5000));
        Set<String> seen=new HashSet<String>();
        for(int n=0;n<6;n++){
            tab(n);
            for(int step=0;step<5;step++){
                for(String name:ids)if(id(name).exists())seen.add(name);
                if(n!=1&&n!=3)break;
                getUiDevice().swipe(40,620,40,240,20);
            }
        }
        assertEquals("Original controls not preserved: "+seen,23,seen.size());
        tab(0);screen("home");Map<String,String> before=prefs();
        tab(4);boolean original=id("CheckBoxAutoHide").isChecked();toggleAutohide();
        assertEquals("Checkbox did not change",!original,id("CheckBoxAutoHide").isChecked());
        command("am","force-stop",PKG);command("am","start","-W","-n",PKG+"/rubberbigpepper.VolumeControl.MainWindow");
        tab(4);assertEquals("Setting did not persist",!original,id("CheckBoxAutoHide").isChecked());
        toggleAutohide();assertEquals("Setting not restored",original,id("CheckBoxAutoHide").isChecked());
        tab(1);id("ImageViewColor").click();
        assertTrue(text("Красный").waitForExists(1000));assertTrue(text("Зелёный").exists());assertTrue(text("Синий").exists());screen("color");
        text("Цветовой круг").click();assertTrue(text("Выбор цвета").exists());getUiDevice().pressBack();text("Отмена").click();
        assertEquals("Cancel changed color",before.get("Main color"),prefs().get("Main color"));
        scrollTo("SpinnerType");id("SpinnerType").click();assertTrue(text("Сплошная полоса").exists());assertTrue(text("Столбики уровня").exists());
        String originalType=before.get("Bar type"),alternateType="0".equals(originalType)?"1":"0";
        text("1".equals(alternateType)?"Столбики уровня":"Сплошная полоса").click();waitPref("Bar type",alternateType);screen("bars");
        id("SpinnerType").click();text("1".equals(originalType)?"Столбики уровня":"Сплошная полоса").click();waitPref("Bar type",originalType);
        for(String action:new String[]{"MIXER","EQUALIZER"}){
            command("am","startservice","-n",PKG+"/rubberbigpepper.CommonCtrl.VolumeControlService","-a","local.volumecontrol.redesign."+action);
            int[] bounds=overlay();screen(action.toLowerCase(java.util.Locale.US));getUiDevice().click(bounds[2]-20,bounds[1]+20);
        }
        Map<String,String> after=prefs();for(String name:new String[]{"Main color","Background color","Gravity","Size","Auto hide","Bar type","Stream type"})assertEquals("Navigation changed "+name,before.get(name),after.get(name));
        tab(0);screen("final");
        FileWriter result=new FileWriter("/data/local/tmp/volume-stock-result.txt");result.write("PASS API18: 6 tabs; 23 controls; autohide process persistence; RGB and color circle; cancel; both scale types; mixer and equalizer overlays; settings restored\n");result.close();
    }
}
