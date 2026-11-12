import java.util.*;
import rubberbigpepper.VolumeControl.redesign.TabDefaults;

public class TabDefaultsTest {
    public static void main(String[] args){
        Set<String> owned=new HashSet<String>();
        for(int page=0;page<5;page++){
            Map<String,Object> defaults=TabDefaults.forPage(page);
            if(defaults.isEmpty())throw new AssertionError("Empty reset: "+page);
            for(String key:defaults.keySet())if(!owned.add(key))throw new AssertionError("Shared page key: "+key);
            Map<String,Object> before=new HashMap<String,Object>();
            for(int other=0;other<5;other++)for(String key:TabDefaults.forPage(other).keySet())before.put(key,"custom");
            for(String key:new String[]{"Band 0 level","Band 17 level","Band ٢ level","Stream 3 level","Unknown future setting"})before.put(key,"preserve");
            defaults=TabDefaults.forPage(page,before.keySet());
            Map<String,Object> after=new HashMap<String,Object>(before);after.putAll(defaults);
            for(String key:before.keySet())if(!defaults.containsKey(key)&&!before.get(key).equals(after.get(key)))throw new AssertionError("Unrelated key changed: "+key);
            Map<String,Object> once=new HashMap<String,Object>(after);after.putAll(TabDefaults.forPage(page,after.keySet()));
            if(!once.equals(after))throw new AssertionError("Reset is not idempotent");
            if(page==3)for(String key:new String[]{"Band 0 level","Band 17 level","Band ٢ level","Bass level","Virtualizer level"})
                if(!Integer.valueOf(0).equals(after.get(key)))throw new AssertionError("Effect not flat: "+key);
        }
        if(!Integer.valueOf(3).equals(TabDefaults.forPage(0).get("Stream type")))throw new AssertionError("Default channel");
        if(!Integer.valueOf(11).equals(TabDefaults.forPage(1).get("Gravity")))throw new AssertionError("Safe position");
        if(!Boolean.FALSE.equals(TabDefaults.forPage(4).get("Start after reboot")))throw new AssertionError("Autostart default");
        try{TabDefaults.forPage(5);throw new AssertionError("About has no settings");}catch(IllegalArgumentException expected){}
        System.out.println("TabDefaults PASS: 5 isolated resets, unrelated settings preserved, repeatable");
    }
}
