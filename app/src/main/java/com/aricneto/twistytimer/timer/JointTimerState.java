package com.aricneto.twistytimer.timer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.aricneto.twistytimer.items.Solve;

/**
 * The {@link TimerState} instances that represent the current and previous
 * states of a {@link PuzzleTimer}. When the timer is started, {@link #push()}
 * backs up the current state at that instant to become the previous state
 * and a new, unused current state is created. If the timer is subsequently
 * cancelled, {@link #pop()} can undo that change, discarding the cancelled
 * current state and restoring the backed-up previous state to once again
 * become the current state. This class is also the
 * {@code android.os.Parcelable} object that holds the saved instance state of
 * a {@link PuzzleTimer}.
 *
 * @author damo
 */
class JointTimerState implements Parcelable {
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
     *     The duration of the normal inspection period (in milliseconds).
     *     This does <i>not</i> include any extra time allowed (under
     *     penalty) when the inspection countdown reaches zero. Use zero (or
     *     a negative) to indicate that there is no inspection period before
     *     starting the solve timer.
     * @param isHoldToStartEnabled
     *     {@code true} if, before starting the inspection countdown or solve
     *     timer, the user must "hold" their touch for a minimum duration
     *     before the timer will register the touch as an intention to start
     *     the solve timer; or {@code false} if, after touching down, the
     *     solve timer will start immediately when the touch is lifted up,
     *     regardless of how long the touch was held down.
     */
    JointTimerState(long inspectionDuration, boolean isHoldToStartEnabled) {
        mPrototypeState
            = new TimerState(inspectionDuration, isHoldToStartEnabled);
        mCurrentState  = mPrototypeState.newUnusedState();
        mPreviousState = mPrototypeState.newUnusedState();
    }

    /**
     * Creates a new joint timer state from a parcel. The parcel must previously
     * have been written by {@link #writeToParcel(Parcel, int)}.
     *
     * @param in The parcel from which to read the state.
     */
    private JointTimerState(Parcel in) {
        mCurrentState   = in.readParcelable(getClass().getClassLoader());
        mPreviousState  = in.readParcelable(getClass().getClassLoader());
        mPrototypeState = in.readParcelable(getClass().getClassLoader());
    }

    /**
     * Sets the prototype time state that will be used to define new timer
     * states. When new states are created by {@link #push()}, the prototype
     * is used to provide the inspection duration and hold-to-start flag
     * status. However, if the states are saved to a parcel and then
     * restored, changes to these configuration values may need to be updated
     * for any new states that will be created, so the prototype can be
     * overwritten to inject those new values. This does not affect any
     * existing "used" timer states, only new states or existing "unused"
     * states. For example, if the timer state is saved while the timer is
     * running and the configuration values change before the timer state is
     * restored, then the restored timer will continue to run using the old
     * configuration values until it stops.
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

        // FIXME: Is this correct? While the inspection duration is probably
        // best retained in "mCurrentState" with its old value, should the
        // hold-to-start flag not just be reset? Would that cause problems
        // with a running timer if it was in a holding stage? Would that even
        // be possible? Maybe just leave it as it is for now. The only issue
        // is that if the settings are changed while the timer is running, it
        // seems logical that the inspection period should just continue on
        // the old countdown, but if a hold-to-start is changed, should a
        // hold-to-start of the solve timer during the inspection period not
        // use the new setting? There is no logical reason to use the old
        // setting.
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
     * Writes the current and previous state to a parcel.
     *
     * @param dest  The parcel to which to write the joint state.
     * @param flags Use zero.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mCurrentState, flags);
        dest.writeParcelable(mPreviousState, flags);
        dest.writeParcelable(mPrototypeState, flags);
    }

    /**
     * Pushes the current timer state to the previous state to back it up and
     * initialises a new current timer state in readiness to begin a new
     * solve attempt. If the attempt is abandoned or cancelled, call
     * {@link #pop()} to revert to the previous timer state. If the attempt
     * runs to completion, call {@link #commit()} to accept the new current
     * state and erase the previous timer state.
     *
     * @throws IllegalStateException
     *     If the current stage is not {@link TimerStage#STOPPED STOPPED}.
     *     Pushing a new state from other stages is not permitted, as it
     *     would break a running timer or would overwrite the backed-up
     *     previous state.
     */
    void push() throws IllegalStateException {
        // NOTE: This check will help to detect bugs that might otherwise be
        // tricky to pin down.
        final TimerStage stage = mCurrentState.getStage();

        if (stage != TimerStage.STOPPED) {
            throw new IllegalStateException(
                "Cannot push() the timer state when stage is '" + stage + "'.");
        }

        mPreviousState = mCurrentState;
        mCurrentState = mPrototypeState.newUnusedState();
    }

    /**
     * <p>
     * Commits the current timer state by erasing any previous timer state and
     * updating the active {@code Solve} instance. Updates the solve with the
     * elapsed solve time, any penalties and the date-time stamp of the solve
     * (which is set to the current system time).
     * </p>
     * <p>
     * If this is called immediately after {@link #pop()}, it has no effect,
     * as it simply re-commits the current state that was restored by the pop
     * (roll-back).
     * </p>
     *
     * @throws IllegalStateException
     *     If the {@code Solve} instance on the current state is {@code null}.
     *     The instance should have been set at the start of the solve attempt
     *     that is now being committed.
     */
    void commit() throws IllegalStateException {
        final Solve newSolve = mCurrentState.getSolve();

        if (newSolve == null) {
            throw new IllegalStateException("The solve was never set.");
        }

        // The elapsed time that is set includes any time penalties.
        newSolve.setTime(mCurrentState.getResultTime());
        newSolve.setPenalties(mCurrentState.getPenalties());
        newSolve.setDate(System.currentTimeMillis());

        // Erase the previous state. Bit of a no-op, but it avoids confusion.
        mPreviousState = mPrototypeState.newUnusedState();
    }

    /**
     * Rolls back the timer state if a solve attempt is cancelled. The new
     * "current" state that was created by {@link #push()} is discarded and
     * replaced by the "previous" state that was backed up by that method.
     * If no new state was created by {@code push()}, or if the backed-up
     * previous state was already discarded by {@link #commit()}, this method
     * will have the same effect as calling {@link #reset()}.
     *
     * @throws IllegalStateException
     *     If the current stage is not {@link TimerStage#STOPPED STOPPED}.
     *     Restoring the previous state when the current state is at any
     *     other stage is not permitted.
     */
    void pop() throws IllegalStateException {
        // NOTE: This check will help to detect bugs that might otherwise be
        // tricky to pin down.
        final TimerStage stage = mCurrentState.getStage();

        if (stage != TimerStage.STOPPED) {
            throw new IllegalStateException(
                "Cannot pop() the timer state when stage is '" + stage + "'.");
        }

        mCurrentState = mPreviousState;
        mPreviousState = mPrototypeState.newUnusedState();
    }

    /**
     * Resets the joint state to the same state as when first constructed.
     * This <i>cannot</i> be undone by calling {@link #pop()}. The current
     * and previous timer states will be discarded and the current state will
     * be at the {@link TimerStage#UNUSED UNUSED} stage.
     *
     * @throws IllegalStateException
     *     If the current stage is not already
     *     {@link TimerStage#STOPPED STOPPED} or {@code UNUSED}. Resetting
     *     the state from other stages is not permitted.
     */
    void reset() {
        // NOTE: This check will help to detect bugs that might otherwise be
        // tricky to pin down.
        final TimerStage stage = mCurrentState.getStage();

        // Allow "UNUSED" as it is essentially a harmless no-op.
        if (stage != TimerStage.UNUSED && stage != TimerStage.STOPPED) {
            throw new IllegalStateException(
                "Cannot reset timer state when stage is '" + stage + "'.");
        }

        mCurrentState = mPrototypeState.newUnusedState();
        mPreviousState = mPrototypeState.newUnusedState();
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
}
