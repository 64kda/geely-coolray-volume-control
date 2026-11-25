import com.android.uiautomator.core.*;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;
import android.graphics.Rect;
import java.io.*;

/** Real settings taps, selected state, saved process restart and evidence screenshots. */
public final class LauncherIconUiTest extends UiAutomatorTestCase {
    private static final String PKG="rubberbigpepper.VolumeControl.redesign";
    private UiObject text(String value){return new UiObject(new UiSelector().text(value));}
    private void command(String...args)throws Exception{
        Process p=new ProcessBuilder(args).redirectErrorStream(true).start();
        InputStream in=p.getInputStream();while(in.read()!=-1){}
        assertEquals("Command failed",0,p.waitFor());
    }
    private void openChooser()throws Exception{
        command("am","start","-W","-n",PKG+"/rubberbigpepper.VolumeControl.MainWindow");
        assertTrue(text("Управление").waitForExists(3000));
        assertFalse("Redundant header shortcut must be absent",new UiObject(new UiSelector().description("Изменить иконку приложения")).exists());
        UiScrollable nav=new UiScrollable(new UiSelector().className("android.widget.HorizontalScrollView"));
        nav.setAsHorizontalList();
        assertTrue("Settings tab must be reachable",nav.scrollIntoView(new UiSelector().text("Работа приложения")));
        text("Работа приложения").click();
        UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
        assertTrue(page.scrollIntoView(new UiSelector().text("Иконка приложения")));
    }
    public void testChooserAndProcessRestart()throws Exception{
        Configurator.getInstance().setWaitForIdleTimeout(200);
        int[] ids={7,6,8,10};String[] names={"Регулятор","Уровни","Динамик","Слайдер"};
        try{
            for(int index=0;index<ids.length;index++){
                openChooser();
                UiSelector selector=new UiSelector().descriptionStartsWith("Иконка "+names[index]).className("android.widget.Button");
                UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
                assertTrue(page.scrollIntoView(selector));
                UiObject choice=new UiObject(selector);Rect bounds=choice.getBounds();
                assertTrue("Touch area too small",bounds.width()>=48&&bounds.height()>=48);
                assertTrue("Clipped choice",bounds.left>=0&&bounds.right<=getUiDevice().getDisplayWidth());
                choice.click();
                assertTrue("Selection is not visible",new UiObject(new UiSelector().description("Иконка "+names[index]+", выбрана").selected(true)).waitForExists(2000));
                assertTrue(getUiDevice().takeScreenshot(new File("/data/local/tmp/icon-choice-"+ids[index]+".png")));
                command("am","force-stop",PKG);openChooser();
                assertTrue(page.scrollIntoView(selector));
                assertTrue("Choice lost after process restart",new UiObject(new UiSelector().description("Иконка "+names[index]+", выбрана").selected(true)).exists());
            }
        }finally{
            openChooser();
            UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
            UiSelector selector=new UiSelector().descriptionStartsWith("Иконка Регулятор");
            page.scrollIntoView(selector);new UiObject(selector).click();
        }
    }
}
