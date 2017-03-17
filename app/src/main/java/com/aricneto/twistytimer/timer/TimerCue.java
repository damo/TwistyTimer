package com.aricneto.twistytimer.timer;

import com.aricneto.twistytimer.items.Solve;

/**
 * <p>
 * Timer cues are notifications that are sent to a listener when the state of
 * the puzzle timer changes at key points in its life-cycle. Timer cues are
 * notified in a call-back to {@link OnTimerEventListener#onTimerCue(TimerCue)}.
 * The listener can respond by triggering some visual or audible cue to alert
 * the user to the change.
 * </p>
 * <p>
 * Most timer cues fire only once during a solve attempt on the first transition
 * to a new timer state. If the timer state is saved and restored, the timer cue
 * will not fire again on restoration. For example, if the timer overruns the
 * normal inspection period, {@link #CUE_INSPECTION_OVERRUN} is notified and
 * the listener may sound an alert and change the color of the timer display.
 * However, if the state is then saved and restored (e.g., if the device
 * orientation changes) before the inspection period inspection period times
 * out, then the cue is not fired again on restoration, as a second audible
 * alert would be confusing and inappropriate. However, if the color has been
 * changed, that needs to be restored in response to the call-back to the
 * {@link OnTimerEventListener#onTimerSet(TimerState)} method of the listener.
 * In general, if a cue notification triggers a transient response, such as a
 * sound effect or visual transition, it can be handled solely in the
 * {@code onTimerCue(TimerCue)} call-back; however, if the cue notification
 * changes the state of the listener (e.g., the timer text color), then that
 * change needs to be made in the {@code onTimerCue(TimerCue)} call-back, but
 * the state must also always be detected and restored in the
 * {@code onTimerSet(TimerState)} call-back. {@code onTimerSet(TimerState)}
 * should not, typically, concern itself with sound effects, visual transitions,
 * or other transient responses to changes in the state; it should only set the
 * state of the UI to match the state of the timer.
 * </p>
 *
 * @author damo
 */
public enum TimerCue {
    // The documentation of this public "TimerCue" enum does not refer to the
    // non-public "TimerStage" enum, as such references would inappropriately
    // leak internal implementation details.

    // NOTE: The point of "TimerCue" is to provide just that, "cues" (or
    // "hints") to the timer UI to allow it TO DRIVE ITS OWN FINITE STATE
    // MACHINE. Just like the "onTouch*", "onTick", etc. that are notified to
    // "PuzzleTimer", it is up to the UI to actually manage its state. There is
    // no point in trying to do both things from the timer. Therefore, the timer
    // need only fire cues when something has changed in a manner that probably
    // requires SOME SORT of change to the UI. However, it is not the job of
    // the "PuzzleTimer" to do any hand-holding or to pollute itself with the
    // internal, and unpredictable, concerns of the UI. The primary focus should
    // be on the needs of the "TimerView", while a few cues might be used to
    // fill in any *glaring* gaps in the FSM within the "TimerFragment", but
    // only if they contribute significantly to some simplification of that FSM
    // and only if the cue is relatively generic.

    /**
     * <p>
     * Inspection and hold-to-start are enabled and the user has touched down to
     * begin the hold-for-start period before starting the inspection countdown.
     * The inspection countdown has not started, and, if the touch is lifted up
     * before the hold-for-start period has elapsed, the countdown will not
     * start and {@link #CUE_CANCELLING} will be notified. This cue is notified
     * only once per solve attempt and only if inspection is enabled <i>and</i>
     * the hold-for-start behaviour is enabled.
     * </p>
     * <p>
     * If the "start cue" preference is enabled, no such visual cue should be
     * effected until {@link #CUE_INSPECTION_READY_TO_START} is notified.
     * </p>
     */
    CUE_INSPECTION_HOLDING_FOR_START,

    /**
     * <p>
     * The inspection period is ready to start. The countdown is not running,
     * but will start running when the user lifts up the touch. This cue is
     * notified only once per solve attempt and only if inspection is enabled.
     * </p>
     * <p>
     * If the "start cue" preference is enabled, the visual cue should be
     * effected now.
     * </p>
     */
    CUE_INSPECTION_READY_TO_START,

    /**
     * <p>
     * The inspection period has started and the inspection timer is counting
     * down. This cue will be notified only once and only if inspection is
     * enabled. If {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START} is notified,
     * but the hold time is deemed too short, {@link #CUE_INSPECTION_REASSERTED}
     * will be notified to indicate a return to the normal inspection countdown.
     * </p>
     * <p>
     * Expectation: The UI will show the current inspection time prominently,
     * perhaps reverting any highlight that was applied to the displayed time
     * when {@link #CUE_INSPECTION_READY_TO_START} was notified. The UI should
     * expect regular update notifications of the remaining inspection time,
     * as the countdown has begun.
     * </p>
     */
    CUE_INSPECTION_STARTED,

    /**
     * <p>
     * The inspection period is counting down and the user has touched down
     * to begin the hold-for-start period before starting the timing of the
     * solve attempt. The solve timer has not started, and, if the touch is
     * lifted up before the hold-to-start period has elapsed, the solve timer
     * will not start and {@link #CUE_INSPECTION_REASSERTED} will be notified.
     * All the while, the inspection countdown continues to run and the UI
     * continues to receive notifications of the remaining inspection time.
     * </p>
     * <p>
     * Expectation: The UI will continue to show (and update) the inspection
     * time, perhaps making the inspection time less prominent. The UI will
     * show the solve time holding at "0.00" (or in whatever format is
     * appropriate), perhaps making it more prominent than the inspection time.
     * The solve time should not be highlighted (if the "start cue" preference
     * is enabled), as it will not be ready to start just yet.
     * </p>
     */
    CUE_INSPECTION_SOLVE_HOLDING_FOR_START,

    /**
     * <p>
     * The inspection period has started and the inspection timer is counting
     * down, but the user's touch was not held down long enough to exceed the
     * hold-to-start threshold time. This cue may be notified more than once,
     * each time a hold-to-start state is aborted.
     * </p>
     * <p>
     * While this cue is similar to {@link #CUE_INSPECTION_STARTED}, it makes a
     * necessary distinction. When {@code CUE_INSPECTION_STARTED} is notified,
     * the inspection period has been started for the first time. Any visual
     * "start cue" (determined by a preference) should be cleared. However, if
     * {@code CUE_INSPECTION_REASSERTED} is received, it may be after a cue
     * such as {@link #CUE_INSPECTION_OVERRUN} has been notified and the same
     * visual cue might have been applied to indicate this overrun state to the
     * user. Therefore, it would not be appropriate to remove any highlighting
     * when {@code CUE_INSPECTION_REASSERTED} is received, unlike for
     * {@code CUE_INSPECTION_STARTED}.
     * </p>
     * <p>
     * Expectation: The UI will show the current inspection time in the same
     * manner as for {@code CUE_INSPECTION_STARTED}.
     * </p>
     */
    CUE_INSPECTION_REASSERTED,

    /**
     * <p>
     * The inspection period is counting down and the user has touched down
     * to indicate readiness to start the solve timer. If hold-for-start is
     * enabled, the required hold period has now elapsed. Lifting the touch up
     * will start the timer.
     * </p>
     * <p>
     * Expectation: The UI will continue to show (and update) the inspection
     * time, perhaps making the inspection time less prominent. The UI will show
     * the solve time holding at "0.00" (or in whatever format is appropriate),
     * perhaps making it more prominent than the inspection period. The solve
     * time may be highlighted (if the "start cue" preference is enabled) to
     * indicate that it will start as soon as the touch is lifted.
     * </p>
     */
    CUE_INSPECTION_SOLVE_READY_TO_START,

    /**
     * <p>
     * The inspection period is counting down and only 7 seconds remain
     * before the countdown reaches zero.
     * </p>
     * <p>
     * The WCA Regulations A3a1 stipulate a 15-second inspection time with
     * notice given to the competitor as the inspection time elapses:
     * '<i>A3d2) When 8 seconds of inspection have elapsed, the judge calls
     * "8 SECONDS"</i>' and '<i>A3d3) When 12 seconds of inspection have
     * elapsed, the judge calls "12 SECONDS"</i>'. These calls were introduced
     * into the regulations in 2008, replacing a call of "10 SECONDS" in the
     * previous version. The notes in the version history record, 'Art. A3d2,
     * changed 15 to 13. New starting procedure, it takes around 2 seconds to
     * start the timer.' It seems that the intention was to give enough advance
     * notice to competitors to allow them to finish inspection, put down the
     * puzzle, place their hands on the timer, and then begin the solve. The
     * "8 SECONDS" and "12 SECONDS" calls are therefore probably not intended
     * to give notice that about 1/2 and 3/4 of the (rounded) inspection time
     * have elapsed, rather they are similar to the previous regulations that
     * gave notice when there were 5- and 0-seconds-to-go, only now the
     * warnings are given 2-3 seconds earlier to allow the competitor to wrap
     * up and prepare the solve timer.
     * </p>
     * <p>
     * The app allows the inspection time to be set to a value other than 15
     * seconds. In keeping with the likely intentions of the regulations, the
     * cues reporting the elapsed inspection time, will be notified relative
     * to the end of the inspection time, i.e., with 7 seconds remaining and
     * with 3 seconds remaining. For a 15-second inspection time, these will
     * correspond to the "8 SECONDS" and "12 SECONDS" elapsed time calls, but
     * not so if the inspection time is longer or shorter than 15 seconds. If
     * the inspection time is set to 7 seconds or shorter, the
     * 7-seconds-to-go cue will <i>not</i> be notified. If the inspection
     * time is 3 seconds or shorter, <i>neither</i> cue will be notified.
     * </p>
     * <p>
     * Expectation: The UI will continue to show (and update) the inspection
     * time. An audible or visual cue might be used to warn the user that the
     * inspection time is running out. The UI should display the solve time
     * as appropriate for the most recently notified cue (e.g., if
     * {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START} has been received and
     * has not been rescinded by a subsequent {@link #CUE_INSPECTION_REASSERTED}
     * cue).
     * </p>
     */
    // Equivalent to "8 SECONDS" call allowing for variable inspection time.
    CUE_INSPECTION_7S_REMAINING,

    /**
     * <p>
     * The inspection period is counting down and only 3 seconds remain before
     * the countdown reaches zero.
     * </p>
     * <p>
     * See {@link #CUE_INSPECTION_7S_REMAINING} for more details on this and
     * the related cue and the conditions under which it may not be notified.
     * </p>
     */
    // Equivalent to "12 SECONDS" call allowing for variable inspection time.
    CUE_INSPECTION_3S_REMAINING,

    /**
     * <p>
     * The inspection period is counting down and has reached zero. A penalty
     * of "2 SECONDS" is incurred. The user must start the solve timer within
     * two seconds or the inspection period will time out, a "DNF" penalty
     * will be incurred and the solve attempt will be terminated. This cue may
     * be notified even after {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START}
     * or {@link #CUE_INSPECTION_SOLVE_READY_TO_START} has been notified.
     * </p>
     * <p>
     * Immediately after this cue is notified, a call-back will be made to
     * {@link OnTimerEventListener#onTimerPenalty(TimerState)}.
     * </p>
     * <p>
     * Expectation: The UI will continue to show (and update) the inspection
     * time. The inspection time remaining that is notified in refresh events
     * will be zero or negative during the overrun period. The UI may indicate
     * that a "+2" penalty has been incurred and that the inspection time is
     * fast running out. An audible or visual cue might be used. The UI should
     * display the solve time as appropriate for the most recently notified cue
     * (e.g., if {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START} has been
     * received and has not been rescinded by a subsequent
     * {@link #CUE_INSPECTION_REASSERTED} cue).
     * </p>
     */
    CUE_INSPECTION_OVERRUN,

    /**
     * <p>
     * The inspection period has stopped counting down as it reached zero and
     * the subsequent two-second overrun period also expired. A DNF penalty
     * is incurred and the solve attempt is stopped. {@link #CUE_STOPPING} will
     * be notified presently.
     * </p>
     * <p>
     * Immediately after this cue is notified, a call-back will be made to
     * {@link OnTimerEventListener#onTimerPenalty(TimerState)}.
     * </p>
     */
    CUE_INSPECTION_TIME_OUT,

    /**
     * <p>
     * The user has touched down to begin the hold-for-start period before
     * starting the timing of the solve attempt. There was no prior inspection
     * period. The solve timer has not started, and, if the touch is lifted up
     * before the hold-for-start period has elapsed, the solve timer will not
     * start and {@link #CUE_CANCELLING} will be notified.
     * </p>
     * <p>
     * Expectation: The UI will show the solve time holding at "0.00" (or in
     * whatever format is appropriate). If the "start cue" preference is
     * enabled, the solve time should not be highlighted, as it will not yet be
     * ready to start. As there was no inspection period, no inspection time
     * should be shown.
     * </p>
     */
    CUE_SOLVE_HOLDING_FOR_START,

    /**
     * <p>
     * The user has touched down to indicate readiness to start the solve
     * timer. There was no prior inspection period. If hold-for-start is
     * enabled, the required hold period has now elapsed. Lifting the touch
     * up will start the timer.
     * </p>
     * <p>
     * Expectation: The UI will show the solve time holding at "0.00" (or in
     * whatever format is appropriate). The solve time may be highlighted to
     * indicate that it will start as soon as the touch is lifted (if the
     * "start cue" preference is enabled). As there was no inspection period,
     * no inspection time should be shown.
     * </p>
     */
    CUE_SOLVE_READY_TO_START,

    /**
     * <p>
     * The solve timer has started and is now recording the elapsed time for a
     * solve attempt.
     * </p>
     * <p>
     * Expectation: The UI will show only the elapsed solve time. If the time
     * was highlighted for a prior "start cue", that highlight should be
     * reverted. If there was a prior inspection period, the inspection time
     * will not be shown (though indications may be shown for any "+2" penalties
     * incurred before starting the solve timer).
     * </p>
     */
    CUE_SOLVE_STARTED,

    /**
     * <p>
     * The solve timer was started but has now been paused. The solve attempt
     * has not finished, but the timer is not recording any further elapsed
     * time for the solve attempt.
     * </p>
     * <p>
     * Expectation: The UI will show only the elapsed solve time, but should
     * also indicate that the time is paused (e.g., by showing a prominent icon
     * that can be used to resume the timer).
     * </p>
     */
    CUE_SOLVE_PAUSED,

    /**
     * <p>
     * The solve timer was paused but has now been resumed. The solve attempt
     * has not finished and the timer will resume recording of the elapsed time
     * for the solve attempt from the value that was recorded at the instant
     * that the timer was paused.
     * </p>
     * <p>
     * Expectation: The UI will show only the elapsed solve time.
     * </p>
     */
    CUE_SOLVE_RESUMED,

    /**
     * <p>
     * The timer (inspection or solve) has stopped running and no result should
     * be recorded for this solve attempt. This is notified only once per solve
     * attempt. If the hold-to-start behaviour is enabled and the "hold" was
     * lifted too early, this will also be notified to signal the end of the
     * attempt even though the timer was never started. If inspection is
     * enabled, the cancelling a hold-for-start during the inspection period
     * that would start the solve timer will not cause this cue to fire.
     * </p>
     * <p>
     * When a solve attempt is cancelled, the timer state will roll back to the
     * result of the previous solve attempt, or to an unused state if there was
     * no such previous result. This <i>restored</i> state will be notified in
     * a call to {@link OnTimerEventListener#onTimerSet(TimerState)} after this
     * cancelling cue has been notified; the cancelled state is <i>not</i>
     * notified in any further call-backs and is discarded.
     * </p>
     */
    CUE_CANCELLING,

    /**
     * <p>
     * The timer has stopped running and a result can be recorded. This is
     * notified only once per solve attempt. This cue will always be the last
     * cue notified. {@link OnTimerEventListener#onTimerSet(TimerState)} will
     * be notified next to allow the result to be displayed before a call-back
     * to {@link SolveHandler#onSolveStop(Solve)} allows the solve to be saved.
     * </p>
     */
    CUE_STOPPING
}
