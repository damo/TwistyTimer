package com.aricneto.twistytimer.timer;

import android.os.Parcel;

import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;

import org.json.JSONObject;
import org.junit.Test;

import static com.aricneto.twistytimer.timer.TimerCue.*;
import static org.junit.Assert.*;

/**
 * Unit tests for the aspects of the {@link TimerState} class that must be
 * tested in an Android container. Specifically, the class's implementation of
 * the {@code android.os.Parcelable} API cannot be tested in a simple JUnit
 * test, as the {@code android.os.Parcel} class has no mock implementation that
 * can run outside of an Android context, so an "instrumented" test is required.
 * The tests that do not depend on Android are implemented in a separate, plain
 * JUnit test case class.
 *
 * @author damo
 */
public class TimerStateInstrumentedTestCase {
    @Test
    public void testParcelable() throws Exception {
        // Test with default and non-default values to ensure that all expected
        // fields are properly restored to the values that they had before, not
        // just to the default value, or, if they had a default value, that the
        // correct default value is preserved.

        final TimerState ts1 = new TimerState(3_000L, false);
        final TimerState ts2 = new TimerState(-1L, true); // No inspection.

        // No special contents should be flagged (e.g., file descriptors).
        assertEquals(0, ts1.describeContents());
        assertEquals(0, ts2.describeContents());

        // A simple check for each timer state that some cues can(not) be fired
        // and that if one is fired now, it cannot be re-fired now and cannot
        // be re-fired after the state is saved and then restored (later).
        assertTrue(ts1.canFireTimerCue(CUE_INSPECTION_STARTED));
        assertTrue(ts1.fireTimerCue(CUE_INSPECTION_STARTED)); // Fire!
        assertTrue(ts1.canFireTimerCue(CUE_SOLVE_STARTED));
        assertFalse(ts1.canFireTimerCue(CUE_INSPECTION_HOLDING_FOR_START));

        assertTrue(ts2.canFireTimerCue(CUE_SOLVE_HOLDING_FOR_START));
        assertTrue(ts2.fireTimerCue(CUE_SOLVE_HOLDING_FOR_START)); // Fire!
        assertTrue(ts2.canFireTimerCue(CUE_SOLVE_STARTED));
        assertFalse(ts2.canFireTimerCue(CUE_INSPECTION_STARTED));

        // Set a non-default refresh period can be restored.
        ts1.setRefreshPeriod(41L);
        ts2.setRefreshPeriod(42L);

        // Incur some pre-start penalties.
        ts1.incurPreStartPenalty(Penalty.PLUS_TWO);
        ts1.incurPreStartPenalty(Penalty.PLUS_TWO);

        // Set a non-default stage.
        ts1.setStage(TimerStage.STOPPED);
        ts2.setStage(TimerStage.SOLVE_STARTED);

        // Simulate the running of the timers.
        //
        // Let "ts1" run for 2 seconds of its its 3-second inspection period
        // before starting the solve timer and letting it run for 5 seconds and
        // then stopping it. Throw in a pause/resume just for good measure.
        // There will be a "+2" penalty in there, too (added above).
        ts1.startInspection(100_000L);
        ts1.stopInspection(102_000L); // Stop after two seconds.

        ts1.startSolve(102_000L);
        ts1.pauseSolve(104_500); // Pause after 2.5 seconds.
        ts1.resumeSolve(164_500); // Resume 1 minute later.
        ts1.stopSolve(167_000L); // Stop after 2.5 more seconds (5 total)

        assertEquals(2_000L, ts1.getElapsedInspectionTime());
        assertEquals(5_000L, ts1.getElapsedSolveTime());

        // Let "ts2" (no inspection period) start the solve timer, but it will
        // stay running when it is persisted. Just perform a "mark()" now after
        // a notional 7 seconds and then again after restoring it a notional
        // one second later.
        ts2.startSolve(200_000L);
        ts2.mark(207_000L);

        assertEquals(7_000L, ts2.getElapsedSolveTime());

        // Save to a "Parcel" and restore from that "Parcel".
        final Parcel p1 = Parcel.obtain();
        final Parcel p2 = Parcel.obtain();

        ts1.writeToParcel(p1, 0);
        ts2.writeToParcel(p2, 0);

        p1.setDataPosition(0);
        p2.setDataPosition(0);

        final TimerState ts1P = TimerState.CREATOR.createFromParcel(p1);
        final TimerState ts2P = TimerState.CREATOR.createFromParcel(p2);

        p1.recycle();
        p2.recycle();

        // "TimerState.equals()" can do most of the comparisons. If "equals()"
        // fails, it might be best to use separate assertions. A new "mark()"
        // needs to be set before the restored "ts2P" can be interrogated, as
        // the old mark from "ts2" is not persisted (intentionally). Apply the
        // same time mark to both, so that they should still be equal.
        assertEquals(ts1, ts1P);

        ts2.mark(208_000L);
        ts2P.mark(208_000L);
        assertEquals(ts2, ts2P);

        // Stop both "ts2" and "ts2P" to ensure they they behave the same way
        // when they are compared again.
        ts2.stopSolve(210_000L);
        ts2P.stopSolve(210_000L);
        assertEquals(ts2, ts2P);

        // Persist to JSON and then restore, as that is the only operation that
        // updates the "TimerState.mInspExtraTime" field. Pausing the solve
        // timer changes "TimerState.mSolveExtraTime", but the JSON round- trip
        // will update both fields. The JSON round-trip is the only way to
        // ensure that the "Parcelable" implementation is saving and restoring
        // both fields properly. Detailed testing of JSON is done elsewhere.
        final JSONObject j1 = ts1.toJSON(110_000L, 1_000_000);
        final JSONObject j2 = ts2.toJSON(210_000L, 2_000_000);

        final TimerState ts1J = TimerState.fromJSON(j1, 10_000L, 1_010_000L);
        final TimerState ts2J = TimerState.fromJSON(j2, 20_000L, 2_010_000L);

        assertEquals(ts1, ts1J);
        assertEquals(ts2, ts2J);

        // A "Solve" instance should also be restored properly. One was not set
        // above, as the elapsed time, penalties, etc. of a "TimerState" are
        // taken from its "Solve" when it is available. If it were set before
        // all of the above assertions, it would not be possible to detect if
        // the "Parcelable" implementation was restoring the TimerState's own
        // fields properly, as they would be masked by the "Solve". Delegating
        // to the solve instance is tested elsewhere, so here just test that if
        // a solve is added, that it is still there (and equal) after a
        // round-trip through a "Parcel". Testing just "ts1" is enough. Use
        // different values from those set on the "TimerState", as that will
        // help to spot cross-contamination issues.
        final Solve s1 = new Solve(
            100L, 999L, PuzzleType.TYPE_333, "Normal", 123L,
            "U1 L1", Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.DNF),
            "Hello, world!", false);

        ts1.setSolve(s1);

        final Parcel p1a = Parcel.obtain();

        ts1.writeToParcel(p1a, 0);
        p1a.setDataPosition(0);

        final TimerState ts1Pa = TimerState.CREATOR.createFromParcel(p1a);

        p1a.recycle();

        assertNotNull(ts1Pa.getSolve());
        assertEquals(s1, ts1Pa.getSolve());
        assertEquals(ts1, ts1Pa);

        // "CREATOR.newArray()" is not used, but just check it for completeness.
        assertEquals(42, TimerState.CREATOR.newArray(42).length);
    }
}
