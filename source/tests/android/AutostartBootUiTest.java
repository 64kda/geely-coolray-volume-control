import com.android.uiautomator.core.*;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;
import java.io.*;

/** Paired with the host runner's real reboot: enable, observe boot service, restore prior value. */
public final class AutostartBootUiTest extends UiAutomatorTestCase {
    private static final String PKG="rubberbigpepper.VolumeControl.redesign";
    private UiObject text(String value){return new UiObject(new UiSelector().text(value));}
    private void command(String...args)throws Exception{
        Process p=new ProcessBuilder(args).redirectErrorStream(true).start();InputStream in=p.getInputStream();while(in.read()!=-1){};
        assertEquals("Command failed",0,p.waitFor());
    }
    private UiObject boot(){return new UiObject(new UiSelector().text("Автозапуск с магнитолой").className("android.widget.CheckBox"));}
    private void openSetting()throws Exception{
        command("am","start","-W","-n",PKG+"/rubberbigpepper.VolumeControl.MainWindow");
        assertTrue(text("Управление").waitForExists(5000));
        UiScrollable nav=new UiScrollable(new UiSelector().className("android.widget.HorizontalScrollView"));nav.setAsHorizontalList();
        assertTrue(nav.scrollIntoView(new UiSelector().text("Работа приложения")));text("Работа приложения").click();
        UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
        assertTrue("Autostart control is not reachable",page.scrollIntoView(new UiSelector().text("Автозапуск с магнитолой")));
    }
    public void testEnableBeforeHostReboot()throws Exception{
        openSetting();UiObject setting=boot();assertTrue("Autostart option is missing",setting.waitForExists(2000));
        FileOutputStream saved=new FileOutputStream("/data/local/tmp/volume-autostart-original.txt");
        saved.write((setting.isChecked()?"true":"false").getBytes("UTF-8"));saved.close();
        if(!setting.isChecked())setting.click();assertTrue("Autostart did not turn on",setting.isChecked());
    }
    public void testRestorePriorAutostartAfterReboot()throws Exception{
        FileInputStream input=new FileInputStream("/data/local/tmp/volume-autostart-original.txt");
        ByteArrayOutputStream out=new ByteArrayOutputStream();int value;while((value=input.read())!=-1)out.write(value);input.close();
        boolean original="true".equals(out.toString("UTF-8"));openSetting();UiObject setting=boot();
        assertTrue("Autostart option vanished after reboot",setting.waitForExists(2000));
        if(setting.isChecked()!=original)setting.click();assertEquals("Autostart test did not restore its initial setting",original,setting.isChecked());
    }
}
