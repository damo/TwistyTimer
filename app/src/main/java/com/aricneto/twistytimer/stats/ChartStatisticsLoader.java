package com.aricneto.twistytimer.stats;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.TTIntent;

import static com.aricneto.twistytimer.utils.TTIntent.*;

/**
 * <p>
 * A loader used to populate a {@link ChartStatistics} object from the database
 * for use in the main chart in the {@code TimerGraphFragment}.
 * </p>
 * <p>
 * This is a <i>passive</i> loader. When first created, it registers a local
 * broadcast listener and then waits until it receives a broadcast intent
 * instructing it to load the solve time data to prepare the chart statistics.
 * The loader will then begin to load the data on a background thread and will
 * deliver the result to the component via the {@code onLoadFinished} call-back.
 * </p>
 * <p>
 * This loader accepts the following broadcast intent actions where the intent
 * category must be {@link TTIntent#CATEGORY_SOLVE_DATA_CHANGES}. The intents
 * must carry the intent extras documented in the description of each intent
 * action constant in the {@link TTIntent} class.
 * </p>
 * <ul>
 *     <li>{@link TTIntent#ACTION_BOOT_CHART_STATISTICS_LOADER}: Causes the
 *         loader to load the data and prepare the chart statistics for the
 *         solve times selected by the main state, if it is not already loaded.
 *         This is useful just after starting the loader, as no other broadcast
 *         intent may be received until the user makes some change through the
 *         user interface.</li>
 *     <li>{@link TTIntent#ACTION_MAIN_STATE_CHANGED}: The same effect as
 *         booting the loader. If the loader is already booted, and the main
 *         state of the loaded data does not match that of the main state on
 *         this intent, then the data is re-loaded for the new main state.</li>
 *     <li>{@link TTIntent#ACTION_ONE_SOLVE_ADDED}: The intent includes an extra
 *         identifying the {@link Solve} that was added. If the "history" flag
 *         is raised on the flag and the chart statistics are loaded only for
 *         the current session, then the solve does not affect the chart
 *         statistics and will be ignored. If the solve time was added for the
 *         same main state as already loaded and follows the chronological order
 *         of the loaded solves, the chart statistics will be updated with the
 *         new solve time without requiring a new database read. If the solve
 *         matches the loaded main state but is not in chronological order, a
 *         full re-load will be performed. If the chart statistics are not yet
 *         loaded, or if the solve does not match the main state already loaded,
 *         the solve will be ignored. However, if the chart statistics are not
 *         yet loaded, then an attempt to add a new solve is probably a
 *         programming error (the loader should be booted first), so a warning
 *         will be logged.</li>
 *     <li>{@link TTIntent#ACTION_ONE_SOLVE_UPDATED}: Re-loads the chart
 *         statistics for the same main state that is already loaded. Properties
 *         of the solve may have changed in ways that make it unsafe to assume
 *         that it can simply be appended to the loaded chart statistics, so a
 *         full re-load is necessary. If no chart statistics are loaded, a
 *         warning will be logged, as the loader is expected to have been booted
 *         before solves are updated.</li>
 *     <li>{@link TTIntent#ACTION_ONE_SOLVE_DELETED}: As for
 *         {@code ACTION_ONE_SOLVE_UPDATED}, except that if the deleted solve
 *         does not match the main state of the loaded chart statistics, no
 *         action will be taken.</li>
 *     <li>{@link TTIntent#ACTION_MANY_SOLVES_DELETED}: As for
 *         {@code ACTION_ONE_SOLVE_UPDATED}, except that if the puzzle type and
 *         solve category are given in the intent (they are optional) and either
 *         does not match the main state of the loaded chart statistics, no
 *         action will be taken.</li>
 *     <li>{@link TTIntent#ACTION_MANY_SOLVES_ADDED}: As for
 *         {@code ACTION_MANY_SOLVES_DELETED}.</li>
 *     <li>{@link TTIntent#ACTION_SOLVES_MOVED_TO_HISTORY}: As for
 *         {@code ACTION_MANY_SOLVES_DELETED}, except that the puzzle type and
 *         solve category extras are <i>not</i> optional for this action.
 *         However, if the chart statistics are loaded for the full history of
 *         all times, then the data will not be re-loaded. The chart for the
 *         full history of all times includes the times for the current session,
 *         so moving those times to the history has no effect on the validity
 *         of the chart.</li>
 * </ul>
 * <p>
 * All other intent actions are ignored. The {@link TTIntent} class provides
 * convenient methods for constructing and broadcasting the above intents.
 * </p>
 *
 * @author damo
 */
public class ChartStatisticsLoader extends AsyncTaskLoader<ChartStatistics> {
    // NOTE: See the "IMPLEMENTATION NOTE" in "ScrambleLoader"; the same
    // "passive" approach is used here.

    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG
        = ChartStatisticsLoader.class.getSimpleName();

    /**
     * The chart style information that will be used to set the labels, colors
     * and other styles on the data sets created to present the chart
     * statistics.
     */
    private final ChartStyle mChartStyle;

    /**
     * The cached chart statistics that have been loaded previously. This
     * reference will be reset to {@code null} if the data changes and the data
     * will need to be loaded again when requested.
     */
    private ChartStatistics mChartStats;

    /**
     * The broadcast receiver that is notified of changes to the solve time
     * data.
     */
    private TTIntent.TTCategoryBroadcastReceiver mSolveDataChangedReceiver;

    /**
     * A broadcast receiver that is notified of changes to the solve time data.
     */
    private static class SolveDataChangedReceiver
                   extends TTIntent.TTCategoryBroadcastReceiver {
        /**
         * The loader to be notified of changes to the solve time data.
         */
        private final ChartStatisticsLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader of changes
         * to the solve time data.
         *
         * @param loader
         *     The loader to be notified of changes to the solve times.
         */
        SolveDataChangedReceiver(ChartStatisticsLoader loader) {
            super(TTIntent.CATEGORY_SOLVE_DATA_CHANGES);
            mLoader = loader;
        }

        /**
         * Receives notification of a change to the solve time data and notifies
         * the loader if the change is pertinent and a new load is required.
         *
         * @param context The context of the receiver.
         * @param intent  The intent detailing the action that has occurred.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceive(): " + intent);

            // Was probably validated when broadcast, but there is no harm in
            // checking it here, just in case something slipped through.
            TTIntent.validate(intent);

            final MainState newMainState = TTIntent.getMainState(intent);
            final Solve solve = TTIntent.getSolve(intent);
            final PuzzleType puzzleType = TTIntent.getPuzzleType(intent);
            final String solveCategory = TTIntent.getSolveCategory(intent);

            switch (intent.getAction()) {
                case ACTION_BOOT_CHART_STATISTICS_LOADER:
                case ACTION_MAIN_STATE_CHANGED:
                    // Shortly after starting, there may be no changes to the
                    // solve time data and no changes to the main state, but
                    // something has to get the ball rolling. The "bootstrap"
                    // intent can be ignored if data is already loaded for the
                    // same main state. Booting is handled in the same way as a
                    // change to the main state, but the boot action is targeted
                    // specifically at this type of loader.
                    //noinspection ConstantConditions (validated above)
                    if (mLoader.resetForNewMainState(newMainState)) {
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_ONE_SOLVE_ADDED:
                    if (mLoader.isStatisticsLoadedAsExpected()
                            && solve != null) {
                        mLoader.updateForNewSolve(solve);
                    }
                    break;

                case ACTION_MANY_SOLVES_ADDED:
                case ACTION_MANY_SOLVES_DELETED:
                    // Puzzle type and category are optional for these actions.
                    // The "history" flag is not specified on the intent, so it
                    // must be assumed that the added/deleted solves affect the
                    // loaded chart statistics, whether or not they are loaded
                    // only for the current session. Of course, if the puzzle
                    // type and category are not a match, then nothing needs to
                    // be done.
                    //
                    // NOTE: While it would be possible to add support for a
                    // "history" flag to the intent extras, there are no common
                    // use cases where it would make much difference. It would
                    // also add the complication of a three-state flag: true,
                    // false, and not specified, so it is not worth optimising.
                    if (mLoader.isStatisticsLoadedAsExpected()
                            && mLoader.matchesStatistics(
                                   puzzleType, solveCategory)) {
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_SOLVES_MOVED_TO_HISTORY:
                    // Puzzle type and category are mandatory for this action.
                    // The "history" flag is not specified on the intent. Only
                    // solves in the current session can be moved to the
                    // history. If only the current session is charted, the
                    // statistics are affected by the move if the type/category
                    // match. If all past and current sessions are charted, the
                    // chart statistics are not affected.
                    if (mLoader.isStatisticsLoadedAsExpected()
                            && mLoader.isCurrentSessionOnly()
                            && mLoader.matchesStatistics(
                                   puzzleType, solveCategory)) {
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_ONE_SOLVE_DELETED:
                    // If a solve is deleted, it can be ignored if it does not
                    // match the main state of the loaded chart statistics.
                    // If only solves for the current session are being charted
                    // and the deleted solve is from a past session, then no
                    // action needs to be taken.
                    if (mLoader.isStatisticsLoadedAsExpected()
                            && solve != null
                            && mLoader.matchesStatistics(
                                 solve.getPuzzleType(), solve.getCategory())
                            && (!mLoader.isCurrentSessionOnly()
                                || !solve.isHistory())) {
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_ONE_SOLVE_UPDATED:
                    // If a solve is updated, the puzzle type and/or category
                    // might have been changed (though AFAIK the UI does not
                    // support that functionality). Even if the type/category do
                    // not match the loaded statistics, a re-load is necessary,
                    // as the value of those properties may have been a match
                    // before the solve was updated.
                    if (mLoader.isStatisticsLoadedAsExpected()) {
                        mLoader.onContentChanged();
                    }
                    break;
            }
        }
    }

    /**
     * Creates a new loader for the solve time chart statistics. The given
     * chart statistics define the set of "average-of-N" calculations to be
     * made. The chart statistics may relate to the current session only, or to
     * all past and current sessions.
     *
     * @param context
     *     The context for this loader. This may be an application context.
     * @param chartStyle
     *     The {@link ChartStyle} defining the styles to be applied to the data
     *     sets in the loaded chart statistics. Must not be {@code null}.
     */
    public ChartStatisticsLoader(@NonNull Context context,
                                 @NonNull ChartStyle chartStyle) {
        super(context);

        // NOTE: "ChartStyle" must be initialised from an "Activity" context,
        // as it needs to access theme attributes. Therefore, it cannot be
        // instantiated here, as the given context may be an "Application"
        // context, which cannot access theme attributes. It is the
        // responsibility of the Activity or Fragment that creates this loader
        // to instantiate the ChartStyle with the necessary context before
        // creating the loader. ChartStyle will not hold a reference to the
        // context, so no memory leaks should occur. However, holding a
        // reference to an Activity context from this Loader would be a really
        // bad idea.
        mChartStyle = chartStyle;
    }

    /**
     * Resets the chart statistics if they have been loaded, but are not
     * compatible with the given main state. This will <i>not</i> trigger an
     * automatic reload from the database; that is the responsibility of the
     * caller. Unlike the non-chart statistics, the chart statistics are
     * sensitive to changes in the value of the main state's "history" flag.
     *
     * @param newMainState
     *     The new main state for which chart statistics are required.
     *
     * @return
     *     {@code true} if the current chart statistics were not compatible
     *     with the given main state (or if the statistics were {@code null})
     *     and new statistics were created and need to be populated from the
     *     database; or {@code false} if the current chart statistics were
     *     already loaded and are compatible with the given main state.
     */
    private boolean resetForNewMainState(@NonNull MainState newMainState) {
        // Compare puzzle type, solve category *AND* "isHistoryEnabled" flag.
        if (mChartStats == null
            || !mChartStats.getMainState().equals(newMainState)) {

            mChartStats = ChartStatistics.newChartStatistics(
                newMainState, mChartStyle);
            return true;
        }

        // "mChartStats" is not null and its main state matches the given
        // state, so it is OK.
        return false;
    }

    /**
     * Attempts a quick update of the chart statistics without resorting to a
     * full read of the database. If the statistics were previously read from
     * the database, then a single new solve time, following in chronological
     * order, can be added directly to the statistics and the update will be
     * delivered to the activity or fragment. If the chart statistics cannot
     * be quickly updated for the new solve, a full reload will be performed.
     * If the puzzle type or category do not match the loaded chart statistics,
     * the new solve is ignored. If only the current session is being charted
     * and the solve has the "history" flag raised, the solve will be ignored.
     * If no chart statistics are currently loaded, an error will occur, so
     * check {@link #isStatisticsLoadedAsExpected()} first.
     *
     * @param solve
     *     The new solve that was created and may need to be added to the
     *     loaded chart statistics.
     */
    private void updateForNewSolve(@NonNull Solve solve) {
        // See comments in "StatisticsLoader.updateForNewSolve" for details.
        if (matchesStatistics(solve.getPuzzleType(), solve.getCategory())
                && (!isCurrentSessionOnly() || !solve.isHistory())) {
            // Same type/category and solve is in the set being charted.
            if (mChartStats.getLatestSolveDate() > solve.getDate()) {
                // Does not follow chronological order: a full reload is needed.
                if (DEBUG_ME) Log.d(TAG,
                    "  Quick update of chart stats not possible. Will reload!");
                onContentChanged();
            } else {
                // This is the latest solve in chronological order. Easy.
                if (solve.getPenalties().hasDNF()) {
                    mChartStats.addDNF(solve.getDate());
                } else {
                    mChartStats.addTime(solve.getExactTime(), solve.getDate());
                }

                if (DEBUG_ME) Log.d(TAG,
                    "  Delivering quick update to chart statistics!");
                // A new instance is needed, otherwise it will not be delivered.
                deliverResult(new ChartStatistics(mChartStats));
            }
        } // else "solve" is irrelevant and is ignored.
    }

    /**
     * Checks that the chart statistics have been loaded as expected. If not
     * already loaded, a warning will be logged. This test is appropriate when
     * the chart statistics should have been loaded previously by a "boot"
     * action before notification of a different action was received. If not
     * already loaded, it is most likely the result of a programming error, so
     * a warning is logged.
     *
     * @return
     *     {@code true} if the chart statistics have been loaded; or
     *     {@code false} if the chart statistics have not yet been loaded (and
     *     a warning has been logged).
     */
    private boolean isStatisticsLoadedAsExpected() {
        if (mChartStats == null) {
            Log.w(TAG, "Expected chart statistics to be loaded already.");
            return false;
        }
        return true;
    }

    /**
     * Indicates if the loaded chart statistics match to the given puzzle type
     * and solve category. The expectation is that a re-load will be necessary
     * unless it is <i>known for certain</i> that the puzzle type and category
     * do not match the loaded chart statistics. Therefore, if the puzzle type
     * and/or category are {@code null}, a match is assumed. The "history" flag
     * is ignored. If the chart statistics are not yet loaded, an error will
     * occur, so first check {@link #isStatisticsLoadedAsExpected()}.
     *
     * @param puzzleType    The puzzle type to be tested.
     * @param solveCategory The solve category to be tested.
     *
     * @return
     *     {@code false} if both the puzzle type solve category are not
     *     {@code null} and they do not match the puzzle type and solve
     *     category reported by the loaded chart statistics; or {@code true}
     *     for all other cases.
     */
    private boolean matchesStatistics(
        @Nullable PuzzleType puzzleType, @Nullable String solveCategory) {
        final MainState ms = mChartStats.getMainState();

        return (puzzleType == null || puzzleType == ms.getPuzzleType())
               && (solveCategory == null
                   || solveCategory.equals(ms.getSolveCategory()));
    }

    /**
     * Indicates if only solves for the current session are charted. If the
     * chart statistics are not yet loaded, an error will occur, so first check
     * {@link #isStatisticsLoadedAsExpected()}.
     *
     * @return
     *     {@code true} if the chart statistics include only solves for the
     *     current session; or {@code false} if it includes solves from all
     *     past and current sessions.
     */
    private boolean isCurrentSessionOnly() {
        return !mChartStats.getMainState().isHistoryEnabled();
    }

    /**
     * Starts loading the statistics from the database. If statistics were
     * previously loaded, they will be re-delivered. If the statistics that
     * were so delivered are out of date, or if no statistics were available
     * for immediate delivery, a new full re-load of the statistics from the
     * database will be triggered and deliver will occur once that background
     * task completes.
     */
    @Override
    protected void onStartLoading() {
        if (DEBUG_ME) Log.d(TAG, "onStartLoading()");

        // If not already listening for changes to the time data, start
        // listening now.
        if (mSolveDataChangedReceiver == null) {
            if (DEBUG_ME) Log.d(TAG,
                "  Monitoring changes affecting ChartStatistics.");
            mSolveDataChangedReceiver = new SolveDataChangedReceiver(this);
            // Register here and unregister in "onReset()".
            TTIntent.registerReceiver(mSolveDataChangedReceiver);
        }

        // If a change has been notified, then load the statistics from the
        // database. However, if no change has been notified, then the first
        // call to "onStartLoading" will *not* cause a load to be forced. The
        // loader will just sit passively and await the receipt of a broadcast
        // intent.
        if (takeContentChanged()) {
            if (mChartStats != null) {
                if (DEBUG_ME) Log.d(TAG,
                    "  forceLoad() called to load chart statistics...");
                forceLoad();
            } else {
                // This is not expected to happen!
                Log.e(TAG,
                    "Expected ChartStatistics to be prepared before loading!");
                commitContentChanged(); // Swallow this change request.
            }
        }
    }

    /**
     * Resets the loaded statistics and stops monitoring requests for updates
     * or notifications of other changes.
     */
    @Override
    protected void onReset() {
        if (DEBUG_ME) Log.d(TAG, "onReset()");

        super.onReset();

        mChartStats = null; // Forget everything.

        if (mSolveDataChangedReceiver != null) {
            if (DEBUG_ME) Log.d(TAG,
                "  NOT monitoring changes affecting ChartStatistics.");
            // Receiver will be re-registered in "onStartLoading", if that is
            // called again.
            TTIntent.unregisterReceiver(mSolveDataChangedReceiver);
            mSolveDataChangedReceiver = null;
        }
    }

    /**
     * Loads the chart statistics from the database on a background thread.
     *
     * @return The loaded chart statistics.
     */
    @Override
    public ChartStatistics loadInBackground() {
        if (DEBUG_ME) Log.d(TAG,
            "loadInBackground(): Loading all ChartStatistics....");

        // Take a copy of the field in case it is changed on another thread in
        // mid load.
        final ChartStatistics statsToLoad = mChartStats;

        if (statsToLoad != null) {
            // Because "DEBUG_ME" is either always "true" or always "false"....
            @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
            final long startTime
                = DEBUG_ME ? SystemClock.elapsedRealtime() : 0L;

            // This is a full, clean load, so clear out any results from the
            // previous load.
            statsToLoad.reset();
            TwistyTimer.getDBHandler().populateChartStatistics(statsToLoad);
            // TODO: Add support for cancellation by adding a call-back to
            // "populateChartStatistics", so it can poll for cancellation as it
            // iterates over the solve records. Not a high priority, though.

            if (DEBUG_ME) Log.d(TAG,
                String.format("  Loaded ChartStatistics in %,d ms.",
                    SystemClock.elapsedRealtime() - startTime));

            // If this is not the first time loading the data, a different
            // object must be returned if the "LoaderManager" is to trigger
            // "onLoadFinished" (go figure). As "mChartStats" is still the
            // same object, create a new copy instead to trick
            // "LoaderManager" into doing what is expected.

            // Re-sync, in case field changed on another thread.
            mChartStats = statsToLoad;

            // No need to update field to the new instance.
            return new ChartStatistics(statsToLoad);
        }

        // "mChartStats" is only set to "null" in "onReset()" and it is not
        // expected that this "loadInBackground" method would then be called.
        // Perhaps the was an initial load when the loader was started and
        // before any request arrived. Very suspicious. Warn about it.
        Log.w(TAG, "  Unable to load into null ChartStatistics!");

        return null;
    }
}
