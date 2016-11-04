package com.aricneto.twistytimer.timer;

import org.junit.Test;

import static com.aricneto.twistytimer.timer.DefaultPuzzleClock.floorDiv;
import static com.aricneto.twistytimer.timer.DefaultPuzzleClock.floorMod;
import static com.aricneto.twistytimer.timer.DefaultPuzzleClock.getDelayToNextTick;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests for some aspects of the {@link DefaultPuzzleClock} implementation.
 *
 * @author damo
 */
public class DefaultPuzzleClockTestCase {
    /**
     * Tests the {@code floorMod} function, ensuring it operates differently from the Java
     * remainder/modulo operator where appropriate.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testFloorMod() throws Exception {
        // When the signs are the same, "floorMod" is the same as "%".
        assertEquals( 3 % 10, floorMod( 3, 10));
        assertEquals(99 % 10, floorMod(99, 10));

        // When the signs are different, "floorMod" is *not* the same as "%".
        assertEquals(7, floorMod(-3, 10));
        assertEquals(-3,  -3 % 10);

        assertEquals( 1, floorMod(-99, 10));
        assertEquals(-9, -99 % 10);

        // NOTE: The behaviour when the divisor is negative is documented as "not defined", so it
        // is not tested.
    }

    /**
     * Tests the {@code floorDiv} function, ensuring it operates differently from the Java integer
     * division operator where appropriate.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testFloorDiv() throws Exception {
        // When the signs are the same, "floorDiv" is the same as "/".
        assertEquals( 3 / 10, floorDiv( 3, 10));
        assertEquals(10 /  3, floorDiv(10,  3));
        assertEquals(99 / 10, floorDiv(99, 10));

        // When the signs are different, "floorDiv" is *not* the same as "/".
        assertNotEquals(-3 / 10, floorDiv(-3, 10));
        assertEquals(-1, floorDiv(-3, 10)); // -3 / 10 = -0.3, so floor is -1, not 0.

        assertNotEquals(10 / -3, floorDiv(10, -3));
        assertEquals(-4, floorDiv(10, -3)); // 10 / -3 = -3.3333, so floor is -4, not -3.
    }

    /**
     * Tests the calculation of the delay until the next periodic tick.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testGetDelayToNextTick() throws Exception {
        final long period = 100;

        // POSITIVE ORIGIN TIME:
        // Assume that the origin time was 1,234 ms and the period is 100 ms. Expect the ticks to
        // occur at 1,234; 1,334; 1,434; 1,534; etc. The delay should be the difference between
        // "now" and the next planned tick time. However, this should still work if the origin
        // time is in the future (i.e., if now < 1,234).
        long originTime = 1_234;
        long phaseOffset = floorMod(originTime, period); // How "tickEvery" calculates it.

        // The "phaseOffset" is the offset from the "phase" if the origin time were zero. It is
        // always positive.
        assertEquals( 34, phaseOffset);

        assertEquals( 34, getDelayToNextTick(    0, period, phaseOffset));
        assertEquals( 33, getDelayToNextTick(    1, period, phaseOffset));
        assertEquals(  1, getDelayToNextTick(   33, period, phaseOffset));
        assertEquals(100, getDelayToNextTick(   34, period, phaseOffset));
        assertEquals( 99, getDelayToNextTick(   35, period, phaseOffset));
        assertEquals( 35, getDelayToNextTick(   99, period, phaseOffset));
        assertEquals( 34, getDelayToNextTick(  100, period, phaseOffset));
        assertEquals( 34, getDelayToNextTick(  200, period, phaseOffset));
        assertEquals( 34, getDelayToNextTick(1_100, period, phaseOffset));
        assertEquals(100, getDelayToNextTick(1_134, period, phaseOffset));
        assertEquals( 60, getDelayToNextTick(1_174, period, phaseOffset));
        assertEquals(  1, getDelayToNextTick(1_233, period, phaseOffset));
        assertEquals(100, getDelayToNextTick(1_234, period, phaseOffset));
        assertEquals( 99, getDelayToNextTick(1_235, period, phaseOffset));
        assertEquals(  1, getDelayToNextTick(9_933, period, phaseOffset));
        assertEquals(100, getDelayToNextTick(9_934, period, phaseOffset));
        assertEquals( 99, getDelayToNextTick(9_935, period, phaseOffset));

        // "Now" at zero then negative. Ticks in phase at: 34, -66, -166, -266, etc.
        assertEquals( 34, getDelayToNextTick(   0, period, phaseOffset));
        assertEquals( 99, getDelayToNextTick( -65, period, phaseOffset)); //  +34 next
        assertEquals(100, getDelayToNextTick( -66, period, phaseOffset)); //  +34 next
        assertEquals(  1, getDelayToNextTick( -67, period, phaseOffset)); //  -66 next
        assertEquals( 34, getDelayToNextTick(-500, period, phaseOffset)); // -466 next

        // NEGATIVE ORIGIN TIME:
        // Assume that the origin time was -166 ms and the period is 100 ms. Expect the ticks to
        // occur every 100 ms at, may -166; -66; 34, 134; 234; etc.
        originTime = -166;
        phaseOffset = floorMod(originTime, period);

        assertEquals( 34, phaseOffset); // Always positive, still.

        assertEquals( 14, getDelayToNextTick( -180, period, phaseOffset));
        assertEquals(100, getDelayToNextTick( -166, period, phaseOffset));
        assertEquals( 34, getDelayToNextTick( -100, period, phaseOffset));
        assertEquals( 34, getDelayToNextTick(    0, period, phaseOffset));
        assertEquals(100, getDelayToNextTick(   34, period, phaseOffset));
    }
}
