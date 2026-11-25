import java.util.*;
import rubberbigpepper.VolumeControl.redesign.IconSelection;

public final class IconSelectionTest {
    static final class Fake implements IconSelection.Backend {
        int saved=7, operations, failAt=-1; boolean failSave;
        Set<Integer> enabled=new HashSet<Integer>(Arrays.asList(7));
        public int saved(){return saved;}
        public boolean save(int id){if(failSave)return false;saved=id;return true;}
        public void enable(int id,boolean value){
            if(++operations==failAt)throw new IllegalStateException("Package manager failure");
            if(value)enabled.add(id);else enabled.remove(id);
            check(!enabled.isEmpty(),"No launcher may disappear during transition");
        }
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int from:IconSelection.ids())for(int to:IconSelection.ids()){
            Fake b=new Fake();check(IconSelection.select(b,from),"initial selection");
            check(IconSelection.select(b,to),"switch");check(b.saved==to&&b.enabled.equals(Collections.singleton(to)),"exactly chosen icon");
            check(IconSelection.restore(b)&&b.enabled.equals(Collections.singleton(to)),"restart keeps selection");
            for(int failure=1;failure<=4;failure++){
                Fake broken=new Fake();IconSelection.select(broken,from);broken.operations=0;broken.failAt=failure;
                check(!IconSelection.select(broken,to),"failure must be reported");
                check(broken.saved==from&&broken.enabled.equals(Collections.singleton(from)),"rollback keeps previous icon");
            }
        }
        Fake disk=new Fake();disk.failSave=true;
        check(!IconSelection.select(disk,6)&&disk.saved==7&&disk.enabled.equals(Collections.singleton(7)),"disk failure rollback");
        Fake interrupted=new Fake();interrupted.enabled.add(8);check(IconSelection.restore(interrupted)&&interrupted.enabled.equals(Collections.singleton(7)),"repair interrupted selection");
        Fake invalid=new Fake();check(!IconSelection.select(invalid,99)&&invalid.operations==0,"reject invalid option");
        invalid.saved=99;check(IconSelection.restore(invalid)&&invalid.enabled.equals(Collections.singleton(7)),"invalid stored value falls back");
        System.out.println("IconSelection: all 16 transitions, failures, persistence and recovery PASS");
    }
}
