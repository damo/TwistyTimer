package com.aricneto.twistytimer.timer;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * A listener that logs timer events. Add this to a {@link PuzzleTimer} as an
 * extra listener, so that the other event listeners do not need to implement
 * basic logging. If used, this should be added as the first listener, so it
 * will be called first and log its output before any other log messages from
 * other listeners.
 *
 * @author damo
 */
public class OnTimerEventLogger implements OnTimerEventListener {
    @Override
    public void onTimerCue(@NonNull TimerCue cue) {
        Log.d(getClass().getSimpleName(), "onTimerCue(" + cue + ')');
    }

    @Override
    public void onTimerSet(@NonNull TimerState timerState) {
        Log.d(getClass().getSimpleName(), "onTimerSet(" + timerState + ')');
    }

    @Override
    public void onTimerPenalty(@NonNull TimerState timerState) {
        Log.d(getClass().getSimpleName(), "onTimerPenalty(" + timerState + ')');
    }
}
