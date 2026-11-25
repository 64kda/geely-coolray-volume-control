import rubberbigpepper.VolumeControl.redesign.AutoHideState;

/** Deterministic time: no sleeps, Android runtime or duplicated timer implementation. */
public final class AutoHideRegressionTest {
    static final class Clock implements AutoHideState.Clock, AutoHideState.Scheduler, AutoHideState.Output {
        long time, due;
        Runnable pending;
        float alpha = -1;
        int outputs;
        public long now() { return time; }
        public void cancel(Runnable task) { if (pending == task) pending = null; }
        public void schedule(Runnable task, long delay) {
            check(delay >= 0, "negative schedule delay");
            check(pending == null, "two callbacks scheduled for one scale");
            pending = task;
            due = time + delay;
        }
        public void visibility(float value) { alpha = value; outputs++; }
        void at(long target) {
            check(target >= time, "clock moved backwards");
            while (pending != null && due <= target) {
                time = due;
                Runnable task = pending;
                pending = null;
                task.run();
            }
            time = target;
        }
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void alpha(Clock clock, float expected, String message) {
        check(Math.abs(clock.alpha - expected) < .0001f, message + ": " + clock.alpha);
    }
    static AutoHideState state(Clock clock, boolean enabled, int delay, int fade) {
        return new AutoHideState(clock, clock, clock, enabled, delay, fade);
    }
    static void fadeBoundaries() {
        for (int delay : new int[]{1000, 2000, 3000, 5000, 10000}) {
            for (int fade : new int[]{0, 150, 300, 600}) {
                Clock clock = new Clock();
                state(clock, true, delay, fade);
                clock.at(delay - 1); alpha(clock, 1, "hidden before delay");
                clock.at(delay); alpha(clock, fade == 0 ? 0 : 1, "fade start");
                if (fade > 0) {
                    clock.at(delay + 50);
                    alpha(clock, 1f - 50f / fade, "fade progress");
                    clock.at(delay + fade - 1);
                    check(clock.alpha > 0, "fade finished early");
                }
                clock.at(delay + fade); alpha(clock, 0, "not hidden at deadline");
                check(clock.pending == null, "hidden scale keeps scheduling");
            }
        }
    }
    static void holdReleaseAndStaleCallbacks() {
        Clock clock = new Clock();
        AutoHideState state = state(clock, true, 2000, 300);
        Runnable startup = clock.pending;
        clock.at(1000); state.hold();
        check(clock.pending == null, "hold left a timer running");
        clock.at(9000); startup.run();
        alpha(clock, 1, "cancelled startup callback hid held scale");
        state.hold(); state.release();
        Runnable release = clock.pending;
        clock.at(10999); alpha(clock, 1, "release reused startup deadline");
        clock.at(11000); clock.at(11150); alpha(clock, .5f, "release fade midpoint");
        state.activity(); alpha(clock, 1, "activity did not restore faded scale");
        release.run(); alpha(clock, 1, "old release callback changed new session");
        clock.at(13450); alpha(clock, 0, "new activity deadline not respected");
        state.hold(); alpha(clock, 1, "hidden scale cannot reopen");
        state.release(); Runnable pending = clock.pending;
        state.close(); int outputs = clock.outputs;
        pending.run(); state.activity(); state.hold(); state.release();
        check(clock.pending == null && clock.outputs == outputs, "closed scale still changes visibility");
    }
    static void independentTimersAndDisabledAutoHide() {
        Clock left = new Clock(), right = new Clock();
        AutoHideState leftState = state(left, true, 2000, 300);
        state(right, true, 2000, 300);
        left.at(1000); leftState.hold();
        left.at(5000); right.at(5000);
        alpha(left, 1, "left hold lost"); alpha(right, 0, "left hold postponed right timer");
        leftState.release(); left.at(7300); alpha(left, 0, "left release did not hide");
        Clock disabled = new Clock();
        AutoHideState off = state(disabled, false, 2000, 300);
        off.hold(); off.release(); disabled.at(100000);
        alpha(disabled, 1, "disabled auto-hide hid scale");
        check(disabled.pending == null, "disabled auto-hide schedules callbacks");
    }
    public static void main(String[] args) {
        fadeBoundaries(); holdReleaseAndStaleCallbacks(); independentTimersAndDisabledAutoHide();
        System.out.println("AutoHideRegression PASS: 20 delay/fade combinations, hold/release, stale callbacks, close, independent timers, disabled mode");
    }
}
