package com.aricneto.twistytimer.layout;

/*
 * The Android chronometer widget revised so as to count milliseconds
 */

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.TimeUtils;

/**
 * A chronometer for twisty puzzles of all types. This supports timing in milliseconds, display of
 * the elapsed time to a high resolution (hundredths of a second) or low resolution (whole seconds),
 * addition of standard "+2" and "DNF" penalties, and "hold-for-start" behaviour that can restore a
 * previous time if the hold is cancelled.
 */
public class ChronometerMilli extends TextView {
    @SuppressWarnings("unused")
    private static final String TAG = "Chronometer";

    /**
     * The number of milliseconds between updates to the displayed of a low-resolution elapsed time.
     * Low resolution times are typically displayed to whole seconds.
     */
    private static final long TICK_TIME_LR = 100L; // 0.1 seconds to avoid jerkiness.

    /**
     * The number of milliseconds between updates to the displayed of a high-resolution elapsed
     * time. High resolution times are typically displayed to 100ths of a second.
     */
    private static final long TICK_TIME_HR = 10L; // 0.01 seconds (100 fps). Probably overkill.

    private static final int TICK_WHAT = 2;

    private String hideTimeText;
    private boolean hideTimeEnabled;

    /**
     * The time (system elapsed real time in milliseconds) at which this chronometer was started.
     * Will be zero if the chronometer has not been started or has been reset.
     */
    private long mStartedAt;

    /**
     * The time (system elapsed real time in milliseconds) at which this chronometer was stopped.
     * Will be zero if the chronometer has not been stopped or has been reset.
     */
    private long mStoppedAt;

    /**
     * The penalty applied to this solve time.
     */
    @NonNull
    private Penalty mPenalty = Penalty.NONE;

    /**
     * Indicates if the chronometer has been started and is now running and measuring elapsed time.
     */
    private boolean mIsRunning;

    /**
     * Indicates if the chronometer's recorded time has been annulled. This flag is cleared when
     * the chronometer is next reset or started.
     */
    private boolean mIsAnnulled;

    /**
     * Indicates if the chronometer view is currently visible on the screen or not. This is not
     * the same as {@code View.isVisible() == View.VISIBLE}, as a view can be marked {@code VISIBLE}
     * whether it is on the screen or not.
     */
    private boolean mIsVisibleOnScreen;

    /**
     * Indicates if the chronometer is running (see {@link #mIsRunning}), is visible on the screen
     * (see {@link #mIsVisibleOnScreen}) and the handler used to update the time value on the screen
     * should continue to do so. If this is reset, the time updates on the screen will stop (though
     * the chronometer could still be running off screen).
     */
    private boolean mIsUpdatingTimeOnScreen;

    /**
     * Indicates if this chronometer is holding in readiness to be started once the minimum hold
     * period has elapsed.
     */
    private boolean mIsHoldingForStart;

    /**
     * The text that was being displayed by this chronometer before entering the hold-for-start
     * state. If the state is cancelled, this text will be restored.
     */
    private CharSequence mTextSavedBeforeHolding;

    /**
     * Indicates if seconds will be shown to a high resolution while the timer is started. If
     * enabled, fractions (hundredths) of a second will be displayed while the chronometer is
     * running. See {@link #updateText()} for details on how and when this preference is applied.
     */
    private boolean mShowHiRes;

    /**
     * The normal text color. This is saved before the text is highlighted and restored when
     * highlighting is turned off.
     */
    private int mNormalColor;

    public ChronometerMilli(Context context) {
        this(context, null, 0);
    }

    public ChronometerMilli(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChronometerMilli(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        // Save the current (normal) text color, as it may be overwritten with the highlight color.
        // The highlight color is available from "getHighlightColor", as that is never changed.
        // The implementation assumes that different colors for different states will not be used.
        mNormalColor = getCurrentTextColor();

        mShowHiRes = Prefs.getBoolean(
                R.string.pk_show_hi_res_timer, R.bool.default_show_hi_res_timer);
        hideTimeEnabled = Prefs.getBoolean(
                R.string.pk_hide_time_while_running, R.bool.default_hide_time_while_running);
        hideTimeText = getContext().getString(R.string.hideTimeText);

        // The initial state will cause "0.00" to be displayed.
        updateText();
    }

    /**
     * Sets the highlighted state of the time value text displayed by this chronometer. This can be
     * used with the start cue and hold-for-start behaviour.
     *
     * @param isHighlighted
     *     {@code true} to highlight the text in a different color; or {@code false} to restore the
     *     normal text color.
     */
    public void setHighlighted(boolean isHighlighted) {
        setTextColor(isHighlighted ? getHighlightColor() : mNormalColor);
    }

    /**
     * Indicates if this chronometer has been started and is now running and measuring the elapsed
     * time since being started. If started and running, it can be stopped, but it cannot be
     * started again or reset until it has been stopped. This should be tested before calling
     * {@link #start()}, {@link #stop()} or {@link #reset()}, if the state is not already known.
     *
     * @return
     *     {@code true} if this chronometer has been started and is running; or {@code false} if it
     *     has not been started.
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * Indicates if the chronometer was annulled with a call to {@link #annul()}. This will be
     * cleared if either {@link #start()} or {@link #reset()} is called.
     *
     * @return
     *     {@code true} if the chronometer was annulled; or {@code false} if it was not.
     */
    public boolean isAnnulled() {
        return mIsAnnulled;
    }

    /**
     * <p>
     * Gets the elapsed time (in milliseconds) measured by this chronometer including any additional
     * time penalties. This method may be called even if this chronometer is currently started. Any
     * penalty times set by {@link #setPenalty(Penalty)} will be included in the reported elapsed
     * time.
     * </p>
     * <p>
     * If a "DNF" penalty was applied, the elapsed time will be reported <i>as recorded</i>. For
     * example, if a DNF is applied after the timer is stopped, then the time elapsed when the
     * timer was stopped is still reported by this method. However, if a DNF is applied before the
     * timer is started, such as when the inspection period times out, then the reported time will
     * be zero.
     * </p>
     * <p>
     * Recording the elapsed time for a DNF is useful if ever there is a need to calculate the
     * cumulative time. WCA Regulations A1a2: "Cumulative time limits may be enforced (e.g. 3
     * results with a cumulative time limit of 20 minutes). The time elapsed in a DNF result counts
     * towards the cumulative time limit." Also WCA Guidelines A1a2+: "In case of a cumulative
     * time limit, the judge records the original recorded time for a DNF on the score sheet in
     * parentheses, e.g. "DNF (1:02.27)". In this app, a simple enhancement would be to report the
     * cumulative elapsed solving time for the current session, or for all time. Recording the
     * elapsed time for all DNF finishes is necessary to make such reports meaningful.
     * </p>
     *
     * @return
     *     The elapsed time measured by this chronometer including any time penalties. This is the
     *     exact elapsed time with millisecond precision; it is not rounded.
     */
    public long getElapsedTime() {
        switch (mPenalty) {
            default:
            case NONE:
            case DNF:
                return getElapsedTimeExcludingPenalties();

            case PLUS_TWO:
                return getElapsedTimeExcludingPenalties() + Penalties.PLUS_TWO_DURATION_MS;
        }
    }

    /**
     * Gets the elapsed time (in milliseconds) measured by this chronometer excluding any additional
     * time penalties. This method may be called even if this chronometer is currently started. Any
     * penalty time set by {@link #setPenalty(Penalty)} will <i>not</i> be included in the reported
     * elapsed time.
     *
     * @return The elapsed time measured by this chronometer excluding penalties.
     */
    private long getElapsedTimeExcludingPenalties() {
        // If the chronometer is started, then the elapsed time is the difference between "now" and
        // "mStartedAt". If the chronometer has never been started, has been stopped, or has been
        // reset, then the difference between "mStoppedAt" and "mStartedAt" is used. This ensures
        // that the initial state or reset state will display "0.00".
        return (mIsRunning ? SystemClock.elapsedRealtime() : mStoppedAt) - mStartedAt;
    }

    /**
     * Holds the chronometer is a state ready to be started from zero. This will display a zero
     * start time, but, if {@link #cancelHoldForStart()} is called, the previously displayed value
     * be restored. If {@link #start()} is called subsequently, the recorded elapsed time and any
     * penalties will <i>not</i> be reset automatically, so be sure to call {@link #reset()} first,
     * if appropriate. Both of those methods also exit this state, so {@code cancelHoldForStart()}
     * will no longer have any effect. This method does not affect the annulled state reported by
     * {@link #isAnnulled()}.
     *
     * @throws IllegalStateException
     *     If the chronometer is already started.
     */
    public void holdForStart() {
        if (mIsRunning) {
            // There is no use case where the chronometer will be held *before* starting when it is
            // *already* started, so throw an exception to highlight a likely bug in the caller.
            throw new IllegalStateException("Cannot hold chronometer if already started.");
        }

        // "TimerFragment" directly sets the text on this chronometer view when doing an inspection
        // count-down, inspection penalty, or displaying "DNF". Those functions should really be
        // performed using a separate text view, or should be properly integrated into this class.
        // In the meantime, before holding for the start, save the displayed text (whatever it is)
        // and restore it if "cancelHoldForStart" is called. Do not call "updateText" to restore the
        // previous elapsed time, as "TimerFragment" may have hijacked this view to show something
        // else.
        //
        // Also, this "where-did-that-text-come-from?" condition can also be the result of the
        // default state-saving of this view, as full state saving and restoration of the elapsed
        // time, penalties, etc. is not yet implemented.
        mIsHoldingForStart = true;
        mTextSavedBeforeHolding = getText();
        updateText(); // Will display "0.00" because "mIsHoldingForStart" is set.
    }

    /**
     * Cancels the hold-for-start state and restores the value previously displayed by this
     * chronometer. If the chronometer is not in the hold-for-start state, this method will have
     * no effect. This does <i>not</i> raise {@link #isAnnulled()}.
     */
    public void cancelHoldForStart() {
        if (mIsHoldingForStart) {
            mIsHoldingForStart = false;
            if (mTextSavedBeforeHolding != null) {
                // Do not call "updateText" to restore the saved value, as the saved text may not
                // have been set by this chronometer.
                setText(mTextSavedBeforeHolding);
            }
        }
    }

    /**
     * Ends the hold-for-start state <i>without</i> restoring the value previously displayed by
     * this chronometer. If the chronometer is not in the hold-for-start state, this method will
     * have no effect. This method is called automatically if the chronometer is started or reset.
     */
    private void endHoldForStart() {
        if (mIsHoldingForStart) {
            mIsHoldingForStart = false;
            mTextSavedBeforeHolding = null;
        }
    }

    /**
     * Starts the chronometer, resuming the recording of the elapsed time from where it left off
     * when it was last stopped. To restart from zero and clear penalties, call {@link #reset()}
     * first. If this chronometer is already started, calling this method will have no effect.
     * This will also exit the "hold-for-start" state if it is active; the displayed text value
     * saved when that state was entered will not be restored.
     */
    public void start() {
        if (mIsRunning) {
            return;
        }

        // For some puzzle types, the elapsed time could be long (many minutes, or even hours), so
        // the need to support a "pause" feature during informal timing sessions may be useful.
        // Here, calculate the new "mStartedAt" value and then offset it into the past by the
        // amount of elapsed time already recorded, which will allow sequences of state changes
        // such as "reset-start-stop-start-stop-start-stop" to accumulate time as necessary. If
        // already started, "stop-start" is effectively "pause-resume". Do not include any penalty
        // time in the elapsed time offset, it will remain separate.
        mStartedAt = SystemClock.elapsedRealtime() - getElapsedTimeExcludingPenalties();
        mStoppedAt = 0L;
        mIsRunning = true;
        mIsAnnulled = false;

        // If we were holding for a start, stop doing that now and discard any saved text.
        endHoldForStart();

        updateText();
        updateRunning();
    }

    /**
     * Stops the chronometer. The elapsed time will no longer be incremented until the chronometer
     * is started again. If this chronometer is already stopped, calling this method will have no
     * effect.
     *
     * @throws IllegalStateException
     *     If the chronometer is already started.
     */
    public void stop() {
        if (!mIsRunning) {
            return;
        }

        mIsRunning = false;
        mStoppedAt = SystemClock.elapsedRealtime();

        // Update the text to show the exact elapsed time at this precise moment.
        updateText();

        // Stop updating the display if necessary, as the chronometer is no longer running.
        updateRunning();
    }

    /**
     * Resets the time to zero. The chronometer must be stopped before it can be reset. This will
     * also exit the "hold-for-start" state if it is active; the displayed text value saved when
     * that state was entered will not be restored.
     *
     * @throws IllegalStateException
     *     If the chronometer is currently started.
     */
    public void reset() throws IllegalStateException {
        if (mIsRunning) {
            // There is no use case where the chronometer will be reset without first being
            // stopped, so throw an exception to highlight a likely bug in the caller.
            throw new IllegalStateException("Chronometer cannot be reset if it has been started.");
        }

        mStartedAt = 0L;
        mStoppedAt = 0L;
        mPenalty = Penalty.NONE;
        mIsAnnulled = false;

        // If we were holding for a start, stop doing that now and discard any saved text.
        endHoldForStart();

        // No need to call "updateRunning()", as we have not changed the "running" state.
        updateText();
    }

    /**
     * <p>
     * Annuls the timer's recorded time and resets the time to zero. If the chronometer is running,
     * it will first be stopped. This will also exit the "hold-for-start" state if it is active;
     * the displayed text value saved when that state was entered will not be restored. Once
     * annulled {@link #isAnnulled()} will return {@code true} until either {@link #start()} or
     * {@link #reset()} is called.
     * </p>
     * <p>
     * The annul operation essentially stops the timer if it is running and then resets it. The
     * only difference is that {@link #isAnnulled()} is raised. This allows a recorded time (which
     * has been reset to zero by this operation) to be marked as one that should be disregarded,
     * not saved.
     * </p>
     */
    public void annul() throws IllegalStateException {
        if (mIsRunning) {
            stop();
        }

        reset(); // Clears the "mIsAnnulled" flag, so do this before raising the flag again.
        mIsAnnulled = true;
    }

    /**
     * Sets a penalty to be applied to the currently recorded elapsed time. If a 2-second penalty
     * is applied, a "+" is appended to the display of the elapsed time to indicate that a penalty
     * time has been added and {@link #getElapsedTime()} will include the extra penalty. If a
     * did-not-finish penalty is set, "DNF" is displayed. Any previously set penalty is replaced
     * by the new penalty. {@code Penalty.NONE} can also be set to remove a 2-second or DNF penalty
     * and restore the elapsed time.
     *
     * @param penalty The the penalty to be applied.
     */
    public void setPenalty(@NonNull Penalty penalty) {
        mPenalty = penalty;

        // Show the new time with the included penalty and the "+" penalty indicator, if needed.
        updateText();
    }

    /**
     * <p>
     * Updates the text that displays the current elapsed time. The formatting of the time depends
     * on the state of the chronometer and the preference for showing fractional seconds values.
     * When the chronometer is stopped, fractional seconds values are shown. When the chronometer is
     * started (running), fractional seconds are only shown if the respective preference is enabled.
     * Fractional seconds are never shown for elapsed times of one hour or longer, regardless of the
     * state of the chronometer.
     * </p>
     * <p>
     * A preference to hide the elapsed time while the chronometer is running is also supported. If
     * the preference is enabled and the chronometer is started, then the elapsed time will not be
     * shown; a fixed string will be shown in its place.
     * </p>
     * <p>
     * If a "+2" penalty has been applied and the chronometer is stopped, "+" will be appended to
     * the display of the elapsed time. If a "DNF" penalty has been applied, "DNF" will be displayed
     * instead of the elapsed time.
     * </p>
     *
     * @return
     *     {@code true} if the displayed text presented a high-resolution, fractional value for
     *     the number of seconds, or {@code false} if only whole seconds were shown. This may be
     *     used to inform the necessary update frequency.
     */
    private synchronized boolean updateText() {
        final CharSequence timeText; // May be "String" or "Spannable".
        final boolean isHiRes;

        if (mIsRunning && hideTimeEnabled) {
            timeText = hideTimeText;
            isHiRes = false; // No need for rapid updates if not showing the time.
        } else {
            // The displayed time will include any time penalty. If holding before starting, then
            // assume that the elapsed time will be started at zero, display that zero time and
            // ignore any penalty applied to the time. If running, do not indicate the "+2" penalty
            // has been applied (only because that is the way it has always been done).
            final long time = mIsHoldingForStart ?           0L : getElapsedTime();
            final Penalty penalty // May still end up as "DNF".
                    = mIsHoldingForStart || (mIsRunning && mPenalty == Penalty.PLUS_TWO)
                        ? Penalty.NONE
                        : mPenalty;

            // Time is always hi-res when the timer is stopped. Otherwise, if running, respect the
            // preference for not showing 100ths of a second, if it is enabled.
            isHiRes = !mIsRunning || TimeUtils.isChronoShowingHiRes(time, mShowHiRes, penalty);
            timeText = TimeUtils.formatChronoTime(time, isHiRes, penalty); // Use hi-res override.
        }

        setText(timeText);

        return isHiRes;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsVisibleOnScreen = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mIsVisibleOnScreen = visibility == VISIBLE;
        updateRunning();
    }

    /**
     * Updates the running state of this chronometer. The chronometer is "running" if it is in the
     * started state and is visible. In the "running" state, the display of the current elapsed time
     * will be updated regularly.
     */
    private void updateRunning() {
        final boolean isUpdatingTimeOnScreen = mIsVisibleOnScreen && mIsRunning;

        if (isUpdatingTimeOnScreen != mIsUpdatingTimeOnScreen) {
            // State has changed:
            //
            //   If the chronometer was not running but has now started running, then kick off a
            //   chain of messages that will update the display of the elapsed time at regular
            //   intervals. One message is queued here and then a new message is queued as each
            //   message is handled by "TimeUpdateHandler.handleMessage".
            //
            //   If the chronometer was running but has now stopped running, clear
            //   "mIsUpdatingTimeOnScreen" (which causes "TimeUpdateHandler.handleMessage" to break
            //   the chain of update messages) and then clear any other unhandled "tick" messages
            //   from the queue.
            //
            // If the state has not changed, then things can be left alone: either the message
            // chain is active and perpetuating itself, or it is inactive.
            mIsUpdatingTimeOnScreen = isUpdatingTimeOnScreen;

            if (isUpdatingTimeOnScreen) {
                // Use a very short "tick" time (1 ms) before the very first update.
                mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT, this), 1L);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
        }
    }

    private Handler mHandler = new TimeUpdateHandler();

    // "static" handler class to prevent memory leaks.
    private static final class TimeUpdateHandler extends Handler {
        public void handleMessage(Message m) {
            if (m.obj != null) {
                final ChronometerMilli chronometer = (ChronometerMilli) m.obj;

                // Update the time display before checking if the chronometer is still "running".
                // This ensures that the time display is up-to-date with the exact elapsed time.
                //
                // Adapt the interval between updates to the current resolution of the display
                // of the seconds value, i.e., update faster if showing 100ths of a second.
                final long tickTime = chronometer.updateText() ? TICK_TIME_HR : TICK_TIME_LR;

                if (chronometer.mIsUpdatingTimeOnScreen) {
                    // Only chain a new message for the next update if still running visibly.
                    sendMessageDelayed(Message.obtain(this, TICK_WHAT, chronometer), tickTime);
                }
            }
        }
    }
}
