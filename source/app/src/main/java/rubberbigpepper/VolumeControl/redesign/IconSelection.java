package rubberbigpepper.VolumeControl.redesign;

/** Enables the destination before removing any launcher entry (also during rollback). */
public final class IconSelection {
    public static final int DEFAULT = 7;
    private static final int[] IDS = {7, 6, 8, 10};
    public interface Backend {
        int saved();
        boolean save(int id);
        void enable(int id, boolean enabled);
    }
    public static int[] ids() { return IDS.clone(); }
    public static int normalize(int id) {
        for (int candidate : IDS) if (id == candidate) return id;
        return DEFAULT;
    }
    private static void activate(Backend backend, int id) {
        backend.enable(id, true);
        for (int candidate : IDS) if (candidate != id) backend.enable(candidate, false);
    }
    public static boolean select(Backend backend, int requested) {
        if (normalize(requested) != requested) return false;
        int previous = normalize(backend.saved());
        try {
            activate(backend, requested);
            if (!backend.save(requested)) throw new IllegalStateException("Icon preference was not saved");
            return true;
        } catch (RuntimeException failure) {
            try { backend.save(previous); } catch (RuntimeException persistenceFailure) { }
            try { activate(backend, previous); } catch (RuntimeException rollbackFailure) {
                // Destination was enabled first; never remove the surviving entry here.
            }
            return false;
        }
    }
    public static boolean restore(Backend backend) {
        try { activate(backend, normalize(backend.saved())); return true; }
        catch (RuntimeException failure) { return false; }
    }
    private IconSelection() {}
}
