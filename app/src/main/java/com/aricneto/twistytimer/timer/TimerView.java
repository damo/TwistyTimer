package com.aricneto.twistytimer.timer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.utils.TimeUtils;
import com.aricneto.twistytimer.utils.WCAMath;

/**
 * <p>
 * A view that displays the inspection countdown and solve timer. The displayed
 * values are managed by a {@link PuzzleTimer}. An instance of this view can be
 * added as a listener for the timer's events and refresh notifications.
 * </p>
 * <p>
 * This view does not support any padding attributes; padding is simply ignored.
 * </p>
 * <p>
 * This view does not save its instance state. It is expected that the parent
 * fragment or activity will initialise this view from a {@code PuzzleTimer}
 * whose instance state can been saved. See the description of that class for
 * more details.
 * </p>
 *
 * @author damo
 */
public class TimerView extends View
             implements OnTimerEventListener, OnTimerRefreshListener {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * Flag to enable more debug logging for this class. When {@link #DEBUG_ME}
     * is enabled and this flag is also enabled, more details are logged when
     * the time is refreshed and redrawn.
     */
    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG_ME_MORE = DEBUG_ME && false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerView.class.getSimpleName();

    /**
     * The default text color. This is used for both the normal and the "cued"
     * state. Use the {@code normalTextColor} and {@code cueTextColor} layout
     * attributes to override this default.
     */
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;

    /**
     * The default maximum height of the time display in pixels. The height of
     * the time display does not include the height of the "headline" that may
     * be displayed above it.
     */
    private static final int DEFAULT_MAX_TIME_HEIGHT = 50;

    /**
     * The minimum height of the time text that will be drawn when animating
     * the scaling transition of the time text values. If the height of the
     * text is less than this value when both values have a non-zero scale, the
     * text will not be drawn, as it would be too small to be readable. If the
     * time text has a 1.0 scale (i.e., only one time value will be drawn), this
     * limit will be ignored and the time text will be drawn regardless of how
     * small its height. The value is expressed in pixels.
     */
    private static final float MIN_SCALED_TEXT_HEIGHT = 4f;

    /**
     * The scale factor for the size of the text used to show the fractions of a
     * second. The text size should be set smaller than the normal text size, as
     * it looks prettier.
     */
    private static final float TEXT_SIZE_RATIO_100THS = 0.666f;

    /**
     * The proportion of the total height of the view that defines the height of
     * the gap between the inspection time and solve time when both times are
     * shown.
     */
    private static final float LINE_GAP_FACTOR = 0.06f;

    /**
     * The proportion of the total height of the view that defines the height of
     * the area used to display the "headline" text.
     */
    private static final float HEADLINE_FACTOR = 0.30f;

    /**
     * The scale value when showing only the solve time.
     */
    private static final float SCALE_SHOW_ONLY_SOLVE_TIME = 1.0f;

    /**
     * The scale value when showing only the inspection time.
     */
    private static final float SCALE_SHOW_ONLY_INSPECTION_TIME = 0.0f;

    /**
     * The scale value when presenting the "0.00" solve time while the
     * inspection countdown is still running.
     */
    private static final float SCALE_SHOW_BOTH_TIMES = 0.666f;

    /**
     * The duration (in milliseconds) of the animation used when rescaling the
     * time values. This is the duration for a full traversal from 0% to 100%
     * scale (or <i>vice versa</i>); if the actual scale change is less, the
     * duration will be adjusted <i>pro rata</i>.
     */
    // NOTE: Preferably a shorter duration than the hold-for-start duration,
    // so the animation ends well before the timer is ready to start.
    private static final long SCALE_ANIM_DURATION = 250;

    /**
     * The sample text used to show the time when in edit mode.
     */
    private static final StringBuilder SAMPLE_TIME_TEXT
        = new StringBuilder("59:59.99");

    /**
     * The static text to show instead of the elapsed solve time while the timer
     * is running. If {@code null} (the default), the solve time is shown.
     */
    private String mHideTimeText;

    /**
     * Indicates if the "start cue" behaviour is enabled. Enabled by default.
     */
    private boolean mShowStartCue = true;

    /**
     * Indicates if the elapsed solve time will be shown to a high resolution
     * (1/100ths of a second) while the timer is running. Enabled by default.
     */
    private boolean mShowHiRes = true;

    /**
     * The normal text color.
     */
    private final int mNormalColor;

    /**
     * The "cued" text color
     */
    private final int mCueColor;

    /**
     * The maximum height of the time value that will be displayed under the
     * "headline".
     */
    private final int mMaxTimeHeight;

    /**
     * The paint resource used when drawing the inspection time text.
     */
    private final Paint mInspectionTimePaint = new Paint();

    /**
     * The paint resource used when drawing the inspection time text.
     */
    private final Paint mSolveTimePaint = new Paint();

    /**
     * The paint resource used when drawing the headline text.
     */
    private final Paint mHeadlinePaint = new Paint();

    /**
     * The animator used when changing the scale of the text values.
     */
    // Created on demand, not part of "SavedState.
    private ValueAnimator mAnimator;

    /**
     * The offset from the top of the region displaying the time to the baseline
     * of the time text. This is defined for the time text when only one time
     * value is shown at its full scale.
     */
    // Set by "onMeasure", not part of "SavedState.
    private float mTimeTextBaseline;

    /**
     * The size of the time text required to fit the text to the height of the
     * region that displays the time. This is defined for the time text when
     * only one time value is shown at its full scale.
     */
    // Set by "onMeasure", not part of "SavedState.
    private float mTimeTextSize;

    /**
     * The height of the region in which the timer content will be drawn. If the
     * layout imposes a larger height, the drawn region will be centred
     * vertically within its bounds.
     */
    // Set by "onMeasure", not part of "SavedState.
    private int mDrawnHeight;

    /**
     * The current scale, during any animated transition, of the solve time text
     * relative to the height of the view.
     */
    private float mSolveTimeScale = 1.0f;

    /**
     * The text value for the currently remaining inspection time.
     */
    private StringBuilder mInspectionTimeText = new StringBuilder(20);

    /**
     * The text value for the currently elapsed solve time.
     */
    private StringBuilder mSolveTimeText = new StringBuilder(20);

    /**
     * The "headline" text for the current timer state. This will show
     * "INSPECTION" during the inspection period and detail any penalties
     * applied to the base time at the end of the solve.
     */
    private String mHeadlineText;

    /**
     * The current scale factor for the timer display.
     */
    private float mDisplayScale = 1f;

    public TimerView(Context context) {
        this(context, null, 0);
    }

    public TimerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable H/W acceleration for this view. It seems to cause all
        // sorts of drawing problems in an (API 23) emulator.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        final TypedArray ta = context.obtainStyledAttributes(
            attrs, R.styleable.TimerView, defStyle, 0);

        mNormalColor = ta.getColor(
            R.styleable.TimerView_normalTextColor, DEFAULT_TEXT_COLOR);
        mCueColor = ta .getColor(
            R.styleable.TimerView_cueTextColor, DEFAULT_TEXT_COLOR);
        mMaxTimeHeight = ta.getDimensionPixelSize( // Rounds to >= 1.
            R.styleable.TimerView_maxTimeHeight, DEFAULT_MAX_TIME_HEIGHT);

        ta.recycle();

        // "sans-serif-light" maps to "Roboto Light". "Roboto Thin" might be
        // nicer, but it is not available until API 17 (and API 16 is the
        // minimum supported at this time). It would probably be easy to a)
        // ship the font, or b) use "Roboto Thin" if it is available and fall
        // back to "Roboto Light", but that can be done some other time.
        final Typeface typeface
            = Typeface.create("sans-serif-light", Typeface.NORMAL);

        mInspectionTimePaint.setColor(mNormalColor);
        mInspectionTimePaint.setAntiAlias(true);
        mInspectionTimePaint.setTypeface(typeface);

        mSolveTimePaint.setColor(mNormalColor);
        mSolveTimePaint.setAntiAlias(true);
        mSolveTimePaint.setTypeface(typeface);

        mHeadlinePaint.setColor(mNormalColor);
        mHeadlinePaint.setAntiAlias(true);
        mHeadlinePaint.setTextAlign(Paint.Align.CENTER);

        if (DEBUG_ME) {
            Log.d(TAG, "Timer background highlighted while debugging.");
            setBackgroundColor(Color.DKGRAY);
        }
    }

    /**
     * Sets the scale of the timer display in proportion to its default scale.
     *
     * @param newDisplayScale
     *     The scale factor for the display of the timer. 1.0 is the default
     *     scale. Smaller values will cause the timer to be drawn smaller than
     *     the default; larger values will cause the timer to be drawin larger
     *     than the default.
     *
     * @throws IllegalArgumentException
     *     If the scale is less than 0.1 or greater than 10.0.
     */
    public void setDisplayScale(
            @FloatRange(from = 0.1, to = 10.0) float newDisplayScale)
            throws IllegalArgumentException {

        if (newDisplayScale < 0.1f || newDisplayScale > 10f) {
            throw new IllegalArgumentException(
                "Display scale is out of range: " + newDisplayScale);
        }

        // Invalidate the layout, so "onMeasure" will update the dimensions
        // appropriately.
        mDisplayScale = newDisplayScale;
        requestLayout();
    }

    /**
     * Enables (or disables) the display of a high resolution elapsed solve time
     * while the timer is running. This setting does not affect the display of
     * the remaining inspection time and does not affect the display of the
     * elapsed solve time if the timer is stopped. If stopped, the timer always
     * displays the highest resolution solve time in accordance with WCA
     * Regulations for rounding/truncation. High resolution times are shown by
     * default.
     *
     * @param isEnabled
     *     {@code true} to show a high resolution time (to 1/100ths of a second
     *     where appropriate); or {@code false} to show a low resolution time
     *     (to whole seconds), though only while the timer is running.
     */
    public void setHiResTimerEnabled(boolean isEnabled) {
        mShowHiRes = isEnabled;
    }

    /**
     * Enables (or disables) the display of a "start cue" when the timer is
     * ready to start. If enabled, when the user touches down on the display
     * the time value will change color to indicate that the timer will start
     * as soon as that touch is lifted up. If hold-to-start behaviour is
     * enabled, that color change will be delayed until the minimum hold
     * duration has elapsed. The start cue is enabled by default and affects
     * both the inspection time and the solve time. It is recommended that it
     * be enabled if hold-to-start is behaviour is enabled, as that will ensure
     * that the hold-to-start delay does not cause confusion.
     *
     * @param isEnabled
     *     {@code true} to enable the start cue; or {@code false} to disable it.
     */
    public void setStartCueEnabled(boolean isEnabled) {
        mShowStartCue = isEnabled;
    }

    /**
     * Sets the static text to display to hide (or mask) the elapsed solve time
     * while the timer is running. When the timer is stopped, reset, or in the
     * hold-to-start or ready-to-start states, the elapsed solve time will be
     * displayed as normal. This setting does not affect the display of the
     * remaining inspection time. The default value is {@code null}.
     *
     * @param hideTimeText
     *     The static text to display in place of the elapsed solve time while
     *     the timer is running. If {@code null}, the solve time will not be
     *     hidden while the timer is running.
     */
    public void setHideTimeText(String hideTimeText) {
        mHideTimeText = hideTimeText;
    }

    /**
     * Highlights the inspection time value to provide the user with a cue
     * suggesting that the timer is ready to start. This will have no effect if
     * the cue behaviour is disabled, or if the cue color already matches the
     * required state. If the color is changed, this view will be invalidated.
     *
     * @param isCued
     *     {@code true} to highlight the text in a different color; or {@code
     *     false} to restore the normal text color.
     */
    public void cueInspectionTime(boolean isCued) {
        if (mShowStartCue) {
            final int newColor = isCued ? mCueColor : mNormalColor;

            if (newColor != mInspectionTimePaint.getColor()) {
                mInspectionTimePaint.setColor(newColor);
                invalidate();
            }
        }
    }

    /**
     * Highlights the solve time value to provide the user with a cue suggesting
     * that the timer is ready to start. This will have no effect if the start
     * cue behaviour is disabled, or if the cue color already matches the
     * required state. If the color is changed, this view will be invalidated.
     *
     * @param isCued
     *     {@code true} to highlight the text in a different color; or {@code
     *     false} to restore the normal text color.
     */
    public void cueSolveTime(boolean isCued) {
        if (mShowStartCue) {
            final int newColor = isCued ? mCueColor : mNormalColor;

            if (newColor != mSolveTimePaint.getColor()) {
                mSolveTimePaint.setColor(newColor);
                invalidate();
            }
        }
    }

    /**
     * Sets the scale of the solve time text. The solve time text is scaled
     * relative to the height of this widget with 1.0 representing "full
     * height". Setting the scale of the solve time text also sets the scale of
     * the inspection time text, with the inspection time text being scaled to
     * {@code 1.0 - scale}. Therefore, if one field is scaled to 1.0, the other
     * will be scaled to 0.0 and become invisible. Changes to the solve time
     * scale may be animated by calling {@link #scaleSolveTimeTo(float)}.
     *
     * @param scale
     *     The scale to apply to the solve time text in the range 0.0 to 1.0.
     */
    private void setSolveTimeScale(
            @FloatRange(from = 0.0, to = 1.0) float scale) {
        if (mSolveTimeScale != scale) {
            mSolveTimeScale = scale;
            invalidate();
        }
    }

    /**
     * Animates a change to the scale of the solve time text. See {@link
     * #setSolveTimeScale(float)} for details.
     *
     * @param scale
     *     The scale to apply to the solve time text in the range 0.0 to 1.0.
     */
    private void scaleSolveTimeTo(
           @FloatRange(from = 0.0, to = 1.0) float scale) {
        if (DEBUG_ME) Log.d(TAG,
            "scaleSolveTimeTo(" + scale + "): from=" + mSolveTimeScale);

        if (mAnimator != null) {
            mAnimator.cancel();
        } else {
            // This "ObjectAnimator" is created once and then reused. Using a
            // "Property" is faster than passing "solveTimeScale" and having
            // the animator use introspection.
            mAnimator = ObjectAnimator.ofFloat(this,
                new Property<TimerView, Float>(
                    Float.class, "solveTimeScale") {
                    @Override
                    public Float get(TimerView timerView) {
                        return timerView.mSolveTimeScale;
                    }

                    @Override
                    public void set(TimerView timerView, Float value) {
                        timerView.setSolveTimeScale(value);
                    }
                },
                // This "random" scale value set on creation will be
                // overwritten below for each new scale animation ("ofFloat"
                // will crash if no value is passed now).
                0.1234f);

            mAnimator.setInterpolator(new AccelerateInterpolator());
        }

        // The animation needs to be quick; quicker than the hold-for-start
        // duration, ideally. As the animation can start from different
        // "scale" values (e.g., scaling from 1.0 to 0.6, or 0.0 to 1.0, etc.,
        // a fixed duration would not give a consistent rate of scaling:
        // small changes would take just as long to animate as large changes.
        // Therefore, base the duration on the scale "delta", e.g., if
        // scaling from 0.0 to 1.0, use the full duration, but if scaling
        // from 0.3 to 0.8, use only half of the duration (0.8 - 0.3 = 0.5).
        mAnimator.setDuration(Math.round(
            SCALE_ANIM_DURATION * Math.abs(scale - mSolveTimeScale)));

        // "scale" is the target value for the end of the animation. The
        // "ObjectAnimator" will use the "Property.get" (defined above) to
        // get the start value and will call "Property.set" for each frame of
        // the animation.
        mAnimator.setFloatValues(scale);
        mAnimator.start();
    }

    @Override
    public void onTimerCue(@NonNull TimerCue cue) {
        // The displayed time values and the headline are not updated here.
        // They are only updated in "onTimerSet()" and "onTimerRefresh**()"
        // event call-backs.
        switch (cue) {
            case CUE_INSPECTION_HOLDING_FOR_START:
            case CUE_INSPECTION_STARTED:
                cueInspectionTime(false);
                // fall through
            case CUE_INSPECTION_RESUMED:
                // Highlight is *not* changed for "CUE_INSPECTION_RESUMED". The
                // highlight may have been set for "CUE_INSPECTION_OVERRUN" and
                // will stay valid.
                scaleSolveTimeTo(SCALE_SHOW_ONLY_INSPECTION_TIME);
                break;

            case CUE_INSPECTION_READY_TO_START:
                cueInspectionTime(true);
                scaleSolveTimeTo(SCALE_SHOW_ONLY_INSPECTION_TIME);
                break;

            case CUE_INSPECTION_OVERRUN:
                cueInspectionTime(true);
                // fall through
            case CUE_INSPECTION_7S_REMAINING:
            case CUE_INSPECTION_3S_REMAINING:
            case CUE_INSPECTION_TIME_OUT:
                // The text is *not* scaled for some cues. For example,
                // "CUE_INSPECTION_OVERRUN" could fire while holding for the
                // start of the solve timer, which will be showing with the
                // running inspection countdown timer. If there is an overrun
                // and a "+2" penalty is incurred, "onTimerSet()" will be
                // called to report the new penalty and allow a full update.
                break;

            case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
                cueSolveTime(false);
                scaleSolveTimeTo(SCALE_SHOW_BOTH_TIMES);
                break;

            case CUE_INSPECTION_SOLVE_READY_TO_START:
                cueSolveTime(true);
                scaleSolveTimeTo(SCALE_SHOW_BOTH_TIMES);
                break;

            case CUE_SOLVE_READY_TO_START:
                cueSolveTime(true);
                scaleSolveTimeTo(SCALE_SHOW_ONLY_SOLVE_TIME);
                break;

            case CUE_SOLVE_HOLDING_FOR_START:
            case CUE_SOLVE_STARTED:
            case CUE_CANCELLING:
            case CUE_STOPPING:
                cueSolveTime(false);
                scaleSolveTimeTo(SCALE_SHOW_ONLY_SOLVE_TIME);
                break;
        }
    }

    @Override
    public long onTimerRefreshSolveTime(long elapsedTime, long refreshPeriod) {
        mSolveTimeText.setLength(0);

        if (mHideTimeText != null) {
            mSolveTimeText.append(mHideTimeText);
        } else {
            appendRunningSolveTime(mSolveTimeText, elapsedTime, mShowHiRes);
        }

        // The refresh period probably reflects the timer resolution (or near
        // enough), so each time "onTimerRefreshSolveTime()" is called the text
        // will probably change. Therefore, always call "invalidate()" without
        // going to the bother of trying to detect if the text actually changed.
        invalidate();

        // >= 10 minutes or always low-resolution: present time in whole
        // seconds and set the refresh period to match. Use the default
        // ("-1") refresh period, which should be fast enough to show
        // 1/100ths of a second convincingly (it is set at c. 30 Hz).
        return elapsedTime >= 600_000L || !mShowHiRes || mHideTimeText != null
               ? 1_000L : -1L;
    }

    @Override
    public long onTimerRefreshInspectionTime(
            long remainingTime, long refreshPeriod) {

        mInspectionTimeText.setLength(0);
        appendRunningInspectionTime(mInspectionTimeText, remainingTime);
        invalidate();

        // At zero or lower, start updating every 0.1 seconds instead of 1.0
        // seconds. This must be done *at* zero, otherwise the refresh period
        // would not be changed in time to show 0.0, -0.1, -0.2, etc.
        return remainingTime > 0 ? 1_000L : 100L;
    }

    @Override
    public void onTimerSet(@NonNull TimerState timerState) {
        // NOTE: "onTimerSet()" is not called for "temporary" states, such as
        // when the user is holding the timer before starting the solve while
        // the inspection countdown continues to run. Those are the only states
        // where the scale would be "SCALE_SHOW_BOTH_TIMES". Therefore, only one
        // of the remaining inspection time or the elapsed solve time is shown.
        // However, *both* times must be formatted to ensure they are ready if
        // an interaction presents both times before any further "onTimerSet"
        // or "onTimerRefresh**()" notifications.
        formatInspectionTime(timerState);
        formatSolveTime(timerState);

        if (timerState.isInspectionEnabled() && !timerState.isReset()
            && !timerState.isStopped() && !timerState.isSolveRunning()) {

            mHeadlineText = formatInspectionRunningHeadline(timerState);

            // Highlight the inspection time if there is an overrun and a "+2"
            // penalty has been incurred (implicit).
            cueInspectionTime(timerState.getRemainingInspectionTime() < 0);
            scaleSolveTimeTo(SCALE_SHOW_ONLY_INSPECTION_TIME);
        } else {
            if (timerState.isStopped() && timerState.getSolve() != null
                    && timerState.getSolve().getPenalties().hasPenalties()) {
                // Only show the detailed result in the headline if there are
                // penalties to be explained. Otherwise, keep it clean.
                mHeadlineText = timerState.getSolve().toStringResult() + " =";
            } else if (timerState.isSolveRunning()) {
                mHeadlineText = formatSolveRunningHeadline(timerState);
            } else {
                // Timer is reset, or it is stopped with no penalties.
                // TODO: Consider text like "Touch to start!"
                mHeadlineText = null;
            }

            cueInspectionTime(false);
            scaleSolveTimeTo(SCALE_SHOW_ONLY_SOLVE_TIME);
        }

        // Never highlight the solve time. It is only highlighted in temporary
        // "holding" or "ready" states, which are never active in "onTimerSet".
        cueSolveTime(false);

        invalidate();
    }

    @Override
    public void onTimerPenalty(@NonNull TimerState timerState) {
        // A penalty was incurred while the timer was running. Only change the
        // text that is displaying the penalties; the highlights and text scale
        // remain the same. This allows the handling of a penalty during a
        // state such as when the inspection countdown overruns while the user
        // is holding down a touch before starting the solve timer. In that
        // case, the two times are being displayed, but that presentation is
        // not affected by the incurred penalty, as the scale is not changed.
        //
        // If the timer is stopped or reset, there is no need to update
        // anything here.
        //
        // The contract differs from "onTimerSet", which performs a full, clean
        // initialisation of the display. "onTimerSet" is never called when
        // the timer is in a state where both times are being displayed.
        if (!timerState.isReset() && !timerState.isStopped()) {
            // One of the two timers is running (or is about to start running).
            if (!timerState.isInspectionEnabled()
                    || timerState.isSolveRunning()) {
                mHeadlineText = formatSolveRunningHeadline(timerState);
            } else {
                // Timer is holding/ready to start inspection, or inspection is
                // running and timer may or may not also be holding/ready to
                // start the solve attempt.
                mHeadlineText = formatInspectionRunningHeadline(timerState);
            }
        }

        invalidate();
    }

    private String formatInspectionRunningHeadline(
            @NonNull TimerState timerState) {
        // Penalties are not shown in the headline during inspection, as the
        // display of the remaining inspection time is highlighted during
        // the "overrun" period, so a "+2" penalty can be inferred from
        // that rather than adding more clutter. If the penalty is a "DNF",
        // then the timer will be stopped and inspection will not be running.
        return getContext().getString(R.string.inspection);
    }

    @Nullable
    private String formatSolveRunningHeadline(@NonNull TimerState timerState) {
        final Penalties penalties = timerState.getPenalties();

        // NOTE: Group the pre-start and post-start penalties together into one
        // simple headline. (Typically, there will be no post-start penalties
        // at this point, as the UI does not allow them to be added while the
        // timer is running. However, this approach will accommodate changes.)

        final int numPlusTwos = penalties.getPreStartPlusTwoCount()
                                + penalties.getPostStartPlusTwoCount();

        if (numPlusTwos > 1) {
            // "\u00d7", the multiply sign, looks better than the letter "x".
            return "+" + numPlusTwos + "\u00d72s";
        } else if (numPlusTwos > 0) {
            return "+2s";
        }

        // No penalties were incurred.
        return null;
    }

    /**
     * Formats the solve time appropriately for the given timer state. The
     * resolution and rounding of the time and the inclusion/exclusion of time
     * penalties depends on whether or not the timer is stopped.
     *
     * @param timerState The current timer state.
     */
    private void formatSolveTime(@NonNull TimerState timerState) {
        mSolveTimeText.setLength(0);

        if (timerState.isStopped()) {
            // If the timer is stopped, then the solve time must include any
            // time penalties ("getResultTime()") and the time is always shown
            // to a high resolution (subject to the 10-minute limit). The
            // low-resolution preference does not apply when stopped.
            if (timerState.getPenalties().hasDNF()) {
                mSolveTimeText.append(Penalty.DNF.getDescription());
            } else {
                // If stopped, formatting and rounding are performed following
                // WCA Regulations for a "result" time. This may differ from
                // the rounding/truncation that is used when the timer is
                // running, so the more general "TimeUtils" method is used.
                // Efficiency is not important here.
                mSolveTimeText.append(
                    TimeUtils.formatResultTime(timerState.getResultTime()));
            }
        } else if (timerState.isSolveRunning()) {
            // If the solve timer is running, the time may be shown with a low
            // or high resolution, or even masked and not shown at all.
            if (mHideTimeText != null) {
                mSolveTimeText.append(mHideTimeText);
            } else {
                // Rounding/truncation is different for a "running" timer. Use
                // the local ad hoc method to do the rounding and formatting.
                // It is also more efficient, which is important, as a running
                // timer may be updated many times per second.
                appendRunningSolveTime(mSolveTimeText,
                    timerState.getElapsedSolveTime(), mShowHiRes);
            }
        } else {
            // Probably reset, not started (solve), "holding" or ready, so
            // format to full resolution, but do not include penalties. There
            // could be a "+2" penalty incurred if the inspection period has
            // overrun its normal time limit and that is not included until the
            // timer is stopped.
            appendRunningSolveTime(
                mSolveTimeText, timerState.getElapsedSolveTime(), true);
        }
    }

    /**
     * Formats the remaining inspection time. The text will be cleared if the
     * inspection time should not be shown.
     *
     * @param timerState The timer state that provides the inspection time.
     */
    private void formatInspectionTime(@NonNull TimerState timerState) {
        // Outside of the inspection period, clear the inspection time text to
        // avoid confusion during scale transitions. "Inspection period"
        // includes any hold-to-start or ready-to-start state before inspection
        // starts, so "isInspectionRunning()" would not be the correct test.
        mInspectionTimeText.setLength(0);

        if (timerState.isInspectionEnabled() && !timerState.isReset()
              && !timerState.isStopped() && !timerState.isSolveRunning()) {
            appendRunningInspectionTime(
                mInspectionTimeText, timerState.getRemainingInspectionTime());
        }
    }

    /**
     * Formats the solve time, rounding or truncating the value appropriately
     * for a running timer and appends the text to a buffer.
     *
     * @param buffer
     *     The buffer to which to append the formatted time value. This will
     *     not be reset before appending the text.
     * @param time
     *     The time to be formatted (in milliseconds).
     * @param showHiRes
     *     {@code true} if the time should be formatted to a high resolution
     *     (1/100ths of a second for values under 10 minutes); or {@code false}
     *     if all values should be formatted to a low resolution of whole
     *     seconds.
     *
     * @return The given {@code buffer}.
     */
    @VisibleForTesting
    // While "java.lang.Appendable" might be more correct as a return type, it
    // clutters the code with the need to handle "IOException" from "append()".
    static StringBuilder appendRunningSolveTime(
            @NonNull StringBuilder buffer, long time, boolean showHiRes) {
        // NOTE: As this method be called many times per second when refreshing
        // a high resolution time value, it is important that it be efficient.
        // Object allocations need to be eliminated, as garbage collection will
        // make the timer appear "jerky" if refreshing a resolution of 0.01 s.
        // Such "jerky" behaviour was observed in an earlier, "quick-n-dirty"
        // implementation that used "DateTime.toString()". The required time
        // formats are quite simple, so ad hoc formatting will do. A buffer
        // eliminates string allocations.

        if (time >= 600_000L || !showHiRes) {
            // >= 10 minutes or always low-resolution: present time in whole
            // seconds. Round the time to the nearest (or equal) whole second
            // that is not greater than the time. This means that the elapsed
            // does not change from, say, "10:00" to "10:01" until that extra
            // whole second has elapsed.
            final long tSecs = WCAMath.floorToMultiple(time, 1_000L) / 1_000L;

            if (tSecs < 3_600L) {
                // Format as "m:ss". No zero padding for the minutes. Show zero
                // for the minutes, rather than formatting to just "s", as the
                // latter might look a bit odd.
                appendDigitsN(buffer, (int) (tSecs / 60L)).append(':');
                appendDigits2(buffer, (int) (tSecs % 60L));
            } else {
                // Format as "h:mm:ss". No zero padding for the hours.
                appendDigitsN(buffer, (int) (tSecs / 3_600L)).append(':');
                appendDigits2(buffer, (int) (tSecs / 60L % 60L)).append(':');
                appendDigits2(buffer, (int) (tSecs % 60L));
            }
        } else {
            // < 10 minutes: show high-resolution time. Rounding similar to
            // above, but to whole 1/100ths of a second (10 ms).
            final long t100ths = WCAMath.floorToMultiple(time, 10L) / 10L;

            if (t100ths < 6_000L) { // < 1 minute in 100ths
                // Format as "s.SS". No zero padding for the seconds.
                appendDigitsN(buffer, (int) (t100ths / 100L)).append('.');
                appendDigits2(buffer, (int) t100ths);
            } else {
                // Format as "m:ss.SS". No zero padding for the minutes.
                appendDigitsN(buffer, (int) (t100ths / 6_000L)).append(':');
                appendDigits2(buffer, (int) (t100ths / 100L % 60L)).append('.');
                appendDigits2(buffer, (int) t100ths);
            }
        }

        return buffer;
    }

    /**
     * Formats the remaining inspection time, rounding or truncating the time
     * appropriately for a running inspection countdown, and appends the text
     * to a buffer.
     *
     * @param buffer
     *     The buffer to which to append the formatted inspection time. This
     *     will not be reset before appending the text.
     * @param time
     *     The remaining inspection time (in milliseconds). A negative value
     *     indicates that the inspection countdown is overrun. The overrun
     *     period lasts for 2 seconds before the countdown times out and the
     *     result becomes an "DNF".
     *
     * @return The given {@code buffer}.
     */
    @VisibleForTesting
    static StringBuilder appendRunningInspectionTime(
            @NonNull StringBuilder buffer, long time) {
        if (time < 0) {
            // Time is overrun and is negative. Switch to a higher resolution
            // of 1/10th seconds (100 ms) to instill panic.
            //
            // During the overrun, "time" goes from -1 ms to -2,000 ms. However,
            // this will be presented as running from "+2.0" to "+0.0". Rounding
            // to the "ceiling" ensures the time changes at the right instant.
            //
            // The value is "clipped" to ensure that any side effects of slow
            // refresh notifications do not result in any overflow/underflow.
            final int t10ths = (int) WCAMath.ceilToMultiple(
                Math.min(TimerState.INSPECTION_OVERRUN_DURATION,
                    Math.max(0, TimerState.INSPECTION_OVERRUN_DURATION + time)),
                    100L) / 100;

            // Format as "+s.S". Assume always only one digit for each field.
            buffer.append('+');
            appendDigits1(buffer, t10ths / 10).append('.');
            appendDigits1(buffer, t10ths);
        } else {
            // Round the time to the nearest (or equal) whole second that is
            // not less than the time. This means the countdown does not change
            // from, say, "15" to "14" until a whole second has elapsed and the
            // countdown ends exactly when "0" is reached, not one second, or
            // one half second later.

            // Format as "s". No zero padding. Assume it will fit an "int".
            appendDigitsN(
                buffer, (int) WCAMath.ceilToMultiple(time, 1_000L) / 1_000);
        }

        return buffer;
    }

    /**
     * Appends a single decimal numeral to a buffer. The least significant
     * decimal digit (representing the units position) is appended and any more
     * significant digits are ignored. If the digit (or the value) is a zero,
     * zero will be appended.
     *
     * @param buffer
     *     The buffer to which to append exactly one decimal numeral.
     * @param value
     *     The value from which to derive the decimal digit. The behaviour is
     *     not defined if the value is negative.
     *
     * @return
     *     The given {@code buffer}.
     */
    @VisibleForTesting
    static StringBuilder appendDigits1(
            @NonNull StringBuilder buffer, int value) {
        return buffer.append((char) ('0' + value % 10));
    }

    /**
     * Appends two decimal numerals to a buffer. The two least significant
     * decimal digits (representing the units and tens positions) are appended
     * and any more significant digits are ignored. If the tens position is a
     * zero, zero will be appended. For example, "12" is appended as "12", "3"
     * (or "1203") is appended as "03", and "0" is appended as "00".
     *
     * @param buffer
     *     The buffer to which to append exactly two decimal numerals.
     * @param value
     *     The value from which to derive the decimal digits. The behaviour is
     *     not defined if the value is negative.
     *
     * @return
     *     The given {@code buffer}.
     */
    @VisibleForTesting
    static StringBuilder appendDigits2(
            @NonNull StringBuilder buffer, int value) {
        return buffer.append((char) ('0' + value / 10 % 10))
                     .append((char) ('0' + value % 10));
    }

    /**
     * Appends one or more decimal numerals to a buffer. The first (most
     * significant) digit will not be a zero unless the value is zero and only
     * one digit is appended (i.e., there is no left-padding with zeros). No
     * grouping separators (e.g., thousands separators) are appended.
     *
     * @param buffer
     *     The buffer to which to append at least one decimal numeral.
     * @param value
     *     The value from which to derive the decimal digits. Values from 1 to
     *     {@code Integer.MAX_VALUE} are supported. The behaviour is not defined
     *     if the value is negative.
     *
     * @return
     *     The given {@code buffer} with the numerals appended.
     */
    @VisibleForTesting
    static StringBuilder appendDigitsN(
            @NonNull StringBuilder buffer, int value) {
        // Find the order of magnitude of the value. Start searching from the
        // smaller values, as they are more likely. Stop at 1,000,000,000: a
        // positive 32-bit "int" cannot hold values greater than that order of
        // magnitude.
        int mag
            = value <            10 ?             1
            : value <           100 ?            10
            : value <         1_000 ?           100
            : value <        10_000 ?         1_000
            : value <       100_000 ?        10_000
            : value <     1_000_000 ?       100_000
            : value <    10_000_000 ?     1_000_000
            : value <   100_000_000 ?    10_000_000
            : value < 1_000_000_000 ?   100_000_000
            :                         1_000_000_000;

        do {
            buffer.append((char) ('0' + value / mag % 10));
        } while ((mag /= 10) >= 1);

        return buffer;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (DEBUG_ME_MORE) {
            Log.d(TAG,
                "onDraw(): h='" + mHeadlineText + "', i='" + mInspectionTimeText
                + "', s='" + mSolveTimeText + "', ss=" + mSolveTimeScale);
        }

        // Center horizontally. May still get clipped on the left and right.
        final float cX = getWidth() / 2f;
        // Center vertically. Will not clip.
        final float y = (getHeight() - mDrawnHeight) / 2f;
        final float headH = mDrawnHeight * HEADLINE_FACTOR;

        // Draw a "dummy" time value when in the layout editor.
        if (isInEditMode()) {
            mHeadlinePaint.setTextSize(headH * 0.7f);
            canvas.drawText(getClass().getSimpleName(),
                cX, y + headH * 0.6f, mHeadlinePaint);
            drawTimeText(canvas, SAMPLE_TIME_TEXT, mTimeTextSize,
                cX, y + mTimeTextBaseline, mSolveTimePaint);
            return;
        }

        // Set, say, 10% (0.10) as the line gap and then allocate that gap to
        // each "side" in *inverse* proportion to the size of the text on that
        // side. When the sides are 0.50:0.50, the proportions would be
        // 0.45:0.10:0.45; when the sides are 0.70:0.30, the proportions would
        // be 0.67:0.10:0.23 (i.e., split the 10% 70/30 and then subtract the
        // parts from the *opposite* sides). Once one side gets to 0.10, then
        // it will no longer be drawn (or maybe even earlier at < 4px) and the
        // other side will grow until it gets the whole 10%. For example, if
        // the ratio gets to 0.90:0.10, the proportions will be 0.89:0.10:0.01;
        // by 0.95:0.05, the proportions become 0.945:0.10:-0.045. Therefore,
        // once one side turns negative (after the proportions are calculated),
        // it is not drawn. The "gap" does not really exist (nothing is drawn),
        // so one side will reach 1.00 and be shown at the full height.
        final float iTextSize
            = mTimeTextSize * (1f - mSolveTimeScale * (1f +  LINE_GAP_FACTOR));
        final float sTextSize
            = mTimeTextSize
              * (mSolveTimeScale * (1f + LINE_GAP_FACTOR) - LINE_GAP_FACTOR);

        if (mHeadlineText != null) {
            // May have descenders; do not oversize.
            // TODO: Find the proper baseline and text size for the headline.
            mHeadlinePaint.setTextSize(headH * 0.7f);
            canvas.drawText(
                mHeadlineText, cX, y + headH * 0.6f, mHeadlinePaint);
        }

        if (iTextSize >= MIN_SCALED_TEXT_HEIGHT
            || mSolveTimeScale == SCALE_SHOW_ONLY_INSPECTION_TIME) {
            // Scale the position of the baseline from the top in proportion
            // to the text scaling. As the text shrinks the baseline moves
            // closer to the top.
            drawTimeText(canvas, mInspectionTimeText, iTextSize,
                cX, y + headH + mTimeTextBaseline / mTimeTextSize * iTextSize,
                mInspectionTimePaint);
        }

        if (sTextSize >= MIN_SCALED_TEXT_HEIGHT
            || mSolveTimeScale == SCALE_SHOW_ONLY_SOLVE_TIME) {
            // Scale the position of the baseline from the bottom in proportion
            // to the text scaling. As the text shrinks, the baseline moves
            // closer to the bottom.
            final float blToBottom = mDrawnHeight - headH - mTimeTextBaseline;

            drawTimeText(canvas, mSolveTimeText, sTextSize, cX,
                y + headH + mTimeTextBaseline
                  + blToBottom * (mTimeTextSize - sTextSize) / mTimeTextSize,
                mSolveTimePaint);
        }
    }

    private static void drawTimeText(
            @NonNull Canvas canvas, @NonNull StringBuilder timeText,
            float textSize, float centerX, float baselineY,
            @NonNull Paint paint) {

        final int len = timeText.length();

        if (len > 0) {
            // "StringBuilder" does not have an efficient "lastIndexOf" method.
            // What it has takes "String" and allocates "char[]", so....
            int dotIdx = -1;

            for (int i = len - 1; i >= 0; i--) {
                if (timeText.charAt(i) == '.') {
                    dotIdx = i;
                    break;
                }
            }

            if (dotIdx > 0) {
                // There is a decimal point with at least one digit before
                // it: "X.yz". Show the fraction of a second in a smaller
                // text size than the rest of the value.
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(textSize);
                final float bigW = paint.measureText(timeText, 0, dotIdx);

                paint.setTextSize(textSize * TEXT_SIZE_RATIO_100THS);
                final float smallW
                    = paint.measureText(timeText, dotIdx, timeText.length());

                final float bigX = centerX - (bigW + smallW) / 2f;
                final float smallX = bigX + bigW;

                canvas.drawText(timeText, dotIdx, timeText.length(),
                    smallX, baselineY, paint);
                paint.setTextSize(textSize);
                canvas.drawText(timeText, 0, dotIdx, bigX, baselineY, paint);
            } else {
                paint.setTextSize(textSize);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(timeText, 0, len, centerX, baselineY, paint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (DEBUG_ME_MORE) {
            Log.d(TAG, "onMeasure():");
            Log.d(TAG,
                "  width spec.:  " + MeasureSpec.toString(widthMeasureSpec));
            Log.d(TAG,
                "  height spec.: " + MeasureSpec.toString(heightMeasureSpec));
        }

        // Stick to the desired height, unless it is not specified. In that
        // case, use the width as a guide, but use a minimum height (and let
        // things clip), just for sanity.
        //
        // If the width is not specified, allow that the timer may show 9:59.99
        // as the maximum value under ten minutes before the precision changes
        // to seconds. Let the timer run as high as 99:59:59 (1 second shy of
        // 100 hours). Therefore, the width can be defined by the width of
        // "00:00:00" in the current font. It should be reasonably safe to
        // assume that the digit glyphs are all the same width, or, at least,
        // that no digit glyph is wider than that for "0".
        //
        // This does not include space for the penalty markers. However, if
        // these markers are shown above/below the time display, rather than
        // to the left or right, it will probably be best, as the aspect ratio
        // will be closer to square and it allows the markers to be wider, such
        // as "+2s x 3". Even showing the full "A + B + C = D" form in a small
        // font over the final time, etc.
        //
        // NOTE: The typical expectation is that the height will be specified
        // "EXACTLY". In that case, the text size required to fit that height
        // (allowing that there is a "heading" line that must also fit) is
        // determined using the sample text "0123456789.:DNF", as these are the
        // only characters that will be displayed for the time value. This
        // ensures that if the typeface is one that has ascenders, descenders,
        // unequals heights, or uneven baselines, etc., for any of these
        // characters, those quirks will be accommodated.
        final int h = MeasureSpec.getSize(heightMeasureSpec);
        final int hMode = MeasureSpec.getMode(heightMeasureSpec);
        final int w = MeasureSpec.getSize(widthMeasureSpec);
        final int wMode = MeasureSpec.getMode(widthMeasureSpec);

        // The "ideal" height is based on the maximum time height, the headline
        // height and the user's preferred display scale. If the parent layout
        // offers more height, it will not be used; if the parent offers less
        // height, the limit will be respected. If the parent insists on
        // "EXACTLY" a height that is greater than "idealH", the timer will be
        // centred vertically within the over-sized region. However, if extra
        // width is offered, it will be used, as that will help to avoid
        // clipping. The timer contents are always drawn centered horizontally.
        final int idealH = Math.round(
            mMaxTimeHeight / (1f - HEADLINE_FACTOR) * mDisplayScale);
        // Just make something up for "idealW" in case width is "UNSPECIFIED".
        // "* 10" is plenty for something like "999:59:59" or "9:59:59.99".
        final int idealW = idealH * 10;
        int newH = idealH;
        int newW = idealW;

        switch (hMode) {
            case MeasureSpec.AT_MOST:     newH = Math.min(idealH, h); break;
            case MeasureSpec.EXACTLY:     newH = h;                   break;
            case MeasureSpec.UNSPECIFIED: newH = idealH;              break;
        }

        // Will center vertically if higher than ideal.
        mDrawnHeight = Math.min(idealH, newH);

        switch (wMode) {
            case MeasureSpec.AT_MOST:     newW = w;      break;
            case MeasureSpec.EXACTLY:     newW = w;      break;
            case MeasureSpec.UNSPECIFIED: newW = idealW; break;
        }

        // For now, there is no attempt made to ensure the view is wide enough
        // to fit the text. If the text is too wide, it will just be clipped.
        final float tmpTextSize = mSolveTimePaint.getTextSize();

        mTimeTextBaseline = setTextSizeForHeight(
            mDrawnHeight * (1f - HEADLINE_FACTOR), "0123456789:.DNF",
            mSolveTimePaint);
        mTimeTextSize = mSolveTimePaint.getTextSize();
        // Restore the size to avoid any side effects.
        mSolveTimePaint.setTextSize(tmpTextSize);

        if (DEBUG_ME_MORE) {
            Log.d(TAG, "  new width:    " + newW);
            Log.d(TAG, "  new height:   " + newH);
        }

        setMeasuredDimension(newW, newH);
    }

    /**
     * Sets the text size on the {@code Paint} to the maximum value that will
     * fit the text within a region of the given height. The text size may be
     * slightly less than the optimum maximum value, but it will not be
     * greater.
     *
     * @param height
     *     The height of the region into which the text must fit. The minimum
     *     value is 1.0.
     * @param text
     *     The text to fit within the given height.
     * @param textPaint
     *     The {@code Paint} resource on which to set the text size. Any
     *     existing text size set on this object will be ignored and
     *     overwritten. No other properties will be modified. The typeface and
     *     other properties of the text should be set as required before calling
     *     this method.
     *
     * @return
     *     The Y-coordinate of the baseline to use when placing the text with
     *     the region of the given height. The value is relative to the top
     *     of the region (with positive Y oriented downwards).
     */
    private float setTextSizeForHeight(@FloatRange(from=1) float height,
                                       @NonNull String text,
                                       @NonNull Paint textPaint) {
        // The "accuracy" will be the ratio of the rendered height to the
        // required height. It will be > 1.0 if the rendered text is too high
        // and < 1.0 if it is not high enough.
        float accuracy;
        // The "minAccuracy" is the threshold that defines "good enough". It is
        // typically in the 0.90 to 0.98 range. If the "accuracy" is greater
        // than or equal to the minimum accuracy and is less than 1.0, then the
        // search is complete. The goal is to make a guess that is within 2% of
        // ideal, or 1 pixel, whichever is the lower accuracy. For example, if
        // "height" is 10 px, then a target accuracy of 0.90 represents a single
        // pixel of inaccuracy; but if "height" is 100 px, then 0.90 represents
        // 10 px of inaccuracy and 1 px represents accuracy to 0.99, which is
        // overkill for text of that size, so it will be limited to 0.98. Most
        // practical use cases should be within that sort of ball-park for the
        // "height" value.
        final float minAccuracy = Math.min((height - 1f) / height, 0.98f);
        final Rect bounds = new Rect();

        // This "algorithm" is something that I dreamed up a few years ago. It
        // works quite well. It typically gets to within one or two percent of
        // "perfect" and has a very predictable run-time, as there is no loop.
        //
        // "height * 1.3" gives a reasonably good starting point for text
        // samples that do not combine ascenders and descenders (e.g., if all
        // capital letters or all numerals).
        textPaint.setTextSize(height * 1.3f);
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        accuracy = bounds.height() / height;
        if (accuracy > 1f || accuracy < minAccuracy) {
            // Text is too high, or not high enough. Adjust the size in
            // proportion to "accuracy".
            textPaint.setTextSize(textPaint.getTextSize() / accuracy);
            textPaint.getTextBounds(text, 0, text.length(), bounds);

            accuracy = bounds.height() / height;
            if (accuracy > 1f || accuracy < minAccuracy) {
                // Text is too high, or not high enough. This is the last chance
                // to get the value right, so be conservative and underestimate
                // it slightly. For example, if the "minAccuracy" is 0.90, then
                // make a best guess using "textSize / accuracy", but then
                // underestimate it by "(0.90 + 1.0) / 2 = 0.95" to make sure
                // there will be no overshoot.
                textPaint.setTextSize(
                    textPaint.getTextSize() / accuracy * (minAccuracy + 1f)
                    / 2f);
                textPaint.getTextBounds(text, 0, text.length(), bounds);
            }
        }

        // "bounds" describes the text using y=0 as the baseline, so "-top" is
        // the distance from the top of the region defined by the "height" to
        // the baseline. Adjust as necessary to center the text vertically in
        // case the height of the text is a bit less than the required height.
        return -bounds.top - (height - bounds.height()) / 2f;
    }
}
