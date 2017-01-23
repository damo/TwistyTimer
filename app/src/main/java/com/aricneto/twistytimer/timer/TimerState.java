package com.aricneto.twistytimer.timer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.Solve;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
 * Pausing of the timer is supported for the elapsed solve time only, not for
 * the inspection countdown. The elapsed solve time for a solve attempt could
 * be quite lengthy. Even proficient users will take a few minutes to solve a
 * 7x7x7 cube and the doorbell might ring during that time! If support is added
 * for the very largest puzzles, the solve could take several hours and be
 * attempted in sessions over a number of days. Therefore, a pause facility
 * that can survive app restarts and device reboots, has some justification.
 * On the other hand, the inspection countdown is typically short (the
 * regulation inspection time is 15 seconds), so the user can simply cancel the
 * solve attempt during the inspection countdown and start a new attempt when
 * ready.
 * </p>
 * <p>
 * Access to many methods is restricted to classes in the same package, as
 * the state must not be changed outside of the context of the owning
 * {@code PuzzleTimer} instance. The public methods provide an immutable,
 * non-destructive API to support updating the timer's user-interface via the
 * call-backs of the {@link OnTimerEventListener} interface.
 * </p>
 * <p>
 * Many methods take the value (in milliseconds) of the current instant in time
 * as a parameter. Timing will be most reliable if the time base is the current
 * system "uptime", not the system real ("wall") time. The "uptime" is not
 * affected by unpredictable changes to the system clock, such as when the clock
 * is re-synchronised to a remote source (e.g., using NTP). Whatever time base
 * is chosen, it must be used consistently for all calls. The value from
 * {@link PuzzleClock#now()} is appropriate in most cases, which the system
 * real time value from {@link PuzzleClock#nowRealTime()} being required also
 * for some methods where indicated in their descriptions.
 * </p>
 * <p>
 * <i>Note: the {@link #equals(Object)} method is not consistent with the
 * {@code hashCode()} method; instances that are equal by the former may not
 * have the same hash code.</i>
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
     * The flag value used for the inspection duration to indicate that the
     * inspection period is disabled. All other values for the inspection
     * duration will be positive and indicate that inspection is enabled.
     */
    private static final long INSPECTION_DISABLED = 0L;

    /**
     * The default refresh period (in milliseconds) for the display of the
     * remaining inspection time during the inspection countdown. The timer can
     * feed back its own preferred refresh period each time it is notified of a
     * refresh. For example, if the inspection countdown is overrun, the timer
     * can change to a more rapid refresh to show a higher time precision for
     * the remaining two seconds before the inspection period times out.
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
    // NOTE: Typically, the solve timer will show a resolution to 0.01 seconds.
    // However, refreshing the timer display at 100 Hz is a bit OTT, as the
    // 1/100th digits would be a blur. It would probably be indistinguishable
    // from a refresh rate of 50 Hz or even 25 Hz. It is not too hard to see
    // the 1/10th digits cycle through 0 to 9, but if much faster than that it
    // would be a challenge. Say that 3x faster would be enough to convince the
    // user that the 1/100th digits cycle through 0 to 9, displaying each digit.
    // The refresh rate would then be 30 Hz, for a period of approx. 33 ms.
    // A little trick can be employed to improve the "illusion": use a refresh
    // period that is a prime number. A refresh period of, say, 31 ms is close
    // enough to 33 ms, but it will cause the refresh events to fire (in phase)
    // at such instants that the 1/100th digit will not follow any clear pattern
    // and will be sure to cover all digits before it cycles through them again.
    // Contrast this to using, say, a refresh period of 35 ms. If that were used
    // the refresh events would fire (in phase) only when the 1/100th field
    // contained a "0" or a "5", so it would probably look quite wrong.
    private static final long DEFAULT_SOLVE_TIMER_REFRESH_PERIOD = 31L;

    /** JSON key name for the system real time at persistence. */
    private static final String JK_PERSISTED_AT_REAL_TIME = "pt";

    /** JSON key name for the hold-to-start flag. */
    private static final String JK_HOLD_TO_START_ENABLED = "h";

    /** JSON key name for the inspection duration. */
    private static final String JK_INSP_DURATION = "id";

    /** JSON key name to flag that the inspection timer is running. */
    private static final String JK_INSP_RUNNING = "ir";

    /** JSON key name for the inspection extra time. */
    private static final String JK_INSP_EXTRA = "ix";

    /** JSON key name to flag that the solve timer is running. */
    private static final String JK_SOLVE_RUNNING = "sr";

    /** JSON key name to flag that the solve timer is paused. */
    private static final String JK_SOLVE_PAUSED = "sp";

    /** JSON key name for the solve extra time. */
    private static final String JK_SOLVE_EXTRA = "sx";

    /** JSON key name for the timer stage. */
    private static final String JK_TIMER_STAGE = "ts";

    /** JSON key name for the timer cues. */
    private static final String JK_TIMER_CUES = "c";

    /** JSON key name for the incurred penalties. */
    private static final String JK_PENALTIES = "p";

    /** JSON key name for the {@code Solve} instance. */
    private static final String JK_SOLVE = "s";

    /**
     * The start and stop time value used to indicate "not started" and "not
     * stopped".
     */
    // NOTE: "-1" is not used as it might be problematic with some schemes
    // used to support pausing and resuming the timer. See comments elsewhere
    // in this class for details.
    @VisibleForTesting
    static final long NEVER = Long.MIN_VALUE;

    /**
     * The start time value used to indicate the paused state. When paused, the
     * corresponding stop time value will always be {@link #NEVER}.
     */
    private static final long PAUSED = Long.MIN_VALUE + 1L;

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
     * Extra time to be added to the elapsed inspection time after calculating
     * the difference between the start and stop times. This supports long-term
     * persistence or the state of a running timer, e.g., across app restarts
     * or device reboots (pausing the countdown is not supported).
     */
    private long mInspExtraTime;

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
     * Extra time to be added to the elapsed solve time after calculating the
     * difference between the start and stop times. This supports long-term
     * persistence of the state of a running timer, e.g., across app restarts,
     * device reboots, or pausing of the solve timer.
     */
    private long mSolveExtraTime;

    /**
     * The most recent "marked" time. This captures the current time at the
     * instant that the time is "marked". This value is used to provide a stable
     * result for the remaining inspection time and the elapsed solve time where
     * no "stopped-at" time has been recorded.
     */
    // This value is not persisted, as it would be out-of-date when restored.
    private transient long mMarkedAt = NEVER;

    /**
     * The duration (in milliseconds) of the normal inspection period for this
     * solve attempt. It will be {@link #INSPECTION_DISABLED} (zero) if there
     * is no inspection period.
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
    // NOTE: This is not persisted to JSON (only to Parcelable instance state).
    // It is updated after each refresh event, so persistence is unnecessary.
    // The "TimerState" constructor does *not* check if inspection is enabled
    // and, if so, change this to "DEFAULT_INSPECTION_TIMER_REFRESH_PERIOD".
    // The initial value does not really matter, as this field will be updated
    // after each refresh event, with the listener feeding back its preferred
    // refresh period, depending on several factors, including preferences, the
    // magnitude of the time, the inspection/solve phase, etc.
    private long mRefreshPeriod = DEFAULT_SOLVE_TIMER_REFRESH_PERIOD;

    /**
     * The penalties incurred for this solve attempt.
     */
    @NonNull
    private Penalties mPenalties = Penalties.NO_PENALTIES;

    /**
     * The {@link Solve} instance associated with the solve attempt that is
     * described by this timer state. The {@code Solve} will capture the results
     * of the attempt.
     */
    @Nullable // May be null only when "mStage" is "UNUSED".
    private Solve mSolve;

    /**
     * Creates a new timer state. The new state reports that neither the
     * inspection countdown nor the solve timer have started, that there are
     * no penalties, and that the timer is reset (see {@link #isReset()}).
     *
     * @param inspectionDuration
     *     The duration of the normal inspection period (in milliseconds). This
     *     does <i>not</i> include any extra time allowed (under penalty) when
     *     the inspection countdown reaches zero. Use zero (or a negative) to
     *     indicate that there is no inspection period before starting the solve
     *     timer. If negative, the value will be stored and reported as zero.
     * @param isHoldToStartEnabled
     *     {@code true} if, before starting the solve timer (though not the
     *     inspection timer), the user must "hold" their touch for a minimum
     *     duration before the timer will register the touch as an intention
     *     to start the solve timer; or {@code false} if, after touching
     *     down, the solve timer will start immediately when the touch is
     *     lifted up, regardless of how long the touch was held down.
     */
    TimerState(long inspectionDuration, boolean isHoldToStartEnabled) {
        // NOTE: A zero inspection duration means "disabled". Allowing negative
        // values to mean the same thing avoids the need for any validation, so
        // there is no need to throw an exception from this constructor, which
        // is good practice for constructors in general. Converting negative
        // values to "INSPECTION_DISABLED" (zero) avoids confusion later.
        mInspDuration = Math.max(INSPECTION_DISABLED, inspectionDuration);
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
        mInspDuration = in.readLong();
        mIsHoldToStartEnabled = in.readByte() != 0;

        mInspStartedAt = in.readLong();
        mInspStoppedAt = in.readLong();
        mInspExtraTime = in.readLong();

        mSolveStartedAt = in.readLong();
        mSolveStoppedAt = in.readLong();
        mSolveExtraTime = in.readLong();

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
     * <p>
     * Writes this timer state to a {@code Parcel}. The state can later be
     * restored from the parcel. The current instant in time most recently
     * recorded by {@link #mark(long)} is not persisted to the parcel, as it
     * may be out-of-date when it is restored. Therefore, when restoring the
     * state from a parcel, consider marking the current time at that instant.
     * This will only be necessary if the restored state was tracking a
     * still-running inspection countdown or solve timer when it was saved
     * and if the elapsed/remaining time will be read before there is any
     * other opportunity to mark the current time.
     * </p>
     * <p>
     * The parcelable form of the timer state is not suitable for long-term
     * persistence, as it cannot accommodate the resetting of the system uptime
     * if the device is rebooted. For long-term persistence, use the JSON form
     * of the timer state created by {@link #toJSON(long, long)}.
     * </p>
     *
     * @param dest  The destination parcel to which to write this timer state.
     * @param flags Use zero.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mInspDuration);
        dest.writeByte((byte) (mIsHoldToStartEnabled ? 1 : 0));

        dest.writeLong(mInspStartedAt);
        dest.writeLong(mInspStoppedAt);
        dest.writeLong(mInspExtraTime);

        dest.writeLong(mSolveStartedAt);
        dest.writeLong(mSolveStoppedAt);
        dest.writeLong(mSolveExtraTime);

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
     * Only those cues that are appropriate for the configuration of this
     * timer state are loaded: the cues included depend on the duration of the
     * inspection period (if there is any inspection) and the enabled status
     * of the hold-to-start behaviour.
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
     * Gets the current timer refresh period. If no refresh period has yet been
     * set, the default period is set to be suitable for refreshing the solve
     * timer when it is displaying a high-resolution time (to 1/100ths of a
     * second). For more details, see {@link #setRefreshPeriod(long)}.
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
     * milliseconds) between notifications of the timer's current time to the
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
     * Gets the appropriate "origin time" for scheduling refresh "tick" events.
     * The origin time will be the start time of the inspection period, if the
     * inspection countdown is still running; or the start time of the solve
     * attempt, if the solve timer is still running. Corrections may be made to
     * allow for timers restored after being persisted, or timers that have been
     * paused. For more details on the origin time and ticking "in phase", see
     * {@link PuzzleClock#tickEvery}.
     *
     * @return
     *     The appropriate origin time depending on which timing phase is
     *     running. If neither timer phase is running, or the solve timer is
     *     paused, the result will be zero. The origin time may be negative.
     *     The origin time is expressed in milliseconds and uses the normal
     *     time base of the running timer, i.e., it should use the same time
     *     base as {@link PuzzleClock#now()}.
     */
    long getRefreshOriginTime() {
        // The origin time is the instant with respect to the "PuzzleClock" time
        // base (system uptime, typically) at which the timer was started from
        // zero. However, if the timer was paused or restored from its JSON
        // persistent form, the original start instant is lost and the instants
        // recorded by "mInspStartedAt" or "mSolveStartedAt" need to be adjusted
        // for any "extra" time. This adjustment could cause the origin time to
        // be negative, but the "DefaultPuzzleClock" can handle that when the
        // refresh events are scheduled.
        return isInspectionRunning()
                   ? mInspStartedAt  - mInspExtraTime
             : isSolveRunning() && !isSolvePaused()
                   ? mSolveStartedAt - mSolveExtraTime
             : 0L;
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
     * Commits this timer state's recorded values to the {@code Solve} instance.
     * This applies the elapsed solve time and penalties recorded in this timer
     * state by the puzzle timer to the {@code Solve} instance, replacing that
     * instance with a new one. The given real time value is used to set the
     * date-time stamp for the solve attempt.
     *
     * @param nowRealTime
     *     The current system real time (in milliseconds since the Unix epoch).
     *     The value from {@link PuzzleClock#nowRealTime()} is appropriate.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the {@code Solve} instance on the current state is {@code null}.
     *     The instance should have been set at the start of the solve attempt
     *     that is now being committed.
     */
    @NonNull
    TimerState commitSolve(long nowRealTime) throws IllegalStateException {
        if (mSolve == null) {
            throw new IllegalStateException("The solve was never set.");
        }

        setSolve(mSolve
            .withTimeExcludingPenalties(getElapsedSolveTimeIgnoringSolve())
            .withPenaltiesAdjustingTime(getPenaltiesIgnoringSolve())
            .withDate(nowRealTime));

        return this;
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
        if (isStopped() && mSolve != null) {
            return mSolve.getPenalties(); // Updated by "commitSolve()".
        }

        return getPenaltiesIgnoringSolve();
    }

    /**
     * Gets the penalties that have been incurred for this solve attempt. This
     * performs the same operation described for {@link #getPenalties()}, but it
     * will <i>never</i> delegate to a referenced {@code Solve} instance; the
     * penalties are always taken from this timer state.
     *
     * @return
     *     The penalties incurred for this solve attempt. May be
     *     {@link Penalties#NO_PENALTIES}, but will never be {@code null}.
     */
    @NonNull
    private Penalties getPenaltiesIgnoringSolve() {
        return mPenalties;
    }

    /**
     * Incurs a pre-start penalty. A penalty will not be incurred if it is a DNF
     * penalty and a DNF has already been incurred, or because it is a "+2" and
     * the maximum number of pre-start "+2" penalties has already been incurred.
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
     * instants when the inspection countdown is stopped and the solve timer
     * is started, this method will still return {@code true}, while the other
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
                // Same reason as for "TimerCue" values in "loadTimerCues()":
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
     * <p>
     * Indicates if the solve timer is running. If the solve timer is running,
     * the inspection countdown timer will not be running, as the two are
     * mutually exclusive.
     * </p>
     * <p>
     * If the solve timer is paused, it is still reported as "running", as the
     * timer has been started but has not yet been stopped. To identify the
     * paused status of a running solve timer, call {@link #isSolvePaused()}.
     * </p>
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
     * Indicates if the solve timer is paused. A paused timer is still reported
     * as running, as it has been started but not (permanently) stopped. See
     * {@link #isSolveRunning()} for more details.
     *
     * @return
     *     {@code true} if the solve timer was started but not yet stopped and
     *     is currently paused; or {@code false} if the timer is not paused.
     *
     * @see #pauseSolve(long)
     * @see #resumeSolve(long)
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public boolean isSolvePaused() {
        return mSolveStartedAt == PAUSED;
    }

    /**
     * <p>
     * Gets the elapsed solve time. If the timer has not been started, or if the
     * solve attempt timed out during the inspection period, or if the timer has
     * started but no current time has been "marked" (see below), the elapsed
     * time will be zero. If the solve timer has started and has not yet
     * stopped, the elapsed time is calculated with respect to the most recent
     * current time instant recorded by {@link #mark(long)}.
     * </p>
     * <p>
     * If the timer is paused, the "mark" is reset, but all elapsed time prior
     * to the pause instant is recorded. That elapsed time will be reported
     * while the timer is paused. It will also be reported after the timer is
     * resumed until a new current time is marked. The elapsed time, in the
     * absence of a current time mark, will be reported similarly if the state
     * is persisted to JSON and then restored: the elapsed time at the instant
     * of persistence will be reported until a new current time is marked.
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
            // remove time penalties from the reported result. These values
            // will (one hopes) have been updated by "commitSolve()".
            return mSolve.getExactTime()
                   - mSolve.getPenalties().getTimePenalty();
        }

        return getElapsedSolveTimeIgnoringSolve();
    }

    /**
     * Gets the elapsed solve time. This performs the same operation described
     * for {@link #getElapsedSolveTime()}, but it will <i>never</i> delegate to
     * a referenced {@code Solve} instance; the values are always taken from
     * this timer state.
     *
     * @return
     *    The elapsed solve time in milliseconds. The value <i>excludes</i> any
     *    time penalties.
     */
    private long getElapsedSolveTimeIgnoringSolve() {
        return mSolveExtraTime
               + (mSolveStartedAt == NEVER || mSolveStartedAt == PAUSED ? 0
                  : mSolveStoppedAt == NEVER ?
                        mMarkedAt == NEVER ? 0 : mMarkedAt - mSolveStartedAt
                  : mSolveStoppedAt - mSolveStartedAt);
    }

    /**
     * <p>
     * Gets the precise result time for the solve attempt. This includes the
     * elapsed solve time and the sum of all pre-start and post-start "+2"
     * penalties (if any), but it does not include any elapsed inspection time.
     * If a solve attempt was stopped with a DNF because the inspection period
     * timed out, then the "+2" penalty incurred in the 2-second overrun before
     * the inspection period time-out will be the only component of the reported
     * result time.
     * </p>
     * <p>
     * If the solve timer has not been started, or if the timer has started and
     * no current time has been "marked", the elapsed solve time component will
     * be zero, so only "+2" penalties will contribute to the total. If the
     * solve timer has started and has not yet stopped, the elapsed solve time
     * component of the total is calculated with respect to the most recent
     * current time instant recorded by {@link #mark(long)}.
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
     * penalty incurred for overrunning the inspection time two seconds before
     * inspection timed out. However, the elapsed inspection time is still
     * <i>not</i> included in the result. If a DNF is incurred (manually) after
     * the solve timer is stopped, the result time will include all pre-start
     * and post-start "+2" penalties (if any) and the elapsed solve time
     * recorded when the timer was stopped. Ths allows the result of solve
     * attempt to count towards a cumulative time limit, even when the attempt
     * incurs a DNF penalty.
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
            return mSolve.getExactTime(); // This value includes penalties.
        }

        return getElapsedSolveTimeIgnoringSolve()
               + getPenaltiesIgnoringSolve().getTimePenalty();
    }

    /**
     * Indicates if the touch down must be held for a short period before it
     * will trigger the start of the solve timer. If enabled, touches that are
     * held down for shorter than the "hold" duration will result in the timer
     * returning to its previous state. There is no hold-to-start behaviour
     * prior to starting the inspection countdown. However, if enabled, it
     * applies to the start of the solve timer whether or not inspection is
     * enabled.
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
        return mInspDuration != INSPECTION_DISABLED;
    }

    /**
     * Gets the elapsed inspection time. If the countdown has not been started,
     * or if inspection is not enabled, or if the countdown has started but no
     * current time has been "marked", the elapsed time will be zero. If the
     * countdown has started and has not yet stopped, the elapsed time is
     * calculated with respect to the most recent current time instant recorded
     * by {@link #mark(long)}. If presenting the inspection countdown in the UI,
     * it may be more convenient to use {@link #getRemainingInspectionTime()}.
     *
     * @return The elapsed inspection time in milliseconds.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public long getElapsedInspectionTime() {
        return mInspExtraTime
               + (mInspStartedAt == NEVER ? 0
                  : mInspStoppedAt == NEVER ?
                         mMarkedAt == NEVER ? 0 : mMarkedAt - mInspStartedAt
                  : mInspStoppedAt - mInspStartedAt);
    }

    /**
     * Gets the remaining inspection time. If the inspection countdown has not
     * been started, or if the inspection countdown has started but no current
     * time has been "marked", the remaining time will be the full duration of
     * the normal inspection period. If there is no inspection period before
     * the solve attempt, the remaining time will be zero (the caller can test
     * {@link #isInspectionEnabled()} to find out). If the normal inspection
     * period has elapsed and the 2-second overrun period is active, the
     * remaining time will be negative. If the inspection countdown has started
     * and has not yet stopped, the remaining time is calculated with respect
     * to the most recent current time instant recorded by {@link #mark(long)}.
     * If the time has not been "marked", the result will be zero.
     *
     * @return
     *     The remaining normal inspection time in milliseconds; negative if
     *     the inspection period has been overrun.
     */
    // Needs to be public to support alternatives to "TimerView".
    @SuppressWarnings("WeakerAccess")
    public long getRemainingInspectionTime() {
        return isInspectionEnabled()
            ? mInspDuration - getElapsedInspectionTime()
            : 0L;
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
     * <p>
     * Marks the current time that will be used to calculate the remaining
     * inspection time and elapsed solve time. This is only used when either
     * period has started but not yet stopped.
     * </p>
     * <p>
     * <i>Warning:</i> There is an assumption that the time base increases
     * monotonically (i.e., time always runs forwards and never backwards). If
     * the time base (typically the system uptime) is reset while the device is
     * still running and a timer is running, the timer will no longer be able to
     * determine how much time has elapsed. The timer will perceive an abrupt
     * backward jump in time which could cause many problems. To avoid these,
     * this method will check that a new current time mark is not in the past
     * relative to the previous time mark or to the start time of any active
     * timer. If the time base is reset, this will cause the timer to appear to
     * stop running until the uptime "catches up" to its previous value. This
     * is very unlikely, however, and should not be a concern in practice.
     * </p>
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer. The current time value from
     *     {@link PuzzleClock#now()} is appropriate.
     *
     * @return This timer state, for convenience.
     */
    @NonNull
    TimerState mark(long now) {
        // NOTE: If time were allowed to run backwards, it could cause all
        // sorts of problems with negative solve times, etc.
        if (isInspectionRunning()) {
            mMarkedAt = mMarkedAt == NEVER
                ? Math.max(mInspStartedAt, now)
                : Math.max(mInspStartedAt, Math.max(mMarkedAt, now));
        } else if (isSolveRunning()) {
            // When paused, "mSolveStartedAt" is set to "PAUSED", so just accept
            // the mark, as it will have no bearing on the elapsed solve time
            // until the solve timer is resumed.
            mMarkedAt = mMarkedAt == NEVER
                ? isSolvePaused()
                    ? now
                    : Math.max(mSolveStartedAt, now)
                : isSolvePaused()
                    ? Math.max(mMarkedAt, now)
                    : Math.max(mSolveStartedAt, Math.max(mMarkedAt, now));
        } else {
            mMarkedAt = mMarkedAt == NEVER ? now : Math.max(mMarkedAt, now);
        }

        return this;
    }

    /**
     * Gets the value of the current time mark. {@link #mark(long)} may adjust
     * the last-notified value if it appears incorrect, so the value returned
     * by this method is not necessarily the same as the value that was passed
     * to {@code mark(long)}. Access to this value is made available for the
     * purposes of testing; it has no other use.
     *
     * @return
     *     The value of the current time mark. May be {@link #NEVER} if it was
     *     not set, or was reset by other operations.
     */
    @VisibleForTesting
    long getMark() {
        return mMarkedAt;
    }

    /**
     * Marks the instant in time when a solve starts. The difference between
     * this start instant and the later stop instant ({@link #stopSolve(long)})
     * gives the final elapsed time. Before the timer stops, the difference
     * between this start instant and the last marked time ({@link #mark(long)})
     * gives the running elapsed time. Any previous mark is reset by this
     * method. Corrections are made automatically if the timer is paused or
     * persisted to, and restored from, JSON. The default solve timer refresh
     * period is set, but that period may be changed by the refresh event
     * listener. If the timer is paused, it should be resumed by calling
     * {@link #resumeSolve(long)}.
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer. The value from
     *     {@link PuzzleClock#now()} is appropriate.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the solve was previously started, or if an inspection period is
     *     enabled but has not been completed, or if the solve timer is paused.
     */
    @NonNull
    TimerState startSolve(long now) throws IllegalStateException {
        // Exceptions will help to avoid nasty bugs that are hard to figure out.
        if (isInspectionEnabled() && mInspStoppedAt == NEVER) {
            throw new IllegalStateException(
                "Solve timer cannot start before inspection is complete.");
        }
        if (mSolveStartedAt == PAUSED) {
            // Allowing "startSolve()" to perform the same operation as
            // "resumeSolve()" makes testing more difficult, as it inflates the
            // number of initial conditions for which to test "startSolve()".
            throw new IllegalStateException(
                "Solve timer already started and paused.");
        }
        if (mSolveStartedAt != NEVER) {
            throw new IllegalStateException("Solve timer already started.");
        }

        mSolveStartedAt = now;
        mSolveStoppedAt = NEVER;
        mSolveExtraTime = 0L;
        setRefreshPeriod(DEFAULT_SOLVE_TIMER_REFRESH_PERIOD);

        // This is important. If not done and time was previously marked during
        // the inspection period, the elapsed solve time would appear negative.
        // (It could also be set to "now", but "NEVER" is more consistent.)
        mMarkedAt = NEVER;

        return this;
    }

    /**
     * Marks the instant in time when a solve stops.
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer. If this is earlier in time than
     *     the start time (or resume time, if paused) of the timer, the later
     *     time will be used instead, which could result in zero elapsed time,
     *     but never negative elapsed time.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the solve was previously stopped, or has not yet been started.
     */
    @NonNull
    TimerState stopSolve(long now) throws IllegalStateException {
        if (!isSolveRunning()) { // Paused still counts as running.
            throw new IllegalStateException(
                "Solve timer already stopped or never started.");
        }

        // The timer can be stopped when it is in a paused state. For example,
        // the user may have hit "Pause" by mistake instead of "Stop" and may
        // want to stop the timer without starting it again and then stopping
        // it as quickly as possible. In this case, the last "interval" between
        // by the start and stop instants will be set to zero by setting both
        // instants to "now"; "mSolveExtraTime" then records all elapsed time.
        if (mSolveStartedAt == PAUSED) {
            mSolveStartedAt = now;
        }

        // Make sure that time cannot "run backwards". This could happen if the
        // system uptime were reset while the timer was running. The result will
        // be zero elapsed time (excl. "extra" time), which is probably wrong,
        // but is unavoidable and better than a problematic negative time.
        //
        // NOTE: An exception is not thrown, as this could be caused by the
        // system resetting the uptime, which will just result in the timer
        // appearing to stop running, rather than a crash.
        mSolveStoppedAt = Math.max(now, mSolveStartedAt);

        return this;
    }

    /**
     * Pauses the running solve timer. When the timer is resumed, the time that
     * elapsed between the instant that the timer is paused and the instant that
     * it is resumed (see {@link #resumeSolve(long)}) is not included in the
     * total elapsed time. The timer may only be paused if it is currently
     * running. The current "marked" time is reset and will need to be updated
     * after the paused timer is resumed before changes to the elapsed time can
     * be reported.
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer. If this is earlier in time than
     *     the last start time (or resume time, if previously paused), then the
     *     later time will be used and this time will be ignored.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the solve timer is not currently running or is already paused.
     */
    @NonNull
    TimerState pauseSolve(long now) throws IllegalStateException {
        // Pausing a paused timer is not a valid use case, so it is better to
        // throw an exception rather than allow it and possibly mask a bug.
        if (mSolveStartedAt == PAUSED) {
            throw new IllegalStateException("Solve timer already paused.");
        }
        if (!isSolveRunning()) {
            throw new IllegalStateException(
                "Solve timer already stopped or never started.");
        }

        // Transfer all elapsed time to "mSolveExtraTime". It may already have
        // accumulated some time if the timer was paused and resumed before.
        // See "stopSolve()" for notes on the defensive "max" correction.
        mSolveExtraTime += Math.max(0, now - mSolveStartedAt);
        mSolveStartedAt = PAUSED;

        // The "mark" will not be valid once the timer has been paused: elapsed
        // real time or uptime is not accumulated as elapsed solve time while
        // the timer is paused.
        mMarkedAt = NEVER;

        return this;
    }

    /**
     * Resumes the paused solve timer. The time that elapsed between the instant
     * that the timer is paused (see {@link #pauseSolve(long)}) and the instant
     * that it is resumed is not included in the total elapsed time. The timer
     * may only be resumed if it is currently paused. Any previous time mark
     * (see {@link #mark(long)}) is reset.
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the solve timer is not currently paused.
     */
    @NonNull
    TimerState resumeSolve(long now) throws IllegalStateException {
        // In theory, just checking for "PAUSED" should be enough, as that
        // implies that the solve timer must be running. However, just in case
        // the code changes and a stopped timer has a "PAUSED" start instant...
        if (!isSolveRunning()) {
            throw new IllegalStateException(
                "Solve timer already stopped or never started.");
        }
        if (mSolveStartedAt != PAUSED) {
            throw new IllegalStateException("Solve timer is not paused.");
        }

        // All elapsed time at the instant pausing was recorded in
        // "mSolveExtraTime" and it will remain there.
        mSolveStartedAt = now;
        mMarkedAt = NEVER;

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
     * @return
     *     The instant when the normal inspection countdown is due to end. This
     *     could be negative. The value is in milliseconds with reference to
     *     the normal time base used by the timer.
     */
    long getInspectionEnd() {
        return isInspectionRunning()
            ? mInspStartedAt - mInspExtraTime + mInspDuration
            : 0L;
    }

    /**
     * Gets the duration of the normal inspection period. This is the duration
     * from the start of the countdown until the countdown hit zero. It does
     * <i>not</i> include any overrun period that may follow for a few seconds
     * before the inspection period times out for good.
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
     * Marks the instant in time when inspection starts. Any previously-recorded
     * inspection stop instant is reset. The default inspection countdown timer
     * refresh period is set, but may be changed by the refresh event listener.
     * Any previous time mark (see {@link #mark(long)}) is reset.
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the inspection period was previously started, or if inspection is
     *     not enabled.
     */
    TimerState startInspection(long now) throws IllegalStateException {
        if (!isInspectionEnabled()) {
            throw new IllegalStateException("Inspection is not enabled.");
        }

        if (mInspStartedAt != NEVER) {
            throw new IllegalStateException("Inspection already started.");
        }

        mInspStartedAt = now;
        mInspStoppedAt = NEVER;
        mInspExtraTime = 0L;
        setRefreshPeriod(DEFAULT_INSPECTION_TIMER_REFRESH_PERIOD);

        // For consistency with "startSolve(long)".
        mMarkedAt = NEVER;

        return this;
    }

    /**
     * Marks the instant in time when inspection stops. This can be as late as
     * the end of the inspection overrun period, not just when the inspection
     * countdown reaches zero.
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer. If the value of {@code now}
     *     would result in the elapsed inspection time being greater than the
     *     inspection duration plus the overrun duration, the value will be set
     *     to exactly the latter duration. If the inspection period timed out,
     *     pass {@code now} as -1 to automatically set the stop time and give a
     *     total elapsed inspection time that is exactly the normal inspection
     *     duration plus the overrun duration. This corrects for any
     *     inaccuracies in the scheduling of the notification of the stop
     *     instant causing it to arrive either slightly early or slightly late.
     *
     * @return This timer state, for convenience.
     *
     * @throws IllegalStateException
     *     If the inspection period was previously stopped, or has not yet been
     *     started; or if inspection is not enabled.
     */
    @NonNull
    TimerState stopInspection(long now) throws IllegalStateException {
        if (!isInspectionEnabled()) {
            throw new IllegalStateException("Inspection is not enabled.");
        }

        if (mInspStartedAt == NEVER || mInspStoppedAt != NEVER) {
            throw new IllegalStateException(
                "Inspection already stopped or never started.");
        }

        final long maxTime = mInspDuration + INSPECTION_OVERRUN_DURATION;

        // If the timer state has been persisted to JSON while running and the
        // device has been rebooted before the state is restored, the stopped-at
        // time might be calculated to be before the started-at time when a
        // large value of "extra" time is accounted for (i.e., it is possible
        // for "mInspExtraTime" to be greater than "maxTime"). It is cleaner to
        // just reset the relevant fields to a simpler state. The instants do
        // not matter if the timer is stopped, only the difference matters.
        if (now == -1L) {
            mInspStartedAt = 0L;
            mInspStoppedAt = maxTime;
            mInspExtraTime = 0L;
        } else {
            // See "stopSolve()" for notes on the defensive "max" correction.
            mInspStoppedAt = Math.max(now, mInspStartedAt);

            if (mInspExtraTime + mInspStoppedAt - mInspStartedAt > maxTime) {
                // Recorded time would be greater than the maximum, so clip it.
                mInspStartedAt = 0L;
                mInspStoppedAt = maxTime;
                mInspExtraTime = 0L;
            }
        }

        return this;
    }

    /**
     * <p>
     * Indicates if this timer state is equal to another object. The object must
     * not be {@code null}, must be a {@code TimerState} instance, and must have
     * the same configuration for the inspection period and hold-to-start
     * behaviour, the same elapsed inspection time, the same elapsed solve time,
     * the same set of unfired timer cues, be at the same "stage", have the same
     * penalties and have the same refresh period. If this timer state has a
     * non-{@code null} {@code Solve} reference, the object must reference an
     * equal {@code Solve} (by {@link Solve#equals(Object)}). The last marked
     * time (see {@link #mark(long)}) is <i>not</i> compared; however, if a
     * timer is running, it must have a marked time in order to report its
     * elapsed time, which <i>is</i> compared. The refresh origin time is not
     * compared.
     * </p>
     * <p>
     * The instants when the inspection or solve timers were started and/or
     * stopped are not compared, only the elapsed time. Timers that have been
     * paused and resumed, or timers that have been persisted to JSON and then
     * restored while running, may still be equal to timers that have not
     * undergone such operations. However, see {@link #getElapsedSolveTime()}
     * for details on how the reported elapsed time of the former timers may be
     * non-zero even in the absence of a current time mark, unlike the latter
     * timers (never paused or persisted) which will report zero in the absence
     * of a current time mark.
     * </p>
     *
     * @param obj
     *     The other object to test against this timer state for equality.
     *
     * @return
     *     {@code true} if the object is equal to this timer state; or
     *     {@code false} if it is not.
     */
    // NOTE: There is no corresponding and consistent "hasCode()" method, as
    // this "equals(Object)" method is only used for testing purposes.
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TimerState) {
            final TimerState o = (TimerState) obj;

            return
                isHoldToStartEnabled() == o.isHoldToStartEnabled()
                && getInspectionDuration() == o.getInspectionDuration()
                && isSolveRunning() == o.isSolveRunning()
                && isSolvePaused() == o.isSolvePaused()
                && isInspectionRunning() == o.isInspectionRunning()
                // "isRunning()", "isReset()", etc. depend on the TimerStage:
                && getStage() == o.getStage()
                && getRemainingInspectionTime()
                    == o.getRemainingInspectionTime()
                && getElapsedSolveTime() == o.getElapsedSolveTime()
                && getPenalties().equals(o.getPenalties())
                && ((getSolve() != null && getSolve().equals(o.getSolve()))
                    || (getSolve() == null && o.getSolve() == null))
                && mTimerCues.equals(o.mTimerCues);
        }

        return false;
    }

    /**
     * Gets a compact string representation of this timer state. This is
     * intended to support logging and debugging and may not be very efficient.
     *
     * @return A string representation of this timer state.
     */
    @Override
    public String toString() {
        // Use the "getter" methods for the elapsed solve time and penalties,
        // as they will be taken from "mSolve", which may be updated after the
        // timer has stopped.
        if (isInspectionEnabled()) {
            return String.format(Locale.US,
                "%s{i0=%s; i1=%s; ix=%,d; id=%,d; ir=%,d; ie=%,d; ip=+2x%d%s;"
                + " s0=%s; s1=%s; sx=%,d; se=%,d; sp=+2x%d%s; rp=%,d}",
                mStage.name(),
                formatInstant(mInspStartedAt), formatInstant(mInspStoppedAt),
                mInspExtraTime, mInspDuration,
                getRemainingInspectionTime(), getElapsedInspectionTime(),
                getPenalties().getPreStartPlusTwoCount(),
                getPenalties().hasPreStartDNF() ? "+DNF" : "",
                formatInstant(mSolveStartedAt), formatInstant(mSolveStoppedAt),
                mSolveExtraTime, getElapsedSolveTime(),
                getPenalties().getPostStartPlusTwoCount(),
                getPenalties().hasPostStartDNF() ? "+DNF" : "", mRefreshPeriod);
        }

        return String.format(Locale.US,
            "%s{s0=%s; s1=%s; sx=%,d; se=%,d; sp=+2x%d%s; rp=%,d}",
            mStage.name(),
            formatInstant(mSolveStartedAt), formatInstant(mSolveStoppedAt),
            mSolveExtraTime,
            getElapsedSolveTime(), getPenalties().getPostStartPlusTwoCount(),
            getPenalties().hasPostStartDNF() ? "+DNF" : "", mRefreshPeriod);
    }

    /**
     * Formats an instant in time for use in {@link #toString()}.
     *
     * @param instant
     *     The time instant to be formatting (in milliseconds). The special flag
     *     values {@link #NEVER} and {@link #PAUSED} are formatted to "never"
     *     and "paused", respectively.
     *
     * @return
     *     The time instant formatted to a string.
     */
    private static String formatInstant(long instant) {
        // "never" is much more readable than "-9,223,372,036,854,775,808", etc.
        return instant == NEVER  ? "never"
             : instant == PAUSED ? "paused"
             : String.format(Locale.US, "%,d", instant);
    }

    /**
     * <p>
     * Gets a JSON representation of this timer state. This is suitable for
     * longer-term persistence of the running timer state; longer than the life
     * of the fragment, activity or application process hosting the timer. See
     * the description of {@link PuzzleTimer#onSavePersistentState()} for more
     * details on how a potential reset of the system uptime is accommodated.
     * </p>
     * <p>
     * The current refresh period is not persisted. As some real time may elapse
     * while the timer state is persisted, the "old" refresh period may not be
     * appropriate when the state is restored. For example, the state might be
     * persisted while the timer is running and showing a high-resolution time
     * of under 10 minutes, but it might be restored after the typical 10-minute
     * limit on high-resolution times has passed and a longer refresh period
     * would not be more appropriate. When the state of a running timer is
     * restored, a refresh event is fired immediately, so the listener can then
     * decide what "new" refresh period is the most appropriate and return it
     * to the puzzle timer that notified it of the event.
     * </p>
     *
     * @param now
     *     The current instant in time (in milliseconds) with reference to
     *     the normal time base used by the timer. The value from
     *     {@link PuzzleClock#now()} is appropriate. {@link #mark(long)} will
     *     be called with this value to allow the elapsed inspection and/or
     *     solve time to be recorded in the JSON object.
     * @param nowRealTime
     *     The current system real time (in milliseconds since the Unix epoch).
     *     The value from {@link PuzzleClock#nowRealTime()} is appropriate. If
     *     the timer is running when its state is saved, the later value of the
     *     system real time passed to {@link #fromJSON(JSONObject, long, long)}
     *     will be compared with this time to determine how much time elapsed
     *     while the state of the timer was persisted. When the state is
     *     restored, this elapsed real time will be added to any elapsed time
     *     recorded before the state was saved.
     *
     * @return
     *     A JSON object representing this timer state.
     */
    // NOTE: Injecting the system real time makes testing much easier.
    JSONObject toJSON(long now, long nowRealTime) {
        // If "mark()" is not invoked by the caller of this method, then the
        // elapsed time could be reported as zero and would not be valid. As
        // it is not possible to enforce that the caller invoke "mark()" just
        // before "toJSON()", it needs to be done here.
        mark(now);

        final JSONObject json = new JSONObject();

        try {
            // The system real time must be recorded to allow the restoration
            // of the timer state if the system "uptime" is reset while the
            // state is persisted. For example, if the app is closed while the
            // timer is running, the device is then rebooted and the app
            // re-started. This causes the uptime to be reset, so the old time
            // base (the uptime before the reboot) is no longer consistent with
            // the new time base (the uptime after the reboot). Therefore, field
            // values that are set to time instants based on the uptime cannot
            // be persisted as is. Instead, the elapsed durations are calculated
            // and these are used on restoration (in "fromJSON") to recalculate
            // the uptime instants (if a timer is still running).
            json.put(JK_PERSISTED_AT_REAL_TIME, nowRealTime);

            // Keep the JSON compact by only writing non-default values.
            // Parsing JSON can be slow, so smaller is better.
            if (mInspDuration != INSPECTION_DISABLED) {
                json.put(JK_INSP_DURATION, mInspDuration);
            }
            if (mIsHoldToStartEnabled) {
                json.put(JK_HOLD_TO_START_ENABLED, true);
            }

            // If inspection is not enabled, or if the inspection timer was
            // never started, record nothing. This will signal "fromJSON" that
            // all values should be restored to their defaults.
            //
            // If the inspection timer is currently running, record the elapsed
            // time (including any previous "extra" time) as the new "extra"
            // time value and set "JK_INSP_RUNNING" to "true" to indicate that
            // the inspection countdown is running and that "fromJSON" will
            // need to restore "mInspStarted" based on the recorded system real
            // time at the instant of persistence and the new system uptime at
            // the instance of restoration.
            //
            // If the inspection timer is stopped, record only the elapsed time
            // as the new "extra" time and let "fromJSON" do the rest.
            if (isInspectionEnabled() && mInspStartedAt != NEVER) {
                json.put(JK_INSP_EXTRA, getElapsedInspectionTime());
                if (isInspectionRunning()) {
                    json.put(JK_INSP_RUNNING, true);
                } // else inspection timer ran, but was stopped.
            } // else inspection timer never ran.

            if (mSolveStartedAt != NEVER) {
                json.put(JK_SOLVE_EXTRA, getElapsedSolveTime());
                if (isSolveRunning()) {
                    json.put(JK_SOLVE_RUNNING, true);
                    if (isSolvePaused()) {
                        json.put(JK_SOLVE_PAUSED, true);
                    }
                } // else solve timer ran, but was stopped.
            } // else solve timer never ran.

            if (mStage != TimerStage.STOPPED) { // The most likely stage.
                json.put(JK_TIMER_STAGE, mStage.name());
            }
            if (mPenalties != Penalties.NO_PENALTIES) {
                // Not worth implementing a "Penalties.toJSON()" method.
                json.put(JK_PENALTIES, mPenalties.encode());
            }
            if (mSolve != null) {
                // This is only really necessary if the solve is not yet saved,
                // otherwise it could be restored from the database. However,
                // it is simpler to restore it along with the JSON content, as
                // (asynchronous) re-loading of the solve would complicate the
                // restoration process and make it far less transparent.
                json.put(JK_SOLVE, mSolve.toJSON());
            }
            if (mTimerCues.size() > 0) {
                final JSONArray jsonCues = new JSONArray();

                for (TimerCue cue : mTimerCues) {
                    jsonCues.put(cue.name());
                }
                json.put(JK_TIMER_CUES, jsonCues);
            }
        } catch (JSONException ignore) {
            // This can only happen if a key name used above is null. Ignore it.
        }

        return json;
    }

    /**
     * Restores a timer state from its JSON object form. The JSON object must
     * have been created by {@link #toJSON(long, long)}. If the timer was
     * running when its state was saved, the real time that elapsed between the
     * instant it was persisted and the instant it is restored will be added to
     * the recorded elapsed time.
     *
     * @param json
     *     The JSON object containing the data for a timer state.
     * @param now
     *     The current instant in time (in milliseconds) with reference to the
     *     normal time base used by the timer. The value from
     *     {@link PuzzleClock#now()} is appropriate. {@link #mark(long)} will be
     *     called on the new {@code TimerState} passing this time instant before
     *     this method returns. The value only affects the restored state if the
     *     timer was running at the instant it was persisted by {@code toJSON}.
     * @param nowRealTime
     *     The current system real time (in milliseconds since the Unix epoch).
     *     The value from {@link PuzzleClock#nowRealTime()} is appropriate.
     *     See the description of the same parameter in {@code toJSON} for more
     *     details.
     *
     * @return
     *     A new timer state initialised from the JSON data.
     *
     * @throws JSONException
     *     If there is a problem initialising the timer state from the given
     *     JSON object. For example, if any of the required properties are not
     *     defined on the object or have invalid or unrecognised values.
     */
    // NOTE: Injecting the system real time makes testing much easier.
    static TimerState fromJSON(JSONObject json, long now, long nowRealTime)
            throws JSONException {
        final long persistedAtRT = json.getLong(JK_PERSISTED_AT_REAL_TIME);
        // NOTE: The system clock might have been re-synchronised or might not
        // have been set properly after a reboot, so never calculate the time
        // that elapsed while the state was "hibernating" in its persistent
        // form to be negative.
        final long persistedForRT = Math.max(0L, nowRealTime - persistedAtRT);

        final TimerState newTimerState = new TimerState(
            json.optLong(JK_INSP_DURATION, INSPECTION_DISABLED),
            json.optBoolean(JK_HOLD_TO_START_ENABLED, false));

        if (json.has(JK_INSP_EXTRA)) {
            // The inspection time that had elapsed up to the instant that the
            // timer state was persisted to JSON.
            newTimerState.mInspExtraTime = json.optLong(JK_INSP_EXTRA, 0L);

            if (json.optBoolean(JK_INSP_RUNNING, false)) {
                // Inspection was running when the state was persisted, so it
                // must be restored into a running state. The real (wall) time
                // that elapsed between the instant the state was persisted and
                // "now" (real time) is added to the "extra" time. The new start
                // instant is set to "now" (uptime). Further uptime that elapses
                // before the timer is stopped will be calculated with respect
                // to that uptime instant, i.e., as the difference between the
                // (current) start and (future) stop or "mark" instants.
                //
                // NOTE: It is no harm to limit the total elapsed time to the
                // maximum that could be recorded: the inspection time plus the
                // overrun time (as if "stopInspection(-1)" were called).
                newTimerState.mInspExtraTime = Math.min(
                    newTimerState.mInspExtraTime + persistedForRT,
                    newTimerState.mInspDuration + INSPECTION_OVERRUN_DURATION);
                newTimerState.mInspStartedAt = now;
                newTimerState.mInspStoppedAt = NEVER;
            } else {
                // Inspection was already stopped at the instant that the state
                // was persisted. Just use zero for the start and stop instants
                // (as zero does not match "NEVER") and let "mInspExtraTime"
                // record the total elapsed time.
                newTimerState.mInspStartedAt = 0L;
                newTimerState.mInspStoppedAt = 0L;
            }
        } else {
            // Inspection is disabled or was never started: use defaults.
            newTimerState.mInspStartedAt = NEVER;
            newTimerState.mInspStoppedAt = NEVER;
            newTimerState.mInspExtraTime = 0L;
        }

        // The same (more-or-less) approach is used for the elapsed solve time.
        // The main difference is the handling of a "paused" timer: it does not
        // accumulate real time that elapsed while in its persistent state.
        if (json.has(JK_SOLVE_EXTRA)) {
            newTimerState.mSolveExtraTime = json.optLong(JK_SOLVE_EXTRA, 0L);

            if (json.optBoolean(JK_SOLVE_RUNNING, false)) {
                if (json.optBoolean(JK_SOLVE_PAUSED, false)) {
                    newTimerState.mSolveStartedAt = PAUSED;
                } else {
                    newTimerState.mSolveExtraTime =
                        newTimerState.mSolveExtraTime + persistedForRT;
                    newTimerState.mSolveStartedAt = now;
                }
                newTimerState.mSolveStoppedAt = NEVER;
            } else {
                newTimerState.mSolveStartedAt = 0L;
                newTimerState.mSolveStoppedAt = 0L;
            }
        } else {
            newTimerState.mSolveStartedAt = NEVER;
            newTimerState.mSolveStoppedAt = NEVER;
            newTimerState.mSolveExtraTime = 0L;
        }

        // It is possible for the persistent state to be created by one version
        // of the app and then restored by another. Rather than trying to
        // enforce that the enum values must never be renamed, or that the
        // penalties encoding or JSON format never be changed, it is simpler to
        // just treat these cases as invalid JSON data and let the application
        // recover by deleting the old persistent state and starting with a new
        // one. The only time this would cause data loss is if the app were
        // upgraded while a timer was running, as that is the only condition
        // where the solve attempt has not already been saved to the database.
        // That is unlikely and so is a change to the JSON format, enum names,
        // or penalties encoding, so the combined likelihood is very remote.
        //
        // If compatibility across app versions becomes a concern, the necessary
        // handling can always be added here in the new version of the app that
        // introduces the issue. At that time, a JSON key/value pair might also
        // be added that defines the "schema version" of the JSON to support
        // such "upgrades". For now, in the absence of such a version entry,
        // the version can be assumed to be version "1".
        try {
            newTimerState.mStage = json.has(JK_TIMER_STAGE)
                ? TimerStage.valueOf(json.getString(JK_TIMER_STAGE))
                : TimerStage.STOPPED;

            newTimerState.mPenalties = json.has(JK_PENALTIES)
                ? Penalties.decode(json.getInt(JK_PENALTIES))
                : Penalties.NO_PENALTIES;

            newTimerState.mTimerCues.clear();
            if (json.has(JK_TIMER_CUES)) {
                final JSONArray jsonCues = json.getJSONArray(JK_TIMER_CUES);
                final int len = jsonCues.length();

                for (int i = 0; i < len; i++) {
                    newTimerState.mTimerCues.add(
                        TimerCue.valueOf(jsonCues.getString(i)));
                }
            }
        } catch (IllegalArgumentException e) {
            // It is simplest to wrap the unchecked exception in the checked
            // "JSONException", as the caller can treat all problems similarly.
            //
            // IMPORTANT: Android's JSON has no "JSONException(Throwable)"
            // constructor while in other JSON libraries that do, "getCause()"
            // may be overridden and ignore what "initCause(Throwable)" sets, so
            // this code should work in Android, but may act in an unexpected
            // manner if a non-Android JSON library is used for simple,
            // non-Android-instrumented JUnit testing.
            throw (JSONException)
                new JSONException("TimerStage or Penalties is invalid.")
                    .initCause(e);
        }

        newTimerState.mSolve = json.has(JK_SOLVE)
            ? Solve.fromJSON(json.getJSONObject(JK_SOLVE))
            : null;

        // In case the caller forgets... Redundant if timer is not running.
        newTimerState.mark(now);

        return newTimerState;
    }
}
