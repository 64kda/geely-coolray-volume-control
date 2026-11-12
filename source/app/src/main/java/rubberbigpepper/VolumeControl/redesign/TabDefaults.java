package rubberbigpepper.VolumeControl.redesign;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Only keys owned by the selected settings page; never clear the shared file. */
public final class TabDefaults {
    public static Map<String,Object> forPage(int page){
        Map<String,Object> values=new LinkedHashMap<String,Object>();
        switch(page){
            case 0: values.put("Stream type",3);break;
            case 1:
                values.put("Main color",0xff1ed760);values.put("Background color",0xe0555555);
                values.put("Size",26);values.put("Length",50);values.put("Gravity",11);
                values.put("Horizontal offset",0);values.put("Vertical offset",0);
                values.put("Bar type",0);values.put("Fullscreen",false);values.put("Percent position",0);break;
            case 2:
                values.put("Action on 0 volume",0);values.put("Show notification",false);
                values.put("Sound notification",false);break;
            case 3:
                values.put("Enable EqMix",true);values.put("Active color",0xff1ed760);
                values.put("Inactive color",0xff282828);values.put("Equalizer enabled",false);
                values.put("Bass level",0);values.put("Virtualizer level",0);
                values.put("Flat band defaults",true);break;
            case 4:
                values.put("Start after reboot",false);values.put("Auto hide",false);
                values.put("Auto hide delay ms",2000);values.put("Auto hide fade ms",300);
                values.put("Reaction delay",false);
                values.put("Touch guard feedback",true);values.put("Touch guard enabled",false);values.put("Touch guard hold ms",250);break;
            default: throw new IllegalArgumentException("No settings page: "+page);
        }
        return values;
    }
    public static Map<String,Object> forPage(int page,Set<String> existingKeys){
        Map<String,Object> values=forPage(page);
        if(page==3)for(String key:existingKeys)if(key.matches("Band \\p{Nd}+ level"))values.put(key,0);
        return values;
    }
}
