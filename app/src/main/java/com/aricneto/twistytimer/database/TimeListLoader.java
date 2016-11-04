package com.aricneto.twistytimer.database;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.TTIntent;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_BOOT_CHART_STATISTICS_LOADER;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_MAIN_STATE_CHANGED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MOVED_TO_HISTORY;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED;

/**
 * A loader that reads solve times from the database to support the {@code TimerListFragment}.
 */
public class TimeListLoader extends CursorLoader {
    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG = TimeListLoader.class.getSimpleName();

    /**
     * The main state information corresponding to the data that was last loaded.
     */
    @Nullable
    private MainState mMainState;

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
        private final TimeListLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader of changes to the solve
         * time data.
         *
         * @param loader The loader to be notified of changes to the solve times.
         */
        TimeDataChangedReceiver(TimeListLoader loader) {
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
/*
            // All of these intents need to hold the MainState extra, or they cannot be processed.
            // The main state is not necessarily "new"; it can be the same as the last-notified
            // state. This allows a complete change of the selected data without the need to
            // re-create or restart the loader.
            final MainState newMainState = TTIntent.getMainState(intent);

            if (newMainState == null) {
                throw new IllegalStateException("Intent must include state information: " + intent);
            }

            switch (intent.getAction()) {
                case ACTION_BOOT_CHART_STATISTICS_LOADER:
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
                    // The main state was changed. Check if the change was relevant and ensure that
                    // "mChartStats" is created and/or is configured correctly.
                    if (mLoader.resetForNewMainState(newMainState)) {
                        if (DEBUG_ME) Log.d(TAG, "  Main state changed. Will reload!");
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_TIME_ADDED:
                    // The "all times" chart includes times from the current session, so an update
                    // is always required when a new time is added to the session.
                    final Solve newSolve = TTIntent.getSolve(intent);

                    if (newSolve != null) {
                        mLoader.updateForNewSolve(newMainState, newSolve);
                    } else {
                        throw new IllegalStateException("Intent must include the Solve: " + intent);
                    }
                    break;

                case ACTION_TIMES_MOVED_TO_HISTORY:
                    // If session times were moved to the history, AND THE FULL HISTORY IS BEING
                    // SHOWN, then the chart is already up-to-date, as the charted full history of
                    // times already includes the times from the current session. If the chart is
                    // showing only the current session, then a full re-load is required. (There
                    // is no check if the current session is already empty, as that would just be
                    // an over-complication and the database re-load will be very quick, anyway.)
                    //
                    // First, make sure the main state on the chart statistics is in sync with the
                    // latest main state that arrived with the intent. Then check the history flag.
                    if (mLoader.resetForNewMainState(newMainState)
                            || !newMainState.isHistoryEnabled()) {
                        // The chart was not in sync with the given main state, or no data had been
                        // loaded before, or session times are being shown, so update everything.
                        if (DEBUG_ME) Log.d(TAG, "  Session moved to history. Will reload!");
                        mLoader.onContentChanged();
                    } // else the chart is already up to date.
                    break;

                case ACTION_TIMES_MODIFIED:
                    if (DEBUG_ME) Log.d(TAG, "  Unknown changes. Will reload!");
                    // If other unspecified modifications were made (e.g., deletions, changes to
                    // penalties, etc.), a full re-load will be needed. First ensure that
                    // "mChartStats" is created and/or is configured correctly.
                    mLoader.resetForNewMainState(newMainState);
                    mLoader.onContentChanged();
                    break;
            }
*/
        }
    }

    /**
     * Creates a new loader.
     *
     * @param mainState
     *     The main state information at the time the loader was created. The data appropriate to
     *     this state will be loaded when requested by the loader manager.
     */
    public TimeListLoader(@NonNull MainState mainState) {
        super(TwistyTimer.getAppContext());

//        Log.e(TimeListLoader.class.getSimpleName(),
//                "new TimeListLoader(mainState=" + mainState + ')');
        mMainState = mainState;
    }

    /**
     * Gets the main state that describes the data loaded by this loader instance.
     *
     * @return The main state information.
     */
    @NonNull
    public MainState getMainState() {
        return mMainState;
    }

    /**
     * Loads the solve times to a {@code Cursor}.
     *
     * @return The cursor to use to access the solve times.
     */
    @Override
    public Cursor loadInBackground() {
        return TwistyTimer.getDBHandler().getAllSolvesFor(
                mMainState.getPuzzleType(), mMainState.getSolveCategory(),
                mMainState.isHistoryEnabled());
    }
}
