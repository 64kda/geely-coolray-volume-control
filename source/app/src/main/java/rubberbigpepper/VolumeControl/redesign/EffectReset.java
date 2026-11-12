package rubberbigpepper.VolumeControl.redesign;

import android.content.SharedPreferences;
import android.media.audiofx.*;
import android.util.Log;

/** Current service effects only: never creates an effect or changes stream volumes. */
public final class EffectReset {
    private static Object equalizer,bass,virtualizer;
    public static void setTargets(Object eq,Object low,Object stereo){equalizer=eq;bass=low;virtualizer=stereo;}
    public static void clearTargets(){equalizer=null;bass=null;virtualizer=null;}
    public static int bandDefault(SharedPreferences prefs,int previous){return prefs.getBoolean("Flat band defaults",false)?0:previous;}
    public static boolean resetLive(){
        boolean success=true;
        try{if(equalizer instanceof Equalizer){Equalizer eq=(Equalizer)equalizer;
            success&=eq.setEnabled(false)==AudioEffect.SUCCESS;
            for(short band=0;band<eq.getNumberOfBands();band++)eq.setBandLevel(band,(short)0);
        }}catch(RuntimeException failure){Log.w("VolumeReset","Equalizer reset failed",failure);success=false;}
        try{if(bass instanceof BassBoost){BassBoost effect=(BassBoost)bass;effect.setStrength((short)0);success&=effect.setEnabled(false)==AudioEffect.SUCCESS;}}
        catch(RuntimeException failure){Log.w("VolumeReset","Bass reset failed",failure);success=false;}
        try{if(virtualizer instanceof Virtualizer){Virtualizer effect=(Virtualizer)virtualizer;effect.setStrength((short)0);success&=effect.setEnabled(false)==AudioEffect.SUCCESS;}}
        catch(RuntimeException failure){Log.w("VolumeReset","Virtualizer reset failed",failure);success=false;}
        return success;
    }
}
