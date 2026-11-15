import rubberbigpepper.VolumeControl.redesign.ScalePlacement;

public final class ScalePlacementTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        for(int old=0;old<16;old++){
            check(ScalePlacement.normalize(old)==old);
            check(ScalePlacement.geometry(old)==old);
            check(!ScalePlacement.dual(old));check(!ScalePlacement.jointEnabled(old,true));
        }
        check(ScalePlacement.geometry(16)==5);check(ScalePlacement.geometry(17)==11);
        check(ScalePlacement.geometry(18)==11);check(ScalePlacement.sides(18)==3);
        check(ScalePlacement.sides(16)==1);check(ScalePlacement.sides(17)==2);
        check(ScalePlacement.jointEnabled(18,true));check(!ScalePlacement.jointEnabled(18,false));
        for(int invalid:new int[]{-1,19,Integer.MIN_VALUE,Integer.MAX_VALUE}){
            check(ScalePlacement.geometry(invalid)==0);check(!ScalePlacement.dual(invalid));
            check(ScalePlacement.sides(invalid)!=3);
        }
        System.out.println("ScalePlacement PASS: all 16 legacy indices unchanged, three appended modes, safe fallback");
    }
}
