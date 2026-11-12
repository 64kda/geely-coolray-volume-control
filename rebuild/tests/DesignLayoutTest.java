import com.android.uiautomator.core.*;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;
import android.graphics.Rect;

/** Checks the visible home layout without changing audio or preferences. */
public class DesignLayoutTest extends UiAutomatorTestCase {
    private UiObject text(String value) { return new UiObject(new UiSelector().text(value)); }
    public void testHome() throws Exception {
        Configurator.getInstance().setWaitForIdleTimeout(200);
        assertTrue("Missing volume heading", text("Текущая громкость").waitForExists(3000));
        Rect show = text("Показать шкалу").getBounds();
        Rect hide = text("Скрыть шкалу").getBounds();
        int minTouch = 48; // The test emulator uses mdpi.
        assertTrue("Small primary action", show.height() >= minTouch);
        assertTrue("Small secondary action", hide.height() >= minTouch);
        assertTrue("Actions overlap", show.right <= hide.left);
        UiObject preview = text("Плавающая шкала");
        boolean compact = "true".equals(getParams().getString("compact"));
        if (compact) {
            UiScrollable page = new UiScrollable(new UiSelector().className("android.widget.ScrollView"));
            assertTrue("Preview cannot be reached", page.scrollIntoView(new UiSelector().text("Плавающая шкала")));
            assertTrue("Preview is clipped horizontally", preview.getBounds().right <= getUiDevice().getDisplayWidth());
            assertTrue("Compact preview should use a full row", preview.getBounds().left < getUiDevice().getDisplayWidth()/2);
        } else {
            assertTrue("Preview missing", preview.exists());
            assertTrue("Wide layout should have two columns", preview.getBounds().left > hide.right);
        }
    }
}
