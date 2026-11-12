import com.android.uiautomator.core.*;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;
import android.graphics.Rect;
import java.io.*;

/** Regression: all four swatches stay compact after repeated color selection. */
public class ColorSwatchTest extends UiAutomatorTestCase {
    private static final String PKG="rubberbigpepper.VolumeControl.redesign";
    private UiObject id(String name){return new UiObject(new UiSelector().resourceId(PKG+":id/"+name));}
    private UiObject text(String label){return new UiObject(new UiSelector().text(label));}
    private void tab(String name)throws Exception{
        new UiObject(new UiSelector().text(name).className("android.widget.Button").instance(0)).click();getUiDevice().waitForIdle(300);
    }
    private void check(String name)throws Exception{
        UiObject sample=id(name);assertTrue("Swatch missing: "+name,sample.waitForExists(1500));Rect first=sample.getBounds();
        assertTrue("Swatch stretches across row: "+name+" "+first,first.width()<=96);
        assertTrue("Touch target too small: "+name,first.width()>=48&&first.height()>=48);
        for(int i=0;i<2;i++){
            sample.click();assertTrue(text("Выбор цвета").waitForExists(1500));
            Rect red=new UiObject(new UiSelector().className("android.widget.SeekBar").instance(0)).getBounds();
            getUiDevice().click(red.left+red.width()*(i==0?1:3)/4,red.centerY());
            assertTrue(text("Применить").waitForExists(1000));text("Применить").click();
            assertTrue(sample.waitForExists(1500));getUiDevice().waitForIdle(300);
            Rect after=sample.getBounds();assertEquals("Width changed after applying color",first.width(),after.width());assertEquals("Height changed after applying color",first.height(),after.height());
        }
    }
    public void testSwatches()throws Exception{
        Configurator.getInstance().setWaitForIdleTimeout(100).setWaitForSelectorTimeout(1000);
        getUiDevice().wakeUp();getUiDevice().pressMenu();
        Process process=new ProcessBuilder("am","start","-W","-n",PKG+"/rubberbigpepper.VolumeControl.MainWindow").start();process.waitFor();
        assertTrue(text("Управление").waitForExists(5000));
        tab("Шкала");check("ImageViewColor");check("ImageViewBackgroundColor");
        getUiDevice().takeScreenshot(new File("/data/local/tmp/color-fix-scale.png"));
        tab("Эквалайзер");check("ImageViewActiveColor");check("ImageViewInactiveColor");
        getUiDevice().takeScreenshot(new File("/data/local/tmp/color-fix-equalizer.png"));
        FileWriter writer=new FileWriter("/data/local/tmp/color-fix-result.txt");writer.write("PASS API18: all 4 swatches compact; two apply cycles each; stable bounds; 48dp touch targets\n");writer.close();
    }
}
