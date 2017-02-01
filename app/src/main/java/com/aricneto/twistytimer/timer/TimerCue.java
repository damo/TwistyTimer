package com.aricneto.twistytimer.timer;

/**
 * <p>
 * The timer cues that are notified to the user-interface when the state of the
 * time changes at key points in its life-cycle. The cues are notified in a
 * call-back to {@link OnTimerEventListener#onTimerCue(TimerCue)}. The UI can
 * respond by triggering some visual or audible cue to alert the user to the
 * change.
 * </p>
 * <p>
 * </p>
 * <p>
 * FIXME: Something about what to do and not to do on receiving a cue: these
 * are for visual and audio updates, only. They should not be depended on to
 * fire and should not trigger changes to data, etc.
 * </p>
 * <p>
 * The purpose of timer cues may be most easily understood in the context of
 * using them to support the playing of sound effects in response to a user's
 * interactions with the puzzle timer. FIXME: elaborate.
 * </p>
 * <h2>Cue Sequence When Inspection is Enabled</h2>
 * <h2>Cue Sequence When Inspection is Disabled</h2>
 * @author damo
 */
public enum TimerCue {
    // NOTE: These cues are fired during "TimerStage" transitions. These are,
    // for the most part, a public subset of the timer state-machine's
    // "TimerStage" enum, though there are a few extras that are specific to
    // the needs of the UI. However, "Stage" is an internal ("private") enum
    // for the "PuzzleTimer" FSM, so the documentation of this public "TimerCue"
    // enum does not refer to those stages, or even to the fact that these
    // cues are fired (mostly) on stage transitions, as that is all
    // information behind the facade of this black box FSM.

    // FIXME: NOTE-TO-SELF!
    //
    // The current behaviour of the UI is that when hold-to-start is enabled,
    // the first touch down does not hide the tool-bars, etc., i.e., it does
    // not "go full-screen". Only if the hold is long enough and
    // "ready-to-start" is reached does the UI go full-screen. This is only
    // the case when inspection is not enabled, as if inspection is enabled
    // the hold-for-start will happen during inspection when the timer has
    // already gone full-screen.
    //
    // The problem is when there is no inspection period. While holding for
    // start it is nice to show "0.00", so some cue is required (i.e.,
    // CUE_SOLVE_HOLDING_FOR_START). However, if the hold is for too short a
    // time, this must be backed out and "onTimerCancelled()" is currently
    // used for that purpose, as it signals: "Show the result of the previous
    // solve, but do not attempt to save it, as it is already saved."
    // However, it is messy to try to send that cue via the "CANCELLING"
    // stage, as that confuses the issue about whether or not "CUE_STOPPING"
    // should be sent or not.
    //
    // Ideally, I want to fire cues in such a way that the UI can be
    // supported if it wants to:
    //
    //   a) Go full-screen when the hold-to-start period starts.
    //   b) Wait to go full-screen until the hold-to-start period ends,
    //      i.e., when ready-to-start.
    //   c) Wait to go full-screen until the ready-to-start period ends,
    //      i.e., when started.
    //
    // For each of these choices, there must be one, and only one cue that
    // can be relied on to go full-screen and, correspondingly, one and only
    // one cue that can be relied upon to exit the full-screen presentation.
    // There must also be the necessary provision of the timer state at the
    // "end".
    //
    // For a), use "CUE_SOLVE_HOLDING_FOR_START" to go full-screen. Perhaps
    // if this is cancelled, fire a corresponding
    // "CUE_SOLVE_HOLDING_FOR_START_CANCELLED" cue after notifying
    // "onTimerCancelled". However, is "CUE_STOPPING" then fired? It should
    // not be, but how could that be arranged if we still end in the
    // "STOPPED" stage? wouldn't it make the state handling a bit messier? I
    // think "CUE_STARTING" should only fire *after* a ready-to-start stage
    // and "CUE_STOPPING" should fire immediately after any
    // "onTimerNewResult", "onTimerCancelled" or "onTimerReset" call. FIXME:
    // Actually, does it fire after either of the latter two? How about
    // firing "CUE_CANCELLED" if cancelled and "CUE_RESET" if reset? These
    // duplicate the other listener methods, but it allows the UI to separate
    // things like UI state management based only on cues from model
    // management (e.g., saving or restoring "Solve" objects) from that UI
    // stuff. In fact, should these two sets of methods be separated from
    // each other by creating two separate interfaces? OnTimerResult and
    // OnTimerCue, for example. The refresh calls could go into the latter.
    // What does the TimerView need in this respect? I think that just one
    // "CUE_STOPPING" is needed (not "CUE_RESET" and "CUE_CANCELLED") as the
    // stopped condition is always the same from the point of view of the
    // widget: show only the elapsed time, the penalties, etc.
    //
    // Is "CUE_STARTING" needed, then? It would probably provide some balance,
    // I suppose.
    //
    // To be honest, all of this would be a lot easier if the UI just managed
    // its own state and if the cues were just linked to timer life-cycle
    // events. If the UI created its own mini state machine, it would allow
    // the cues to be kept under control.

    // OK....
    //
    // The point of the timer cues is to provide just that, "cues", or "hints",
    // to the timer UI to allow it TO DRIVE ITS OWN FINITE STATE MACHINE. Just
    // like the "onTouch*", "onTick", etc. that are notified to "PuzzleTimer",
    // it is up to the UI to actually manage its state. There is no point in
    // trying to do both things from the timer. Therefore, the timer need
    // only fire cues when something has changed in a manner that probably
    // requires SOME SORT of change to the UI. However, it is not the job of
    // the "PuzzleTimer" to do any hand-holding or to pollute itself with the
    // internal, and unpredictable, concerns of the UI. The primary focus
    // should be on the needs of the "TimerView", while a few cues might be
    // used to fill in any *glaring* gaps in the FSM within the
    // "TimerFragment", but only if they contribute significantly to some
    // simplification of that FSM and only if the cue is relatively generic.
    //
    // IMPORTANT: A key consequence of this is that when saved state is
    // restored, the fragment can save and restore its own FSM state that was
    // established after a series of timer cues, so there will be no need to
    // re-fire the timer cues when the "PuzzleTimer" state is restored.
    //
    // FIXME: What about the cues that fire around the time of the "STOPPING"
    // or "CANCELLING" stages? In particular, is there a consistent approach
    // that should be taken to

    // FIXME: What about the cues consumed by the "TimerView"? Should the
    // "onTimerCue" and "onTimerRefresh*" be the only methods notified to the
    // widget? Should the "onTimerNewResult", etc. be reserved for fragment?
    // A clear separation might help to clarify the purpose of each subset of
    // the "TimerUIListener" API. It might also make a better case for the
    // widget to support adding listeners for two different sets of
    // call-backs and, perhaps, for adding more than one listener for each.
    // Perhaps, the "onTimerRefresh*" should be separated out. The refresh
    // operations are *solely* the concern of the "TimerView". Having
    // multiple listeners for those calls would complicate the feedback of
    // the required refresh rate.

    /**
     * <p>
     * Inspection is enabled and the user has touched down to begin the
     * hold-for-start period before starting the inspection countdown. The
     * inspection countdown has not started, and, if the touch is lifted up
     * before the hold-for-start period has elapsed, the countdown will not
     * start and {@link #CUE_STOPPING} will be notified. This cue is notified
     * only once per solve attempt and only if inspection is enabled
     * <i>and</i> if hold-for-start behaviour is enabled. As the timer may
     * not be subsequently started, this cue is <i>not</i> preceded by
     * {@link #CUE_STARTING}.
     * </p>
     * <p>
     * Expectation: The UI will show the inspection time holding at "15" (or
     * whatever the custom inspection time has been set to and in whatever
     * format is appropriate). The inspection time should not be highlighted,
     * as it will not be ready to start just yet.
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
     * Expectation: The UI will show the full inspection time (e.g., "15"),
     * perhaps highlighting it to indicate that lifting the touch up will
     * start the countdown.
     * </p>
     */
    CUE_INSPECTION_READY_TO_START,

    /**
     * <p>
     * The inspection period has started and the inspection timer is counting
     * down. This cue may be notified more than once, as when
     * {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START} is notified, but the
     * hold time is deemed too short. In that case, it will be followed by
     * this cue again, instead of {@link #CUE_INSPECTION_SOLVE_READY_TO_START}.
     * </p>
     * <p>
     * Expectation: The UI will show the current inspection time prominently,
     * perhaps reverting any highlight that was applied to the when
     * {@link #CUE_INSPECTION_READY_TO_START} was notified. The UI should
     * expect regular update notifications of the remaining inspection time,
     * as the countdown has begun. The puzzle start time ("0.00") should be
     * displayed with less prominence, if it is displayed at all.
     * </p>
     */
    CUE_INSPECTION_STARTED,

    /**
     * <p>
     * The inspection period is counting down and the user has touched down
     * to begin the hold-for-start period before starting the timing of the
     * solve attempt. The solve timer has not started, and, if the touch is
     * lifted up before the hold-for-start period has elapsed, the solve
     * timer will not start. All the while, the inspection countdown
     * continues to run and the UI may continue to receive notifications of
     * the remaining inspection time.
     * </p>
     * <p>
     * Expectation: The UI will continue to show (and update) the inspection
     * time, perhaps making the inspection time less prominent. The UI will
     * show the solve time holding at "0.00" (or in whatever format is
     * appropriate), perhaps making it more prominent than the inspection
     * period. The solve time should not be highlighted, as it will not be
     * ready to start just yet.
     * </p>
     */
    CUE_INSPECTION_SOLVE_HOLDING_FOR_START,

    /**
     * <p>
     * The inspection period has started and the inspection timer is counting
     * down. This cue may be notified more than once, as when
     * {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START} is notified, but the
     * hold time is deemed too short. In that case, it will be followed by
     * this cue again, instead of {@link #CUE_INSPECTION_SOLVE_READY_TO_START}.
     * </p>
     * <p>
     * Expectation: The UI will show the current inspection time prominently,
     * perhaps reverting any highlight that was applied to the when
     * {@link #CUE_INSPECTION_READY_TO_START} was notified. The UI should
     * expect regular update notifications of the remaining inspection time,
     * as the countdown has begun. The puzzle start time ("0.00") should be
     * displayed with less prominence, if it is displayed at all.
     * </p>
     */
    // FIXME: Document this properly.
    CUE_INSPECTION_REASSERTED,

    /**
     * <p>
     * The inspection period is counting down and the user has touched down
     * to indicate readiness to start the solve timer. If hold-for-start is
     * enabled, the required hold period has now elapsed. Lifting the touch
     * up will start the timer.
     * </p>
     * <p>
     * Expectation: The UI will continue to show (and update) the inspection
     * time, perhaps making the inspection time less prominent. The UI will
     * show the solve time holding at "0.00" (or in whatever format is
     * appropriate), perhaps making it more prominent than the inspection
     * period. The solve time may be highlighted to indicated that it will
     * start as soon as the touch is lifted.
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
     * elapsed, the judge calls "12 SECONDS"</i>'. These calls were
     * introduced into the regulations in 2008, replacing a call of "10
     * SECONDS" in the previous version. The notes in the version history
     * record, 'Art. A3d2, changed 15 to 13. New starting procedure, it takes
     * around 2 seconds to start the timer.' It seems that the intention was
     * to give enough advance notice to competitors to allow them to finish
     * inspection, put down the puzzle, place their hands on the timer, and
     * then begin the solve. The "8 SECONDS" and "12 SECONDS" calls are
     * therefore probably not intended to give notice that about 1/2 and 3/4
     * of the (rounded) inspection time have elapsed, rather they are similar
     * to the previous regulations that gave notice when there were 5- and
     * 0-seconds-to-go, only now the warnings are given 2-3 seconds earlier
     * to allow the competitor to wrap up and prepare the solve timer.
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
     * has not been rescinded by a subsequent {@link #CUE_INSPECTION_STARTED}
     * cue).
     * </p>
     */
    // Equivalent to "8 SECONDS" call allowing for variable inspection time.
    CUE_INSPECTION_7S_REMAINING,

    /**
     * <p>
     * The inspection period is counting down and only 3 seconds remain
     * before the countdown reaches zero.
     * </p>
     * <p>
     * See {@link #CUE_INSPECTION_7S_REMAINING} for (far) more details on
     * this and the related cue and the conditions under which it may not be
     * notified.
     * </p>
     * <p>
     * Expectation: The UI will continue to show (and update) the inspection
     * time. An audible or visual cue might be used to warn the user that the
     * inspection time is fast running out. The UI should display the solve
     * time as appropriate for the most recently notified cue (e.g., if
     * {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START} has been received and
     * has not been rescinded by a subsequent {@link #CUE_INSPECTION_STARTED}
     * cue).
     * </p>
     */
    // Equivalent to "12 SECONDS" call allowing for variable inspection time.
    CUE_INSPECTION_3S_REMAINING,

    /**
     * <p>
     * The inspection period is counting down and has reached zero. A +2
     * seconds penalty is incurred. The user must start the solve timer
     * within two seconds, or the inspection period will time out and a "DNF"
     * penalty will be incurred and the solve attempt terminated. This cue
     * may be notified after {@link #CUE_INSPECTION_SOLVE_HOLDING_FOR_START}
     * or {@link #CUE_INSPECTION_SOLVE_READY_TO_START} has been notified.
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
     * {@link #CUE_INSPECTION_STARTED} cue).
     * </p>
     */
    // FIXME: If a "hold" is lifted early, then "CUE_INSPECTION_STARTED" is
    // re-fired. However, it would be better if "CUE_INSPECTION_OVERRUN" were
    // re-fired instead if the time was overrun. Maybe it would be simpler to
    // have a new cue, "CUE_INSPECTION_REASSERTED" instead, as that would avoid
    // having "CUE_INSPECTION_STARTED" mean two things: started for the first
    // time having been cued as ready-to-start, or resumed after a hold was
    // cancelled. The former requires the start cue highlighting to be cleared,
    // but the latter requires the highlighting to be set depending on whether
    // or not there is an overrun. "CUE_INSPECTION_REASSERTED" would just mean:
    // "Show the inspection time at full scale AND LEAVE THE HIGHLIGHTING
    // UNCHANGED".
    CUE_INSPECTION_OVERRUN,

    // FIXME: Document this.
    CUE_INSPECTION_TIME_OUT,

    /**
     * <p>
     * The user has touched down to begin the hold-for-start period before
     * starting the timing of the solve attempt. There was no prior
     * inspection period. The solve timer has not started, and, if the touch
     * is lifted up before the hold-for-start period has elapsed, the solve
     * timer will not start and {@link #CUE_STOPPING} will be notified. As the
     * timer may not be subsequently started, this cue is <i>not</i> preceded
     * by {@link #CUE_STARTING}.
     * </p>
     * <p>
     * Expectation: The UI will show the solve time holding at "0.00" (or in
     * whatever format is appropriate). The solve time should not be
     * highlighted, as it will not be ready to start just yet. As there was
     * no inspection period, no inspection time will be shown.
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
     * indicated that it will start as soon as the touch is lifted. As there
     * was no inspection period, no inspection time will be shown.
     * </p>
     */
    CUE_SOLVE_READY_TO_START,

    /**
     * <p>
     * The solve timer has started and is recording the elapsed time for a
     * solve attempt.
     * </p>
     * <p>
     * Expectation: The UI will show (and update) the elapsed solve time. If
     * the time was highlighted for a prior ready-to-start cue, that
     * highlight should be reverted. If there was a prior inspection period,
     * the inspection time will not be shown (though indications may be shown
     * for any "+2" penalties incurred before starting the solve timer).
     * </p>
     */
    CUE_SOLVE_STARTED,

    // FIXME: Document these:
    CUE_SOLVE_PAUSED,
    CUE_SOLVE_RESUMED,

    /**
     * <p>
     * The timer has stopped running. This is notified only once per solve
     * attempt. If the hold-for-start behaviour is enabled and the "hold" was
     * lifted too early, this will also be notified to signal the end of the
     * attempt, even though the timer was never started. If inspection is
     * enabled, the cancelling a hold-for-start during the inspection period
     * that would start the solve timer will not cause this cue to fire. This
     * cue will always be the last cue notified <i>just before</i> a call to
     * either {@code onTimerNewResult()} or {@code onTimerCancelled()} on the
     * listener interface.
     * </p>
     * <p>
     * Note: if the timer is cancelled, this cue will report the timer state
     * that is <i>being cancelled</i>, not the previous timer state that will
     * be restored. The restored previous timer state will be reported by
     * {@link OnTimerEventListener#onTimerSet(TimerState)} just <i>after</i>
     * {@code CUE_STOPPING} is notified.
     * </p>
     * <p>
     * Expectation: The UI may take action to return to the normal state when
     * the timer is not running.
     * </p>
     */
    // FIXME: Only fire this *XOR* "CUE_STOPPING". That allows them to be treated
    // separately or together in the listener. If the listener were playing
    // sound effects, it might be necessary for it to hold state to figure out
    // if "CUE_CANCELLED" happened before "CUE_STOPPING" and then not play the
    // normal "stopped" sound effect in that case.

    // FIXME: Document (or otherwise accommodate) that the timer state passed
    // when notifying "CUE_CANCELLING" is in a relatively indeterminate state,
    // as the timer has not been stopped (or maybe it should be) and the solve
    // instance is not complete (maybe it should be deleted).
    CUE_CANCELLING,

    /**
     * <p>
     * The timer has stopped running. This is notified only once per solve
     * attempt. If the hold-for-start behaviour is enabled and the "hold" was
     * lifted too early, this will also be notified to signal the end of the
     * attempt, even though the timer was never started. If inspection is
     * enabled, the cancelling a hold-for-start during the inspection period
     * that would start the solve timer will not cause this cue to fire. This
     * cue will always be the last cue notified <i>just before</i> a call to
     * either {@code onTimerNewResult()} or {@code onTimerCancelled()} on the
     * listener interface.
     * </p>
     * <p>
     * Note: if the timer is cancelled, this cue will report the timer state
     * that is <i>being cancelled</i>, not the previous timer state that will
     * be restored. The restored previous timer state will be reported by
     * {@link OnTimerEventListener#onTimerSet(TimerState)} just <i>after</i>
     * {@code CUE_STOPPING} is notified.
     * </p>
     * <p>
     * Expectation: The UI may take action to return to the normal state when
     * the timer is not running.
     * </p>
     */
    CUE_STOPPING;
}
