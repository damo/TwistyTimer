package com.aricneto.twistytimer.timer;

/**
 * The stages (i.e., states) for the {@link PuzzleTimer} state machine. The
 * stages change as touch events, "tick" events, and other automatic
 * timing-related are detected.
 *
 * @author damo
 */
// NOTE: This enum is not part of the public API of "PuzzleTimer" or
// "TimerState", so it has default (package) access only.
enum TimerStage {
    // TERMINOLOGY NOTE: These are the "states" of a "state machine", but
    // they are dubbed "stages". The current "TimerStage" is just one element
    // of the more general "TimerState". As it is this wider state that will
    // be saved and restored as "instance state", overloading this enum with
    // the name "State", while perfectly appropriate in the context of an
    // abstract FSM, would only cause confusion in the context of the wider
    // "TimerState".

    /**
     *
     */
    // FIXME: Describe this. NOTE: Can never return to "UNUSED", it is the
    // starting stage. Also, can never leave "STOPPED", it is the finishing
    // stage. "UNUSED" allows "STOPPED" to be made unresponsive to touch
    // events, etc., so the timer stays "STOPPED".
    UNUSED,

    /**
     *
     */
    // FIXME: Write explanation!!! Used transition to the appropriate first
    // stage (depending on the configuration).
    STARTING,

    /**
     *
     */
    // FIXME: This would seem to make sense: the main reason *I* like the
    // hold-to-start behaviour is that it often prevents accidental touches
    // from starting the timer. However, *I* run with no inspection period,
    // so I do not notice the lack of this same convenience for inspection. I
    // think it makes sense to support it for both, and it also keeps things
    // nice and consistent.
    INSPECTION_HOLDING_FOR_START,

    /**
     * <p>
     * Inspection is enabled and the inspection period is ready to start.
     * This stage begins with the first touch down on the timer UI from the
     * {@link #STOPPED} stage or {@link #INSPECTION_PAUSED} stage (if
     * resuming from a pause). There is no time limit in this stage and the
     * inspection countdown is <i>not</i> running. This stage will remain
     * active until a touch up event is notified and the stage transitions to
     * {@link #INSPECTION_STARTING}. The UI will be notified at the start of
     * this stage, allowing it to prepare for the beginning of the inspection
     * period. Any touch down event is ignored, as the touch is already
     * expected to be down on entering this stage.
     * </p>
     * <p>
     * When this stage is entered, the UI will be notified. The remaining
     * inspection time (which will be the full inspection duration) will be
     * given. If a "start cue" is enabled for the inspection period, it
     * should be applied now.
     * </p>
     */
    INSPECTION_READY_TO_START,

    /**
     * <p>
     * The inspection period is starting and the inspection countdown begins
     * running. This stage is entered with the first touch up on the timer UI
     * from the {@link #INSPECTION_READY_TO_START} stage. This stage will
     * remain active for a short period before transitioning to
     * {@link #INSPECTION_STARTED}. While in this stage, touch events will
     * <i>not</i> start the timer, as they are assumed to be accidental
     * "bounces" from the original touch that initiated this stage. Other
     * than ignoring touches, this stage acts like {@code INSPECTION_STARTED}.
     * </p>
     * <p>
     * While in this stage, the UI will be notified of the remaining
     * inspection time. There will be one notification immediately on
     * entering this stage, and then periodic notifications thereafter.
     * However, as this stage is very brief, the stage may have transitioned
     * to {@code INSPECTION_STARTED} before any notification, other than the
     * first, is reported.
     * </p>
     */
    // NOTE: The separation of this stage from "INSPECTION_STARTED", aside
    // from "de-bouncing", also provides a clear instant when the inspection
    // start time can be recorded, as this stage is only entered once per
    // solve attempt, whereas "INSPECTION_STARTED" can be entered several
    // times (if "INSPECTION_SOLVE_HOLDING_FOR_START" is aborted).
//    INSPECTION_STARTING,

    /**
     * <p>
     * The inspection period has started, the inspection countdown is
     * running, and the timer will respond normally to touch events.
     * </p>
     * <p>
     * If the inspection countdown reaches zero, a "+2" penalty will be
     * incurred and the stage will remain unchanged. If the stage has not
     * reached {@link #SOLVE_STARTING} (or beyond) within two seconds of the
     * countdown reaching zero, a "DNF" penalty will be incurred and the
     * stage will transition to {@link #STOPPING}, ending the solve attempt.
     * </p>
     * <p>
     * If hold-to-start is disabled:
     * </p>
     * <ul>
     *     <li>On a touch down event the stage transitions to
     *     {@link #INSPECTION_SOLVE_READY_TO_START}. FIXME: De-bounce this touch?
     *     </li>
     *     <li>On a touch up event, the stage remains unchanged. This touch
     *     up is not expected, as this stage should have been entered in
     *     response to a touch up event which should already have been
     *     consumed.</li>
     * </ul>
     * <p>
     * If hold-to-start is enabled:
     * </p>
     * <ul>
     *     <li>On a touch down event the stage transitions to
     *     {@link #INSPECTION_SOLVE_HOLDING_FOR_START}. FIXME: De-bounce this touch?
     *     </li>
     *     <li>On a touch up event, the stage remains unchanged. This touch
     *     up is not expected, as this stage should have been entered in
     *     response to a touch up event which should already have been
     *     consumed.</li>
     * </ul>
     * <p>
     * While this stage is running, the UI will be notified regularly of the
     * remaining inspection time, so that it can update the timer display.
     * The updates will include notifications of any penalties incurred due
     * to an overrun of the expected inspection period.
     * </p>
     */
    INSPECTION_STARTED,

    /**
     * <p>
     * The inspection period has started, the inspection countdown is still
     * running, the hold-to-start behaviour is enabled and a touch down
     * event has been received (in a previous stage). If this stage is still
     * active after the hold-to-start duration has elapsed, the stage will
     * transition to {@link #INSPECTION_SOLVE_READY_TO_START}. If a touch up
     * event is received before the hold-to-start duration has elapsed, the
     * stage will return to {@link #INSPECTION_STARTED}. If a touch down
     * event is received, it will be ignored (it is not expected, as a touch
     * down event should have caused this stage to be entered and the touch
     * should still remain down).
     * </p>
     * <p>
     * If the inspection countdown reaches zero, the same actions are taken
     * as described for the {@link #INSPECTION_STARTED} stage.
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified and may
     * prepare the display for the start of a solve, though the solve will
     * not be fully ready to start until the hold-to-start period has
     * elapsed. The UI will continue to be notified of the remaining
     * inspection time and any penalties that have been incurred.
     * </p>
     */
    INSPECTION_SOLVE_HOLDING_FOR_START,

    /**
     * <p>
     * The inspection period has started, the inspection countdown is still
     * running, and a touch down event was received (in a previous stage). If
     * a touch up event is received, the stage will transition to
     * {@link #SOLVE_STARTING}. If a touch down event is received, it will be
     * ignored.
     * </p>
     * <p>
     * If the inspection countdown reaches zero, the same actions are taken
     * as described for the {@link #INSPECTION_STARTED} stage.
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified and may
     * prepare the display for the start of a solve. The UI will continue to
     * be notified of the remaining inspection time and any penalties that
     * have been incurred.
     * </p>
     */
    INSPECTION_SOLVE_READY_TO_START,

    /**
     * <p>
     * There was no inspection period, the hold-to-start behaviour is
     * enabled and a touch down event was received in the {@link #STOPPED}
     * stage. If this stage is still active after the hold-to-start duration
     * has elapsed, the stage will transition automatically to
     * {@link #SOLVE_READY_TO_START}. If a touch up event is received before
     * the hold-to-start duration has elapsed, the stage will transition
     * {@link #CANCELLING} and the UI may revert the timer display to show
     * the previous result (if any). There is no maximum time limit for
     * holding in this stage. If a touch down event is received, it will be
     * ignored.
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified and may
     * prepare the display for the start of a solve, though the solve will
     * not be fully ready to start until the hold-to-start period has
     * elapsed. If a "start cue" is enabled, it should <i>not</i> yet be
     * applied.
     * </p>
     */
    SOLVE_HOLDING_FOR_START,

    /**
     * <p>
     * The was no inspection period and a touch down event was received (in a
     * previous stage). If a touch up event is received, the stage will
     * transition to {@link #SOLVE_STARTING}. There is no maximum time limit
     * for holding in this stage. If a touch down event is received, it will
     * be ignored.
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified and may
     * prepare the display for the start of a solve. If a "start cue" is
     * enabled, it should be applied now.
     * </p>
     */
    SOLVE_READY_TO_START,

    /**
     * <p>
     * The solve is starting and the timer begins running. This stage is
     * entered with the first touch up on the timer UI from the
     * {@link #SOLVE_READY_TO_START} stage. This stage will remain active for
     * a short period before transitioning to {@link #SOLVE_STARTED}. While
     * in this stage, touch events will <i>not</i> stop the timer, as they
     * are assumed to be accidental "bounces" from the original touch that
     * initiated this stage. Other than ignoring touches, this stage acts
     * like {@code SOLVE_STARTED}.
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified of the
     * elapsed time (which will be zero, as the timer has yet to record any
     * elapsed time). While in this stage, the UI will be notified regularly
     * of the elapsed time, so that it can update the timer display.
     * </p>
     */
//    SOLVE_STARTING,

    /**
     * <p>
     * The solve has started, the timer is running, and the timer will
     * respond normally to touch events.
     * </p>
     * <p>
     * If a touch down event is detected, the timer is stopped and the stage
     * transitions to {@link #STOPPING}.
     * </p>
     * <p>
     * If a touch up event is detected, the stage remains unchanged. A touch
     * down event is expected before the next touch up event and action is
     * only taken on that touch down event.
     * </p>
     * <p>
     * While in this stage, the UI will be notified regularly of the elapsed
     * time, so that it can update the timer display.
     * </p>
     */
    SOLVE_STARTED,

    // FIXME: Document this....
    SOLVE_PAUSED,

    /**
     * <p>
     * The timer was cancelled and the solve attempt abandoned. This is a
     * transient stage that rolls back the timer state to what it was before
     * the solve attempt began and then automatically transitions to
     * {@link #STOPPED}.
     * </p>
     * <p>
     * No touch events are received in this stage.
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified of the
     * cancellation of the solve attempt. It should re-report the result of
     * the previous solve attempt, if any.
     * </p>
     */
    // FIXME: Cues? "CUE_STOPPING" is fired by "STOPPED", but "STOPPING"
    // transitions to "STOPPED" before calling "onTimerCancelled()".
    CANCELLING,

    /**
     * <p>
     * The timer has stopped and a new solve result is available. The attempt
     * was not cancelled. The elapsed time recorded for the solve attempt
     * ceases to be updated. There may be no elapsed time recorded if the
     * inspection period timed out and an inspection stage transitioned
     * directly to this stage to record a "DNF" solve. This is a transient
     * stage that commits the new timer state (discarding the previous timer
     * state) and then automatically transitions to {@link #STOPPED}.
     * </p>
     * <p>
     * No touch events are received in this stage.
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified of the
     * results of the new solve attempt via the
     * {@link OnTimerEventListener#onTimerEndSolve(TimerState)} call-back.
     * </p>
     */
    // FIXME: Cues? "CUE_STOPPING" is fired by "STOPPED", but "STOPPING"
    // transitions to "STOPPED" before calling "onTimerNewResult()".
    STOPPING,

    /**
     * <p>
     * The timer has reported the result of the solve attempt (if any) and is
     * ready to begin a new solve attempt.
     * </p>
     * <p>
     * If a touch down event is received the stage will transition to one of:
     * </p>
     * <ul>
     *     <li>{@link #INSPECTION_READY_TO_START}, if inspection is enabled.
     *     </li>
     *     <li>{@link #SOLVE_READY_TO_START}, if inspection is disabled and
     *     "hold-to-start" behaviour is disabled.</li>
     *     <li>{@link #SOLVE_HOLDING_FOR_START}, if inspection is disabled and
     *     "hold-to-start" behaviour is enabled.</li>
     * </ul>
     * <p>
     * If a touch up event is detected in this stage, it will be ignored. It
     * is likely to be the touch up event after the touch down event that
     * stopped the timer and caused the transition from
     * {@link #SOLVE_STARTED} to this stage (via the {@link #STOPPING} stage).
     * </p>
     * <p>
     * Immediately on entering this stage, the UI will be notified of the
     * completion of the solve attempt, the elapsed time for the solve (if
     * any), and any penalties incurred (during the inspection period).
     * </p>
     */
    // FIXME: Cues? "CUE_STOPPING" is fired by "STOPPED", but "STOPPING" and
    // "CANCELLING" transition to "STOPPED" before calling "onTimerNewResult
    // ()" or "onTimerCancelled()".
    // FIXME: Would "TERMINATED" be a better name for this? Once "STOPPED" a
    // timer cannot be started again (a new TimerState is needed and it will
    // start from "UNUSED"). However, "TERMINATED" suggests "aborted" rather
    // than "completed". My quibble is with the notion that "STOPPED" does
    // not suggest permanence and that it might be assumed that it could be
    // "*_STARTED" again. How about "COMPLETED" or "DONE" or "FINISHED" or
    // "ENDED"? "ENDED" sounds like it might have the most apt meaning.
    STOPPED,
}
