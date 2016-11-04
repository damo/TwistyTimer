package com.aricneto.twistytimer.stats;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.WCAMath;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A collection of {@link AverageCalculator} instances that distributes solve times to each
 * calculator. The calculators can be segregated into averages for times from the current session
 * only, and averages for times from all past sessions (including the current session). This also
 * provides a simple API to access the all-time and session mean, best and worst times and solve
 * count.
 *
 * @author damo
 */
public class Statistics {
    /**
     * A special time value that represents a solve that "did-not-finish" (DNF). This is also used
     * to represent the calculated value of an average where too many solves included in the
     * average were DNF solves. This value is recognised by many of the {@code formatTime*()}
     * methods in {@link com.aricneto.twistytimer.utils.PuzzleUtils}.
     */
    public static final long TIME_DNF = -665L;

    /**
     * A value that indicates that a calculated time is unknown. This is usually the case when not
     * enough times have been recorded to satisfy the required number of solves to be included in
     * the calculation of the average, or when all recorded solves are DNFs.
     */
    public static final long TIME_UNKNOWN = -666L;

    /**
     * The average calculators for averages of times across all sessions. The calculators are keyed
     * by the number of times used to calculate the average.
     */
    private final Map<Integer, AverageCalculator> mAllTimeACs;

    /**
     * The average calculators for averages of times in the current session only. The calculators
     * are keyed by the number of times used to calculate the average.
     */
    private final Map<Integer, AverageCalculator> mSessionACs;

    /**
     * The frequencies of solve times across all sessions. The keys are the solve times in
     * milliseconds, but truncated to whole seconds, and the values are the number of solve times.
     * {@link #TIME_DNF} may also be a key.
     */
    private final Map<Long, Integer> mAllTimeTimeFreqs;

    /**
     * The frequencies of solve times for the current session. The keys are the solve times in
     * milliseconds, but truncated to whole seconds and the values are the number of solve times.
     * {@link #TIME_DNF} may also be a key.
     */
    private final Map<Long, Integer> mSessionTimeFreqs;

    /**
     * The main state defining the puzzle type and solve category for these statistics.
     */
    @NonNull
    private final MainState mMainState;

    /**
     * An average calculator for solves across all past sessions and the current session. May be
     * {@code null}.
     */
    private AverageCalculator mOneAllTimeAC;

    /**
     * An average calculator for solves only in the current session. May be {@code null}.
     */
    private AverageCalculator mOneSessionAC;

    /**
     * Creates a new collection for statistics. Use a factory method to create a standard set of
     * statistics.
     *
     * @param mainState
     *     The main state information defining the puzzle type, solve category and history/session
     *     selection for these statistics.
     */
    @SuppressLint("UseSparseArrays") // They are documented to be *worse* performing.
    private Statistics(@NonNull MainState mainState) {
        mMainState = mainState;

        mAllTimeACs = new HashMap<>();
        mSessionACs = new HashMap<>();

        // NOTE: "TreeMap" ensures that the entries are ordered by key value, i.e., in increasing
        // order of solve time.
        mAllTimeTimeFreqs = new TreeMap<>();
        mSessionTimeFreqs = new TreeMap<>();
    }

    /**
     * <p>
     * Creates a new statistics instance that is a shallow copy of a given statistics instance.
     * for statistics. This is not intended to support the creation of a fully independent copy of
     * the statistics, it is only intended to allow a new object to be created that contains the
     * same statistics.
     * </p>
     * <p>
     * The {@code LoaderManager} and {@code Loader} classes can be used to wrap a {@code Statistics}
     * instance, populate it initially from the database and deliver incremental changes as new
     * solves are added. It is in this last respect that a problem arises: {@code LoaderManager}
     * will not deliver the same object as it last delivered. The incremental updates are very
     * efficient, but a new {@code Statistics} instance needs to be created for a new delivery to
     * succeed. The quick shallow copy provided by this constructor will work around that issue.
     * </p>
     *
     * @param statisticsToCopy
     *     The statistics to be copied to a new instance.
     */
    // "package" scope for use by "StatisticsLoader".
    Statistics(@NonNull Statistics statisticsToCopy) {
        mMainState = statisticsToCopy.mMainState;

        mAllTimeACs = statisticsToCopy.mAllTimeACs;
        mSessionACs = statisticsToCopy.mSessionACs;

        mAllTimeTimeFreqs = statisticsToCopy.mAllTimeTimeFreqs;
        mSessionTimeFreqs = statisticsToCopy.mSessionTimeFreqs;

        mOneAllTimeAC = statisticsToCopy.mOneAllTimeAC;
        mOneSessionAC = statisticsToCopy.mOneSessionAC;
    }

    /**
     * Gets the main state information for these statistics.
     *
     * @return The main state information.
     */
    @NonNull
    public MainState getMainState() {
        return mMainState;
    }

    /**
     * Creates a new set of statistical averages for the detailed table of all-time and session
     * statistics reported on the statistics/graph tab. Averages of 3, 5, 12, 50, 100 and 1,000 are
     * added for all sessions and for the current session only. The average of 3 permits no DNF
     * solves. The averages of 5 and 12 permit no more than one DNF solves. The averages of 50, 100
     * and 1,000 permit all but one solve to be a DNF solve.
     *
     * @param mainState
     *     The main state information defining the puzzle type, solve category and history/session
     *     selection for these statistics.
     *
     * @return
     *     The detailed set of all-time solve time statistics for the statistics/graph tab.
     */
    static Statistics newAllTimeStatistics(@NonNull MainState mainState) {
        final Statistics stats = new Statistics(mainState);

        // Averages for all sessions.
        stats.addAverageOf(3, true, false);
        stats.addAverageOf(5, true, false);
        stats.addAverageOf(12, true, false);
        stats.addAverageOf(50, false, false);
        stats.addAverageOf(100, false, false);
        stats.addAverageOf(1_000, false, false);

        // Averages for the current session only.
        stats.addAverageOf(3, true, true);
        stats.addAverageOf(5, true, true);
        stats.addAverageOf(12, true, true);
        stats.addAverageOf(50, false, true);
        stats.addAverageOf(100, false, true);
        stats.addAverageOf(1_000, false, true);

        return stats;
    }

    /**
     * Creates a new set of statistical averages for the averages displayed in the graph of the
     * times from the all past and current sessions. Averages of 50 and 100 are added for the
     * <i>current session only</i> (a requirement for the {@link ChartStatistics} constructor, even
     * though the all-time data will be graphed). These averages permit all but one solves to be
     * DNFs.
     *
     * @param mainState
     *     The main state information defining the puzzle type, solve category and history/session
     *     selection for these statistics.
     *
     * @return
     *     The solve time statistics for graphing the all-time averages.
     */
    static Statistics newAllTimeAveragesChartStatistics(@NonNull MainState mainState) {
        final Statistics stats = new Statistics(mainState);

        // Averages for the current session only IS NOT A MISTAKE! The "ChartStatistics" class
        // passes all data in for the "current session", but makes a distinction using its own API.
        stats.addAverageOf(50, false, true);
        stats.addAverageOf(100, false, true);

        return stats;
    }

    /**
     * Creates a new set of statistical averages for the averages displayed in the graph of the
     * times from the current session. Averages of 5 and 12 are added for the current session
     * only. These averages permit no more than one DNF solve.
     *
     * @param mainState
     *     The main state information defining the puzzle type, solve category and history/session
     *     selection for these statistics.
     *
     * @return
     *     The solve time statistics for graphing the current session averages.
     */
    static Statistics newCurrentSessionAveragesChartStatistics(@NonNull MainState mainState) {
        final Statistics stats = new Statistics(mainState);

        stats.addAverageOf(5, true, true);
        stats.addAverageOf(12, true, true);

        return stats;
    }

    /**
     * Resets all statistics and averages that have been collected previously. The average-of-N
     * calculators and time frequencies are cleared of data, but they are not destroyed, so the
     * configuration remains the same. The main state remains unchanged.
     */
    public void reset() {
        for (final AverageCalculator allTimeAC : mAllTimeACs.values()) {
            allTimeAC.reset();
        }

        for (final AverageCalculator sessionAC : mSessionACs.values()) {
            sessionAC.reset();
        }

        mAllTimeTimeFreqs.clear();
        mSessionTimeFreqs.clear();
    }

    /**
     * <p>
     * Indicates if all of the solve time averages required are across the current session only. If
     * only times for the current session are required, a more efficient approach may be taken to
     * load the saved solve times.
     * </p>
     * <p>
     * When the main state indicates that the history of all times is selected, it does not
     * necessarily change the result of this method. For statistics, the full history includes all
     * times from all past and current sessions. It is possible for
     * the reporting of this m
     * </p>
     *
     * @return
     *     {@code true} if all required averages apply only to solve times for the current session;
     *     or {@code false} if the averages include at least one average for solve times across all
     *     past and current sessions. If no averages for either set of solve times are required,
     *     the result will be {@code true}.
     */
    public boolean isForCurrentSessionOnly() {
        return mOneAllTimeAC == null;
    }

    /**
     * Gets an array of all distinct values of "N" for which calculators for the "average-of-N"
     * have been added to these statistics. There is no distinction between current-session-only
     * and all-time average calculators; the distinct set of values of "N" across both sets of
     * calculators is returned.
     *
     * @return
     *     The distinction values of "N" for all "average-of-N" calculators in these statistics.
     */
    int[] getNsOfAverages() {
        // NOTE: This is intended only to support the needs of "ChartStatistics", which assumes
        // that the union of all average calculators (created by appropriate factory methods in
        // this "Statistics" class) are exclusively for the current session or exclusively for all
        // sessions, not a mixture of both. "ChartStatistics" just needs to get all values of "N"
        // for which averages are required, so that it can create corresponding objects to store
        // the chart data. This ensures that it is not coupled to the details of the factory
        // methods in this class. An efficient implementation is not of much concern here.
        final Set<Integer> distinctNs = new TreeSet<>();

        distinctNs.addAll(mAllTimeACs.keySet());
        distinctNs.addAll(mSessionACs.keySet());

        final int[] ns = new int[distinctNs.size()];
        int i = 0;

        for (final Integer n : distinctNs) {
            ns[i++] = n;
        }

        return ns;
    }

    /**
     * Creates a new calculator for the "average of <i>n</i>" solve times.
     *
     * @param n
     *     The number of solve times that will be averaged (e.g., 3, 5, 12, ...). Must be greater
     *     than zero. If a calculator for the same value of {@code n} has been added for the same
     *     value of {@code isForCurrentSessionOnly}, the previous calculator will be overwritten.
     * @param disqualifyDNFs
     *     {@code true} if an average is disqualified if there are too many DNFs, or {@code false}
     *     if DNFs should be ignored (mostly). See {@link AverageCalculator#getCurrentAverage()}
     *     for more details on the calculation.
     * @param isForCurrentSessionOnly
     *     {@code true} to collect times only for the current session, or {@code false} to collect
     *     times across all past and current sessions.
     *
     * @throws IllegalArgumentException
     *     If {@code n} is not greater than zero.
     */
    private void addAverageOf(int n, boolean disqualifyDNFs, boolean isForCurrentSessionOnly) {
        final AverageCalculator ac = new AverageCalculator(n, disqualifyDNFs);

        if (isForCurrentSessionOnly) {
            mSessionACs.put(n, ac);
            if (mOneSessionAC == null) {
                mOneSessionAC = ac;
            }
        } else {
            mAllTimeACs.put(n, ac);
            if (mOneAllTimeAC == null) {
                mOneAllTimeAC = ac;
            }
        }
    }

    /**
     * Gets the calculator for the "average of <i>n</i>" solve times.
     *
     * @param n
     *     The number of solve times that were averaged (e.g., 3, 5, 12, ...).
     * @param isForCurrentSessionOnly
     *     {@code true} for the calculator that collected times only for the current session, or
     *     {@code false} to for the calculator that collected times across all past and current
     *     sessions.
     *
     * @return
     *     The requested average calculator, or {@code null} if no such calculator was defined for
     *     these statistics.
     */
    public AverageCalculator getAverageOf(int n, boolean isForCurrentSessionOnly) {
        if (isForCurrentSessionOnly) {
            return mSessionACs.get(n);
        }
        return mAllTimeACs.get(n);
    }

    /**
     * Records a solve time. The time value should be in milliseconds. If the solve is a DNF,
     * call {@link #addDNF} instead.
     *
     * @param time
     *     The solve time in milliseconds. Must be positive (though {@link #TIME_DNF} is also
     *     accepted). The value will be rounded in accordance with WCA Regulations as it is added.
     * @param isForCurrentSession
     *     {@code true} if the solve was added during the current session; or {@code false} if
     *     the solve was added in a previous session.
     *
     * @throws IllegalArgumentException
     *     If the time is not greater than zero and is not {@code TIME_DNF}.
     */
    public void addTime(long time, boolean isForCurrentSession) throws IllegalArgumentException {
        // "time" is validated (and rounded per WCA) on first calling "AverageCalculator.addTime".
        for (final AverageCalculator allTimeAC : mAllTimeACs.values()) {
            allTimeAC.addTime(time);
        }

        if (isForCurrentSession) {
            for (final AverageCalculator sessionAC : mSessionACs.values()) {
                sessionAC.addTime(time);
            }
        }

        // Update the time frequencies. The time value is truncated to the nearest (not greater)
        // whole second and used as the key to a "bucket" that counts times in the same range.
        //
        // Times in the range 0.000 to 0.999 seconds are mapped to the "0 s" bucket, times from
        // "1.000" to "1.999" are mapped to the "1 s" bucket, and so on. This allows the sum of,
        // say, buckets 0 to 9 (inclusive) to represent the number of "sub-10-second" solves. This
        // would not be the case if the times were rounded instead of truncated, as those same
        // buckets would then represent the number of "sub-10-and-a-half-second" solves. The former
        // rolls off the tongue much more easily.
        //
        // The only wrinkle is that the WCA requires that times over ten minutes be rounded to the
        // nearest whole second. Therefore, a time of 600.5 seconds would be rounded to 601 seconds
        // when it is recorded as a "result". It is this official "result" that is put into a
        // "bucket", so the WCA rounding is applied before truncating the time to get the "bucket".
        // (For times under 10 minutes, the WCA truncate it to the nearest (not greater) 1/100s,
        // so that has no effect on the subsequent truncation for the "bucket", as such <10min
        // times never round up to the next second and never round down to the previous second.)
        final long timeForFreq = time == TIME_DNF
                ? TIME_DNF
                : WCAMath.floorToMultiple(WCAMath.roundResult(time), 1_000);

        Integer oldFreq = mAllTimeTimeFreqs.get(timeForFreq);
        mAllTimeTimeFreqs.put(timeForFreq, oldFreq == null ? 1 : oldFreq + 1);

        if (isForCurrentSession) {
            oldFreq = mSessionTimeFreqs.get(timeForFreq);
            mSessionTimeFreqs.put(timeForFreq, oldFreq == null ? 1 : oldFreq + 1);
        }
    }

    /**
     * Records a did-not-finish (DNF) solve, one where no time was recorded.
     *
     * @param isForCurrentSession
     *     {@code true} if the DNF solve was added during the current session; or {@code false} if
     *     the solve was added in a previous session.
     */
    // This methods takes away any confusion about what time value represents a DNF.
    public void addDNF(boolean isForCurrentSession) {
        addTime(TIME_DNF, isForCurrentSession);
    }

    /**
     * Gets the best solve time of all those added to these statistics for a solve in the current
     * session. See {@link #getAllTimeBestTime()} for details on the rounding applied to the time.
     *
     * @return
     *     The best time ever added for the current session. The result will be
     *     {@link #TIME_UNKNOWN} if no times have been added, or if all added times were DNFs.
     */
    public long getSessionBestTime() {
        return mOneSessionAC != null ? mOneSessionAC.getBestTime() : TIME_UNKNOWN;
    }

    /**
     * Gets the worst time (not a DNF) of all those added to these statistics for a solve in the
     * current session. See {@link #getAllTimeBestTime()} for details on the rounding applied to
     * the time.
     *
     * @return
     *     The worst time ever added for the current session. The result will be
     *     {@link #TIME_UNKNOWN} if no times have been added, or if all added times were DNFs.
     */
    public long getSessionWorstTime() {
        return mOneSessionAC != null ? mOneSessionAC.getWorstTime() : TIME_UNKNOWN;
    }

    /**
     * Gets the total number of solve times (including DNFs) that were added to these statistics
     * for the current session. To get the number of non-DNF solves, subtract the result of
     * {@link #getSessionNumDNFSolves()}.
     *
     * @return The number of solve times that were added for the current session.
     */
    public int getSessionNumSolves() {
        return mOneSessionAC != null ? mOneSessionAC.getNumSolves() : 0;
    }

    /**
     * Gets the total number of DNF solves that were added to these statistics for the current
     * session.
     *
     * @return The number of DNF solves that were added for the current session.
     */
    public int getSessionNumDNFSolves() {
        return mOneSessionAC != null ? mOneSessionAC.getNumDNFSolves() : 0;
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
     * statistics for the current session. The result in rounded in the same manner as for
     * {@link #getAllTimeMeanTime()}; see that method for details.
     *
     * @return
     *     The mean time of all non-DNF solves that were added for the current session. The result
     *     will be {@link #TIME_UNKNOWN} if no times have been added, or if all added times were
     *     DNFs.
     */
    public long getSessionMeanTime() {
        return mOneSessionAC != null ? mOneSessionAC.getMeanTime() : TIME_UNKNOWN;
    }

    /**
     * <p>
     * Gets the solve time frequencies for the current sessions. The times are truncated to whole
     * seconds, but still expressed as milliseconds. The keys are the times (and {@link #TIME_DNF}
     * can be a key), and the values are the number of solves times that fell into the one-second
     * interval for that key. For example, if the key is "4", the value is the number of solve
     * times of "four-point-something seconds".
     * </p>
     * <p>
     * Solve times added by {@link #addTime(long, boolean)} are truncated to whole seconds when
     * identifying the appropriate frequency "bucket" to which each time will be assigned. However,
     * before the time is truncated, it is first rounded in accordance with WCA Regulations. This
     * will only affect times over ten minutes, which are first rounded to the nearest second (with
     * half-up rounding) and that whole second value is unchanged by the truncation used to assign
     * it to a frequency "bucket". For example, if the time is 9:59.999 (under ten minutes), it is
     * first truncated (per WCA) to 9:59.99 (nearest, not greater, 1/100 s) and then truncated to
     * 9:59 (599 s) to identify its "bucket". However, if the time is 10:00.500 (over ten minutes),
     * it is first <i>rounded</i> (per WCA) to 10:01 and then truncated to the same 10:01 (601 s)
     * to identify its "bucket". Therefore, the "600-second bucket" contains only times for the
     * <i>half-second</i> interval (with millisecond precision) from 10:00.000 to 10:00.499, while
     * all other "buckets" cover a full one-second interval.
     * </p>
     *
     * @return
     *     The solve time frequencies. The iteration order of the map begins with any DNF solves
     *     and then continues in increasing order of time value. This may be modified freely.
     */
    public Map<Long, Integer> getSessionTimeFrequencies() {
        return new TreeMap<>(mSessionTimeFreqs);
    }

    /**
     * Gets the best solve time of all those added to these statistics for a solve in all past
     * and current sessions. The time is rounded in accordance with WCA Regulations for "results".
     * See the method and class descriptions of {@link WCAMath#roundResult(long)} for more details
     * on the rounding performed.
     *
     * @return
     *     The best time ever added for all past and current sessions. The result will be
     *     {@link #TIME_UNKNOWN} if no times have been added, or if all added times were DNFs.
     */
    public long getAllTimeBestTime() {
        // "AverageCalculator.getBestTime()" performs the rounding.
        return mOneAllTimeAC != null ? mOneAllTimeAC.getBestTime() : TIME_UNKNOWN;
    }

    /**
     * Gets the worst time (not a DNF) of all those added to these statistics for a solve in all
     * past and current sessions. See {@link #getAllTimeBestTime()} for details on the rounding
     * applied to the time.
     *
     * @return
     *     The worst time ever added for all past and current sessions. The result will be
     *     {@link #TIME_UNKNOWN} if no times have been added, or if all added times were DNFs.
     */
    public long getAllTimeWorstTime() {
        // "AverageCalculator.getWorstTime()" performs the rounding.
        return mOneAllTimeAC != null ? mOneAllTimeAC.getWorstTime() : TIME_UNKNOWN;
    }

    /**
     * Gets the total number of solve times (including DNFs) that were added to these statistics
     * for all past and current sessions. To get the number of non-DNF solves, subtract the result
     * of {@link #getAllTimeNumDNFSolves()}.
     *
     * @return The number of solve times that were added for all past and current sessions.
     */
    public int getAllTimeNumSolves() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getNumSolves() : 0;
    }

    /**
     * Gets the total number of DNF solves that were added to these statistics for all past and
     * current sessions.
     *
     * @return The number of DNF solves that were added for all past and current sessions.
     */
    public int getAllTimeNumDNFSolves() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getNumDNFSolves() : 0;
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
     * statistics for all past and current sessions. The returned millisecond value is rounded in
     * accordance with WCA Regulations for "averages"/"means". See the method and class descriptions
     * of {@link WCAMath#roundAverage(long)} for more details on the rounding performed.
     *
     * @return
     *     The mean time of all non-DNF solves that were added for all past and current sessions.
     *     The result will be {@link #TIME_UNKNOWN} if no times have been added, or if all added
     *     times were DNFs.
     */
    public long getAllTimeMeanTime() {
        // "AverageCalculator.getMeanTime()" will perform appropriate WCA rounding.
        return mOneAllTimeAC != null ? mOneAllTimeAC.getMeanTime() : TIME_UNKNOWN;
    }

    /**
     * Gets the solve time frequencies for all past and current sessions. The times are truncated
     * to whole seconds, but still expressed as milliseconds. The keys are the times (and
     * {@link #TIME_DNF} can be a key), and the values are the number of solves times that fell
     * into the one-second interval for that key. For example, if the key is "4", the value is the
     * number of solve times of "four-point-something seconds". See the method description of
     * {@link #getSessionTimeFrequencies()} for more details on the manner in which times are
     * rounded and/or truncated before being assigned to frequency "buckets" for counting.
     *
     * @return
     *     The solve time frequencies. The iteration order of the map begins with any DNF solves
     *     and then continues in increasing order of time value. This may be modified freely.
     */
    public Map<Long, Integer> getAllTimeTimeFrequencies() {
        return new TreeMap<>(mAllTimeTimeFreqs);
    }
}
