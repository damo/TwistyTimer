package com.aricneto.twistytimer.timer;

import org.junit.Test;

import static com.aricneto.twistytimer.timer.TimerView.*;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the static time formatting routines used for efficient
 * display of time values by the {@link TimerView} while the timer is running.
 *
 * @author damo
 */
public class TimerViewTestCase {
    @Test
    public void testAppendDigits1() throws Exception {
        checkAppendDigits1("0",   0);
        checkAppendDigits1("0",  10);
        checkAppendDigits1("0", 990);

        checkAppendDigits1("1",   1);
        checkAppendDigits1("1",  21);
        checkAppendDigits1("1", 841);

        checkAppendDigits1("5",   5);
        checkAppendDigits1("5",  85);
        checkAppendDigits1("5", 765);

        checkAppendDigits1("9",   9);
        checkAppendDigits1("9",  59);
        checkAppendDigits1("9", 109);

        // Integer.MAX_VALUE == 2147483647
        checkAppendDigits1("7", Integer.MAX_VALUE);
    }

    @Test
    public void testAppendDigits2() throws Exception {
        checkAppendDigits2("00",   0);
        checkAppendDigits2("00", 100);
        checkAppendDigits2("00", 900);

        checkAppendDigits2("01",   1);
        checkAppendDigits2("21",  21);
        checkAppendDigits2("41", 841);

        checkAppendDigits2("05",   5);
        checkAppendDigits2("85",  85);
        checkAppendDigits2("65", 765);

        checkAppendDigits2("09",   9);
        checkAppendDigits2("59",  59);
        checkAppendDigits2("09", 109);

        // Integer.MAX_VALUE == 2147483647
        checkAppendDigits2("47", Integer.MAX_VALUE);
    }

    @Test
    public void testAppendDigitsN() throws Exception {
        // Check one value of each possible magnitude, right on the edge cases.
        checkAppendDigitsN("0", 0);
        checkAppendDigitsN("1", 1);

        checkAppendDigitsN( "9",  9);
        checkAppendDigitsN("10", 10);
        checkAppendDigitsN("11", 11);

        checkAppendDigitsN( "99",  99);
        checkAppendDigitsN("100", 100);
        checkAppendDigitsN("101", 101);

        checkAppendDigitsN( "999",   999);
        checkAppendDigitsN("1000", 1_000);
        checkAppendDigitsN("1001", 1_001);

        checkAppendDigitsN( "9999",  9_999);
        checkAppendDigitsN("10000", 10_000);
        checkAppendDigitsN("10001", 10_001);

        checkAppendDigitsN( "99999",  99_999);
        checkAppendDigitsN("100000", 100_000);
        checkAppendDigitsN("100001", 100_001);

        checkAppendDigitsN( "999999",   999_999);
        checkAppendDigitsN("1000000", 1_000_000);
        checkAppendDigitsN("1000001", 1_000_001);

        checkAppendDigitsN( "9999999",  9_999_999);
        checkAppendDigitsN("10000000", 10_000_000);
        checkAppendDigitsN("10000001", 10_000_001);

        checkAppendDigitsN( "99999999",  99_999_999);
        checkAppendDigitsN("100000000", 100_000_000);
        checkAppendDigitsN("100000001", 100_000_001);

        checkAppendDigitsN( "999999999",   999_999_999);
        checkAppendDigitsN("1000000000", 1_000_000_000);
        checkAppendDigitsN("1000000001", 1_000_000_001);

        checkAppendDigitsN("2147483647", Integer.MAX_VALUE);

        // Test some other assorted values.
        checkAppendDigitsN(  "30",   30);
        checkAppendDigitsN( "100",  100);
        checkAppendDigitsN("9900", 9900);

        checkAppendDigitsN(  "21",   21);
        checkAppendDigitsN( "841",  841);
        checkAppendDigitsN("7771", 7771);

        checkAppendDigitsN(   "5",    5);
        checkAppendDigitsN(  "85",   85);
        checkAppendDigitsN( "765",  765);
        checkAppendDigitsN("4765", 4765);

        checkAppendDigitsN(  "59",   59);
        checkAppendDigitsN( "109",  109);
        checkAppendDigitsN("8109", 8109);
    }

    /**
     * Tests the appending of the solve time. There are some key behaviours
     * to test: the solve time counts up, so the time values should be rounded
     * down (floor function) at the appropriate level of resolution; the
     * resolution changes when the value exceeds 10 minutes; the magnitude of
     * the value determines what format template will be used; and there is a
     * flag that forces high or low resolution times, regardless of the
     * magnitude of the value.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testAppendRunningSolveTime() throws Exception {
        // NOTE ON ROUNDING/TRUNCATION OF TIMES WHILE TIMER IS RUNNING
        // -----------------------------------------------------------
        // Per the WCA Regulations, times under 10 minutes are truncated to the
        // nearest (not greater) 1/100th of a second, while times over 10
        // minutes are rounded to the nearest whole second. HOWEVER, when the
        // timer is running the behaviour is slightly different: all values are
        // truncated (floor function) to their unit of resolution. This ensures
        // that the timer display does not "tick" to the next higher value
        // until that whole interval has elapsed. For example, "10:59" does not
        // change to "11:00" until the full 59th second after the 10th minute
        // has elapsed. Were WCA rounding applied, the timer would tick from
        // "9:59.99" to "10:00" (changing resolution) at the right instant, but
        // would then tick from "10:00" to "10:01" after only half a second
        // (as values over ten minutes would be rounded "half-up" to the nearest
        // whole second). This is not desirable for a running timer. However,
        // if the timer is stopped at, say, "10:00[.75]", it will have been
        // showing "10:00" because it was running, but will now show "10:01"
        // because it is stopped and the WCA rounding has been applied. However,
        // this behaviour is at a higher level than the method under test, so
        // it is not of concern here.

        // If high resolution timing is requested, the formatted time should
        // show 100ths of a second for times under 10 minutes and whole seconds
        // for times of 10 minutes and over.
        checkAppendRunningSolveTime("0.00",  0, true);
        checkAppendRunningSolveTime("0.00",  1, true);
        checkAppendRunningSolveTime("0.00",  9, true);
        checkAppendRunningSolveTime("0.01", 10, true);
        checkAppendRunningSolveTime("0.01", 11, true);
        checkAppendRunningSolveTime("0.01", 19, true);
        checkAppendRunningSolveTime("0.02", 20, true);
        checkAppendRunningSolveTime("0.02", 21, true);

        checkAppendRunningSolveTime("0.09",  99, true);
        checkAppendRunningSolveTime("0.10", 100, true);
        checkAppendRunningSolveTime("0.10", 101, true);
        checkAppendRunningSolveTime("0.10", 109, true);
        checkAppendRunningSolveTime("0.11", 110, true);

        checkAppendRunningSolveTime("0.99",   999, true);
        checkAppendRunningSolveTime("1.00", 1_000, true);
        checkAppendRunningSolveTime("1.00", 1_001, true);
        checkAppendRunningSolveTime("1.00", 1_009, true);
        checkAppendRunningSolveTime("1.01", 1_010, true);

        checkAppendRunningSolveTime("1.99", 1_999, true);
        checkAppendRunningSolveTime("2.00", 2_000, true);
        checkAppendRunningSolveTime("2.00", 2_001, true);
        checkAppendRunningSolveTime("2.00", 2_009, true);
        checkAppendRunningSolveTime("2.01", 2_010, true);

        checkAppendRunningSolveTime( "9.99",  9_999, true);
        checkAppendRunningSolveTime("10.00", 10_000, true);
        checkAppendRunningSolveTime("10.00", 10_001, true);
        checkAppendRunningSolveTime("10.00", 10_009, true);
        checkAppendRunningSolveTime("10.01", 10_010, true);

        checkAppendRunningSolveTime(  "59.99", m_s_S(0, 59, 999), true);
        checkAppendRunningSolveTime("1:00.00", m_s_S(1,  0,   0), true);
        checkAppendRunningSolveTime("1:00.00", m_s_S(1,  0,   1), true);
        checkAppendRunningSolveTime("1:00.00", m_s_S(1,  0,   9), true);
        checkAppendRunningSolveTime("1:00.01", m_s_S(1,  0,  10), true);

        checkAppendRunningSolveTime("1:00.99", m_s_S(1,  0, 999), true);
        checkAppendRunningSolveTime("1:01.00", m_s_S(1,  1,   0), true);
        checkAppendRunningSolveTime("1:01.00", m_s_S(1,  1,   1), true);
        checkAppendRunningSolveTime("1:01.00", m_s_S(1,  1,   9), true);
        checkAppendRunningSolveTime("1:01.01", m_s_S(1,  1,  10), true);

        checkAppendRunningSolveTime( "9:59.99", m_s_S( 9, 59, 999), true);
        checkAppendRunningSolveTime("10:00",    m_s_S(10,  0,   0), true);
        checkAppendRunningSolveTime("10:00",    m_s_S(10,  0, 999), true);
        checkAppendRunningSolveTime("10:01",    m_s_S(10,  1,   0), true);
        checkAppendRunningSolveTime("10:01",    m_s_S(10,  1, 999), true);
        checkAppendRunningSolveTime("10:02",    m_s_S(10,  2,   0), true);

        checkAppendRunningSolveTime(  "59:59",   m_s_S(   59, 59, 999), true);
        checkAppendRunningSolveTime("1:00:00", h_m_s_S(1,  0,  0,   0), true);
        checkAppendRunningSolveTime("1:00:00", h_m_s_S(1,  0,  0, 999), true);
        checkAppendRunningSolveTime("1:00:01", h_m_s_S(1,  0,  1, 999), true);
        checkAppendRunningSolveTime("1:59:59", h_m_s_S(1, 59, 59, 999), true);
        checkAppendRunningSolveTime("2:00:00", h_m_s_S(2,  0,  0,   0), true);

        checkAppendRunningSolveTime( "9:59:59", h_m_s_S( 9, 59, 59, 999), true);
        checkAppendRunningSolveTime("10:00:00", h_m_s_S(10,  0,  0,   0), true);
        checkAppendRunningSolveTime("99:00:00", h_m_s_S(99,  0,  0,   0), true);

        checkAppendRunningSolveTime(
             "999:00:00", h_m_s_S(  999,  0,  0,   0), true);
        checkAppendRunningSolveTime(
            "9999:00:00", h_m_s_S(9_999,  0,  0,   0), true);

        // If high resolution timing is *not* requested, the formatted time
        // should show only whole seconds for all times. These values always
        // show the minutes field, even if zero.
        checkAppendRunningSolveTime("0:00",    0, false);
        checkAppendRunningSolveTime("0:00",   99, false);
        checkAppendRunningSolveTime("0:00",  999, false);
        checkAppendRunningSolveTime("0:01", 1_000, false);
        checkAppendRunningSolveTime("0:01", 1_999, false);
        checkAppendRunningSolveTime("0:02", 2_000, false);

        checkAppendRunningSolveTime("0:09",  9_999, false);
        checkAppendRunningSolveTime("0:10", 10_000, false);
        checkAppendRunningSolveTime("0:10", 10_001, false);
        checkAppendRunningSolveTime("0:10", 10_999, false);
        checkAppendRunningSolveTime("0:11", 11_000, false);

        checkAppendRunningSolveTime("0:59", m_s_S(0, 59, 999), false);
        checkAppendRunningSolveTime("1:00", m_s_S(1,  0,   0), false);
        checkAppendRunningSolveTime("1:00", m_s_S(1,  0, 999), false);
        checkAppendRunningSolveTime("1:01", m_s_S(1,  1,   0), false);
        checkAppendRunningSolveTime("1:01", m_s_S(1,  1,   1), false);
        checkAppendRunningSolveTime("1:01", m_s_S(1,  1, 999), false);
        checkAppendRunningSolveTime("1:02", m_s_S(1,  2,   0), false);

        checkAppendRunningSolveTime( "9:59", m_s_S( 9, 59, 999), false);
        checkAppendRunningSolveTime("10:00", m_s_S(10,  0,   0), false);
        checkAppendRunningSolveTime("10:00", m_s_S(10,  0, 999), false);
        checkAppendRunningSolveTime("10:01", m_s_S(10,  1,   0), false);
        checkAppendRunningSolveTime("10:01", m_s_S(10,  1, 999), false);
        checkAppendRunningSolveTime("10:02", m_s_S(10,  2,   0), false);

        checkAppendRunningSolveTime(  "59:59",   m_s_S(   59, 59, 999), false);
        checkAppendRunningSolveTime("1:00:00", h_m_s_S(1,  0,  0,   0), false);
        checkAppendRunningSolveTime("1:00:00", h_m_s_S(1,  0,  0, 999), false);
        checkAppendRunningSolveTime("1:00:01", h_m_s_S(1,  0,  1, 999), false);
        checkAppendRunningSolveTime("1:59:59", h_m_s_S(1, 59, 59, 999), false);
        checkAppendRunningSolveTime("2:00:00", h_m_s_S(2,  0,  0,   0), false);

        checkAppendRunningSolveTime(
             "9:59:59", h_m_s_S( 9, 59, 59, 999), false);
        checkAppendRunningSolveTime(
            "10:00:00", h_m_s_S(10,  0,  0,   0), false);
        checkAppendRunningSolveTime(
            "99:00:00", h_m_s_S(99,  0,  0,   0), false);

        checkAppendRunningSolveTime(
             "999:00:00", h_m_s_S(  999,  0,  0,   0), false);
        checkAppendRunningSolveTime(
            "9999:00:00", h_m_s_S(9_999,  0,  0,   0), false);

    }

    /**
     * Tests the appending of the inspection time. There are some key behaviours
     * to test: the inspection time counts down, so the time values should be
     * rounded up (ceiling function) at the appropriate level of resolution; the
     * inspection time can be negative during the overrun period and the time
     * is formatted with a different resolution during that period.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testAppendRunningInspectionTime() throws Exception {
        // Should show a whole number of seconds, given a value in milliseconds.
        checkAppendRunningInspectionTime(   "0",         0);
        checkAppendRunningInspectionTime(  "30",    30_000);
        checkAppendRunningInspectionTime( "999",   999_000);
        checkAppendRunningInspectionTime("1000", 1_000_000);

        // Rounding should use a ceiling function. The time should not be
        // decremented until a full one-second interval has elapsed.
        checkAppendRunningInspectionTime("1",     1);
        checkAppendRunningInspectionTime("1",   999);
        checkAppendRunningInspectionTime("1", 1_000);

        checkAppendRunningInspectionTime("2", 1_001);
        checkAppendRunningInspectionTime("2", 1_999);
        checkAppendRunningInspectionTime("2", 2_000);

        checkAppendRunningInspectionTime("3", 2_001);

        // Negative values have special handling. Once the value turns negative,
        // the resolution changes to 10ths of a second. The formatted result
        // should be clipped to the range "+2.0" to "+0.0". Rounding should work
        // as above: a ceiling function, but with a resolution of 1/10 seconds.
        // The two-second range is defined by "INSPECTION_OVERRUN_DURATION", so
        // check that first in case it ever changes and makes failures in these
        // tests hard to diagnose.
        assertEquals(2_000L, TimerState.INSPECTION_OVERRUN_DURATION);

        checkAppendRunningInspectionTime("+2.0",   -1);
        checkAppendRunningInspectionTime("+2.0",  -99);
        checkAppendRunningInspectionTime("+1.9", -100);
        checkAppendRunningInspectionTime("+1.9", -101);
        checkAppendRunningInspectionTime("+1.9", -199);
        checkAppendRunningInspectionTime("+1.8", -200);
        checkAppendRunningInspectionTime("+1.8", -201);

        checkAppendRunningInspectionTime("+1.1",   -901);
        checkAppendRunningInspectionTime("+1.1",   -999);
        checkAppendRunningInspectionTime("+1.0", -1_000);
        checkAppendRunningInspectionTime("+1.0", -1_001);
        checkAppendRunningInspectionTime("+1.0", -1_099);
        checkAppendRunningInspectionTime("+0.9", -1_100);
        checkAppendRunningInspectionTime("+0.9", -1_101);

        checkAppendRunningInspectionTime("+0.2", -1_899);
        checkAppendRunningInspectionTime("+0.1", -1_900);
        checkAppendRunningInspectionTime("+0.1", -1_910);
        checkAppendRunningInspectionTime("+0.1", -1_999);

        // "+0.0" is first shown at the exact instant that the time runs out.
        checkAppendRunningInspectionTime("+0.0", -2_000);

        // Now drop below the minimum expected value and check "+0.0" holds.
        checkAppendRunningInspectionTime("+0.0",  -2_001);
        checkAppendRunningInspectionTime("+0.0",  -2_099);
        checkAppendRunningInspectionTime("+0.0",  -2_100);
        checkAppendRunningInspectionTime("+0.0",  -2_101);
        checkAppendRunningInspectionTime("+0.0",  -3_101);
        checkAppendRunningInspectionTime("+0.0", -10_101);
        checkAppendRunningInspectionTime("+0.0", -99_999);
    }

    /**
     * Checks that the result of appending one digit with {@code appendDigits1}
     * is as expected.
     *
     * @param expected The expected content to be appended.
     * @param value    The value from which the digit is derived.
     */
    private static void checkAppendDigits1(String expected, int value) {
        assertEquals(
            expected, appendDigits1(new StringBuilder(), value).toString());
    }

    /**
     * Checks that the result of appending two digits with {@code appendDigits2}
     * is as expected.
     *
     * @param expected The expected content to be appended.
     * @param value    The value from which the digits are derived.
     */
    private static void checkAppendDigits2(String expected, int value) {
        assertEquals(
            expected, appendDigits2(new StringBuilder(), value).toString());
    }

    /**
     * Checks that the result of appending one or more digits with
     * {@code appendDigitsN} is as expected.
     *
     * @param expected The expected content to be appended.
     * @param value    The value from which the digits are derived.
     */
    private static void checkAppendDigitsN(String expected, int value) {
        assertEquals(
            expected, appendDigitsN(new StringBuilder(), value).toString());
    }

    /**
     * Checks that the result of appending the elapsed solve time is as
     * expected.
     *
     * @param expected
     *     The expected content to be appended.
     * @param time
     *     The time value to be formatted and appended (in milliseconds).
     * @param showHiRes
     *     {@code true} if high resolution times (to 100ths of a second) should
     *     be formatted if the time is under ten minutes); or {@code false} if
     *     the time should be rounded/truncated to whole seconds.
     */
    private static void checkAppendRunningSolveTime(
            String expected, long time, boolean showHiRes) {
        assertEquals(expected,
            appendRunningSolveTime(new StringBuilder(), time, showHiRes)
                .toString());
    }

    /**
     * Checks that the result of appending the remaining inspection time is as
     * expected.
     *
     * @param expected
     *     The expected content to be appended.
     * @param time
     *     The time value to be formatted and appended (in milliseconds).
     */
    private static void checkAppendRunningInspectionTime(
            String expected, long time) {
        assertEquals(expected,
            appendRunningInspectionTime(new StringBuilder(), time).toString());
    }

    /**
     * Converts time fields into the number of milliseconds represented by those
     * fields.
     *
     * @param minutes      The number of minutes.
     * @param seconds      The number of seconds.
     * @param milliseconds The number of milliseconds.
     */
    private static long m_s_S(int minutes, int seconds, int milliseconds) {
        return minutes * 60_000L + seconds * 1_000L + milliseconds;
    }

    /**
     * Converts time fields into the number of milliseconds represented by those
     * fields.
     *
     * @param hours        The number of hours.
     * @param minutes      The number of minutes.
     * @param seconds      The number of seconds.
     * @param milliseconds The number of milliseconds.
     */
    private static long h_m_s_S(
            int hours, int minutes, int seconds, int milliseconds) {
        return hours * 3_600_000L + minutes * 60_000L
               + seconds * 1_000L + milliseconds;
    }
}
