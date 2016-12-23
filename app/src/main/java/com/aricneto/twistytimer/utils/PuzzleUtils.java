package com.aricneto.twistytimer.utils;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.stats.AverageCalculator;
import com.aricneto.twistytimer.stats.Statistics;

import java.util.Map;

/**
 * Utility methods for managing puzzle types, formatting solve times and
 * sharing solve statistics.
 */
public final class PuzzleUtils {
    /**
     * Shares an average-of-N, formatted to a simple string.
     *
     * @param n
     *     The value of "N" for which the average is required.
     * @param stats
     *     The statistics that contain the required details about the average.
     * @param activityContext
     *     An activity context required to start the sharing activity. An
     *     application context is not appropriate, as using it may disrupt the
     *     task stack.
     *
     * @return
     *     {@code true} if it is possible to share the average; or {@code false}
     *     if it is not.
     */
    public static boolean shareAverageOf(
            int n, @NonNull Statistics stats,
            @NonNull Activity activityContext) {

        final String averageOfN = formatAverageOfN(n, stats);

        if (averageOfN != null) {
            final Intent shareIntent = new Intent();
            final String puzzleName
                = stats.getMainState().getPuzzleType().getShortName();

            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                puzzleName + ": " + formatAverageOfN(n, stats));
            shareIntent.setType("text/plain");
            activityContext.startActivity(shareIntent);

            return true;
        }

        return false;
    }

    /**
     * Formats the details of the most recent average-of-N calculation for
     * times recorded in the current session. The string shows the average
     * value and the list of times that contributed to the calculation of
     * that average. If the average calculation requires the elimination of
     * the best and worst times, these times are shown in parentheses.
     *
     * @param n
     *     The value of "N" for which the "average-of-N" is required.
     * @param stats
     *     The statistics from which to get the details of the average
     *     calculation.
     *
     * @return
     *     The average-of-N in string format; or {@code null} if there is no
     *     average calculated for that value of "N", or if insufficient (less
     *     than "N") times have been recorded in the current session, of if
     *     {@code stats} is {@code null}.
     */
    private static String formatAverageOfN(int n, Statistics stats) {
        if (stats == null || stats.getAverageOf(n, true) == null) {
            return null;
        }

        final AverageCalculator.AverageOfN aoN
            = stats.getAverageOf(n, true).getAverageOfN();
        final long[] times = aoN.getTimes();
        final long average = aoN.getAverage();

        if (average == Statistics.TIME_UNKNOWN || times == null) {
            return null;
        }

        final StringBuilder s
            = new StringBuilder(TimeUtils.formatAverageTime(average));

        s.append(" = ");

        for (int i = 0; i < n; i++) {
            final String time = TimeUtils.formatResultTime(times[i]);

            // The best and worst indices may be -1, but that is OK: they just
            // will not be marked.
            if (i == aoN.getBestTimeIndex() || i == aoN.getWorstTimeIndex()) {
                s.append('(').append(time).append(')');
            } else {
                s.append(time);
            }

            if (i < n - 1) {
                s.append(", ");
            }
        }

        return s.toString();
    }

    /**
     * Shares a single solve time.
     *
     * @param solve
     *     The solve time to be shared.
     * @param activityContext
     *     An activity context required to start the sharing activity. An
     *     application context is not appropriate, as using it may disrupt the
     *     task stack.
     *
     * @return
     *     {@code true} if the solve was shared successfully; or {@code false}
     *     if it was not.
     */
    public static boolean shareSolveTime(@NonNull Solve solve,
                                         @NonNull Activity activityContext) {
        final Intent shareIntent = new Intent();
        final String puzzleName = solve.getPuzzleType().getShortName();

        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT,
            puzzleName + ": "
            // "formatResultTime()" will do the rounding/truncation.
            + TimeUtils.formatResultTime(solve.getExactTime()) + "s."
            + (solve.hasComment()  ? "\n" : "")
            + solve.getComment()    // empty if not present
            + (solve.hasScramble() ? "\n" : "")
            + solve.getScramble()); // empty if not present
        shareIntent.setType("text/plain");
        activityContext.startActivity(shareIntent);

        return true;
    }

    /**
     * Shares a histogram showing the frequency of solve times falling into
     * intervals of one second. Only times for the current session are presented
     * in the histogram.
     *
     * @param stats
     *     The statistics that contain the required details to present the
     *     histogram. If {@code null}, no statistics will be shared.
     * @param activityContext
     *     An activity context required to start the sharing activity. An
     *     application context is not appropriate, as using it may disrupt the
     *     task stack.
     *
     * @return
     *     {@code true} if it is possible to share the histogram; or
     *     {@code false} if it is not.
     */
    public static boolean shareHistogram(
            @Nullable Statistics stats, @NonNull Activity activityContext) {

        // Count includes DNFs.
        final int solveCount = stats != null ? stats.getSessionNumSolves() : 0;

        if (solveCount > 0) {
            final Intent shareIntent = new Intent();
            final String puzzleName
                = stats.getMainState().getPuzzleType().getShortName();

            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                activityContext.getString(
                    R.string.fab_share_histogram_solvecount,
                    puzzleName, solveCount)
                + ":" + createHistogram(stats));
            shareIntent.setType("text/plain");
            activityContext.startActivity(shareIntent);

            return true;
        }

        return false;
    }

    /**
     * Creates a histogram of the frequencies of solve times for the current
     * session. Times are truncated to whole seconds.
     *
     * @param stats
     *     The statistics from which to get the frequencies.
     *
     * @return
     *     A multi-line string presenting the histogram using "ASCII art"; or
     *     an empty string if no times have been recorded.
     */
    private static String createHistogram(@NonNull Statistics stats) {
        final StringBuilder histogram = new StringBuilder(1_000);
        final Map<Long, Integer> timeFreqs = stats.getSessionTimeFrequencies();

        // Iteration order starts with "DNF" and then goes by increasing time.
        // Use a fixed-width field for the time, right justified, assuming that
        // the "bucket" time will be no greater than 9:59:59 (7 characters).
        for (Long time : timeFreqs.keySet()) {
            // "time" is already truncated to whole seconds, or is "TIME_DNF".
            histogram.append(
                String.format("\n%7s: %s",
                    TimeUtils.formatResultTimeLoRes(time), // 1s bucket interval
                    convertToBar(timeFreqs.get(time))));   // "bar" of count
        }

        return histogram.toString();
    }

    /**
     * Creates a "bar" string containing a sequence of block ('█') characters to
     * form the "bar". Used for histograms.
     *
     * @param n The length (in characters) of the "bar".
     *
     * @return
     *     A string containing the required number of block characters to form
     *     the "bar".
     */
    private static String convertToBar(int n) {
        final StringBuilder bar = new StringBuilder();

        for (int i = 0; i < n; i++) {
            bar.append("█");
        }

        return bar.toString();
    }
}
