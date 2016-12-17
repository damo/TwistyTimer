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
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.WCAMath;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Locale;

/**
 * <p>
 * A view that displays the inspection countdown and solve timer. The
 * displayed values are managed by a {@link PuzzleTimer}. An instance of this
 * {@code TimerView} can be added as a listener for the timer's events and
 * refresh notifications.
 * </p>
 * <p>
 * This vi
 * </p>
 * <p>
 * This view does not support any padding attributes; padding is simply ignored.
 * </p>
 * <p>
 * This view does not save its instance state. It is expected that the parent
 * fragment or activity will initialise this view from a {@code PuzzleTimer}
 * whose instance state has been saved. See the description of that class for
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
    private static final boolean DEBUG_ME = true;

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
     * Indicates if the time should be hidden while the solve timer is running.
     * If enabled, the time will be replaced by the {@link #mHideTimeText}.
     */
    private final boolean mIsHideTimeWhileRunning;

    /**
     * The text shown instead of the time if the time is hidden while running.
     */
    private final String mHideTimeText;

    /**
     * Indicates if the "cue start" behaviour is enabled. If enabled, the timer
     * display will be highlighted when it is ready to start and, in that state,
     * lifting up the touch will start the timer. If the hold-to-start behaviour
     * is enabled, the timer display will be highlighted when the hold time has
     * elapsed, even if this "cue start" behaviour is disabled.
     */
    private final boolean mShowStartCue;

    /**
     * Indicates if the hold-to-start behaviour is enabled. On touching down on
     * the timer, the touch must exceed a threshold time (c. half a second)
     * before lifting the touch will start the timer. This only applies at the
     * start of the solve timer, not at the start of the inspection countdown.
     * The timer display will be highlighted when the hold time has elapsed,
     * even if this "cue start" behaviour is disabled.
     */
    private final boolean mIsHoldToStartEnabled;

    /**
     * Indicates if seconds will be shown to a high resolution while the timer
     * is started. If enabled, fractions (hundredths) of a second will be
     * displayed while the chronometer is running.
     */
    private final boolean mShowHiRes;

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
    private String mInspectionTimeText;

    /**
     * The text value for the currently elapsed solve time.
     */
    private String mSolveTimeText;

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

        // Preferences are not available in edit mode, so just hard-code
        // something suitable.
        // FIXME: IMPORTANT: Decide if preferences should be accessed from
        // here at all. This has a bit of a gamy whiff about it. In particular,
        // why is "setDisplayScale" handled differently?
        if (!isInEditMode()) {
            mShowHiRes = Prefs.getBoolean(R.string.pk_show_hi_res_timer,
                R.bool.default_show_hi_res_timer);

            mShowStartCue = Prefs.getBoolean(R.string.pk_start_cue_enabled,
                R.bool.default_start_cue_enabled);

            mIsHoldToStartEnabled = Prefs.getBoolean(
                R.string.pk_hold_to_start_enabled,
                R.bool.default_hold_to_start_enabled);

            mIsHideTimeWhileRunning = Prefs.getBoolean(
                R.string.pk_hide_time_while_running,
                R.bool.default_hide_time_while_running);
            mHideTimeText = getContext().getString(R.string.hideTimeText);
        } else {
            mShowHiRes = true;
            mShowStartCue = true;
            mIsHoldToStartEnabled = true;
            mIsHideTimeWhileRunning = false;
            mHideTimeText = null;
        }

        // FIXME: Remove this later:
        setBackgroundColor(Color.DKGRAY);
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
        @FloatRange(from = 0.1, to = 10.0) float newDisplayScale) throws
        IllegalArgumentException {
        // FIXME: Is there a reason why  this is not just built-in like the
        // other preferences?
        // FIXME: Should the "text offset" also be built in? Should none of
        // these be built in at all? They probably just pollute the class.

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
     * Highlights the inspection time value to provide the user with a cue
     * suggesting that the timer is ready to start. This will have no effect if
     * the cue behaviour is disabled; or if the cue color is already set
     * appropriately. If the color is changed, this view will be invalidated.
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
     * that the timer is ready to start. This will have no effect if the cue
     * behaviour is disabled, unless the hold-to-start behaviour is enabled, as
     * the latter takes precedence; or if the cue color is already set
     * appropriately. If the color is changed, this view will be invalidated.
     *
     * @param isCued
     *     {@code true} to highlight the text in a different color; or {@code
     *     false} to restore the normal text color.
     */
    public void cueSolveTime(boolean isCued) {
        if (mShowStartCue || mIsHoldToStartEnabled) {
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
        final String newText;

        if (mIsHideTimeWhileRunning) {
            newText = mHideTimeText;
        } else {
            newText = formatSolveTime(elapsedTime, mShowHiRes);
        }

        // If the refresh period is less than the timer resolution (e.g., if
        // the refresh period is 10 ms and the time is formatted to whole
        // seconds), or if the time is being hidden while the timer runs, the
        // text may not have changed and "invalidate()" is not needed.
        if (!newText.equals(mSolveTimeText)) {
            mSolveTimeText = newText;
            invalidate();
        }

        // >= 10 minutes or always low-resolution: present time in whole
        // seconds and set the refresh period to match. Use the default
        // ("-1") refresh period, which should be fast enough to show
        // 1/100ths of a second convincingly (it is set at c. 30 Hz).
        return elapsedTime >= 600_000L || !mShowHiRes
               || mIsHideTimeWhileRunning ? 1_000L : -1L;
    }

    @Override
    public long onTimerRefreshInspectionTime(
            long remainingTime, long refreshPeriod) {
        final String newText = formatInspectionTime(remainingTime);

        if (!newText.equals(mInspectionTimeText)) {
            mInspectionTimeText = newText;
            invalidate();
        }

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
        mInspectionTimeText = formatInspectionTime(timerState);
        mSolveTimeText      = formatSolveTime(timerState);

        if (timerState.isInspectionEnabled() && !timerState.isReset()
            && !timerState.isStopped() && !timerState.isSolveRunning()) {

            mHeadlineText = formatInspectionRunningHeadline(timerState);

            // Highlight the inspection time if there is an overrun and a "+2"
            // penalty has been incurred (implicit).
            cueInspectionTime(timerState.getRemainingInspectionTime() < 0);
            scaleSolveTimeTo(SCALE_SHOW_ONLY_INSPECTION_TIME);
        } else {
            if (timerState.isStopped()) {
                mHeadlineText = formatResultHeadline(timerState);
            } else if (timerState.isSolveRunning()) {
                mHeadlineText = formatSolveRunningHeadline(timerState);
            } else {
                // Timer is reset. TODO: Consider text like "Touch to start!"
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

    private String formatResultHeadline(@NonNull TimerState timerState) {
        final Penalties penalties = timerState.getPenalties();

        // NOTE: The solve time in the headline does not include time penalties.
        if (penalties.hasPreStartDNF()) {
            // There will be no solve time (well, it is zero), but there may be
            // "+2" penalties with the DNF. "\u00d7" is the multiply sign and
            // looks better than "x" (the letter). "\u207a" is the superscript
            // plus sign and looks better for the "+2" penalties.
            // FIXME: Actually, the superscript plus looks terrible.
            // FIXME? Not using "Penalty.PLUS_TWO.getDescription()" for now.
            return String.format(Locale.US,
                "\u207a2\u00d7%d + %s =",
                penalties.getPreStartPlusTwoCount(),
                Penalty.DNF.getDescription());
        } else if (penalties.hasPostStartDNF()) {
            return String.format(Locale.US,
                "\u207a2\u00d7%d + %s + \u207a2\u00d7%d + %s =",
                penalties.getPreStartPlusTwoCount(),
                formatSolveTime(timerState.getElapsedSolveTime(), true),
                penalties.getPostStartPlusTwoCount(),
                Penalty.DNF.getDescription());
        }

        // Not a DNF.
        return String.format(Locale.US,
            "\u207a2\u00d7%d + %s + \u207a2\u00d7%d =",
            penalties.getPreStartPlusTwoCount(),
            formatSolveTime(timerState.getElapsedSolveTime(), true),
            penalties.getPostStartPlusTwoCount());
    }

    private String formatInspectionRunningHeadline(
            @NonNull TimerState timerState) {
        // Penalties are not shown in the headline during inspection, as the
        // display of the remaining inspection time is highlighted during
        // the "overrun" period, so a "+2" penalty can be inferred from
        // that, rather than adding more clutter. If the penalty is a "DNF",
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

        if (numPlusTwos > 0) {
            return String.format(Locale.US, "\u207a2\u00d7%d", numPlusTwos);
        }

        // No penalties were incurred.
        return null;
    }

    private String formatSolveTime(@NonNull TimerState timerState) {
        // If the timer is stopped, then the solve time must include any time
        // penalties, otherwise the penalties are not included.
        //
        // If the timer is stopped, the time is shown to a high resolution
        // (subject to the 10-minute limit). The preference to show a low
        // resolution solve does not apply when the timer is stopped.
        //
        // If the solve timer is running, the time may be shown with a low or
        // high resolution, or even masked and not shown at all.
        if (timerState.isStopped()) {
            // "getResultTime()" includes time penalties. Just show "DNF" if
            // the is any DNF, though.
            if (timerState.getPenalties().hasDNF()) {
                return Penalty.DNF.getDescription();
            }
            return formatSolveTime(timerState.getResultTime(), true);
        }

        if (timerState.isSolveRunning()) {
            if (mIsHideTimeWhileRunning) {
                return mHideTimeText;
            }
            return formatSolveTime(
                timerState.getElapsedSolveTime(), mShowHiRes);
        }

        // Probably reset of not started, so format to full resolution, but do
        // not include penalties. There could be a "+2" penalty incurred if the
        // inspection period has overrun its normal time limit.
        return formatSolveTime(timerState.getElapsedSolveTime(), true);
    }

    private static String formatSolveTime(long time, boolean showHiRes) {
        if (time >= 600_000L || !showHiRes) {
            // >= 10 minutes or always low-resolution: present time in whole
            // seconds.
            //
            // Round the time to the nearest (or equal) whole second that is
            // not greater than the time. This means that the elapsed does
            // not change from, say, "10:00" to "10:01" until that extra
            // whole second has elapsed.
            return new DateTime(WCAMath.floorToMultiple(time, 1_000L),
                DateTimeZone.UTC).toString("m:ss");
        }

        // < 10 minutes: show high-resolution time (which is enabled if this
        // is reached).
        //
        // Rounding is like the above, but to a whole 1/100th second (10 ms).
        final long t = WCAMath.floorToMultiple(time, 10L);

        if (t < 60_000L) {
            return new DateTime(t, DateTimeZone.UTC).toString("s.SS");
        }

        return new DateTime(t, DateTimeZone.UTC).toString("m:ss.SS");
    }

    private static String formatInspectionTime(@NonNull TimerState timerState) {
        if (!timerState.isInspectionEnabled() || timerState.isReset()
            || timerState.isStopped() || timerState.isSolveRunning()) {
            // Outside of the inspection period has ended, clear the inspection
            // time text, to avoid confusion during scale transitions.
            return null;
        }

        return formatInspectionTime(timerState.getRemainingInspectionTime());
    }

    private static String formatInspectionTime(long time) {
        if (time < 0) {
            // Time is overrun and is negative. Switch to a higher resolution
            // of 1/10th seconds (100 ms) to instill panic.
            //
            // During the overrun, "time" goes from -1 ms to -2,000 ms. However,
            // this will be presented as running from "+2.0" to "+0.0". Rounding
            // to the "ceiling" ensures the time changes at the right instant.
            return new DateTime(
                WCAMath.ceilToMultiple(
                    TimerState.INSPECTION_OVERRUN_DURATION + time, 100),
                DateTimeZone.UTC).toString("'+'s.S");
        }

        // Round the time to the nearest (or equal) whole second that is not
        // less than the time. This means that the countdown does not change
        // from, say, "15" to "14" until a whole second has elapsed and the
        // countdown ends exactly when "0" is reached, not one second, or one
        // half second later. Formatting with "DateTime" would be overkill.
        return Long.toString(WCAMath.ceilToMultiple(time, 1_000L) / 1_000L);
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
            drawTimeText(canvas, "59:59.99", mTimeTextSize,
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
            @NonNull Canvas canvas, @Nullable String timeText, float textSize,
            float centerX, float baselineY, @NonNull Paint paint) {
        if (timeText != null && !timeText.isEmpty()) {
            final int dotIdx = timeText.lastIndexOf('.');

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
                canvas.drawText(timeText, centerX, baselineY, paint);
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
