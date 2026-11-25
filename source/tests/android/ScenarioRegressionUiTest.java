import com.android.uiautomator.core.*;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import java.io.*;
import java.util.regex.*;

/** Runtime checks for app lifecycle, visible autohide disable behavior, and mixer audio writes. */
public final class ScenarioRegressionUiTest extends UiAutomatorTestCase {
    private static final String PKG="rubberbigpepper.VolumeControl.redesign";
    private UiObject text(String value){return new UiObject(new UiSelector().text(value));}
    private UiObject id(String value){return new UiObject(new UiSelector().resourceId(PKG+":id/"+value));}
    private boolean waitGone(UiObject object,long timeout)throws Exception{
        long until=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<until){if(!object.exists())return true;Thread.sleep(100);}return !object.exists();
    }
    private String command(String...args)throws Exception{
        Process p=new ProcessBuilder(args).redirectErrorStream(true).start();
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];
        InputStream in=p.getInputStream();for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);
        assertEquals("Command failed: "+args[0],0,p.waitFor());return out.toString("UTF-8");
    }
    private void launch()throws Exception{
        command("am","start","-W","-n",PKG+"/rubberbigpepper.VolumeControl.MainWindow");
        assertTrue("App did not open its main screen",text("Управление").waitForExists(5000));
    }
    private void tab(String name)throws Exception{
        UiScrollable nav=new UiScrollable(new UiSelector().className("android.widget.HorizontalScrollView"));
        nav.setAsHorizontalList();assertTrue("Tab missing: "+name,nav.scrollIntoView(new UiSelector().text(name)));
        text(name).click();
    }
    private void workPage()throws Exception{tab("Работа приложения");}
    private void homePage()throws Exception{tab("Управление");}
    private void scrollWorkTo(String value)throws Exception{
        UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
        assertTrue("Setting is missing: "+value,page.scrollIntoView(new UiSelector().text(value)));
    }
    private UiObject autoHide(){return new UiObject(new UiSelector().text("Скрывать шкалу после использования").className("android.widget.CheckBox"));}
    private int[] scaleFrame()throws Exception{
        String windows=command("dumpsys","window","windows");
        for(String block:windows.split("Window #"))if(block.contains("package="+PKG)&&
                (block.contains("ty=2010")||block.contains("type=2010"))){
            Matcher m=Pattern.compile("mFrame=\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]").matcher(block);
            if(m.find()){int[] b={Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4))};
                if(b[2]-b[0]>100&&b[3]-b[1]>100)return b;}
        }
        throw new AssertionError("Floating scale window is missing");
    }
    private int greenInScale(String name)throws Exception{
        File file=new File("/data/local/tmp/volume-flow-"+name+".png");
        assertTrue("Could not capture screen",getUiDevice().takeScreenshot(file));
        Bitmap image=BitmapFactory.decodeFile(file.getAbsolutePath());assertNotNull("Screenshot could not be decoded",image);
        int[] b=scaleFrame();int count=0;
        for(int y=Math.max(0,b[1]);y<Math.min(image.getHeight(),b[3]);y++)for(int x=Math.max(0,b[0]);x<Math.min(image.getWidth(),b[2]);x++){
            int c=image.getPixel(x,y),r=(c>>16)&255,g=(c>>8)&255,blue=c&255;
            if(g>70&&g>r*1.4&&g>blue*1.3)count++;
        }
        image.recycle();return count;
    }
    public void testLaunchAndClose()throws Exception{
        Configurator.getInstance().setWaitForIdleTimeout(150);
        command("am","force-stop",PKG);launch();
        getUiDevice().pressHome();assertTrue("Home screen did not replace the app",waitGone(text("Управление"),3000));
        launch();assertTrue("App did not reopen after close",text("Управление").exists());
    }
    public void testAutoHideOffLeavesScaleVisible()throws Exception{
        Configurator.getInstance().setWaitForIdleTimeout(150);
        launch();workPage();scrollWorkTo("Скрывать шкалу после использования");UiObject toggle=autoHide();assertTrue("Autohide setting is missing",toggle.waitForExists(2000));
        boolean wasEnabled=toggle.isChecked();
        try{
            if(!wasEnabled){toggle.click();assertTrue("Autohide could not be enabled",toggle.isChecked());}
            homePage();int initial=greenInScale("autohide-initial");assertTrue("Scale was not visible before timeout",initial>0);
            Thread.sleep(2600);int hidden=greenInScale("autohide-hidden");assertEquals("Scale did not hide while autohide was enabled",0,hidden);
            workPage();toggle=autoHide();toggle.click();assertFalse("Autohide could not be disabled",toggle.isChecked());
            homePage();int reopened=greenInScale("autohide-disabled");assertTrue("Scale did not reappear after disabling autohide",reopened>0);
            Thread.sleep(2600);int after=greenInScale("autohide-disabled-wait");assertTrue("Scale hid after autohide was disabled",after>0);
        }finally{
            workPage();scrollWorkTo("Скрывать шкалу после использования");toggle=autoHide();if(toggle.isChecked()!=wasEnabled)toggle.click();
        }
    }
    public void testScaleStyleCanChangeAndRestore()throws Exception{
        launch();tab("Шкала");UiObject style=id("SpinnerType");
        UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
        assertTrue("Scale type selector is missing",page.scrollIntoView(new UiSelector().resourceId(PKG+":id/SpinnerType")));
        UiObject selected=style.getChild(new UiSelector().resourceId("android:id/text1"));
        String original=selected.getText();assertTrue("Unknown current scale style: "+original,
                original.equals("Сплошная полоса")||original.equals("Столбики уровня")||original.equals("Равномерные сегменты"));
        String alternate=original.equals("Сплошная полоса")?"Столбики уровня":"Сплошная полоса";
        style.click();assertTrue("Alternate style is not offered",text(alternate).waitForExists(1500));text(alternate).click();
        assertEquals("Scale style did not change",alternate,style.getChild(new UiSelector().resourceId("android:id/text1")).getText());
        style.click();text(original).click();assertEquals("Scale style was not restored",original,
                style.getChild(new UiSelector().resourceId("android:id/text1")).getText());
    }
    private UiObject guardToggle()throws Exception{
        scrollWorkTo("Защита от случайного касания");
        return new UiObject(new UiSelector().text("Защита от случайного касания").className("android.widget.CheckBox"));
    }
    private UiObject guardHoldSpinner()throws Exception{
        scrollWorkTo("Удерживать перед регулировкой");Rect label=text("Удерживать перед регулировкой").getBounds();
        UiObject best=null;int nearest=Integer.MAX_VALUE;
        for(int i=0;i<12;i++){
            UiObject candidate=new UiObject(new UiSelector().className("android.widget.Spinner").instance(i));
            if(!candidate.exists())break;Rect bounds=candidate.getBounds();int gap=bounds.top-label.bottom;
            if(gap>=0&&gap<nearest){best=candidate;nearest=gap;}
        }
        assertNotNull("Guard hold selector is missing",best);return best;
    }
    private String spinnerValue(UiObject spinner)throws Exception{return spinner.getChild(new UiSelector().resourceId("android:id/text1")).getText();}
    private void choose(UiObject spinner,String value)throws Exception{
        spinner.click();assertTrue("Spinner option missing: "+value,text(value).waitForExists(1500));text(value).click();
        assertEquals("Spinner did not select "+value,value,spinnerValue(spinner));
    }
    private void gesture(int hold,int delta)throws Exception{
        int[] b=scaleFrame();int x=(b[0]+b[2])/2,y=(b[1]+b[3])/2;
        String command="CLASSPATH=/data/local/tmp/touch-inject.jar app_process /system/bin TouchGesture "+x+" "+y+" "+hold+" "+delta;
        assertTrue("Touch injector did not deliver MotionEvents",command("sh","-c",command).contains("TouchGesture delivered"));
    }
    public void testProtectedAndUnprotectedVolumeGestures()throws Exception{
        launch();workPage();UiObject protection=guardToggle();UiObject hold=guardHoldSpinner();
        boolean previousProtection=protection.isChecked();String previousHold=spinnerValue(hold);
        try{
            choose(hold,"250 мс");if(!protection.isChecked())protection.click();assertTrue("Protection did not enable",protection.isChecked());
            homePage();int[] before=musicLevel();int direction=before[0]>1?50:-50;
            gesture(20,0);assertEquals("Short protected touch changed volume",before[0],musicLevel()[0]);
            gesture(500,0);assertEquals("Stationary protected hold changed volume",before[0],musicLevel()[0]);
            gesture(500,direction);int guarded=musicLevel()[0];assertFalse("Confirmed protected drag did not adjust volume",before[0]==guarded);
            workPage();protection=guardToggle();protection.click();assertFalse("Protection did not disable",protection.isChecked());
            homePage();int[] unguardedBefore=musicLevel();direction=unguardedBefore[0]>1?50:-50;
            gesture(20,direction);assertFalse("Unprotected touch did not adjust volume",unguardedBefore[0]==musicLevel()[0]);
        }finally{
            workPage();protection=guardToggle();if(protection.isChecked()!=previousProtection)protection.click();
            hold=guardHoldSpinner();if(!spinnerValue(hold).equals(previousHold))choose(hold,previousHold);
        }
    }
    private int[] musicLevel()throws Exception{
        String audio=command("dumpsys","audio");
        Matcher block=Pattern.compile("- STREAM_MUSIC:(.*?)(?=\\n\\s*- STREAM_|\\z)",Pattern.DOTALL).matcher(audio);
        assertTrue("Music stream not found in dumpsys audio",block.find());
        Matcher current=Pattern.compile("Current: [^:]+: (\\d+)").matcher(block.group(1));
        Matcher max=Pattern.compile("Max:\\s*(\\d+)").matcher(block.group(1));
        assertTrue("Music stream current level unavailable",current.find());assertTrue("Music stream max level unavailable",max.find());
        return new int[]{Integer.parseInt(current.group(1)),Integer.parseInt(max.group(1))};
    }
    private UiObject musicMixerSlider(){return new UiObject(new UiSelector().descriptionContains("Музыка и мультимедиа"));}
    private void moveMixer(UiObject slider,int current,int target,int max)throws Exception{
        Rect b=slider.getBounds();int travel=Math.max(1,b.width()-24);
        int start=b.left+12+travel*current/max,end=b.left+12+travel*target/max;
        getUiDevice().swipe(start,b.centerY(),end,b.centerY(),20);Thread.sleep(300);
    }
    public void testMixerOpensAdjustsAndCloses()throws Exception{
        Configurator.getInstance().setWaitForIdleTimeout(150);
        launch();workPage();UiObject protection=guardToggle();boolean wasProtected=protection.isChecked();
        if(wasProtected)protection.click();UiObject slider=null;int[] before=null;
        try{
            homePage();UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
            assertTrue("Mixer action is missing",page.scrollIntoView(new UiSelector().text("Микшер")));
            text("Микшер").click();slider=musicMixerSlider();assertTrue("Mixer music slider is missing",slider.waitForExists(3000));
            before=musicLevel();int target=before[0]>before[1]/2?Math.max(0,before[1]/5):before[1]*4/5;
            moveMixer(slider,before[0],target,before[1]);int changed=musicLevel()[0];
            assertFalse("Moving the mixer did not change music volume",before[0]==changed);
            slider=musicMixerSlider();moveMixer(slider,changed,before[0],before[1]);
        }finally{
            if(slider!=null&&before!=null&&slider.exists())try{moveMixer(slider,musicLevel()[0],before[0],before[1]);}catch(Exception ignored){}
            UiObject close=new UiObject(new UiSelector().description("Закрыть"));if(close.exists())close.click();
            if(wasProtected){workPage();protection=guardToggle();if(!protection.isChecked())protection.click();}
        }
        assertTrue("Mixer slider remained open after close",waitGone(musicMixerSlider(),2000));
    }
}
