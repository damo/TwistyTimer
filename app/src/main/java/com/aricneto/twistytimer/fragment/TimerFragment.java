package com.aricneto.twistytimer.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.scramble.ScrambleData;
import com.aricneto.twistytimer.scramble.ScrambleImageLoader;
import com.aricneto.twistytimer.scramble.ScrambleLoader;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalCross;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalXCross;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.timer.OnTimerEventListener;
import com.aricneto.twistytimer.timer.OnTimerEventLogger;
import com.aricneto.twistytimer.timer.PuzzleTimer;
import com.aricneto.twistytimer.timer.SolveHandler;
import com.aricneto.twistytimer.timer.TimerCue;
import com.aricneto.twistytimer.timer.TimerState;
import com.aricneto.twistytimer.timer.TimerView;
import com.aricneto.twistytimer.utils.LoggingLoaderCallbacks;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.TTIntent;
import com.skyfishjy.library.RippleBackground;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.*;
import static com.aricneto.twistytimer.utils.TimeUtils.formatAverageTime;
import static com.aricneto.twistytimer.utils.TimeUtils.formatResultTime;

public class TimerFragment extends BaseMainFragment
        implements OnBackPressedInFragmentListener,
                   StatisticsCache.StatisticsObserver,
                   OnTimerEventListener, SolveHandler {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * Flag to enable detailed debug logging of touch events for the timer.
     */
    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG_TOUCHES = DEBUG_ME && false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerFragment.class.getSimpleName();

    /**
     * The prefix used before the key names for the values in the saved
     * instance state.
     */
    private static final String KEY_PREFIX
        = TimerFragment.class.getName() + '.';

    /**
     * The key used to identify the saved instance state of the puzzle timer.
     */
    private static final String KEY_PUZZLE_TIMER_STATE
        = KEY_PREFIX + "puzzleTimer";

    /**
     * The key used to identify the saved instance state of the unsolved
     * scramble data, if there is one. The associated scramble image is not
     * saved; it will be re-generated from the scramble sequence if the
     * scramble sequence is restored from saved instance state.
     */
    private static final String KEY_UNSOLVED_SCRAMBLE
        = KEY_PREFIX + "unsolvedScramble";

    /**
     * A view tag used on the "+/-DNF" button to indicate if a DNF should be
     * annulled or incurred when the button is clicked. If this tag is present,
     * the button action should annul a DNF; if not present, incur a DNF.
     */
    private static final Object ANNUL_DNF = new Object();

    // NOTE: There are an awful lot of views, so it will be easier to tell
    // them apart from all of the other fields by prefixing them with "mv"
    // for "member-view".
    private Unbinder mUnbinder;
    // Panel showing the overview of the session statistics.
    @BindView(R.id.session_stats_panel)   View     mvStatsPanel;
    @BindView(R.id.sessionStatsTimesAvg)  TextView mvStatsTimesAvg;
    @BindView(R.id.sessionStatsTimesMore) TextView mvStatsTimesMore;

    // The main time display and the larger view that reacts to touches to
    // start/stop the timer.
    @BindView(R.id.timer)             TimerView   mvTimerView;
    @BindView(R.id.timer_touch_proxy) View        mvTimerTouchProxy;

    // The scramble text, image and big expanded image (shown when the small
    // image is clicked).
    @BindView(R.id.scramble_text)    TextView  mvScrambleText;
    @BindView(R.id.scramble_image)   ImageView mvScrambleImg;
    @BindView(R.id.expanded_image)   ImageView mvBigScrambleImg;
    @BindView(R.id.root)             View      mvBigScrambleImgBoundary;
    @BindView(R.id.progress_spinner) View      mvScrambleProgress;

    // The panel showing the hints for the cross and X-cross. The panel is
    // revealed using a sliding layout.
    @BindView(R.id.sliding_layout)    SlidingUpPanelLayout mvCrossHintsSlider;
    @BindView(R.id.cross_hints_panel) View          mvCrossHintsPanel;
    @BindView(R.id.hintText)          TextView      mvCrossHintsText;
    @BindView(R.id.hintProgress)      View          mvCrossHintsProgress;
    @BindView(R.id.hintProgressText)  TextView      mvCrossHintsProgressText;

    // The quick action buttons and other decorations shown after a solve
    // time is recorded.
    @BindView(R.id.quick_action_buttons) View             mvQuickActionPanel;
    @BindView(R.id.button_delete)        ImageView        mvDeleteButton;
    @BindView(R.id.button_dnf)           TextView         mvDNFButton;
    @BindView(R.id.button_plus_two)      TextView         mvPlusTwoButton;
    @BindView(R.id.button_minus_two)     TextView         mvMinusTwoButton;
    @BindView(R.id.button_comment)       ImageView        mvCommentButton;
    @BindView(R.id.ripple_background)    RippleBackground mvCongratsRipple;
    @BindView(R.id.congrats_text)        TextView         mvCongratsText;

    /**
     * The puzzle timer that manages the inspection countdown and solve
     * timing. This timer object does not have any user interface; it
     * notifies this fragment via call-back methods when the user interface
     * needs to be updated.
     */
    // NOTE: Persisted as part of the instance state of this fragment.
    private PuzzleTimer mTimer;

    /**
     * The database ID of the {@code Solve} that was last verified to still
     * exist in the database.
     */
    private long mVerifiedSolveID = Solve.NO_ID;

    /**
     * Just after the timer is stopped, the tool-bar is restored into view. The
     * restoration is animated and takes a short time. During this time, this
     * flag should be raised to block touch events from reaching the timer and
     * starting it again before the tool-bar is fully restored.
     */
    private boolean mIsRestoringToolBar = false;

    /**
     * The next available scramble that has been generated, but has not yet
     * been solved. This is updated each time a new scramble is generated and
     * again when the corresponding scramble image is generated. It is reset to
     * {@code null} if it is "consumed" at the start of a solve attempt.
     */
    private ScrambleData mUnsolvedScramble;

    /**
     * The animator that is currently running to zoom between the small
     * scramble image and the big scramble image shown when the small image
     * is clicked. If {@code null}, then no animation is running. If
     * non-{@code null} and the image (either) is clicked, then the animator
     * must be cancelled to stop the animation and apply the correct reverse
     * animation. See {@link #zoomScrambleImage(View, View, View, int)}.
     */
    private Animator mScrambleImgAnimator;

    private boolean mShowCrossHints;

    /**
     * The most recently notified solve time statistics. When
     * {@link #proclaimRecordTimes(Solve)} is called, the new time can be
     * compared to these statistics to detect a new record.
     */
    private Statistics mRecentStatistics;

    /**
     * The receiver for broadcast notifications about interactions with, or
     * changes to the state of, the user interface. Specifically, interactions
     * with elements of the UI that are managed by {@link TimerMainFragment}
     * can be identified by this fragment and acted upon as necessary.
     */
    private final TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceiveWhileAdded(): " + intent);

            switch (intent.getAction()) {
                case ACTION_TOOLBAR_RESTORED:
                    showItems();
                    mIsRestoringToolBar = false;
                    break;

                case ACTION_GENERATE_SCRAMBLE:
                    // Action here is not in the same intent category that
                    // "ScrambleLoader" is monitoring. Relay this request from
                    // the tool-bar button to that loader. Safe to access state,
                    // as "isAdded()" is a given.
                    //
                    // This action is fired in response to the user clicking
                    // the generate-scramble button in the tool-bar. Therefore,
                    // first clear any current unsolved scramble, so that
                    // "generateScramble" will not just detect that a scramble
                    // is already available and do nothing. The user probably
                    // wants a new scramble, not the old one.
                    mUnsolvedScramble = null;
                    generateScramble(getMainState().getPuzzleType());
                    break;
            }
        }
    };

    /**
     * <p>
     * The receiver for broadcast notifications that describe changes to the
     * solve time data in the database.
     * </p>
     * <p>
     * This timer fragment displays the result of the last solve attempt and
     * provides quick action buttons to allow basic changes to, or deletion
     * of, the corresponding saved {@code Solve}. If the source of a change
     * (identified by an intent extra that holds the source class) is
     * <i>not</i> this timer fragment class, then the display of the last solve
     * attempt will be reset to ensure that no further edits are possible and
     * that out-of-date solve data is not presented.
     * </p>
     */
    private final TTFragmentBroadcastReceiver mSolveDataChangedReceiver
        = new TTFragmentBroadcastReceiver(this, CATEGORY_SOLVE_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceiveWhileAdded(): " + intent);

            // Validation ensures that where an intent action expects a "Solve"
            // instance or solve ID that one will be present, or an exception
            // will be thrown. Null-checks can then be skipped in the "case"
            // statements later.
            TTIntent.validate(intent);

            final Solve intentSolve = TTIntent.getSolve(intent);
            final long intentSolveID = intentSolve != null
                ? intentSolve.getID() : TTIntent.getSolveID(intent);

            // This receiver is not registered until after "mTimer" is set,
            // so "mTimer" will not be null.
            final Solve timerSolve = mTimer.getTimerState().getSolve();
            final long timerSolveID = timerSolve != null
                ? timerSolve.getID() : Solve.NO_ID;

            // IMPORTANT: The general approach here is to check if the timer is
            // "stopped" (which does not include being "reset") and then update
            // the timer to reflect any changes to the solve it is managing. If
            // the timer is reset, then it is not managing any solve. If the
            // timer is running, then the solve it is managing has yet to be
            // saved. In either case, changes to the solves saved in the
            // database cannot have any effect on the reset or running timer.
            //    *ALL* changes affecting the timer state or the UI state are
            // applied by calling "PuzzleTimer.onSolveChanged(Solve)" or
            // "PuzzleTimer.reset()". These will cause "onTimerSet(TimerState)"
            // to be called back on this fragment and the changes will be
            // applied to the UI by that method (and only by that method).
            //    If the timer is running, it might be cancelled and then roll
            // back to the "previous" solve result. However, until the result
            // is rolled back, no action is taken to verify if changes to the
            // solves in the database affect that "previous" solve. After the
            // roll-back, "onTimerSet" will be called and a verification task
            // will be initiated to ensure that the "previous" solve still
            // exists and that its details are up-to-date. This keeps things
            // nice and simple: only the current solve is ever verified and
            // that is only necessary if the timer is stopped. The only caveat
            // is that "mValidatedSolveID" needs to be cleared in cases where
            // it might record the ID of the "previous" solve. If it were not
            // cleared, then "onTimerSet" would not trigger the necessary
            // verification task if the running solve attempt were cancelled
            // and rolled back to the "previous" solve result. The simplest
            // approach is to clear "mValidatedSolveID" in "onSolveStart()",
            // as that method is called each time a new solve attempt starts
            // and each time a roll-back could potentially follow.

            if (!mTimer.getTimerState().isStopped() || timerSolve == null) {
                // If the timer is not "stopped", then it has no current solve
                // that could match any solve in the database, so changes to
                // the data cannot affect the timer state. Do nothing.
                return;
            }

            switch (intent.getAction()) {
                case ACTION_SOLVE_VERIFIED:
                case ACTION_ONE_SOLVE_UPDATED:
                    // If the solve that passed verification or has been updated
                    // is the one that is currently being managed by the timer,
                    // then record that is has been verified and update it with
                    // the intent solve (which was just re-read from, or written
                    // to, the database and is sure to be up-to-date). This will
                    // trigger a call-back to "onTimerSet".
                    if (timerSolveID == intentSolveID) {
                        mVerifiedSolveID = intentSolveID;
                        //noinspection ConstantConditions (validated above)
                        mTimer.onSolveChanged(intentSolve);
                    }
                    break;

                case ACTION_SOLVE_NOT_VERIFIED:
                case ACTION_ONE_SOLVE_DELETED:
                    // If the solve that failed verification or was deleted is
                    // the one that is currently being managed by the timer,
                    // then reset the timer (which will call back to
                    // "onTimerSet()").
                    if (timerSolveID == intentSolveID) {
                        mVerifiedSolveID = Solve.NO_ID;
                        mTimer.reset();
                    }
                    break;

                case ACTION_ONE_SOLVE_ADDED:
                    // If the timer is stopped, but its solve has no ID, then
                    // it is waiting for that solve to be saved and assigned
                    // its ID. Replace the solve in the timer with the saved
                    // solve that has its newly assigned solve ID. As the
                    // intent solve has just been saved to the database,
                    // consider it "verified" by default.
                    //    If a solve is added manually, it could be confused
                    // with a solve being managed by the timer that is awaiting
                    // a save. However, that is not likely given the way the UI
                    // works and the serial processing of database writes. At
                    // worst, the timer will show the manually-added solve as
                    // its latest result, which will work just fine. The solve
                    // that the timer was saving will be saved as normal, just
                    // not shown afterwards, as the timer's solve now has an
                    // ID other than "NO_ID".
                    //    The likelihood of confusion with a manually-added
                    // solve can be reduced by checking that the solve has the
                    // same puzzle type and solve category as the timer's solve.
                    // However this is unlikely to be different and would not
                    // really matter, anyway, so do not bother to check it.
                    if (timerSolveID == Solve.NO_ID) {
                        mVerifiedSolveID = intentSolveID;
                        //noinspection ConstantConditions (validated above)
                        mTimer.onSolveChanged(intentSolve);
                    }
                    break;

                case ACTION_MANY_SOLVES_ADDED:
                    // Nothing to do. Adding other new solves does not affect
                    // any result being displayed by the timer.
                    break;

                case ACTION_MANY_SOLVES_DELETED:
                case ACTION_SOLVES_MOVED_TO_HISTORY:
                    // It is not known whether or not the solve currently being
                    // managed by the timer was deleted or moved to the history.
                    // Just re-verify the current solve to find out. Clear
                    // "mVerifiedSolveID" and update the current solve with
                    // itself, which will trigger a call-back to "onTimerSet()"
                    // which will, in turn, disable the editing controls until
                    // the result of the verification task is broadcast back to
                    // this receiver.
                    //    If the timer's solve was deleted, then the timer will
                    // be reset when "ACTION_SOLVE_NOT_VERIFIED" is received.
                    //    If the timer's solve was moved to the history, then
                    // the change to the "history" flag will be applied to the
                    // solve held by the timer when "ACTION_SOLVE_VERIFIED" is
                    // received. This ensures that any subsequent edits will
                    // not lose the change to the "history" flag when they
                    // update the database record based on edits applied to the
                    // "Solve" instance held by the timer.
                    //    If the intent extras define a puzzle type and solve
                    // category that do not match those properties of the
                    // timer's solve instance, then this re-verification would
                    // not be necessary. However, it is simpler not to bother
                    // checking for a match, as a match is likely in most cases,
                    // given the way the UI works.
                    mVerifiedSolveID = Solve.NO_ID;
                    mTimer.onSolveChanged(timerSolve);
                    break;
            }
        }
    };
    public TimerFragment() {
        // Required empty public constructor
    }

    public static TimerFragment newInstance() {
        final TimerFragment fragment = new TimerFragment();
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
//        if (DEBUG_ME) Looper.myLooper().setMessageLogging(
//            new LogPrinter(Log.DEBUG, TAG));

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG,
            "onCreate(savedInstanceState=" + savedInstanceState + ')');

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG,
            "onCreateView(savedInstanceState=" + savedInstanceState + ')');

        // Inflate the layout for this fragment and bind the fields to their
        // View objects.
        final View root = inflater.inflate(
            R.layout.fragment_timer, container, false);

        mUnbinder = ButterKnife.bind(this, root); // Unbound in "onDestroyView".

        mvDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSolve();
            }
        });

        mvDNFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // A view "tag" indicates the "toggle" direction for the DNF.
                if (v.getTag() == ANNUL_DNF) {
                    annulPostStartPenalty(Penalty.DNF);
                } else {
                    incurPostStartPenalty(Penalty.DNF);
                }
            }
        });

        mvPlusTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incurPostStartPenalty(Penalty.PLUS_TWO);
            }
        });

        mvMinusTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                annulPostStartPenalty(Penalty.PLUS_TWO);
            }
        });

        mvCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editSolveComment();
            }
        });

        mvCrossHintsPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUnsolvedScramble != null) {
                    mvCrossHintsText.setVisibility(View.GONE);
                    mvCrossHintsText.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP, 16);
                    mvCrossHintsText.setGravity(Gravity.START);
                    mvCrossHintsProgress.setVisibility(View.VISIBLE);
                    mvCrossHintsProgressText.setVisibility(View.VISIBLE);
                    mvCrossHintsSlider.setPanelState(PanelState.EXPANDED);
                    new GetOptimalCross().executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, mUnsolvedScramble);
                }
            }
        });

        // Necessary for the scramble image to show
        mvScrambleImg.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mvBigScrambleImg.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Get the system's default "short" animation time; use it when
        // zooming the scramble image.
        final int animDuration = getResources().getInteger(
            android.R.integer.config_shortAnimTime);

        // The listener that will expand the small scramble image.
        mvScrambleImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomScrambleImage(mvScrambleImg, mvBigScrambleImg,
                    mvBigScrambleImgBoundary, animDuration);
            }
        });

        // Relay (filtered) touch events from the "proxy" that accepts touches
        // through to the "PuzzleTimer" (not a view). The "TimerView" itself is
        // not touch-sensitive.
        final float touchMargin
            = getResources().getDimension(R.dimen.timer_touch_proxy_margin);

        mvTimerTouchProxy.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (DEBUG_TOUCHES) Log.d(TAG,
                    "TimerTouchProxy: onTouch(): action="
                    + motionEvent.getAction());

                if (mIsRestoringToolBar) {
                    // Not ready to start the timer, yet. Waiting on the
                    // animation of the restoration of the tool-bars after the
                    // timer was stopped. In this state, the timer cannot be
                    // running or counting down, so touches should be ignored
                    // to prevent it from starting again until the UI settles.
                    if (DEBUG_TOUCHES) Log.d(TAG,
                        "  Touch ignored: currently restoring tool-bar.");
                    return false;
                }

                // When the timer is not "busy", ignore touches along the left
                // margin of the view, as the user is probably trying to open
                // the side drawer menu. Otherwise, pass on those touches, so
                // there is no "dead zone" when trying to stop the timer.
                if (motionEvent.getX() <= touchMargin && !isTimerBusy()) {
                    if (DEBUG_TOUCHES) Log.d(TAG,
                        "  Touch ignored: too close to screen margin.");
                    return false;
                }

                // Just need to notify the PuzzleTimer of these events and it
                // will do the rest.
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTimer.onTouchDown();
                        return true;

                    case MotionEvent.ACTION_UP:
                        mTimer.onTouchUp();
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        // The parent view has intercepted the recent sequence
                        // of touch events and has cancelled the touches for
                        // this view. Most likely, a "ViewPager" has interpreted
                        // the touches as a "swipe" gesture that it will respond
                        // to by changing the current page (or tab).
                        //
                        // "ACTION_CANCEL" is similar to "ACTION_UP", but the
                        // normal operation performed for an
                        // "ACTION_DOWN"..."ACTION_UP" sequence must *not* be
                        // performed. "PuzzleTimer" understands this and will
                        // revert whatever it did for the last "onTouchDown"
                        // if it was not followed by "onTouchUp".
                        //
                        // IMPORTANT: "PuzzleTimer" will not cancel the last
                        // "onTouchDown" if that touch stopped the timer. The
                        // timer will remain stopped. This ensures that even
                        // "sloppy" touches will always stop the timer
                        // immediately.
                        mTimer.onTouchCancelled();
                        return true;

                    default:
                        if (DEBUG_TOUCHES) Log.d(TAG,
                            "  Touch ignored: unexpected motion event action.");
                        break;
                }

                return false;
            }
        });

        return root;
    }

    private void incurPostStartPenalty(@NonNull Penalty penalty) {
        if (mTimer == null || !mTimer.getTimerState().isStopped()) {
            // Unlikely, as a button has probably just been pressed and buttons
            // are only shown when the timer is stopped. Still, be careful.
            return;
        }

        final Solve oldSolve = mTimer.getTimerState().getSolve();

        if (oldSolve != null) {
            final Penalties oldPenalties = oldSolve.getPenalties();
            final Penalties newPenalties
                = oldPenalties.incurPostStartPenalty(penalty);

            if (!newPenalties.equals(oldPenalties)) {
                final Solve newSolve
                    = oldSolve.withPenaltiesAdjustingTime(newPenalties);

                // Disable the quick action buttons while the background task
                // is running; they will still be shown (and will reflect the
                // newly-incurred penalty), but they will not allow concurrent
                // edits.
                //
                // "mVerifiedSolveID" is also reset, as that previously-verified
                // solve (if set) is about to change, so if "onTimerSet" is
                // called (for some reason), it will not re-enable the quick
                // action buttons until it runs a verification, which will pick
                // up the details of the updated solve.
                //
                // The task broadcasts "ACTION_ONE_SOLVE_UPDATED" when done.
                // When that action is received, the timer will be notified via
                // "onSolveChanged()" and it can then record the newly-updated
                // "Solve" and call back "onTimerSet()". "onTimerSet()" will
                // updated the timer display and the state of the quick-action
                // buttons.
                mVerifiedSolveID = Solve.NO_ID;
                showQuickActionButtons(newSolve, false); // Show but disable.
                // If a new penalty is incurred any "congratulations" on the
                // solve being a new record may no longer be valid.
                hideCongratulations();
                TwistyTimer.getDBHandler().updateSolveAndNotifyAsync(newSolve);
            }
        }
    }

    private void annulPostStartPenalty(@NonNull Penalty penalty) {
        // NOTE: See comments in "incurPostStartPenalty(Penalty)" for details.
        if (mTimer == null || !mTimer.getTimerState().isStopped()) {
            return;
        }

        final Solve oldSolve = mTimer.getTimerState().getSolve();

        if (oldSolve != null) {
            final Penalties oldPenalties = oldSolve.getPenalties();
            final Penalties newPenalties
                = oldPenalties.annulPostStartPenalty(penalty);

            if (!newPenalties.equals(oldPenalties)) {
                final Solve newSolve
                    = oldSolve.withPenaltiesAdjustingTime(newPenalties);

                mVerifiedSolveID = Solve.NO_ID;
                showQuickActionButtons(oldSolve, false);
                // Annulling a penalty may result in a record "worst" score not
                // being the worst any more, so hide any "congratulations" text.
                hideCongratulations();
                TwistyTimer.getDBHandler().updateSolveAndNotifyAsync(newSolve);
            }
        }
    }

    private void deleteSolve() {
        // NOTE: See comments in "incurPostStartPenalty(Penalty)" for details.
        if (mTimer == null || !mTimer.getTimerState().isStopped()) {
            return;
        }

        final Solve oldSolve = mTimer.getTimerState().getSolve();

        if (oldSolve != null) {
            new MaterialDialog.Builder(getContext())
                .content(R.string.delete_dialog_confirmation_title)
                .positiveText(R.string.delete_dialog_confirmation_button)
                .negativeText(R.string.delete_dialog_cancel_button)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog,
                                        @NonNull DialogAction which) {
                        // Like "incurPostStartPenalty()", but when the delete
                        // task is done, "ACTION_ONE_SOLVE_DELETED" is received.
                        mVerifiedSolveID = Solve.NO_ID;
                        showQuickActionButtons(oldSolve, false);
                        hideCongratulations();
                        TwistyTimer.getDBHandler()
                                   .deleteSolveAndNotifyAsync(oldSolve);
                    }
                })
                .show();
        }
    }

    private void editSolveComment() {
        // NOTE: See comments in "incurPostStartPenalty(Penalty)" for details.
        if (mTimer == null || !mTimer.getTimerState().isStopped()) {
            return;
        }

        final Solve oldSolve = mTimer.getTimerState().getSolve();

        if (oldSolve != null) {
            MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .title(R.string.add_comment)
                .input("", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog,
                                        CharSequence input) {
                        final Solve newSolve
                            = oldSolve.withComment(input.toString());

                        if (!newSolve.equals(oldSolve)) {
                            mVerifiedSolveID = Solve.NO_ID;
                            showQuickActionButtons(newSolve, false);
                            // Changing the comment does not affect the validity
                            // of any "congratulations" text.
                            TwistyTimer.getDBHandler()
                                       .updateSolveAndNotifyAsync(newSolve);

                            // This is from the old implementation. While the
                            // comment will be added on a background task, just
                            // assume it will succeed and notify the user that
                            // the comment has been added, as there will be no
                            // other visual indication of the change, unlike
                            // when penalties are added, or a solve is deleted.
                            Toast.makeText(getContext(),
                                getString(R.string.added_comment),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                .positiveText(R.string.action_done)
                .negativeText(R.string.action_cancel)
                .build();

            final EditText editText = dialog.getInputEditText();

            if (editText != null) {
                editText.setSingleLine(false);
                editText.setLines(3);
                editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            }

            dialog.show();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated(savedInstanceState="
                + savedInstanceState + ')');
        super.onActivityCreated(savedInstanceState);

        if (DEBUG_ME) Log.d(TAG,
            "  onActivityCreated -> initLoader: SCRAMBLE LOADER");
        getLoaderManager().initLoader(
            MainActivity.SCRAMBLE_LOADER_ID, null,
            new LoggingLoaderCallbacks<ScrambleData>(TAG, "SCRAMBLE LOADER") {
                @Override
                public Loader<ScrambleData> onCreateLoader(
                    int id, Bundle args) {
                    if (DEBUG_ME) super.onCreateLoader(id, args);
                    return new ScrambleLoader(getContext());
                }

                @Override
                public void onLoadFinished(Loader<ScrambleData> loader,
                                           ScrambleData data) {
                    if (DEBUG_ME) super.onLoadFinished(loader, data);
                    onScrambleGenerated(data);
                }

                @Override
                public void onLoaderReset(Loader<ScrambleData> loader) {
                    if (DEBUG_ME) super.onLoaderReset(loader);
                }
            });

        if (DEBUG_ME) Log.d(TAG,
            "  onActivityCreated -> initLoader: SCRAMBLE IMAGE LOADER");
        getLoaderManager().initLoader(
            MainActivity.SCRAMBLE_IMAGE_LOADER_ID, null,
            new LoggingLoaderCallbacks<ScrambleData>(
                    TAG, "SCRAMBLE IMAGE LOADER") {
                @Override
                public Loader<ScrambleData> onCreateLoader(
                    int id, Bundle args) {
                    if (DEBUG_ME) super.onCreateLoader(id, args);
                    return new ScrambleImageLoader(getContext());
                }

                @Override
                public void onLoadFinished(Loader<ScrambleData> loader,
                                           ScrambleData data) {
                    if (DEBUG_ME) super.onLoadFinished(loader, data);
                    onScrambleImageGenerated(data);
                }

                @Override
                public void onLoaderReset(Loader<ScrambleData> loader) {
                    if (DEBUG_ME) super.onLoaderReset(loader);
                }
            });

        // The view has been created, so restore the instance state and link
        // the timer to the UI. From API 17, this could be done in
        // "onViewStateRestored", but this needs to support API 16, for now.
        //
        // Create the puzzle timer. When created, the timer will be in the
        // "sleeping" state. It will call the listeners until after
        // "PuzzleTimer.wake()" is called in "onResume()".
        mTimer = new PuzzleTimer(this);
        if (DEBUG_ME) mTimer.addOnTimerEventListener(new OnTimerEventLogger());
        mTimer.addOnTimerEventListener(this);
        mTimer.addOnTimerEventListener(mvTimerView);
        mTimer.setOnTimerRefreshListener(mvTimerView);

        if (savedInstanceState != null) {
            // If the "Parcelable" is "null", "onRestoreInstanceState" will
            // have no effect.
            mTimer.onRestoreInstanceState(
                savedInstanceState.getParcelable(KEY_PUZZLE_TIMER_STATE));

            // The unsolved scramble may be null. If not null, it will *not*
            // have any image, as images are not saved and restored. An image
            // will be generated later in "onResume()", if needed.
            mUnsolvedScramble
                = savedInstanceState.getParcelable(KEY_UNSOLVED_SCRAMBLE);
        }

        // If the statistics are already loaded, the update notification will
        // have been missed, so fire that notification now. If the statistics
        // are non-null, they will be displayed. If they are null (i.e., not
        // yet loaded), nothing will be displayed until this fragment, as a
        // registered observer, is notified when loading is complete.
        onStatisticsUpdated(StatisticsCache.getInstance().getStatistics());

        // Unregister these in "onDestroyView".
        StatisticsCache.getInstance().registerObserver(this);
        registerReceiver(mUIInteractionReceiver);
        registerReceiver(mSolveDataChangedReceiver);

        // Do not generate a scramble now. The loaders may not be ready and
        // listening until after this method returns and the message queue is
        // processed. Wait until "onResume()".
    }

    @Override
    public void onStart() {
        if (DEBUG_ME) Log.d(TAG, "onStart()");
        super.onStart();

        // The view and all of the instance state has been restored. However,
        // this may not be the first time that "onStart()" has been called.
        // "SettingsActivity" may have been started and some preferences may
        // have changed, so now is the time to apply the up-to-date values of
        // the shared preferences to the views and other components.

        // Inject the up-to-date inspection duration and hold-to-start flag
        // into the puzzle timer. If the instance state was restored in
        // "onActivityCreated()", the up-to-date values will be used for any
        // *new* solve attempts. If the timer was running when its state was
        // saved, the old preference values will be used until the completion
        // of that solve attempt.

        mTimer.setInspectionDuration(
            Prefs.getBoolean(R.string.pk_inspection_enabled,
                             R.bool.default_inspection_enabled)
                ? Prefs.getInspectionTime() * 1_000L // Convert s to ms.
                : 0);                                // Inspection disabled.
        mTimer.setHoldToStartEnabled(
            Prefs.getBoolean(R.string.pk_hold_to_start_enabled,
                             R.bool.default_hold_to_start_enabled));

        // The start cue is always enabled if hold-to-start is enabled.
        mvTimerView.setStartCueEnabled(
            Prefs.getBoolean(R.string.pk_start_cue_enabled,
                             R.bool.default_start_cue_enabled)
            || Prefs.getBoolean(R.string.pk_hold_to_start_enabled,
                                R.bool.default_hold_to_start_enabled));
        mvTimerView.setHiResTimerEnabled(
            Prefs.getBoolean(R.string.pk_show_hi_res_timer,
                             R.bool.default_show_hi_res_timer));
        mvTimerView.setHideTimeText(
            Prefs.getBoolean(R.string.pk_hide_time_while_running,
                             R.bool.default_hide_time_while_running)
                ? getString(R.string.hideTimeText)
                : null); // Solve time will not be hidden/masked.

        // Apply most of the "advanced timer appearance settings".
        if (Prefs.getBoolean(R.string.pk_advanced_timer_settings_enabled,
                             R.bool.default_advanced_timer_settings_enabled)) {
            mvTimerView.setDisplayScale(Prefs.getTimerDisplayScale());

            if (Prefs.getBoolean(R.string.pk_large_quick_actions_enabled,
                                 R.bool.default_large_quick_actions_enabled)) {
                mvDeleteButton.setImageDrawable(ContextCompat.getDrawable(
                    getContext(), R.drawable.ic_delete_white_36dp));
                mvCommentButton.setImageDrawable(ContextCompat.getDrawable(
                    getContext(), R.drawable.ic_comment_white_36dp));

                final float qabTextSize
                    = getResources().getDimension(R.dimen.qab_text_size_large);

                mvDNFButton.setTextSize(qabTextSize);
                mvPlusTwoButton.setTextSize(qabTextSize);
                mvMinusTwoButton.setTextSize(qabTextSize);
            }

            final int timerTextOffsetPx
                = Prefs.getInt(R.string.pk_timer_text_offset_px,
                               R.integer.default_timer_text_offset_px);

            mvTimerView.setTranslationY(-timerTextOffsetPx);
            mvQuickActionPanel.setTranslationY(-timerTextOffsetPx);
            mvCongratsText.setTranslationY(-timerTextOffsetPx);
        }

        // Main activity state is available, so hints can be set up and a
        // scramble generated (none will be generated if scrambles are not
        // enabled). Hints are only enabled for 3x3x3 puzzles and the puzzle
        // type may not be known in "onCreateView", so they are set up now.
        // Similarly, the scramble image height is different for different
        // puzzle types.
        final PuzzleType puzzleType = getMainState().getPuzzleType();

        setUpCrossHints(puzzleType);    // ... also sets "mShowCrossHints".
        setUpScrambleImage(puzzleType); // ... if enabled.

        // NOTE: There used to be a "lock" field that prevented the timer from
        // starting until the scramble was generated. However, this is now gone.
        // It is left to the discretion of the user to decide to start before
        // the solve is generated or to wait for the scramble. If the scrambles
        // are slow to generate, the user will not be forced to wait for them.
        if (Prefs.isScrambleEnabled()) {
            mvScrambleText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mvScrambleText.getTextSize() * Prefs.getScrambleTextScale());
            if (!Prefs.showScrambleImage()) {
                mvScrambleImg.setVisibility(View.GONE);
            }
        } else {
            mvScrambleText.setVisibility(View.GONE);
            mvScrambleImg.setVisibility(View.GONE);
            // If the scramble preference has just been disabled, there may be
            // a scramble lying around since before that change, so drop it.
            mUnsolvedScramble = null;
        }

        if (!Prefs.showSessionStats()) {
            mvStatsPanel.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume()");
        super.onResume();

        mvCrossHintsSlider.setPanelState(PanelState.HIDDEN);

        // Generate a scramble if scrambles are enabled and if no unsolved
        // scramble is available already (from "mUnsolvedScramble", possibly
        // restored from instance state). If there is an unsolved scramble, but
        // it has no image, then only the image will be generated (if generation
        // of images is enabled). If there is an unsolved scramble, but it has
        // the wrong puzzle type, it will be discarded and a new scramble (and
        // image) will be generated.
        generateScramble(getMainState().getPuzzleType());

        // Wake the puzzle timer. This will cause it to start sending
        // notifications to update the display, etc. It is placed into its
        // "sleep" state in "onPause()", as there is no point in updating the
        // UI if the timer display is not (fully) visible. Waking the timer
        // will cause a notification to be sent to "onTimerRestored".
        mTimer.wake();
    }

    @Override
    public void onPause() {
        if (DEBUG_ME) Log.d(TAG, "onPause()");
        super.onPause();

        // Put the timer into its "sleep" state. This will cause it to cease
        // sending notifications to update the display, etc., as there is no
        // point in refreshing the UI if the timer display is not (fully)
        // visible. Re-awaken it in "onResume()".
        mTimer.sleep();
    }

    /**
     * Saves the instance state of this fragment. The instance state is
     * primarily the current timer state. The state is restored in
     * {@link #onActivityCreated(Bundle)}, if the saved state is passed to
     * that method. It is typically saved just before {@link #onStop()} is
     * called.
     *
     * @param outState The bundle in which to save the instance state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState(" + outState + ')');
        super.onSaveInstanceState(outState);

        // FIXME: An app restart could occur if the device were restarted,
        // which would invalidate the measurement as the time base, i.e., the
        // system uptime, would be reset. Therefore, it is probably not
        // practical to support persisting the timer across app restarts.
        // However, it will (probably) survive simple cases, such as hitting
        // "Home" and returning to the app. Persisting the timer state for a
        // longer period is also viable if the timer is "STOPPED".
        if (mTimer != null) {
            outState.putParcelable(
                KEY_PUZZLE_TIMER_STATE, mTimer.onSaveInstanceState());
        }

        if (mUnsolvedScramble != null) {
            // NOTE: If there is a scramble image, it will not be saved.
            // Instead, it will be re-generated when the state is restored.
            outState.putParcelable(KEY_UNSOLVED_SCRAMBLE, mUnsolvedScramble);
        }
    }

    @Override
    public void onStop() {
        if (DEBUG_ME) Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()");

        mRecentStatistics = null;
        mVerifiedSolveID = Solve.NO_ID;

        mUnbinder.unbind();

        StatisticsCache.getInstance().unregisterObserver(this);
        unregisterReceiver(mUIInteractionReceiver);
        unregisterReceiver(mSolveDataChangedReceiver);

        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()");
        super.onDetach();
    }

    @Override
    public void onMainStateChanged(
            @NonNull MainState newMainState, @NonNull MainState oldMainState) {
        super.onMainStateChanged(newMainState, oldMainState); // For logging.

        // If the puzzle type changes, any displayed scramble and image will
        // become invalid. "generateScramble()" will determine what to do.
        generateScramble(getMainState().getPuzzleType()); // ... if enabled.
    }

    /**
     * Hides the sliding panel showing the scramble image, or stops the
     * chronometer, if either action is necessary.
     *
     * @return
     *     {@code true} if the "Back" button press was consumed to hide the
     *     scramble or stop the timer; or {@code false} if neither was
     *     necessary and the "Back" button press was ignored.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");

        if (isResumed()) {
            if (mTimer != null && mTimer.cancel()) {
                // "cancel()" (probably) had/will have an effect: consume event.
                return true;
            } else if (mvCrossHintsSlider != null
                    && mvCrossHintsSlider.getPanelState() != PanelState.HIDDEN
                    && mvCrossHintsSlider.getPanelState()
                       != PanelState.COLLAPSED) {
                mvCrossHintsSlider.setPanelState(PanelState.HIDDEN);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onTimerCue(@NonNull TimerCue cue) {
        // IMPORTANT: One or more calls to "onTimerCue()" are always "bracketed"
        // by calls to "onTimerSet", one when the timer is started (or restored)
        // and one when the timer is stopped (or cancelled). The state of the
        // UI is mostly managed from "onTimerSet", but there are a few
        // transitions and other actions that are managed from "onTimerCue".

        // TODO: If adding sound effects, it may be neater to create a separate
        // sound effects class that implements "OnTimerEventListener" and then
        // add an instance to the "PuzzleTimer" as another listener for cues.
        // For an on/off switch: just add/remove that listener.

        switch (cue) {
            case CUE_INSPECTION_HOLDING_FOR_START:
            case CUE_SOLVE_HOLDING_FOR_START:
                // The UI will not be changed until the hold period expires and
                // "CUE_INSPECTION_READY_TO_START" is notified.
                break;

            case CUE_SOLVE_READY_TO_START:
            case CUE_INSPECTION_READY_TO_START:
                showFullScreenTimer(true);
                break;

            case CUE_INSPECTION_STARTED:
            case CUE_INSPECTION_RESUMED:
            case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
            case CUE_INSPECTION_SOLVE_READY_TO_START:
                break;

            case CUE_INSPECTION_7S_REMAINING:
            case CUE_INSPECTION_3S_REMAINING:
            case CUE_INSPECTION_OVERRUN:
            case CUE_INSPECTION_TIME_OUT:
                // For now, nothing special is done for these cues (though
                // the timer display may do something if it wants). Perhaps
                // play an audible cue. NOTE: If playing audio, it might be
                // simpler to create a separate "SoundEffects" class that
                // implements "OnTimerEventListener" and "onTimerCue()". Then
                // add an instance to the "PuzzleTimer" as another listener.
                break;

            case CUE_SOLVE_STARTED:
                break;

            case CUE_CANCELLING:
                // FIXME: Show "toast" declaring that the effort has been
                // cancelled ***UNLESS*** the timer was never actually started.
                // Therefore, if the elapsed inspection time and/or elapsed
                // solve time (or whatever flag can communicate that state)
                // are zero, then the timer was cancelled during a "holding"
                // stage, so toast would just be confusing, as the user will
                // probably not consider a "too-short hold" to be announced as
                // as, "Timer cancelled!"
                break;

            case CUE_STOPPING:
                // "onTimerSet()" and "onSolveStop" (if not cancelled) will be
                // called after this cue is notified, so the work will happen
                // there.
                break;
        }
    }

    /**
     * <p>
     * Sets up the user-interface components of this fragment to match the state
     * of the puzzle timer. If the timer is stopped (not reset), the quick
     * action buttons that allow the result to be edited (usually to change the
     * penalties) are shown. If the timer is reset or running, those buttons
     * are not shown. If the timer is running, the fragment is displayed
     * "full screen" (in coordination with {@link TimerMainFragment}) and only
     * the timer display is shown. If not running (i.e., if stopped or reset),
     * the "full screen" display is reversed.
     * </p>
     * <p>
     * At the instant when the timer is stopped, {@link #onSolveStop(Solve)} is
     * called to allow the new {@code Solve} result to be saved to the database.
     * The save operation is performed on a background thread. Therefore, when
     * this {@code onTimerSet} method is called, the new solve may not yet be
     * saved and may have no assigned solve record ID. Until the ID is assigned,
     * the solve cannot be edited, as the ID is needed when updating the solve
     * record in the database after each edit. Therefore, while the quick-action
     * buttons are displayed when the timer is stopped, they will not be enabled
     * until notification is received (by a local broadcast receiver) that the
     * solve has been saved and has an ID. When that notification is received,
     * the broadcast receiver will notify the timer that the solve has changed
     * (it now has an ID), and the timer will store the new solve reference and
     * call back to this {@code onTimerSet} method. The FIXME: ?????
     * </p>
     * <p>
     * At any time, the solve managed by the
     * </p>
     * <p>
     * The display of the time value, penalties and final result is the
     * responsibility of the {@link TimerView}, which is notified of changes
     * via its implementation of this same call-back method.
     * </p>
     *
     * @param timerState FIXME: Finish this....
     */
    @Override
    public void onTimerSet(@NonNull TimerState timerState) {
        // --------------------------
        // IF THE TIMER IS STOPPED...
        // --------------------------
        //
        // If the timer is stopped (which does not include it being reset),
        // then display the result of a solve attempt and allow this result to
        // be edited with the "quick action" buttons. However, the solve must
        // first be verified to exist in the database. The timer should not be
        // shown full-screen.
        //
        // The solve will not exist in the database if it is in the process of
        // being saved (by a background task launched from "onSolveStop"), or
        // if it has been deleted. Until its existence has been verified, the
        // quick action buttons will be shown as disabled, as editing is unsafe.
        //
        // If the solve has no ID, then it is in the process of being saved, so
        // wait for "ACTION_ONE_SOLVE_ADDED" to be received. In the meantime,
        // keep the quick action buttons hidden. The timer has just stopped, so
        // it was not already showing the buttons while it was running. There is
        // no point in showing the buttons but disabling them; just wait until
        // the solve is saved and show and enable the buttons at the same time.
        //
        // If the solve has an ID, then it needs to be verified to exist before
        // the buttons are enabled. In this case, launch a verification task and
        // wait for "ACTION_SOLVE_VERIFIED" or "ACTION_SOLVE_NOT_VERIFIED" to
        // be received. Until verified, show the quick action buttons, but keep
        // then disabled. Hiding the buttons would not be appropriate, as the
        // buttons are probably already showing and hiding them while the verify
        // task runs would just appear to make them flicker.
        //
        // However, if "mVerifiedSolveID" matches the ID of the solve (and
        // neither are "Solve.NO_ID"), then verification has already completed,
        // the solve is known to exist in the database and it can be edited, so
        // show and enable the quick action buttons.
        //
        // ------------------------
        // IF THE TIMER IS RESET...
        // ------------------------
        //
        // If the timer is reset, then it has no solve to edit, so hide the
        // quick action buttons. The timer should not be shown full-screen.
        // Also hide any "Congratulations!" text or other adornments that may
        // have been showing before the solve was reset. For example, if a
        // solve is deleted, then the quick action buttons will be disabled
        // while the delete task is running (because the "mVerifiedSolveID"
        // will have been set to "Solve.NO_ID" before the task started), and
        // once that task is complete, the timer will be reset and the buttons
        // will be hidden.
        //
        // --------------------------
        // IF THE TIMER IS RUNNING...
        // --------------------------
        //
        // If the timer is running (per "TimerState.isRunning()"), then it
        // has no solve result that can be edited (yet). Hide the quick action
        // buttons and any other adornments. The timer should be shown
        // full-screen.
        //
        // ----------------------------------------
        // IF THE TIMER IS ... NONE OF THE ABOVE...
        // ----------------------------------------
        //
        // If the timer is not stopped, reset, or running, then it is in the
        // process of starting and is in a hold-to-start or ready-to-start
        // state. In these states, the quick action buttons should be hidden,
        // but the timer should not be shown full-screen until it starts proper.
        // That transition to full-screen can be handled by the appropriate
        // timer cue notified to "onTimerCue()" when ready-to-start.
        //
        // A timer is never restored/wakened into a hold-to-start or
        // ready-to-start state, so there is no need for "onTimerSet()" to try
        // to figure out if it is in one of those states and decide if the timer
        // should be displayed full-screen or not. If the timer is running
        // (i.e., it has gone past those states), then show it full-screen;
        // if not running, then the timer cue *will* be fired soon enough and
        // the full-screen transition will happen then, so do not show it
        // full-screen just yet.
        //

        if (timerState.isStopped()) {
            showFullScreenTimer(false);

            final Solve solve = timerState.getSolve();

            if (solve != null) {
                final long solveID = solve.getID();

                if (solveID == Solve.NO_ID) {
                    // Waiting for "ACTION_ONE_SOLVE_ADDED".
                    hideQuickActionButtons();
                } else if (solveID != mVerifiedSolveID) {
                    // The last verified solve had a different ID (or is set to
                    // "NO_ID") and this solve is not yet verified to exist.
                    // Verify now and wait for "ACTION_SOLVE_VERIFIED".
                    mVerifiedSolveID = Solve.NO_ID;
                    TwistyTimer.getDBHandler()
                               .verifySolveAndNotifyAsync(solveID);
                    showQuickActionButtons(solve, false); // Show disabled.
                } else {
                    showQuickActionButtons(solve, true);  // Show enabled.
                }
            } else {
                throw new IllegalStateException(
                    "BUG! Stopped timer is missing its Solve instance.");
            }
        } else if (timerState.isReset()) {
            showFullScreenTimer(false);
            hideQuickActionButtons();
            hideCongratulations();
        } else if (timerState.isRunning()) {
            showFullScreenTimer(true);
            hideQuickActionButtons();
            hideCongratulations();
        } else {
            // Timer is in a hold-to-start or ready-to-start state. Wait for
            // "onTimerCue()" before going full-screen.
            showFullScreenTimer(false);
            hideQuickActionButtons();
            hideCongratulations();
        }
    }

    @Override
    public void onTimerPenalty(@NonNull TimerState timerState) {
        // Do nothing. Only of interest to the "TimerView", at present.
    }

    @Override
    @NonNull
    public Solve onSolveStart() {
        if (DEBUG_ME) Log.d(TAG, "onSolveStart()");

        final MainState mainState = getMainState();
        final Solve newSolve = new Solve(
            mainState.getPuzzleType(), mainState.getSolveCategory(),
            consumeScramble(mainState.getPuzzleType())); // May be null.

        // Ensure that, if the timer is cancelled, the "previous" solve that may
        // then be restored will be re-verified by "onTimerSet()". This ensures
        // that changes made to its database record by any background tasks
        // completed before then will be detected properly at that time.
        mVerifiedSolveID = Solve.NO_ID;

        return newSolve;
    }

    @Override
    public void onSolveStop(@NonNull Solve solve) {
        if (DEBUG_ME) Log.d(TAG, "onSolveStop(" + solve + ')');

        // As "onSolveStop()" is called only once per solve attempt (i.e., it
        // is not called again if the solve is restored after a configuration
        // change, etc.), this is the place to proclaim the new solve to be a
        // record (if appropriate). There is no need to save it first. In fact,
        // if it were saved first and the loaded statistics were updated with
        // this solve (in response to "ACTION_ONE_SOLVE_ADDED"), it would no
        // longer be possible to tell if was a new best/worst record time; it
        // must be compared to the *previous* best/worst times from the
        // statistics *before* adding it to those statistics.
        if (!solve.getPenalties().hasDNF()) {
            proclaimRecordTimes(solve);
        }

        // The solve is saved *asynchronously*. "onTimerSet()" has been called,
        // but it did not enable the quick action buttons because the solve had
        // no assigned ID. On receiving "ACTION_ONE_SOLVE_ADDED", the solve get
        // its record ID, "onTimerSet()" will be called again, and it will
        // enable the buttons.
        TwistyTimer.getDBHandler().addSolveAndNotifyAsync(solve);
    }

    /**
     * <p>
     * Indicates if the timer is currently "busy". The timer is "busy" if it is
     * running, or if it the user is interacting with the timer and it is in a
     * hold-to-start or ready-to-start state. Conversely, it is not busy when it
     * is stopped or reset. The timer will not be reported as busy if it has not
     * yet been created.
     * </p>
     * <p>
     * If the timer is busy, the display should not be interrupted with the
     * results of background tasks, such as new scramble sequences or scramble
     * images. For example, if in a hold-to-start state, the scramble image for
     * the newly-pending solve attempt may be showing, so if a new scramble is
     * generated in the background, it would be confusing if that were to
     * change suddenly before the user lifts up the touch and starts the timer.
     * </p>
     *
     * @return {@code true} if the timer is busy, or {@code false} if it is not.
     */
    private boolean isTimerBusy() {
        // NOTE: "TimerState.isRunning()" is not the test used here, as that
        // excludes the hold-to-start and ready-to-start states.
        return mTimer != null
               && !mTimer.getTimerState().isReset()
               && !mTimer.getTimerState().isStopped();
    }

    /**
     * Proclaims a new all-time best or worst solve time, if the new solve
     * time sets a record. The first valid solve time will not set any
     * records; it is itself the best and worst time and only later times
     * will be compared to it. If the solve time is not greater than zero, or
     * if the solve is a DNF, the solve will be ignored and no new records
     * will be declared.
     *
     * @param solve The solve (time) to be tested.
     */
    private void proclaimRecordTimes(Solve solve) {
        // NOTE: The old approach did not check for PB/record solves until at
        // least 4 previous solves had been recorded for the *current
        // session*. This seemed a bit arbitrary. Perhaps it had to do with
        // waiting for the best and worst times to be loaded. If a user
        // records their *first* solve for the current session and it beats
        // the best time from *any* past session, it should be reported
        // *immediately*, not ignored just because the session has only
        // started. However, the limit should perhaps have been 4 previous
        // solves in the full history of all past and current sessions. If
        // this is the first ever session, then it would be annoying if each
        // of the first few times were reported as a record of some sort.
        // Therefore, do not report PB records until at least 4 previous
        // *non-DNF* times have been recorded in the database across all
        // sessions, including the current session.

        final long newTime = solve.getTime(); // Rounded, same as in statistics.

        if (solve.getPenalties().hasDNF() || newTime <= 0
                || mRecentStatistics == null
                || mRecentStatistics.getAllTimeNumSolves()
                   - mRecentStatistics.getAllTimeNumDNFSolves() < 4) {
            // Not a valid time, or there are no previous statistics, or
            // not enough previous times to make reporting meaningful (or
            // non-annoying), so cannot check for a new PB.
            return;
        }

        boolean isBestTime = false;

        if (Prefs.proclaimBestTime()) {
            final long previousBestTime
                = mRecentStatistics.getAllTimeBestTime();

            // If "previousBestTime" is a DNF or UNKNOWN, it will be less
            // than zero, so the new solve time cannot better (i.e., lower).
            if (newTime < previousBestTime ) {
                isBestTime = true;
                mvCongratsRipple.startRippleAnimation();
                mvCongratsText.setText(getString(R.string.personal_best_message,
                    // Not strictly a "result" time, but use that formatter.
                    formatResultTime(previousBestTime - newTime)));
                mvCongratsText.setVisibility(View.VISIBLE);

                // TODO: Explain why this is here. Does it not stop by itself?
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mvCongratsRipple != null) {
                            mvCongratsRipple.stopRippleAnimation();
                        }
                    }
                }, 2940);
            }
        }

        if (Prefs.proclaimWorstTime() && !isBestTime) {
            final long previousWorstTime
                = mRecentStatistics.getAllTimeWorstTime();

            // If "previousWorstTime" is a DNF or UNKNOWN, it will be less
            // than zero. Therefore, make sure it is at least greater than
            // zero before testing against the new time.
            if (previousWorstTime > 0 && newTime > previousWorstTime) {
                mvCongratsText.setText(
                    getString(R.string.personal_worst_message,
                        formatResultTime(newTime - previousWorstTime)));

                final int bgDrawableRes
                    = Prefs.getBoolean(R.string.pk_timer_bg_enabled,
                                       R.bool.default_timer_bg_enabled)
                        ? R.drawable.ic_emoticon_poop_white_18dp
                        : R.drawable.ic_emoticon_poop_black_18dp;

                mvCongratsText.setCompoundDrawablesWithIntrinsicBounds(
                    bgDrawableRes, 0, bgDrawableRes, 0);
                mvCongratsText.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Sets up the panel showing the hints for solving the "cross" after
     * applying the current scramble. This is only displayed for 3x3x3
     * puzzles and only if scrambles are enabled.
     *
     * @param puzzleType The currently-selected puzzle type.
     */
    private void setUpCrossHints(@NonNull PuzzleType puzzleType) {
        // "mShowCrossHints" field will be used later to determine if the
        // option to show hints should be displayed after a scramble is
        // generated.
        mShowCrossHints = Prefs.showCrossHints(puzzleType);

        if (mShowCrossHints) {
            mvCrossHintsPanel.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets up the scramble image (its size, in particular).
     *
     * @param puzzleType The currently-selected puzzle type.
     */
    private void setUpScrambleImage(@NonNull PuzzleType puzzleType) {
        // This set-up operation has to wait until the activity is attached
        // so the main state can be accessed to get the puzzle type. Call it
        // from "onActivityCreated".
        if (Prefs.showScrambleImage()) {
            if (Prefs.getBoolean(R.string.pk_advanced_timer_settings_enabled,
                    R.bool.default_advanced_timer_settings_enabled)) {
                // The preference value is a percentage, so convert that to a
                // multiplier. For example, 150% -> 1.5x.
                final float scrambleImageScale = Prefs.getScrambleImageScale();

                mvScrambleImg.getLayoutParams().width *= scrambleImageScale;
                mvScrambleImg.getLayoutParams().height
                    *= calculateScrambleImageHeightMultiplier(
                        scrambleImageScale, puzzleType);
            } else {
                mvScrambleImg.getLayoutParams().height
                    *= calculateScrambleImageHeightMultiplier(
                        1, puzzleType); // 1 == 100%
            }
        }
    }

    /**
     * Calculates scramble image height multiplier to respect aspect ratio.
     *
     * @param multiplier
     *     The height multiplier (must be the same multiplier as the width).
     * @param puzzleType
     *     The type of the selected puzzle.
     *
     * @return The scramble image height in pixels.
     */
    private float calculateScrambleImageHeightMultiplier(
            float multiplier, @NonNull PuzzleType puzzleType) {
        switch (puzzleType) {
            // 3 faces of the cube vertically divided by 4 faces horizontally
            // (it draws the cube like a cross)
            case TYPE_333:
            case TYPE_222:
            case TYPE_444:
            case TYPE_555:
            case TYPE_666:
            case TYPE_777:
            case TYPE_SKEWB: // "Skewb" one is the same as the NxNxN cubes
                return (multiplier / 4) * 3;

            case TYPE_CLOCK:
            case TYPE_MEGA:
                return (multiplier / 2);

            case TYPE_PYRA:
                // Just pythagoras. Height of an equilateral triangle
                return (float) (multiplier / Math.sqrt(1.25));

            //noinspection UnnecessaryDefault
            default:
            case TYPE_SQ_1:
                return multiplier;
        }
    }

    /**
     * Refreshes the display of the statistics. If this fragment has no view,
     * or if the given statistics are {@code null}, no update will be attempted.
     *
     * @param stats The updated statistics. These will not be modified.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onStatisticsUpdated(Statistics stats) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated(" + stats + ')');

        if (getView() == null) {
            // Must have arrived after "onDestroyView" was called: do nothing.
            return;
        }

        // Save these for later. The best and worst times can be retrieved and
        // compared to the next new solve time to be added via "addNewSolve".
        mRecentStatistics = stats; // May be null.

        if (stats == null) {
            return;
        }

        // "TIME_UNKNOWN" times will be formatted as "--", not "0.00". This is
        // useful when an average has yet to be calculated. For example, the
        // Ao50 value after only 49 solves are added. The values are returned
        // from "stats" already rounded appropriately.
        String sessionCount = String.format(
            Locale.getDefault(), "%,d", stats.getSessionNumSolves());
        String sessionMeanTime = formatAverageTime(stats.getSessionMeanTime());
        String sessionBestTime = formatResultTime(stats.getSessionBestTime());
        String sessionWorstTime = formatResultTime(stats.getSessionWorstTime());

        String sessionCurrentAvg5 = formatAverageTime(
            stats.getAverageOf(5, true).getCurrentAverage());
        String sessionCurrentAvg12 = formatAverageTime(
            stats.getAverageOf(12, true).getCurrentAverage());
        String sessionCurrentAvg50 = formatAverageTime(
            stats.getAverageOf(50, true).getCurrentAverage());
        String sessionCurrentAvg100 = formatAverageTime(
            stats.getAverageOf(100, true).getCurrentAverage());

        mvStatsTimesAvg.setText(
            sessionCurrentAvg5 + "\n"
            + sessionCurrentAvg12 + "\n"
            + sessionCurrentAvg50 + "\n"
            + sessionCurrentAvg100);

        mvStatsTimesMore.setText(
            sessionMeanTime + "\n"
            + sessionBestTime + "\n"
            + sessionWorstTime + "\n"
            + sessionCount);
    }

    private void showFullScreenTimer(boolean isFullScreen) {
        if (DEBUG_ME) Log.e(TAG, "showFullScreenTimer(" + isFullScreen + ')');

        // FIXME: Document that hiding a hidden tool bar or showing one that is
        // already showing will no longer cause any problems.

        if (isFullScreen) {
            // If full-screen mode is turned off, the other branch of this "if"
            // statement fires and "mIsRestoringToolBar" is set to "true".
            // However, if before the tool-bar is fully restored it is hidden
            // again (which often happens when "onTimerSet()" is trying to
            // initialise the correct UI state), then "ACTION_TOOLBAR_RESTORED"
            // will not be broadcast and "mIsRestoringToolBar" will remain set.
            // Therefore, it is necessary to reset it here to ensure that touch
            // events will not be ignored if the tool-bar is not fully restored.
            mIsRestoringToolBar = false;
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_HIDE_TOOLBAR);
            hideItems();
        } else {
            // "TimerMainFragment" hosts the tool-bar, so it will restore it.
            // However, it animates it back into view, so this fragment must do
            // nothing until it receives the "ACTION_TOOLBAR_RESTORED" intent
            // broadcast when that animation ends. While "mIsRestoringToolBar"
            // is true, touch events will not be relayed to the puzzle timer,
            // so the timer cannot be restarted, etc., until everything settles
            // down. That flag is cleared once that broadcast intent is received
            // (or if "showFullScreenTimer(false)" is called before the tool-bar
            // is fully restored, see above).
            mIsRestoringToolBar = true;
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SHOW_TOOLBAR);
            // "showItems()" is called on receiving "ACTION_TOOLBAR_RESTORED".
        }
    }

    private void showItems() {
        if (DEBUG_ME) Log.d(TAG, "showItems()");

        if (Prefs.isScrambleEnabled()) {
            mvScrambleText.setEnabled(true);
            mvScrambleText.setVisibility(View.VISIBLE);

            if (Prefs.showScrambleImage()) {
                mvScrambleImg.setEnabled(true);
                mvScrambleImg.setVisibility(View.VISIBLE);
            }

            if (mShowCrossHints) {
                mvCrossHintsPanel.setEnabled(true);
                mvCrossHintsPanel.setVisibility(View.VISIBLE);
            }
        }

        if (Prefs.showSessionStats()) {
            mvStatsPanel.setVisibility(View.VISIBLE);
        }
    }

    private void hideItems() {
        if (DEBUG_ME) Log.d(TAG, "hideItems()");

        // FIXME: It looks like the issue with the strange fade in/out is
        // caused by the "GONE" state of the views. Changing from "GONE" to
        // "INVISIBLE" maybe makes them appear for the purpose of fading them
        // out.

        if (Prefs.isScrambleEnabled()) {
            mvScrambleText.setEnabled(false);
            mvScrambleText.setVisibility(View.INVISIBLE);

            if (Prefs.showScrambleImage()) {
                mvScrambleImg.setEnabled(false);
                mvScrambleImg.setVisibility(View.INVISIBLE);
            }

            if (mShowCrossHints) {
                mvCrossHintsPanel.setEnabled(false);
                mvCrossHintsPanel.setVisibility(View.INVISIBLE);
            }
        }

        if (Prefs.showSessionStats()) {
            mvStatsPanel.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Hides the "congratulations" text and related content that is shown when
     * a new solve attempt beats a previous record.
     */
    private void hideCongratulations() {
        mvCongratsText.setVisibility(View.INVISIBLE);
        mvCongratsText.setCompoundDrawables(null, null, null, null);
    }

    /**
     * Hides the quick action buttons. This affects the visibility of the
     * whole panel that contains the buttons.
     */
    private void hideQuickActionButtons() {
        if (DEBUG_ME) Log.d(TAG, "hideQuickActionButtons()");

        // Use "INVISIBLE", not "GONE", otherwise a layout transition from
        // "INVISIBLE" to "GONE" might be animated visibly, which looks awful.
        mvQuickActionPanel.setVisibility(View.INVISIBLE);
    }

    /**
     * Shows the quick action buttons. The enabled state of the buttons (and
     * the text of some of the buttons) is matched to the state of the solve
     * and its penalties. If the quick action buttons are not enabled in the
     * preferences, they will not be shown (and will be hidden if already
     * showing when they should not be).
     *
     * @param solve
     *     The solve for which the quick action buttons should be presented.
     *     If the solve has no database record ID set (i.e., if the ID is set
     *     to {@link Solve#NO_ID}), the buttons will be shown, but they will
     *     not be enabled.
     * @param isVerified
     *     {@code true} if the solve has been verified to exist in the database
     *     and is a normal solve record (i.e., it is not a "fake" solve record
     *     used to define the name of a solve category); or {@code false} if
     *     the solve has not been so verified. If the solve is not verified,
     *     the buttons will be displayed, but they will not be enabled. When
     *     the solve is later verified, call this method again to enable the
     *     buttons as appropriate.
     */
    private void showQuickActionButtons(
            @NonNull Solve solve, boolean isVerified) {
        if (DEBUG_ME) Log.d(TAG,
            "showQuickActionButtons(" + solve + ", " + isVerified + ')');

        if (!Prefs.showQuickActions()) {
            hideQuickActionButtons();
            return;
        }

        final Penalties p = solve.getPenalties();

        // FIXME: There is a bit of an issue with the width of the "DNF" button
        // view: as it changes from "+DNF" to "-DNF", there is a slight change
        // to its width which makes the UI look a bit jumpy. It would probably
        // be too cramped if separate "+DNF" and "-DNF" buttons were added. Can
        // the width be fixed?
        if (p.canIncurPostStartPenalty(Penalty.DNF)) {
            // A post-start DNF can be incurred. Show "+DNF" and clear the tag.
            mvDNFButton.setText("+DNF");
            mvDNFButton.setEnabled(isVerified);
            mvDNFButton.setTag(null);
        } else if (p.canAnnulPostStartPenalty(Penalty.DNF)) {
            // This DNF can be annulled. Show "-DNF" and tag as "ANNUL_DNF", so
            // the click handler will know which way to "toggle" the DNF.
            mvDNFButton.setText("-DNF");
            mvDNFButton.setEnabled(isVerified);
            mvDNFButton.setTag(ANNUL_DNF);
        } else {
            // If a post-start DNF can neither be incurred nor annulled, it is
            // because there is a pre-start DNF prohibiting it. A pre-start DNF
            // cannot be annulled, as it was not added by the user. Show "-DNF",
            // but disable the button.
            // FIXME: Might be better to strike this out, or something like
            // that. Just disabling it might be confusing.
            mvDNFButton.setText("-DNF");
            mvDNFButton.setEnabled(false);
            mvDNFButton.setTag(ANNUL_DNF);
        }

        // Only *post*-start "+2" (and DNF) penalties can be edited. If there
        // is a pre-start "+2" caused by overrunning the inspection time, then
        // it cannot be annulled. (That would be cheating!)
        mvPlusTwoButton.setEnabled(
            isVerified && p.canIncurPostStartPenalty(Penalty.PLUS_TWO));
        mvMinusTwoButton.setEnabled(
            isVerified && p.canAnnulPostStartPenalty(Penalty.PLUS_TWO));

        // Until the solve is verified, it is not safe to allow it to be
        // deleted or have its comment changed.
        mvDeleteButton.setEnabled(isVerified);
        mvCommentButton.setEnabled(isVerified);

        // Make the buttons *look* disabled if they are disabled.
        mvDNFButton.setAlpha(mvDNFButton.isEnabled() ? 1f : 0.5f);
        mvPlusTwoButton.setAlpha(mvPlusTwoButton.isEnabled() ? 1f : 0.5f);
        mvMinusTwoButton.setAlpha(mvMinusTwoButton.isEnabled() ? 1f : 0.5f);
        mvDeleteButton.setAlpha(mvDeleteButton.isEnabled() ? 1f : 0.5f);
        mvCommentButton.setAlpha(mvCommentButton.isEnabled() ? 1f : 0.5f);

        mvQuickActionPanel.setVisibility(View.VISIBLE);
    }

    /**
     * Consumes the unsolved scramble, if one is available, and returns its
     * scramble sequence for use in a new solve attempt. If scramble generation
     * is enabled, a new scramble will be generated in the background to replace
     * the consumed scramble, or to provide a scramble if none was available.
     * That next new scramble will become available shortly after.
     *
     * @param puzzleType
     *     The puzzle type for which an unsolved scramble sequence is required.
     *
     * @return
     *     An available, unsolved scramble sequence for the given puzzle type;
     *     or {@code null} if no unsolved scramble sequence is available
     *     immediately for that puzzle type, or if generation of scrambles is
     *     disabled.
     */
    private String consumeScramble(@NonNull PuzzleType puzzleType) {
        if (Prefs.isScrambleEnabled()) {
            final ScrambleData unsolvedScramble = mUnsolvedScramble;

            // "Consume" the unsolved scramble for the current solve attempt
            // and generate a new one that will be available for use by a later
            // attempt. Assume that the puzzle type for the later attempt will
            // be the same, as it *probably* will be.
            mUnsolvedScramble = null;
            generateScramble(puzzleType);

            if (unsolvedScramble != null
                    && unsolvedScramble.puzzleType == puzzleType) {
                return unsolvedScramble.scramble;
            }
        }

        return null;
    }

    /**
     * <p>
     * Generates a new scramble <i>if scrambles are enabled</i> and if there is
     * no suitable unused scramble already generated. If scrambles are not
     * enabled, no action will be taken.
     * </p>
     * <p>
     * If needed, a new scramble will be generated on a background thread by
     * the {@code ScrambleLoader}. {@link #onScrambleGenerated(ScrambleData)}
     * will be called when that generation task is complete. A scramble image
     * can be generated at that time, if required.
     * </p>
     * <p>
     * If an unsolved scramble sequence is already available for the given
     * puzzle type, it will not be necessary to generate a new sequence. If the
     * unsolved scramble does not match the required puzzle type, it will be
     * cleared and a new scramble will be generated. If the unsolved scramble
     * is available and is suitable, but it does not have any corresponding
     * scramble image, an image will be generated for it. In the latter case,
     * see {@link #generateScrambleImage(ScrambleData)} for details.
     * </p>
     *
     * @param puzzleType
     *     The puzzle type for which a new scramble is required.
     *
     * @return
     *     {@code true} if a new scramble will be generated; or {@code false}
     *     if scrambles are not enabled, or if an unsolved scramble is available
     *     for the puzzle type, so no scramble will be generated. If a scramble
     *     image needs to be generated, it will not affect this returned value.
     */
    boolean generateScramble(@NonNull PuzzleType puzzleType) {
        if (DEBUG_ME) Log.d(TAG, "generateScramble(puzzleType="
                                 + puzzleType.typeName() + ')');

        // NOTE: This is the same behaviour as before: scrambles are either
        // enabled and generated automatically when required, or they are
        // disabled and never generated---even if the *visible* scramble button
        // on the tool-bar is pressed! TODO: That probably needs to change.
        if (Prefs.isScrambleEnabled()) {
            if (mUnsolvedScramble == null
                    || mUnsolvedScramble.puzzleType != puzzleType) {
                // In case puzzle type is wrong. This prevents any new solve
                // from using an old unused scramble for the wrong puzzle type
                // and also allows "onScrambleGenerated()" to test if another
                // scramble has been generated and delivered while the task was
                // running in the background, as it will no longer be null.
                mUnsolvedScramble = null;

                TTIntent.broadcastNewScrambleRequest(puzzleType);

                if (mShowCrossHints) {
                    mvCrossHintsSlider.setPanelState(PanelState.HIDDEN);
                }

                mvScrambleText.setText(R.string.generating_scramble);
                mvScrambleText.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, 0, 0);
                mvScrambleText.setClickable(false);
                mvScrambleImg.setVisibility(View.INVISIBLE);

                if (!isTimerBusy()) {
                    // Scrambles and images can be generated in the background
                    // while the timer is "busy". In that case, the progress
                    // spinner should not be displayed, as it would intrude on
                    // the display of the timer.
                    mvScrambleProgress.setVisibility(View.VISIBLE);
                }

                return true;

            } else {
                // A suitable scramble sequence is already available. Make sure
                // it is visible. It may not be visible if it is being restored
                // from instance state after a configuration change that has
                // reset the views to their default state.
                showScrambleSequence(mUnsolvedScramble.scramble);
                generateScrambleImage(mUnsolvedScramble); // If not available.
            }
        }

        return false;
    }

    /**
     * Generates a new scramble image <i>if scramble images are enabled</i> and
     * if the scramble data does not already hold an image for its scramble
     * sequence. The caller does not need to check if scramble images are
     * enabled before calling this method. The scramble image will be generated
     * on a background thread by the {@link ScrambleImageLoader}.
     * {@link #onScrambleImageGenerated(ScrambleData)} will be called when that
     * generation task is complete.
     *
     * @param scrambleData
     *     The scramble sequence and puzzle type for which a new scramble
     *     image is required.
     *
     * @return
     *     {@code true} if a new scramble image will be generated; or
     *     {@code false} if scramble images are not enabled, or if the scramble
     *     data already includes an image, so no image will be generated.
     */
    public boolean generateScrambleImage(@NonNull ScrambleData scrambleData) {
        if (DEBUG_ME) Log.d(TAG, "generateScrambleImage(" + scrambleData + ')');

        if (Prefs.showScrambleImage()) {
            if (scrambleData.image == null) {
                TTIntent.broadcastNewScrambleImageRequest(scrambleData);

                // The scramble image view is hidden by "generateScramble()"
                // and it will remain hidden until the image is generated.
                if (!isTimerBusy()) {
                    // Scrambles and images can be generated in the background
                    // while the timer is running. In that case, the progress
                    // spinner should not be displayed.
                    mvScrambleProgress.setVisibility(View.VISIBLE);
                }
                return true;
            } else {
                // A suitable scramble image is already available. Make sure
                // it is visible. It may not be visible if it is being restored
                // from instance state after a configuration change that has
                // reset the views to their default state.
                showScrambleImage(scrambleData.image);
            }
        }

        // Not enabled or already generated.
        return false;
    }

    /**
     * Notifies this fragment that the {@link ScrambleLoader} has completed
     * the generation of a new scramble sequence. The sequence may now be
     * displayed and, if required, a corresponding image can be generated. If
     * an unsolved scramble sequence is already available for the current puzzle
     * type, or if this new scramble no loner matches the current puzzle type,
     * this new scramble will be discarded.
     *
     * @param scrambleData
     *     The scramble data describing the generated scramble sequence and
     *     the puzzle type for which it was generated.
     */
    private void onScrambleGenerated(final ScrambleData scrambleData) {
        if (DEBUG_ME) Log.d(TAG, "onScrambleGenerated(" + scrambleData + ')');

        if (!isAdded() || getView() == null) {
            if (DEBUG_ME) Log.d(TAG,
                "  Cannot display scramble! Not attached or no view.");
            // May have received the data too late for it to be displayed (or
            // too early).
            return;
        }

        final MainState mainState = getMainState();

        if (scrambleData.puzzleType != mainState.getPuzzleType()) {
            if (DEBUG_ME) Log.d(TAG,
                "  New scramble is out-of-date: puzzle type has changed.");
            return;
        }

        // "mUnsolvedScramble" is set to null before a scramble is generated.
        // If it is not still null, investigate....
        if (mUnsolvedScramble != null
                && mUnsolvedScramble.puzzleType == mainState.getPuzzleType()) {
            // The new scramble is superfluous. Perhaps two or more scrambles
            // were generated at about the same time and another finished first.
            // Keep that other one, as there may already be a task running to
            // generate its corresponding image.
            if (DEBUG_ME) Log.d(TAG,
                "  New scramble is redundant: another is available.");
            return;
        }

        mUnsolvedScramble = scrambleData;

        if (!generateScrambleImage(scrambleData)) {
            // No need to keep showing the progress spinner, as no image will
            // be generated (or one is already generated).
            mvScrambleProgress.setVisibility(View.GONE);
        }

        showScrambleSequence(scrambleData.scramble);
    }

    /**
     * Notifies this fragment that the {@link ScrambleImageLoader} has completed
     * the generation of a new scramble image for a scramble sequence. The image
     * may now be displayed. If the scramble sequence identified for the image
     * does not match the currently unsolved scramble sequence, the image will
     * be discarded. This may occur if the scramble sequence is consumed for use
     * in a new solve attempt before the image generation task for that scramble
     * sequence has finished.
     *
     * @param scrambleDataWithImage
     *     The scramble data including the newly-generated scramble image and
     *     the scramble sequence and the puzzle type for which it was generated.
     */
    private void onScrambleImageGenerated(ScrambleData scrambleDataWithImage) {
        if (DEBUG_ME) Log.d(TAG,
            "onScrambleImageGenerated(" + scrambleDataWithImage + ')');

        if (!isAdded() || getView() == null) {
            if (DEBUG_ME) Log.d(TAG,
                "  Cannot display scramble image! Not attached or no view.");
            // May have received the data too late for it to be displayed (or
            // too early).
            return;
        }

        if (mUnsolvedScramble != null
                && mUnsolvedScramble.scramble.equals(
                        scrambleDataWithImage.scramble)
                && mUnsolvedScramble.puzzleType
                        == scrambleDataWithImage.puzzleType) {
            // Replace the unsolved scramble instance with this new instance
            // that contains the image. The existence of the image needs to be
            // known for proper management of this fragment's instance state.
            mUnsolvedScramble = scrambleDataWithImage;

            showScrambleImage(scrambleDataWithImage.image);
        } // else this is not the image you are looking for.
    }

    /**
     * Shows a scramble sequence. If scrambles are disabled, or if the sequence
     * is {@code null}, nothing is changed. If the timer is currently busy, the
     * new scramble sequence will be set, but it will not be revealed.
     *
     * @param scramble The scramble sequence to be displayed.
     */
    private void showScrambleSequence(final String scramble) {
        if (!Prefs.isScrambleEnabled() || scramble == null) {
            return;
        }

        mvScrambleText.setVisibility(View.INVISIBLE);
        mvScrambleText.setText(scramble);
        mvScrambleText.post(new Runnable() {
            @Override
            public void run() {
                if (mvScrambleText == null) {
                    // Runnable was executed after unbinding the views, perhaps.
                    return;
                }

                Rect scrambleRect = new Rect(
                    mvScrambleText.getLeft(), mvScrambleText.getTop(),
                    mvScrambleText.getRight(), mvScrambleText.getBottom());
                Rect timerRect = new Rect(
                    mvTimerView.getLeft(), mvTimerView.getTop(),
                    mvTimerView.getRight(), mvTimerView.getBottom());
                Rect congratsRect = new Rect(
                    mvCongratsText.getLeft(), mvCongratsText.getTop(),
                    mvCongratsText.getRight(), mvCongratsText.getBottom());

                if (Rect.intersects(scrambleRect, timerRect)
                        || (mvCongratsText.getVisibility() == View.VISIBLE
                            && Rect.intersects(scrambleRect, congratsRect))) {
                    mvScrambleText.setClickable(true);
                    mvScrambleText.setText(R.string.scramble_text_tap_hint);
                    mvScrambleText.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_dice_white_24dp, 0, 0, 0);
                    mvScrambleText.setOnClickListener(
                        new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mvCrossHintsText.setText(scramble);
                            mvCrossHintsText.setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                mvScrambleText.getTextSize());
                            mvCrossHintsText.setGravity(Gravity.CENTER);
                            mvCrossHintsProgress.setVisibility(View.GONE);
                            mvCrossHintsProgressText.setVisibility(View.GONE);
                            mvCrossHintsSlider
                                .setPanelState(PanelState.EXPANDED);
                        }
                    });
                } else {
                    mvScrambleText.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, 0, 0);
                    mvScrambleText.setClickable(false);
                }

                if (!isTimerBusy()) {
                    // Scrambles and images can be generated in the background
                    // while the timer is running. In that case, the scramble
                    // text should not be displayed yet.
                    mvScrambleText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Shows a scramble image. If scramble images are disabled, or if the
     * sequence is {@code null}, nothing is changed. If the timer is currently
     * busy, the new scramble image will be set, but it will not be revealed.
     *
     * @param scrambleImage The scramble image to be displayed.
     */
    private void showScrambleImage(final Drawable scrambleImage) {
        if (!Prefs.showScrambleImage() || scrambleImage == null) {
            return;
        }

        // Making the image invisible and then making it visible from a posted
        // "Runnable" makes it fade in when it is ready, or when it is restored
        // after a configuration change. The effect matches that for the
        // scramble sequence text, so it looks better this way. The view is set
        // "invisible" by default in the layout XML.
        mvScrambleProgress.setVisibility(View.INVISIBLE);
        mvScrambleImg.setVisibility(View.INVISIBLE);
        mvScrambleImg.post(new Runnable() {
            @Override
            public void run() {
                if (mvScrambleImg == null) {
                    // Runnable was executed after unbinding the views, perhaps.
                    return;
                }

                mvScrambleImg.setImageDrawable(scrambleImage);
                mvBigScrambleImg.setImageDrawable(scrambleImage);

                if (!isTimerBusy()) {
                    mvScrambleImg.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private class GetOptimalCross
            extends AsyncTask<ScrambleData, Void, String> {
        @Override
        protected String doInBackground(ScrambleData... scrambles) {
            // Increase the thread priority, as the user is waiting impatiently
            // for the "tip" to appear. The default priority is probably the
            // "background" priority, which is set by "AsyncTask". This priority
            // may be subject to special handling, such as restricting the
            // thread to no more than 5-10% of the CPU if any other threads are
            // busy. However, this task is part of the foreground application,
            // so it should not have to compete with background apps for CPU
            // time. Therefore, keep the priority low enough to avoid stalling
            // the UI, but just high enough to keep this thread out of the
            // specially restricted "background" group. Use the
            // "android.os.Process" methods to set the Linux thread priorities,
            // as those are what matter.
            final int threadID = Process.myTid();
            final int savedThreadPriority = Process.getThreadPriority(threadID);

            Process.setThreadPriority(threadID,
                Process.THREAD_PRIORITY_BACKGROUND
                + Process.THREAD_PRIORITY_MORE_FAVORABLE);

            try {
                final ScrambleData scrambleData = scrambles[0];
                String tip = new RubiksCubeOptimalCross(
                    getString(R.string.optimal_cross))
                        .getTip(scrambleData.scramble);

                if (Prefs.showXCrossHints(scrambleData.puzzleType)) {
                    tip += "\n\n";
                    tip += new RubiksCubeOptimalXCross(
                        getString(R.string.optimal_x_cross))
                            .getTip(scrambleData.scramble);
                }

                return tip;
            } finally {
                // The thread on which this task is running may be returned
                // to a pool and used again for a different task. Therefore,
                // restore the thread priority when done.
                //
                // The inspection warning about throwing an exception inside
                // a "finally" block is suppressed: "threadID" cannot refer
                // to a thread that does not exist, as it is the ID of *this*
                // thread from "Process .myTid()" (above).
                //noinspection ThrowFromFinallyBlock
                Process.setThreadPriority(threadID, savedThreadPriority);
            }
        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);

            // Do not intrude on the timer if it is "busy".
            if (mvCrossHintsProgressText != null && mvCrossHintsText != null
                    && !isTimerBusy()) {
                mvCrossHintsText.setText(text);
                mvCrossHintsText.setVisibility(View.VISIBLE);

                mvCrossHintsProgress.setVisibility(View.GONE);
                mvCrossHintsProgressText.setVisibility(View.GONE);
            }
        }
    }

    private void zoomScrambleImage(
            final View smallImg, final View bigImg, View boundary,
            final int animationDuration) {
        // If there's an animation in progress, cancel it immediately and
        // proceed with this one.
        if (mScrambleImgAnimator != null) {
            mScrambleImgAnimator.cancel();
        }

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the
        // thumbnail, and the final bounds are the global visible rectangle
        // of the container view. Also set the container view's offset as the
        // origin for the bounds, since that's the origin for the positioning
        // animation properties (X, Y).
        smallImg.getGlobalVisibleRect(startBounds);
        boundary.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(- globalOffset.x, - globalOffset.y);
        finalBounds.offset(- globalOffset.x, - globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        final float startScale;

        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail. The default layout transition animations need to be
        // suspended while the visibility is changed.
        smallImg.setAlpha(0f);

        final ViewGroup parent = (ViewGroup) bigImg.getParent();
        final LayoutTransition lt = parent.getLayoutTransition();

        parent.setLayoutTransition(null);
        bigImg.setVisibility(View.VISIBLE);
        parent.setLayoutTransition(lt);

        // Set the pivot point for SCALE_X and SCALE_Y transformations to the
        // top-left corner of the zoomed-in view (the default is the center
        // of the view).
        bigImg.setPivotX(0f);
        bigImg.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        final AnimatorSet growAnim = new AnimatorSet();

        growAnim
            .play(ObjectAnimator.ofFloat(
                bigImg, View.X, startBounds.left, finalBounds.left))
            .with(ObjectAnimator.ofFloat(
                bigImg, View.Y, startBounds.top, finalBounds.top))
            .with(ObjectAnimator.ofFloat(bigImg, View.SCALE_X, startScale, 1f))
            .with(ObjectAnimator.ofFloat(bigImg, View.SCALE_Y, startScale, 1f));

        growAnim.setDuration(animationDuration);
        growAnim.setInterpolator(new DecelerateInterpolator());
        growAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // "onAnimationEnd" is also called after a "cancel()".
                mScrambleImgAnimator = null;
            }
        });
        growAnim.start();

        mScrambleImgAnimator = growAnim;

        // Upon clicking the zoomed-in image, it should zoom back down to the
        // original bounds and show the thumbnail instead of the big,
        // expanded image.
        bigImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mScrambleImgAnimator != null) {
                    mScrambleImgAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                final AnimatorSet shrinkAnim = new AnimatorSet();

                shrinkAnim
                    .play(ObjectAnimator.ofFloat(
                        bigImg, View.X, startBounds.left))
                    .with(ObjectAnimator.ofFloat(
                        bigImg, View.Y, startBounds.top))
                    .with(ObjectAnimator.ofFloat(
                        bigImg, View.SCALE_X, startScale))
                    .with(ObjectAnimator.ofFloat(
                        bigImg, View.SCALE_Y, startScale));

                shrinkAnim.setDuration(animationDuration);
                shrinkAnim.setInterpolator(new DecelerateInterpolator());
                shrinkAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // "onAnimationEnd" is also called after a "cancel()".
                        smallImg.setAlpha(1f);
                        bigImg.setVisibility(View.GONE);
                        mScrambleImgAnimator = null;
                    }
                });

                shrinkAnim.start();
                mScrambleImgAnimator = shrinkAnim;
            }
        });
    }
}
