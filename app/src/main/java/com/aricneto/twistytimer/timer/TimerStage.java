package com.aricneto.twistytimer.timer;

import com.aricneto.twistytimer.items.Solve;

/**
 * <p>
 * The stages (i.e., states) for the {@link PuzzleTimer} state machine. The
 * stages change as touch events, "tick" events, and other automatic
 * timing-related are detected.
 * </p>
 * <p>
 * Many stage transitions notify the UI and other listeners via a call-back to
 * {@link OnTimerEventListener#onTimerSet(TimerState)}. This may be followed by
 * a call-back to {@link OnTimerEventListener#onTimerCue(TimerCue)} to provide
 * a more specific hint about the new stage. Timer cues specific to a stage are
 * (typically) not notified again if the timer state is saved and then restored
 * for that stage. However, {@code onTimerSet(TimerState)} is notified again.
 * </p>
 *
 * @author damo
 */
// NOTE: This enum is not part of the public API of "PuzzleTimer" or
// "TimerState", so it has default (package) access only.
enum TimerStage {
    // TERMINOLOGY NOTE: These are the "states" of a "state machine", but they
    // are dubbed "stages". The current "TimerStage" is just one element of the
    // more general "TimerState". As it is this wider state that will be saved
    // and restored as "instance state", overloading this enum with the name
    // "State", while perfectly appropriate in the context of an abstract FSM,
    // would only cause confusion in the context of the wider "TimerState".

    /**
     * <p>
     * The puzzle timer has not yet been used and there has been no interaction
     * with the user in readiness to start the inspection countdown or solve
     * timer. The puzzle timer's state always begins with this stage and then
     * ends with the {@link #STOPPED} stage (or is discarded if cancelled). The
     * state never returns to this stage. If no current solve has started, no
     * previous solve has been restored restored, or the timer is explicitly
     * reset, a new state is created and is initialised to this stage.
     * </p>
     * <p>
     * The puzzle timer will respond to touches when at this stage. When a
     * touch down is notified, the stage will transition to {@link #STARTING}.
     * </p>
     * <p>
     * When this stage is entered, or if the timer state is restored to
     * this stage, the UI is notified of the reset timer state via
     * {@code onTimerSet(TimerState)}. When the timer state is at this stage,
     * {@link TimerState#isReset()} will return {@code true}.
     * </p>
     */
    UNUSED,

    /**
     * <p>
     * A transitional stage where the correct start behaviour is identified.
     * This stage is entered from {@link #UNUSED} on a touch down event. It
     * transitions directly to one of {@link #INSPECTION_HOLDING_FOR_START},
     * {@link #INSPECTION_READY_TO_START}, {@link #SOLVE_HOLDING_FOR_START},
     * or {@link #SOLVE_READY_TO_START}, depending on the preference settings
     * for an inspection period and for hold-to-start behaviour.
     * </p>
     * <p>
     * When this stage is entered, the UI will be notified via a call-back to
     * {@code onTimerSet(TimerState)}. A timer cue is notified after the
     * transition to the following stage. The timer is never restored to this
     * stage. When the state is at this stage, {@link TimerState#isReset()} and
     * {@link TimerState#isRunning()} will both return {@code false}.
     * </p>
     */
    STARTING,

    /**
     * <p>
     * Inspection is enabled, the hold-to-start behaviour is enabled and a
     * touch down event was received in the {@link #UNUSED} stage causing it
     * to transition through {@link #STARTING}. If this stage is still active
     * after the hold-to-start duration has elapsed, the stage will transition
     * automatically to {@link #INSPECTION_READY_TO_START}. If a touch up event
     * is received before the hold-to-start duration has elapsed, the stage will
     * transition to {@link #CANCELLING}. If a touch down event is received, it
     * will be ignored, as only a touch up is expected. There is no maximum time
     * limit for holding in this stage, though a hold duration of about 0.5
     * seconds is recommended. If the touch event is cancelled (i.e., if the
     * touch down that caused the transition to this stage is interpreted as a
     * "swipe" action and will not be followed by a touch up event), the stage
     * will transition to {@link #CANCELLING} and a new timer state will be
     * created at the {@link #UNUSED} stage.
     * </p>
     * <p>
     * The transition through {@code STARTING} notifies the UI via a call to
     * {@code onTimerSet(TimerState)}. When the transition is complete, this
     * stage will then notify {@code onTimerCue(TimerCue)} with
     * {@link TimerCue#CUE_INSPECTION_HOLDING_FOR_START}. The UI may prepare the
     * display for the start of a solve, though the inspection countdown will
     * not be fully ready to start until the hold-to-start period has elapsed.
     * The timer is never restored to this stage; the state is restored to the
     * {@code UNUSED} stage instead.
     * </p>
     */
    INSPECTION_HOLDING_FOR_START,

    /**
     * <p>
     * Inspection is enabled and the inspection period is ready to start.
     * If hold-to-start behaviour is enabled, the timer state transitions
     * automatically to this stage from {@link #INSPECTION_HOLDING_FOR_START}
     * after the minimum hold-to-start period has elapsed. If hold-to-start is
     * disabled, the timer state transitions to this stage directly from
     * {@link #STARTING}. If a touch up event is received, the timer state will
     * transition to the {@link #INSPECTION_STARTED} stage and the inspection
     * countdown will begin. There is no time limit for holding the touch down
     * in this stage. If a touch down event is received, it will be ignored, as
     * only a touch up is expected. If the touch event is cancelled, the stage
     * will transition to {@link #CANCELLING} and the previous timer state (at
     * either the {@link #UNUSED} or {@link #STOPPED} stage) will be restored.
     * </p>
     * <p>
     * The transition through {@code STARTING} (if it occurs) notifies the UI
     * via a call to {@code onTimerSet(TimerState)}. On the transition to this
     * stage, the timer will call back to {@link SolveHandler#onSolveStart()}
     * and then notify {@code onTimerCue(TimerCue)} with
     * {@link TimerCue#CUE_INSPECTION_READY_TO_START}. The timer is never
     * restored to this stage; the state is restored to the {@code UNUSED}
     * stage instead.
     * </p>
     */
    INSPECTION_READY_TO_START,

    /**
     * <p>
     * The inspection period has started, the inspection countdown is running.
     * </p>
     * <p>
     * If the inspection countdown reaches zero, a "2 SECONDS" penalty will be
     * incurred and the stage will remain unchanged for a further overrun period
     * of two seconds. If the stage has not reached {@link #SOLVE_STARTED}
     * (or beyond) within two seconds of the countdown reaching zero, an
     * additional "DNF" penalty will be incurred and the stage will transition
     * through {@link #STOPPING} to {@link #STOPPED}, ending the solve attempt.
     * </p>
     * <p>
     * If hold-to-start is disabled, a touch down event triggers a transition to
     * {@link #INSPECTION_SOLVE_READY_TO_START}. If hold-to-start is enabled, a
     * touch down transitions to {@link #INSPECTION_SOLVE_HOLDING_FOR_START}.
     * The inspection countdown (including any overrun) continues to run during
     * those transitions.
     * </p>
     * <p>
     * If a touch up event is notified while at {@code INSPECTION_STARTED},
     * the stage remains unchanged. A touch up is not expected, as this stage
     * should have been entered in response to a touch up event from
     * {@link #INSPECTION_READY_TO_START}, or on returning early from
     * {@code INSPECTION_SOLVE_HOLDING_FOR_START}, and that touch up event
     * should have been consumed.
     * </p>
     * <p>
     * While this stage is running, the UI will be notified regularly of the
     * remaining inspection time via call-backs to
     * {@link OnTimerRefreshListener#onTimerRefreshInspectionTime(long, long)}.
     * </p>
     * <p>
     * Each transition to {@code INSPECTION_STARTED} (including when returning
     * early from {@code INSPECTION_SOLVE_HOLDING_FOR_START}) triggers a
     * call-back to {@code onTimerSet(TimerState)}. On the first transition to
     * this stage from {@code INSPECTION_READY_TO_START}, the timer will notify
     * {@link TimerCue#CUE_INSPECTION_STARTED} to {@code onTimerCue(TimerCue)}.
     * That cue is not notified on returning early to this stage from
     * {@code INSPECTION_SOLVE_HOLDING_FOR_START}, in that case,
     * {@link TimerCue#CUE_INSPECTION_REASSERTED} will be notified instead, to
     * indicate that the "half-way" state between the inspection and solve
     * periods has returned to the normal operation of the inspection period.
     * The timer state may be saved and restored to {@code INSPECTION_STARTED}.
     * </p>
     * <p>
     * Further time-dependent cues may also be notified while this stage
     * or the related {@code INSPECTION_SOLVE_HOLDING_FOR_START} or
     * {@code INSPECTION_SOLVE_READY_TO_START} stages are active. If the
     * inspection period is longer than seven seconds, the timer will notify
     * {@link TimerCue#CUE_INSPECTION_7S_REMAINING} when seven seconds remain.
     * If the inspection period is longer than three seconds, the timer will
     * notify {@link TimerCue#CUE_INSPECTION_3S_REMAINING} when three seconds
     * remain. If the inspection countdown reaches zero and the two-second
     * overrun period begins (and a "+2" penalty is incurred), the timer will
     * notify {@link TimerCue#CUE_INSPECTION_OVERRUN}. If the overrun period
     * times out (and a "DNF" penalty is incurred), the timer will notify
     * {@link TimerCue#CUE_INSPECTION_TIME_OUT}.
     * </p>
     */
    INSPECTION_STARTED,

    /**
     * <p>
     * The inspection countdown is running, hold-to-start behaviour is enabled
     * and a touch down event was received at the {@link #INSPECTION_STARTED}
     * stage. If this holding stage is still active after the hold-to-start
     * duration has elapsed, the stage transitions to
     * {@link #INSPECTION_SOLVE_READY_TO_START}. If a touch up event is received
     * before the hold-to-start duration has elapsed, the stage will return to
     * {@code INSPECTION_STARTED} (notifying the relevant cue described for that
     * stage). If a touch down event is received, it will be ignored (it is not
     * expected, as a touch down event should have caused this stage to be
     * entered and the touch should still remain down). If the touch event is
     * cancelled, the stage will return to {@code INSPECTION_STARTED} and the
     * inspection countdown will continue.
     * </p>
     * <p>
     * If the inspection countdown reaches zero, overruns or times out, the same
     * actions are taken as described for the {@code INSPECTION_STARTED} stage.
     * Notification of refresh requests and the time-dependent cues continue
     * during this stage, also as described for {@code INSPECTION_STARTED}.
     * </p>
     * <p>
     * On each transition to this stage, the puzzle timer notifies
     * {@link TimerCue#CUE_INSPECTION_SOLVE_HOLDING_FOR_START}. The timer
     * state is never restored to this stage; the state is restored to the
     * {@code INSPECTION_STARTED} stage instead.
     * </p>
     */
    INSPECTION_SOLVE_HOLDING_FOR_START,

    /**
     * <p>
     * The inspection countdown is running and a touch down event was received
     * at the {@link #INSPECTION_STARTED} stage. The state will first transition
     * through {@link #INSPECTION_SOLVE_HOLDING_FOR_START} if hold-to-start
     * behaviour is enabled. If a touch up event is received, the stage will
     * transition to {@link #SOLVE_STARTED}. If a touch down event is received,
     * it will be ignored. If the touch event is cancelled, the stage will
     * return to {@code INSPECTION_STARTED} and the inspection countdown will
     * continue.
     * </p>
     * <p>
     * If the inspection countdown reaches zero, overruns or times out, the same
     * actions are taken as described for the {@code INSPECTION_STARTED} stage.
     * Notification of refresh requests and the time-dependent cues continue
     * during this stage, also as described for {@code INSPECTION_STARTED}.
     * </p>
     * <p>
     * On each transition to this stage, the puzzle timer notifies
     * {@link TimerCue#CUE_INSPECTION_SOLVE_READY_TO_START}. The timer state
     * is never restored to this stage; the state is restored to the
     * {@code INSPECTION_STARTED} stage instead.
     * </p>
     */
    INSPECTION_SOLVE_READY_TO_START,

    /**
     * <p>
     * There was no inspection period, the hold-to-start behaviour is enabled
     * and a touch down event was received in the {@link #UNUSED} stage which
     * transitioned to this stage via {@link #STARTING}. If this stage is still
     * active after the hold-to-start duration has elapsed, the stage will
     * transition automatically to {@link #SOLVE_READY_TO_START}. If a touch up
     * event is received before the hold-to-start duration has elapsed, or if
     * the touch event is cancelled, the state will transition to the
     * {@link #CANCELLING} stage and the previous timer state (at either the
     * {@link #UNUSED} or {@link #STOPPED} stage) will be restored. There is no
     * maximum time limit for holding in this stage. If a touch down event is
     * received, it will be ignored.
     * </p>
     * <p>
     * The transition through {@code STARTING} notifies the UI via a call to
     * {@code onTimerSet(TimerState)}. On the transition to this stage, the
     * timer will notify {@code onTimerCue(TimerCue)} with
     * {@link TimerCue#CUE_SOLVE_HOLDING_FOR_START}. The timer is never
     * restored to this stage; the state is restored to the {@code UNUSED}
     * stage instead.
     * </p>
     */
    SOLVE_HOLDING_FOR_START,

    /**
     * <p>
     * Inspection is disabled and the solve timer is ready to start. If
     * hold-to-start behaviour is enabled, the timer state transitions
     * automatically to this stage from {@link #SOLVE_HOLDING_FOR_START}
     * after the minimum hold-to-start period has elapsed. If hold-to-start
     * is disabled, the timer state transitions to this stage directly from
     * {@link #STARTING}. If a touch up event is received, the timer state will
     * transition to the {@link #SOLVE_STARTED} stage and the solve timer will
     * begin. There is no time limit for holding the touch down in this stage.
     * If a touch down event is received, it will be ignored, as only a touch up
     * is expected. If the touch event is cancelled, the stage will transition
     * to {@link #CANCELLING} and the previous timer state (at either the
     * {@link #UNUSED} or {@link #STOPPED} stage) will be restored.
     * </p>
     * <p>
     * The transition through {@code STARTING} notifies the UI via a call
     * to {@code onTimerSet(TimerState)}. On the transition to this stage,
     * the timer will call back to {@link SolveHandler#onSolveStart()} and
     * then notify {@code onTimerCue(TimerCue)} with
     * {@link TimerCue#CUE_SOLVE_READY_TO_START}. The timer is never restored
     * to this stage; the state is restored to the {@code UNUSED} stage instead.
     * </p>
     */
    SOLVE_READY_TO_START,

    /**
     * <p>
     * The solve attempt has started and the solve timer is running. If a touch
     * down event is detected, the timer is stopped and the stage transitions
     * through {@link #STOPPING} to {@link #STOPPED}. If a touch up event is
     * detected, the stage remains unchanged. A touch down event is expected
     * before the next touch up event and action is only taken on that touch
     * down event. If the touch event is cancelled after a touch down event,
     * the effect of the touch down event is not reverted: the timer remains
     * stopped.
     * </p>
     * <p>
     * While this stage is running, the UI will be notified regularly of the
     * elapsed solve time via call-backs to
     * {@link OnTimerRefreshListener#onTimerRefreshSolveTime(long, long)}.
     * </p>
     * <p>
     * When the state transitions to this stage, or is restored to this stage,
     * the puzzle timer notifies {@code onTimerSet(TimerState)}. On the first
     * transition to this stage, the timer notifies {@code onTimerCue(TimerCue)}
     * with {@link TimerCue#CUE_SOLVE_STARTED}. If the timer is resumed from the
     * {@link #SOLVE_PAUSED} stage and returns to {@code SOLVE_STARTED}, it
     * notifies {@link TimerCue#CUE_SOLVE_RESUMED}. The timer state may be
     * saved and restored to this {@code SOLVE_STARTED} stage.
     * </p>
     */
    SOLVE_STARTED,

    /**
     * <p>
     * The solve attempt has started and the solve timer was running but has
     * now been paused, so elapsed time is not being accumulated. The timer is
     * paused in response to an explicit call to {@link PuzzleTimer#pause()};
     * it cannot be paused in response to regular touch events. Refresh
     * notifications are suspended while at this stage.
     * </p>
     * <p>
     * If a touch up event (following a touch down event) is detected, or if an
     * explicit call is made to {@link PuzzleTimer#resume()}, the solve timer
     * resumes running from the elapsed time that was recorded at the instant
     * that it was paused, the stage returns to {@link #SOLVE_STARTED} and the
     * puzzle timer notifies {@link TimerCue#CUE_SOLVE_RESUMED}.
     * </p>
     * <p>
     * When the state transitions to this stage, or is restored to this stage,
     * the puzzle timer notifies {@code onTimerSet(TimerState)}. On each
     * transition to this stage, the timer notifies {@code onTimerCue(TimerCue)}
     * with {@link TimerCue#CUE_SOLVE_PAUSED}. The timer state may be saved and
     * restored at the {@code SOLVE_PAUSED} stage.
     * </p>
     */
    SOLVE_PAUSED,

    /**
     * <p>
     * The timer was cancelled and the solve attempt abandoned. This is a
     * transient stage that rolls back the timer state to what it was before
     * the solve attempt began. The reverted timer state will be at either the
     * {@link #UNUSED} or {@link #STOPPED} stage. The timer can be cancelled
     * when in a waking state by calling {@link PuzzleTimer#cancel()}, or if
     * the touch event is cancelled when at one of the holding-for-start or
     * read-to-start stages.
     * </p>
     * <p>
     * When the timer state transitions through this stage, the timer notifies
     * {@code onTimerCue(TimerCue)} with {@link TimerCue#CUE_CANCELLING} before
     * the state is rolled back. The timer state cannot be saved or restored at
     * the {@code CANCELLING} stage.
     * </p>
     */
    CANCELLING,

    /**
     * <p>
     * The timer has stopped and a new solve result is available. The attempt
     * was not cancelled. The elapsed time recorded for the solve attempt ceases
     * to be updated. There may be no elapsed time recorded if the inspection
     * period timed out and an inspection stage transitioned directly to this
     * stage to record a "DNF" solve. This is a transient stage that commits
     * the new timer state (discarding the previous timer state) and then
     * automatically transitions to {@link #STOPPED}. Touch events are ignored
     * at this stage.
     * </p>
     * <p>
     * When the timer state transitions through this stage, the timer notifies
     * {@code onTimerCue(TimerCue)} with {@link TimerCue#CUE_STOPPING} before
     * the state is committed, the stage transitions to {@code STOPPED} and a
     * call-back is issued to {@link SolveHandler#onSolveStop(Solve)}. The timer
     * state cannot be saved or restored at the {@code STOPPING} stage.
     * </p>
     */
    STOPPING,

    /**
     * <p>
     * A solve attempt was completed and a result is available for display.
     * If a touch down event is detected, this timer state is saved for possible
     * later roll-back, a new timer state is created at the {@link #UNUSED} and
     * the timer immediately transitions to the {@link #STARTING} stage.
     * </p>
     * <p>
     * When the state transitions to this stage, or is restored to this stage,
     * the puzzle timer notifies {@code onTimerSet(TimerState)}. No timer cues
     * or {@link SolveHandler} call-backs are notified (those are only notified
     * if there is a transition through {@link #STOPPING} the first time a solve
     * attempt is completed). The timer state may be saved and restored at the
     * {@code STOPPED} stage.
     * </p>
     */
    STOPPED,
}
