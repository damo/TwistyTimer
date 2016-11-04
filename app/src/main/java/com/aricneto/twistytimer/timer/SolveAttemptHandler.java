package com.aricneto.twistytimer.timer;

import android.support.annotation.NonNull;

import com.aricneto.twistytimer.items.Solve;

/**
 * <p>
 * An interface for components requiring notification of timer events. Events
 * include user-interface cues that are synchronised with the life-cycle
 * events of a {@link PuzzleTimer} and events that mark the final delivery of
 * the definitive timer result when the timer is stopped. The periodic
 * refresh events are delivered via a separate {@link OnTimerRefreshListener}
 * interface, as they are typically only of interest to the UI widget that
 * displays the time value, not to the higher-level controller component.
 * </p>
 * <p>
 * {@link #onTimerCue(TimerUICue, TimerState)} is notified when the state of
 * the timer changes in some way. Details of the current timer state are
 * given on each notification. The UI cues are expected to drive all of the
 * changes to the presentation of the timer, aside from the simpler task of
 * refreshing the displayed time. UI cues are not intended to support every
 * possible requirement of the higher-level components (such as a {@code
 * Fragment} that manages the timer widget). The cues do not represent the
 * "states" of the component's state machine; they are triggers for
 * transitions between states that are defined by the component.
 * </p>
 * <p>
 * When a timer is stopped, cancelled, or reset, one of
 * {@link #onTimerStop(TimerState)}, {@link #onTimerCancelled(TimerState)},
 * or {@link #onTimerReset(TimerState)} is called. This call will be made
 * <i>after</i> the last call to {@code onTimerCue} for that solve attempt.
 * This final method call delivers the definitive timer state of the solve
 * attempt, which should be used when saving the result, or, if cancelled,
 * presenting the rolled-back result for further editing.
 * </p>
 *
 * @author damo
 */
public interface SolveAttemptHandler {

    // FIXME: Perhaps this is also just "onTimerSet()", too. The goal is to
    // keep the display of the timer consistent with edits made to the Solve.
    // If the edits are made via "PuzzleTimer", then it can call "onTimerSet"
    // to refresh the display. However, if the edits are made via the
    // "PuzzleTimer", when are they saved? How would the save operation
    // overlap with the need to save the "Solve" for the first time in
    // "onTimerStopped"?
    //
    // I think it can be omitted for now and added back later if it seems
    // appropriate.
//    void onSolveEdited();

    // Create new "Solve" recording puzzle type, category and scramble.
    @NonNull
    Solve onSolveAttemptStart();

    // Save new solve result. The solve will have been updated from the
    // timer's details.

    /**
     * <p>
     * Notifies the listener that a new solve attempt has been completed. The
     * {@code Solve} returned from {@link #onSolveAttemptStart()} is updated
     * with the elapsed time of the solve and any penalties incurred before
     * being date-stamped with the current system time and passed to this
     * method. The listener is expected to save this new result and then await
     * the final notification of the solve attempt, the call to
     * {@link OnTimerEventListener#onTimerSet(TimerState)}, which can be used
     * to display the result.
     * </p>
     * <p>
     * The solve can be saved synchronously or asynchronously. If saved
     * synchronously, the solve result and user-interface controls to edit the
     * solve result, can be displayed when {@code onTimerSet()} is called. If
     * saved asynchronously, the solve result can be displayed, but the UI
     * controls to edit the solve should not be enabled and/or displayed until
     * the save operation is complete. It is the responsibility of the listener
     * to ensure the proper sequence.
     * </p>
     * <p>
     * On saving the solve, the listener may set the database ID on the solve
     * using {@link Solve#setID(long)} to support later editing operations.
     * Edits should not change the value of the elapsed time, other than as a
     * result of changing the incurred penalties. Edits to the penalties should
     * not be made directly on the {@code Solve} instance, but instead through
     * the {@code PuzzleTimer} instance.
     * </p>
     *
     * @param solve
     *     The solve instance describing the result of the solve attempt.
     */
    void onSolveAttemptStop(@NonNull Solve solve);
}
