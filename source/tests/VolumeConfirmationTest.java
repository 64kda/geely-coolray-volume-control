import rubberbigpepper.VolumeControl.redesign.VolumeConfirmation;
public final class VolumeConfirmationTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        VolumeConfirmation c=new VolumeConfirmation();
        c.written(9,100);check(c.observe(7,449)==0);check(c.observe(9,449)==1);
        check(c.observe(9,600)==1);check(c.observe(8,601)==2);
        c.written(10,1000);c.written(11,1100);
        check(c.observe(10,1449)==0);check(c.observe(8,1450)==2);
        c.written(4,2000);check(c.observe(3,2350)==2);
        c.reset();check(c.observe(4,2351)==2);
        System.out.println("VolumeConfirmation PASS: stale/ack/duplicate, rapid writes, bounded external override, failed-write readback, reset");
    }
}
