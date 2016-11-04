package com.aricneto.twistytimer.stats;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.TTIntent;

import static com.aricneto.twistytimer.utils.TTIntent.*;

/**
 * <p>
 * A loader used to populate a {@link Statistics} object from the database for use in the timer
 * and timer graph (statistics table) fragments. The main timer fragment will manage the listener
 * and notify its subordinate fragments of any updates.
 * </p>
 * <p>
 * This is a <i>passive</i> loader. When first created, it registers a local broadcast listener and
 * then waits until it receives a broadcast intent instructing it to load the solve time data to
 * prepare the statistics. The loader will then begin to load the data on a background thread and
 * will deliver the result to the component via the {@code onLoadFinished} call-back.
 * </p>
 * <p>
 * This loader accepts the following broadcast intent actions where the intent category must be
 * {@link TTIntent#CATEGORY_TIME_DATA_CHANGES}. All intents must carry an intent extra that
 * identifies the current {@link MainState}.
 * </p>
 * <ul>
 *     <li>{@link TTIntent#ACTION_BOOT_STATISTICS_LOADER}: Causes the loader to load the data and
 *         prepare the statistics for the solve times selected by the main state, if it is not
 *         already loaded. This is useful just after starting the loader, as no other broadcast
 *         intent may be received until the user makes some change through the user interface.</li>
 *     <li>{@link TTIntent#ACTION_MAIN_STATE_CHANGED}: The same effect as booting the loader.
 *         If the loader is already booted, and the main state of the loaded data does not match
 *         that of the main state on this intent, then the data is re-loaded for the new main
 *         state.</li>
 *     <li>{@link TTIntent#ACTION_TIME_ADDED}: Must include an intent extra identifying the
 *         {@link Solve} that was added (in addition to the main state extra). If the solve time
 *         was added for the same main state as already loaded, the statistics will be updated
 *         with the new time without requiring a new database read. If the time is added manually
 *         and is not added as the latest time in the current session, the broadcast action should
 *         be {@code ACTION_TIMES_MODIFIED} to cause a full re-load.</li>
 *     <li>{@link TTIntent#ACTION_TIMES_MODIFIED}: Re-loads all of the data for the main state
 *         identified in the intent, even if the main state has not changed.</li>
 *     <li>{@link TTIntent#ACTION_TIMES_MOVED_TO_HISTORY}: Re-loads all of the data for the main
 *         state identified in the intent, even if the main state has not changed.</li>
 * </ul>
 * <p>
 * The {@link TTIntent} class provides convenient methods for constructing and broadcasting some
 * of these intents.
 * </p>
 *
 * @author damo
 */
public class StatisticsLoader extends AsyncTaskLoader<Statistics> {
    // NOTE: See the "IMPLEMENTATION NOTE" in "ScrambleLoader"; the same approach is used here.

    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG = StatisticsLoader.class.getSimpleName();

    /**
     * The cached statistics that have been loaded previously. This reference will be reset to
     * {@code null} if the data changes and the data will need to be loaded again when requested.
     */
    private Statistics mStatistics;

    /**
     * The broadcast receiver that is notified of changes to the solve time data.
     */
    private TTIntent.TTCategoryBroadcastReceiver mTimeDataChangedReceiver;

    /**
     * A broadcast receiver that is notified of changes to the solve time data.
     */
    private static class TimeDataChangedReceiver extends TTIntent.TTCategoryBroadcastReceiver {
        /**
         * The loader to be notified of changes to the solve time data.
         */
        private final StatisticsLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader of changes to the solve
         * time data.
         *
         * @param loader The loader to be notified of changes to the solve times.
         */
        TimeDataChangedReceiver(StatisticsLoader loader) {
            super(TTIntent.CATEGORY_TIME_DATA_CHANGES);
            mLoader = loader;
        }

        /**
         * Receives notification of a change to the solve time data and notifies the loader if the
         * change is pertinent and a new load is required.
         *
         * @param context The context of the receiver.
         * @param intent  The intent detailing the action that has occurred.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceive: " + intent);

            // All of these intents need to hold the MainState extra, or they cannot be processed.
            // The main state is not necessarily "new"; it can be the same as the last-notified
            // state. This allows a complete change of the selected data without the need to
            // re-create or restart the loader.
            final MainState newMainState = TTIntent.getMainState(intent);

            if (newMainState == null) {
                throw new IllegalStateException("Intent must include state information: " + intent);
            }

            switch (intent.getAction()) {
                case ACTION_BOOT_STATISTICS_LOADER:
                    // Shortly after starting, there may be no changes to the solve time data and
                    // no changes to the main state, but something has to get the ball rolling.
                    // This "bootstrap" intent can be ignored if data is already loaded for the
                    // same main state. This is no different in its handling than a change to the
                    // main state, but it is targeted specifically at this loader.
                    if (mLoader.resetForNewMainState(newMainState)) {
                        if (DEBUG_ME) Log.d(TAG, "  Boot request received. Initial load now!");
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_MAIN_STATE_CHANGED:
                    // The main state was changed. If only the history flag was changed, then the
                    // statistics are already up-to-date as that has no effect: statistics are
                    // always loaded for all times from past and current sessions.
                    if (mLoader.resetForNewMainState(newMainState)) {
                        if (DEBUG_ME) Log.d(TAG, "  Main state changed. Will reload!");
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_TIME_ADDED:
                    final Solve newSolve = TTIntent.getSolve(intent);

                    if (newSolve != null) {
                        mLoader.updateForNewSolve(newMainState, newSolve);
                    } else {
                        throw new IllegalStateException("Intent must include the Solve: " + intent);
                    }
                    break;

                case ACTION_TIMES_MODIFIED:
                case ACTION_TIMES_MOVED_TO_HISTORY:
                    if (DEBUG_ME) Log.d(TAG, "  Unknown changes or history toggle. Will reload!");
                    // If times were moved to the history or other unspecified modifications were
                    // made (e.g., deletions, changes to penalties, etc.), then "mStatistics"
                    // cannot be simply updated. A full re-load will be needed. First ensure that
                    // "mStatistics" is created and/or is configured correctly, then reload.
                    mLoader.resetForNewMainState(newMainState);
                    mLoader.onContentChanged();
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
     * Resets the statistics if they have been loaded, but are not compatible with the given main
     * state. This will <i>not</i> trigger an automatic reload from the database; that is the
     * responsibility of the caller.
     *
     * @param newMainState
     *     The new main state for which statistics are required.
     *
     * @return
     *     {@code true} if the current statistics were not compatible with the given main state
     *     (or if the statistics were {@code null}) and new statistics were created and need to
     *     be populated from the database; or {@code false} if the current statistics were already
     *     loaded and are compatible with the given main state.
     */
    private boolean resetForNewMainState(@NonNull MainState newMainState) {
        // The "isHistoryEnabled" state is not relevant, as all solve times are always loaded.
        if (mStatistics == null || !mStatistics.getMainState().equalsIgnoreHistory(newMainState)) {
            mStatistics = Statistics.newAllTimeStatistics(newMainState);
            return true;
        }

        // "mStatistics" is not null and its main state matches the given state, so it is OK.
        return false;
    }

    /**
     * Attempts a quick update of the statistics without resorting to a full read of the database.
     * If the statistics were previously read from the database, then a single new solve time,
     * added for the current session, can be added directly to the statistics and the update will
     * be delivered to the activity or fragment. If the new solve cannot be updated, then a
     * full reload will be performed.
     *
     * @param newSolve
     *     The new solve to be added to the statistics.
     */
    private void updateForNewSolve(@NonNull MainState newMainState, @NonNull Solve newSolve) {
        if (resetForNewMainState(newMainState)) {
            // "mStatistics" was not compatible with the new main state, so a full reload is
            // needed. "newSolve" is ignored; that solve will be loaded from the database.
            if (DEBUG_ME) Log.d(TAG, "  Quick update not possible. Will reload!");
            onContentChanged();
        } else {
            // "mStatistics" is not null and is still compatible with the new main state. If the
            // solve is also compatible, then add it to "mStatistics" and do a quick delivery.
            if (newSolve.getPuzzleType() == newMainState.getPuzzleType()
                && newSolve.getCategory().equals(newMainState.getSolveCategory())) {

                if (newSolve.getPenalties().hasDNF()) {
                    mStatistics.addDNF(true);
                } else {
                    mStatistics.addTime(newSolve.getExactTime(), true);
                }

                // Need deliver a different "Statistics" instance from the last delivery, otherwise
                // the "LoaderManager" treats it as already delivered and will not deliver it
                // again. The copy-constructor will do a quick, cheap shallow copy. There is no
                // need to update the field with the copy.
                if (DEBUG_ME) Log.d(TAG, "  Delivering quick update to statistics!");
                deliverResult(new Statistics(mStatistics)); // via "onLoadFinished()"
            } else {
                // This is weird. Log the problem and just do a reload.
                Log.w(TAG, "Mismatch between main state and solve. BUG? Reloading all data....");
                onContentChanged();
            }
        }
    }

    /**
     * Starts loading the statistics from the database if a request has been received to do so. If
     * not request has been received, then the statistics will not be loaded.
     */
    @Override
    protected void onStartLoading() {
        if (DEBUG_ME) Log.d(TAG, "onStartLoading()");

        // If not already listening for changes to the time data, start listening now.
        if (mTimeDataChangedReceiver == null) {
            if (DEBUG_ME) Log.d(TAG, "  Monitoring changes affecting Statistics.");
            mTimeDataChangedReceiver = new TimeDataChangedReceiver(this);
            // Register here and unregister in "onReset()".
            TTIntent.registerReceiver(mTimeDataChangedReceiver);
        }

        // If a change has been notified, then load the statistics from the database. However, if
        // no change has been notified, then the first call to "onStartLoading" will *not* cause
        // a load to be forced. The loader will just sit passively and await the receipt of a
        // broadcast intent.
        if (takeContentChanged()) {
            if (mStatistics != null) {
                if (DEBUG_ME) Log.d(TAG, "  forceLoad() called to load statistics...");
                forceLoad();
            } else {
                // This is not expected to happen!
                Log.e(TAG, "Expected Statistics to be prepared before loading!");
                commitContentChanged(); // Swallow this change request.
            }
        }
    }

    /**
     * Resets the loaded statistics and stops monitoring requests for updates or notifications of
     * other changes.
     */
    @Override
    protected void onReset() {
        if (DEBUG_ME) Log.d(TAG, "onReset()");

        super.onReset();

        mStatistics = null; // Forget everything.

        if (mTimeDataChangedReceiver != null) {
            if (DEBUG_ME) Log.d(TAG, "  NOT monitoring changes affecting Statistics.");
            // Receiver will be re-registered in "onStartLoading", if that is called again.
            TTIntent.unregisterReceiver(mTimeDataChangedReceiver);
            mTimeDataChangedReceiver = null;
        }
    }

    /**
     * Loads the statistics from the database on a background thread.
     *
     * @return The loaded statistics.
     */
    @Override
    public Statistics loadInBackground() {
        if (DEBUG_ME) Log.d(TAG, "loadInBackground(): Loading all Statistics....");

        // Take a copy of the field in case it is changed on another thread in mid load.
        final Statistics statsToLoad = mStatistics;

        if (statsToLoad != null) {
            // Because "DEBUG_ME" is either always "true" or always "false"....
            @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
            final long startTime = DEBUG_ME ? SystemClock.elapsedRealtime() : 0L;

            // This is a full, clean load, so clear out any results from the previous load.
            // TODO: Add support for cancellation: add a call-back to "populateStatistics", so it
            // can poll for cancellation while it iterates over the solves records.
            statsToLoad.reset();
            TwistyTimer.getDBHandler().populateStatistics(statsToLoad);

            if (DEBUG_ME) Log.d(TAG, String.format("  Loaded Statistics in %,d ms.",
                    SystemClock.elapsedRealtime() - startTime));

            // If this is not the first time loading the data, a different object must be returned
            // if the "LoaderManager" is to trigger "onLoadFinished" (go figure). As "mStatistics"
            // is still the same object, create a new copy instead to trick "LoaderManager" into
            // doing what is expected.
            mStatistics = statsToLoad; // Re-sync, in case field changed on another thread.
            return new Statistics(statsToLoad); // No need to update field to the new instance.
        }

        // "mStatistics" is only set to "null" in "onReset()" and it is not expected that this
        // "loadInBackground" method would then be called. Perhaps the was an initial load when
        // the loader was started and before any request arrived. Very suspicious. Warn about it.
        Log.w(TAG, "  Unable to load into null Statistics!");

        return null;
    }
}
