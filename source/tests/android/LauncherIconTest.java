package local.volumecontrol.icontests;

import android.app.Instrumentation;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.test.InstrumentationTestCase;
import java.util.*;
import rubberbigpepper.VolumeControl.redesign.LauncherIcons;

/** Runs with target instrumentation: real PackageManager and app preferences, not shell toggles. */
public final class LauncherIconTest extends InstrumentationTestCase {
    public void testAllIconTransitionsAndResolvedArtwork() throws Exception {
        Context target=getInstrumentation().getTargetContext();
        assertEquals("Run on the dedicated mdpi API18 emulator",160,target.getResources().getDisplayMetrics().densityDpi);
        assertEquals("Runtime must be API18",18,android.os.Build.VERSION.SDK_INT);
        final LauncherIcons icons=new LauncherIcons(target);int previous=icons.selected();
        try {
            for(int from:new int[]{7,6,8,10}){
                for(int id:new int[]{7,6,8,10}){
                    assertTrue("Select source icon "+from,icons.select(from));
                    assertTrue("Real component switch failed "+id,icons.select(id));
                    assertEquals(id,new LauncherIcons(target).selected());
                    Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(target.getPackageName());
                    List<ResolveInfo> entries=target.getPackageManager().queryIntentActivities(query,0);
                    assertEquals("Must expose exactly one visible entry",1,entries.size());
                    ResolveInfo entry=entries.get(0);
                    assertTrue(entry.activityInfo.name.endsWith(".LauncherIcon"+id));
                    assertEquals("Launcher label", "Регулятор громкости",entry.loadLabel(target.getPackageManager()).toString());
                    assertTrue("Resolved entry must launch the target",target.getPackageManager().getActivityInfo(
                        new ComponentName(target.getPackageName(),"rubberbigpepper.VolumeControl.MainWindow"),0).enabled);
                    Drawable icon=entry.loadIcon(target.getPackageManager());
                    assertTrue("Launcher received a small icon and would upscale it",icon.getIntrinsicWidth()>=512);
                    Bitmap actual=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
                    icon.setBounds(0,0,512,512);icon.draw(new Canvas(actual));
                    Bitmap expected=BitmapFactory.decodeStream(getInstrumentation().getContext().getAssets().open("art-"+id+".png"));
                    assertTrue("Launcher artwork differs from reviewed baseline "+id,actual.sameAs(expected));
                    actual.recycle();expected.recycle();
                    Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(new ComponentName(target.getPackageName(),entry.activityInfo.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    android.app.Activity activity=getInstrumentation().startActivitySync(launch);
                    assertNotNull("Visible launcher entry cannot start app",activity);
                    getInstrumentation().runOnMainSync(new Finish(activity));
                }
            }
        } finally { assertTrue("Restore initial icon",icons.select(previous)); }
    }
    private static final class Finish implements Runnable {
        private final android.app.Activity activity;
        Finish(android.app.Activity value){activity=value;}
        public void run(){activity.finish();}
    }
}
