package com.aricneto.twistytimer.timer;

import android.support.annotation.NonNull;

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
    // IMPORTANT: "onTimerCue" sometimes seems redundant for cues like
    // "CUE_STARTING" where there is already a dedicated "onTimerStarted".
    // However, the intention is to separate the "definitive" call-backs from
    // the "less reliable" cues such that the cues are used for things like
    // visual transitions and sound effects and the other are used for
    // initialisation and management of the solve data.

    // FIXME: Seriously consider dropping "TimerState" parameter. That would
    // make it very clear that a "cue" is only a prompt to change some UI
    // state or perform some UI action and has nothing to do with updating the
    // time value (done by "onTimerSet" and "OnTimerRefreshListener.*") or
    // saving/restoring solves, etc. Would this be workable?
//    void onTimerCue(@NonNull TimerCue cue, @NonNull TimerState timerState);
    void onTimerCue(@NonNull TimerCue cue);

    // FIXME: Should the "onTimerCue" be sandwiched between some new
    // "onTimerCreated" (or something) method and the later
    // "onTimerNewResult" (etc.) methods? It may be useful to have something
    // to call to initialise the current timer state before it is run.
    // However, if the sequence is "onTimerCue", "onTimerNewResult",
    // "onTimerCue", "onTimerNewResult", "onTimerCue", "onTimerNewResult",
    // etc., then isn't "onTimerCue" already "sandwiched"? It just needs to
    // be allowed to call "onTimerNewResult" prior to any "onTimerCue" to set
    // up the initial, quiet timer state. However, this is a bit less obvious
    // if the timer is being restored while it is running, as there is no
    // desire to save any "result". Perhaps this is just an issue for the FSM
    // in the fragment. Let's see how it turns out.

    // OK: This means "update the timer display to match the given state". It
    // can be called after "wake()" or "reset()" or just after a state "push
    // ()" or "pop()". Yeah, "set" sounds right for all of those cases.
    //
    // This is, therefore, the definitive "just-show-this-state" method. It
    // differs from the cues in that it is guaranteed to fire at the right
    // time, every time; it does not require any special operations on the
    // data, unlike "onTimerStarted" (create Solve) , "onTimerStopped" (save
    // solve), or "onTimerCancelled" (FIXME: WTF?) is this needed any more?
    // Does it just get merged into "onTimerSet" in the same way that
    // "onTimerReset" did? What specific operation would be performed on the
    // data to handle a "cancel()"? Isn't it just a case of discarding it and
    // showing the "popped" state (which could be "UNUSED")? There could be a
    // cue fired to allow a sound effect, or something, but otherwise not
    // much is needed. The only issue might be that of the solve is cancelled
    // after the timer has started, the restored previous solve might cause
    // confusion. Perhaps there is a case to be made for discarding the
    // previous solve when "onTimerStarted" gets called, so cancelling after
    // that would just return to a "0.00" state with no editing controls.
    // OTOH, a cue could be used to display some toast that would identify
    // the restored state as being from the previous solve attempt. Still, it
    // might be safer to disable the editing controls in case it is deleted
    // by accident.

    // FIXME: Document that "timerState" is never a cancelled timer state.

    // FIXME: Document that this is called each time the penalties change,
    // even if the inspection timer is running. In such cases, it will be
    // called *before* the respective cue (e.g., "CUE_INSPECTION_OVERRUN").

    // FIXME: This should be called when...
    //
    // 1. A timer is woken from its sleeping state.
    //
    // 2. A new, unused timer is created (e.g., after "reset()" or "push()").
    //
    // 3. A timer is restored (though it should be restored into a sleeping
    //    state, so "wake()" triggers the call-back).
    //
    // 4. When a penalty has been incurred (allowing updates to the headline).
    //
    // 5. When the inspection timer is presented (allowing updates to the
    //    headline).
    //
    // 6. When the solve timer is presented (headline, again).
    //
    // 7. When the timer is stopped (final "result" headline, and now include
    //    time penalties in reported elapsed solve time).
    //
    // If inspection is enabled, "Inspection" should appear in the headline if
    // the inspection time is showing. Therefore, on the "HOLDING" and "READY"
    // stages. However, not on the "INSPECTION_SOLVE_HOLDING_FOR_START" or
    // "INSPECTION_SOLVE_READY_TO_START" stages, as inspection should still be
    // showing. However, is that not really just up to the UI? The test would
    // be that "isInspectionRunning()" would still be true.
    //
    // If I always call "onTimerSet" or any stage transition---or maybe any
    // "setUp()" call---and for any new penalty, then that might be
    // sufficient. For the most part, there is not much repetition of stages,
    // so there is not much overhead in doing that. HOWEVER, there might be a
    // problem with things like calling "onTimerSet" for a cancelled stage
    // instead of the restored state. Perhaps do it piecemeal and skip some of
    // the stages where it is not necessary.
    //
    // "wake()" can only restore into these stages:
    //
    //     UNUSED
    //     INSPECTION_STARTING
    //     INSPECTION_STARTED
    //     SOLVE_STARTING
    //     SOLVE_STARTED
    //     STOPPED
    //
    // Therefore, if "wake()" calls "onTimerSet", there will be no need to call
    // it in "setUp" for these stages, unless it is necessary for these stages.
    //
    // The set-up for each of these stages transitions to another stage
    // immediately, so "sleep()" never saves the timer state at these stages.
    //
    //     STARTING
    //     CANCELLING
    //     STOPPING
    //
    // If "sleep()" is called from these stages, the timer will be transitioned
    // to "STOPPED" (via "CANCELLING") before its state is saved:
    //
    //     INSPECTION_HOLDING_FOR_START
    //     INSPECTION_READY_TO_START
    //     SOLVE_HOLDING_FOR_START
    //     SOLVE_READY_TO_START
    //
    // If "sleep()" is called from these stages, the timer will be transitioned
    // to "INSPECTION_STARTED" before its state is saved:
    //
    //     INSPECTION_SOLVE_READY_TO_START
    //     INSPECTION_SOLVE_HOLDING_FOR_START
    //
    // "onTimerSet" is required as follows:
    //
    //     UNUSED **
    //         An unused timer may have a specific headline (or no headline)
    //         that may be different from other headlines. The inspection time
    //         should be blank. The solve time may be "0.00", or maybe "-.--",
    //         or some other such indication that the timer is not used.
    //
    //     INSPECTION_HOLDING_FOR_START ++
    //     INSPECTION_READY_TO_START ++
    //     INSPECTION_STARTING **
    //     INSPECTION_STARTED **
    //         All of these states will show the "Inspection" headline and
    //         normal inspection time.
    //
    //     SOLVE_HOLDING_FOR_START ++
    //     SOLVE_READY_TO_START ++
    //     SOLVE_STARTING **
    //     SOLVE_STARTED **
    //         All of these stages will show the running solve headline and
    //         the running elapsed time (which *excludes* penalties). The
    //         inspection time should be blank. (Either inspection is not
    //         enabled, or "isSolveRunning" will be true.) The elapsed solve
    //         time may be masked or shown with lower precision than when
    //         stopped or unused.
    //
    //     STOPPED **
    //         The final "result" headline will be shown and the elapsed solve
    //         time will *include* penalties. The inspection time will be blank.
    //         FIXME: The concern here is that "STOPPED" is used when at the
    //         end of a cancelled timer state, but the restored state will be
    //         "STOPPED" or "UNUSED", so it would be wrong to call "onTimerSet"
    //         twice (or would it). Either it is not wrong, or it needs to
    //         change (which would be awkward), or maybe a new "CANCELLED"
    //         stage is needed to avoid the problem. A timer's state could not
    //         be restored at the "CANCELLED" stage, as the state is discarded
    //         (popped) before "sleep()" has a chance to be called.
    //
    //     INSPECTION_SOLVE_READY_TO_START
    //     INSPECTION_SOLVE_HOLDING_FOR_START
    //     STARTING
    //     CANCELLING
    //     STOPPING
    //         These never fire "onTimerSet". The "SOLVE" variants do not affect
    //         any of the presentation of the continuing inspection countdown,
    //         other than to show the "0.00" solve time alongside the normal
    //         inspection headline and remaining inspection time. As "wake()
    //         does not restore into those stages either, there is no need to
    //         call "onTimerSet" for those stages. The only thing to note is
    //         that the elapsed solve time should be formatted in readiness for
    //         it being shown (as "0.00") in these stages. However, the time
    //         should not be masked until the solve timer actually starts.
    //
    //
    // ** = Stage can be restored from saved state and will be set up again by
    // "wake()". The only concern is that "onTimerSet" does not, in the normal,
    // course of events, need to be called from both a "...STARTING" and a
    // "...STARTED" stage, it only needs to be called from the first one.
    //
    // ++ = Stage is only entered via transition from "STARTING" stage. In fact,
    // "STARTING" redirects to a READY_TO_START stage if hold-to-start behaviour
    // is disabled, otherwise it redirects to a HOLDING_FOR_START stage. If the
    // latter, an "alarm tick" is used to trigger the next transition. This
    // means that one one of each pair of HOLDING/READY stages is entered via
    // "STARTING", which makes "STARTING" a good place to add a call to
    // "onTimerSet" in respect of these stages. This also works well, as these
    // stages are not restored after a "wake()", so there would be no other
    // place where "onTimerSet" would be called.
    //
    // As the TimerCue that triggers transitions may need to have the text in
    // the right format first, "onTimerSet" should always be called first,
    // except in the case of the final "bracketing" call when "STOPPED".
    //

    // IMPORTANT: Document that this is only ever called in a small sub-set of
    // possible timer states/stages.
    void onTimerSet(@NonNull TimerState timerState);

    // IMPORTANT: This can only be called when the timer is running, but it can
    // be called for *any* running timer stage (though really only during the
    // inspection stages, as that is the only time a running timer can incur a
    // penalty given the current app functionality).
    // NOTE: Was "onTimerPenaltyIncurred", but what if a penalty was annulled?
    // It doesn't happen now, but maybe in the future....
    void onTimerPenalty(@NonNull TimerState timerState);

}
