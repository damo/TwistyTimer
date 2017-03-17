package com.aricneto.twistytimer.timer;

import android.support.annotation.NonNull;

/**
 * <p>
 * An interface for components requiring notification of timer life-cycle
 * events. Events include user-interface cues that are synchronised with the life-cycle
 * events of a {@link PuzzleTimer} and events that mark the final delivery of
 * the definitive timer result when the timer is stopped. The periodic
 * refresh events are delivered via a separate {@link OnTimerRefreshListener}
 * interface, as they are typically only of interest to the UI widget that
 * displays the time value, not to the higher-level controller component.
 * </p>
 * <p>
 * {@link #onTimerCue(TimerCue, TimerState)} is notified when the state of the
 * timer changes in some way. Details of the current timer state are given on
 * each notification. The timer cues are expected to drive all of the changes
 * to the presentation of the timer, aside from the simpler task of refreshing
 * the displayed time. Timer cues are not intended to support every possible
 * requirement of the higher-level components (such as a {@code Fragment} that
 * manages the timer widget). The cues do not represent the "states" of the
 * component's state machine; they are triggers for transitions between states
 * that are defined by the component.
 * </p>
 * FIXME: Do the FIXMEs below belong in "PuzzleTimer"? Given the relationship
 * between the different interfaces there. Really on the the cues can be put in
 * their own class description, as the rest need to be together to present the
 * timer details in a proper context.
 * FIXME: Describe the temporal relationship between "onTimerCue" and the other
 * methods (i.e., specifically the manner in which other methods "bracket" the
 * calls to "onTimerCue". Refer the reader to the "TimerCue" class description
 * for more details on the specific cues.
 * FIXME: Mention the relationship between cues and refresh events.
 * FIXME: Mention the relationship between cues and "SolveAttemptHandler".
 *
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
public interface OnTimerEventListener {
    /**
     * <p>
     * Notifies the listener that the state of the timer has been changed and
     * that the listener should update its own state to reflect the new timer
     * state.
     * </p>
     * <p>
     * This method is only called from a subset of the possible timer states:
     * only those states from which the state of the listener is expected to be
     * determinable entirely from the timer state. It is not called for some
     * transient or "interrupting" states, such as when a hold-to-start period
     * elapses and a ready-to-start state is entered, or if alarms such as
     * inspection period overruns or time-outs occur and penalties are incurred;
     * only timer cues are notified in those cases. An example of when this is
     * important is described for {@link #onTimerPenalty(TimerState)}.
     * </p>
     * <p>
     * This method is always called at the start and end of a solve attempt, on
     * the transition from the inspection countdown to the solve timer, when
     * the timer is paused or resumed and when the timer state is restored
     * after being saved (e.g., during a device configuration change or reboot).
     * The timer state is never saved when it is in a hold-to-start or
     * ready-to-start state; if that is the case, the timer state is first
     * returned to the previous state (i.e., to the inspection countdown state
     * if the hold or ready state is for starting the solve timer during the
     * inspection period, or to the reset/unused state otherwise). Therefore,
     * on restoring the timer state, it will not be in a transient or
     * "interrupting" state, such as those described above.
     * </p>
     *
     * @param timerState
     *     The timer state to which the listener should synchronise its own
     *     state.
     */
    void onTimerSet(@NonNull TimerState timerState);

    /**
     * <p>
     * Notifies the listener that the penalties for the solve attempt have
     * changed. A new penalty may have been incurred or an existing penalty
     * annulled.
     * </p>
     * <p>
     * Penalties can be incurred automatically during the inspection period: if
     * the inspection time reaches zero and an overrun period begins ("+2"); or
     * if that overrun period expires ("DNF"). This penalty event may happen
     * during any timer state, such as during the inspection period in a
     * "hold-to-start" or "ready-to-start" state for the solve timer. This is
     * unlike {@link #onTimerSet(TimerState)} which is never called from such
     * transient states. Therefore, the implementation of {@code onTimerPenalty}
     * should change only those properties of the listener that relate to a
     * change in the penalties and leave other properties as they are. For
     * example, if the timer is in a ready-to-start state and a "start cue"
     * highlight has been applied to the timer text, that highlight should not
     * be changed by the implementation of this method.
     * </p>
     *
     * @param timerState
     *     The state of the puzzle timer from which the penalties can be read.
     */
    void onTimerPenalty(@NonNull TimerState timerState);

    /**
     * <p>
     * Notifies the listener at the instant that a specific change has occurred
     * in the state of the timer, or that a time-dependent "alarm" event has
     * been triggered. "Alarm" events include notifications of the remaining
     * inspection time, overrun of the inspection time when the countdown
     * reaches zero, and the transition from a hold-to-start to a ready-to-start
     * state.
     * </p>
     * <p>
     * Most timer cues can fire only once during the life of a solve attempt.
     * For example, say the inspection countdown reaches zero and a cue notifies
     * that the inspection countdown has entered the overrun period. If the
     * timer state is then saved and restored (e.g., because the orientation of
     * the device has changed), the timer cue will not be notified again on
     * restoration of the saved state. This is consistent with the expected
     * uses of timer cues, such as sound effects or UI transitions. Such effects
     * should only occur once when a timer state is first reached, not if that
     * timer state is restored having been previously saved.
     * </p>
     *
     * @param cue
     *     The timer cue identifying the state change that has just occurred.
     */
    void onTimerCue(@NonNull TimerCue cue);
}
