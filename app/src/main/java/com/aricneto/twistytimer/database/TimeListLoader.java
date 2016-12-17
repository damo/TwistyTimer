package com.aricneto.twistytimer.database;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.content.CursorLoader;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.utils.MainState;

/**
 * A loader that reads solve times from the database to support the
 * {@code TimerListFragment}.
 */
public class TimeListLoader extends CursorLoader {
    /**
     * The main state information corresponding to the data that was loaded.
     */
    @NonNull
    private final MainState mMainState;

    /**
     * Creates a new loader.
     *
     * @param mainState
     *     The main state information at the time the loader was created. The
     *     data appropriate to this state will be loaded when requested by the
     *     loader manager.
     */
    public TimeListLoader(@NonNull MainState mainState) {
        super(TwistyTimer.getAppContext());

        mMainState = mainState;
    }

    /**
     * Gets the main state that describes the data loaded by this loader
     * instance.
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
