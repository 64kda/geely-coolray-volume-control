import rubberbigpepper.VolumeControl.redesign.VolumeMath;
public class VolumeMathTest {
    public static void main(String[] args) {
        if (VolumeMath.streamAt(2)!=3 || VolumeMath.streamAt(7)!=4 || VolumeMath.streamAt(8)!=8)
            throw new AssertionError("Original channel order changed");
        if (VolumeMath.scale(0,15,7)!=0 || VolumeMath.scale(15,15,7)!=7 || VolumeMath.scale(9,15,10)!=6)
            throw new AssertionError("Channel normalization failed");
        if (VolumeMath.scale(20,15,7)!=7 || VolumeMath.scale(-2,15,7)!=0 || VolumeMath.scale(1,0,7)!=0)
            throw new AssertionError("Invalid range was not clamped");
        if (VolumeMath.percent(3,15)!=20 || VolumeMath.percent(0,0)!=0)
            throw new AssertionError("Displayed percentage failed");
        if (VolumeMath.ringerMode(0,0)!=-1 || VolumeMath.ringerMode(0,1)!=1 || VolumeMath.ringerMode(0,2)!=0 || VolumeMath.ringerMode(1,2)!=2)
            throw new AssertionError("Configured zero-volume action failed");
        for(int stream:new int[]{0,1,2,3,4,5,8})if(VolumeMath.displayStream(stream,0,false)!=stream)throw new AssertionError("Selected stream changed");
        if(VolumeMath.displayStream(-1,0,false)!=3||VolumeMath.displayStream(-2,0,true)!=3||VolumeMath.displayStream(-2,0,false)!=2
            ||VolumeMath.displayStream(-2,2,true)!=0||VolumeMath.displayStream(-2,3,false)!=0)throw new AssertionError("Display stream resolution failed");
        System.out.println("VolumeMath: channel order, normalization, limits and display PASS");
    }
}
