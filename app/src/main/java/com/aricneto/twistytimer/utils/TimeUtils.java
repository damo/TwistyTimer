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
     * Formats a time for a single puzzle solve attempt (a WCA "result" time).
     * The time format includes mark-up to make it "pretty" and is suitable for
     * presentation in the user interface in a view that supports
     * {@code android.text.Spanned} text ({@code android.widget.TextView}, for
     * example). The time field displaying the units of smallest magnitude will
     * be presented using a smaller text size relative to the other fields. The
     * units of smallest magnitude depend on the magnitude of the time. For
     * times under ten minutes, the smallest units are hundredths of a second.
     * For times over ten minutes, the smallest units are whole seconds.
     * </p>
     * <p>
     * The time value should be rounded/truncated in accordance with WCA
     * Regulations <i>before</i> passing it to this method. For example, the
     * value from {@link Solve#getTime()} is already rounded correctly.
     * </p>
     *
     * @param time
     *     The result time to be formatted (in milliseconds).
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
    public static Spanned formatResultPretty(long time)
            throws IllegalArgumentException {
        assertTimeIsReal(time); // Throws IAE if TIME_DNF, TIME_UNKNOWN or -ve.

        return Html.fromHtml(formatTime(time,
                "k':'mm'<small>:'ss'</small>'",
                     "m'<small>:'ss'</small>'",
                     "m':'ss'<small>:'SS'</small>'",
                          "s'<small>.'SS'</small>'"));
    }

    /**
     * <p>
     * Formats a time for an active puzzle solve attempt presented in an
     * on-screen "chronometer". The time format includes "pretty" mark-up as
     * described in {@link #formatResultPretty(long)}, except that if a
     * low-resolution time is showing only the (whole) seconds field, that
     * single field is not displayed in a smaller text size.
     * </p>
     * <p>
     * For a chronometer, it is most appropriate that the time be
     * <i>truncated</i> to the nearest (not greater) unit of resolution.
     * Truncation ensures, for example, that a chronometer showing time in
     * whole seconds does not "tick" to the next second when it passes the
     * half-way point between two second; it will only "tick" when the full
     * second has elapsed. As the displayed resolution of the time varies
     * with the magnitude of the time passed to this method, the necessary
     * truncation will be performed by this method before formatting the time.
     * </p>
     * <p>
     * A chronometer, such as
     * {@link com.aricneto.twistytimer.layout.ChronometerMilli} also keeps a
     * record of penalties to be applied to the time and may include
     * indications in the time text of these penalties, or replace the text
     * entirely for some penalties. To support this, the current penalty can
     * be identified and the formatted time will be simply "DNF" if the
     * penalty is a DNF, or will have a small "+" appended if the penalty is
     * a two-second penalty. It is up to the chronometer to decide to include
     * that penalty in the {@code time}, or not.
     * </p>
     * <p>
     * An option is also given to allow the chronometer to show a high- or low-resolution time.
     * (This supports one of the application's settings.)
     * </p>
     *
     * @param time
     *     The time to be formatted (in milliseconds). No rounding or truncation is necessary.
     * @param showHiRes
     *     {@code true} to show the time to a high resolution of 100ths of a second (for times
     *     under 10 minutes); or {@code false} to show times to a low resolution of whole seconds
     *     times of all magnitudes.
     * @param penalty
     *     The current penalty applying to the chronometer's time value.
     *
     * @return
     *     The formatted time in a {@code Spanned} object that includes mark-up to make the time
     *     look "pretty" and include penalty indicators.
     *
     * @throws IllegalArgumentException
     *     If the time is negative, or one of the special values {@link Statistics#TIME_DNF} or
     *     {@link Statistics#TIME_UNKNOWN}; these values are <i>not</i> supported. However, a
     *     DNF can be notified using {@code penalty}.
     */
    @SuppressWarnings("deprecation") // Same reason as "formatResultPretty".
    public static Spanned formatChronoTime(long time, boolean showHiRes, Penalty penalty)
            throws IllegalArgumentException {
        assertTimeIsReal(time); // Throws IAE if TIME_DNF, TIME_UNKNOWN or negative.

        final String text;

        if (penalty == Penalty.DNF) {
            text = Penalty.DNF.getDescription();
        } else {
            final boolean isLT10Mins = time / MS_IN_M < 10L;

            // Truncate to 1 s (1,000 ms) or 0.01 s (10 ms) depending on the magnitude of the time
            // and the resolution to be shown. The 10-minute threshold matches the point at which
            // the format strings (below) switch from 100ths to whole seconds. Keep them in sync.
            final long chronoTime
                    = WCAMath.floorToMultiple(time, showHiRes && isLT10Mins ? 10L : 1_000L);

            text = formatTime(chronoTime,
                           "k':'mm'<small>:'ss'</small>'",
                                "m'<small>:'ss'</small>'",
                    showHiRes ? "m':'ss'<small>:'SS'</small>'" // High-res < 10 mins.
                              : "m'<small>:'ss'</small>'",             // Low-res  < 10 mins.
                    showHiRes ?             "s'<small>.'SS'</small>'"  // High-res <  1 min.
                              :             "s")                       // Low-res  <  1 min.
                    + (penalty == Penalty.PLUS_TWO ? " <small>+</small>" : "");
        }

        return Html.fromHtml(text);
    }

    /**
     * Indicates if, for the same parameters,
     * {@link #formatChronoTime(long, boolean, Penalty)} will return a
     * high-resolution time for display. A high-resolution time displays the
     * value to 100ths of a second. This may be useful when determining how
     * often to update the displayed time.
     *
     * @param time
     *     The time to be tested (in milliseconds). No rounding or truncation
     *     is necessary.
     * @param showHiRes
     *     {@code true} to show the time to a high resolution of 100ths of a
     *     second (for times under 10 minutes); or {@code false} to show
     *     times to a low resolution of whole seconds times of all magnitudes.
     * @param penalty
     *     The current penalty applying to the chronometer's time value.
     *
     * @return
     *     {@code true} if {@code showHiRes} is {@code true} and {@code penalty}
     *     is not {@code DNF} and the time is less than ten minutes.
     */
    public static boolean isChronoShowingHiRes(
            long time, boolean showHiRes, Penalty penalty) {
        // NOTE: This method it not in "ChronometerMilli", as it it closely
        // tied to the chosen time formats in "formatChronoTime" (in this
        // class). Aside from the "showHiRes" and "penalty" parameters, it is
        // the magnitude of the time and whether or not the format strings in
        // the other method include "s.SS" or just "s" that matters.
        return showHiRes && penalty != Penalty.DNF && time / MS_IN_M < 10L;
    }

    /**
     * <p>
     * Formats the given result, average (or mean) time to a plain string
     * with the regulation WCA resolution. Times of ten minutes or over are
     * formatted to whole seconds, while times under ten minutes are
     * formatted to hundredths of a second. However, there are different
     * regulations for the rounding of "results" <i>versus</i> "averages" and
     * "means", so the given value must be rounded appropriately
     * <i>before</i> it is passed to this method for formatting. The format
     * is suitable for presentation in tables of statistics, or when sharing
     * result/average times in short messages.
     * </p>
     * <p>
     * If the time is {@link Statistics#TIME_UNKNOWN}, "--" will be returned;
     * if the time is {@link Statistics#TIME_DNF}, "DNF" is returned. This
     * allows that an average time may be "unknown" until it enough solve
     * times have been recorded and may, even then, be a "DNF" if too many of
     * the recorded times are DNFs. Similarly, in a table of statistics, the
     * most recent time may be "unknown" until the first time is recorded and
     * may then be "DNF". This is slightly different from other presentations
     * of "DNF" results, as a table of statistics may not have room to show
     * the recorded time <i>and</i> an separate penalty indication (such as
     * when viewing individual times in the {@code EditSolveDialog}).
     * </p>
     *
     * @param time
     *     The time value in milliseconds. The time should first be rounded
     *     in accordance with WCA Regulations for "results" or for
     *     "averages"/"means", whichever is appropriate.
     *
     * @return
     *     The string representation of the time for presentation as a
     *     statistic.
     *
     * @throws IllegalArgumentException
     *     If the time is negative (other than the special values described
     *     above, which may be represented as negative "flag" times).
     */
    public static String formatTimeStatistic(long time)
            throws IllegalArgumentException {
        assertTimeIsNotNegative(time);

        return formatTime(time,
                "k':'mm':'ss",
                     "m':'ss",
                     "m':'ss'.'SS",
                          "s'.'SS");
    }

    /**
     * Formats the given time to a plain string with a low resolution of
     * whole seconds for all times. This is useful for presenting histograms
     * of times that fall into one-second frequency "buckets", or for
     * labeling the time axis of the chart.
     *
     * @param time
     *     The time value in milliseconds. The value will be rounded to the
     *     nearest whole second when formatted. To truncate to the nearest
     *     (not greater) whole second, round this value <i>before</i> passing
     *     it to this method. The value {@link Statistics#TIME_DNF} is
     *     supported.
     *
     * @return
     *     The string representation of the time.
     *
     * @throws IllegalArgumentException
     *     If the time is negative, or the special value
     *     {@link Statistics#TIME_UNKNOWN}.
     */
    public static String formatTimeLoRes(long time) {
        // The histogram may show "DNF" as a "bucket", but never "TIME_UNKNOWN".
        assertTimeIsNotNegative(time);
        assertTimeIsNotUnknown(time);

        return formatTime(time,
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
     * @param time
     *     The time value in milliseconds. This must <i>not</i> be rounded.
     *     The full precision (to the millisecond) should be given to ensure
     *     that the time value is not changed when exported and then
     *     re-imported, allowing proper detection of duplicate times. The value
     *     should come straight from the database record, or from
     *     {@link Solve#getExactTime()} (<i>not</i> {@code Solve.getTime()}).
     *
     * @return
     *     The string representation of the time.
     *
     * @throws IllegalArgumentException
     *     If the time is negative, or one of the special values
     *     {@link Statistics#TIME_DNF} or {@link Statistics#TIME_UNKNOWN};
     *     these values are <i>not</i> supported.
     */
    public static String formatTimeExternal(long time) {
        return formatTime(time,
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
     *     For "unknown" times, such as the time reported for an average-of-N
     *     times when less than "N" times have been recorded, use
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
                    // time format. Format is expected to be "M:S.s" (zero
                    // padding to "MM" and "SS" is optional).
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
                // Format is expected to be "S.s", with arbitrary precision and
                // padding.
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
