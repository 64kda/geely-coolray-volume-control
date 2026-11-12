package rubberbigpepper.VolumeControl.redesign;

import android.media.AudioManager;
import rubberbigpepper.CommonCtrl.ZoomView;

/** Settings only read audio state. Only explicit user gestures may write volume. */
public final class ScaleState {
    public static void sync(ZoomView view,AudioManager audio,int selected){
        int stream=VolumeMath.displayStream(selected,audio.getMode(),audio.isMusicActive());
        view.setMax(audio.getStreamMaxVolume(stream));
        view.setProgress(audio.getStreamVolume(stream));
    }
}
