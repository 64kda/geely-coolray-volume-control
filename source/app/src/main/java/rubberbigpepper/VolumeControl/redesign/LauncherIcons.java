package rubberbigpepper.VolumeControl.redesign;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/** Uses only API1 component toggles; the real Activity always remains enabled. */
public final class LauncherIcons {
    private static final String KEY = "Launcher icon";
    private final Context context;
    private final SharedPreferences preferences;
    private final PackageManager packages;
    private final IconSelection.Backend backend = new IconSelection.Backend() {
        public int saved() { return selected(); }
        public boolean save(int id) { return preferences.edit().putInt(KEY, id).commit(); }
        public void enable(int id, boolean enabled) {
            ComponentName component = new ComponentName(context.getPackageName(),
                context.getPackageName()+".LauncherIcon"+id);
            int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            if (packages.getComponentEnabledSetting(component) != state)
                packages.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);
        }
    };
    public LauncherIcons(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences("Common", 0);
        packages = this.context.getPackageManager();
    }
    public int selected() { return IconSelection.normalize(preferences.getInt(KEY, IconSelection.DEFAULT)); }
    public boolean select(int id) { return IconSelection.select(backend, id); }
    public boolean restore() { return IconSelection.restore(backend); }
    public Drawable preview(int id) {
        int resource = context.getResources().getIdentifier("launcher_art_"+id,"drawable",context.getPackageName());
        return context.getResources().getDrawable(resource);
    }
}
