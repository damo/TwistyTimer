package com.aricneto.twistytimer.stats;

import com.aricneto.twistytimer.stats.AverageCalculator.AverageOfN;

import org.junit.Test;

import static com.aricneto.twistytimer.stats.Statistics.TIME_DNF;
import static com.aricneto.twistytimer.stats.Statistics.TIME_UNKNOWN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests the {@link AverageCalculator} class. Averages are tests for 1, 3 and
 * 5 solves. This covers the trivial case (1), the case where a simple
 * arithmetic mean is used (3) and the case where a truncated arithmetic mean
 * is used (5). Values above 5 should be no different. Each average is tested
 * for both DNF handling modes (automatic disqualification or not).
 *
 * @author damo
 */
public class AverageCalculatorTestCase {
    @Test
    public void testCreateOne() throws Exception {
        final AverageCalculator ac = new AverageCalculator(1, false);

        assertEquals(1, ac.getN());
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(TIME_UNKNOWN, ac.getCurrentAverage());
        assertEquals(TIME_UNKNOWN, ac.getBestAverage());

        assertEquals(TIME_UNKNOWN, ac.getBestTime());
        assertEquals(TIME_UNKNOWN, ac.getWorstTime());
        assertEquals(TIME_UNKNOWN, ac.getTotalTime());
        assertEquals(TIME_UNKNOWN, ac.getMeanTime());
    }

    @Test
    public void testCreateThree() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, false);

        assertEquals(3, ac.getN());
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(TIME_UNKNOWN, ac.getCurrentAverage());
        assertEquals(TIME_UNKNOWN, ac.getBestAverage());

        assertEquals(TIME_UNKNOWN, ac.getBestTime());
        assertEquals(TIME_UNKNOWN, ac.getWorstTime());
        assertEquals(TIME_UNKNOWN, ac.getTotalTime());
        assertEquals(TIME_UNKNOWN, ac.getMeanTime());
    }

    @Test
    public void testCreateFive() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, false);

        assertEquals(5, ac.getN());
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(TIME_UNKNOWN, ac.getCurrentAverage());
        assertEquals(TIME_UNKNOWN, ac.getBestAverage());

        assertEquals(TIME_UNKNOWN, ac.getBestTime());
        assertEquals(TIME_UNKNOWN, ac.getWorstTime());
        assertEquals(TIME_UNKNOWN, ac.getTotalTime());
        assertEquals(TIME_UNKNOWN, ac.getMeanTime());
    }

    @Test
    public void testCreateFailure() throws Exception {
        try {
            new AverageCalculator(0, false);
            fail("Expected an exception when 'n' is zero.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }

        try {
            new AverageCalculator(-1, false);
            fail("Expected an exception when 'n' is negative.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }
    }

    @Test
    public void testAddTime() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, true);

        // Initial state is already checked in other test methods.
        // Just test that the counters, sums, best, worst, etc. are updated.
        ac.addTime(TIME_DNF);
        assertEquals(1, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());
        assertEquals(TIME_UNKNOWN, ac.getBestTime());
        assertEquals(TIME_UNKNOWN, ac.getWorstTime());
        assertEquals(TIME_UNKNOWN, ac.getTotalTime());
        assertEquals(TIME_UNKNOWN, ac.getMeanTime());

        ac.addTime(500);
        assertEquals(2, ac.getNumSolves());
        assertEquals(500, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(500, ac.getTotalTime());
        assertEquals(500, ac.getMeanTime());

        ac.addTime(300);
        assertEquals(3, ac.getNumSolves());
        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(TIME_DNF);
        assertEquals(4, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());
        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());
    }

    /**
     * Tests that times are rounded per WCA regulations as they are added,
     * that these rounded times are used to calculate the averages, and that
     * the averages are also rounded before they are returned.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testRounding() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, true);

        // Times less than 10 minutes are truncated to a multiple of 10 ms.
        ac.addTimes(1_239, 2_343, 4_547); // Added as: 1,230; 2,340; 4,540.

        assertEquals(3, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());
        assertEquals(1_230, ac.getBestTime());
        assertEquals(4_540, ac.getWorstTime());
        // Original values sum to 8,129, but they should be truncated *before*
        // being summed.
        assertEquals(8_110, ac.getTotalTime());
        // Mean of original values if 8,129 / 3 = 2,709.666. Means are rounded
        // to nearest multiple of 10 ms, which would be 2,710. However, that
        // would be *wrong*, as the mean should be calculated from the sum of
        // the *truncated* result times, so it is 8,110 / 3 = 2,703.333, which
        // is rounded to 2,700.
        assertEquals(2_700, ac.getMeanTime());
        assertEquals(2_700, ac.getCurrentAverage());
        assertEquals(2_700, ac.getBestAverage());
        assertEquals(2_700, ac.getAverageOfN().getAverage());

        // Now try that with larger values around 10 minutes. Those under 10
        // minutes still truncate to a multiple of 10 ms, but those over 10
        // minutes *round* to a multiple of 1,000 ms.
        final long tenMin = 600_000L;

        ac.reset();
        // Added as: 598,760; 597,650; 604,000.
        ac.addTimes(tenMin - 1_239, tenMin - 2_343, tenMin + 3_557);
fail("Write me!");
        assertEquals(3, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());
        assertEquals(597_650, ac.getBestTime());
        assertEquals(604_000, ac.getWorstTime());
        // Original values sum to 1,799,975, which would truncate to 1,799,970,
        // but they should be truncated/rounded *before* being summed.
        // Therefore, the sum is 1,800,410, which is rounded down to 1,800,000
        assertEquals(1_800_000110, ac.getTotalTime());
        // Mean of original values if 8,139 / 3 = 2,713. Means are rounded to
        // nearest multiple of 10 ms, which would be 2,710. However, that would
        // be *wrong*, as the mean should be calculated from the sum of the
        // *truncated* result times, so it is 8,110 / 3 = 2,703.333, which is
        // rounded to 2,700.
        assertEquals(2_700, ac.getMeanTime());
        assertEquals(2_700, ac.getCurrentAverage());
        assertEquals(2_700, ac.getBestAverage());
        assertEquals(2_700, ac.getAverageOfN().getAverage());

    }

    @Test
    public void testAddTimes() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, true);

        // Initial state is already checked in other test methods.
        // Just test that the counters, sums, best, worst, etc. are updated.
        ac.addTimes((long[]) null);
        assertEquals(
            "Null array of times should be ignored.", 0, ac.getNumSolves());

        ac.addTimes();
        assertEquals(
            "Empty array of times should be ignored.", 0, ac.getNumSolves());

        ac.addTimes(TIME_DNF, 500, 300, TIME_DNF);
        assertEquals(4, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());
        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());
    }

    @Test
    public void testAddTimeFailure() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, true);

        // Initial state is already checked in other test methods.
        try {
            ac.addTime(0);
            fail("Expected an exception when added time is zero.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }

        try {
            ac.addTime(-1);
            fail("Expected an exception when added time is negative.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }

        try {
            ac.addTime(TIME_UNKNOWN);
            fail("Expected an exception when added time is UNKNOWN.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }
    }

    /**
     * Tests the trivial edge case where "n" is one. DNFs cause disqualification
     * of the average, but that should make no difference when "n" is one.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfOneDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(1, true);

        // Initial state is already checked in other test methods.
        ac.addTime(500);

        assertEquals(1, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage());
        assertEquals(500, ac.getBestAverage());

        assertEquals(500, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(500, ac.getTotalTime());
        assertEquals(500, ac.getMeanTime());

        ac.addTime(300);

        assertEquals(2, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(TIME_DNF);
        assertEquals(3, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(TIME_DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(1000);
        assertEquals(4, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(1000, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(1000, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(600, ac.getMeanTime());
    }

    /**
     * Tests the trivial edge case where "n" is one. DNFs do not cause
     * disqualification of the average, but that should make no difference
     * when "n" is one.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfOneAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(1, false);

        // Initial state is already checked in other test methods.
        ac.addTime(500);

        assertEquals(1, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage());
        assertEquals(500, ac.getBestAverage());

        assertEquals(500, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(500, ac.getTotalTime());
        assertEquals(500, ac.getMeanTime());

        ac.addTime(300);

        assertEquals(2, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(TIME_DNF);
        assertEquals(3, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(TIME_DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(1000);
        assertEquals(4, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(1000, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(1000, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(600, ac.getMeanTime());
    }

    /**
     * Tests the calculation of the average of three. For an average of three,
     * a truncated mean should not be calculated. Any DNF should cause
     * disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfThreeDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, true);

        // IMPORTANT: Rounding is to the nearest 1/100th of a second (10 ms)
        // for averages less than ten minutes in duration. Averages are also
        // calculated after rounding the solve times.

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150);

        assertEquals(3, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(900, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());

        ac.addTimes(TIME_DNF, 800);

        assertEquals(5, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(TIME_DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1700, ac.getTotalTime());
        assertEquals(430, ac.getMeanTime()); // 1700 / 4 non-DNF solves.

        ac.addTime(100);

        assertEquals(6, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(TIME_DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(360, ac.getMeanTime()); // 1800 / 5 non-DNF solves.

        // Third non-DNF time in a row should push change the current average
        // to a non-DNF average.
        ac.addTime(900);

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(600, ac.getCurrentAverage()); // Last 3: 800, 100, 900.
        assertEquals(300, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.

        ac.addTime(TIME_DNF);

        assertEquals(8, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(TIME_DNF, ac.getCurrentAverage()); // Last 3: 100, 900, DNF
        assertEquals(300, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.

        // Set a new record for the average time.
        ac.addTimes(90, 210, 300);

        assertEquals(11, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(200, ac.getCurrentAverage());
        assertEquals(200, ac.getBestAverage());

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3300, ac.getTotalTime());

        // 3300 / 9 non-DNF solves. 366.6666... is truncated:
        assertEquals(370, ac.getMeanTime());
    }

    /**
     * Tests the calculation of the average of three. For an average of three,
     * a truncated mean should not be calculated. DNFs will not cause automatic
     * disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfThreeAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, false);

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150);

        assertEquals(3, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(900, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());

        ac.addTimes(TIME_DNF, 800);

        assertEquals(5, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        // Last 3: 150, DNF, 800. Ignore DNF:
        assertEquals(480, ac.getCurrentAverage());
        // Earlier sequence 250, 150, DNF. Ignore DNF:
        assertEquals(200, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1700, ac.getTotalTime());
        assertEquals(430, ac.getMeanTime()); // 1700 / 4 non-DNF solves.

        ac.addTimes(100);

        assertEquals(6, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        // Last 3: DNF, 800, 100. Ignore DNF.
        assertEquals(450, ac.getCurrentAverage());
        assertEquals(200, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(360, ac.getMeanTime()); // 1800 / 5 non-DNF solves.

        // Third non-DNF time in a row.
        ac.addTimes(900);

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(600, ac.getCurrentAverage()); // Last 3: 800, 100, 900.
        assertEquals(200, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.

        ac.addTime(TIME_DNF);

        assertEquals(8, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage()); // Last 3: 100, 900, DNF.
        assertEquals(200, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.

        // Set a new record for the average time.
        ac.addTimes(90, 210, 300);

        assertEquals(11, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(200, ac.getCurrentAverage());
        // Average of DNF, 90, 201. Ignore DNF.
        assertEquals(150, ac.getBestAverage());

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3300, ac.getTotalTime());
        // 3300 / 9 non-DNF solves. 366.6666... is truncated:
        assertEquals(370, ac.getMeanTime());

        // All but one time is a DNF. Average is just that one non-DNF time.
        ac.addTimes(100, TIME_DNF, TIME_DNF);

        assertEquals(14, ac.getNumSolves());
        assertEquals(4, ac.getNumDNFSolves());

        // Average of 100, DNF, DNF. Ignore DNFs:
        assertEquals(100, ac.getCurrentAverage());
        assertEquals(100, ac.getBestAverage()); // As above.

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3400, ac.getTotalTime());
        assertEquals(340, ac.getMeanTime()); // 3400 / 10 non-DNF solves.

        // All times are DNF. Average must be a DNF.
        ac.addTime(TIME_DNF);

        assertEquals(15, ac.getNumSolves());
        assertEquals(5, ac.getNumDNFSolves());

        // Average of DNF, DNF, DNF. Ignore DNFs:
        assertEquals(TIME_DNF, ac.getCurrentAverage());
        assertEquals(100, ac.getBestAverage()); // Unchanged.

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3400, ac.getTotalTime());
        assertEquals(340, ac.getMeanTime()); // 3400 / 10 non-DNF solves.
    }

    /**
     * Tests the calculation of the average of five. For an average of five,
     * a truncated mean should be calculated. Any DNF should cause
     * disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfFiveDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, true);

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150, 400, 200);

        assertEquals(5, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        // (250+400+200) / 3. Exclude 150, 500.
        assertEquals(280, ac.getCurrentAverage());
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(1500, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());

        // One DNF should be tolerated and treated as the worst time when
        // calculating the average. (It is not the worst time reported, though,
        // as that is always a non-DNF time.)
        ac.addTimes(TIME_DNF, 800); // Current: 150, 400, 200, DNF, 800

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        // (400+200+800) / 3. Exclude 150, DNF:
        assertEquals(470, ac.getCurrentAverage());
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2300, ac.getTotalTime());
        assertEquals(380, ac.getMeanTime()); // 2300 / 6 non-DNF solves.

        ac.addTimes(300); // Current: 400, 200, DNF, 800, 300

        assertEquals(8, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        // (400+800+300) / 3. Exclude 200, DNF:
        assertEquals(500, ac.getCurrentAverage());
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(370, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Second DNF in "current" 5 times. Result should be disqualified.
        ac.addTime(TIME_DNF); // Current: 200, DNF, 800, 300, DNF

        assertEquals(9, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(TIME_DNF, ac.getCurrentAverage()); // More than one DNF.
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(370, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Test the "reset()" method, too.
        ac.reset();

        assertEquals(5, ac.getN()); // Should not be changed by a reset.
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(TIME_UNKNOWN, ac.getCurrentAverage());
        assertEquals(TIME_UNKNOWN, ac.getBestAverage());

        assertEquals(TIME_UNKNOWN, ac.getBestTime());
        assertEquals(TIME_UNKNOWN, ac.getWorstTime());
        assertEquals(TIME_UNKNOWN, ac.getTotalTime());
        assertEquals(TIME_UNKNOWN, ac.getMeanTime());
    }

    /**
     * Tests the calculation of the average of five. For an average of five,
     * a truncated mean should be calculated. DNFs will not cause automatic
     * disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfFiveAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, false);

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150, 400, 200);

        assertEquals(5, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        // (250+400+200) / 3. Exclude 150, 500:
        assertEquals(280, ac.getCurrentAverage());
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(1500, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());

        // One DNF should be tolerated and treated as the worst time when
        // calculating the average. (It is not the worst time reported, though,
        // as that is always a non-DNF time.)
        ac.addTimes(TIME_DNF, 800); // Current: 150, 400, 200, DNF, 800

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        // (400+200+800) / 3. Exclude 150, DNF:
        assertEquals(470, ac.getCurrentAverage());
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2300, ac.getTotalTime());
        assertEquals(380, ac.getMeanTime()); // 2300 / 6 non-DNF solves.

        ac.addTimes(300); // Current: 400, 200, DNF, 800, 300

        assertEquals(8, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        // (400+800+300) / 3. Exclude 200, DNF:
        assertEquals(500, ac.getCurrentAverage());
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(370, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Second DNF in "current" 5 times. Result should still be tolerated.
        // One DNF is treated as the worst time and all other DNFs are ignored.
        ac.addTime(TIME_DNF); // Current: 200, DNF, 800, 300, DNF

        assertEquals(9, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        // (800+300) / 2. Exclude 200, DNF. Ignore DNFs:
        assertEquals(550, ac.getCurrentAverage());
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(370, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Third DNF in "current" 5 times. Result should still be tolerated.
        // One DNF is treated as the worst time and all other DNFs are ignored.
        ac.addTime(TIME_DNF); // Current: DNF, 800, 300, DNF, DNF

        assertEquals(10, ac.getNumSolves());
        assertEquals(3, ac.getNumDNFSolves());

        assertEquals(800, ac.getCurrentAverage()); // (800) / 1. Exclude 300, DNF. Ignore DNFs.
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(370, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Fourth DNF in "current" 5 times. Result should still be tolerated.
        // One DNF is treated as the worst time and all other DNFs are ignored.
        // As only one non-DNF time remains, it cannot be excluded as the best
        // time (the worst being a DNF).
        ac.addTimes(TIME_DNF, TIME_DNF); // Current: 300, DNF, DNF, DNF, DNF

        assertEquals(12, ac.getNumSolves());
        assertEquals(5, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage()); // 300 is only non-DNF.
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(370, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Fifth DNF in "current" 5 times. Average must be a DNF.
        ac.addTime(TIME_DNF); // Current: DNF, DNF, DNF, DNF, DNF

        assertEquals(13, ac.getNumSolves());
        assertEquals(6, ac.getNumDNFSolves());

        assertEquals(TIME_DNF, ac.getCurrentAverage()); // 300 is only non-DNF.
        assertEquals(280, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(370, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // New non-DNF time, and it is a record time, too.
        ac.addTimes(100); // Current: DNF, DNF, DNF, DNF, 100

        assertEquals(14, ac.getNumSolves());
        assertEquals(6, ac.getNumDNFSolves());

        assertEquals(100, ac.getCurrentAverage()); // 100 is only non-DNF.
        assertEquals(100, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(340, ac.getMeanTime()); // 2700 / 8 non-DNF solves.
    }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in
     * the correct order and identifies the best and worst times correctly.
     * This test covers the case where best and worst times should not be
     * identified because the value of "N" is low. DNFs do not disqualify the
     * average unless all times are DNFs. The implementation is known to rely
     * on its parent class for much of the details, and those are tested in
     * other methods, so these tests are not comprehensive.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForThreeAllowDNFs() throws Exception {
        // NOTE: "N == 3", so there is no elimination of best or worst times
        // when calculating averages, so the best and worst indices will be -1.
        final AverageCalculator ac = new AverageCalculator(3, false);
        AverageOfN aoN;

        // Providing the times in the correct order (oldest first) is
        // important. The source array that is used to fill the result of
        // "getTimes" is a circular queue, so test the cases where the tail
        // pointer ("AverageCalculator.mNext") is at the start, middle, end
        // and just beyond the end and then ensure that the tricky copy to
        // "AverageOfN.mTimes" is correct. Below, the possible values of
        // "mNext" are 0, 1, 2, 3. It only matters once "N" times have been
        // added. The values of "mNext" are noted in comments to show that
        // all are tested. "mNext" is zero at the very start, but is 3 (just
        // off the end of the array) instead of zero after that, so the zero
        // case is not directly testable.

        // Add less than the minimum required number of times. Average cannot
        // be calculated.
        ac.addTimes(500, 250);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(TIME_UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first three times. Average can now be calculated.
        ac.addTime(150);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 500, 250, 150 }, aoN.getTimes()); // mNext == 3
        assertEquals(300, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // 1 DNF does not disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 250, 150, TIME_DNF }, aoN.getTimes()); // mNext == 1
        assertEquals(200, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // 2 DNFs do not disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 150, TIME_DNF, TIME_DNF }, aoN.getTimes()); // mNext==2
        assertEquals(150, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // 3 DNFs disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 3
        assertEquals(TIME_DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());
   }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in
     * the correct order and identifies the best and worst times correctly.
     * This test covers the case where best and worst times should not be
     * identified because the value of "N" is low. DNFs disqualify the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForThreeDisqualifyDNFs() throws Exception {
        // NOTE: "N == 3", so there is no elimination of best or worst times
        // when calculating averages, so the best and worst indices will be -1.
        final AverageCalculator ac = new AverageCalculator(3, true);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot
        // be calculated.
        ac.addTimes(500, 250);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(TIME_UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first three times. Average can now be calculated.
        ac.addTime(150);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 500, 250, 150 }, aoN.getTimes()); // mNext == 3
        assertEquals(300, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // 1 DNF disqualifies the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 250, 150, TIME_DNF }, aoN.getTimes()); // mNext == 1
        assertEquals(TIME_DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // 2 DNFs disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 150, TIME_DNF, TIME_DNF }, aoN.getTimes()); // mNext==2
        assertEquals(TIME_DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // 3 DNFs disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 3
        assertEquals(TIME_DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // No DNFs and the result is valid again.
        ac.addTimes(100, 200, 600);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 100, 200, 600 }, aoN.getTimes()); // mNext == 3
        assertEquals(300, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());
   }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in
     * the correct order and identifies the best and worst times correctly.
     * This test covers the case where best and worst times must be identified
     * because the value of "N" is high enough to trigger the calculation of a
     * truncated mean. DNFs do not disqualify the average unless all times are
     * DNFs. The "best" time is not eliminated if there is only one non-DNF
     * time present.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForFiveAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, false);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot
        // be calculated.
        ac.addTimes(500, 150, 250, 600);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(TIME_UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first five times. Average can now be calculated.
        ac.addTime(350);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 500, 150, 250, 600, 350 }, aoN.getTimes()); // mNext==5
        // Mean of 500+250+350. 150 and 600 are eliminated.
        assertEquals(370, aoN.getAverage());
        assertEquals(1, aoN.getBestTimeIndex());  // 150
        assertEquals(3, aoN.getWorstTimeIndex()); // 600

        // 1 DNF does not disqualify the result. DNF becomes the "worst" time.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 150, 250, 600, 350, TIME_DNF },
            aoN.getTimes()); // mNext == 1
        // Mean of 250+600+350. 150 and DNF are eliminated.
        assertEquals(400, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // 150
        assertEquals(4, aoN.getWorstTimeIndex()); // DNF

        // 2 DNFs do not disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 250, 600, 350, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 2
        // Mean of 600+350. 250, DNF1 eliminated. DNF2 ignored.
        assertEquals(480, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // 250
        assertEquals(3, aoN.getWorstTimeIndex()); // First DNF

        // 3 DNFs do not disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 600, 350, TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 3
        // 350, DNF1 eliminated. DNF2, DNF3 ignored.
        assertEquals(600, aoN.getAverage());
        assertEquals(1, aoN.getBestTimeIndex());  // 350
        assertEquals(2, aoN.getWorstTimeIndex()); // First DNF

        // 4 DNFs do not disqualify the result, but no best time will be
        // eliminated.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 350, TIME_DNF, TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 4
        assertEquals(350, aoN.getAverage()); // All DNFs are ignored.
        // No elimination of the only non-DNF time:
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(1, aoN.getWorstTimeIndex()); // First DNF

        // 5 DNFs disqualify the result. No eliminations.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { TIME_DNF, TIME_DNF, TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 5
        assertEquals(TIME_DNF, aoN.getAverage()); // Average is disqualified.
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Where all times are the same, the best and worst eliminations must
        // not be the at the same index. Expect the best to identified first
        // and the worst second.
        ac.addTimes(100, 100, 100, 100, 100);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 100, 100, 100, 100, 100 }, aoN.getTimes());
        assertEquals(100, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // First time is "best".
        assertEquals(1, aoN.getWorstTimeIndex()); // Next time is "worst".
   }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in
     * the correct order and identifies the best and worst times correctly.
     * This test covers the case where best and worst times must be identified
     * because the value of "N" is high enough to trigger the calculation of a
     * truncated mean. More than one DNF disqualifies the average. The "best"
     * and worst times are identified in the same manner as when DNFs do not
     * cause disqualifications, even where the average is disqualified.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForFiveDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, true);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot
        // be calculated.
        ac.addTimes(500, 150, 250, 600);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(TIME_UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first five times. Average can now be calculated.
        ac.addTime(350);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 500, 150, 250, 600, 350 }, aoN.getTimes()); // mNext==5
        // Mean of 500+250+350. 150 and 600 are eliminated.
        assertEquals(370, aoN.getAverage());
        assertEquals(1, aoN.getBestTimeIndex());  // 150
        assertEquals(3, aoN.getWorstTimeIndex()); // 600

        // 1 DNF does not disqualify the result. DNF becomes the "worst" time.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 150, 250, 600, 350, TIME_DNF },
            aoN.getTimes()); // mNext == 1
        // Mean of 250+600+350. 150 and DNF are eliminated.
        assertEquals(400, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // 150
        assertEquals(4, aoN.getWorstTimeIndex()); // DNF

        // 2 DNFs disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 250, 600, 350, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 2
        assertEquals(TIME_DNF, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // 250
        assertEquals(3, aoN.getWorstTimeIndex()); // First DNF

        // 3 DNFs disqualify the result.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 600, 350, TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 3
        assertEquals(TIME_DNF, aoN.getAverage());
        assertEquals(1, aoN.getBestTimeIndex());  // 350
        assertEquals(2, aoN.getWorstTimeIndex()); // First DNF

        // 4 DNFs disqualify the result, and no best time will be identified.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 350, TIME_DNF, TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 4
        assertEquals(TIME_DNF, aoN.getAverage());
        // No identification of the only non-DNF time.
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(1, aoN.getWorstTimeIndex()); // First DNF

        // 5 DNFs disqualify the result. No eliminations.
        ac.addTime(TIME_DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { TIME_DNF, TIME_DNF, TIME_DNF, TIME_DNF, TIME_DNF },
            aoN.getTimes()); // mNext == 5
        assertEquals(TIME_DNF, aoN.getAverage()); // Average is disqualified.
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Where all times are the same, the best and worst eliminations must
        // not be the at the same index. Expect the best to identified first
        // and the worst second.
        ac.addTimes(100, 100, 100, 100, 100);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(
            new long[] { 100, 100, 100, 100, 100 }, aoN.getTimes());
        assertEquals(100, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // First time is "best".
        assertEquals(1, aoN.getWorstTimeIndex()); // Next time is "worst".
   }
}
