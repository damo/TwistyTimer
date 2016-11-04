package com.aricneto.twistytimer.utils;

/**
 * <p>
 * Utility method for rounding and truncating times in accordance with WCA Regulations (April 18,
 * 2016), Article 9.
 * </p>
 * <dl>
 *     <dt><b>WCA Regulation 9f1</b></dt>
 *     <dd>
 *         <i>"All timed results under 10 minutes are measured and truncated to the nearest
 *         hundredth of a second. All timed averages and means under 10 minutes are measured and
 *         rounded to the nearest hundredth of a second."</i>
 *     </dd>
 *     <dt><b>WCA Regulation 9f2</b></dt>
 *     <dd>
 *         <i>"All timed results, averages, and means over 10 minutes are measured and rounded to
 *         the nearest second (e.g. x.4 becomes x, x.5 becomes x+1)."</i>
 *     </dd>
 * </dl>
 *
 * @author damo
 */
public final class WCAMath {
    /**
     * The number of milliseconds in ten minutes.
     */
    private static final long MS_IN_10_MIN = 600_000L; // 10 minutes == 600 seconds == 600,000 ms

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private WCAMath() {
    }

    /**
     * Gets the rounding multiple that should be used when rounding/truncating the given time value.
     *
     * @param time
     *     The time for which the rounding multiple is required (in milliseconds). May be negative.
     *
     * @return
     *     The rounding multiple appropriate for the magnitude of {@code time}. For times under 10
     *     minutes in magnitude (positive or negative), the multiple be 10 ms (0.01 seconds);
     *     for times of 10 minutes or over in magnitude, the multiple is 1,000 ms (1 second).
     */
    public static long getRoundingMultiple(long time) {
        return Math.abs(time) < MS_IN_10_MIN ? 10L : 1_000L;
    }

    /**
     * Rounds/truncates a "result" time for a single solve. WCA Regulations 9f1 and 9f2 are applied.
     * If the time is an "average" or "mean", it should be rounded by {@link #roundAverage(long)}.
     *
     * @param time The result time to be rounded (in milliseconds).
     *
     * @return The correctly rounded/truncated result time.
     *
     * @throws IllegalArgumentException If the time is negative.
     */
    public static long roundResult(long time) throws IllegalArgumentException {
        // NOTE: Negative values (times) are supported by "roundToMultiple" and "floorToMultiple",
        // but not by this method. No "result" time will every legitimately be negative. However,
        // a negative time value may be used to represent a "DNF" (in some contexts, such as when
        // formatting times). In that case, it would indicate a bug, so this exception flags it.
        if (time < 0) {
            throw new IllegalArgumentException("Result time must not be negative: " + time);
        }

        // NOTE: As noted in "Solve.getTime()", if the regulations change, this method should be
        // modified to take "Solve.getDate()" and then apply the WCA Regulations effective on that
        // date. However, this is probably unlikely, so even the signature of this method does not
        // take a date.
        //
        // For starters, the WCA Regulations dated April 18, 2016 (see the class description) will
        // be applied (any differences in the Regulations before that date have not been
        // investigated). The regulations are interpreted thus:
        //
        //     "... truncated to the nearest ..." is assumed to mean rounded to the nearest
        //     hundredth of a second that is *not greater* than the time. For example, a timed
        //     result of 21.266 for a single solve is rounded down to 21.26, not up to the nearer
        //     21.27. This is made more explicit in the WCA "Guidelines". It should be safe to
        //     assume that it cannot mean the same thing as "... rounded to the nearest ...".
        //
        //     "... rounded to the nearest ..." is assumed to mean half-up rounding to the nearest
        //     hundredth of a second. For example, an average of 21.265 is rounded up to 21.27
        //     while an average of 21.264 is rounded down to 21.26.
        //
        //     "... under 10 minutes ..." and "... over 10 minutes ..." do not specify what should
        //     happen if the time is "equal to 10 minutes" or if it rounds/truncates to 10 minutes.
        //     However, if the value is exactly 10 minutes, then no rounding or truncation of any
        //     kind is required, as it is already rounded and truncated to any multiple of
        //     hundredths or whole seconds.
        //
        return time >= MS_IN_10_MIN
                // Round (half-up) to nearest 1 s (1,000 ms) greater, lesser or equal to "time".
                ? roundToMultiple(time, getRoundingMultiple(time))
                // *Truncate* to nearest or equal 1/100 s (10 ms) not greater than "time". "time"
                // is a "result", so it is truncated ("floor"), not rounded.
                : floorToMultiple(time, getRoundingMultiple(time));
    }

    /**
     * Rounds an "average" or "mean" time for multiple solves. WCA Regulations 9f1 and 9f2 are
     * applied. When calculating the average (truncated mean or "average-of-N") or mean of a
     * number of "result" times, the "result" times must first be rounded/truncated by
     * {@link #roundResult(long)} before they are summed.
     *
     * @param time The average or mean time to be rounded (in milliseconds).
     *
     * @return The correctly rounded average or mean time.
     *
     * @throws IllegalArgumentException If the time is negative.
     */
    public static long roundAverage(long time) {
        // See comment in "roundResult" for the reason why negative times are not allowed. For
        // averages, it is also possible that "AverageCalculator" could return a "DNF" or "UNKNOWN"
        // value, both of which could never legitimately be passed to this method.
        if (time < 0) {
            throw new IllegalArgumentException("Average time must not be negative: " + time);
        }

        // For an average/mean time, *always* round (half-up) to the nearest multiple appropriate
        // for the magnitude of "time" (i.e., 10 ms if abs(time) < 10 minutes, otherwise 1,000 ms).
        return roundToMultiple(time, getRoundingMultiple(time));
    }

    /**
     * Rounds a value to the nearest multiple of another value.
     *
     * @param value
     *     The value to be rounded. May be negative.
     * @param multiple
     *     The multiple to which the value should be rounded. For example, if the multiple is 10
     *     and the value is 996, then it will be rounded to 1,000, the nearest multiple of 10. The
     *     multiple must be a positive number (i.e., greater than zero).
     *
     * @return
     *     The value after rounding to the nearest multiple. Where the value is exactly between two
     *     multiples, the "half-up" rounding convention is applied with negative numbers rounding
     *     towards positive infinity. For example, if the multiple is 10, then 15 rounds to 20 and
     *     -15 rounds to -10. Consistent with the behaviour of {@code java.lang.Math.round}.
     *
     * @throws IllegalArgumentException
     *     If {@code multiple} is not greater than zero.
     */
    public static long roundToMultiple(long value, long multiple) throws IllegalArgumentException {
        if (multiple <= 0) {
            throw new IllegalArgumentException("'multiple' must be positive: " + multiple);
        }

        final long remainder = value % multiple;

        // Half-up rounding is towards positive infinity, for both +ve and -ve values.
        return value >= 0
                // "value" and "remainder" are positive or zero.
                ? remainder >= (multiple + 1) / 2  // "+1" adjusts for odd "multiple".
                    ? value + multiple - remainder // Round up +ve value
                    : value - remainder            // Round down +ve value
                // "value" is negative and "remainder" negative or zero.
                : -remainder > multiple / 2
                    ? value - multiple - remainder // Round up -ve value
                    : value - remainder;           // Round down -ve value
    }

    /**
     * Gets the largest value (closest to positive infinity) that is less than or equal to a given
     * value and that is a multiple of another value. This function may be useful when rounding
     * time values used to present a forward-running (non-countdown) timer.
     *
     * @param value
     *     The value to be rounded down. May be negative.
     * @param multiple
     *     The multiple to which the value should be rounded. For example, if the multiple is 10
     *     and the value is 999, then it will be rounded down to 990, the nearest multiple of 10
     *     that is not larger than 999; or if the multiple is 10 and the value is -991, then it
     *     will be rounded down to -1,000. The multiple must be a positive number (i.e., greater
     *     than zero).
     *
     * @return
     *     The value after rounding down to the multiple. Consistent with the behaviour of
     *     {@code java.lang.Math.floor}.
     *
     * @throws IllegalArgumentException
     *     If {@code multiple} is not greater than zero.
     */
    public static long floorToMultiple(long value, long multiple) throws IllegalArgumentException {
        if (multiple <= 0) {
            throw new IllegalArgumentException("'multiple' must be positive: " + multiple);
        }

        final long remainder = value % multiple;

        // Numbers round towards negative infinity, not zero, so correct when "value" is negative.
        return value - remainder - (value < 0 && remainder != 0 ? multiple : 0);
    }

    /**
     * Gets the smallest value (closest to negative infinity) that is greater than or equal to a
     * given value and that is a multiple of another value. This function may be useful when
     * rounding time values used to present a backward-running (countdown) timer.
     *
     * @param value
     *     The value to be rounded up. May be negative.
     * @param multiple
     *     The multiple to which the value should be rounded. For example, if the multiple is 10
     *     and the value is 991, then it will be rounded up to 1,000, the nearest multiple of 10
     *     that is not smaller than 991; or if the multiple is 10 and the value is -999, then it
     *     will be rounded up to -990. The multiple must be a positive number (i.e., greater than
     *     zero).
     *
     * @return
     *     The value after rounding down to the multiple. Consistent with the behaviour of
     *     {@code java.lang.Math.ceil}.
     *
     * @throws IllegalArgumentException
     *     If {@code multiple} is not greater than zero.
     */
    public static long ceilToMultiple(long value, long multiple) throws IllegalArgumentException {
        final long floorValue = floorToMultiple(value, multiple); // May throw the IAE.

        return floorValue == value ? value : floorValue + multiple;
    }
}
