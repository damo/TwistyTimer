package com.aricneto.twistytimer.utils;

import android.text.Html;
import android.text.Spanned;

import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.stats.Statistics;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static com.aricneto.twistytimer.stats.Statistics.TIME_DNF;
import static com.aricneto.twistytimer.stats.Statistics.TIME_UNKNOWN;

/**
 * Utility methods for formatting solve times to strings and parsing solve
 * times from formatted strings. Formatting methods are typically intended for
 * use in specific contexts, for example, when exporting times to an "external"
 * format file, displaying average-of-N times in the statistics table, or
 * displaying the time shown in the running puzzle timer. In general, when
 * presenting times to the user, the time to be formatted should already be
 * rounded/truncated in accordance with WCA Regulations. Support for time
 * values representing "unknown" averages (such as for an Ao12 when only 10
 * solve times have been recorded), or "DNF" times is dependent on the
 * formatting method chosen, so see the method descriptions for more details.
 */
public class TimeUtils {
    /**
     * The number of milliseconds in one minute.
     */
    private static final long MS_IN_M = 60_000L;

    /**
     * The number of milliseconds in one hour.
     */
    private static final long MS_IN_H = MS_IN_M * 60L;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TimeUtils() {
    }

    /**
     * <p>
     * Formats a time for a single puzzle solve attempt (a WCA "result" time)
     * as a plain string. The format is suitable for presentation in tables of
     * statistics showing best or worst results, or when sharing result times
     * in short messages. To format an average-of-N or mean time, call
     * {@link #formatAverageTime(long)}, as the WCA Regulations for rounding
     * average or mean times are different from those for result times.
     * </p>
     * <p>
     * If the time is {@link Statistics#TIME_UNKNOWN}, "--" will be returned;
     * if the time is {@link Statistics#TIME_DNF}, "DNF" is returned. This
     * allows that a result time, such as the "best result" or "worst result",
     * may be "unknown" until some solve times have been recorded and may, even
     * then, be a "DNF" until some non-DNF results are recorded.
     * </p>
     * <p>
     * The given {@code resultTime} value will be rounded/truncated in
     * accordance with WCA Regulations for result times before it is formatted.
     * Rounding/truncation is performed by {@link WCAMath#roundResult(long)}.
     * See the description of that class and method for more details on the
     * regulations and how they are applied.
     * </p>
     * <p>
     * The formatting applied by this method is not suitable when formatting
     * the value of a running timer, as special rounding/truncation is required
     * in that context. Such formatting is better implemented <i>ad hoc</i>.
     * </p>
     *
     * @param resultTime
     *     The result time to be formatted (in milliseconds). This can be
     *     expressed with millisecond precision, as it will be rounded by this
     *     method before it is formatted.
     *
     * @return
     *     The plain string representation of the result time.
     *
     * @throws IllegalArgumentException
     *     If the time is negative (other than the special values described
     *     above, which may be represented as negative "flag" times).
     */
    public static String formatResultTime(long resultTime)
            throws IllegalArgumentException {
        assertTimeIsNotNegative(resultTime);

        return formatTime(
            resultTime == TIME_DNF || resultTime == TIME_UNKNOWN
                ? resultTime : WCAMath.roundResult(resultTime),
            "k':'mm':'ss",
                 "m':'ss",
                 "m':'ss'.'SS",
                      "s'.'SS");
    }

    /**
     * <p>
     * Formats a time for a single puzzle solve attempt (a WCA "result" time)
     * with mark-up to make it look "pretty". The result is suitable for
     * presentation in the user interface in a view that supports text as a
     * {@code android.text.Spanned} (e.g., {@code android.widget.TextView}).
     * The time field displaying the units of smallest magnitude will be
     * presented using a smaller text size relative to the other fields. The
     * units of smallest magnitude depend on the magnitude of the time. For
     * times under ten minutes, the smallest units are hundredths of a second.
     * For times over ten minutes, the smallest units are whole seconds.
     * </p>
     * <p>
     * The given {@code resultTime} is rounded in the same manner as described
     * for {@link #formatResultTime(long)}.
     * </p>
     *
     * @param resultTime
     *     The result time to be formatted (in milliseconds). This can be
     *     expressed with millisecond precision, as it will be rounded by this
     *     method before it is formatted.
     *
     * @return
     *     The formatted time in a {@code Spanned} object that includes mark-up
     *     to make the time look "pretty".
     *
     * @throws IllegalArgumentException
     *     If the time is negative, or one of the special values
     *     {@link Statistics#TIME_DNF} or {@link Statistics#TIME_UNKNOWN};
     *     these values are <i>not</i> supported.
     */
    // "Html.fromHtml(String)" is deprecated from API 24, but this app supports
    // API 16+ and the suggested replacement method, "Html.fromHtml(String,int)"
    // is not available before API 24.
    @SuppressWarnings("deprecation")
    public static Spanned prettyFormatResultTime(long resultTime)
            throws IllegalArgumentException {
        assertTimeIsReal(resultTime); // IAE if TIME_DNF, TIME_UNKNOWN or -ve.

        return Html.fromHtml(
            formatTime(
                WCAMath.roundResult(resultTime),
                "k':'mm'<small>:'ss'</small>'",
                     "m'<small>:'ss'</small>'",
                     "m':'ss'<small>:'SS'</small>'",
                          "s'<small>.'SS'</small>'"));
    }

    /**
     * <p>
     * Formats the given average (or mean) time to a plain string. The format
     * is suitable for presentation in tables of statistics, or when sharing
     * average times in short messages. To format a result time, call
     * {@link #formatResultTime(long)}, as the WCA Regulations for rounding
     * result times are different from those for average or mean times.
     * </p>
     * <p>
     * If the time is {@link Statistics#TIME_UNKNOWN}, "--" will be returned;
     * if the time is {@link Statistics#TIME_DNF}, "DNF" is returned. This
     * allows that an average time may be "unknown" until it enough solve
     * times have been recorded and may, even then, be a "DNF" if too many of
     * the recorded times are DNFs.
     * </p>
     * <p>
     * The given {@code averageTime} value will be rounded/truncated in
     * accordance with WCA Regulations for average or mean times before it is
     * formatted. Rounding/truncation is performed by
     * {@link WCAMath#roundAverage(long)}. See the description of that class
     * and method for more details on the regulations and how they are applied.
     * </p>
     *
     * @param averageTime
     *     The average time value in milliseconds. This can be expressed with
     *     millisecond precision, as it will be rounded by this method before
     *     it is formatted.
     *
     * @return
     *     The plain string representation of the average time.
     *
     * @throws IllegalArgumentException
     *     If the time is negative (other than the special values described
     *     above, which may be represented as negative "flag" times).
     */
    public static String formatAverageTime(long averageTime)
            throws IllegalArgumentException {
        assertTimeIsNotNegative(averageTime);

        return formatTime(
            averageTime == TIME_DNF || averageTime == TIME_UNKNOWN
                ? averageTime : WCAMath.roundAverage(averageTime),
            "k':'mm':'ss",
                 "m':'ss",
                 "m':'ss'.'SS",
                      "s'.'SS");
    }

    /**
     * <p>
     * Formats the given result time to a plain string with a low resolution of
     * whole seconds for all times. This is useful for presenting histograms
     * of times that fall into one-second frequency "buckets", or for
     * labeling the time axis when charting solve times.
     * </p>
     * <p>
     * The given {@code resultTime} is rounded in the same manner as described
     * for {@link #formatResultTime(long)}, though it will never be formatted
     * to show fractions of a second.
     * </p>
     *
     * @param resultTime
     *     The result time value in milliseconds. The formatted value will be
     *     rounded to the nearest whole second. To truncate to the nearest (not
     *     greater) whole second, truncate this value <i>before</i> passing it
     *     to this method, so that the required value will not be rounded in an
     *     unwanted manner. The value {@link Statistics#TIME_DNF} is supported.
     *
     * @return
     *     The low-resolution string representation of the result time.
     *
     * @throws IllegalArgumentException
     *     If the time is negative (other than {@code TIME_DNF}), or the special
     *     value {@link Statistics#TIME_UNKNOWN}.
     */
    public static String formatResultTimeLoRes(long resultTime) {
        // The histogram may show "DNF" as a "bucket", but never "TIME_UNKNOWN".
        assertTimeIsNotNegative(resultTime);
        assertTimeIsNotUnknown(resultTime);

        return formatTime(
            resultTime == TIME_DNF
                ? resultTime : WCAMath.roundResult(resultTime),
            "k':'mm':'ss",
                 "m':'ss",
                 "m':'ss",
                      "s");
    }

    /**
     * Formats the given time to a string with high resolution to milliseconds
     * in a form suitable when exporting solve times in the "external" file
     * format ("[M:]SS.sss"). The output of this method can be parsed back to a
     * time value using {@link #parseTimeExternal(String)}. There is no hours
     * field, so the minutes field value can exceed 59.
     *
     * @param exactTime
     *     The exact time value in milliseconds. This must <i>not</i> be
     *     rounded or truncated. The full precision (to the millisecond) must
     *     be given to ensure that the time value is not changed when exported
     *     and then re-imported, allowing proper detection of duplicate times.
     *     The value should come straight from a database solve record, or from
     *     {@link Solve#getExactTime()} (<i>not</i> {@code Solve.getTime()},
     *     which returns a rounded value).
     *
     * @return
     *     The string representation of the time.
     *
     * @throws IllegalArgumentException
     *     If the time is negative, or one of the special values
     *     {@link Statistics#TIME_DNF} or {@link Statistics#TIME_UNKNOWN};
     *     these values are <i>not</i> supported.
     */
    public static String formatTimeExternal(long exactTime) {
        assertTimeIsReal(exactTime);

        return formatTime(
            exactTime,
            "m':'ss'.'SSS",
            "m':'ss'.'SSS",
            "m':'ss'.'SSS",
                 "s'.'SSS");
    }

    /**
     * <p>
     * Formats the given time to a string, selecting from one of the given
     * formats depending on the magnitude of the time value. The formats
     * should be specified in the notation supported by the <i>Joda-Time</i>
     * library's
     * <a href="http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html">DateTimeFormat</a>
     * class.
     * </p>
     * <p>
     * The time format is selected based on the magnitude of the {@code time}
     * value in milliseconds. It does not account for rounding that may occur
     * during formatting. For example, if the time is 59,543 ms, that may be
     * formatted to "59.54" seconds. However, if the format specifies only whole
     * seconds, it will be formatted to "60" (seconds), not "1:00" (one minute),
     * as the precise time value is less than one minute and {@code lt1MFormat}
     * is chosen. To avoid this issue, the given time could be rounded to the
     * nearest value of the appropriate magnitude before passing it to this
     * method. The same caveat applies to the application of the mask to zero
     * value: a time near zero may be rounded to zero during formatting. For
     * example, 1 ms might be formatted to "0.00" instead of "--", as 1 ms is
     * not zero. Again, round the time before passing it to this method to
     * achieve the desired outcome.
     * </p>
     * <p>
     * The WCA Regulations specify different rounding and truncation for times
     * below ten minutes and times under ten minutes, so the formatting of such
     * times has different requirements. The format specification arguments
     * allow for different formats on either side of this ten-minute boundary.
     * However, if this is not a requirement for a formatting times in a
     * particular context, the caller may pass the same format specification to
     * several of the arguments and times will be formatted in the same manner
     * for each.
     * </p>
     *
     * @param time
     *     The time (duration) to be formatted. The value is in milliseconds.
     *     The value will not be rounded or truncated before formatting, though
     *     the formatting will round to the nearest whole unit of smallest
     *     magnitude. For "unknown" times, such as the time reported for an
     *     average-of-N times when less than "N" times have been recorded, use
     *     {@code TIME_UNKNOWN} (formatted as "--"). For did-not-finish solve
     *     times, use {@code TIME_DNF} (formatted as "DNF"). Negative times are
     *     not supported.
     * @param gte1HFormat
     *     The format to use for times greater than or equal to one hour.
     * @param gte10MFormat
     *     The format to use for times greater than or equal to ten minutes and
     *     less than one hour.
     * @param gte1MFormat
     *     The format to use for times greater than or equal to one minute and
     *     less than ten minutes.
     * @param lt1MFormat
     *     The format to use for times less than one minute.
     *
     * @return The formatted time value.
     *
     * @throws IllegalArgumentException
     *     If the time is negative, other than the supported negative flag
     *     values {@code TIME_DNF} and {@code TIME_UNKNOWN}.
     */
    private static String formatTime(
            long time, String gte1HFormat, String gte10MFormat,
            String gte1MFormat, String lt1MFormat) {
        assertTimeIsNotNegative(time); // Allows TIME_DNF and TIME_UNKNOWN.

        if (time == TIME_DNF) {
            // Expected to be just the short string "DNF".
            return Penalty.DNF.getDescription();
        }

        if (time == TIME_UNKNOWN) {
            return "--";
        }

        final String format;

        if (time / MS_IN_H >= 1L) {
            format = gte1HFormat;
        } else if (time / MS_IN_M >= 10L) {
            format = gte10MFormat;
        } else if (time / MS_IN_M >= 1L) {
            format = gte1MFormat;
        } else {
            format = lt1MFormat;
        }

        return new DateTime(time, DateTimeZone.UTC).toString(format);
    }

    /**
     * <p>
     * Converts times in the format "M:SS.s", or "S.s" into an integer number
     * of milliseconds. The minutes value may be padded with zeros. The "s"
     * is the fractional number of seconds and may be given to arbitrary
     * precision, but will be rounded to a whole number of milliseconds. For
     * example, "1:23.45" is parsed to 83,450 ms and "95.6789" is parsed to
     * 95,679 ms. Where minutes are present, the seconds can be padded with
     * zeros or not, but the number of seconds must be less than 60, or the
     * time will be treated as invalid. Where minutes are not present, the
     * number of seconds can exceed 59. Note that an hours field is not
     * supported, but the minutes value can exceed 59 if necessary.
     * </p>
     * <p>
     * The parser is suitable for use when parsing the "external" format (simple
     * text) export file. It can also be used when parsing times input manually
     * by the user.
     * </p>
     *
     * @param time
     *     The time string to be parsed. Leading and trailing whitespace will be
     *     trimmed before the time is parsed. If minus signs are present, the
     *     result is not defined.
     *
     * @return
     *     The parsed time in milliseconds. The value is rounded to the nearest
     *     millisecond. If the time cannot be parsed because the format does not
     *     conform to the requirements, zero is returned.
     */
    public static long parseTimeExternal(String time) {
        final String timeStr = time.trim();
        final int colonIdx = timeStr.indexOf(':');
        long parsedTime = 0;

        try {
            if (colonIdx != -1) {
                if (colonIdx > 0 && colonIdx < timeStr.length()) {
                    // At least one digit for the minutes, so still a valid
                    // time format. Format is expected to be "m:s.S" (zero
                    // padding to "mm" and "ss" is optional).
                    final int minutes
                        = Integer.parseInt(timeStr.substring(0, colonIdx));
                    final float seconds
                        = Float.parseFloat(timeStr.substring(colonIdx + 1));

                    if (seconds < 60f) {
                        parsedTime += MS_IN_M * minutes
                                      + Math.round(seconds * 1_000f);
                    } // else "parsedTime" stays zero as seconds >= 60.
                } // else "parsedTime" stays zero: nothing before or after ":".
            } else {
                // Format is expected to be "s.S", with arbitrary precision and
                // padding. Round to eliminate precision errors (e.g., 24.299999
                // will be interpreted as "24,300 ms".
                parsedTime = Math.round(
                    Float.parseFloat(timeStr.substring(colonIdx + 1)) * 1_000f);
            }
        } catch (NumberFormatException ignore) {
            parsedTime = 0; // Invalid time format.
        }

        return parsedTime;
    }

    /**
     * Asserts that the given time value is not {@link Statistics#TIME_DNF}.
     *
     * @param time The time value that should not be a DNF.
     *
     * @throws IllegalArgumentException If the time is a DNF.
     */
    private static void assertTimeIsNotDNF(long time)
            throws IllegalArgumentException {
        if (time == TIME_DNF) {
            throw new IllegalArgumentException("Time must not be a DNF.");
        }
    }

    /**
     * Asserts that the given time value is not {@link Statistics#TIME_UNKNOWN}.
     *
     * @param time The time value that should not be an "unknown" time.
     *
     * @throws IllegalArgumentException If the time is "unknown".
     */
    private static void assertTimeIsNotUnknown(long time)
            throws IllegalArgumentException {
        if (time == TIME_UNKNOWN) {
            throw new IllegalArgumentException("Time must not be UNKNOWN.");
        }
    }

    /**
     * Asserts that the given time value is not negative. The specific negative
     * values of {@link Statistics#TIME_DNF} and {@link Statistics#TIME_UNKNOWN}
     * <i>are</i> accepted and no error will occur. To exclude those time values
     * in addition to all negative times, use {@link #assertTimeIsReal(long)}.
     *
     * @param time The time value that should not be negative.
     *
     * @throws IllegalArgumentException If the time is negative.
     */
    private static void assertTimeIsNotNegative(long time)
            throws IllegalArgumentException {
        if (time < 0 && time != TIME_DNF && time != TIME_UNKNOWN) {
            throw new IllegalArgumentException(
                "Time must not be negative: " + time);
        }
    }

    /**
     * Asserts that the time is a "real" elapsed time. The time must not be a
     * DNF (represented by {@link Statistics#TIME_DNF}), or be unknown
     * (represented by {@link Statistics#TIME_UNKNOWN}), or be negative.
     *
     * @param time The time value that should be a "real" time.
     *
     * @throws IllegalArgumentException
     *     If the time is a DNF, "unknown", or negative.
     */
    private static void assertTimeIsReal(long time)
            throws IllegalArgumentException {
        assertTimeIsNotDNF(time);
        assertTimeIsNotUnknown(time);
        assertTimeIsNotNegative(time);
    }
}
