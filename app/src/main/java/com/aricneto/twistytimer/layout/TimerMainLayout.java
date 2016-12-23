package com.aricneto.twistytimer.layout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * A layout for the {@code TimerMainFragment} that provides a smooth animation
 * for hiding the tool-bar and presenting the timer "full-screen" and <i>vice
 * versa</i>. This is an <i>ad hoc</i> layout that operates on the assumptions
 * that it contains two child views; that the first child is the tool-bar (or
 * contains the tool-bar); that the first child is initially visible; that the
 * second view is the timer panel that will be expanded or contracted when the
 * tool-bar is hidden or shown, etc.
 *
 * @author damo
 */
public class TimerMainLayout extends LinearLayout
        implements ValueAnimator.AnimatorUpdateListener,
                   Animator.AnimatorListener {
    // NOTE: Tried doing this with "LayoutTransition". While the results were
    // close to what was required, there were problems with the timer being
    // laid out full height before being animated; and there was no way to
    // get the "endTransition" call-back to fire if the transition was
    // already complete without yet more messy code.
    //
    // The original implementation had problems in cases where the animation
    // was already running for one transition when the next transition was
    // fired, or if the same transition was run more than once without running
    // the other transition. Tests in conjunction with the new "onTimerCue"
    // call-backs highlighted these and other problems. A cleaner approach was
    // needed.

    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerMainLayout.class.getSimpleName();

    /**
     * The animator used when pushing or pulling a view on the edge of the
     * layout.
     */
    private final ValueAnimator mAnimator;

    /**
     * A task to run when the animation is complete.
     */
    private Runnable mRunAtEnd;

    /**
     * The state of this tool-bar within this layout.
     */
    private enum State {
        MOVING_OUT,

        OUT,

        MOVING_IN,

        IN,

        CANCELLED,
    }

    /**
     * Indicates if the tool-bar is currently going into hiding.
     */
    private State mState = State.IN;

    public TimerMainLayout(Context context) {
        this(context, null);
    }

    public TimerMainLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimerMainLayout(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mAnimator = new ValueAnimator();
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addUpdateListener(this);
        mAnimator.addListener(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do not let "LinearLayout" draw the tool-bar at its destination
        // position fully *inside* the layout when "onAnimationUpdate" first
        // makes it "VISIBLE" just before starting the first frame of the
        // "showToolbar" animation when the tool-bar should be drawn fully
        // *outside" the layout. The pre-animation "setVisibility(VISIBLE)"
        // triggers a layout pass that cannot be avoided, so this veto is
        // required.

        // FIXME: Would this be necessary if the visibility were set to
        // "INVISIBLE" instead of "GONE"?
        if (!mAnimator.isStarted()) {
            super.onLayout(changed, l, t, r, b);
        }
    }

    /**
     * Hides the tool-bar (and tab strip) by sliding it out of view and expands
     * the timer page to fill the increased space available to it.
     *
     * @param runWhenHidden
     *     An optional task to be executed when the tool-bar is out of view.
     */
    public void hideToolbar(@Nullable Runnable runWhenHidden) {
        if (DEBUG_ME) Log.d(TAG, "hideToolbar(): state=" + mState);

        if (getChildCount() >= 2) {
            start(State.OUT, runWhenHidden);
        }
    }

    /**
     * Shows the tool-bar (and tab strip) by sliding it into view and shrinks
     * the timer page to fit the reduced space available to it.
     *
     * @param runWhenShown
     *     An optional task to be executed when the tool-bar is fully back in
     *     view.
     */
    public void showToolbar(@Nullable Runnable runWhenShown) {
        if (DEBUG_ME) Log.d(TAG, "showToolbar(): state=" + mState);

        if (getChildCount() >= 2) {
            start(State.IN, runWhenShown);
        }
    }

    /**
     * Starts the requested animation. If the previous animation was cancelled,
     * this new animation will pick up from where that animation left off. In
     * that case, any {@code runAtEnd} task for that cancelled animation will
     * <i>not</i> be run.
     *
     * @param targetState
     *     The target state that will be active for at the end of the
     *     transition.
     * @param runAtEnd
     *     An optional {@code Runnable} that will be run when the animation
     *     ends normally. If the animation is cancelled, this will not be run.
     */
    private void start(
            @NonNull State targetState, @Nullable Runnable runAtEnd) {
        // Check the current state (not the "targetState").
        if (mAnimator.isStarted()) {
            // Already being shown/hidden. Cancel and restart, so new "runAtEnd"
            // will run, which may also reverse the animation direction if
            // changing from, say, hide to show.
            mAnimator.cancel(); // May change "mState"
        }

        // Measure the tool-bar to ensure it has a valid height. If the
        // tool-bar initially had a visibility of "GONE", then it may never
        // have been measured before. Also, in case the last animation was
        // cancelled before it completed, the starting top position needs to
        // be detected rather than assumed. When starting from a cancelled
        // animation, the duration will be set based on the fraction of the
        // height that remains to be animated. If the tool-bar is "GONE"
        // assume it is fully outside the bounds, so its top is "-h".
        final View toolbar = getChildAt(0);

        // Ignores padding, etc. (probably).
        toolbar.measure(MATCH_PARENT, WRAP_CONTENT);

        final long maxDuration = getResources().getInteger(
            android.R.integer.config_mediumAnimTime);
        final int h = toolbar.getMeasuredHeight();
        final int t = toolbar.getVisibility() == GONE ? -h : toolbar.getTop();

        switch (targetState) {
            case OUT:
                mAnimator.setIntValues(t, -h);
                mAnimator.setDuration(
                    Math.round((h + t) / (float) h * maxDuration));
                mState = State.MOVING_OUT;
                break;

            case IN:
                mAnimator.setIntValues(t, 0);
                mAnimator.setDuration(Math.round(-t / (float) h * maxDuration));
                mState = State.MOVING_IN;
                break;

            case MOVING_OUT:
            case MOVING_IN:
            case CANCELLED:
                throw new IllegalStateException(
                    "Unexpected target state for 'start': " + mState);
        }

        mRunAtEnd = runAtEnd;
        mAnimator.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        if (animation != null && animation.getAnimatedValue() != null) {

            // Assume animation did not start if there were no children.
            final View toolbar = getChildAt(0);
            // The height difference to be applied to the timer view for this
            // frame.
            final int hDelta
                = toolbar.getTop() - (int) animation.getAnimatedValue();

            if (DEBUG_ME) {
                Log.d(TAG,
                    "onAnimationUpdate(): value=" + animation.getAnimatedValue()
                    + ", top=" + toolbar.getTop() + ", hDelta=" + hDelta
                    + ", visible=" + (toolbar.getVisibility() == VISIBLE));
            }

            if (hDelta != 0) {
                final View timer = getChildAt(1);

                // Move the tool-bar view up or down without changing its
                // height. If it is not already visible, then make it visible
                // (such as when this is the first frame of an animation that
                // slides the tool-bar into view). Making the tool-bar visible
                // here will trigger "onLayout" and cause a "flicker", because
                // "LinearLayout" will position the tool-bar normally and draw
                // it before it is immediately animated to a different position.
                // Therefore, "onLayout" is overridden to avoid changes to the
                // visibility causing such a "flicker" if currently animating.
                toolbar.layout(
                        toolbar.getLeft(),  toolbar.getTop() - hDelta,
                        toolbar.getRight(), toolbar.getBottom() - hDelta);
                toolbar.setVisibility(VISIBLE);

                // "measure()" the timer view to inform it of its new height
                // (i.e., so that the timer can update the layout of its own
                // child views), then position the timer so that its top edge
                // stays in contact with the bottom edge of the tool-bar, while
                // the bottom edge of the timer stays fixed.
                timer.measure(MeasureSpec.makeMeasureSpec(
                                  timer.getWidth(), EXACTLY),
                              MeasureSpec.makeMeasureSpec(
                                  timer.getHeight() + hDelta, EXACTLY));
                timer.layout(
                        timer.getLeft(),  timer.getTop() - hDelta,
                        timer.getRight(), timer.getBottom());
            }
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if (DEBUG_ME) Log.d(TAG, "onAnimationStart(): state=" + mState);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (DEBUG_ME) Log.d(TAG, "onAnimationEnd(): state=" + mState);
        final View toolbar = getChildAt(0);
        final View timer = getChildAt(1);

        // Let the inherited "LinearLayout" set the final positions after the
        // animation once they have completed.
        switch (mState) {
            case CANCELLED:
                // Wait for the next animation to pick up from here. "mRunAtEnd"
                // is not run; the visibility is not changed; and a layout
                // update is not requested.
                break;

            case MOVING_OUT:
                // Completed the "hideToolbar" animation normally.
                mState = State.OUT;
                if (mRunAtEnd != null) {
                    mRunAtEnd.run();
                }
                // fall through
            case OUT:
                // FIXME: I think "INVISIBLE" would work better here, as
                // "onLayout" would not need to be overridden (AFAIK).
                toolbar.setVisibility(GONE);
                timer.requestLayout();
                break;

            case MOVING_IN:
                // Completed the "showToolbar" animation normally.
                mState = State.IN;
                if (mRunAtEnd != null) {
                    mRunAtEnd.run();
                }
                // fall through
            case IN:
                toolbar.requestLayout();
                timer.requestLayout();
                break;
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        if (DEBUG_ME) Log.d(TAG, "onAnimationCancel(): state=" + mState);

        // Animation was cancelled while moving in or out. This occurs when
        // "hideToolbar" is called before the animation for "showToolbar" is
        // complete, or vice versa (or even if one of those operations is called
        // while already running). Set the state to "CANCELLED" to indicate that
        // any new animation must pick up where this old one is leaving off; the
        // "mRunAtEnd" will not run; the visibility of the tool-bar will not be
        // changed; and "LinearLayout" will not be permitted to set the final
        // layout positions. When "Animator.cancel()" is called,
        // "onAnimationCancel" is called and then "onAnimationEnd" is called,
        // so the "CANCELLED" state is mostly used to veto the normal end
        // behaviour.
        mState = State.CANCELLED;
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        // Do nothing.
    }
}
