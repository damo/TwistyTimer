package com.aricneto.twistytimer.timer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The {@link TimerState} instances that represent the current and previous
 * states of a {@link PuzzleTimer}. When the timer is started, {@link #push()}
 * backs up the current state at that instant to become the previous state and a
 * new, unused current state is created. If the timer is subsequently cancelled,
 * {@link #pop()} can undo that change, discarding the cancelled current state
 * and restoring the backed-up previous state to once again become the current
 * state. This class is also the {@code android.os.Parcelable} object that holds
 * the saved instance state of a {@link PuzzleTimer}.
 *
 * @author damo
 */
class JointTimerState implements Parcelable {
    /** JSON key name for the prototype timer state. */
    private static final String JK_PROTOTYPE_STATE = "t"; // for "template"

    /** JSON key name for the current timer state. */
    private static final String JK_CURRENT_STATE = "c";

    /** JSON key name for the previous timer state. */
    private static final String JK_PREVIOUS_STATE = "p";

    /**
     * The timer state for the most recent (including active) solve attempt.
     * This is retained when the timer is stopped and only reset when a new
     * solve attempt begins (or if it is explicitly reset). When a new solve
     * attempt begins, it is backed-up to {@link #mPreviousState} and may be
     * restored from there if the new solve attempt is cancelled.
     */
    @NonNull
    private TimerState mCurrentState;

    /**
     * The previous timer state, backed up to allow the active state to be
     * rolled back if a solve attempt is cancelled.
     */
    @NonNull
    private TimerState mPreviousState;

    /**
     * The prototype timer state used to create new, empty timer states with the
     * correct inspection duration and hold-to-start flag status.
     */
    @NonNull
    private TimerState mPrototypeState;

    /**
     * Creates a new joint timer state. The current and previous states will be
     * initialised at the {@link TimerStage#UNUSED} stage.
     *
     * @param inspectionDuration
     *     The duration of the normal inspection period (in milliseconds). This
     *     does <i>not</i> include any extra time allowed (under penalty) when
     *     the inspection countdown reaches zero. Use zero (or a negative) to
     *     indicate that there is no inspection period before starting the
     *     solve timer.
     * @param isHoldToStartEnabled
     *     {@code true} if, before starting the inspection countdown or solve
     *     timer, the user must "hold" their touch for a minimum duration before
     *     the timer will register the touch as an intention to start the solve
     *     timer; or {@code false} if, after touching down, the solve timer will
     *     start immediately when the touch is lifted up, regardless of how long
     *     the touch was held down.
     */
    JointTimerState(long inspectionDuration, boolean isHoldToStartEnabled) {
        this(new TimerState(inspectionDuration, isHoldToStartEnabled),
             null, null);
    }

    /**
     * Creates a new joint timer state. The prototype timer state is required.
     *
     * @param prototypeState
     *     The prototype timer state. Must not be {@code null}.
     * @param currentState
     *     The current timer state. Use {@code null} to set the current state
     *     to a new, unused state based on the prototype state.
     * @param previousState
     *     The previous timer state. Use {@code null} to set the previous state
     *     to a new, unused state based on the prototype state.
     */
    private JointTimerState(@NonNull  TimerState prototypeState,
                            @Nullable TimerState currentState,
                            @Nullable TimerState previousState) {
        mPrototypeState = prototypeState;
        mCurrentState   = currentState == null
            ? prototypeState.newUnusedState() : currentState;
        mPreviousState  = previousState == null
            ? prototypeState.newUnusedState() : previousState;
    }

    /**
     * Creates a new joint timer state from a parcel. The parcel must previously
     * have been written by {@link #writeToParcel(Parcel, int)}.
     *
     * @param in The parcel from which to read the state.
     */
    private JointTimerState(Parcel in) {
        this(readTimerState(in),  // prototype
             readTimerState(in),  // current
             readTimerState(in)); // previous
    }

    /**
     * Reads a timer state instance from a parcel. This is just a convenience
     * method that makes {@link #JointTimerState(Parcel)} more readable.
     *
     * @param in The parcel from which to read the timer state.
     *
     * @return The timer state read from the current position of the parcel.
     */
    private static TimerState readTimerState(Parcel in) {
        return in.readParcelable(JointTimerState.class.getClassLoader());
    }

    /**
     * Sets the prototype time state that will be used to define new timer
     * states. When new states are created by {@link #push()}, the prototype is
     * used to provide the inspection duration and hold-to-start flag status.
     * However, if the states are saved to a parcel and then restored, changes
     * to these configuration values may need to be updated for any new states
     * that will be created, so the prototype can be overwritten to inject those
     * new values. This does not affect any existing "used" timer states, only
     * new states or existing "unused" states. For example, if the timer state
     * is saved while the timer is running and the configuration values change
     * before the timer state is restored, then the restored timer will continue
     * to run using the old configuration values until it stops.
     *
     * @param prototypeState
     *     The prototype timer state that defines the configuration values.
     */
    void setPrototypeTimerState(@NonNull TimerState prototypeState) {
        mPrototypeState = prototypeState;

        // If the current or previous state is at the "UNUSED" stage, then
        // overwrite it, so that it will pick up the new configuration when
        // it is first used.
        if (mCurrentState.getStage() == TimerStage.UNUSED) {
            mCurrentState = mPrototypeState.newUnusedState();
        }

        if (mPreviousState.getStage() == TimerStage.UNUSED) {
            mPreviousState = mPrototypeState.newUnusedState();
        }

        // NOTE: Is this correct? While the inspection duration is probably
        // best retained in "mCurrentState" with its old value, should the
        // hold-to-start flag not just be reset? Would that cause problems
        // with a running timer if it were in a holding stage? Would that even
        // be possible? Maybe just leave it as it is for now. The only issue is
        // that if the settings are changed while the timer is running, it seems
        // logical that the inspection period should just continue on the old
        // countdown, but if a hold-to-start is changed, should a hold-to-start
        // of the solve timer during the inspection period not use the new
        // setting? There is no logical reason to use the old setting.
        //
        // UPDATE: On reflection, there will probably never be support in the
        // UI for changing the hold-to-start setting while a timer is actively
        // running. Therefore, ignoring this issue will keep things simple.
    }

    /**
     * Gets the prototype time state that will be used to define new timer
     * states.
     *
     * @return The prototype timer state that defines the configuration values.
     */
    @NonNull
    TimerState getPrototypeTimerState() {
        return mPrototypeState;
    }

    /**
     * Writes the prototype, current and previous states to a parcel.
     *
     * @param dest  The parcel to which to write the joint state.
     * @param flags Use zero.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mPrototypeState, flags);
        dest.writeParcelable(mCurrentState,   flags);
        dest.writeParcelable(mPreviousState,  flags);
    }

    /**
     * Pushes the current timer state to the previous state to back it up and
     * initialises a new current timer state in readiness to begin a new solve
     * attempt. If the attempt is abandoned or cancelled, call {@link #pop()}
     * to revert to the previous timer state. If the attempt runs to completion,
     * call {@link #commit(long)} to accept the new current state and erase the
     * previous timer state.
     *
     * @return The new "current" timer state.
     *
     * @throws IllegalStateException
     *     If the current stage is not {@link TimerStage#STOPPED STOPPED}.
     *     Pushing a new state from other stages is not permitted, as it
     *     would break a running timer or would overwrite the backed-up
     *     previous state.
     */
    TimerState push() throws IllegalStateException {
        // NOTE: This check will help to detect bugs that might otherwise be
        // tricky to pin down.
        final TimerStage stage = mCurrentState.getStage();

        if (stage != TimerStage.STOPPED) {
            throw new IllegalStateException(
                "Cannot 'push' the timer state when stage is '" + stage + "'.");
        }

        mPreviousState = mCurrentState;
        mCurrentState  = mPrototypeState.newUnusedState();

        return mCurrentState;
    }

    /**
     * <p>
     * Commits the current timer state by erasing any previous timer state and
     * updating the active {@code Solve} instance. Replaces the instance with
     * one that records the timer state's elapsed solve time, penalties and the
     * date-time stamp of the solve (which is set to the current system time).
     * </p>
     * <p>
     * If this is called immediately after {@link #pop()}, it has no effect,
     * as it simply re-commits the current state that was restored by the pop
     * (roll-back).
     * </p>
     *
     * @param nowRealTime
     *     The current system real time (in milliseconds since the Unix epoch).
     *     The value from {@link PuzzleClock#nowRealTime()} is appropriate.
     *     This will be used as the date-time stamp for this solve attempt.
     *
     * @return The "current" timer state that has been committed.
     *
     * @throws IllegalStateException
     *     If the {@code Solve} instance on the current state is {@code null}.
     *     The instance should have been set at the start of the solve attempt
     *     that is now being committed.
     */
    TimerState commit(long nowRealTime) throws IllegalStateException {
        mCurrentState.commitSolve(nowRealTime);

        // Erase the previous state. Bit of a no-op, but it avoids confusion.
        mPreviousState = mPrototypeState.newUnusedState();

        return mCurrentState;
    }

    /**
     * Rolls back the timer state if a solve attempt is cancelled. The new
     * "current" state that was created by {@link #push()} is discarded and
     * replaced by the "previous" state that was backed up by that method.
     * If no new state was created by {@code push()}, or if the backed-up
     * previous state was already discarded by {@link #commit(long)}, this
     * method will have the same effect as calling {@link #reset()}.
     *
     * @return
     *     The new "current" timer state. This is the restored "previous" state
     *     that replaced the popped (and discarded) state.
     *
     * @throws IllegalStateException
     *     If the current stage is not {@link TimerStage#CANCELLING}. Restoring
     *     the previous state when the current state is at any other stage is
     *     not permitted.
     */
    TimerState pop() throws IllegalStateException {
        // NOTE: This check will help to detect bugs that might otherwise be
        // tricky to pin down.
        final TimerStage stage = mCurrentState.getStage();

        if (stage != TimerStage.CANCELLING) {
            throw new IllegalStateException(
                "Cannot 'pop' the timer state when stage is '" + stage + "'.");
        }

        mCurrentState  = mPreviousState;
        mPreviousState = mPrototypeState.newUnusedState();

        return mCurrentState;
    }

    /**
     * Resets the joint state to the same state as when first constructed.
     * This <i>cannot</i> be undone by calling {@link #pop()}. The current
     * and previous timer states will be discarded and the current state will
     * be at the {@link TimerStage#UNUSED UNUSED} stage.
     *
     * @return The new "current" timer state.
     *
     * @throws IllegalStateException
     *     If the current stage is not already
     *     {@link TimerStage#STOPPED STOPPED} or {@code UNUSED}. Resetting
     *     the state from other stages is not permitted.
     */
    TimerState reset() {
        // NOTE: This check will help to detect bugs that might otherwise be
        // tricky to pin down.
        final TimerStage stage = mCurrentState.getStage();

        // Allow "UNUSED" as it is essentially a harmless no-op.
        if (stage != TimerStage.UNUSED && stage != TimerStage.STOPPED) {
            throw new IllegalStateException(
                "Cannot reset timer state when stage is '" + stage + "'.");
        }

        mCurrentState  = mPrototypeState.newUnusedState();
        mPreviousState = mPrototypeState.newUnusedState();

        return mCurrentState;
    }

    /**
     * Gets the current state of the timer. The "current" state is the state
     * of the running timer, or, if it is not running, the state of the timer
     * when it was last stopped. If the timer was not started since it was
     * first created (and no previous instance state was restored), the state
     * will be the default "0.00" state.
     *
     * @return The current state of the timer.
     */
    @NonNull
    TimerState getCurrentTimerState() {
        return mCurrentState;
    }

    /**
     * Describes any "special" contents of the {@code Parcelable} form of this
     * class.
     *
     * @return Always zero, as there are no special contents.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@code CREATOR} factory to satisfy the canonical Android
     * {@code Parcelable} implementation pattern.
     */
    public static final Parcelable.Creator<JointTimerState> CREATOR
        = new Parcelable.Creator<JointTimerState>() {
        @Override
        public JointTimerState createFromParcel(Parcel in) {
            return new JointTimerState(in);
        }

        @Override
        public JointTimerState[] newArray(int size) {
            return new JointTimerState[size];
        }
    };

    /**
     * Gets a JSON representation of this joint timer state. This is suitable
     * for longer-term persistence of the running timer state; longer than the
     * life of the fragment, activity or application process hosting the timer.
     * For more details on the appropriate contexts for the use of this method,
     * see {@link PuzzleTimer#onSavePersistentState()}.
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to
     *     the normal time base used by the timer. The value from
     *     {@link PuzzleClock#now()} is appropriate.
     * @param nowRealTime
     *     The current system real time (in milliseconds since the Unix epoch).
     *     The value from {@link PuzzleClock#nowRealTime()} is appropriate.
     *
     * @return A JSON object representing this timer state.
     */
    JSONObject toJSON(long now, long nowRealTime) {
        final JSONObject json = new JSONObject();

        try {
            json.put(JK_PROTOTYPE_STATE,
                     mPrototypeState.toJSON(now, nowRealTime));
            json.put(JK_CURRENT_STATE,
                     mCurrentState.toJSON(now, nowRealTime));
            json.put(JK_PREVIOUS_STATE,
                     mPreviousState.toJSON(now, nowRealTime));
        } catch (JSONException ignore) {
            // This can only happen if a key name used above is null. Ignore it.
        }

        return json;
    }

    /**
     * Restores a joint timer state from its JSON object form. The JSON object
     * must have been created by {@link #toJSON(long, long)}.
     *
     * @param json
     *     The JSON object containing the data for a joint timer state.
     * @param now
     *     The current instant in time (in milliseconds) with reference to
     *     the normal time base used by the timer. The value from
     *     {@link PuzzleClock#now()} is appropriate.
     * @param nowRealTime
     *     The current system real time (in milliseconds since the Unix epoch).
     *     The value from {@link PuzzleClock#nowRealTime()} is appropriate.
     *
     * @return A new joint timer state initialised from the JSON data.
     *
     * @throws JSONException
     *     If there is a problem initialising the joint timer state from the
     *     given JSON object. For example, if any of the required properties
     *     are not defined on the object.
     */
    static JointTimerState fromJSON(JSONObject json, long now, long nowRealTime)
            throws JSONException {
        return new JointTimerState(
            TimerState.fromJSON(
                json.getJSONObject(JK_PROTOTYPE_STATE), now, nowRealTime),
            TimerState.fromJSON(
                json.getJSONObject(JK_CURRENT_STATE), now, nowRealTime),
            TimerState.fromJSON(
                json.getJSONObject(JK_PREVIOUS_STATE), now, nowRealTime));
    }
}
