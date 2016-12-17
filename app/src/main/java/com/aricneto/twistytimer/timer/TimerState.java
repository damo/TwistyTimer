package com.aricneto.twistytimer.timer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.Solve;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import static com.aricneto.twistytimer.timer.PuzzleTimer
    .INSPECTION_1ST_WARNING_REMAINING_DURATION;
import static com.aricneto.twistytimer.timer.PuzzleTimer
    .INSPECTION_2ND_WARNING_REMAINING_DURATION;

/**
 * <p>
 * The internal state of a {@link PuzzleTimer} for a single solve attempt.
 * The puzzle timer uses {@link JointTimerState} to hold two instances of
 * {@code TimerState}, one for the current solve attempt, and one for the
 * previous solve attempt, to be restored if the current solve attempt is
 * cancelled. This class is {@code Parcelable} to allow it to be persisted.
 * Instances of this class are used to capture the instance state of a puzzle
 * timer via the {@code JointTimerState} class.
 * </p>
 * <p>
 * Access to many methods is restricted to classes in the same package, as
 * the state must not be changed outside of the context of the owning {@code
 * PuzzleTimer} instance. The public methods provide an immutable,
 * non-destructive API to support updating the timer's user-interface via the
 * call-backs of the {@link OnTimerEventListener} interface.
 * </p>
 *
 * @author damo
 */
public class TimerState implements Parcelable {
    /**
     * The duration of the "overrun" period. The overrun period starts when
     * the inspection countdown reaches zero and ends with the termination of
     * the attempt and a "DNF" penalty, unless the solve timer has been
     * started during that interval.
     */
    static final long INSPECTION_OVERRUN_DURATION = 2_000L;

    /**
     * The default refresh period (in milliseconds) for the display of the
     * remaining inspection time during the inspection countdown. The timer
     * can feed back its own preferred refresh period each time it is
     * notified of a refresh. For example, if the inspection countdown is
     * overrun, the timer can change to a more rapid refresh to show a higher
     * time precision for the remaining two seconds before the inspection
     * period times out.
     */
    private static final long DEFAULT_INSPECTION_TIMER_REFRESH_PERIOD = 1_000L;

    /**
     * The default refresh period (in milliseconds) for the display of the
     * elapsed solve time while the solve timer is running. The timer can
     * feed back its own preferred refresh period each time it is notified of
     * a refresh. For example, if the timer will hide the time while it is
     * running, or will never show fractions of a second (both user
     * preferences), then it can feed back a longer refresh period (or a
     * period of zero to disable refreshing) to reduce or eliminate
     * unnecessary refresh notifications.
     */
    // NOTE: Typically, the solve timer will show a resolution to 0.01
    // seconds. However, refreshing the timer display at 100 Hz is a bit OTT,
    // as the 1/100th digits would be a blur. It would probably be
    // indistinguishable from a refresh rate of 50 Hz or even 25 Hz. It is
    // not too hard to see the 1/10th digits cycle through 0 to 9, but when
    // much faster than that it would be a challenge. Say that three times
    // faster would be enough to convince the user that the 1/100th digits
    // cycle through 0 to 9, displaying each digit. The refresh rate would
    // then be 30 Hz, for a period of 33 ms (rounded). A little trick can be
    // employed to improve the "illusion": use a refresh period that is a
    // prime number. A refresh period of, say, 31 ms is close enough to 33 ms,
    // but it will cause the refresh events to fire (if "in phase") at
    // such times that the 1/100th digit will not follow any clear pattern
    // and will be sure to cover all digits before it cycles through them
    // again. Contrast this to using, say, a refresh period of 35 ms. If that
    // were used the refresh events would fire (if "in phase") only when the
    // 1/100th field contained a "0" or a "5", so it would probably look
    // quite wrong.
    private static final long DEFAULT_SOLVE_TIMER_REFRESH_PERIOD = 31L;

    /**
     * The start and stop time value used to indicate "not started" and "not
     * stopped".
     */
    // NOTE: "-1" is not used as it might be problematic with some schemes
    // used to support pausing and resuming the timer. See comments elsewhere
    // in this class for details.
    private static final long NEVER = Long.MIN_VALUE;

    /**
     * The time at which the inspection timer was started. Will be
     * {@link #NEVER} if inspection was not started, or has not yet started.
     */
    private long mInspStartedAt = NEVER;

    /**
     * The time at which the inspection timer was stopped. Will be
     * {@link #NEVER} if inspection was not started, or has not yet started,
     * or, if started, has not yet stopped.
     */
    private long mInspStoppedAt = NEVER;

    /**
     * The time at which the solve timer was started. Will be {@link #NEVER} if
     * the solve was not started, or has not yet started.
     */
    // NOTE: If implementing a "pause()" feature, a new "mIsSolvePaused" flag
    // could be added. "pause()" would save the current time in
    // "mSolveStoppedAt" and set "mIsSolvePaused" to true. "resume()" would
    // take the current time, subtract the difference between
    // "mSolveStartedAt" and "mSolveStoppedAt", use that result to set a new
    // value for "mSolveStartedAt", set "mSolveStoppedAt" back to "-1" and
    // set "mIsSolvePaused" to false. A similar effect could be achieved with
    // a new "mSolvePausedAt" field, which might be a bit less confusing than
    // combining a flag with two meanings for "mSolveStoppedAt". In either
    // case, "mSolveStartedAt" is updated so that the elapsed time between it
    // and "now" is correct on resuming.
    //
    // IMPORTANT: On second thoughts, the "mSolveStoppedAt", etc. use the
    // system uptime to measure elapsed time (which is the correct approach).
    // However, if the timer is paused and then the device is rebooted, the
    // correction required to the uptime might result in the start time being
    // negative (e.g., if the timer is paused at 5 minutes, the system is
    // rebooted, and the timer is started 1 minute after rebooting, the
    // "corrected" value for "mSolveStartedAt" would be -4 minutes. It would
    // also be possible for the start time to be corrected to "-1" and that
    // would cause all sorts of problems. Therefore, the time value used to
    // indicate "not started" and "not stopped" has been changed from -1 to
    // "Long.MIN_VALUE", which should be safer.
    private long mSolveStartedAt = NEVER;

    /**
     * The time at which the solve timer was stopped. Will be {@link #NEVER}
     * if the solve was not started, or has not yet started, or, if started,
     * has not yet stopped.
     */
    private long mSolveStoppedAt = NEVER;

    /**
     * The most recent "marked" time. This captures the current time at the
     * instant that the time is "marked". This value is used to provide a
     * stable result for the remaining inspection time and the elapsed solve
     * time where no "stopped-at" time has been recorded.
     */
    // This value is not persisted, as it would be out-of-date when restored.
    private transient long mMarkedAt = NEVER;

    /**
     * The duration (in milliseconds) of the normal inspection period for
     * this solve attempt. It will be zero (or negative) if there is no
     * inspection period.
     */
    private final long mInspDuration;

    /**
     * Indicates if the hold-to-start behaviour is enabled.
     */
    private final boolean mIsHoldToStartEnabled;

    /**
     * The current stage of the timer.
     */
    @NonNull
    private TimerStage mStage = TimerStage.UNUSED;

    /**
     * The timer cues that may still be notified to the user interface listener.
     */
    @NonNull
    private final Set<TimerCue> mTimerCues;

    /**
     * The refresh period (in milliseconds) used to update the display of the
     * current time. This is initialised to an appropriate default value when
     * the inspection countdown or solve timer are first started.
     */
    private long mRefreshPeriod = DEFAULT_SOLVE_TIMER_REFRESH_PERIOD;

    /**
     * The penalties incurred for this solve attempt.
     */
    @NonNull
    private Penalties mPenalties = Penalties.NO_PENALTIES;

    /**
     * The {@link Solve} instance associated with the solve attempt that is
     * described by this timer state. The {@code Solve} will capture the
     * results of the attempt.
     */
    @Nullable // May be null only when "mStage" is "UNUSED".
    private Solve mSolve;

    /**
     * Creates a new timer state. The new state reports that neither the
     * inspection countdown nor the solve timer have been started, that there
     * are no penalties, and that the current stage is
     * {@link TimerStage#UNUSED UNUSED}.
     *
     * @param inspectionDuration
     *     The duration of the normal inspection period (in milliseconds).
     *     This does <i>not</i> include any extra time allowed (under
     *     penalty) when the inspection countdown reaches zero. Use zero (or
     *     a negative) to indicate that there is no inspection period before
     *     starting the solve timer.
     * @param isHoldToStartEnabled
     *     {@code true} if, before starting the solve timer (though not the
     *     inspection timer), the user must "hold" their touch for a minimum
     *     duration before the timer will register the touch as an intention
     *     to start the solve timer; or {@code false} if, after touching
     *     down, the solve timer will start immediately when the touch is
     *     lifted up, regardless of how long the touch was held down.
     */
    TimerState(long inspectionDuration, boolean isHoldToStartEnabled) {
        mInspDuration = inspectionDuration;
        mIsHoldToStartEnabled = isHoldToStartEnabled;
        mTimerCues = loadTimerCues();
    }

    /**
     * Re-creates a timer state from a parcel. The timer state must have been
     * previous written to a parcel by {@link #writeToParcel(Parcel, int)}.
     *
     * @param in The parcel from which to restore the state.
     */
    protected TimerState(Parcel in) {
        mInspStartedAt = in.readLong();
        mInspStoppedAt = in.readLong();
        mSolveStartedAt = in.readLong();
        mSolveStoppedAt = in.readLong();
        mInspDuration = in.readLong();
        mIsHoldToStartEnabled = in.readByte() != 0;
        mRefreshPeriod = in.readLong();
        mPenalties = Penalties.decode(in.readInt());
        mSolve = in.readParcelable(getClass().getClassLoader()); // May be null.
        // "mMarkedAt" was not persisted, so its default field value is used.

        mStage = TimerStage.valueOf(in.readString());

        mTimerCues = EnumSet.noneOf(TimerCue.class);
        // Array may be empty, but will not be null.
        for (String cueName : in.createStringArray()) {
            mTimerCues.add(TimerCue.valueOf(cueName));
        }
    }

    /**
     * Writes this timer state to a {@code Parcel}. The state can later be
     * restored from the parcel. The current instant in time most recently
     * recorded by {@link #mark(long)} is not persisted to the parcel, as it
     * may be out-of-date when it is restored. Therefore, when restoring the
     * state from a parcel, consider marking the current time at that instant.
     * This will only be necessary if the restored state was tracking a
     * still-running inspection countdown or solve timer when it was saved
     * and if the elapsed/remaining time will be read before there is any
     * other opportunity to mark the current time.
     *
     * @param dest  The destination parcel to which to write this timer state.
     * @param flags Use zero.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mInspStartedAt);
        dest.writeLong(mInspStoppedAt);
        dest.writeLong(mSolveStartedAt);
        dest.writeLong(mSolveStoppedAt);
        dest.writeLong(mInspDuration);
        dest.writeByte((byte) (mIsHoldToStartEnabled ? 1 : 0));
        dest.writeLong(mRefreshPeriod);
        dest.writeInt(mPenalties.encode());
        dest.writeParcelable(mSolve, 0); // May be null.

        // Write the stage as a string and the current timer cues as an array
        // of strings using the names of the respective enum values. There are
        // several other ways to approach this using some other combination
        // of "Parcel" methods, but this has the benefit of being simple.
        dest.writeString(mStage.name());

        final String[] cueNames = new String[mTimerCues.size()];
        int i = 0;

        for (TimerCue cue : mTimerCues) {
            cueNames[i++] = cue.name();
        }

        dest.writeStringArray(cueNames);
    }

    /**
     * Describes the special contents of the {@code Parcel} representation of
     * this timer state.
     *
     * @return Always zero: there are no special contents.
     */
    @Override
    public int describeContents() {
        return 0; // No file descriptors, or suchlike.
    }

    /**
     * The standard {@code CREATOR} field required for the {@code Parcelable}
     * implementation.
     */
    public static final Creator<TimerState> CREATOR
        = new Creator<TimerState>() {
        @Override
        public TimerState createFromParcel(Parcel in) {
            return new TimerState(in);
        }

        @Override
        public TimerState[] newArray(int size) {
            return new TimerState[size];
        }
    };

    /**
     * Loads the set of timer cues that may be notified by the puzzle timer.
     * Only those cues that are appropriate for the configuration of this timer
     * state are loaded: the cues included depend on the duration of the
     * inspection period (if there is any inspection) and the enabled status of
     * the hold-to-start behaviour.
     *
     * @return
     *     The set of timer cues appropriate for the configuration of this
     *     timer state.
     */
    @NonNull
    private Set<TimerCue> loadTimerCues() {
        // NOTE: If new cues are added, it is all to easy to forget to update
        // this method to "load" those new cues. This occurred several times
        // during the early development iterations. Therefore, a very defensive
        // approach is taken here to ensure that nothing is added or omitted
        // inadvertently. If a new cue is omitted, an exception will be thrown.
        // If an old cue is removed, the code will not compile.

        final Set<TimerCue> cues = EnumSet.noneOf(TimerCue.class);

        for (TimerCue cue : TimerCue.values()) {
            final boolean isUsed;

            switch (cue) {
                case CUE_INSPECTION_READY_TO_START:
                case CUE_INSPECTION_STARTED:
                case CUE_INSPECTION_OVERRUN:
                case CUE_INSPECTION_TIME_OUT:
                case CUE_INSPECTION_SOLVE_READY_TO_START:
                    isUsed = isInspectionEnabled();
                    break;

                case CUE_INSPECTION_RESUMED:
                    // This cue is not loaded by default. It is fired from the
                    // "INSPECTION_STARTED" stage, but *only* if returning to
                    // that stage from "INSPECTION_SOLVE_HOLDING_FOR_START".
                    // Therefore, it is loaded "manually" by the latter stage.
                    isUsed = false;
                    break;

                case CUE_INSPECTION_HOLDING_FOR_START:
                case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                    isUsed = isInspectionEnabled() && isHoldToStartEnabled();
                    break;

                case CUE_INSPECTION_7S_REMAINING:
                    // The "elapsed time alarms" fire 7 and 3 seconds before
                    // the end of the inspection period. Do not add these cues
                    // if the inspection period is configured to be shorter,
                    // If the inspection period is exactly 7 or 3 seconds
                    // long, do *not* fire the respective cue at the instant
                    // inspection starts, as that would be pointless.
                    isUsed = isInspectionEnabled()
                             && mInspDuration
                                > INSPECTION_1ST_WARNING_REMAINING_DURATION;
                    break;

                case CUE_INSPECTION_3S_REMAINING:
                    isUsed = isInspectionEnabled()
                             && mInspDuration
                                > INSPECTION_2ND_WARNING_REMAINING_DURATION;
                    break;

                case CUE_SOLVE_HOLDING_FOR_START:
                    isUsed = !isInspectionEnabled() && isHoldToStartEnabled();
                    break;

                case CUE_SOLVE_READY_TO_START:
                    isUsed = !isInspectionEnabled();
                    break;

                case CUE_SOLVE_STARTED:
                case CUE_CANCELLING:
                case CUE_STOPPING:
                    isUsed = true;
                    break;

                default:
                    // If a new cue is added and it is not handled explicitly
                    // above, then throw a wobbler. This is safer than depending
                    // on code inspections being enabled and not ignored.
                    throw new UnsupportedOperationException(
                        "BUG: Add support for cue '" + cue + "'.");
            }

            if (isUsed) {
                cues.add(cue);
            }
        }

        return cues;
    }

    /**
     * Creates a new, unused (reset) timer state based on this timer state.
     * This is equivalent to calling the constructor and passing the same
     * values for the inspection duration and hold-to-start flag as are set on
     * this timer state. The current stage will be {@link TimerStage#UNUSED}.
     *
     * @return A new, unused timer state.
     */
    @NonNull
    TimerState newUnusedState() {
        return new TimerState(mInspDuration, mIsHoldToStartEnabled);
    }

    /**
     * <p>
     * Fires a timer cue. The cue cannot be fired again unless it is first
     * "reloaded" by a call to {@link #reloadTimerCue(TimerCue)}. If a cue has
     * already fired, this method will have no effect. To determine
     * (non-destructively) if a cue can be fired, test
     * {@link #canFireTimerCue(TimerCue)} before calling this method.
     * </p>
     * <p>
     * Note that this method only tracks the state of which cues can fire,
     * have already been fired, or have yet to be fired. No notification is
     * sent to any listener. That is the responsibility of the caller.
     * </p>
     *
     * @param cue The cue to fire.
     *
     * @return
     *     {@code true} if the cue was fired; or {@code false} it was not
     *     fired because it had already been fired, or is not appropriate for
     *     the configuration of this timer state (e.g., if there is no
     *     inspection period, then inspection-related cues cannot fire).
     */
    boolean fireTimerCue(@NonNull TimerCue cue) {
        // *Removed*, so cannot fire again unless "reloaded".
        return mTimerCues.remove(cue);
    }

    /**
     * "Reloads" a timer cue, allowing it to be fired. If the cue is already
     * "loaded", this method will have no effect. This can be used to allow a
     * cue to be fired again after it has been consumed by
     * {@link #fireTimerCue(TimerCue)}.
     *
     * @param cue The cue to be reloaded.
     */
    void reloadTimerCue(@NonNull TimerCue cue) {
        mTimerCues.add(cue);
    }

    /**
     * Indicates if a timer cue is "loaded" and can be fired. This is a
     * non-destructive test that predicts what {@link #fireTimerCue(TimerCue)}
     * will return when it is next called for the same timer cue.
     *
     * @param cue The timer cue to check for its "loaded" status.
     *
     * @return
     *     {@code true} if the cue can be fired; {@code false} if it cannot
     *     be fired.
     */
    boolean canFireTimerCue(@NonNull TimerCue cue) {
        return mTimerCues.contains(cue);
    }

    /**
     * Gets the current timer refresh period. See
     * {@link #setRefreshPeriod(long)} for details.
     *
     * @return
     *     The refresh period (in milliseconds); or zero, to disable refresh
     *     notifications.
     */
    long getRefreshPeriod() {
        return mRefreshPeriod;
    }

    /**
     * Sets the current timer refresh period. This defines the interval (in
     * milliseconds) between notifications of the current time to the
     * user-interface component that is presenting the timer. The current
     * time will be the remaining inspection time if the inspection countdown
     * is running, or the elapsed solve time if the solve timer is running.
     * The user-interface control that is listening for the refresh
     * notifications can feed back its own preferred refresh period to
     * something that it finds to be more appropriate. For more details on
     * setting the refresh period, see
     * {@link OnTimerRefreshListener#onTimerRefreshSolveTime(long, long)}.
     *
     * @param newRefreshPeriod
     *     The new refresh period (in milliseconds). As there is no use case
     *     for refreshing the display with a period of less than 10 ms, if
     *     the value is between 1 and 9 (inclusive), a value of 10 will be
     *     set. Similarly, there is unlikely to be a use case for a refresh
     *     period greater than one minute, so any value greater than 60,000
     *     ms (1 minute) will be set to 1 minute. The special value -1 can be
     *     used to restore the default refresh period for the
     *     currently-running timer (inspection countdown or solve timer).
     *
     * @return
     *     {@code true} if the new refresh period differs from the old
     *     refresh period and any scheduled "tick" events should be updated;
     *     or {@code false} if the new refresh period is the same as the old
     *     refresh period and any scheduled "tick" events are still valid.
     *
     * @throws IllegalArgumentException
     *     If the refresh period is not positive (greater than zero), other
     *     than the special value -1 that is used to restore the default
     *     refresh period.
     */
    boolean setRefreshPeriod(long newRefreshPeriod)
            throws IllegalArgumentException {

        final long oldRefreshPeriod = mRefreshPeriod;

        if (newRefreshPeriod == -1) {
            if (isInspectionRunning()) {
                mRefreshPeriod = DEFAULT_INSPECTION_TIMER_REFRESH_PERIOD;
            } else {
                mRefreshPeriod = DEFAULT_SOLVE_TIMER_REFRESH_PERIOD;
            }
        } else if (newRefreshPeriod > 0) {
            mRefreshPeriod = Math.max(10L,
                Math.min(60_000L, newRefreshPeriod)); // 10 ms to 1 min.
        } else {
            throw new IllegalArgumentException(
                "Refresh period must be positive, or -1 for the default: "
                + newRefreshPeriod);
        }

        return mRefreshPeriod != oldRefreshPeriod;
    }

    /**
     * Gets the appropriate "origin time" for scheduling refresh "tick"
     * events. The origin time will be the start time of the inspection
     * period, if the inspection countdown is still running; or the start
     * time of the solve attempt, if the solve timer is still running. For
     * more details on the origin time and ticking "in phase", see
     * {@link PuzzleClock#tickEvery}.
     *
     * @return
     *     The appropriate origin time depending on which timing phase is
     *     running. If neither timer phase is running, the result will be zero.
     */
    long getRefreshOriginTime() {
        if (isInspectionRunning()) {
            return mInspStartedAt;
        }
        if (isSolveRunning()) {
            return mSolveStartedAt;
        }

        return 0;
    }

    /**
     * <p>
     * Gets the {@link Solve} associated with this timer state. If the timer
     * state is reset or cancelled, it will not have a solve. The solve will be
     * set to the instance returned by {@link SolveHandler#onSolveStart()} only
     * when the timer has started. If the timer is cancelled, that reference to
     * the solve will be reset to {@code null}. When the timer stops, the solve
     * instance (which is immutable) will be replaced with a new instance that
     * records the elapsed solve time, any penalties incurred, and the date-time
     * stamp; all other properties will remain unchanged.
     * </p>
     * <p>
     * If, after the timer has stopped, the {@code Solve} is edited, the new
     * solve instance must be notified to the puzzle timer by passing it to
     * {@link PuzzleTimer#onSolveChanged(Solve)}. The timer will notify its
     * event listeners of the change to the timer's state. That new solve
     * instance will be used as the source for the values returned by
     * {@link #getElapsedSolveTime()}, {@link #getPenalties()} and
     * {@link #getResultTime()}. See the description of the latter method for
     * more details.
     * </p>
     *
     * @return
     *     The solve associated with this timer state; or {@code null} if there
     *     is no solve associated yet, or if the solve was cleared because the
     *     timer was cancelled or reset.
     */
    @Nullable
    public Solve getSolve() {
        return mSolve;
    }

    /**
     * Sets the {@link Solve} associated with this timer state.
     *
     * @param solve
     *     The solve to associate with this timer state. Must not be
     *     {@code null}.
     */
    void setSolve(@NonNull Solve solve) {
        mSolve = solve;
    }

    /**
     * <p>
     * Gets the penalties that have been incurred for this solve attempt. The
     * returned instance is immutable; penalties can only be incurred or
     * annulled via a {@link PuzzleTimer}.
     * </p>
     * <p>
     * If the timer is stopped and has recorded its result in a {@code Solve}
     * instance, that instance will be used as the source of the reported
     * penalties. This is the same as the approach described for
     * {@link #getResultTime()}.
     * </p>
     *
     * @return
     *     The penalties incurred for this solve attempt. May be
     *     {@link Penalties#NO_PENALTIES}, but will never be {@code null}.
     */
    @NonNull
    public Penalties getPenalties() {
        // TODO (maybe): Alternatively, use "mSolve" to store the penalties and
        // do not have any "mPenalties" field on this "TimerState" class.
        if (isStopped() && mSolve != null) {
            return mSolve.getPenalties();
        }

        return mPenalties;
    }

    /**
     * Incurs a pre-start penalty. A penalty will not be incurred if it is a
     * DNF penalty and a DNF has already been incurred, or because it is a
     * "+2" and the maximum number of pre-start "+2" penalties has already
     * been incurred.
     *
     * @param penalty The penalty to be incurred.
     *
     * @throws IllegalStateException
     *     If the timer has been stopped and its results have been recorded in
     *     the referenced {@code Solve} instance. In that state, update the
     *     penalties on the {@code Solve} and notify the puzzle timer by passing
     *     the updated solve to {@link PuzzleTimer#onSolveChanged(Solve)}.
     */
    void incurPreStartPenalty(@NonNull Penalty penalty)
            throws IllegalStateException {
        if (isStopped()) {
            throw new IllegalStateException(
                "Cannot incur penalties on TimerState if timer is stopped.");
        }

        mPenalties = mPenalties.incurPreStartPenalty(penalty);
    }

    /**
     * <p>
     * Indicates if this timer state is reset or not. The state is reset if it
     * has not recorded, is not currently recording, and is not currently
     * preparing to record, a solve attempt. If reset, the timer state has no
     * recorded solve and the user interface should present the reset timer
     * appropriately. For example, the timer display may show zero elapsed
     * time (or other appropriate value) and any editing controls for a solve
     * attempt will be disabled or hidden. The timer state is "reset" after
     * {@link PuzzleTimer#reset()} is called (e.g., when a solve is deleted),
     * or if no previous instance state has been restored (e.g., if the app has
     * just been started).
     * </p>
     * <p>
     * If this state is not reset, it does not necessarily mean that a solve
     * result is available, or even that the timer has started. The timer state
     * is reset on the instant that the user begins to interact with it, such
     * as when holding for a short time before releasing to start the timer.
     * This state may be notified to {@link OnTimerEventListener#onTimerSet}
     * just as the user starts interacting with the timer, but the state will
     * have moved to one of the holding or ready states before subsequently
     * notifying {@link OnTimerEventListener#onTimerCue} in response to those
     * interactions.
     * </p>
     * <p>
     * See also {@link #isStopped()} and {@link #isRunning()}.
     * </p>
     *
     * @return
     *     {@code true} if the timer state is reset; or {@code false} if the
     *     timer state is not reset.
     */
    public boolean isReset() {
        // NOTE: The public API uses "isReset()", rather than "isUnused()"; the
        // latter being a bit too "internal" and potentially confusing.
        return mStage == TimerStage.UNUSED;
    }

    /**
     * <p>
     * Indicates if this timer state is "stopped" or not. The state is "stopped"
     * if a solve attempt has been recorded. Once stopped, a timer cannot be
     * started again. A timer in the "reset" state is not "stopped", as the
     * solve attempt has not yet been started.
     * </p>
     * <p>
     * See also {@link #isReset()} and {@link #isRunning()}.
     * </p>
     *
     * @return
     *     {@code true} if the timer state is stopped; or {@code false} if the
     *     timer state is not stopped.
     */
    public boolean isStopped() {
        // NOTE: "CANCELLING" and "STOPPING" are transient stages that will not
        // be visible outside of the "PuzzleTimer", as the transition from, say,
        // "SOLVE_STARTED" -> "STOPPING" -> "STOPPED" is performed atomically
        // with respect to any outside observers.
        return mStage == TimerStage.STOPPED;
    }

    /**
     * <p>
     * Indicates if this timer state is currently tracking a running timer. The
     * state is "running" when either the inspection countdown (if enabled) or
     * the solve timer are running.
     * </p>
     * <p>
     * This method is similar to a test that {@link #isInspectionRunning()}
     * or {@link #isSolveRunning()} return {@code true}. However, between the
     * instants when the inspection countdown is stopped and the solve timer is
     * started, this method will still return {@code true}, while the other
     * methods could both return {@code false}. This allows a distinction
     * between the states when the timer is holding-to-start or ready-to-start
     * at the beginning of the life-cycle and the states that then follow. This
     * cannot be determined definitively by testing the other two methods, due
     * to the possibility that the timer was in the "gap" between the inspection
     * countdown stopping and the solve timer starting.
     * </p>
     * <p>
     * See also {@link #isReset()} and {@link #isStopped()}.
     * </p>
     *
     * @return
     *     {@code true} if the timer state is running; or {@code false} if the
     *     timer state is not running.
     */
    public boolean isRunning() {
        switch (mStage) {
            case UNUSED:
            case STARTING:
            case INSPECTION_HOLDING_FOR_START:
            case INSPECTION_READY_TO_START:
            case SOLVE_HOLDING_FOR_START:
            case SOLVE_READY_TO_START:
            case CANCELLING:
            case STOPPING:
            case STOPPED:
                return false;

            case INSPECTION_STARTED:
            case INSPECTION_SOLVE_HOLDING_FOR_START:
            case INSPECTION_SOLVE_READY_TO_START:
            case SOLVE_STARTED:
                return true;

            default:
                throw new UnsupportedOperationException(
                    "BUG! Add support to 'isRunning()' for: " + mStage);
        }
    }

    /**
     * Indicates if the inspection countdown timer is running. If inspection
     * is not enabled, it will not be reported as running. If inspection is
     * running, the solve timer will not be running, as the two are mutually
     * exclusive.
     *
     * @return
     *     {@code true} if the inspection countdown is running; or {@code false}
     *     if it is not running.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public boolean isInspectionRunning() {
        return mInspStartedAt != NEVER && mInspStoppedAt == NEVER;
    }

    /**
     * Indicates if the solve timer is running. If the solve timer is
     * running, the inspection countdown timer will not be running, as the
     * two are mutually exclusive.
     *
     * @return
     *     {@code true} if the solve timer is running; or {@code false} if it
     *     is not running.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public boolean isSolveRunning() {
        return mSolveStartedAt != NEVER && mSolveStoppedAt == NEVER;
    }

    /**
     * <p>
     * Gets the elapsed solve time. If the timer has not been started, or if
     * the solve attempt timed out during the inspection period, or if the
     * timer has started but no current time has been "marked", the elapsed
     * time will be zero. If the solve timer has started and has not yet
     * stopped, the elapsed time is calculated with respect to the most
     * recent current time instant recorded by {@link #mark(long)}.
     * </p>
     * <p>
     * If the timer is stopped and has recorded its result in a {@code Solve}
     * instance, that instance will be used as the source of the reported
     * elapsed solve time. This is the same as the approach described for
     * {@link #getResultTime()}, except time penalties are not included in the
     * elapsed solve time value.
     * </p>
     *
     * @return
     *    The elapsed solve time in milliseconds. The value <i>excludes</i> any
     *    time penalties.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public long getElapsedSolveTime() {
        if (isStopped() && mSolve != null) {
            // The solve reports the elapsed time inclusive of penalties, so
            // remove time penalties from the reported result.
            return mSolve.getExactTime()
                   - mSolve.getPenalties().getTimePenalty();
        }

        return mSolveStartedAt == NEVER ? 0
               : mSolveStoppedAt == NEVER ?
                   mMarkedAt == NEVER ? 0 : mMarkedAt - mSolveStartedAt
               : mSolveStoppedAt - mSolveStartedAt;
    }

    /**
     * <p>
     * Gets the precise result time for the solve attempt. This includes the
     * elapsed solve time and the sum of all pre-start and post-start "+2"
     * penalties (if any), but it does not include any elapsed inspection
     * time. If a solve attempt was stopped with a DNF because the inspection
     * period timed out, then the "+2" penalty incurred in the 2-second
     * overrun before the inspection period time-out will be the only component
     * of the reported result time.
     * </p>
     * <p>
     * If the solve timer has not been started, or if the timer has started
     * and no current time has been "marked", the elapsed solve time
     * component will be zero, so only "+2" penalties will contribute to the
     * total. If the solve timer has started and has not yet stopped, the
     * elapsed solve time component of the total is calculated with respect
     * to the most recent current time instant recorded by {@link #mark(long)}.
     * </p>
     * <p>
     * If the solve timer has been stopped and {@link #getSolve()} reports a
     * non-{@code null} {@code Solve} instance, then the result time is taken
     * from {@link Solve#getExactTime()}. Therefore, if the solve is edited
     * after the timer is stopped and {@link PuzzleTimer#onSolveChanged(Solve)}
     * is notified, the timer will update the solve referenced by this timer
     * state and that updated solve reference will be used as the source of the
     * result time value when it is accessed from the resulting call-back to
     * {@link OnTimerEventListener#onTimerSet(TimerState)}. This makes it easy
     * to keep the display of the timer up-to-date when edits are made.
     * </p>
     * <p>
     * A DNF penalty does not change the the calculation of the result time.
     * For example, if a DNF is incurred because the inspection period times
     * out, the result time will be (at least) the "+2" second for the
     * penalty incurred for overrunning the inspection time two seconds
     * before inspection timed out. However, the elapsed inspection time is
     * still <i>not</i> included in the result. If a DNF is incurred
     * (manually) after the solve timer is stopped, the result time will
     * include all pre-start and post-start "+2" penalties (if any) and the
     * elapsed solve time recorded when the timer was stopped. Ths allows the
     * result of solve attempt to count towards a cumulative time limit, even
     * when the attempt incurs a DNF penalty.
     * </p>
     *
     * @return
     *     The result time in milliseconds. The value <i>includes</i> any time
     *     penalties. The value is not rounded or truncated, so it maintains
     *     its millisecond precision. {@code WCAMath#roundResult(long)} may be
     *     used to round the value to an "official" time. However, the result
     *     time reported by this method is the time that should be recorded in
     *     the database, as the rounding should be applied only for presentation
     *     purposes, or prior to calculating average-of-N times or mean times.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public long getResultTime() {
        if (isStopped() && mSolve != null) {
            return mSolve.getExactTime(); // Already includes penalties.
        }

        return getElapsedSolveTime() + mPenalties.getTimePenalty();
    }

    /**
     * Indicates if the touch down must be held for a short period before it
     * will trigger the start of the solve timer. If enabled, touches that
     * are held down for shorter than the "hold" duration will result in the
     * timer returning to its previous state. There is no hold-to-start
     * behaviour prior to starting the inspection countdown. However, if
     * enabled, it applies to the start of the solve timer whether or not
     * inspection is enabled.
     *
     * @return
     *     {@code true} if an initial inspection period is enabled; or
     *     {@code false} if there is no inspection period and only solve timer
     *     will be active.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public boolean isHoldToStartEnabled() {
        return mIsHoldToStartEnabled;
    }

    /**
     * Indicates if there is an inspection period prior to starting the solve
     * timer. The duration of the inspection period can be retrieved from
     * {@link #getInspectionDuration()}.
     *
     * @return
     *     {@code true} if an initial inspection period is enabled; or
     *     {@code false} if there is no inspection period and only solve timer
     *     will be active.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public boolean isInspectionEnabled() {
        return mInspDuration > 0;
    }

    /**
     * Gets the elapsed inspection time. If the countdown has not been
     * started, or if inspection is not enabled, or if the countdown has
     * started but no current time has been "marked", the elapsed time will
     * be zero. If the countdown has started and has not yet stopped, the
     * elapsed time is calculated with respect to the most recent current
     * time instant recorded by {@link #mark(long)}. When presenting the
     * inspection countdown in the UI, it may be more convenient to use
     * {@link #getRemainingInspectionTime()}.
     *
     * @return The elapsed inspection time in milliseconds.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public long getElapsedInspectionTime() {
        return mInspStartedAt == NEVER ? 0
               : mInspStoppedAt == NEVER ?
                    mMarkedAt == NEVER ? 0 : mMarkedAt - mInspStartedAt
               : mInspStoppedAt - mInspStartedAt;
    }

    /**
     * Gets the remaining inspection time. If the inspection countdown has
     * not been started, or if the inspection countdown has started but no
     * current time has been "marked", the remaining time will be the full
     * duration of the normal inspection period. If there is no inspection
     * period before the solve attempt, the remaining time will be zero (the
     * caller can test {@link #isInspectionEnabled()} to find out). If the
     * normal inspection period has elapsed and the 2-second overrun period
     * is active, the remaining time will be negative. If the inspection
     * countdown has started and has not yet stopped, the remaining time is
     * calculated with respect to the most recent current time instant
     * recorded by {@link #mark(long)}.
     *
     * @return
     *     The remaining normal inspection time in milliseconds; negative if
     *     the inspection period has been overrun.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public long getRemainingInspectionTime() {
        if (mInspStartedAt != NEVER) {
            // Inspection has already started and may or may not already be
            // stopped.
            if (mInspStoppedAt != NEVER) {
                return mInspDuration - (mInspStoppedAt - mInspStartedAt);
            }

            if (mMarkedAt != NEVER) {
                // Inspection period is still counting down. The expected end
                // instant for the normal inspection period is the reference
                // used to calculate the remaining time.
                return mInspStartedAt + mInspDuration - mMarkedAt;
            }
        } else if (isInspectionEnabled()) {
            return mInspDuration;
        }

        // Inspection disabled. "mInspDuration" could be -ve, so use 0.
        return 0;
    }

    /**
     * Gets the current timer stage.
     *
     * @return The current timer stage.
     */
    @NonNull
    TimerStage getStage() {
        return mStage;
    }

    /**
     * Sets the current timer stage to a new stage
     *
     * @param newStage
     *     The new stage to set as the current timer stage.
     */
    void setStage(@NonNull TimerStage newStage) {
        mStage = newStage;
    }

    /**
     * Marks the current time that will be used to calculate the remaining
     * inspection time and elapsed solve time. This is only used when either
     * period has started but not yet stopped.
     *
     * @param now The current instant in time.
     *
     * @return This timer state, for convenience.
     */
    @NonNull
    TimerState mark(long now) {
        mMarkedAt = now;
        return this;
    }

    /**
     * Marks the instant in time when a solve starts. Any previously recorded
     * solve stop instant is reset. The default solve timer refresh period is
     * set, but may be changed by the refresh event listener.
     *
     * @param now The current instant in time.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException If the solve was previously started.
     */
    @NonNull
    TimerState startSolve(long now) throws IllegalStateException {
        // Exception will help to avoid nasty bugs that are hard to figure out.
        if (mSolveStartedAt != NEVER) {
            throw new IllegalStateException("Solve already started.");
        }

        mSolveStartedAt = now;
        mSolveStoppedAt = NEVER;
        setRefreshPeriod(DEFAULT_SOLVE_TIMER_REFRESH_PERIOD);

        return this;
    }

    /**
     * Marks the instant in time when a solve stops.
     *
     * @param now The current instant in time.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the solve was previously stopped, or has not yet been started.
     */
    @NonNull
    TimerState stopSolve(long now) throws IllegalStateException {
        if (mSolveStartedAt == NEVER || mSolveStoppedAt != NEVER) {
            throw new IllegalStateException(
                "Solve already stopped or never started.");
        }

        mSolveStoppedAt = now;
        return this;
    }

    /**
     * Gets the instant in time when the normal inspection countdown is due
     * to end. The time base is that used by the {@link PuzzleClock}, not the
     * system (wall) time. If the inspection countdown is not stopped before
     * the end of its normal period, the inspection may overrun by up to two
     * seconds before timing out. The normal end instant is before any
     * overrun or time-out. If the inspection countdown was not started (or
     * if there is no inspection period), the result will be 0.
     *
     * @return The instant when the normal inspection countdown is due to end.
     */
    long getInspectionEnd() {
        return isInspectionRunning() ? mInspStartedAt + mInspDuration : 0L;
    }

    /**
     * Gets the duration of the normal inspection period. This is the
     * duration from the start of the countdown until the countdown hit zero.
     * It does <i>not</i> include any overrun period that may follow for a
     * few seconds before the inspection period times out for good.
     *
     * @return
     *     The duration of the inspection period (in milliseconds). If zero,
     *     there is no inspection period before the solve timer is started,
     *     though {@link #isInspectionEnabled()} may be more convenient if
     *     that is all that needs to be tested.
     */
    long getInspectionDuration() {
        return mInspDuration;
    }

    /**
     * Marks the instant in time when inspection starts. Any previously
     * recorded inspection stop instant is reset. The default inspection
     * countdown timer refresh period is set, but may be changed by the
     * refresh event listener.
     *
     * @param now The current instant in time.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the inspection period was previously started.
     */
    TimerState startInspection(long now) throws IllegalStateException {
        if (mInspStartedAt != NEVER) {
            throw new IllegalStateException("Inspection already started.");
        }

        mInspStartedAt = now;
        mInspStoppedAt = NEVER;
        setRefreshPeriod(DEFAULT_INSPECTION_TIMER_REFRESH_PERIOD);

        return this;
    }

    /**
     * Marks the instant in time when inspection stops. This can be as late
     * as the end of the inspection overrun period, not just when the
     * inspection countdown reaches zero.
     *
     * @param now
     *     The current instant in time. If the inspection period timed out, pass
     *     {@code now} as -1 to automatically set the stop time and give a total
     *     elapsed inspection time that is exactly the normal inspection time
     *     plus the overrun duration. This corrects for any inaccuracies in the
     *     scheduling of the notification of the stop instant.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the inspection period was previously stopped, or has not yet been
     *     started.
     */
    @NonNull
    TimerState stopInspection(long now) throws IllegalStateException {
        if (mInspStartedAt == NEVER || mInspStoppedAt != NEVER) {
            throw new IllegalStateException(
                "Inspection already stopped or never started.");
        }

        mInspStoppedAt = now == -1L
            ? mInspStartedAt + mInspDuration + INSPECTION_OVERRUN_DURATION
            : now;

        return this;
    }

    /**
     * Gets a compact string representation of this timer state. This is
     * intended to support logging and debugging and may not be very efficient.
     *
     * @return A string representation of this timer state.
     */
    @Override
    public String toString() {
        // "never" is much more readable than "-9,223,372,036,854,775,808"!
        final String sStart = mSolveStartedAt == NEVER
            ? "never" : String.format(Locale.US, "%,d", mSolveStartedAt);
        final String sStop = mSolveStoppedAt == NEVER
            ? "never" : String.format(Locale.US, "%,d", mSolveStoppedAt);

        // Use the "getter" methods for the elapsed solve time and penalties,
        // as they will be taken from "mSolve", which may be updated after the
        // timer has stopped.
        if (isInspectionEnabled()) {
            final String iStart = mInspStartedAt == NEVER
                ? "never" : String.format(Locale.US, "%,d", mInspStartedAt);
            final String iStop = mInspStoppedAt == NEVER
                ? "never" : String.format(Locale.US, "%,d", mInspStoppedAt);

            return String.format(Locale.US,
                "%s{i0=%s; i1=%s; id=%,d; ir=%,d; ie=%,d; ip=+2x%d%s;"
                + " s0=%s; s1=%s; se=%,d; sp=+2x%d%s; rp=%,d}", mStage.name(),
                iStart, iStop, mInspDuration,
                getRemainingInspectionTime(), getElapsedInspectionTime(),
                getPenalties().getPreStartPlusTwoCount(),
                getPenalties().hasPreStartDNF() ? "+DNF" : "", sStart, sStop,
                getElapsedSolveTime(),
                getPenalties().getPostStartPlusTwoCount(),
                getPenalties().hasPostStartDNF() ? "+DNF" : "", mRefreshPeriod);
        }

        return String.format(Locale.US,
            "%s{s0=%s; s1=%s; se=%,d; sp=+2x%d%s; rp=%,d}",
            mStage.name(), sStart, sStop, getElapsedSolveTime(),
            getPenalties().getPostStartPlusTwoCount(),
            getPenalties().hasPostStartDNF() ? "+DNF" : "", mRefreshPeriod);
    }
}
