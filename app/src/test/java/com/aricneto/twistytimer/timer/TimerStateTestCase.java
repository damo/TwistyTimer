package com.aricneto.twistytimer.timer;

import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.aricneto.twistytimer.timer.TimerCue
    .CUE_INSPECTION_READY_TO_START;
import static com.aricneto.twistytimer.timer.TimerCue.CUE_INSPECTION_STARTED;
import static com.aricneto.twistytimer.timer.TimerCue.CUE_SOLVE_READY_TO_START;
import static org.junit.Assert.*;

/**
 * Tests most aspects of the {@link TimerState} class. The tests for the
 * implementation of the {@code android.os.Parcelable} API are in a separate
 * class (and source directory), as they depend on {@code android.os.Parcel},
 * which is only available to instrumented tests that are run within an Android
 * run-time context (e.g., on a device or emulator). For tests that depend on
 * JSON, the Android JAR contains only stub classes, so these must be replaced
 * with a real implementation that matches what is packaged with Android. This
 * can be done by adding "{@code testCompile 'org.json:json:20080701'}" to the
 * Gradle build dependencies.
 *
 * @author damo
 */
public class TimerStateTestCase {
    @Test
    public void testCreation() throws Exception {
        TimerState ts;

        // Check all combinations to ensure that there is no mix-up between the
        // hold-to-start flag and the enabled state of inspection.
        //
        // "testCreation()" also tests the "getInspectionDuration()",
        // "isInspectionEnabled()" and  "isHoldToStartEnabled()" methods in as
        // much detail as is necessary, so they are not tested independently.

        ts = new TimerState(15_000, true);
        assertTrue(ts.isInspectionEnabled());
        assertEquals(15_000, ts.getInspectionDuration());
        assertTrue(ts.isHoldToStartEnabled());

        ts = new TimerState(0, true);
        assertFalse(ts.isInspectionEnabled());
        assertEquals(0, ts.getInspectionDuration());
        assertTrue(ts.isHoldToStartEnabled());

        ts = new TimerState(-1, true); // -ve duration should be reported as 0.
        assertFalse(ts.isInspectionEnabled());
        assertEquals(0, ts.getInspectionDuration());
        assertTrue(ts.isHoldToStartEnabled());

        ts = new TimerState(42_000, false);
        assertTrue(ts.isInspectionEnabled());
        assertEquals(42_000, ts.getInspectionDuration());
        assertFalse(ts.isHoldToStartEnabled());

        ts = new TimerState(0, false);
        assertFalse(ts.isInspectionEnabled());
        assertEquals(0, ts.getInspectionDuration());
        assertFalse(ts.isHoldToStartEnabled());

        ts = new TimerState(-100, false);
        assertFalse(ts.isInspectionEnabled());
        assertEquals(0, ts.getInspectionDuration());
        assertFalse(ts.isHoldToStartEnabled());
    }

    @Test
    public void testEquals() throws Exception {
        // Some other tests depend on "equals(Object)", so it is important to
        // test that it is correct, otherwise the results of the other tests
        // would be unreliable.

        final TimerState ts1 = new TimerState(15_000, true);
        final TimerState ts2 = new TimerState(15_000, true);
        final TimerState ts3 = new TimerState(11_111, true);
        final TimerState ts4 = new TimerState(15_000, false);

        // Null object is not equal.
        assertNotEquals(ts1, null);
        assertNotEquals(null, ts1);

        // Different class of object is not equal.
        assertNotEquals(ts1, "Hello!");
        assertNotEquals("Hello!", ts1);

        // Differences in configurations.
        assertEquals(ts1, ts2);    // Same configuration.
        assertNotEquals(ts1, ts3); // Different inspection duration.
        assertNotEquals(ts1, ts4); // Different hold-to-start setting.
        assertNotEquals(ts3, ts4); // Both values are different.

        // Difference in cues, so not equal until cue is reloaded.
        ts2.fireTimerCue(CUE_INSPECTION_READY_TO_START);
        assertNotEquals(ts1, ts2);
        ts2.reloadTimerCue(CUE_INSPECTION_READY_TO_START);
        assertEquals(ts1, ts2);

        // Inspection is started for one and not the other.
        ts1.startInspection(100_000);
        assertNotEquals(ts1, ts2);
        ts2.startInspection(100_000);
        assertEquals(ts1, ts2);

        // Different timer stages.
        ts1.setStage(TimerStage.INSPECTION_STARTED);
        assertNotEquals(ts1, ts2); // ts2 still has default "UNUSED" stage.
        ts2.setStage(TimerStage.INSPECTION_STARTED);
        assertEquals(ts1, ts2);

        // Different penalties.
        ts1.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertNotEquals(ts1, ts2);
        ts2.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(ts1, ts2);

        // Different remaining inspection times due to different time marks.
        ts1.mark(109_000);
        ts2.mark(110_000);
        assertNotEquals(ts1, ts2);
        ts1.mark(110_000);
        assertEquals(ts1, ts2);

        // Different inspection running states, again. Try to make the elapsed
        // time the same by setting a "mark()" on the second state to match the
        // end time of the first state.
        ts1.stopInspection(112_000);
        ts2.mark(112_000);
        assertNotEquals(ts1, ts2);
        ts2.stopInspection(112_000);
        assertEquals(ts1, ts2);

        // Solve timer is started on one and not the other.
        ts1.startSolve(120_000);
        assertNotEquals(ts1, ts2);
        ts2.startSolve(120_000);
        assertEquals(ts1, ts2);

        // Different elapsed solve times due to different time marks.
        ts1.mark(129_000);
        ts2.mark(130_000);
        assertNotEquals(ts1, ts2);
        ts1.mark(130_000);
        assertEquals(ts1, ts2);

        // Different paused states.
        ts1.pauseSolve(130_000);
        ts2.mark(130_000);
        assertNotEquals(ts1, ts2);
        ts2.pauseSolve(130_000);
        assertEquals(ts1, ts2);

        ts1.resumeSolve(131_000);
        assertNotEquals(ts1, ts2);
        ts2.resumeSolve(131_000);
        assertEquals(ts1, ts2);

        // Different solve running states.
        ts1.stopSolve(132_000);
        ts2.mark(132_000);
        assertNotEquals(ts1, ts2);
        ts2.stopSolve(132_000);
        assertEquals(ts1, ts2);

        // Equality of the solve instances...
        final Solve solve1 = new Solve(
            100,
            14_000, // 10 seconds elapsed + 2x2-second penalties
            PuzzleType.TYPE_333, "Normal", 123,
            "U1 L1",
            Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.PLUS_TWO)
                .incurPostStartPenalty(Penalty.PLUS_TWO),
            "Hello, world!", false);
        final Solve solve2 = solve1.withDate(321); // Only difference.

        ts1.setSolve(solve1);
        assertNotEquals(ts1, ts2); // ts2 solve is null
        ts2.setSolve(solve1); // Same solve.
        assertEquals(ts1, ts2);
        //noinspection ConstantConditions (force this to null)
        ts1.setSolve(null);
        assertNotEquals(ts1, ts2); // ts1 solve is null

        ts1.setSolve(solve1);
        ts2.setSolve(solve2);
        assertNotEquals(ts1, ts2);
        ts1.setSolve(solve2);
        assertEquals(ts1, ts2);
    }

    @Test
    public void testNewUnusedState() throws Exception {
        // Initialise this "prototype" state with values that are different from
        // the default values, so the new unused state can be seen to copy both
        // values unchanged.
        final TimerState ts1 = new TimerState(3_456, true);

        // Change a few things and then check that the new *unused* state is
        // reset back to the default state, save for the inspection duration
        // and hold-to-start flag. This need not go into too much detail, as
        // the implementation is not a complete black box.
        ts1.setStage(TimerStage.STOPPED);
        ts1.setRefreshPeriod(1_234);

        assertEquals(3_456, ts1.getInspectionDuration());
        assertTrue(ts1.isHoldToStartEnabled());
        assertEquals(TimerStage.STOPPED, ts1.getStage());
        assertEquals(1_234, ts1.getRefreshPeriod());

        final TimerState ts2 = ts1.newUnusedState();

        assertNotSame(ts1, ts2);
        assertEquals(3_456, ts2.getInspectionDuration());   // Copied.
        assertTrue(ts2.isHoldToStartEnabled());              // Copied.
        assertNotEquals(TimerStage.STOPPED, ts2.getStage()); // Reset.
        assertNotEquals(1_234, ts2.getRefreshPeriod());     // Reset.
    }

    @Test
    public void testCanFireTimerCue() throws Exception {
        // NOTE: Only the initial conditions are tested here, as they depend on
        // the inspection and hold-to-start configurations. Behaviour in more
        // "dynamic" contexts (firing and reloading) is tested elsewhere.

        // Check the expected number of cue values to detect changes to the
        // "TimerCue" enum that might affect the completeness of these tests.
        assertEquals("Test coverage is probably not complete.",
            15, TimerCue.values().length);

        // If inspection is disabled, not all cues can be fired. If inspection
        // is enabled, some of the inspection-time-remaining cues can only be
        // fired if the inspection duration is longer (equal is not enough)
        // than the time notified by the cue. Other cues depend on whether or
        // not hold-to-start is enabled.
        TimerState ts;

        // --------------------------------------------------------------------
        // Inspection and hold-to-start are both disabled.
        ts = new TimerState(0, false);

        // It is simplest to use a "switch" in a loop for this. It also allows
        // the code inspector to warn if enum values are missing from the cases.
        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                // NOTE: "CUE_INSPECTION_RESUMED" is a special case. It is
                // not enabled by default for *any* timer state configuration.
                case CUE_INSPECTION_RESUMED:

                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_HOLDING_FOR_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_SOLVE_READY_TO_START:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        // --------------------------------------------------------------------
        // Inspection is disabled and hold-to-start is enabled.
        ts = new TimerState(0, true);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        // --------------------------------------------------------------------
        // Inspection is enabled and hold-to-start is disabled. Inspection
        // period is long enough for all time-remaining warning cues.
        ts = new TimerState(15_000, false);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        // --------------------------------------------------------------------
        // Inspection is enabled and hold-to-start is enabled. Inspection
        // period is long enough for all time-remaining warning cues.
        ts = new TimerState(15_000, true);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_SOLVE_READY_TO_START:
                case CUE_SOLVE_HOLDING_FOR_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        // --------------------------------------------------------------------
        // Inspection is enabled and hold-to-start is disabled. Inspection
        // period is long enough for one time-remaining warning cue, but not
        // the other cue. This is tested three times ==, < and > the limit.
        ts = new TimerState(7_000, false);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        ts = new TimerState(6_999, false);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        ts = new TimerState(7_001, false);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        // --------------------------------------------------------------------
        // Inspection and hold-to-start are both enabled (just for a change).
        // Inspection period is too short for either time-remaining warning cue.
        ts = new TimerState(3_000, true);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        ts = new TimerState(2_999, true);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }

        ts = new TimerState(3_001, true);

        for (TimerCue cue : TimerCue.values()) {
            switch (cue) {
                case CUE_INSPECTION_RESUMED:
                case CUE_INSPECTION_7S_REMAINING:
                case CUE_SOLVE_HOLDING_FOR_START:
                case CUE_SOLVE_READY_TO_START:
                    assertFalse("Can fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                case CUE_INSPECTION_3S_REMAINING:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    assertTrue("Cannot fire: " + cue, ts.canFireTimerCue(cue));
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer cue: " + cue);
            }
        }
    }

    @Test
    public void testFireTimerCue() throws Exception {
        final TimerState ts = new TimerState(15_000, true);

        // Test that if a cue can be fired, it only fires once and that it does
        // not affect the firing of any other cues. (As the implementation is
        // known, just one other cue is checked to see if it can still be fired,
        // as more than that would be overkill.)
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_STARTED));

        // Should fire once...
        assertTrue(ts.fireTimerCue(CUE_INSPECTION_READY_TO_START));

        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_STARTED));

        // ...but should not fire again....
        assertFalse(ts.fireTimerCue(CUE_INSPECTION_READY_TO_START));

        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_STARTED));
    }

    @Test
    public void testReloadTimerCue() throws Exception {
        final TimerState ts = new TimerState(15_000, true);

        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_STARTED));

        // Should fire once...
        assertTrue(ts.fireTimerCue(CUE_INSPECTION_READY_TO_START));

        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_STARTED));

        // ...but should not fire again...
        assertFalse(ts.fireTimerCue(CUE_INSPECTION_READY_TO_START));

        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_STARTED));

        // ...unless it is reloaded....
        // Also test that reloading this cue does not reload any other cue;
        // just fire one other cue and re-test it.
        assertTrue(ts.fireTimerCue(CUE_INSPECTION_STARTED));
        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_STARTED));

        ts.reloadTimerCue(CUE_INSPECTION_READY_TO_START);

        // Other cue still cannot be fired....
        assertFalse(ts.fireTimerCue(CUE_INSPECTION_STARTED));
        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_STARTED));

        // Reloaded cue can fire again, but only once more....
        assertTrue(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertTrue(ts.fireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_READY_TO_START));
        assertFalse(ts.fireTimerCue(CUE_INSPECTION_READY_TO_START));

        // Other cue *still* cannot be fired....
        assertFalse(ts.fireTimerCue(CUE_INSPECTION_STARTED));
        assertFalse(ts.canFireTimerCue(CUE_INSPECTION_STARTED));
    }

    @Test
    public void testSetRefreshPeriodAndGetRefreshPeriod() throws Exception {
        TimerState ts;

        // The default refresh period is 31 ms, regardless of whether or not
        // the inspection period is enabled.
        ts = new TimerState(15_000, false);
        assertEquals(31, ts.getRefreshPeriod());

        ts = new TimerState(0, false);
        assertEquals(31, ts.getRefreshPeriod());

        // If the refresh period is shorter than 10 ms or longer than 1 minute,
        // it will be clamped to the nearest of those limits.
        ts.setRefreshPeriod(1);
        assertEquals(10, ts.getRefreshPeriod());
        ts.setRefreshPeriod(9);
        assertEquals(10, ts.getRefreshPeriod());
        ts.setRefreshPeriod(10);
        assertEquals(10, ts.getRefreshPeriod());

        ts.setRefreshPeriod(59_999);
        assertEquals(59_999, ts.getRefreshPeriod());
        ts.setRefreshPeriod(60_000);
        assertEquals(60_000, ts.getRefreshPeriod());
        ts.setRefreshPeriod(60_001);
        assertEquals(60_000, ts.getRefreshPeriod());
        ts.setRefreshPeriod(60_000_000);
        assertEquals(60_000, ts.getRefreshPeriod());

        // If the value new period is "-1", the default will be used instead.
        // This will be the default for the inspection timer, if inspection
        // is running (not just enabled), otherwise it is the default for the
        // solve timer. Starting the inspection timer or solve timer will set
        // it to its default for the respective timer phase. The default for
        // the solve timer is the initial value.
        ts = new TimerState(15_000, false);

        assertEquals(31, ts.getRefreshPeriod());
        ts.setRefreshPeriod(-1);
        assertEquals(31, ts.getRefreshPeriod());

        ts.startInspection(123_456); // Should set inspection default period.
        assertTrue(ts.isInspectionRunning());
        assertEquals(1_000, ts.getRefreshPeriod());
        ts.setRefreshPeriod(-1); // Should remain inspection default period.
        assertEquals(1_000, ts.getRefreshPeriod());
        ts.setRefreshPeriod(100); // Override with custom period.
        assertEquals(100, ts.getRefreshPeriod());
        ts.setRefreshPeriod(-1); // Restore the default period.
        assertEquals(1_000, ts.getRefreshPeriod());
        ts.stopInspection(234_567); // Should not affect the period.
        assertFalse(ts.isInspectionRunning());
        assertEquals(1_000, ts.getRefreshPeriod());

        // With neither timer running (in between stopping one and starting the
        // other), the default should be the solve timer default. This tests
        // that the running state of the inspection timer is being identified
        // correctly.
        ts.setRefreshPeriod(-1); // Initialise to a non-default period.
        assertEquals(31, ts.getRefreshPeriod());

        ts.setRefreshPeriod(123); // Initialise to a non-default period.
        ts.startSolve(234_567); // Should set solve default period.
        assertEquals(31, ts.getRefreshPeriod());
        ts.setRefreshPeriod(-1); // Should remain solve default period.
        assertEquals(31, ts.getRefreshPeriod());
        ts.setRefreshPeriod(100); // Override with custom period.
        assertEquals(100, ts.getRefreshPeriod());
        ts.setRefreshPeriod(-1); // Restore the default period.
        assertEquals(31, ts.getRefreshPeriod());
        ts.stopSolve(345_678); // Should not affect the period.
        assertEquals(31, ts.getRefreshPeriod());

        // The period must be "-1" (tested above) or be positive. Zero, or any
        // other negative value, is not allowed.
        try {
            ts.setRefreshPeriod(0);
            fail("Expected an exception for zero refresh period.");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class.getName(),
                e.getClass().getName());
        }

        try {
            ts.setRefreshPeriod(-2);
            fail("Expected an exception for negative refresh period.");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class.getName(),
                e.getClass().getName());
        }

        try {
            ts.setRefreshPeriod(-1_000);
            fail("Expected an exception for negative refresh period.");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class.getName(),
                e.getClass().getName());
        }
    }

    @Test
    public void testGetRefreshOriginTime() throws Exception {
        // The refresh origin time (ROT) for the running timer is the *notional*
        // instant w.r.t. the time base that the timer was started had it never
        // been paused or persisted/restored. This allows the timer to be
        // refreshed "in phase". For example, if the timer is showing whole
        // seconds, the it should be refreshed just at the instant that the
        // next full second has elapsed. Therefore, the ROT should allow the
        // calculation of when the next such instant will occur, so the display
        // can be updated at just the right moment to ensure that it appears to
        // be keeping time properly.
        //
        // NOTE: The ROT does not need to be the notional start instant, as any
        // value that allows for the proper phase calculation will suffice.
        // However, the implementation is assumed to provide the ROT in that
        // manner and that is what is asserted in these tests.

        TimerState ts;

        // With no pausing or persistence, things should be simple.
        ts = new TimerState(15_000, true);

        // Timer not running, so ROT is zero.
        assertEquals(0, ts.getRefreshOriginTime());

        ts.startInspection(100_000);
        assertEquals(100_000, ts.getRefreshOriginTime());

        // A "mark()" should have no effect.
        ts.mark(110_000);
        assertEquals(100_000, ts.getRefreshOriginTime());

        // Timer not running, so ROT is zero, again.
        ts.stopInspection(112_000);
        assertEquals(0, ts.getRefreshOriginTime());

        // Start the solve at +1 ms, just to ensure inspection stop time is not
        // being used as the ROT instead of the solve start time.
        ts.startSolve(112_001);
        assertEquals(112_001, ts.getRefreshOriginTime());

        // A "mark()" should have no effect.
        ts.mark(120_000);
        assertEquals(112_001, ts.getRefreshOriginTime());

        // Timer not running, so ROT is zero, again.
        ts.stopSolve(130_999);
        assertEquals(0, ts.getRefreshOriginTime());

        // ======= NEW TimerState! =======
        // Persistence to JSON while the inspection timer is running will lose
        // the original inspection start instant (as the time base may change
        // if the system uptime is used and the device is rebooted before the
        // "TimerState" instance is restored from JSON). The time that elapsed
        // before the persistence, the real time that elapsed while persisted,
        // and the instant w.r.t to the new time base that the timer was
        // restored all have a bearing on the calculation of the new ROT.
        ts = ts.newUnusedState();
        ts.startInspection(240_000);
        assertEquals(240_000, ts.getRefreshOriginTime());

        // Notionally, timer was started when the system uptime was 4 minutes
        // (240 s) and 5.678 s had elapsed before it was persisted while it was
        // still running. The real/wall time at the instant of persistence was
        // 1 hour after the Unix epoch instant.
        JSONObject json = ts.toJSON(245_678, 3_600_000);

        // Notionally, the system was rebooted very quickly! Only 6.1 seconds
        // of real/wall time elapsed since persistence and the system had
        // already been "up" for 2.5 seconds when the timer was restored.
        final TimerState tsR = TimerState.fromJSON(json, 2_500, 3_606_100);

        // Therefore, 5.678 s had elapsed before persistence and 6.100 s had
        // elapsed since persistence, so the elapsed time is 11.778 s. That was
        // the elapsed time when the system uptime used as the time base was at
        // 2.5 s, so the ROT is 11.778 s before that instant, or -9.278 s w.r.t.
        // the time base. (Negative ROT values are fully supported.)
        assertTrue(tsR.isInspectionRunning()); // Just in case....
        assertEquals(-9_278, tsR.getRefreshOriginTime());
        tsR.mark(4_000); // Should have no effect.
        assertEquals(-9_278, tsR.getRefreshOriginTime());

        tsR.stopInspection(4_500); // +2 s after restoration.
        assertEquals(0, tsR.getRefreshOriginTime());

        // The solve timer should still work after restoration.
        tsR.startSolve(100_000);
        assertEquals(100_000, tsR.getRefreshOriginTime());

        // If the solve timer is paused, the ROT will "shift" by the duration
        // of the pause. While paused, the ROT must be zero.
        tsR.pauseSolve(110_000);
        assertEquals(0, tsR.getRefreshOriginTime());

        tsR.resumeSolve(123_456);
        // Was running for 10 s before being paused and was paused for 13.456 s,
        // so the notional start instant is the instant that is 10 s before the
        // instant is it resumed.
        assertEquals(113_456, tsR.getRefreshOriginTime());

        // The solve timer ROT should also survive JSON persistence if persisted
        // while running. Persist to JSON after 10 more seconds of elapsed time
        // and then restore 60 seconds later when the system has been up 50
        // seconds, for 80 seconds total elapsed time.
        json = tsR.toJSON(133_456, 7_200_000);
        final TimerState tsR2 = TimerState.fromJSON(json, 50_000, 7_260_000);

        assertEquals(-30_000, tsR2.getRefreshOriginTime());
        tsR2.stopSolve(55_000);
        assertEquals(0, tsR2.getRefreshOriginTime());
        assertEquals(85_000, tsR2.getElapsedSolveTime());
    }

    @Test
    public void testGetPenalties() throws Exception {
        // Test that once a Solve is set *and* the timer state is stopped, the
        // penalties come from the "Solve", not the timer state.
        final TimerState ts = new TimerState(0, false);
        final Penalties penPre1x2s
            = Penalties.NO_PENALTIES.incurPreStartPenalty(Penalty.PLUS_TWO);
        final Penalties penPost2x2s = Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.PLUS_TWO)
                .incurPostStartPenalty(Penalty.PLUS_TWO);

        ts.incurPreStartPenalty(Penalty.PLUS_TWO);

        ts.startSolve(100_000);
        // NOTE: The timer is considered stopped when the stage is "STOPPED",
        // not just when "stopSolve()" is called.
        ts.stopSolve(112_345); // 12.345 s later (not including penalties).

        assertEquals(12_345, ts.getElapsedSolveTime());
        assertEquals(14_345, ts.getResultTime()); // Including 1x"+2"
        assertEquals(penPre1x2s, ts.getPenalties());

        // Add the Solve, but do not yet transition the timer to "STOPPED".
        final Solve solve = new Solve(
            100,
            14_000, // 10 seconds elapsed + 2x2-second penalties
            PuzzleType.TYPE_333, "Normal", 123, "U1 L1", penPost2x2s,
            "Hello, world!", false);

        ts.setSolve(solve);
        // These should not have changed.
        assertEquals(12_345, ts.getElapsedSolveTime());
        assertEquals(14_345, ts.getResultTime()); // Including 1x"+2"
        assertEquals(penPre1x2s, ts.getPenalties());

        ts.setStage(TimerStage.STOPPED);
        // Now, the values should come from the "Solve".
        assertEquals(10_000, ts.getElapsedSolveTime());
        assertEquals(14_000, ts.getResultTime());
        assertEquals(penPost2x2s, ts.getPenalties());
    }

    @Test
    public void testIncurPreStartPenalty() throws Exception {
        // Test that once the timer state is stopped, penalties cannot be added.
        final TimerState ts = new TimerState(0, false);
        final Penalties penPre2x2s = Penalties.NO_PENALTIES
            .incurPreStartPenalty(Penalty.PLUS_TWO)
            .incurPreStartPenalty(Penalty.PLUS_TWO);

        ts.incurPreStartPenalty(Penalty.PLUS_TWO);
        ts.incurPreStartPenalty(Penalty.PLUS_TWO);

        ts.startSolve(100_000);
        // NOTE: The timer is considered stopped when the stage is "STOPPED",
        // not just when "stopSolve()" is called.
        ts.stopSolve(112_345); // 12.345 s later (not including penalties).

        assertEquals(12_345, ts.getElapsedSolveTime());
        assertEquals(16_345, ts.getResultTime()); // Including 2x"+2"
        assertEquals(penPre2x2s, ts.getPenalties());

        ts.setStage(TimerStage.STOPPED);
        try {
            ts.incurPreStartPenalty(Penalty.PLUS_TWO);
            fail("Expected exception incurring penalty when timer is stopped.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }
    }

    @Test
    public void testIsReset() throws Exception {
        final TimerState ts = new TimerState(15_000, false);

        assertTrue(ts.isReset()); // Reset is synonymous with "UNUSED".

        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);

            switch (stage) {
                case STARTING:
                case INSPECTION_HOLDING_FOR_START:
                case INSPECTION_READY_TO_START:
                case INSPECTION_STARTED:
                case INSPECTION_SOLVE_HOLDING_FOR_START:
                case INSPECTION_SOLVE_READY_TO_START:
                case SOLVE_HOLDING_FOR_START:
                case SOLVE_READY_TO_START:
                case SOLVE_STARTED:
                case CANCELLING:
                case STOPPING:
                case STOPPED:
                    assertFalse(ts.isReset());
                    break;

                case UNUSED:
                    assertTrue(ts.isReset());
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer stage: " + stage);
            }
        }
    }

    @Test
    public void testIsStopped() throws Exception {
        final TimerState ts = new TimerState(15_000, false);

        // The timer is "UNUSED" by default, but this is not the same as being
        // stopped, as being "STOPPED" means that the timer has a result.
        assertFalse(ts.isStopped());

        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);

            switch (stage) {
                case UNUSED:
                case STARTING:
                case INSPECTION_HOLDING_FOR_START:
                case INSPECTION_READY_TO_START:
                case INSPECTION_STARTED:
                case INSPECTION_SOLVE_HOLDING_FOR_START:
                case INSPECTION_SOLVE_READY_TO_START:
                case SOLVE_HOLDING_FOR_START:
                case SOLVE_READY_TO_START:
                case SOLVE_STARTED:
                case CANCELLING:
                case STOPPING:
                    assertFalse(ts.isStopped());
                    break;

                case STOPPED:
                    assertTrue(ts.isStopped());
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer stage: " + stage);
            }
        }
    }

    @Test
    public void testIsRunning() throws Exception {
        // NOTE: Full code coverage is not possible, as the code that checks if
        // a new value has been added to the "TimerStage" enum without adding a
        // case to the "switch" statement in "isRunning()" cannot be exercised
        // without impractical modifications to "TimerStage" just for testing.
        // At this time, the IDE has no way to disable coverage line-by-line.
        final TimerState ts = new TimerState(15_000, false);

        assertFalse(ts.isRunning());

        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);

            switch (stage) {
                case UNUSED:
                case STARTING:
                case INSPECTION_HOLDING_FOR_START:
                case INSPECTION_READY_TO_START:
                case SOLVE_HOLDING_FOR_START:
                case SOLVE_READY_TO_START:
                case CANCELLING:
                case STOPPING:
                case STOPPED:
                    assertFalse(ts.isRunning());
                    break;

                case INSPECTION_STARTED:
                case INSPECTION_SOLVE_HOLDING_FOR_START:
                case INSPECTION_SOLVE_READY_TO_START:
                case SOLVE_STARTED:
                    assertTrue(ts.isRunning());
                    break;

                //noinspection UnnecessaryDefault (needed for futureproofing)
                default: fail("Test does not cover timer stage: " + stage);
            }
        }
    }

    @Test
    public void testIsInspectionRunningAndIsSolveRunning() throws Exception {
        final TimerState ts = new TimerState(15_000, false);

        assertFalse(ts.isInspectionRunning());
        assertFalse(ts.isSolveRunning());

        // Inspection is only "running" if the inspection timer has been
        // started and not yet stopped. The timer stage is not relevant and
        // should never be relevant, so keep testing that the stage is ignored.
        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);
            assertFalse(ts.isInspectionRunning());
            assertFalse(ts.isSolveRunning());
        }

        ts.startInspection(1_000);
        assertTrue(ts.isInspectionRunning());
        assertFalse(ts.isSolveRunning());

        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);
            assertTrue(ts.isInspectionRunning());
            assertFalse(ts.isSolveRunning());
        }

        ts.stopInspection(2_000);
        assertFalse(ts.isInspectionRunning());
        assertFalse(ts.isSolveRunning());

        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);
            assertFalse(ts.isInspectionRunning());
            assertFalse(ts.isSolveRunning());
        }

        ts.startSolve(2_000);
        assertFalse(ts.isInspectionRunning());
        assertTrue(ts.isSolveRunning());

        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);
            assertFalse(ts.isInspectionRunning());
            assertTrue(ts.isSolveRunning());
        }

        ts.stopSolve(7_000);
        assertFalse(ts.isInspectionRunning());
        assertFalse(ts.isSolveRunning());

        for (TimerStage stage : TimerStage.values()) {
            ts.setStage(stage);
            assertFalse(ts.isInspectionRunning());
            assertFalse(ts.isSolveRunning());
        }
    }

    @Test
    public void testInspectionTimer() throws Exception {
        // Other test methods for the solve timer check that there is no
        // interference between the inspection timer and the solve timer,
        // so such concerns are not re-tested here. Similarly, the handling
        // of the "extra time" fields is tested in the JSON test cases.
        TimerState ts;

        // Use an inspection duration of 9.001 seconds, as that makes it very
        // unlikely that a default value will match it and mask bugs.
        ts = new TimerState(9_001, false);

        // Until started, the elapsed time is reported as zero and the "end"
        // instant cannot be predicted.
        assertEquals(0, ts.getElapsedInspectionTime());
        assertEquals(0, ts.getInspectionEnd());
        // The remaining inspection time must *not* include the overrun period.
        assertEquals(9_001, ts.getRemainingInspectionTime());

        // Set a mark in the future (relative to "startInspection()" below). It
        // should be reset and not cause any problems.
        ts.mark(105_000);

        ts.startInspection(100_000); // Start at "T".

        // Until there is a "mark()" the elapsed time is reported as zero and
        // the full inspection period should still be remaining.
        assertEquals(0, ts.getElapsedInspectionTime());
        assertEquals(9_001, ts.getRemainingInspectionTime());
        assertEquals(109_001, ts.getInspectionEnd());

        ts.mark(105_000); // T+5s
        assertEquals(5_000, ts.getElapsedInspectionTime());
        assertEquals(4_001, ts.getRemainingInspectionTime());
        assertEquals(109_001, ts.getInspectionEnd());

        ts.stopInspection(108_000); // Stop at T+8s
        assertEquals(8_000, ts.getElapsedInspectionTime());
        assertEquals(1_001, ts.getRemainingInspectionTime());
        assertEquals(0, ts.getInspectionEnd()); // Already stopped, so zero.

        // After stopping, any new "mark()" should have no effect. The recorded
        // elapsed and remaining times should be preserved unchanged.
        ts.mark(109_000);
        assertEquals(8_000, ts.getElapsedInspectionTime());
        assertEquals(1_001, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();

        // Similar to above, but with no "mark()" before "startInspection()".
        // Check that the elapsed inspection time is zero until marked.
        ts.startInspection(100_000); // Start at "T".

        assertEquals(0, ts.getElapsedInspectionTime());
        assertEquals(9_001, ts.getRemainingInspectionTime());

        ts.mark(105_000); // T+5s
        assertEquals(5_000, ts.getElapsedInspectionTime());
        assertEquals(4_001, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();

        // Set a mark before starting. The mark is in the past relative to the
        // start time of the inspection period, so it should be ignored and not
        // result in a negative elapsed inspection time. As the mark should be
        // reset on starting, the elapsed time must be zero until marked again.
        ts.mark(90_000); // Should be ignored.
        ts.startInspection(100_000); // Start at "T".
        assertEquals(0, ts.getElapsedInspectionTime()); // No valid mark, so 0.
        assertEquals(9_001, ts.getRemainingInspectionTime());

        ts.mark(105_000); // Should be respected.
        assertEquals(5_000, ts.getElapsedInspectionTime());
        assertEquals(4_001, ts.getRemainingInspectionTime());

        // Stop inspection at a point in time before the previous mark. There
        // is no ideal way to handle this, so the approach is to accept the
        // given stop time as the correct one.
        ts.stopInspection(104_000);
        assertEquals(4_000, ts.getElapsedInspectionTime());
        assertEquals(5_001, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();

        // Count down to zero and then stop.
        ts.startInspection(100_000); // Start at "T".
        ts.stopInspection(109_001); // T+5s
        assertEquals(9_001, ts.getElapsedInspectionTime());
        assertEquals(0, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();

        // Overrun and stop within the overrun period. The remaining time
        // should turn negative.
        ts.startInspection(100_000); // Start at "T".
        ts.stopInspection(110_001); // T+9.001s+1s overrun
        assertEquals(10_001, ts.getElapsedInspectionTime());
        assertEquals(-1_000, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();

        // Overrun and stop *exactly* at the end of the overrun period.
        ts.startInspection(100_000); // Start at "T".
        ts.stopInspection(111_001); // T+9.001s+2s overrun
        assertEquals(11_001, ts.getElapsedInspectionTime());
        assertEquals(-2_000, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();

        // Overrun and stop *after* the end of the overrun period. The elapsed
        // time should be limited to the normal inspection duration and the
        // overrun duration, as anything longer cannot be valid.
        ts.startInspection(100_000); // Start at "T".
        ts.stopInspection(121_001); // T+9.001s+2s overrun + 10s extra (ignored)
        assertEquals(11_001, ts.getElapsedInspectionTime());
        assertEquals(-2_000, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();

        // Stop at instant "-1" this should be interpreted as stopping exactly
        // at the end of the overrun period, ignoring any time marks.
        ts.startInspection(100_000); // Start at "T".
        ts.mark(121_001); // 10s after the maximum end instant.
        ts.stopInspection(-1);
        assertEquals(11_001, ts.getElapsedInspectionTime());
        assertEquals(-2_000, ts.getRemainingInspectionTime());

        // ======= NEW TimerState! =======
        // NO INSPECTION PERIOD.
        ts = new TimerState(0, false);

        assertEquals(0, ts.getElapsedInspectionTime());
        assertEquals(0, ts.getRemainingInspectionTime());
        assertEquals(0, ts.getInspectionEnd()); // No inspection, so zero.
    }

    @Test
    public void testInspectionTimerErrors() throws Exception {
        TimerState ts;

        ts = new TimerState(15_000, false);

        try {
            ts.stopInspection(110_000);
            fail("Expected exception stopping inspection before starting it.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        ts.startInspection(100_000);

        try {
            ts.startInspection(100_000);
            fail("Expected exception starting inspection if already started.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        ts.stopInspection(110_000);

        try {
            ts.stopInspection(110_000);
            fail("Expected exception stopping inspection if already stopped.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        // ======= NEW "TimerState" =======
        // Inspection is disabled this time.
        ts = new TimerState(0, true);

        try {
            ts.startInspection(100_000);
            fail("Expected exception starting inspection if disabled.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        try {
            ts.stopInspection(110_000);
            fail("Expected exception stopping inspection if disabled.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }
    }

    @Test
    public void testSolveTimer() throws Exception {
        final TimerState ts = new TimerState(15_000, false);

        // The elapsed solve time does *not* include time penalties. Add a
        // penalty and test later that it is always ignored.
        ts.incurPreStartPenalty(Penalty.PLUS_TWO);

        // The elapsed solve time should not be confused with the elapsed
        // inspection time. Therefore, ensure that the inspection time is
        // non-zero and keep checking that it is not altered by operations that
        // are only expected to affect the elapsed solve time. A "mark()" is
        // set here to ensure that, after the solve timer starts, the old mark
        // for an instant in time before the solve timer started, is not used
        // to calculate the elapsed solve time.
        ts.startInspection(100_000);
        ts.mark(105_000);
        assertEquals(5_000, ts.getElapsedInspectionTime());
        ts.stopInspection(109_876); // 9.876 s after starting.
        assertEquals(9_876, ts.getElapsedInspectionTime());

        // Before the solve timer is started, it should report zero time.
        assertEquals(0, ts.getElapsedSolveTime());

        // There is no need for the start time of the solve timer to be the
        // same as the stop time of the inspection timer (though the same value
        // is likely in practice). Start at the next round uptime instant:
        assertFalse(ts.isSolveRunning());
        assertFalse(ts.isSolvePaused());
        ts.startSolve(110_000); // Started at "T".
        assertTrue(ts.isSolveRunning());
        assertFalse(ts.isSolvePaused());

        // Until there is a "mark()", the solve timer should report zero time.
        assertEquals(0, ts.getElapsedSolveTime());
        ts.mark(115_432); // Still running at T+5.432 seconds.
        assertEquals(5_432, ts.getElapsedSolveTime());

        // If the timer is paused, the time at the instant of pausing should
        // hold until the timer is resumed, regardless of any "mark()".
        ts.pauseSolve(116_543); // Paused at T+6.543 seconds.
        assertTrue(ts.isSolveRunning()); // Paused is not *stopped*.
        assertTrue(ts.isSolvePaused());
        assertEquals(6_543, ts.getElapsedSolveTime());
        ts.mark(120_000); // Paused, so this should have no effect.
        assertEquals(6_543, ts.getElapsedSolveTime());

        // If the timer is resumed, it should hold the same value as when
        // paused until there is a new "mark()".
        ts.resumeSolve(200_000); // Resumed at new "T".
        assertTrue(ts.isSolveRunning());
        assertFalse(ts.isSolvePaused());
        assertEquals(6_543, ts.getElapsedSolveTime());
        ts.mark(201_234);
        assertEquals(7_777, ts.getElapsedSolveTime()); // 6.543 + 1.234

        // The inspection time should still be the same as before.
        assertEquals(9_876, ts.getElapsedInspectionTime());

        // If a Solve reference is set and the timer is stopped, the elapsed
        // solve time is taken from that Solve, otherwise it is taken from the
        // timer state. NOTE: Normally, the elapsed time from the Solve would
        // be identical to the elapsed time from the TimerState. However, for
        // testing it is necessary to make them different, so that the source
        // of the value can be confirmed. The time value must not be rounded,
        // i.e., it should use "Solve.getExactTime()", not "Solve.getTime()",
        // as the latter performs rounding. Both of those sources include time
        // penalties, so the value from "TimerState.getElapsedSolveTime()" must
        // remove any time penalties from its result.

        // NOTE: The timer is considered stopped when the stage is "STOPPED",
        // not just when "stopSolve()" is called.
        ts.stopSolve(205_000); // 6.543 + 5 = 11.543 elapsed (excl. penalties)
        assertEquals(11_543, ts.getElapsedSolveTime());
        assertEquals(13_543, ts.getResultTime()); // Including 1x"+2"

        // "STOPPED", but "Solve" is null: values do *not* come from the Solve.
        ts.setStage(TimerStage.STOPPED);
        assertEquals(11_543, ts.getElapsedSolveTime());
        assertEquals(13_543, ts.getResultTime()); // Including 1x"+2"

        // Add the Solve, but do not yet transition the timer to "STOPPED".
        final Solve solve = new Solve(
            100,
            14_000, // 10 seconds elapsed + 2x2-second penalties
            PuzzleType.TYPE_333, "Normal", 123,
            "U1 L1",
            Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.PLUS_TWO)
                .incurPostStartPenalty(Penalty.PLUS_TWO),
            "Hello, world!", false);

        // Solve is non-null, but timer is not "STOPPED": values do *not* come
        // from the Solve.
        ts.setStage(TimerStage.STOPPING);
        ts.setSolve(solve);
        assertSame(solve, ts.getSolve());
        assertEquals(11_543, ts.getElapsedSolveTime());
        assertEquals(13_543, ts.getResultTime());

        // "STOPPED" *and* non-null Solve: values come from the "Solve".
        ts.setStage(TimerStage.STOPPED);
        assertEquals(10_000, ts.getElapsedSolveTime());
        assertEquals(14_000, ts.getResultTime());
    }

    @Test
    public void testSolveTimerErrors() throws Exception {
        TimerState ts;

        // ======= NEW "TimerState"! =======
        // Inspection is enabled, so inspection must complete before solving.
        ts = new TimerState(15_000, false);

        try {
            ts.startSolve(100_000);
            fail("Expected error starting solve before starting inspection.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        ts.startInspection(150_000);

        try {
            ts.startSolve(200_000);
            fail("Expected error starting solve before stopping inspection.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        // ======= NEW "TimerState"! =======
        // Inspection is *disabled*, just to skip unneeded start/stop code.
        ts = new TimerState(0, true);
        ts.startSolve(100_000);

        try {
            ts.startSolve(100_000);
            fail("Expected exception starting solve if already started.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        ts.pauseSolve(110_000);

        try {
            ts.startSolve(120_000);
            fail("Expected exception starting solve if started and paused.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        try {
            ts.pauseSolve(120_000);
            fail("Expected exception pausing solve if already paused.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        ts.resumeSolve(130_000);

        try {
            ts.startSolve(140_000);
            fail("Expected exception starting solve on resuming from paused.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        try {
            ts.resumeSolve(140_000);
            fail("Expected exception resuming solve when not paused.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        ts.stopSolve(140_000);

        try {
            ts.stopSolve(150_000);
            fail("Expected exception stopping solve if already stopped.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        try {
            ts.startSolve(150_000);
            fail("Expected exception starting solve if already stopped.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        try {
            ts.pauseSolve(150_000);
            fail("Expected exception pausing solve if already stopped.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        try {
            ts.resumeSolve(150_000);
            fail("Expected exception resuming solve if already stopped.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }
    }

    @Test
    public void testCommitSolve() throws Exception {
        final TimerState ts = new TimerState(0, false);

        ts.startSolve(100_000);
        ts.stopSolve(110_111);
        ts.incurPreStartPenalty(Penalty.PLUS_TWO);

        try {
            ts.commitSolve(3_600_000);
            fail("Expected exception committing solve before it is set.");
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        // Committing a solve will replace the "Solve" instance with another
        // that takes the elapsed solve time and penalties from the "TimerState"
        // and sets the date-time stamp to the given value. The "Solve" is
        // initialised to have different values form those on the "TimerState".
        final Solve solve1 = new Solve(
            100,
            24_000, // 20 seconds elapsed + 2x2-second penalties
            PuzzleType.TYPE_444, "Cross Training", 123,
            "U1 L1",
            Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.PLUS_TWO)
                .incurPostStartPenalty(Penalty.PLUS_TWO)
                .incurPostStartPenalty(Penalty.DNF),
            "Hello, world!", false);

        ts.setSolve(solve1);
        assertSame(solve1, ts.getSolve());

        ts.commitSolve(3_600_000);

        final Solve solve2 = ts.getSolve();

        // Only the +2 penalty from the timer state should be set and it should
        // be added to the 10.111 s elapsed time when reported by the "Solve".
        assertNotNull(solve2);
        assertNotSame(solve1, solve2);

        assertEquals(12_110, solve2.getTime()); // Rounded, incl. penalties.
        assertEquals(12_111, solve2.getExactTime()); // Incl. penalties.
        assertEquals(PuzzleType.TYPE_444, solve2.getPuzzleType());
        assertEquals(3_600_000, solve2.getDate()); // From timer state.
        assertEquals("U1 L1", solve2.getScramble()); // Unchanged.
        assertEquals("Cross Training", solve2.getCategory()); // Unchanged.
        assertEquals("Hello, world!", solve2.getComment()); // Unchanged.
    }

    @Test
    public void testSetStageAndGetStage() throws Exception {
        final TimerState ts = new TimerState(0, false);

        // These methods are simple field accessors; they perform no validation.
        // Validation of which stages are allowed to follow others is performed
        // by the "PuzzleTimer" class.

        assertEquals("Default value", TimerStage.UNUSED, ts.getStage());
        ts.setStage(TimerStage.STOPPED);
        assertEquals(TimerStage.STOPPED, ts.getStage());
    }

    @Test
    public void testMark() throws Exception {
        // "mark()" must be able to avert disaster in the event that the system
        // uptime is reset. There is no simple way to correct such a change, so
        // the approach is simply to do whatever is necessary to avoid ending
        // up with a negative value for the elapsed inspection or solve time.

        TimerState ts;

        // ======= NEW TimerState! =======
        ts = new TimerState(15_000, false);
        assertEquals(TimerState.NEVER, ts.getMark());
        // If neither time is running, then any new mark must be greater than
        // any past mark.
        ts.mark(100_000); // First mark.
        assertEquals(100_000, ts.getMark()); // Always accepted.
        ts.mark(90_000); // New mark before previous mark.
        assertEquals(100_000, ts.getMark()); // Unchanged.
        ts.mark(110_000); // New mark after previous mark.
        assertEquals(110_000, ts.getMark()); // Changed.

        // ======= NEW TimerState! =======
        ts = ts.newUnusedState();
        // If inspection is running and no mark has been set, then the mark is
        // set to the greater of the inspection start instant and the new mark.
        ts.startInspection(100_000);
        ts.mark(50_000); // Earlier in time than the inspection start...
        assertEquals(100_000, ts.getMark()); // ...so use the inspection start.

        // If inspection is running and a mark is set (to 100,000, above), then
        // the mark is set to the greater of the inspection start instant, the
        // old mark and the new mark.
        ts.mark(60_000);// Still before inspection start.
        assertEquals(100_000, ts.getMark()); // Unchanged.
        ts.mark(110_000);// After inspection start.
        assertEquals(110_000, ts.getMark()); // Changed.
        ts.mark(105_000); // Before the previous mark.
        assertEquals(110_000, ts.getMark()); // Unchanged.

        // ======= NEW TimerState! =======
        ts = new TimerState(0, false); // NO INSPECTION!
        // If the solve timer is running and no mark has been set, then the mark
        // is set to the greater of the solve start instant and the new mark.
        ts.startSolve(100_000);
        ts.mark(50_000); // Earlier in time than the solve start...
        assertEquals(100_000, ts.getMark()); // ...so use the solve start.

        // If the solve timer is running and a mark is set (to 100,000, above),
        // then the mark is set to the greater of the solve start instant, the
        // old mark and the new mark.
        ts.mark(60_000);// Still before solve start.
        assertEquals(100_000, ts.getMark()); // Unchanged.
        ts.mark(110_000);// After solve start.
        assertEquals(110_000, ts.getMark()); // Changed.
        ts.mark(105_000); // Before the previous mark.
        assertEquals(110_000, ts.getMark()); // Unchanged.

        // ======= NEW TimerState! =======
        ts = new TimerState(15_000, false);
        // Starting inspection should always reset the mark, whatever its value.
        ts.mark(200_000);
        assertEquals(200_000, ts.getMark());
        ts.startInspection(100_000); // Earlier than mark, but still reset mark.
        assertEquals(TimerState.NEVER, ts.getMark());
        ts.mark(110_000);
        assertEquals(110_000, ts.getMark());
        // Stopping inspection does not affect the mark.
        ts.stopInspection(113_000);
        assertEquals(110_000, ts.getMark());

        // Starting the solve timer should always reset the mark, too.
        ts.startSolve(114_000);
        assertEquals(TimerState.NEVER, ts.getMark());
        ts.mark(120_000);
        assertEquals(120_000, ts.getMark());

        // Pausing the solve timer resets the mark. Setting a mark while solved
        // does not really serve much purpose. The solve start instant is reset
        // while paused, so the only requirement is that the mark is greater
        // than any other mark. As the mark is reset on pausing, the first mark
        // after pausing is accepted.
        ts.pauseSolve(121_000);
        assertEquals(TimerState.NEVER, ts.getMark());
        ts.mark(50_000);
        assertEquals(50_000, ts.getMark()); // Accepted without question.
        ts.mark(100_000);
        assertEquals(100_000, ts.getMark()); // Can go forwards.
        ts.mark(90_000);
        assertEquals(100_000, ts.getMark()); // Cannot go backwards.

        // Resuming the solve timer also resets the mark. Now any new mark must
        // be greater than the resume time and any previous mark.
        ts.resumeSolve(200_000);
        assertEquals(TimerState.NEVER, ts.getMark());
        ts.mark(50_000);
        assertEquals(200_000, ts.getMark()); // Resume time is used instead.
        ts.mark(220_000);
        assertEquals(220_000, ts.getMark()); // Can go forwards.
        ts.mark(210_000);
        assertEquals(220_000, ts.getMark()); // Cannot go backwards.

        // Stopping the solve timer does not affect the mark.
        ts.stopSolve(230_000);
        assertEquals(220_000, ts.getMark());
    }

    @Test
    public void testToJSONAndFromJSON() throws Exception {
        TimerState ts;
        TimerState tsR; // "ts Restored"
        JSONObject json;

        final Solve solve = new Solve(
            100,
            14_000,
            PuzzleType.TYPE_555, "Normal", 123,
            "U1 L1",
            Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.PLUS_TWO)
                .incurPostStartPenalty(Penalty.PLUS_TWO),
            "Hello, world!", false);

        // ===== INSPECTION DISABLED =====
        // No state changes on the timer, yet.
        ts = new TimerState(0, true); // And hold-to-start enabled.
        json = ts.toJSON(100_000, 3_600_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_660_000);
        assertEquals(ts, tsR);

        // Changes to the timer stage, penalties, cues and Solve....
        ts.setSolve(solve);
        ts.setStage(TimerStage.SOLVE_READY_TO_START);
        ts.fireTimerCue(CUE_SOLVE_READY_TO_START);
        ts.incurPreStartPenalty(Penalty.PLUS_TWO);
        json = ts.toJSON(100_000, 3_600_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_660_000);
        assertEquals(ts, tsR);

        // Start solve timer and persist while running.
        ts.startSolve(120_000);
        ts.mark(130_000);
        ts.setRefreshPeriod(234);
        json = ts.toJSON(131_000, 3_600_000); // 11 seconds elapsed.
        tsR = TimerState.fromJSON(json, 50_000, 3_660_000); // 60 seconds later.

        // As the solve timer is running for 60 s while persisted, the original
        // "ts" instance needs a "mark()" to make it match. The mark is the
        // elapsed 11 s before persistence and the 60 s in the persistent state,
        // all relative to the start of the solve timer at time 120,000.
        ts.mark(191_000);

        // Mark and refresh period are not tested by "TimerState.equals(Object)"
        // and are not persisted. The mark should be set to the instant of
        // restoration and the refresh period to the default.
        assertEquals(ts, tsR);
        assertEquals(50_000, tsR.getMark());
        assertEquals(31, tsR.getRefreshPeriod());

        // Pause solve and persist while paused. Real time that elapses while
        // the paused solve is persisted is not counted as elapsed solve time.
        ts.pauseSolve(192_000); // +1 s running before paused
        json = ts.toJSON(192_000, 3_600_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_610_000); // +10 s persisted.
        assertEquals(ts, tsR);

        // Resume paused solve and persist again. This time, the 10 s real time
        // that elapses while persisted will count. So far, the tally is 12 s
        // elapsed while not persisted + 60 elapsed while persisted in a
        // running state = 82 seconds elapsed. This new persist/restore op will
        // add another 10 s of real time for a total of 92 s. The original "ts"
        // needs a mark 10 s after the mark when it was resumed to match the
        // 10 s real time  that "tsR" spent in its persisted state.
        ts.resumeSolve(200_000);
        json = ts.toJSON(200_000, 3_600_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_610_000); // +10 s persisted
        ts.mark(210_000);
        assertEquals(ts, tsR);

        // Stop solve and persist.
        ts.stopSolve(220_000);
        ts.setStage(TimerStage.STOPPED);
        json = ts.toJSON(220_000, 3_600_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_610_000);
        assertEquals(ts, tsR);

        // Exercise the special case where all timer cues have been fired.
        for (TimerCue cue : TimerCue.values()) {
            ts.fireTimerCue(cue);
        }
        json = ts.toJSON(230_000, 3_600_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_660_000);
        assertEquals(ts, tsR);

        // Force an error by corrupting the "TimerStage" enum value. The
        // underlying IllegalArgumentException should be wrapped up in a
        // JSONException.
        json = ts.toJSON(230_000, 3_600_000);
        json.put("ts", "BAD_STAGE_ENUM_VALUE_NAME");
        try {
            TimerState.fromJSON(json, 50_000, 3_660_000);
            fail("Expected exception if JSON is corrupt.");
        } catch (Exception e) {
            assertEquals(JSONException.class, e.getClass());

            // NOTE: The JSON implementation used for these tests appears to
            // be different from that packaged with Android in the way that
            // JSONException behaves. In the library used for testing,
            // "getCause()" is overridden and cannot be used to access the
            // cause properly. Just skip these assertions for now.

//            assertNotNull(e.getCause());
//            assertEquals(IllegalArgumentException.class,
//                e.getCause().getClass());
        }

        // ===== INSPECTION ENABLED =====
        // Not as comprehensive this time, as the elapsed solve time and some
        // other values were tested above.
        ts = new TimerState(12_000, false); // And hold-to-start disabled.

        // Persist/restore  before starting inspection timer. Nothing changes.
        json = ts.toJSON(90_000, 3_600_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_610_000); // 10 seconds later.
        assertEquals(ts, tsR);

        // Start inspection timer and persist while running.
        ts.startInspection(100_000);
        json = ts.toJSON(105_000, 3_600_000); // 5 seconds elapsed.
        tsR = TimerState.fromJSON(json, 50_000, 3_606_000); // 6 seconds later.
        ts.mark(111_000);
        assertEquals(ts, tsR);

        // Stop inspection timer and persist/restore again.
        ts.stopInspection(112_000); // +1 s more before stopping (12 in total).
        json = ts.toJSON(112_000, 3_607_000);
        tsR = TimerState.fromJSON(json, 50_000, 3_667_000); // 60 seconds later.
        assertEquals(ts, tsR);
    }

    @Test
    public void testToString() throws Exception {
        // NOTE: This is just done to complete code coverage and find glaring
        // exceptions. "toString()" is used only for debugging and logging, so
        // it is not critical and does not warrant any in-depth testing.
        TimerState ts;
        String oldString;

        ts = new TimerState(15_000, false);
        assertNotNull(ts.toString());

        oldString = ts.toString();
        ts.startInspection(100_000);
        assertNotNull(ts.toString());
        assertNotEquals(oldString, ts.toString());

        oldString = ts.toString();
        ts.incurPreStartPenalty(Penalty.DNF);
        assertNotNull(ts.toString());
        assertNotEquals(oldString, ts.toString());

        oldString = ts.toString();
        ts.stopInspection(110_000);
        ts.startSolve(110_000);
        ts.pauseSolve(120_000);
        assertNotNull(ts.toString());
        assertNotEquals(oldString, ts.toString());

        // A bit messy to exercise the conditional expressions for post-start
        // DNF penalties: set a "Solve" and go to "STOPPED" (if not "STOPPED",
        // there will be no delegation from "getPenalties()" to the "Solve").
        ts.stopSolve(130_000);
        ts.setStage(TimerStage.STOPPED);

        final Solve solve = new Solve(
            100, 20_000, PuzzleType.TYPE_333, "Normal", 123, "U1 L1",
            Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.PLUS_TWO)
                .incurPostStartPenalty(Penalty.DNF),
            "Hello, world!", false);

        oldString = ts.toString();
        ts.setSolve(solve);
        assertNotNull(ts.toString());
        assertNotEquals(oldString, ts.toString());

        // ======== NEW "TimerState"! ========
        // Now, briefly, with inspection disabled.
        ts = new TimerState(0, true);
        assertNotNull(ts.toString());

        oldString = ts.toString();
        ts.startSolve(100_000);
        ts.stopSolve(120_000);
        ts.setStage(TimerStage.STOPPED);
        assertNotNull(ts.toString());
        assertNotEquals(oldString, ts.toString());

        oldString = ts.toString();
        ts.setSolve(solve); // So it will have a post-start DNF.
        assertNotNull(ts.toString());
        assertNotEquals(oldString, ts.toString());
    }
}
