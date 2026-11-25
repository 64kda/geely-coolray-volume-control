import rubberbigpepper.VolumeControl.redesign.TouchGuardState;

/** Exercises the protected touch flow with a deterministic clock and volume sink. */
public final class TouchGuardStateTest {
    static final class Loop implements TouchGuardState.Clock, TouchGuardState.Scheduler, TouchGuardState.Output {
        long now, due;
        Runnable pending;
        int value = 5, writes, tools;
        boolean ready;
        public long now() { return now; }
        public int level() { return value; }
        public void write(int next) { value = next; writes++; }
        public void ready(boolean on) { ready = on; }
        public void tools() { tools++; }
        public void cancel(Runnable task) { if (pending == task) pending = null; }
        public void post(Runnable task, long delay) { pending = task; due = now + delay; }
        void advance(long target) {
            if (pending != null && due <= target) {
                now = due;
                Runnable task = pending; pending = null; task.run();
            }
            now = target;
        }
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    public static void main(String[] args) {
        Loop loop = new Loop();
        TouchGuardState guard = new TouchGuardState(loop, loop, loop, 250, 8f);

        guard.down(50, 0, 0, 10, 100, false, false);
        guard.move(50, 0, 100);
        loop.advance(249);
        check(loop.writes == 0 && loop.value == 5 && !loop.ready, "short touch changed volume before protection armed");
        guard.cancel();

        guard.down(50, 0, loop.now, 10, 100, false, false);
        loop.advance(loop.now + 250);
        check(loop.ready, "hold did not arm protected adjustment");
        guard.move(70, 0, loop.now);
        check(loop.writes == 1 && loop.value == 7, "confirmed drag did not update volume");
        guard.cancel();
        check(!loop.ready && loop.pending == null, "release left guard active");

        Loop cancelled = new Loop();
        TouchGuardState cancelledGuard = new TouchGuardState(cancelled, cancelled, cancelled, 250, 8f);
        cancelledGuard.down(0, 0, 0, 10, 100, false, false);
        cancelledGuard.move(20, 0, 10);
        cancelled.advance(500);
        check(cancelled.writes == 0 && !cancelled.ready, "movement before confirmation changed volume");
        System.out.println("TouchGuardState: short touch blocked, held drag applied, pre-confirm movement cancelled PASS");
    }
}
