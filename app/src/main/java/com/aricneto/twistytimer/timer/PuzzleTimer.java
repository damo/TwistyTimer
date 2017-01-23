package com.aricneto.twistytimer.timer;

import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.Solve;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aricneto.twistytimer.timer.TimerCue.*;
import static com.aricneto.twistytimer.timer.TimerStage.*;

/**
 * <p>
 * The puzzle timer is a finite-state machine (FSM) that models the timing of a
 * puzzle solve attempt from the inspection phase, through to the timing phase,
 * and on to post-timing operations, such as the application of penalties. The
 * FSM is driven by external inputs: touch events notified from the user
 * interface (such as the first tap that starts the timer), and "tick" events
 * from a {@link PuzzleClock}. State changes and timing updates are notified to
 * the user interface to allow it to synchronise its presentation of the timer
 * to the timer's state and to update the display of the time (and any
 * penalties).
 * </p>
 * <p>
 * Calls to methods such as {@link #cancel()} or {@link #onTouchDown()} are
 * handled asynchronously. While they run on the same main UI thread
 * (expected) of the caller, the event notification is posted to message
 * queue of the thread's {@code Looper} and handled some time after the
 * method returns to the caller. Typically, the method from which the call
 * was made will have itself been called directly or indirectly from the
 * handler of a queued message. Therefore, until that chain of calls
 * completes the handling of the message, the event notification cannot be
 * processed. This scheme ensures that there can be no unexpected and
 * hard-to-follow calls invoked on any listeners until after the method
 * returns. It also makes it far less likely that unintentional infinite
 * recursion can arise, such as when a {@code PuzzleTimer} method is called
 * from within a listener method that is then called back synchronously from
 * the {@code PuzzleTimer} method and on and on.
 * </p>
 * <p>
 * Notable exceptions to the asynchronous handling of external inputs are the
 * {@link #wake()} and {@link #sleep()} methods.
 * FIXME: Remind me: Why did I think "wake()" needed to be synchronous?
 * </p>
 * <p>
 * Not all methods calls are handled asynchronously. Those that are are
 * identified clearly in their method descriptions; the others can be assumed
 * to be synchronous.
 * </p>
 * <p>
 * Two listeners may be set to receive notifications as the timer runs:
 * </p>
 * <ul>
 *     <li>{@link #addOnTimerEventListener} adds a listener that will be
 *     notified of timer cues and the final event when the timer is stopped.
 *     These events will be of interest to the controller component (e.g., a
 *     {@code Fragment} or {@code Activity}) that manages the full life-cycle
 *     of a solve attempt. The events will also be of interest to the
 *     view/widget that is displaying the time value. As only one listener
 *     can be set, typically the "controller" should act as the listener and
 *     relay the notifications to other widgets, as necessary.</li>
 *     <li>{@link #setOnTimerRefreshListener} sets the listener that will be
 *     notified periodically to enable the display of the current elapsed
 *     time to be displayed. This is likely only of interest to the
 *     view/widget that is displaying the time value, so it is separated from
 *     the other notifications.</li>
 * </ul>
 * <h2>The Life-Cycle of a {@code PuzzleTimer}</h2>
 * <p>
 * On creation, the timer is placed into its "sleeping"
 * state; it must be placed into its "waking" state by calling {@link #wake()}
 * before it will begin to send notifications of timer cues, refreshes and other
 * events.
 * </p>
 * <h3>Saving the Instance State of a Timer</h3>
 * <h3>Restoring the Instance State of a Timer</h3>
 * <h3>Updating the Configuration Options of a Timer</h3>
 * <p>
 * </p>
 * <p>
 * If {@link #onRestoreInstanceState(Parcelable)} is
 * called after construction, the configuration values passed here take effect
 * for any <i>new</i> solve attempts, but the previously saved values will still
 * be in effect for any solve attempts that were still running when the state
 * was saved by {@link #onSaveInstanceState()}.
 * </p>
 * {@code sleep()} should
 * be called from the {@code onStop()} (FIXME: untrue?) life-cycle method of a
 * {@code Fragment} or {@code Activity}. Calling this method from {@code
 * onPause()} may not be appropriate in some cases, as the timer may still be
 * visible and may still need to be updated, but that decision is left to the
 * caller. Placing the timer into its sleeping state ensures that the events do
 * not occur while the fragment or activity is inactive. However, placing the
 * timer into its sleeping state involves notifying {@link #onTouchCancelled()},
 * which can lead to other FIXME: complete this.... </p> FIXME: Instantiation,
 * injection of configuration values, restoring state, wake/sleep, etc... FIXME:
 * How all this is best integrated into a Fragment/Activity. FIXME: Life-cycle
 * of the solve. Restoring state and verifying that solve still exists, etc.
 * FIXME: When to "cancel()": *only* on "Back" or other UI event; *not* in
 * "onDestroy()", etc. FIXME: "sleep()" needed before "onSaveInstanceState()" to
 * avoid restoring the timer to an unsafe state. Can this be automatic?
 * "onSaveInstanceState()" is not necessarily called *only* just before
 * "onStop()", so I dunno. Anyway, it is most likely that "sleep()" needs to go
 * into "onPaused()", so it occurs before "onSaveInstanceState()" and
 * "onStop()". Were it in "onStop()", it would likely be called *after*
 * "onSaveInstanceState()".
 * <h2>Associating a {@code Solve} Instance</h2>
 * <p>
 * FIXME: When to create a Solve. When to save a Solve (storing ID, etc.). How
 * to edit a Solve. FIXME: How to update the timer after editing a Solve. How to
 * delete a solve. FIXME: Should "onTimerNewResult" be "onTimerNewSolve" and
 * return a "Solve" instance? The solve instance can be saved with the timer
 * state. The Solve can notify the timer state (which may set itself as a change
 * listener) of any changes to its fields. If "TimerState.getSolve()" is null
 * or not makes a nice way to check how the timer should be updated when
 * "onTimerRestored" is called. Only set the solve instance from
 * "onTimerNewResult"? Maybe not: if the puzzle type and category changed since
 * it started what then? Does that even matter, though? It probably does in the
 * sense that the scramble might not match the puzzle type. It also means that
 * the scramble would need to be persisted separately until the solve ends.
 * Therefore, maybe have a new "onTimerNewAttempt" (or something) method that
 * will require the listener to create the partial Solve object. A flag might be
 * added to indicate if the solve is complete or not (that could go on
 * "TimerState").
 * </p>
 * <h2>Refreshing the Display for a Running Timer</h2>
 * <p>
 * </p>
 *
 * @author damo
 */
// All methods on this class should be called from the main UI thread, as
// they may trigger call-backs that are directed at views that update the UI.
@UiThread
public class PuzzleTimer implements PuzzleClock.OnTickListener {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = PuzzleTimer.class.getSimpleName();

    // FIXME: There is an issue with "losing track" of past events during
    // stage transitions. For example, if transitioning from
    // "INSPECTION_STARTED" to "INSPECTION_SOLVE_READY_TO_START" the
    // set-up/tear-down requires that a check be made to see if the
    // inspection period has overrun and if the UI has been notified of this
    // event already. The problem is that I removed the
    // "INSPECTION_OVERRUN_*" stages, so there is no specific stage
    // transition where such an event would be fired. Should all UI
    // notifications coincide with a stage transition? If so, I need a lot
    // more stages, or lots of little "helper flags" to track these events.
    //
    // I like the strict set-up/tear-down approach, as it is much easier to
    // comprehend (and no doubt to code and test). But it does mean that each
    // state that is torn down needs to leave some "foot prints" that the
    // next state can read to see if it needs to do anything in particular.
    // Perhaps extra states are not really necessary. Perhaps I could just
    // create a "set-of-one-time-events" (SOOTE) that contains a token for
    // each event that may be fired to the UI on just one occasion (i.e.,
    // pretty much everything except the periodic updates of the changing
    // elapsed time). As each event is notified, it can be deleted from the
    // set, so if it is not in the set, it can be assumed to have been
    // notified. This could be implemented using a simple enum for the events
    // and an EnumSet. This could double as the enum that defines the events
    // in a manner that allows the UI to figure out what to do (i.e., as an
    // alternative to a large number of call-back methods, just have one main
    // event call-back that takes this event value as a parameter). The
    // alternative would be a bit grim: there would need to be stages defined
    // for things like the "12 SECONDS" call (maybe
    // "INSPECTION_12_SECONDS_ELAPSED") complete with all of the "sub-stages"
    // that tack on "*_SOLVE_READY_TO_START", etc. That would be a nightmare.
    // In reality, I have enough stages to fire the critical events on the
    // transitions. Things like the "12 SECONDS" call could be left
    // unimplemented for now and using the SOOTE approach would make adding
    // them quite easy. Each set-up for an inspection stage transition would
    // just need to be sure to check if the event had fired or not and then
    // schedule the necessary "tick" event to handle it.

    // NOTE: The strict set-up/tear-down approach makes it simpler to save
    // and restore the timer state, as there is no need to figure out what
    // tick events may have been carried over from the previous stage before
    // the current stage at the instant the timer state was saved.

    /**
     * The map of valid stage transitions. For each current stage, there is a
     * set of valid "next" stages. Any attempt to transition from one stage to
     * another stage will result in an error if that transition is not defined
     * by this map.
     */
    // NOTE: This is not strictly necessary. It supports defensive coding
    // that should help to weed out bugs before the implementation matures. The
    // overhead is very minimal.
    private static final Map<TimerStage, Set<TimerStage>> VALID_NEXT_STAGES
        = new EnumMap<TimerStage, Set<TimerStage>>(TimerStage.class) {{
        // "UNUSED" is the starting stage. A timer can never return to this
        // stage. If a "hold" is cancelled, the timer will transition through
        // "CANCELLING" to "STOPPED" and a new "TimerState" will be needed
        // before starting again (see "JointTimerState.push()").
        put(UNUSED, EnumSet.of(
            STARTING
        ));

        put(STARTING, EnumSet.of(
            // Only if "hold-to-start" behaviour is enabled.
            INSPECTION_HOLDING_FOR_START,
            // Only if "hold-to-start" behaviour is enabled.
            SOLVE_HOLDING_FOR_START,
            INSPECTION_READY_TO_START,
            SOLVE_READY_TO_START
        ));

        put(INSPECTION_HOLDING_FOR_START, EnumSet.of(
            // "Hold" time exceeded the threshold.
            INSPECTION_READY_TO_START,
            // "Hold" time was too short, or "cancel()" called.
            CANCELLING
        ));

        put(INSPECTION_READY_TO_START, EnumSet.of(
            // Starts the inspection countdown.
            INSPECTION_STARTED,
            CANCELLING
        ));

        put(INSPECTION_STARTED, EnumSet.of(
            // "onTouchDown()" and hold-to-start enabled.
            INSPECTION_SOLVE_HOLDING_FOR_START,
            // "onTouchDown()" (or "hold" was long enough).
            INSPECTION_SOLVE_READY_TO_START,
            // Inspection time-out "DNF".
            STOPPING,
            // "cancel()" called.
            CANCELLING
        ));

        put(INSPECTION_SOLVE_HOLDING_FOR_START, EnumSet.of(
            // "Hold" time too short or "onTouchCancelled()".
            INSPECTION_STARTED,
            // "Hold" time exceeded the threshold.
            INSPECTION_SOLVE_READY_TO_START,
            // Inspection time-out "DNF".
            STOPPING,
            CANCELLING
        ));

        put(INSPECTION_SOLVE_READY_TO_START, EnumSet.of(
            // "onTouchCancelled()" called.
            INSPECTION_STARTED,
            // Starts solve timer. Stops inspection countdown.
            SOLVE_STARTED,
            // Inspection time-out "DNF".
            STOPPING,
            // "cancel()" called.
            CANCELLING
        ));

        put(SOLVE_HOLDING_FOR_START, EnumSet.of(
            // "Hold" time exceeded the threshold.
            SOLVE_READY_TO_START,
            // "Hold" too short, or call to "onTouchCancelled()" or "cancel()".
            CANCELLING
        ));

        put(SOLVE_READY_TO_START, EnumSet.of(
            // Starts the solve timer.
            SOLVE_STARTED,
            // "onTouchCancelled()" or "cancel()" called.
            CANCELLING
        ));

        put(SOLVE_STARTED, EnumSet.of(
            // Stops the solve timer.
            STOPPING,
            CANCELLING
        ));

        // The current cancelled timer state is discarded when it is at the
        // "CANCELLING" stage. It is replace by a timer state already at the
        // "UNUSED" or "STOPPED" stage, so there is no transition away from
        // "CANCELLING".
        put(CANCELLING,  EnumSet.noneOf(TimerStage.class));

        put(STOPPING, EnumSet.of(
            STOPPED
        ));

        // "STOPPED" is the terminal stage. The timer cannot be restarted
        // from this stage; it must first create a new "TimerState" using
        // "JointTimerState.push()" to begin at "UNUSED".
        put(STOPPED, EnumSet.noneOf(TimerStage.class));
    }};

    /**
     * The ID of the alarm tick event marking the end of the "hold-to-start"
     * waiting period.
     */
    private static final int TICK_ID_HOLDING_FOR_START_ALARM = 110;

    /**
     * The ID of the alarm tick event for the instant when there are 7 seconds
     * remaining for the normal inspection period.
     */
    private static final int TICK_ID_INSPECTION_7S_REMAINING_ALARM = 120;

    /**
     * The ID of the alarm tick event for the instant when there are 3 seconds
     * remaining for the normal inspection period.
     */
    private static final int TICK_ID_INSPECTION_3S_REMAINING_ALARM = 130;

    /**
     * The ID of the alarm tick event marking the end of the normal inspection
     * period. The inspection period may continue for a further two seconds, but
     * the solve time will incur a "+2 seconds" penalty.
     */
    private static final int TICK_ID_INSPECTION_OVERRUN_ALARM = 140;

    /**
     * The ID of the alarm tick event marking the end of the 2-second overrun of
     * the inspection period. The inspection period is now over and the solve
     * attempt will be recorded as a "DNF".
     */
    private static final int TICK_ID_INSPECTION_TIME_OUT_ALARM = 150;

    /**
     * The ID of the periodic tick events that will trigger updates to the
     * display of the running timer. These events will trigger a refresh of the
     * remaining inspection time or the elapsed solve time, depending which
     * phase of the timer is currently running.
     */
    private static final int TICK_ID_TIMER_REFRESH = 160;

    // Define "@TickID" primarily to ensure that the values of the constants
    // are all unique. There are a few other small benefits for code completion
    // and inspections, etc.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        TICK_ID_HOLDING_FOR_START_ALARM,
        TICK_ID_INSPECTION_7S_REMAINING_ALARM,
        TICK_ID_INSPECTION_3S_REMAINING_ALARM,
        TICK_ID_INSPECTION_OVERRUN_ALARM,
        TICK_ID_INSPECTION_TIME_OUT_ALARM,
        TICK_ID_TIMER_REFRESH
    })
    @interface TickID { }

    /** Message "what" to make {@link #cancel()} asynchronous. */
    private static final int MSG_WHAT_CANCEL = 100;

    /** Message "what" to make {@link #reset()} asynchronous. */
    private static final int MSG_WHAT_RESET = 110;

    /** Message "what" to make {@link #wake()} asynchronous. */
    private static final int MSG_WHAT_WAKE = 120;

    /** Message "what" to make {@link #onTouchUp()} asynchronous. */
    private static final int MSG_WHAT_TOUCH_UP = 130;

    /** Message "what" to make {@link #onTouchDown()} asynchronous. */
    private static final int MSG_WHAT_TOUCH_DOWN = 140;

    /** Message "what" to make {@link #onTouchCancelled()} asynchronous. */
    private static final int MSG_WHAT_TOUCH_CANCELLED = 150;

    /** Message "what" to make {@link #onSolveChanged} asynchronous. */
    private static final int MSG_WHAT_SOLVE_CHANGED = 160;

    // Define "@AsyncCommand" primarily to ensure that the values of the
    // constants are all unique. There are a few other small benefits for code
    // completion and inspections, etc.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MSG_WHAT_CANCEL, MSG_WHAT_RESET, MSG_WHAT_WAKE,
        MSG_WHAT_TOUCH_UP, MSG_WHAT_TOUCH_DOWN, MSG_WHAT_TOUCH_CANCELLED,
        MSG_WHAT_SOLVE_CHANGED
    })
    @interface AsyncCommand { }

    /**
     * A handler for the "command" messages.
     */
    // NOTE: "static" is best-practice to avoid memory leaks. The timer is
    // passed as "Message.obj".
    private static final class CommandHandler extends Handler {
        /**
         * Handles a "command" message that queued when the command was notified
         * to the puzzle timer. This ensures that the commands are handled
         * asynchronously (though on the same thread).
         *
         * @param msg
         *     The message to be handled. The {@code Message.what} should be set
         *     to the command identifier and the {@code Message.obj} to the
         *     {@link PuzzleTimer} on which to invoke the command.
         */
        @Override
        public void handleMessage(Message msg) {
            // "@AsyncCommand" helps the code inspection: there will be a
            // warning if any constant does not have a corresponding "case"
            // and a warning if unrelated constants are used.
            @AsyncCommand final int command = msg.what;
            final PuzzleTimer timer = (PuzzleTimer) msg.obj;

            switch (command) {
                case MSG_WHAT_CANCEL:
                    timer.cancelSync();
                    break;
                case MSG_WHAT_RESET:
                    timer.resetSync();
                    break;
                case MSG_WHAT_WAKE:
                    timer.wakeSync();
                    break;
                case MSG_WHAT_TOUCH_DOWN:
                    timer.onTouchDownSync();
                    break;
                case MSG_WHAT_TOUCH_UP:
                    timer.onTouchUpSync();
                    break;
                case MSG_WHAT_TOUCH_CANCELLED:
                    timer.onTouchCancelledSync();
                    break;
                case MSG_WHAT_SOLVE_CHANGED:
                    timer.onSolveChangedSync();
                    break;
            }
        }
    }

    /**
     * The duration of the "hold-to-start" period (in milliseconds). This is the
     * time that must elapse after a touch down before the following touch up
     * will cause the timer to start. If the "hold" is for too short a time, the
     * touches will be ignored. The value is based on the behaviour of the
     * StackMat timer mandated by the WCA.
     */
    // A quick search finds some mentions that a StackMat timer has a hold
    // time of 0.55 seconds. This could not be confirmed officially, but it
    // seems a reasonable place to start.
    private static final long HOLD_TO_START_DURATION = 550L;

    /**
     * The time remaining in the normal inspection period at which the first
     * warning cue will be fired to notify the user that time is running out.
     */
    static final long INSPECTION_1ST_WARNING_REMAINING_DURATION = 7_000L;

    /**
     * The time remaining in the normal inspection period at which the second
     * (and last) warning cue will be fired to notify the user that time is
     * running out.
     */
    static final long INSPECTION_2ND_WARNING_REMAINING_DURATION = 3_000L;

    /**
     * The timer states for the "current" and "previous" solve attempts. This is
     * also the single {@code android.os.Parcelable} object that represents the
     * saved instance state of an instance of the {@code PuzzleTimer}.
     */
    @NonNull
    private JointTimerState mJointState;

    /**
     * Indicates if the timer is currently in its "waking" state. Only in
     * this state, will the timer send notifications for timer cues,
     * refreshes, or other events. By default, the timer will be in its
     * sleeping state when it if first created.
     */
    private boolean mIsAwake;

    /**
     * The puzzle clock that will be used to schedule the "tick" events that
     * drive this timer.
     */
    private PuzzleClock mClock = new DefaultPuzzleClock();

    /**
     * The listeners for the timer cues and "set" events that may be notified by
     * the timer.
     */
    // NOTE: It is expected that most use cases will have at least two event
    // listeners (one for the timer widget and one for its container, e.g., a
    // fragment), so "PuzzleTimer" may as well support multiple listeners,
    // rather than require one listener to notify the other. It might also be
    // convenient to have a third listener that is just used for logging, as
    // the (basic) logging code can then be omitted from the other listeners.
    private final List<OnTimerEventListener> mEventListeners
        = new ArrayList<>();

    /**
     * The listener for the refresh notifications that may be fired while the
     * timer is running.
     */
    // NOTE: Because the refresh listener can feed back its preferred refresh
    // period, having more than one listener would be very difficult to
    // handle. In practice, there is no expectation that a single timer will
    // be updating two separate displays at two different rates, so there is
    // unlikely to be any need for more than one refresh listener. The
    // listener can always delegate to other listeners if really necessary.
    private OnTimerRefreshListener mRefreshListener;

    /**
     * The handler for the solve attempt start and stop events that may be
     * notified by the timer.
     */
    // NOTE: Because the "Solve" is returned by the handler, more that one
    // listener cannot be supported.
    private final SolveHandler mSolveHandler;

    /**
     * The handler used to make the calls to some of the event-notification
     * methods asynchronous.
     */
    private final Handler mCommandHandler = new CommandHandler();

    /**
     * Creates a new puzzle timer. By default, there is no inspection period and
     * the hold-to-start behaviour is disabled. These configuration options can
     * be changed after creation. See the class description for more details on
     * the creation of puzzle timers.
     *
     * @param solveHandler
     *     The handler that will be called when each solve attempt starts and
     *     stops, to allow the timer to hold the details of the solve on
     *     starting (e.g., the puzzle type, scramble sequence, etc.) and then to
     *     save the solve on stopping.
     */
    public PuzzleTimer(@NonNull SolveHandler solveHandler) {
        mJointState = new JointTimerState(0, false);
        mSolveHandler = solveHandler;
    }

    @NonNull
    public Parcelable onSaveInstanceState() throws IllegalStateException {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState() @ " + getTimerStage());
        // Does the timer need an "ID" to make its state unique? It would
        // need to be injected in the "TimerConfig" or in another constructor
        // parameter. It is probably overkill, as there is probably no use
        // case for having two timers running in the same context (though it
        // would allow one device to support two user competing head-to-head).
        // Anyway, if that is ever needed, it will not be hard to retrofit an
        // ID (or some other unique "tag").
        //
        // Alternatively, save/restore the timer state in the context of the
        // "View" that presents its user-interface. The view will have an ID
        // already. However, the view does not keep a reference to the state.

        // FIXME: Take a look at the "View.onSaveInstanceState()" API: that
        // makes more sense, I think, for the way things should be
        // implemented here. It may be enough to make this class "Parcelable"
        // and then have it return "this" from "onSaveInstanceState". The
        // implementation of "Parcelable" would just save the "mCurrentState"
        // and "mPreviousState" to a bundle. The caller will then add "this"
        // (PuzzleTimer) to its own state bundle and give it a key name that
        // makes it unique wrt other possible instances of PuzzleTimer.
        //
        // Take a look at some other of my "Parcelable" doohickeys and see if
        // they should get the same treatment.

        // FIXME: How is something like the "hold-to-start" alarm managed
        // across a save/restore boundary? It may depend on whether or not
        // "onTouchUp" gets called when in the new stage. Also, the touch up
        // could be missed if it happens between save and restore, so that
        // could make the timer change from ...HOLDING... to ...READY... as
        // there would be no touch up event to cancel the hold and it would
        // be assumed to have completed. It would probably be much easier
        // (and saner) to move to a less volatile stage just before saving.
        // Any of the ...HOLDING... or ...READY... stages should revert to
        // the previous stage, which will be either "INSPECTION_STARTED" or
        // "STOPPED", both of those stages just ignore any unexpected "touch
        // up", so that would be fine. It will also mean that there will be
        // no need to try to resume a ...HOLDING... stage with its alarm set
        // to fire at the right instant relative to when that stage was first
        // entered (before the save/restore). Simples.
        //
        // Great! ... But what is involved in reverting to a previous stage?
        // Won't some of the timer cues need to be reloaded? How much of that
        // can be done in "tearDown"/"setUp"? Should it mostly be done here?

        // TODO: What if the touch is held down through the stage change
        // (finger on screen during a rotation, for example)? What if that
        // happens during hold-to-start or ready-to-start? Should those
        // stages be re-initialised? It would seen to be wrong: what if I
        // were to hold-to-start just before the inspection period ended,
        // then rotate the device and then lift off? If I have to wait for
        // the hold-to-start period again, then I might end up with a "+2" or
        // "DNF" due to the delay.

        // FIXME: Put these into a nearby "sane" state before saving? What
        // about calling "tearDown" on the state before saving it and then
        // "setUp" after restoring it? Would that be right? What about
        // "tearDown(STOPPED)" and its call to "beginTimerState"? That would
        // need to be avoided. Perhaps if "STOPPED", then there is no need
        // for a "tearDown". OTOH, why should an exception need to be made?
        // Is there something a bit wrong with the way the stages work?
        // Should the "beginTimerState" call be issued from some other stage?
        // It would seem like it would be needed in the holding-for-start and
        // ready-to-start stages, but only in the latter if hold-to-start
        // behaviour is disabled. That would be even messier to handle. Maybe
        // there is a "transient" stage missing between "STOPPED" and those
        // stages, some sort of generic "STARTING" stage, that works a bit
        // like "STOPPING" and "CANCELLING", which, BTW, are the stages that
        // contain the "commitTimerState" and "rollbackTimerState" calls.

        // Tear down the current stage of the current state. (The previous
        // state will already be in the "STOPPED" stage and can be left as it
        // is.) The tear-down will ensure that all of the "ticks" are stopped.
        // They will be re-started in "restoreInstanceState" when "setUp" is
        // called on the state to resume its normal behaviour.

        // FIXME: Document that "saveInstanceState" is *destructive*: it
        // kills the "ticks". Either that, or the approach is wrong. Perhaps
        // the state should be saved and then the tear-down should happen
        // when the timer is destroyed. Which raises the question: is
        // "destroy()" needed in any event to allow the timer to stop
        // properly when the app is closed? What about if the host activity
        // is pushed into the background? Should the timer "stop()" or "pause
        // ()" and then "resume()" or "start()"?
        //
        // Take a look at the normal life-cycle for "saveInstanceState" and
        // how it relates to an activity/fragment. Is it always followed by
        // destruction of the component? ... I looked: it seems that there is
        // nothing definitive that says that "Activity.onSaveInstanceState"
        // will *only* be called prior to destroying the activity. I think a
        // "destroy()" method might be needed. It might also make sense to
        // look at how "Activity"/"Fragment" methods like "onStart()",
        // "onPause()", etc. might have some bearing on the operation of this
        // timer. Perhaps it will need to stop the ticks, etc. and then pick
        // up where it left off when the activity is resumed. The timer would
        // not actually "pause", it would just cease firing cues, ticks and
        // other events until it is "resumed". However, these "pause" and
        // "resume" operations would be too likely to be misinterpreted as
        // meaning that the timer pauses and stops measuring elapsed time.
        // Perhaps terms like "attach"/"detach" or "sleep"/"wake", etc. might
        // be better choices. These would just invoke "tearDown"/"setUp" on
        // the current stage of the current state. The effect would, for
        // example, mean that if the timer was running when the "Settings"
        // was opened (if the UI made that possible), then the timer would
        // "sleep" and, on returning, "wake" and then appear to be running as
        // before. However, there would have been no activity in the
        // background all of this time: the timer just remembered the start
        // time of the attempt and gets the current system time to calculate
        // the new elapsed time. There would be no need for a "destroy()"
        // method: it would probably be correct to simply call "sleep"/"wake"
        // from "onStart()"/"onStop()" of the "Fragment" or "Activity".
        // "onStop()" would probably be called before calling
        // "onSaveInstanceState"

        // FIXME: Need to "sleep()" or not? I think it might be dangerous.
        // FIXME: Try this for now...

        // Requiring that the timer is asleep means that only one of a small
        // subset of all timer stages can be current. See "sleep()" for details.
        if (isAwake()) {
            throw new IllegalStateException(
                "Cannot save instance state while timer is awake.");
        }

        return mJointState;
    }

    /**
     * <p>
     * Restores the saved instance state of this timer. The instance state
     * should previously have been created by {@link #onSaveInstanceState()}.
     * The waking/sleeping state of the timer when {@code onSaveInstanceState()}
     * was previously called is not restored. If this timer is currently in
     * its waking state (see {@link #wake()}), then it will remain awake and
     * continue sending notifications to its listeners when the new state is
     * restored. If this timer is currently in its sleeping state (see
     * {@link #sleep()}), it will not be awoken by this method. Typically, a
     * timer's instance state will be restored to a new instance of a timer
     * that is created just prior to restoring the instance state. In that
     * case, the new timer will be in its default sleeping state and will
     * remain sleeping after calling this method.
     * </p>
     * <p>
     * The configuration settings for the duration of the inspection period
     * (if any) and the hold-to-start behaviour are saved by {@code
     * onSaveInstanceState()}. After restoration of the state, the original
     * configuration values remain in effect for any solve attempt that was
     * still running when the state was saved. However, for any <i>new</i>
     * solve attempts, the configuration values set in the constructor on
     * this instance of the timer before {@code onRestoreInstanceState
     * (Parcelable)} is called take effect. The values can be changed again
     * after restoration (or after instantiation) by
     * {@link #setInspectionDuration(long)} and
     * {@link #setHoldToStartEnabled(boolean)}.
     * </p>
     * <p>
     *  FIXME: Not true any more, AFAIK, as "wake()" will not be called.
     * After the timer's instance state has been restored (synchronously), an
     * asynchronous notification will be sent to
     * {@link OnTimerEventListener#onTimerSet(TimerState)}
     * on the main UI thread, i.e., the notification will be sent some time
     * <i>after</i> this method has returned (and after the method making the
     * call has also returned). However, if the timer is in the sleeping state
     * when it is restored, no notification will be sent. Instead, the
     * notification will be sent after the next explicit call to {@code wake()}
     * has returned. </p>
     *
     * @param state
     *     The instance state to be restored. If {@code null}, no changes will
     *     be made to the current state.
     *
     * @throws IllegalArgumentException
     *     If the given state is not of the expected type.
     * @throws IllegalStateException
     *     If the timer is not in its sleeping state.
     */
    public void onRestoreInstanceState(@Nullable Parcelable state)
            throws IllegalArgumentException, IllegalStateException {

        if (DEBUG_ME) {
            Log.d(TAG, "onRestoreInstanceState(" + state + ')');
            Log.d(TAG, "  stage before restore=" + getTimerStage());
        }
        // TODO: A tricky bit here is to figure out how to save and restore
        // the "*StartedAt" and "*StoppedAt" fields of the timer state and
        // restore them meaningfully, particularly if the timer was running
        // at the time. Saving the elapsed time is the obvious key part of
        // the approach, but exactly how is "*StartedAt" set when that is
        // restored. This probably needs to wait until there is clarity
        // around the "*_PAUSED" stages and how (or if) they will be used. It
        // may also depend a bit on things like the

        // FIXME: If, though some magic, the timer state can be saved and
        // restored in full flight, how can one tell if the interval between
        // the save and restore instants should be included in the elapsed
        // time or not? I think the answer is that it must be included if the
        // stage is not "STOPPED". If the stage is "STOPPED", then the whole
        // "*StartedAt"/"*StoppedAt" thing is easy. IOW, there will never be
        // a need to worry about restoring "elapsed time": just restore the
        // start and end times normally.

        // FIXME: How is the restored stage "rebooted"? If the default stage is
        // "STOPPED", and a tear-down of "STOPPED" calls "beginTimerState()",
        // is that a problem? Perhaps I just need to start with a "null"
        // current stage and then use that as a clue as to what should happen
        // next. It will not matter, then, that "tearDown(STOPPED)" is not
        // called and does not call "beginTimerState()", as there is no
        // previous state to be backed up. HOWEVER, it *does* matter that no
        // new state will be created. Perhaps this is all OK: if the timer is
        // resumed from a saved state, then the transition from "STOPPED"
        // (with its tearDown) will just do something that will then be
        // overwritten by the restored state. Alternatively, pass the saved
        // instance state to the constructor (or an "onCreate" method) and
        // then *either* there is no saved state and the first stage is
        // "STOPPED", or there is some saved state and the first stage is set
        // from that.

        // What about a case where the stage is "STOPPING" or "CANCELLING"?
        // Is that even possible? Can those stages be "current" outside of an
        // invocation of "transitionTo"? Usual caveat about synchronous
        // behaviour when running on the main UI thread, etc.

        // When restoring saved instance state, it is possible that the
        // configuration of the timer has changed in the interim (i.e., the
        // inspection duration may have changed, or the hold-to-start
        // behaviour enabled or disabled). This "PuzzleTimer" instance into
        // which saved instance state will be restored, will have initialised
        // its "JointTimerState" with the up-to-date configuration values.
        // However, the "Parcelable" "state" contains a "JointTimerState"
        // that may be restoring out-of-date values.
        //
        // If the timer is running, it should continue to run with the old
        // configuration, otherwise it might be unable to be restored. For
        // example, if inspection was disabled and the restored state is in
        // the middle of running the inspection countdown, then that
        // countdown must continue. If the timer is not running, then it does
        // not matter, as it will not be restarted and the configuration does
        // not affect how a "Solve" is extracted and saved. However, what
        // *does* matter is how the new configuration is applied to the timer
        // state for any new solve attempt.
        //
        // The solution is to pass in a "prototype" "TimerState" that will be
        // used to create new, unused timer states the next time "push()" is
        // called on the "JointTimerState". That "prototype" is simply the
        // current "TimerState" from the "JointTimerState" of this new
        // "PuzzleTimer" instance *before* the restored timer state
        // overwrites it.

        // FIXME: This keeps things simple. Is it OK?
        if (isAwake()) {
            throw new IllegalStateException(
                "Cannot restore instance state while timer is awake.");
        }

        if (state instanceof JointTimerState) {
//            final boolean wasAwake = isAwake();
//
//            if (wasAwake) {
//                // Stop any "ticks", etc. for the current state *before* it
//                // is overwritten by the restored state. "sleepSync()" fires
//                // no timer cues or other events, so it is safe to call
//                // synchronously. Calling it asynchronously would not be
//                // viable, as "tearDown" must be called before "mJointState"
//                // is overwritten below.
//                sleepSync();
//            }

            // Overwrite the "JointTimerState" with the restored state, but
            // update the restored prototype state with the current values
            // for the inspection duration and hold-to-start flag, as these
            // may have changed since the timer state was saved.
            final JointTimerState restoredJointState = (JointTimerState) state;

            restoredJointState.setPrototypeTimerState(
                mJointState.getPrototypeTimerState());
            mJointState = restoredJointState;

//            if (wasAwake) {
//                // Re-boot any "ticks", etc. for the current stage of the
//                // *restored* state if the timer was already awake, otherwise
//                // wait for an explicit "wake()" call.
//                wake();
//            }
        } else if (state != null) {
            throw new IllegalArgumentException(
                "Wrong state class. Expected '" + JointTimerState.class
                + "' but got '" + state.getClass() + "'.");
        } // else do nothing if "state" is null.

        if (DEBUG_ME) Log.d(TAG, "  stage after restore=" + getTimerStage());
    }

    /**
     * <p>
     * Gets a string representation of this puzzle timer's state. The state is
     * suitable for longer-term persistence of the timer state; longer than the
     * life of the fragment, activity or application process hosting the timer.
     * The timer must be in its sleeping state when the state is saved, or an
     * error will occur. The persistent state should be restored using
     * {@link #onRestorePersistentState(String)}.
     * </p>
     * <p>
     * The state of any referenced {@code Solve} instance is also persisted.
     * On restoring the timer's state, the existence of the solve should be
     * re-verified (if it has an assigned database record ID), to ensure it
     * still exists in the database.
     * </p>
     * <p>
     * As the device may be rebooted before this persistent state is restored,
     * and the timer may be in a running state when it is serialised, this form
     * must accommodate the resetting of the system "uptime", which is the
     * normal time base used for a running timer. Therefore, the current system
     * real time clock value ("wall time") is recorded and compared to the
     * system real time clock value when the state is restored and that
     * difference is used to correct for the resetting of the system uptime.
     * The system uptime is not subject to interference from system clock
     * synchronisation events, but any such changes to the system clock can
     * affect the accuracy of the timer state when it is restored. However,
     * the timer will never be allowed to "run backwards". This potential for
     * inaccuracy mandates that this persistent form only be used for long-term
     * persistence of the timer state. For short-term persistence of the timer
     * state, such as when saving the instance state of a fragment or activity,
     * the {@link #onSaveInstanceState()} should be used.
     * </p>
     *
     * @return
     *     A string representing the state of this puzzle timer that is
     *     suitable for long-term persistence.
     *
     * @throws IllegalStateException
     *     If the timer is not currently in its sleeping state.
     */
    @NonNull
    public String onSavePersistentState() throws IllegalStateException {
        if (DEBUG_ME) Log.d(TAG,
            "onSavePersistentState() @ " + getTimerStage());

        // Same restriction as for "onSaveInstanceState()".
        if (isAwake()) {
            throw new IllegalStateException(
                "Cannot save persistent state while timer is awake.");
        }

        return mJointState.toJSON(mClock.now(), mClock.nowRealTime())
                          .toString();
    }

    /**
     * Restores this timer's state from its persistent string form. The state
     * string must have been created by {@link #onSavePersistentState()} (see
     * the description of that method for more details). The timer must be in
     * its sleeping state when the state is restored, or an error will occur.
     *
     * @param persistentState
     *     The string representing the persistent state of this timer. If
     *     {@code null}, the state of this timer will not be changed.
     *
     * @throws IllegalArgumentException
     *     If there is a problem initialising the puzzle timer state from the
     *     given persistent state string.
     * @throws IllegalStateException
     *     If the timer is not currently in its sleeping state.
     */
    public void onRestorePersistentState(@Nullable String persistentState)
            throws IllegalArgumentException, IllegalStateException {
        if (DEBUG_ME) {
            Log.d(TAG, "onRestorePersistentState(" + persistentState + ')');
            Log.d(TAG, "  stage before restore=" + getTimerStage());
        }

        if (isAwake()) {
            throw new IllegalStateException(
                "Cannot restore persistent state while timer is awake.");
        }

        if (persistentState != null) {
            try {
                final JointTimerState restoredJointState
                    = JointTimerState.fromJSON(
                        new JSONObject(persistentState),
                        mClock.now(), mClock.nowRealTime());

                // Same approach as in "onRestoreInstanceState()": overwrite
                // the old prototype state, to apply up-to-date preferences.
                restoredJointState.setPrototypeTimerState(
                    mJointState.getPrototypeTimerState());
                mJointState = restoredJointState;
            } catch (JSONException e) {
                throw new IllegalArgumentException(
                    "Invalid persistent state string: cannot parse JSON.", e);
            }
        }

        if (DEBUG_ME) Log.d(TAG, "  stage after restore=" + getTimerStage());
    }

    /**
     * <p> Sets the new duration for the inspection period. If the duration is
     * greater than zero, an inspection countdown will occur before the solve
     * timer can be started. </p> <p> This duration is first set in the
     * constructor. However, if the preference for this duration is changed
     * during the life of this timer instance, it can be changed via this
     * method. However, changes only apply to new solve attempts; if the timer
     * is already running for a solve attempt, this change will not take effect
     * during that attempt. </p>
     *
     * @param duration
     *     The duration of the normal inspection period (in milliseconds). This
     *     does <i>not</i> include any extra time allowed (under penalty) when
     *     the inspection countdown reaches zero. Use zero (or a negative) to
     *     indicate that there should be no inspection period before starting
     *     the solve timer.
     */
    public void setInspectionDuration(long duration) {
        if (DEBUG_ME) Log.d(TAG, "setInspectionDuration(" + duration + ')');
        mJointState.setPrototypeTimerState(new TimerState(duration,
            mJointState.getPrototypeTimerState().isHoldToStartEnabled()));
    }

    /**
     * <p> Enables or disables the "hold-to-start" behaviour. If enabled, the
     * touch down on the timer must be held for a short period before lifting
     * the touch will start the timer. </p> <p> This flag is first set in the
     * constructor. However, if the preference for this behaviour is changed
     * during the life of this timer instance, it can be changed via this
     * method. However, changes only apply to new solve attempts; if the timer
     * is already running for a solve attempt, this change will not take effect
     * during that attempt. </p>
     *
     * @param isEnabled
     *     {@code true} if, before starting the inspection countdown or solve
     *     timer, the user must "hold" their touch for a minimum duration before
     *     the timer will register the touch as an intention to start the solve
     *     timer; or {@code false} if, after touching down, the solve timer will
     *     start immediately when the touch is lifted up, regardless of how long
     *     the touch was held down.
     */
    public void setHoldToStartEnabled(boolean isEnabled) {
        if (DEBUG_ME) Log.d(TAG, "setHoldForStartEnabled(" + isEnabled + ')');
        mJointState.setPrototypeTimerState(new TimerState(
            mJointState.getPrototypeTimerState().getInspectionDuration(),
            isEnabled));
    }

    /**
     * Adds a listener for user-interface cues and timer "set" notifications.
     * The listeners will be notified in the order in which they were added.
     * Notifications will <i>not</i> be sent while the timer is in its {@link
     * #sleep()} state, it must first be awoken by {@link #wake()}. Use {@link
     * #removeOnTimerEventListener(OnTimerEventListener)} to remove a listener.
     *
     * @param listener
     *     The new listener to be notified. If the listener is already added, it
     *     will not be added again, so it can only ever be notified once per
     *     event.
     *
     * @throws NullPointerException
     *     If the listener is {@code null}.
     */
    public void addOnTimerEventListener(
        @NonNull OnTimerEventListener listener) {
        if (DEBUG_ME) Log.d(TAG, "addOnTimerEventListener(" + listener + ')');
        if (!mEventListeners.contains(listener)) {
            mEventListeners.add(listener);
        }
    }

    /**
     * Removes a listener for user-interface cues and timer "set" notifications.
     * The listener was previously added by
     * {@link #addOnTimerEventListener(OnTimerEventListener)}.
     *
     * @param listener
     *     The listener to be removed. If the listener is not currently added,
     *     it will be ignored.
     *
     * @throws NullPointerException
     *     If the listener is {@code null}.
     */
    public void removeOnTimerEventListener(
        @NonNull OnTimerEventListener listener) {
        if (DEBUG_ME) Log.d(TAG,
            "removeOnTimerEventListener(" + listener + ')');
        mEventListeners.remove(listener);
    }

    /**
     * Sets the listener for notifications requesting a refresh of the display
     * of the current time. Refresh notifications will <i>not</i> be sent while
     * the timer is in its {@link #sleep()} state, it must first be awoken by
     * {@link #wake()}.
     *
     * @param listener
     *     The listener that will be notified. Use {@code null} to remove a
     *     listener that was previously set.
     */
    public void setOnTimerRefreshListener(
        @Nullable OnTimerRefreshListener listener) {
        if (DEBUG_ME) Log.d(TAG, "setOnTimerRefreshListener(" + listener + ')');
        mRefreshListener = listener;
    }

    /**
     * Sets the clock that will be used to schedule the "tick" events that drive
     * much of the operation of the puzzle timer. By default, the clock
     * implementation is based on the Android system uptime reported by {@code
     * android.os.SystemClock .elapsedRealtime()}. This default is suitable for
     * production use. However, for the purposes of testing the puzzle timer
     * (and <i>only</i> for that purpose), the clock implementation may be
     * overridden.
     *
     * @param clock
     *     The clock instance to use instead of the default type of clock.
     */
    // NOTE: Default ("package") access, as this method is only for access
    // from unit tests.
    @VisibleForTesting
    void setClock(@NonNull PuzzleClock clock) {
        mClock = clock;
    }

    private void fireOnTimerCue(@NonNull TimerCue cue) {
        if (DEBUG_ME) Log.d(TAG, "fireOnTimerCue(" + cue + ") -> "
                                 + mEventListeners.size() + " listeners");

        // If there are no listeners, the cue will not fire repeatedly at
        // nothing, as the cue is consumed on the first call to "fireTimerCue".
        if (getTimerState().fireTimerCue(cue)) {
            for (OnTimerEventListener listener : mEventListeners) {
                listener.onTimerCue(cue);
            }
        }
    }

    private void fireOnTimerCueBlank(@NonNull TimerCue cue) {
        if (DEBUG_ME) Log.d(TAG, "fireOnTimerCueBlank(" + cue + ") -> *BLANK*");
        // No need to "mark" the current time, as no notification will be sent.
        getTimerState().fireTimerCue(cue);
    }

    private void fireOnTimerSet() {
        if (DEBUG_ME) Log.d(TAG, "fireOnTimerSet(): -> "
                                 + mEventListeners.size() + " listeners");
        for (OnTimerEventListener listener : mEventListeners) {
            listener.onTimerSet(getTimerStateNow());
        }
    }

    private void fireOnTimerPenalty() {
        if (DEBUG_ME) Log.d(TAG, "fireOnTimerPenalty(): -> "
                                 + mEventListeners.size() + " listeners");
        for (OnTimerEventListener listener : mEventListeners) {
            listener.onTimerPenalty(getTimerStateNow());
        }
    }

    private void fireOnSolveStart() {
        if (DEBUG_ME) Log.d(TAG, "fireOnSolveStart() -> " + mSolveHandler);
        getTimerState().setSolve(mSolveHandler.onSolveStart());
    }

    private void fireOnSolveStop() {
        if (DEBUG_ME) Log.d(TAG, "fireOnSolveStop() -> " + mSolveHandler);

        // The solve's elapsed time, penalties and date-time stamp were set
        // when "JointTimerState.commit()" was called from "setUp(STOPPING)"
        final Solve solve = getTimerStateNow().getSolve();

        if (solve == null) {
            // Not a normal NPE, as this means that the timer is being used in
            // the wrong manner or is very broken. This should already have been
            // checked by "JointTimerState.commit()", so "null" is unlikely.
            throw new IllegalStateException(
                "Solve must not be null when timer is stopped.");
        }

        // FIXME: If "onSolveStop(Solve)" performed a synchronous save
        // operation, then it might not be very obvious that "onSolveChanged()"
        // needs to be called to ensure the timer is holding the "Solve"
        // instance that now has a database ID, particularly if the synchronous
        // operation does not broadcast "ACTION_ONE_SOLVE_ADDED".
        //
        // There are problems that need to be overcome:
        //
        //  o If "onSolveStop()" were to return the "Solve" instance that may
        //    have been updated by a synchronous save, then "onSolveChanged()"
        //    cannot be called from here, as the timer stage is probably at
        //    "STOPPING", not "STOPPED", so that would be an error.
        //
        //  o "onSolveStop()" cannot call "onSolveChanged()" directly for the
        //    same reason: the timer is probably "STOPPING", not "STOPPED".
        //
        // However, "onSolveChanged()" is an asynchronous method, so the call
        // to "fireOnSolveStop()" will return before "onSolveChangedSync()" is
        // called, so the state *would* be "STOPPED" when "onSolveChangedSync()"
        // is called *if* "isStopped()" were not tested in "onSolveChanged()"
        // before the command message is queued. Checking "isStopped()" from
        // "onSolveChangedSync()" would be a problem if an error needed to be
        // thrown, as it could not be easily handled. OTOH, a) it would be a
        // bug that needed fixing, so graceful handling is not really needed,
        // and b) it could just not throw an exception and log an error message
        // and ignore the change instead. The problem with "b)" is that it
        // would make bugs harder to detect, so perhaps it should just throw a
        // wobbler.
        //
        // One solution might be to take a returned "Solve" and then use a
        // different "onSolveChanged()" method to queue the command message
        // without performing any "isStopped" test. This would be safe as this
        // "fireOnSolveStop()" method is only called when "STOPPING" is about
        // to transition to "STOPPED".
        //
        // A problem with invoking "onSolveChanged()" at this early point is
        // that "onTimerSet()" would be called back and it is going to be called
        // back anyway on transitioning to "STOPPED". Would that redundancy be
        // much of an issue? After all, if the save were asynchronous, the it
        // would eventually result in "onSolveChanged()" being called and that
        // would trigger "onTimerSet()", even if that was already called just
        // before on the transition to "STOPPED".
        //
        // All of this could be simplified by ensuring that the stage is at
        // "STOPPED" when "onSolveStop()" is called, so any call it makes to
        // "onSolveChanged" happens when "isStopped()" reports true. That, in
        // itself, might be a bit messy to implement, but would be simpler if
        // the sequencing of the calls were changed so that "onSolveStop" is
        // called after "onTimerSet", not before it. "onTimerSet" is going to
        // be called again after "onSolveChanged()", in any event.

        mSolveHandler.onSolveStop(solve);
    }

    /*
     * TODO: Document that this needs to be called from a "Handler", and,
     * therefore, does not need to "post" its own call-backs. In particular,
     * this will be called from "onTick", which is called from the "PuzzleClock"
     * handler, so that should be OK (or will it, as that will depend on the
     * implementation of the clock).
     */
    private void fireOnTimerRefresh() {
        if (mRefreshListener != null) {
//            if (DEBUG_ME) Log.d(TAG, "Refreshing: now=" + mClock.now());
            long newRefreshPeriod = 0;

            // Get the "marked" state, so it can calculate the up-to-date
            // values for the time remaining/elapsed.
            final TimerState timerState = getTimerStateNow();

            if (timerState.isInspectionRunning()) {
                newRefreshPeriod
                    = mRefreshListener.onTimerRefreshInspectionTime(
                        timerState.getRemainingInspectionTime(),
                        timerState.getRefreshPeriod());
            } else if (timerState.isSolveRunning()) {
                newRefreshPeriod = mRefreshListener.onTimerRefreshSolveTime(
                    timerState.getElapsedSolveTime(),
                    timerState.getRefreshPeriod());
            }

            // -1 indicates "revert to default"; 0 indicates "unchanged".
            // "setRefreshPeriod" does not understand 0 (so do not call it),
            // but it understands -1. It will return "true" if the refresh
            // period has changed and rescheduling of the "ticks" is needed.
            if (newRefreshPeriod != 0
                    && timerState.setRefreshPeriod(newRefreshPeriod)) {
                scheduleTimerRefresh();
            }
        }
    }

    /**
     * Gets the state of this timer. The state is the state of the running
     * timer, or, if it is not running, the state of the timer when it was last
     * stopped. If the timer was not started since it was first created (and no
     * previous instance state was restored), the state will be the default
     * "0.00" (reset) state.
     *
     * @return The current state of this timer.
     */
    @NonNull
    public TimerState getTimerState() {
        return mJointState.getCurrentTimerState();
    }

    /**
     * <p>
     * Gets the state of this timer, marking the state with the current time.
     * Marking the current time set the remaining inspection time or the
     * elapsed solve time at the instant this method was called. These time
     * values will not change until an updated current time value is marked.
     * </p>
     * <p>
     * This is a convenience method that marks the current time on the state
     * returned by {@link #getTimerState()}.
     * </p>
     *
     * @return
     *     The current state of this timer, marked with the current time at the
     *     instant that this method was called.
     */
    @NonNull
    private TimerState getTimerStateNow() {
        return getTimerState().mark(mClock.now());
    }

    /**
     * Gets the current stage of the timer's finite state machine.
     *
     * @return The current stage of the current timer state.
     */
    @NonNull
    private TimerStage getTimerStage() {
        return getTimerState().getStage();
    }

    /**
     * Transitions from the current timer stage to a new timer stage, updating
     * the properties of the timer, reconfiguring its "tick" events, and
     * notifying the user interface listener as appropriate.
     *
     * @param newStage
     *     The required new stage for the timer.
     *
     * @throws IllegalStateException
     *     If a transition from the current stage to the new stage is an
     *     illegal (i.e., unexpected) transition.
     */
    private void transitionTo(
            @NonNull TimerStage newStage) throws IllegalStateException {
        if (DEBUG_ME) Log.d(TAG, "Stage transition: " + getTimerStage()
                                 + " -> " + newStage);

        if (!VALID_NEXT_STAGES.get(getTimerStage()).contains(newStage)) {
            throw new IllegalStateException(
                "Illegal stage transition from '" + getTimerStage()
                + "' to '" + newStage + "'.");
        }

        // IMPORTANT: For many stage transitions, there are some elements of
        // the stage's set-up that do not change. For example, for many of
        // the "INSPECTION_*" stages, the inspection countdown continues
        // running after a stage transition. However, preservation of some of
        // this set-up is *NOT* attempted across these transitions. Instead,
        // the tear-down of the old stage is complete and unconditional
        // before the set-up of the next stage. This makes for transitions
        // that are far easier to understand, test and maintain. Before the
        // set-up and after the tear-down of *every* stage, there are *no*
        // scheduled clock "tick" events and no opportunities for confusion.
        // Stage transition operations are therefore entirely independent.
        // However, there is still a wider timer state that informs each
        // stage transition, such as the current elapsed time, penalties,
        // enabled state of some configuration options, etc.
        //
        // One consequence of the complete tear-down of an old state before
        // the set-up of a new stage is that some race conditions may arise.
        // For example, if (assuming a 15-second inspection time) there were
        // an "alarm tick" scheduled to ensure the "12 SECONDS" call (WCA
        // Regulation A3d3) would be notified to the UI. Say, this tick was
        // cancelled just before the 12-second mark (say at 11,999 ms) and
        // then the set-up of the next stage occurred just after the mark
        // (say at 12,001 ms). However, this "alarm" event must not be
        // forgotten. To support this, the "PuzzleClock" is lenient about
        // scheduling events in the past and will fire those events
        // immediately. For example, if the simple calculation of the delay
        // for the "12 SECONDS" call results in a "-1 ms" delay because 12,001
        // ms have elapsed, then "PuzzleClock.tickIn(-1)" (or
        // "PuzzleClock.tickAt(t)", where "t" is some absolute time value
        // that is now 1 ms in the past) will fire the event immediately
        // rather than ignore it. Also, the "PuzzleTimer" must keep track of
        // which "alarm" events it has fired and which one-off notifications
        // it has already sent to the UI, so that if those 12,001 ms have
        // elapsed, the set-up operation of the new stage can "know" if the
        // event already fired at 12,000 ms (before the call to "setUp" for
        // the new stage), or if it was just missed and still needs to be
        // scheduled.
        //
        // A final concern is that a "tick" event may be fired after it has
        // been cancelled, because it was already beginning to fire just as
        // "PuzzleClock.cancel(int)" was called. However, this should never
        // be the case. The "tick" events are scheduled on the "Looper" queue
        // of the main UI thread, so a call to "cancel(int)" is synchronous
        // with the queue operations: either the event already fired and
        // "onTick" was called, or the event did not already fire and cannot
        // fire until after any running "PuzzleTimer" method (such as this
        // "transitionTo(TimerStage)" method) has returned. Therefore, any
        // "PuzzleTimer" method calling "PuzzleClock.cancel(int)" is
        // *guaranteed* to cancel a "tick" event that has not already been
        // notified to "PuzzleTimer.onTick()". For the same reasons, when
        // "tearDown" is called here before setting the new stage and calling
        // "setUp" on that new stage, the sequence of calls are "atomic" with
        // respect to any timer tick notifications; these calls cannot be
        // interrupted by "onTick"
        //
        // NOTE: If, for some reason, a decision is made to run the
        // "PuzzleClock" on a different thread from the "PuzzleTimer", either
        // the "PuzzleClock" must queue its messages on the "Looper" of the
        // main UI thread, or all bets are off and much synchronisation of
        // methods will be needed to ensure that stage transitions continue
        // to appear "atomic".

        tearDown(getTimerStage());

        // Set the new stage before calling "setUp(TimerStage)", as that
        // method may call back to this one to effect an immediate transition
        // to another new stage. Say "transitionTo(A)" is called and "setUp(A)"
        // calls "transitionTo(B)". If the field were set after "setUp", the B
        // stage of the nested "follow-on" transition would be set first and
        // then be overwritten by the A stage, which is the wrong order.
        getTimerState().setStage(newStage);

        setUp(newStage);
    }

    /**
     * <p>
     * Sets up the timer for a new stage. The previous stage must first have
     * been terminated by calling {@link #tearDown(TimerStage)} on that
     * previous stage. The current stage property is not changed by this
     * method; it should be set as necessary by the caller.
     * </p>
     * <p>
     * The "started at" and "stopped at" time instants for the inspection and
     * solve phases are recorded when the touch event is handled in
     * {@link #onTouchDown()} or {@link #onTouchUp()}. These instants must be
     * <i>must</i> be recorded before calling this method (i.e., typically
     * before calling {@link #transitionTo(TimerStage)}, which will then call
     * this method.
     * </p>
     *
     * @param stage
     *     The new stage for which to set up the timer.
     */
    private void setUp(@NonNull TimerStage stage) {
        if (DEBUG_ME) Log.d(TAG, "setUp(" + stage + ')');
        // NOTE: Inspection and solve timers are started and stopped in
        // "onTouchUp"/"onTouchDown", rather than in "setUp"/"tearDown", as
        // that ensures the times are recorded with the greatest accuracy and
        // is easier to follow. However, if the inspection period times out,
        // inspection is stopped in "onTick".

        // IMPORTANT: This is where "mCurrentState.mCues" and the fire or reload
        // operations come in. Once a timer cue is "fired" it cannot be fired
        // again unless it is "reloaded". Many timer cues are one-off cues that
        // can only occur once in the life-cycle of a solve attempt (from
        // "STOPPED", through various stages, and then back to "STOPPED" again).
        // It is possible for the instant when a cue should be fired to occur
        // after the "tearDown" for one stage and before the "setUp" of the next
        // stage, when all tick events have been cancelled. On the set-up of a
        // new stage, a simple check of "canFireTimerCue" will indicate if the
        // cue fired in the previous stage or not. The scheduling is simple:
        // just schedule it to fire at the instant when it is due, regardless
        // of whether that instant is in the past or the future. This ensures
        // that it will be fired as close to when it should be as is possible.
        //
        // It is important *not* to check if it is too late to fire the timer
        // cue or not. This would run the risk of not firing the cue. For
        // example, if the timer cue is due to fire just after a stage
        // transition "tearDown" cancels the tick events, but by the time of
        // the "setUp" of the next stage, the time to fire the cue has passed.
        // In this case, the timer cue, which "can fire" must be scheduled,
        // even though it will fire a little bit late.
        //
        // If "canFireTimerCue" is not checked, then the tick event will be
        // notified, but the timer cue will *not* fire if it has already been
        // fired. Things will work as expected, but it is preferred to avoid
        // the overhead of the extra tick scheduling and notification.

        final TimerState state = getTimerState();

        // IMPORTANT: "transitionTo()" sets the stage of the current timer state
        // *before* calling "setUp()". Therefore, when firing "onTimerSet" and
        // other events below, the stage seen by the listener is the same as
        // that identified by the "case" label. The exception to this is when
        // "transitionTo()" is called from a "case", so pay attention!

        switch (stage) {
            case UNUSED:
                // This is just a stage used to represent a "reset" timer
                // before using it and to react to the first "onTouchDown".
                fireOnTimerSet();
                break;

            case STARTING:
                // "STARTING" is triggered on the first "onTouchDown" from
                // the "UNUSED" stage (possibly when redirected from the
                // "STOPPED" stage of the previous state) and now transitions
                // to the first "real" stage.
                //
                // Fire "onTimerSet" *before* calling "transitionTo", so the
                // call-back to "onTimerSet" is made before any cues are fired.
                // The stage will be "STARTING" when "onTimerSet" is called,
                // but that is OK.
                fireOnTimerSet();
                transitionTo(
                    state.isHoldToStartEnabled()
                        ? state.isInspectionEnabled()
                            ? INSPECTION_HOLDING_FOR_START
                            : SOLVE_HOLDING_FOR_START
                        : state.isInspectionEnabled()
                            ? INSPECTION_READY_TO_START
                            : SOLVE_READY_TO_START);
                break;

            case INSPECTION_STARTED:
                // "onTimerSet" may fire more than once, as this stage can be
                // re-entered from "INSPECTION_SOLVE_HOLDING_FOR_START", but
                // allowing it to fire more than once keeps things simpler.
                fireOnTimerSet();
                // This cue will only fire once per solve attempt.
                fireOnTimerCue(CUE_INSPECTION_STARTED);
                // This cue is not loaded by default; it is loaded on demand by
                // "INSPECTION_SOLVE_HOLDING_FOR_START". It will fire on each
                // return from that stage to this one when the "hold" is not
                // long enough to start the solve timer.
                fireOnTimerCue(CUE_INSPECTION_RESUMED);
                // Allow this cue to be fired each time there is a transition
                // from this stage to "INSPECTION_SOLVE_HOLDING_FOR_START".
                state.reloadTimerCue(CUE_INSPECTION_SOLVE_HOLDING_FOR_START);

                scheduleInspectionAlarms(state);
                scheduleTimerRefresh();
                break;

            case INSPECTION_SOLVE_HOLDING_FOR_START:
                // *Inspection* is still running; *solve* is holding.
                fireOnTimerCue(CUE_INSPECTION_SOLVE_HOLDING_FOR_START);
                // Allow "CUE_INSPECTION_RESUMED" to fire each time there is a
                // transition from this stage back to "INSPECTION_STARTED" when
                // the "hold" is too short to reach the ready-to-start stage.
                state.reloadTimerCue(CUE_INSPECTION_RESUMED);

                scheduleHoldingForStartAlarm();
                scheduleInspectionAlarms(state);
                scheduleTimerRefresh();
                break;

            case INSPECTION_SOLVE_READY_TO_START:
                // *Inspection* is still running; *solve* is ready.
                // This can only be fired once, as there is no going back to
                // other inspection stages from this stage.
                fireOnTimerCue(CUE_INSPECTION_SOLVE_READY_TO_START);

                scheduleInspectionAlarms(state);
                scheduleTimerRefresh();
                break;

            case INSPECTION_HOLDING_FOR_START:
                // If the "hold" is too short, then the stage will go through
                // "CANCELLING" to "STOPPED". Any *new* attempt will start
                // with a new "UNUSED" timer state and all of the cues will be
                // re-initialised, so there is no need to "reload" this cue.
                fireOnTimerCue(CUE_INSPECTION_HOLDING_FOR_START);

                scheduleHoldingForStartAlarm();
                break;

            case SOLVE_HOLDING_FOR_START:
                // See "INSPECTION_HOLDING_FOR_START" (above) for note on how
                // this cue never needs to be reloaded.
                fireOnTimerCue(CUE_SOLVE_HOLDING_FOR_START);

                scheduleHoldingForStartAlarm();
                break;

            case INSPECTION_READY_TO_START:
                // Cannot avoid starting the solve attempt now that the timer is
                // ready, so fire "onSolveStart". Timer cues fire further below.
                fireOnSolveStart();
                fireOnTimerCue(CUE_INSPECTION_READY_TO_START);
                break;

            case SOLVE_READY_TO_START:
                // See note above for "INSPECTION_READY_TO_START".
                fireOnSolveStart();
                fireOnTimerCue(CUE_SOLVE_READY_TO_START);
                break;

            case SOLVE_STARTED:
                fireOnTimerSet();
                fireOnTimerCue(CUE_SOLVE_STARTED);

                scheduleTimerRefresh();
                break;

            case CANCELLING:
                fireOnTimerCue(CUE_CANCELLING);
                // Restore the previous state overwriting the cancelled state.
                // Call "setUp()", not "transitionTo()", as "pop()" has already
                // set the current state to the restored state. The new current
                // stage can only be "STOPPED" or "USED". The set-up of either
                // will fire "onTimerSet" to mark the end of this solve attempt.
                // The discarded current state (overwritten by the "pop()") was
                // already passed to "tearDown()" by the "transitionTo()" call
                // that led here.
                setUp(mJointState.pop().getStage());
                break;

            case STOPPING:
                fireOnTimerCue(CUE_STOPPING);
                // Update the "Solve" with the elapsed time, penalties and
                // date-time stamp with a call to "commit()". The transition to
                // "STOPPED" fires the call to "onTimerSet" that marks the end
                // of this solve attempt. "onSolveStop(Solve)" is then called
                // to allow the timer's referenced "Solve" to be saved. When it
                // is saved (which can be done asynchronously) and is assigned
                // a database record ID, "PuzzleTimer.onSolveChanged(Solve)"
                // must be called (if the timer is still stopped) to ensure
                // that this timer's referenced "Solve" includes that new ID.
                mJointState.commit(mClock.nowRealTime());
                transitionTo(STOPPED); // Triggers "onTimerSet()".
                fireOnSolveStop();
                break;

            case STOPPED:
                // Most of the work was done in the "STOPPING" or "CANCELLING"
                // stages, or this is a timer restored to the "STOPPED" stage.
                fireOnTimerSet();
                break;
        }
    }

    /**
     * Schedules "tick" events that will notify the refresh listener that the
     * displayed time needs to be updated. If there are refresh "ticks"
     * already scheduled, they will be cancelled before scheduling the new
     * ticks. The current refresh period (from the timer state will be used).
     * The origin time will be set depending on which timer (inspection or
     * solve) is currently running. Therefore, these "ticks" should be
     * scheduled only after the inspection timer or solve timer have been
     * started.
     */
    private void scheduleTimerRefresh() {
        final TimerState state = getTimerState();

        if (DEBUG_ME) {
            Log.d(TAG,
                "scheduleTimerRefresh(): period=" + state.getRefreshPeriod()
                + " ms, origin=" + state.getRefreshOriginTime());
        }

        // Cancel any existing refresh ticks, as this may be a re-scheduling
        // after feedback from the refresh listener.
        mClock.cancelTick(this, TICK_ID_TIMER_REFRESH);
        mClock.tickEvery(this, TICK_ID_TIMER_REFRESH, state.getRefreshPeriod(),
            state.getRefreshOriginTime());
    }

    private void scheduleHoldingForStartAlarm() {
        mClock.tickIn(this, TICK_ID_HOLDING_FOR_START_ALARM,
            HOLD_TO_START_DURATION);
    }

    private void scheduleInspectionAlarms(TimerState timerState) {
        // The "iEnd" instant does not include the overrun period.
        final long iEnd = timerState.getInspectionEnd();

        // Schedule the timer cues in order of firing. When using the
        // "DefaultPuzzleClock" implementation, this adds each message to the
        // front of the queue in turn. When the messages are handled, they
        // will be handled in the reverse order. This will allow, say,
        // "TICK_ID_INSPECTION_TIME_UP_ALARM" to be handled first if the
        // timer is put to "sleep()" during inspection and does not "wake()"
        // until inspection would have been over. When that tick it handled,
        // it can cancel the other ticks and their related cues, as they
        // would not be of any use and would only cause a "cue storm" that
        // would complicate the timer cue handling in the UI components.
        //
        // This works because if the "futureTime" parameter is negative,
        // "DefaultPuzzleClock.tickAt" will truncate it to zero, which will
        // cause the "MessageQueue" of the "Looper" to add the "Message" to
        // the head of the queue, instead of in chronological order.
        if (timerState.canFireTimerCue(CUE_INSPECTION_7S_REMAINING)) {
            mClock.tickAt(this, TICK_ID_INSPECTION_7S_REMAINING_ALARM,
                iEnd - INSPECTION_1ST_WARNING_REMAINING_DURATION);
        }

        if (timerState.canFireTimerCue(CUE_INSPECTION_3S_REMAINING)) {
            mClock.tickAt(this, TICK_ID_INSPECTION_3S_REMAINING_ALARM,
                iEnd - INSPECTION_2ND_WARNING_REMAINING_DURATION);
        }

        if (timerState.canFireTimerCue(CUE_INSPECTION_OVERRUN)) {
            mClock.tickAt(this, TICK_ID_INSPECTION_OVERRUN_ALARM,
                iEnd);
        }

        // This "tick" will cause the timer to stop with a "DNF".
        mClock.tickAt(this, TICK_ID_INSPECTION_TIME_OUT_ALARM,
            iEnd + TimerState.INSPECTION_OVERRUN_DURATION);
    }

    /**
     * <p>
     * Tears down (terminates) an old stage, returning the timer to a neutral
     * point between stages. After tearing down an old stage, a new stage
     * must be initialised immediately by calling {@link #setUp(TimerStage)}.
     * The current stage property is not changed by this method; it should be
     * changed by the caller, if necessary.
     * </p>
     * <p>
     * This method does not&mdash;and, if changed, <i>must not</i>&mdash;send
     * notifications for any timer cues or other events. That would cause
     * problems for {@link #onRestoreInstanceState}, which must be able to
     * tear down the old state synchronously.
     * </p>
     *
     * @param stage
     *     The old stage to be terminated.
     */
    private void tearDown(@NonNull TimerStage stage) {
        if (DEBUG_ME) Log.d(TAG, "tearDown(stage=" + stage + ')');
        // The nuclear option! Cancel any "tick" events that may have been
        // scheduled.
        mClock.cancelAllTicks(this);
    }

    /**
     * <p>
     * Notifies the timer that the timer's user interface has detected a touch
     * down. If the timer is not awake (see {@link #isAwake()}), this event
     * will be ignored.
     * </p>
     * <p>
     * This method returns immediately. A command event is sent to the message
     * queue and will be handled in turn. The test of the waking state is
     * performed when the command event is handled, not when it is queued.
     * </p>
     * <p>
     * A running solve timer is stopped in response to a touch-down event. If
     * this event is notified while the timer is running, the timer will be
     * stopped <i>before</i> this method returns. This ensures that the elapsed
     * time measured by the timer is as accurate as possible and will not be
     * subject to any delays incurred while the command event is queued.
     * However, timer cues and other timer events relating to the stopping of
     * the timer will subsequently be notified asynchronously.
     * </p>
     */
    public void onTouchDown() {
        // NOTE: See note in "onTouchDownSync()" about testing "isAwake()" for
        // some background. It is unlikely that "onTouchDown()" will be called
        // from the same method that calls "sleep()", so assume that the timer
        // is awake now, as it is expected that it *will* be awake when
        // "onTouchDownSync()" gets called to complete this operation for the
        // "SOLVE_STARTED" case.
        if (getTimerStage() == SOLVE_STARTED) {
            // Cues and other events will be fired from "transitionTo(STOPPING)"
            // called from "onTouchDownSync()" for "SOLVE_STARTED" stage.
            //
            // Under WCA Regulations if the "timer stops before the end of the
            // solve" and the time is "strictly below 0.06 seconds", then a new
            // attempt is allowed, as it is deemed to be a timer malfunction
            // (A6b1), but if the time is "0.06 seconds or higher" the attempt
            // is a DNF (A6b2). The old implementation did not allow solve times
            // under 0.06 seconds, but this timer implementation does allow
            // them. The reason is that these rules only apply if the timer
            // stops *before* the end of the solve, not after the solve is
            // complete. As the app cannot make that determination, it is left
            // to the user to discard the attempt or incur a DNF manually after
            // stopping the timer.
            //
            // These regulations exist "to accommodate concerns about StackMat
            // timer malfunctions" (WCA Guidelines A6b+). The likely culprit is
            // "switch bouncing", but the analogous "touch bouncing" when using
            // a touch screen should be damped within the touch-screen driver,
            // so there is no need to complicate this implementation by
            // re-inventing that wheel.
            //
            // However, if faulty drivers or cheap touch screens prove to be a
            // problem, a simple solution would be to check the elapsed solve
            // time before stopping the timer and then ignore any "onTouchDown"
            // events until the elapsed time reaches 60 ms.
            getTimerState().stopSolve(mClock.now());
        }

        mCommandHandler.sendMessage(
            mCommandHandler.obtainMessage(MSG_WHAT_TOUCH_DOWN, this));
    }

    /**
     * Handles the command event notifying that the touch has been placed down.
     * The command event is queued by {@link #onTouchDown()}.
     */
    private void onTouchDownSync() {
        // NOTE: "isAwake()" is tested here in "onTouchDownSync()", not in
        // "onTouchDown()". If a client method were to call "wake()" and then
        // "onTouchDown()" in that order from a single method, then each call
        // would post its message to the queue. However, because the handling
        // is asynchronous, the call to "wake()" does not execute immediately
        // and the call to "onTouchDown()" will not see any change to the
        // waking state. By checking "isAwake()" here in "onTouchDownSync()",
        // the "wakeSync()" command that was queued first will have run
        // first, so "isAwake()" is up to date for the execution of this next
        // "onTouchDownSync" command.
        if (!isAwake()) {
            return;
        }

        switch (getTimerStage()) {
            case STOPPED:
                // The first touch activates the "UNUSED" or "STOPPED" timer.
                // For a new solve attempt, "push()" will back-up the current
                // state making it the "previous state" before creating and
                // setting the current state to be a new, clean state set at
                // the "UNUSED" stage, which is then transitioned straight to
                // "STARTING" (which will call "onTimerSet").
                mJointState.push();
                // fall through
            case UNUSED: // "push()" is redundant if state is already "UNUSED".
                transitionTo(STARTING);
                break;

            case INSPECTION_STARTED:
                transitionTo(getTimerState().isHoldToStartEnabled()
                    ? INSPECTION_SOLVE_HOLDING_FOR_START
                    : INSPECTION_SOLVE_READY_TO_START);
                break;

            case SOLVE_STARTED:
                // Timer was stopped synchronously in "onTouchDown()", which
                // stops it as soon as possible to ensure accuracy and avoid
                // any message queue delays. Now fire the cues and events....
                transitionTo(STOPPING);
                break;

            case INSPECTION_HOLDING_FOR_START:
            case INSPECTION_READY_TO_START:
            case INSPECTION_SOLVE_HOLDING_FOR_START:
            case INSPECTION_SOLVE_READY_TO_START:
            case SOLVE_HOLDING_FOR_START:
            case SOLVE_READY_TO_START:
            case STARTING:
            case CANCELLING:
            case STOPPING:
                // Ignore touch down, as touch is already expected to be "down"
                // in these stages, or they are transitional stages that do not
                // take any inputs. The next stage transition will happen on
                // the next touch up.
                break;
        }
    }

    /**
     * <p>
     * Notifies the timer that the touch has been lifted up from the timer's
     * user interface. This usually follows {@link #onTouchDown()}, though it
     * may occur erroneously if the touch down occurred before the timer was
     * cancelled and the touch up occurred after that cancellation, or if the
     * touch was already down before the timer user interface began listening
     * for touch events.  If the timer is not awake (see {@link #isAwake()}),
     * this event will be ignored.
     * </p>
     * <p>
     * This method returns immediately. A command event is sent to the
     * message queue and will be handled in turn. The test of the waking
     * state is performed when the command event is handled, not when it is
     * queued.
     * </p>
     */
    public void onTouchUp() {
        mCommandHandler.sendMessage(
            mCommandHandler.obtainMessage(MSG_WHAT_TOUCH_UP, this));
    }

    /**
     * Handles the command event notifying that the touch has been lifted up.
     * The command event is queued by {@link #onTouchUp()}.
     */
    private void onTouchUpSync() {
        // NOTE: See comment in "onTouchDownSync()" for reason why "isAwake()"
        // is tested here.
        if (!isAwake()) {
            return;
        }

        final long now = mClock.now();
        final TimerState state = getTimerState();

        switch (state.getStage()) {
            case UNUSED:
                // Stage is exited immediately on a touch down, so cannot still
                // be at this stage for the corresponding touch up.
                break;

            case INSPECTION_HOLDING_FOR_START:
            case SOLVE_HOLDING_FOR_START:
                // The "hold" was not for long enough. If it had been long
                // enough, "onTick()" would have transitioned to
                // "INSPECTION_READY_TO_START" (or to "SOLVE_READY_TO_START",
                // if inspection was not enabled). Abort and roll back to the
                // previous timer state.
                transitionTo(CANCELLING);
                break;

            case INSPECTION_READY_TO_START:
                state.startInspection(now);
                transitionTo(INSPECTION_STARTED);
                break;

            case INSPECTION_SOLVE_HOLDING_FOR_START:
                // If the "hold-to-start" waiting period had already elapsed,
                // then the stage would have been changed to
                // "INSPECTION_SOLVE_READY_TO_START" (see "onTick()"). If the
                // touch up is received in this stage, then the "hold" was
                // too short, so return to the normal inspection countdown.
                // The countdown has not yet timed out: if it had timed out
                // during the "hold-to-start" period, "onTick" would have
                // transitioned the stage to "STOPPING".
                transitionTo(INSPECTION_STARTED);
                break;

            case INSPECTION_SOLVE_READY_TO_START:
                // About to start timing the solve (i.e., timer is now *leaving*
                // the ready-to-start stage), so the inspection period ends now.
                state.stopInspection(now);
                // fall through
            case SOLVE_READY_TO_START: // If inspection is *disabled*.
                state.startSolve(now);
                transitionTo(SOLVE_STARTED);
                break;

            case STARTING:
            case CANCELLING:
            case STOPPING:
            case INSPECTION_STARTED:
            case SOLVE_STARTED:
            case STOPPED:
                // Ignore the touch up. These are transitional stages that do
                // not react to touches, or they only react to a touch down.
                break;
        }
    }

    /**
     * <p>
     * Notifies the timer that the last notified touch has been cancelled.
     * This may occur after {@link #onTouchDown()} if the user's touch event
     * became a swipe event intended to be consumed by a different component,
     * such as a pager. If the timer is not awake (see {@link #isAwake()}),
     * this event will be ignored.
     * </p>
     * <p>
     * This method returns immediately. A command event is sent to the
     * message queue and will be handled in turn. The test of the waking
     * state is performed when the command event is handled, not when it is
     * queued.
     * </p>
     */
    public void onTouchCancelled() {
        mCommandHandler.sendMessage(
            mCommandHandler.obtainMessage(MSG_WHAT_TOUCH_CANCELLED, this));
    }

    /**
     * Handles the command event notifying that the touch has been cancelled.
     * The command event is queued by {@link #onTouchCancelled()}.
     */
    private void onTouchCancelledSync() {
        // NOTE: See comment in "onTouchDownSync()" for reason why "isAwake()"
        // is tested here.
        if (!isAwake()) {
            return;
        }

        // A cancelled touch requires that the actions taken in "onTouchDown"
        // be reverted (with an exception for "SOLVE_STARTED -> STOPPED").
        switch (getTimerStage()) {
            case UNUSED:
            case STOPPED:
            case INSPECTION_STARTED:
                // Do nothing. The stage will already have transitioned on the
                // "onTouchDown", so the new stage will do the reversal for the
                // "onTouchCancelled".
            case SOLVE_STARTED:
                // While a touch down in "SOLVE_STARTED" stops the timer and
                // transitions to "STOPPING", there is no desire to have that
                // touch down be then reinterpreted as a "swipe" and be
                // cancelled, thus setting the timer going again.
                //
                // It is reasonable to expect that the touch down to stop the
                // timer could be fast and sloppy, so leniency needs to be
                // shown to ensure that any sort of touch---including, but
                // not limited to, any tap, slap, nudge, fudge, smack, whack,
                // crack, thwack, bang, clang, sock, shock, knock, stroke,
                // poke, lunge, plunge, prod, nod, bash, smash, dab, jab, or
                // stab---is respected and will stop the timer immediately
                // and *keep* it stopped. (It is also expected that the UI
                // will disable the swipe-to-change-tabs behaviour while the
                // timer is running, so "onTouchCancelled" is unlikely.)
                break;

            case INSPECTION_HOLDING_FOR_START:
            case INSPECTION_READY_TO_START:
            case SOLVE_HOLDING_FOR_START:
            case SOLVE_READY_TO_START:
                // Revert what "onTouchDown" did for "UNUSED" or "STOPPED".
                transitionTo(CANCELLING);
                break;

            case INSPECTION_SOLVE_READY_TO_START:
            case INSPECTION_SOLVE_HOLDING_FOR_START:
                // Revert what "onTouchDown" did for "INSPECTION_STARTED".
                transitionTo(INSPECTION_STARTED);
                break;

            case STARTING:
            case CANCELLING:
            case STOPPING:
                // "onTouchDown" ignored the touch for these transitional
                // stages, so nothing to cancel.
                break;
        }
    }

    // NOTE: Already called from another "Handler" in the "PuzzleClock".
    // FIXME: May want to check if implementations need to be told to use a
    // handler, or may need to add more indirection here, anyway.
    // FIXME: NOTE: Adding a "Handler" layer here could make the class harder
    // to test (though one could just directly test "onTickSync()").
    @Override
    public void onTick(@TickID int tickID) {
        // See comment in "setUp" and the description of "wake()": if some
        // alarms arrive before others, then cancel the other alarms and
        // consume their corresponding timer cues. This avoids a "notification
        // storm" if "wake()" is called after a "sleep ()" that lasts for
        // most, or all, of the inspection period.
        switch (tickID) {
            case TICK_ID_HOLDING_FOR_START_ALARM:
            case TICK_ID_INSPECTION_7S_REMAINING_ALARM:
            case TICK_ID_TIMER_REFRESH:
                // These are handled in the next "switch" (below).
                break;

            case TICK_ID_INSPECTION_TIME_OUT_ALARM:
                // 7s, 3s, and overrun alarms are now redundant, so cancel them.
                // The cue is fired "blank", i.e., it sends no notification,
                // and cannot be fired again from the next "switch" (below).
                mClock.cancelTick(this, TICK_ID_INSPECTION_OVERRUN_ALARM);
                fireOnTimerCueBlank(CUE_INSPECTION_OVERRUN);
                // fall through
            case TICK_ID_INSPECTION_OVERRUN_ALARM:
                // 7s and 3s alarms are now redundant, so cancel them.
                mClock.cancelTick(this, TICK_ID_INSPECTION_3S_REMAINING_ALARM);
                fireOnTimerCueBlank(CUE_INSPECTION_3S_REMAINING);
                // fall through
            case TICK_ID_INSPECTION_3S_REMAINING_ALARM:
                // 7s alarm is now redundant, so cancel it.
                mClock.cancelTick(this, TICK_ID_INSPECTION_7S_REMAINING_ALARM);
                fireOnTimerCueBlank(CUE_INSPECTION_7S_REMAINING);
                break;
        }

        final TimerStage stage = getTimerStage();

        switch (tickID) {
            case TICK_ID_HOLDING_FOR_START_ALARM:
                if (stage == INSPECTION_HOLDING_FOR_START) {
                    transitionTo(INSPECTION_READY_TO_START);
                } else if (stage == INSPECTION_SOLVE_HOLDING_FOR_START) {
                    transitionTo(INSPECTION_SOLVE_READY_TO_START);
                } else if (stage == SOLVE_HOLDING_FOR_START) {
                    transitionTo(SOLVE_READY_TO_START);
                } else {
                    throw new IllegalStateException(
                        "Unexpected holding-for-start 'tick': stage=" +  stage);
                }
                break;

            case TICK_ID_INSPECTION_7S_REMAINING_ALARM:
                // Will not fire again if it was fired "blank" in the first
                // "switch" (above).
                fireOnTimerCue(CUE_INSPECTION_7S_REMAINING);
                break;

            case TICK_ID_INSPECTION_3S_REMAINING_ALARM:
                // Will not fire again if it was fired "blank" in the first
                // "switch" (above).
                fireOnTimerCue(CUE_INSPECTION_3S_REMAINING);
                break;

            case TICK_ID_INSPECTION_OVERRUN_ALARM:
                // Inspection countdown has reached zero before the solve timer
                // was started. This incurs a "+2" penalty, but there are still
                // two seconds allowed to start solving. Does not affect the
                // current stage. Inspection countdown continues.
                getTimerState().incurPreStartPenalty(Penalty.PLUS_TWO);
                // Will not fire again if it was fired "blank" in the first
                // "switch" (above).
                fireOnTimerCue(CUE_INSPECTION_OVERRUN);
                fireOnTimerPenalty();
                break;

            case TICK_ID_INSPECTION_TIME_OUT_ALARM:
                // Inspection time and the 2-second "overrun" have elapsed: DNF!
                getTimerState().incurPreStartPenalty(Penalty.DNF);
                // Stopping inspection at "-1" will automatically set the stop
                // instant to exactly the start instant plus the inspection
                // duration and overrun duration.
                getTimerState().stopInspection(-1);

                fireOnTimerCue(CUE_INSPECTION_TIME_OUT);
                fireOnTimerPenalty();

                // A time-out "DNF" is still a "normal" result; there will
                // be no roll-back, so stop normally and (eventually) fire
                // "onSolveStop" and then "onTimerSet".
                transitionTo(STOPPING);
                break;

            case TICK_ID_TIMER_REFRESH:
                fireOnTimerRefresh();
                break;
        }
    }

    /**
     * <p>
     * Notifies the stopped timer that the database record backing the timer's
     * current {@link Solve} instance has been changed and that instance should
     * be replaced with a more up-to-date one. Typically, this occurs when a
     * newly created solve has been saved and has been assigned its database
     * record ID, or if that saved solve is edited manually after the timer is
     * stopped. If the timer is awake (see {@link #isAwake()}), notification of
     * a change to the solve instance will be followed by a call-back to
     * {@link OnTimerEventListener#onTimerSet(TimerState)} and the listener
     * should display the updated solve details.
     * </p>
     * <p>
     * If the timer is stopped and it holds a {@code Solve} instance, the
     * methods of {@link TimerState} that report the elapsed solve time
     * (excluding penalties), the result time (including penalties) and the
     * penalties themselves will report the values held in the {@code Solve}.
     * Therefore, if the solve is changed independently of the timer, but the
     * timer is then notified with {@code onSolveChanged(Solve)}, the timer
     * will reflect those changes appropriately. The solve instance held by
     * the timer can then be used as the basis of further edits and database
     * updates, as it is sure to reflect the up-to-date state of the solve
     * record.
     * </p>
     * <p>
     * If the solve record is deleted, call {@link #reset()} instead of this
     * method.
     * </p>
     * <p>
     * This method may only be called after {@code onSolveStop(Solve)} has been
     * notified to the listener and the timer state reports {@code true} from
     * {@link TimerState#isStopped()}. If called before that notification, or
     * after the timer has been started again, an error will occur.
     * </p>
     * <p>
     * This method returns immediately. A command event is sent to the message
     * queue and will be handled in turn. The test of the waking state is
     * performed when the command event is handled, not when it is queued. The
     * new {@code Solve} is recorded before the command event is queued,
     * regardless of the waking state of the timer.
     * </p>
     *
     * @param newSolve
     *     The new solve instance to be recorded as the current result of this
     *     stopped timer. This will be recorded and will trigger a call-back to
     *     {@code onTimerSet(TimerState)} even if it is the same as the current
     *     solve instance.
     *
     * @throws IllegalStateException
     *     If the timer is not stopped.
     */
    public void onSolveChanged(@NonNull Solve newSolve) {
        if (DEBUG_ME) Log.d(TAG, "onSolveChanged(" + newSolve + ')');

        // NOTE: This method does not attempt to test if the new solve or the
        // old solve are the same, or if their IDs match. The internals of a
        // "Solve" instance or its relationship to any database records are not
        // of concern to a "PuzzleTimer". It is the caller that should wrap
        // invocations of this method with whatever logic it deems appropriate.

        final TimerState state = getTimerStateNow();

        if (!state.isStopped()) {
            throw new IllegalStateException(
                "Cannot change the Solve: timer is not stopped.");
        }

        // Passing the "Solve" object through the "Message" would be a bit
        // messy, as there is only one "Message.obj" and it is already used for
        // "this" PuzzleTimer to allow "this.onSolveChangedSync()" to be called
        // back. Therefore, just change the solve first and then queue the
        // message that triggers the asynchronous call-back.
        if (DEBUG_ME) Log.d(TAG, "  Updating timer Solve to: " + newSolve);
        state.setSolve(newSolve);

        mCommandHandler.sendMessage(
            mCommandHandler.obtainMessage(MSG_WHAT_SOLVE_CHANGED, this));
    }

    /**
     * Handles the command event notifying that the {@code Solve} associated
     * with the current solve attempt has been changed. The command event is
     * queued by {@link #onSolveChanged(Solve)}.
     */
    private void onSolveChangedSync() {
        if (isAwake()) {
            fireOnTimerSet();
        }
    }

    /**
     * <p>
     * Resets the timer, clearing the stored result of any previous solve.
     * When the state has been reset,
     * {@link OnTimerEventListener#onTimerSet(TimerState)} will be called, to
     * allow the UI to refresh its display to show the new reset state. If
     * the timer is currently running, or if the timer is already reset, this
     * method will have no effect and will not notify any listener. No timer
     * cue will be notified as a result of this operation. If the timer is not
     * awake (see {@link #isAwake()}), this reset attempt will be ignored.
     * </p>
     * <p>
     * This method returns immediately. A command event is sent to the
     * message queue and will be handled in turn. The test of the waking
     * state is performed when the command event is handled, not when it is
     * queued.
     * </p>
     */
    public void reset() {
        mCommandHandler.sendMessage(
            mCommandHandler.obtainMessage(MSG_WHAT_RESET, this));
    }

    /**
     * Handles the command event notifying that the timer has been reset. The
     * command event is queued by {@link #reset()}.
     */
    private void resetSync() {
        // NOTE: See comment in "onTouchDownSync()" for reason why "isAwake()"
        // is tested here.
        if (isAwake() && getTimerStage() == STOPPED) {
            // The current timer state instance is not actually reset; it is
            // discarded and replaced with a new "UNUSED" state. Therefore,
            // "transitionTo()" is not used, as the timer stage instance
            // changes between "tearDown()" and "setUp()". The "setUp(UNUSED)"
            // call will fire "onTimerSet" to ensure the display is notified
            // of the change and is updated.
            tearDown(getTimerStage());
            setUp(mJointState.reset().getStage());
        }
    }

    /**
     * <p>
     * Cancels the current solve attempt. This stops the timer and restores
     * the timer state that was saved before the timing sequence started.
     * This has no effect if the timer is already stopped or has never been
     * started. When the cancelled solve attempt has been stopped,
     * {@link TimerCue#CUE_STOPPING} will be notified and will report the
     * state of the cancelled timer. That notification will be followed by
     * notification of the restored state after the cancellation (i.e., the
     * restored state that was backed up when the solve attempt started) in a
     * call-back to {@link OnTimerEventListener#onTimerSet(TimerState)}. If
     * the timer is not awake (see {@link #isAwake()}), this cancellation
     * attempt will be ignored.
     * </p>
     * <p>
     * This method returns immediately. A command event is sent to the message
     * queue and will be handled in turn. The test of the waking state is
     * performed when the command event is handled, not when it is queued.
     * Therefore, it is possible for this method to return {@code false} and
     * then subsequently to actually perform a cancel operation and <i>vice
     * versa</i>.
     * </p>
     *
     * @return
     *     {@code true} if, at the instant that the command event to cancel
     *     the timer was queued, that the timer was in a state that could be
     *     cancelled; or {@code false} if the timer was not in a state that
     *     could be cancelled. The timer can be cancelled if it is not in the
     *     sleeping state and is not unused or already stopped. As the handling
     *     of the command event is asynchronous, there is no guarantee that the
     *     state will be the same when the command is handled, but it is likely
     *     that it will be the same.
     */
    public boolean cancel() {
        mCommandHandler.sendMessage(
            mCommandHandler.obtainMessage(MSG_WHAT_CANCEL, this));

        // This is mostly for the owning fragment's handling of "Back" button
        // presses: if "cancel()" had any effect, the "Back" event should be
        // consumed. Certainty is not really critical.
        return isAwake()
               && !getTimerState().isReset()
               && !getTimerState().isStopped();
    }

    /**
     * Handles the command event notifying that the timer has been cancelled.
     * The command event is queued by {@link #cancel()}.
     */
    private void cancelSync() {
        // NOTE: See comment in "onTouchDownSync()" for reason why "isAwake()"
        // is tested here.
        if (isAwake()
            && !getTimerState().isReset()
            && !getTimerState().isStopped()) {
            // This will cause a roll-back to the previous timer state, so
            // there is no need to stop any running timers, etc. Any "ticks"
            // are cancelled during the transition "tearDown" and any solve
            // reference is reset to null.
            transitionTo(CANCELLING);
        }
    }

    /**
     * <p>
     * Places this timer into its "sleep" state. While asleep, the timer does
     * not notify timer cues, refresh requests or other events, but the timer
     * is not paused. When awoken from this state by a call to {@link #wake()},
     * it will resume sending timer cues and refresh events and the time that
     * elapsed while the timer was sleeping will be counted towards the
     * elapsed time. If the timer is already asleep, no action will be taken
     * by this method.
     * </p>
     * <p>
     * While <i>in</i> the sleep state, the timer will not notify events, but,
     * if awake when this method is called, the timer will first be transitioned
     * to a safe state before sleeping quietly. <i>Synchronous</i>
     * notifications may be sent <i>during the transition</i> to the safe
     * state prior to sleeping. The safety transition is triggered by a
     * synchronous call from this method to cancel the current touch sequence
     * (i.e., as if {@link #onTouchCancelled()} were called, but
     * synchronously). The effect is that if the timer at a holding-for-start
     * or ready-to-start stage, i.e., the stages that are in effect only
     * while the user is holding down a touch, it will revert to a stage that
     * does not require the touch to be held. This ensures that when it
     * wakes, it will not be in an invalid state caused by the touch being
     * lifted while it was asleep.
     * </p>
     * <p>
     * If put to sleep when one of these holding stages and inspection has
     * not yet started, or if inspection is disabled and the solve has not
     * yet started, then the solve attempt will be cancelled and
     * {@link OnTimerEventListener#onTimerSet(TimerState)} will be called
     * <i>before</i> this method returns. That call may be preceded by calls
     * to {@link OnTimerEventListener#onTimerCue(TimerCue)} related to the
     * transition.
     * </p>
     * <p>
     * If put to sleep during one of the holding stages when inspection has
     * not yet started, or if inspection is disabled and the solve has not
     * yet started, then the solve attempt will be cancelled and
     * {@link OnTimerEventListener#onTimerSet(TimerState)} will be called
     * <i>before</i> this method returns. That call-back may be preceded by
     * calls to {@link OnTimerEventListener#onTimerCue(TimerCue)} related to
     * the transition. If put to sleep during one of the holding stages when
     * the inspection countdown is running, the timer will not be cancelled;
     * the timer will cancel the touch and return to the normal inspection
     * countdown stage before sleeping. This may trigger call-backs to
     * {@link OnTimerEventListener#onTimerCue(TimerCue)}.
     * </p>
     * <p>
     * If not at one of those holding stages when this method is called, no
     * call-backs will be notified during the transition to the sleeping state.
     * </p>
     * <p>
     * Synchronous call-backs are necessary when putting the timer to sleep.
     * If called from the {@code onPause()} method of an activity or fragment
     * that is undergoing a configuration change, an asynchronous sleep
     * command might not be handled until after the re-started activity or
     * fragment has transitioned all the way back to {@code onResumed()}, at
     * which point the sleep command would no longer be appropriate. The
     * call-backs need to be handled before {@code onPaused()} returns, as
     * that ensures that the user-interface is still present and there will
     * not be any problems if it needs to be updated.
     * </p>
     * <p>
     * See the class description for more details on the use of this method
     * when integrating a timer into the life-cycle of an activity or fragment.
     * </p>
     */
    // NOTE: This used to be asynchronous, like "wake()", but debug logging
    // clearly showed that calling "sleep()" from "Fragment.onPaused()"
    // before a configuration change resulted in "sleepSync()" being called
    // only after the restarted fragment reached "onResumed()".
    //
    // Call only from the UI thread, because synchronous call-backs to
    // methods that may update the user interface are expected.
    @UiThread
    public void sleep() {
        if (DEBUG_ME) Log.d(TAG, "sleep(): isAwake=" + isAwake());
        if (isAwake()) {

            // "onTouchCancelledSync()" must be called while the timer is awake,
            // otherwise it will do nothing. This call is the trigger for the
            // synchronous call-backs. Calling it limits the number of stages
            // at which the timer can be in "onSaveInstanceState()" or "wake()"
            // to just "UNUSED", "INSPECTION_STARTED", "SOLVE_STARTED" and
            // "STOPPED".
            onTouchCancelledSync();

            // Clear "mIsAwake" first to prevent unexpected call-backs during
            // "tearDown()".
            mIsAwake = false;
            tearDown(getTimerStage());

            // NOTE: As "sleep()" is supposed to stop all notifications,
            // sending a notification to confirm the timer is now asleep
            // would be a contradiction and, as the UI (fragment) probably
            // called "sleep()", calling it back would be redundant.
        }
    }

    /**
     * <p>
     * Wakes this timer from its "sleep" state. The timer is not paused while
     * it is asleep, it is just "quiet". When awoken from sleep, it will
     * resume sending timer cues and refresh events that were suspended by
     * calling {@link #sleep()} and the time that elapsed while the timer was
     * sleeping will be counted towards the elapsed time. Before notifying
     * any new timer cues, notification will first be sent to
     * {@link OnTimerEventListener#onTimerSet(TimerState)}.
     * </p>
     * <p>
     * If the timer is put into its sleep state, it may notify several timer
     * cues and other events in rapid succession when it is later awoken. For
     * example, the timer may enter the sleep state just before the inspection
     * countdown reaches zero and then be held in the sleep state for several
     * seconds, long enough for the inspection period to time out. When
     * {@code wake()} is then called, the timer <i>may</i> notify the timer cue
     * for overrunning the inspection period, the timer cue for timing out, and
     * the terminal event that delivers the "DNF" result that arises when
     * inspection times out. In such a case, the timer will attempt to skip
     * earlier cues and send only the last relevant cue or event notification.
     * However, this may dependent on external factors (e.g., the implementation
     * of the {@link PuzzleClock} and the behaviour of the {@code Looper}'s {
     * @code MessageQueue}), so there is no guarantee that redundant cues will
     * not be sent.
     * </p>
     * <p>
     * This method returns immediately. A command event is sent to the
     * message queue and will be handled in turn. If the timer is already
     * awake, no action will be taken when the command event is handled. As
     * the handling is asynchronous, {@link #isAwake()} will not return
     * {@code true} until after the command event is handled, which cannot be
     * before the method that calls {@code wake()} has returned.
     * </p>
     */
    public void wake() {
        if (DEBUG_ME) Log.d(TAG, "wake(): isAwake=" + isAwake());

        // NOTE: Check "isAwake()" in "wakeSync()", in case the state changes
        // while queued.
        mCommandHandler.sendMessage(
            mCommandHandler.obtainMessage(MSG_WHAT_WAKE, this));
    }

    /**
     * Handles the command event notifying that the timer has been awoken from
     * its sleep state. The command event is queued by {@link #wake()}.
     */
    private void wakeSync() throws IllegalStateException {
        if (DEBUG_ME) Log.d(TAG, "wakeSync(): isAwake=" + isAwake());
        if (!isAwake()) {
            // Set "mIsAwake" first, as this allows timer cues, etc. to fire
            // during "setUp".
            mIsAwake = true;

            // In the sleeping state, the stage can only be one of "UNUSED",
            // "INSPECTION_STARTED", "SOLVE_STARTED", or "STOPPED". "setUp()"
            // will fire "onTimerSet()" for any of those stages. As elsewhere,
            // "transitionTo()" is not called, as the current stage is not
            // going to change; it just needs to be set up.
            setUp(getTimerStage());
        }
    }

    /**
     * Indicates if the timer is currently in its waking state. When first
     * created, the timer will be in its sleeping state (as if {@link #sleep()}
     * were called. Until {@link #wake()} is called, the timer will not notify
     * listeners of timer cues, refreshes, or other events. These waking and
     * sleeping states do not affect the timer's measurement of elapsed time.
     * The timer is not "paused" while it is sleeping, so time that elapses
     * while sleeping is still measured.
     *
     * @return
     *     {@code true} if the timer is awake; or {@code false} if it is
     *     sleeping.
     */
    private boolean isAwake() {
        return mIsAwake;
    }
}
