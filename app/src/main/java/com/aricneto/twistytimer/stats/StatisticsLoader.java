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
 * A loader used to populate a {@link Statistics} object from the database
 * for use in the timer and timer graph (statistics table) fragments. The
 * main timer fragment will manage the listener and notify its subordinate
 * fragments of any updates.
 * </p>
 * <p>
 * This is a <i>passive</i> loader. When first created, it registers a local
 * broadcast listener and then waits until it receives a broadcast intent
 * instructing it to load the solve time data to prepare the statistics. The
 * loader will then begin to load the data on a background thread and will
 * deliver the result to the component via the {@code onLoadFinished} call-back.
 * </p>
 * <p>
 * This loader accepts the following broadcast intent actions where the intent
 * category must be {@link TTIntent#CATEGORY_SOLVE_DATA_CHANGES}. The intents
 * must carry the intent extras documented in the description of each intent
 * action constant in the {@link TTIntent} class.
 * </p>
 * <ul>
 *     <li>{@link TTIntent#ACTION_BOOT_STATISTICS_LOADER}: Causes the loader
 *         to load the data and prepare the statistics for the solve times
 *         selected by the main state, if it is not already loaded. This is
 *         useful just after starting the loader, as no other broadcast intent
 *         may be received until the user makes some change through the user
 *         interface.</li>
 *     <li>{@link TTIntent#ACTION_MAIN_STATE_CHANGED}: The same effect as
 *         booting the loader. If the loader is already booted, and the main
 *         state of the loaded data does not match that of the main state on
 *         this intent, then the data is re-loaded for the new main state. A
 *         change to the "history" flag of the main state does not cause a
 *         re-load of the statistics, as statistics for all past sessions and
 *         the current session are always loaded together.</li>
 *     <li>{@link TTIntent#ACTION_ONE_SOLVE_ADDED}: The intent includes an extra
 *         identifying the {@link Solve} that was added. If the solve time was
 *         added for the same main state as already loaded and follows the
 *         chronological order of the loaded solves, the statistics will be
 *         updated with the new solve time without requiring a new database
 *         read. If the solve matches the loaded main state but is not in
 *         chronological order, a full re-load will be performed. If the
 *         statistics are not yet loaded, or if the solve does not match the
 *         main state already loaded, the solve will be ignored. However, if
 *         the statistics are not yet loaded, then an attempt to add a new
 *         solve is probably a programming error (the loader should be booted
 *         first), so a warning will be logged.</li>
 *     <li>{@link TTIntent#ACTION_ONE_SOLVE_UPDATED}: Re-loads the statistics
 *         for the same main state that is already loaded. Properties of the
 *         solve may have changed in ways that make it unsafe to assume that it
 *         can simply be appended to the loaded statistics, so a full re-load is
 *         necessary. If no statistics are loaded, a warning will be logged, as
 *         the loader is expected to have been booted before solves are
 *         updated.</li>
 *     <li>{@link TTIntent#ACTION_ONE_SOLVE_DELETED}: As for
 *         {@code ACTION_ONE_SOLVE_UPDATED}, except that if the deleted solve
 *         does not match the main state of the loaded statistics, no action
 *         will be taken.</li>
 *     <li>{@link TTIntent#ACTION_MANY_SOLVES_DELETED}: As for
 *         {@code ACTION_ONE_SOLVE_UPDATED}, except that if the puzzle type and
 *         solve category are given in the intent (they are optional) and either
 *         does not match the main state of the loaded statistics, no action
 *         will be taken.</li>
 *     <li>{@link TTIntent#ACTION_MANY_SOLVES_ADDED}: As for
 *         {@code ACTION_MANY_SOLVES_DELETED}.</li>
 *     <li>{@link TTIntent#ACTION_SOLVES_MOVED_TO_HISTORY}: As for
 *         {@code ACTION_MANY_SOLVES_DELETED}, except that the puzzle type and
 *         solve category extras are <i>not</i> optional for this action.</li>
 * </ul>
 * <p>
 * All other intent actions are ignored. The {@link TTIntent} class provides
 * convenient methods for constructing and broadcasting the above intents.
 * </p>
 *
 * @author damo
 */
public class StatisticsLoader extends AsyncTaskLoader<Statistics> {
    // NOTE: See the "IMPLEMENTATION NOTE" in "ScrambleLoader"; the same
    // approach is used here.

    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG = StatisticsLoader.class.getSimpleName();

    /**
     * The cached statistics that have been loaded previously. This reference
     * will be reset to {@code null} if the data changes and the data will need
     * to be loaded again when requested.
     */
    private Statistics mStatistics;

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
        private final StatisticsLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader of
         * changes to the solve time data.
         *
         * @param loader
         *     The loader to be notified of changes to the solve times.
         */
        SolveDataChangedReceiver(StatisticsLoader loader) {
            super(TTIntent.CATEGORY_SOLVE_DATA_CHANGES);
            mLoader = loader;
        }

        /**
         * Receives notification of a change to the solve time data and notifies
         * the loader if the change is pertinent and a new load is required.
         *
         * @param context The context of the receiver.
         * @param intent  The intent detailing the action that has occurred.
         *
         * @throws IllegalArgumentException
         *     If an intent is missing any of the intent extras required for
         *     the specific action set on the intent.
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
                case ACTION_BOOT_STATISTICS_LOADER:
                case ACTION_MAIN_STATE_CHANGED:
                    // Shortly after starting, there may be no changes to the
                    // solve time data and no changes to the main state, but
                    // something has to get the ball rolling. The "bootstrap"
                    // intent can be ignored if data is already loaded for the
                    // same main state. Booting is handled in the same way as a
                    // change to the main state, but the boot action is targeted
                    // specifically at this type of loader.
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
                case ACTION_SOLVES_MOVED_TO_HISTORY:
                    // Puzzle type and category are mandatory for this action.

                    // A full re-load is required if the extras are unknown, or
                    // if they are a match for the loaded statistics.
                    if (mLoader.isStatisticsLoadedAsExpected()
                            && mLoader.matchesStatistics(
                                   puzzleType, solveCategory)) {
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_ONE_SOLVE_DELETED:
                    // If a solve is deleted, it can be ignored if it does not
                    // match the main state of the loaded statistics.
                    if (mLoader.isStatisticsLoadedAsExpected()
                            && solve != null
                            && mLoader.matchesStatistics(
                                 solve.getPuzzleType(), solve.getCategory())) {
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
     * Creates a new loader for the solve time statistics.
     *
     * @param context
     *     The context for this loader. This may be an application context.
     */
    public StatisticsLoader(@NonNull Context context) {
        super(context);
        if (DEBUG_ME) Log.d(TAG, "new StatisticsLoader()");
    }

    /**
     * Resets the statistics if they have been loaded, but are not compatible
     * with the given main state. This will <i>not</i> trigger an automatic
     * reload from the database; that is the responsibility of the caller.
     *
     * @param newMainState
     *     The new main state for which statistics are required.
     *
     * @return
     *     {@code true} if the current statistics were not compatible with
     *     the given main state (or if the statistics were {@code null}) and
     *     new statistics were created and need to be populated from the
     *     database; or {@code false} if the current statistics were already
     *     loaded and are compatible with the given main state.
     */
    private boolean resetForNewMainState(@NonNull MainState newMainState) {
        // The "isHistoryEnabled" state is not relevant, as all solve times
        // are always loaded.
        if (mStatistics == null
            || !mStatistics.getMainState().equalsIgnoreHistory(newMainState)) {

            mStatistics = Statistics.newAllTimeStats(newMainState);
            return true;
        }

        // "mStatistics" is not null and its main state matches the given
        // state, so it is OK.
        return false;
    }

    /**
     * Attempts a quick update of the statistics without resorting to a full
     * read of the database. If the statistics were previously read from the
     * database, then a single new solve time, respecting the chronological
     * order and matching the puzzle type and solve category of the loaded
     * statistics, can be added directly to the statistics and the update will
     * be delivered to the activity or fragment. If the statistics cannot be
     * updated with the new solve because it is not the latest solve in
     * chronological order, a full reload will be performed. If the puzzle
     * type or category do not match the loaded statistics, the new solve is
     * ignored. If no statistics are currently loaded, an error will occur,
     * so check {@link #isStatisticsLoadedAsExpected()} first.
     *
     * @param solve
     *     The new solve that was created and may need to be added to the
     *     loaded statistics.
     */
    private void updateForNewSolve(@NonNull Solve solve) {
        // NOTE: Typically, new solves will be created by the running app and
        // naturally be in chronological order and assigned to the current
        // session. However, that is not necessarily a given. While at present
        // the manually-input solves also follow that pattern, it is better not
        // to assume that will always hold, so check the date-time stamp to
        // avoid any future bugs if these expectations change.

        // The manual input of solves could support the setting of the puzzle
        // type and/or solve category. (It does not support this at the time
        // of writing; it just uses the app's currently-set values of those
        // properties.) If the main state and the solve type/category do not
        // match the main state of the loaded statistics, then there is no
        // need to update the statistics, as the new solve is not relevant.
        if (matchesStatistics(
                solve.getPuzzleType(), solve.getCategory())) {
            // Solve is relevant to these statistics.
            if (mStatistics.getLatestSolveDate() > solve.getDate()) {
                // Does not follow chronological order: a full reload is needed.
                if (DEBUG_ME) Log.d(TAG,
                    "  Quick update of stats not possible. Will reload!");
                onContentChanged();
            } else {
                // The solve has the same type/category and it is the latest
                // solve in the chronological order.
                if (solve.getPenalties().hasDNF()) {
                    mStatistics.addDNF(solve.getDate(), solve.isHistory());
                } else {
                    mStatistics.addTime(solve.getExactTime(), solve.getDate(),
                                        solve .isHistory());
                }

                // Need deliver a different "Statistics" instance from the last
                // delivery, otherwise the "LoaderManager" treats it as already
                // delivered and will not deliver it again. The copy-constructor
                // will do a quick, cheap shallow copy. There is no need to
                // update the field with the copy.
                if (DEBUG_ME) Log.d(TAG, "  Delivering quick update to stats!");
                deliverResult(new Statistics(mStatistics)); // -> onLoadFinished
            }
        } // else the solve is irrelevant and is ignored.
    }
    /**
     * Checks that the statistics have been loaded as expected. If not already
     * loaded, a warning will be logged. This test is appropriate when the
     * statistics should have been loaded previously by a "boot" action before
     * notification of a different action was received. If not already loaded,
     * it is most likely the result of a programming error, so a warning is
     * logged.
     *
     * @return
     *     {@code true} if the statistics have been loaded; or {@code false}
     *     if the statistics have not yet been loaded (and a warning has been
     *     logged).
     */
    private boolean isStatisticsLoadedAsExpected() {
        if (mStatistics == null) {
            Log.w(TAG, "Expected statistics to be loaded already.");
            return false;
        }
        return true;
    }

    /**
     * Indicates if the loaded statistics match the given puzzle type and solve
     * category. The expectation is that a re-load will be necessary unless it
     * is <i>known for certain</i> that the puzzle type and category do not
     * match the loaded statistics. Therefore, if the puzzle type and/or
     * category are {@code null}, a match is assumed. If the statistics are
     * not yet loaded, an error will occur, so first check
     * {@link #isStatisticsLoadedAsExpected()}.
     *
     * @param puzzleType    The puzzle type to be tested.
     * @param solveCategory The solve category to be tested.
     *
     * @return
     *     {@code false} if both the puzzle type solve category are not
     *     {@code null} and they do not match the puzzle type and solve
     *     category reported by the loaded statistics; or {@code true} for
     *     all other cases.
     */
    private boolean matchesStatistics(
            @Nullable PuzzleType puzzleType, @Nullable String solveCategory) {
        final MainState ms = mStatistics.getMainState();

        return (puzzleType == null || puzzleType == ms.getPuzzleType())
               && (solveCategory == null
                   || solveCategory.equals(ms.getSolveCategory()));
    }

    /**
     * Starts loading the statistics from the database if a request has been
     * received to do so. If not request has been received, then the statistics
     * will not be loaded.
     */
    @Override
    protected void onStartLoading() {
        if (DEBUG_ME) Log.d(TAG, "onStartLoading()");

        // If not already listening for changes to the time data, start
        // listening now.
        if (mSolveDataChangedReceiver == null) {
            if (DEBUG_ME) Log.d(TAG,
                "  Monitoring changes affecting Statistics.");
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
            if (mStatistics != null) {
                if (DEBUG_ME) Log.d(TAG,
                    "  forceLoad() called to load statistics...");
                forceLoad();
            } else {
                // This is not expected to happen!
                Log.e(TAG,
                    "Expected Statistics to be prepared before loading!");
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

        mStatistics = null; // Forget everything.

        if (mSolveDataChangedReceiver != null) {
            if (DEBUG_ME) Log.d(TAG,
                "  NOT monitoring changes affecting Statistics.");
            // Receiver will be re-registered in "onStartLoading", if that is
            // called again.
            TTIntent.unregisterReceiver(mSolveDataChangedReceiver);
            mSolveDataChangedReceiver = null;
        }
    }

    /**
     * Loads the statistics from the database on a background thread.
     *
     * @return The loaded statistics.
     */
    @Override
    public Statistics loadInBackground() {
        if (DEBUG_ME) Log.d(TAG,
            "loadInBackground(): Loading all Statistics....");

        // Take a copy of the field in case it is changed on another thread in
        // mid load.
        final Statistics statsToLoad = mStatistics;

        if (statsToLoad != null) {
            // Because "DEBUG_ME" is either always "true" or always "false"....
            @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
            final long startTime
                = DEBUG_ME ? SystemClock.elapsedRealtime() : 0L;

            // This is a full, clean load, so clear out any results from the
            // previous load.
            // TODO: Add support for cancellation: add a call-back to
            // "populateStatistics", so it can poll for cancellation while it
            // iterates over the solves records.
            statsToLoad.reset();
            TwistyTimer.getDBHandler().populateStatistics(statsToLoad);

            if (DEBUG_ME) Log.d(TAG,
                String.format("  Loaded Statistics in %,d ms.",
                    SystemClock.elapsedRealtime() - startTime));

            // If this is not the first time loading the data, a different
            // object must be returned if the "LoaderManager" is to trigger
            // "onLoadFinished" (go figure). As "mStatistics" is still the same
            // object, create a new copy instead to trick "LoaderManager" into
            // doing what is expected.

            // Re-sync, in case field changed on another thread.
            mStatistics = statsToLoad;
            // No need to update field to the new instance.
            return new Statistics(statsToLoad);
        }

        // "mStatistics" is only set to "null" in "onReset()" and it is not
        // expected that this "loadInBackground" method would then be called.
        // Perhaps the was an initial load when the loader was started and
        // before any request arrived. Very suspicious. Warn about it.
        Log.w(TAG, "  Unable to load into null Statistics!");

        return null;
    }
}
