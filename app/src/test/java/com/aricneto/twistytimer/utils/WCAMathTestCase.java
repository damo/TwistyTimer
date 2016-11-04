package com.aricneto.twistytimer.utils;

import org.junit.Test;

import static com.aricneto.twistytimer.utils.WCAMath.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link WCAMath}. See the description of that class for details on the WCA Regulations
 * that are being tested here.
 *
 * @author damo
 */
public class WCAMathTestCase {
    /**
     * Tests the rounding of a value to the nearest multiple of another value. Half-up rounding
     * is towards positive infinity for both positive and negative numbers.
     *
     * @throws Exception The the test cannot be executed.
     */
    @Test
    public void testRoundToMultiple() throws Exception {
        // Note odd/even multiples exercise the detection of the mid-point for half-up rounding.

        // Value is zero.
        assertEquals(0, roundToMultiple(0,  1));
        assertEquals(0, roundToMultiple(0,  2));
        assertEquals(0, roundToMultiple(0,  3));
        assertEquals(0, roundToMultiple(0,  4));
        assertEquals(0, roundToMultiple(0,  5));
        assertEquals(0, roundToMultiple(0, 10));

        // Multiple is an even number.
        assertEquals( 0, roundToMultiple( 4, 10));
        assertEquals(10, roundToMultiple( 5, 10));
        assertEquals(10, roundToMultiple( 6, 10));
        assertEquals(10, roundToMultiple( 7, 10));
        assertEquals(10, roundToMultiple( 8, 10));
        assertEquals(10, roundToMultiple( 9, 10));
        assertEquals(10, roundToMultiple(10, 10));
        assertEquals(10, roundToMultiple(11, 10));
        assertEquals(10, roundToMultiple(12, 10));
        assertEquals(10, roundToMultiple(13, 10));
        assertEquals(10, roundToMultiple(14, 10));
        assertEquals(20, roundToMultiple(15, 10));
        assertEquals(20, roundToMultiple(16, 10));

        // Multiple is an odd number.
        assertEquals( 0, roundToMultiple( 1, 5));
        assertEquals( 0, roundToMultiple( 2, 5));
        assertEquals( 5, roundToMultiple( 3, 5));
        assertEquals( 5, roundToMultiple( 4, 5));
        assertEquals( 5, roundToMultiple( 5, 5));
        assertEquals( 5, roundToMultiple( 6, 5));
        assertEquals( 5, roundToMultiple( 7, 5));
        assertEquals(10, roundToMultiple( 8, 5));
        assertEquals(10, roundToMultiple( 9, 5));
        assertEquals(10, roundToMultiple(10, 5));
        assertEquals(10, roundToMultiple(11, 5));
        assertEquals(10, roundToMultiple(12, 5));
        assertEquals(15, roundToMultiple(13, 5));
        assertEquals(15, roundToMultiple(14, 5));
        assertEquals(15, roundToMultiple(15, 5));
        assertEquals(15, roundToMultiple(16, 5));
        assertEquals(15, roundToMultiple(17, 5));
        assertEquals(20, roundToMultiple(18, 5));

        // Value is a negative number and multiple is an even number. Half-up rounding should be
        // towards positive infinity. For example, if the multiple if 4, then 6 rounds half-up to
        // 8, while -6 rounds half-up to -4 (not -8).
        assertEquals(  0, roundToMultiple( -1, 4));
        assertEquals(  0, roundToMultiple( -2, 4)); // Half-*UP* towards +ve infinity!
        assertEquals( -4, roundToMultiple( -3, 4));
        assertEquals( -4, roundToMultiple( -4, 4));
        assertEquals( -4, roundToMultiple( -5, 4));
        assertEquals( -4, roundToMultiple( -6, 4)); // Half-*UP* towards +ve infinity!
        assertEquals( -8, roundToMultiple( -7, 4));
        assertEquals( -8, roundToMultiple( -8, 4));
        assertEquals( -8, roundToMultiple( -9, 4));
        assertEquals( -8, roundToMultiple(-10, 4)); // Half-*UP* towards +ve infinity!
        assertEquals(-12, roundToMultiple(-11, 4));

        // Value is a negative number and multiple is an odd number.
        assertEquals(  0, roundToMultiple( -1, 3));
        assertEquals( -3, roundToMultiple( -2, 3));
        assertEquals( -3, roundToMultiple( -3, 3));
        assertEquals( -3, roundToMultiple( -4, 3));
        assertEquals( -6, roundToMultiple( -5, 3));
        assertEquals( -6, roundToMultiple( -6, 3));
        assertEquals( -6, roundToMultiple( -7, 3));
        assertEquals( -9, roundToMultiple( -8, 3));
        assertEquals( -9, roundToMultiple( -9, 3));
        assertEquals( -9, roundToMultiple(-10, 3));
        assertEquals(-12, roundToMultiple(-11, 3));
    }

    /**
     * Tests the rounding of a value to the nearest multiple of another value when the multiple is
     * an illegal zero value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRoundToMultipleIllegalZeroMultiple() {
        roundToMultiple(10, 0);
    }

    /**
     * Tests the rounding of a value to the nearest multiple of another value when the multiple is
     * an illegal negative value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRoundToMultipleIllegalNegativeMultiple() {
        roundToMultiple(10, -1);
    }

    /**
     * Tests the truncation of a value to the nearest multiple of a second value not greater than
     * the first value.
     *
     * @throws Exception The the test cannot be executed.
     */
    @Test
    public void testFloorToMultiple() throws Exception {
        // Value is zero.
        assertEquals(0, floorToMultiple(0,  1));
        assertEquals(0, floorToMultiple(0,  2));
        assertEquals(0, floorToMultiple(0,  3));
        assertEquals(0, floorToMultiple(0,  4));
        assertEquals(0, floorToMultiple(0,  5));
        assertEquals(0, floorToMultiple(0, 10));

        // Multiple is an even number.
        assertEquals( 0, floorToMultiple( 4, 10));
        assertEquals( 0, floorToMultiple( 5, 10));
        assertEquals( 0, floorToMultiple( 6, 10));
        assertEquals( 0, floorToMultiple( 7, 10));
        assertEquals( 0, floorToMultiple( 8, 10));
        assertEquals( 0, floorToMultiple( 9, 10));
        assertEquals(10, floorToMultiple(10, 10));
        assertEquals(10, floorToMultiple(11, 10));
        assertEquals(10, floorToMultiple(12, 10));
        assertEquals(10, floorToMultiple(13, 10));
        assertEquals(10, floorToMultiple(14, 10));
        assertEquals(10, floorToMultiple(15, 10));
        assertEquals(10, floorToMultiple(16, 10));

        // Multiple is an odd number.
        assertEquals( 0, floorToMultiple( 1, 5));
        assertEquals( 0, floorToMultiple( 2, 5));
        assertEquals( 0, floorToMultiple( 3, 5));
        assertEquals( 0, floorToMultiple( 4, 5));
        assertEquals( 5, floorToMultiple( 5, 5));
        assertEquals( 5, floorToMultiple( 6, 5));
        assertEquals( 5, floorToMultiple( 7, 5));
        assertEquals( 5, floorToMultiple( 8, 5));
        assertEquals( 5, floorToMultiple( 9, 5));
        assertEquals(10, floorToMultiple(10, 5));
        assertEquals(10, floorToMultiple(11, 5));
        assertEquals(10, floorToMultiple(12, 5));
        assertEquals(10, floorToMultiple(13, 5));
        assertEquals(10, floorToMultiple(14, 5));
        assertEquals(15, floorToMultiple(15, 5));
        assertEquals(15, floorToMultiple(16, 5));
        assertEquals(15, floorToMultiple(17, 5));
        assertEquals(15, floorToMultiple(18, 5));

        // Value is a negative number and multiple is an even number.
        assertEquals( -4, floorToMultiple( -1, 4));
        assertEquals( -4, floorToMultiple( -2, 4));
        assertEquals( -4, floorToMultiple( -3, 4));
        assertEquals( -4, floorToMultiple( -4, 4));
        assertEquals( -8, floorToMultiple( -5, 4));
        assertEquals( -8, floorToMultiple( -6, 4));
        assertEquals( -8, floorToMultiple( -7, 4));
        assertEquals( -8, floorToMultiple( -8, 4));
        assertEquals(-12, floorToMultiple( -9, 4));
        assertEquals(-12, floorToMultiple(-10, 4));
        assertEquals(-12, floorToMultiple(-11, 4));

        // Value is a negative number and multiple is an odd number.
        assertEquals( -3, floorToMultiple( -1, 3));
        assertEquals( -3, floorToMultiple( -2, 3));
        assertEquals( -3, floorToMultiple( -3, 3));
        assertEquals( -6, floorToMultiple( -4, 3));
        assertEquals( -6, floorToMultiple( -5, 3));
        assertEquals( -6, floorToMultiple( -6, 3));
        assertEquals( -9, floorToMultiple( -7, 3));
        assertEquals( -9, floorToMultiple( -8, 3));
        assertEquals( -9, floorToMultiple( -9, 3));
        assertEquals(-12, floorToMultiple(-10, 3));
        assertEquals(-12, floorToMultiple(-11, 3));
    }

    /**
     * Tests the truncation of a value to the nearest (not greater) multiple of another value when
     * the multiple is an illegal zero value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFloorToMultipleIllegalZeroMultiple() {
        floorToMultiple(10, 0);
    }

    /**
     * Tests the rounding of a value to the nearest (not greater) multiple of another value when
     * the multiple is an illegal negative value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFloorToMultipleIllegalNegativeMultiple() {
        floorToMultiple(10, -1);
    }

    /**
     * Tests the rounding up of a value to the nearest multiple of a second value not less than
     * the first value.
     *
     * @throws Exception The the test cannot be executed.
     */
    @Test
    public void testCeilToMultiple() throws Exception {
        // Value is zero.
        assertEquals(0, ceilToMultiple(0,  1));
        assertEquals(0, ceilToMultiple(0,  2));
        assertEquals(0, ceilToMultiple(0,  3));
        assertEquals(0, ceilToMultiple(0,  4));
        assertEquals(0, ceilToMultiple(0,  5));
        assertEquals(0, ceilToMultiple(0, 10));

        // Multiple is an even number.
        assertEquals(10, ceilToMultiple( 4, 10));
        assertEquals(10, ceilToMultiple( 5, 10));
        assertEquals(10, ceilToMultiple( 6, 10));
        assertEquals(10, ceilToMultiple( 7, 10));
        assertEquals(10, ceilToMultiple( 8, 10));
        assertEquals(10, ceilToMultiple( 9, 10));
        assertEquals(10, ceilToMultiple(10, 10));
        assertEquals(20, ceilToMultiple(11, 10));
        assertEquals(20, ceilToMultiple(12, 10));
        assertEquals(20, ceilToMultiple(13, 10));
        assertEquals(20, ceilToMultiple(14, 10));
        assertEquals(20, ceilToMultiple(15, 10));
        assertEquals(20, ceilToMultiple(16, 10));

        // Multiple is an odd number.
        assertEquals( 5, ceilToMultiple( 1, 5));
        assertEquals( 5, ceilToMultiple( 2, 5));
        assertEquals( 5, ceilToMultiple( 3, 5));
        assertEquals( 5, ceilToMultiple( 4, 5));
        assertEquals( 5, ceilToMultiple( 5, 5));
        assertEquals(10, ceilToMultiple( 6, 5));
        assertEquals(10, ceilToMultiple( 7, 5));
        assertEquals(10, ceilToMultiple( 8, 5));
        assertEquals(10, ceilToMultiple( 9, 5));
        assertEquals(10, ceilToMultiple(10, 5));
        assertEquals(15, ceilToMultiple(11, 5));
        assertEquals(15, ceilToMultiple(12, 5));
        assertEquals(15, ceilToMultiple(13, 5));
        assertEquals(15, ceilToMultiple(14, 5));
        assertEquals(15, ceilToMultiple(15, 5));
        assertEquals(20, ceilToMultiple(16, 5));
        assertEquals(20, ceilToMultiple(17, 5));
        assertEquals(20, ceilToMultiple(18, 5));

        // Value is a negative number and multiple is an even number.
        assertEquals(  0, ceilToMultiple( -1, 4));
        assertEquals(  0, ceilToMultiple( -2, 4));
        assertEquals(  0, ceilToMultiple( -3, 4));
        assertEquals( -4, ceilToMultiple( -4, 4));
        assertEquals( -4, ceilToMultiple( -5, 4));
        assertEquals( -4, ceilToMultiple( -6, 4));
        assertEquals( -4, ceilToMultiple( -7, 4));
        assertEquals( -8, ceilToMultiple( -8, 4));
        assertEquals( -8, ceilToMultiple( -9, 4));
        assertEquals( -8, ceilToMultiple(-10, 4));
        assertEquals( -8, ceilToMultiple(-11, 4));
        assertEquals(-12, ceilToMultiple(-12, 4));

        // Value is a negative number and multiple is an odd number.
        assertEquals(  0, ceilToMultiple( -1, 3));
        assertEquals(  0, ceilToMultiple( -2, 3));
        assertEquals( -3, ceilToMultiple( -3, 3));
        assertEquals( -3, ceilToMultiple( -4, 3));
        assertEquals( -3, ceilToMultiple( -5, 3));
        assertEquals( -6, ceilToMultiple( -6, 3));
        assertEquals( -6, ceilToMultiple( -7, 3));
        assertEquals( -6, ceilToMultiple( -8, 3));
        assertEquals( -9, ceilToMultiple( -9, 3));
        assertEquals( -9, ceilToMultiple(-10, 3));
        assertEquals( -9, ceilToMultiple(-11, 3));
        assertEquals(-12, ceilToMultiple(-12, 3));
    }

    /**
     * Tests the truncation of a value to the nearest (not lesser) multiple of another value when
     * the multiple is an illegal zero value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCeilToMultipleIllegalZeroMultiple() {
        ceilToMultiple(10, 0);
    }

    /**
     * Tests the rounding of a value to the nearest (not lesser) multiple of another value when
     * the multiple is an illegal negative value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCeilToMultipleIllegalNegativeMultiple() {
        ceilToMultiple(10, -1);
    }

    /**
     * Tests that a "result" is correctly rounded according to WCA Regulations.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testRoundResult() throws Exception {
        // Values under 10 minutes are truncated to the nearest (not greater) 1/100 s.
        assertEquals(0, roundResult(9));
        assertEquals(10, roundResult(10));
        assertEquals(10, roundResult(15));
        assertEquals(10, roundResult(19));
        assertEquals(20, roundResult(20));
        assertEquals(20, roundResult(20));

        assertEquals(21_000, roundResult(21_001));
        assertEquals(21_000, roundResult(21_009));
        assertEquals(21_010, roundResult(21_010));
        assertEquals(21_010, roundResult(21_019));
        assertEquals(21_990, roundResult(21_999));
        assertEquals(599_990, roundResult(599_999)); // *Almost* 10 minutes.

        // Values over 10 minutes are rounded to the nearest second (half-up rounding).
        assertEquals(600_000, roundResult(600_499));
        assertEquals(601_000, roundResult(600_500));
        assertEquals(601_000, roundResult(600_999));
        assertEquals(601_000, roundResult(601_499));
        assertEquals(602_000, roundResult(601_500));
        assertEquals(1_601_000, roundResult(1_601_499));
        assertEquals(1_602_000, roundResult(1_601_500));
        assertEquals(1_602_000, roundResult(1_601_999));
    }

    /**
     * Tests that the rounding multiple for WCA use is correctly chosen.
     *
     * @throws Exception If the test cannot be executed.
     */
    @Test
    public void testGetRoundingMultiple() throws Exception {
        // Times with a *magnitude* < 10 minutes are rounded/truncated to a multiple of 10 ms.
        assertEquals(10, getRoundingMultiple( 599_999));
        assertEquals(10, getRoundingMultiple(       1));
        assertEquals(10, getRoundingMultiple(       0));
        assertEquals(10, getRoundingMultiple(      -1));
        assertEquals(10, getRoundingMultiple(-599_999));

        // Times with a *magnitude* >= 10 minutes are rounded/truncated to a multiple of 1,000 ms.
        assertEquals(1_000, getRoundingMultiple( 600_001));
        assertEquals(1_000, getRoundingMultiple( 600_000));
        assertEquals(1_000, getRoundingMultiple(-600_000));
        assertEquals(1_000, getRoundingMultiple(-600_001));
    }
}
