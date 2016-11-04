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
 * {@link #onTimerCue(TimerCue, TimerState)} is notified when the state of
 * the timer changes in some way. Details of the current timer state are
 * given on each notification. The UI cues are expected to drive all of the
 * changes to the presentation of the timer, aside from the simpler task of
 * refreshing the displayed time. UI cues are not intended to support every
 * possible requirement of the higher-level components (such as a {@code
 * Fragment} that manages the timer widget). The cues do not represent the
 * "states" of the component's state machine; they are triggers for
 * transitions between states that are defined by the component.
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
    void onTimerCue(@NonNull TimerCue cue, @NonNull TimerState timerState);

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
    void onTimerSet(@NonNull TimerState timerState);

}
