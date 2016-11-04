package com.aricneto.twistytimer.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
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
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.scramble.ScrambleData;
import com.aricneto.twistytimer.scramble.ScrambleImageData;
import com.aricneto.twistytimer.scramble.ScrambleImageLoader;
import com.aricneto.twistytimer.scramble.ScrambleLoader;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalCross;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalXCross;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.timer.OnTimerEventListener;
import com.aricneto.twistytimer.timer.OnTimerEventLogger;
import com.aricneto.twistytimer.timer.PuzzleTimer;
import com.aricneto.twistytimer.timer.SolveAttemptHandler;
import com.aricneto.twistytimer.timer.TimerCue;
import com.aricneto.twistytimer.timer.TimerState;
import com.aricneto.twistytimer.timer.TimerWidget;
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
import static com.aricneto.twistytimer.utils.TimeUtils.formatTimeStatistic;

public class TimerFragment extends BaseMainFragment
        implements OnBackPressedInFragmentListener,
                   StatisticsCache.StatisticsObserver,
                   OnTimerEventListener, SolveAttemptHandler {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

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
     * The key used to identify the saved instance state of the fragment
     * state field.
     */
    private static final String KEY_FRAGMENT_STATE
        = KEY_PREFIX + "fragmentState";

    /**
     * The key used to identify the saved instance state of the puzzle timer.
     */
    private static final String KEY_PUZZLE_TIMER_STATE
        = KEY_PREFIX + "puzzleTimer";

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
    @BindView(R.id.timer)             TimerWidget mvTimerWidget;
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
    @BindView(R.id.cross_hints_panel) View                 mvCrossHintsPanel;
    @BindView(R.id.hintText)          TextView             mvCrossHintsText;
    @BindView(R.id.hintProgress)      View                 mvCrossHintsProgress;
    @BindView(R.id.hintProgressText)  TextView             mvCrossHintsProgressText;

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
     * An enumeration of the different states of this fragment.
     */
    private enum State {

        // FIXME: Probably only need three states, as "onTimerSet" is notified
        // at key points an can be used to set up the UI appropriately. Either
        // the timer is running, or it is not. If it is not running, then either
        // there is a solve that can be edited, or there is not. All of this
        // can probably be determined from the timer state, so is this enum
        // needed at all?
        //
        // FIXME: IMPORTANT: Not having to deal with this separate state field
        // would make things easier, as there would be no way for the state of
        // the timer to be inconsistent with the state of the fragment.

        /**
         * There is no current solve available to be edited and the timer is
         * stopped. The timer must be in its reset state and showing "0.00"
         * (or similar).
         */
        NO_SOLVE,

        /**
         * The user has started to interact with the timer and the timer
         * <i>may</i> start soon. The timer is still stopped, but should
         * present the appearance of being ready to start by showing (and
         * holding) either the inspection time or the "0.00" solve time (if
         * inspection is disabled). However, the timer will not yet be
         * presented "full-screen". If the start is aborted (e.g., if there
         * is a hold-to-start action and the "hold" is for too short a
         * duration), then the state will return to {@link #NO_SOLVE} or
         * {@link #SOLVE_IN_EDIT}, whichever is appropriate. This state may
         * be skipped if the hold-to-start behaviour is not enabled for the
         * solve timer, or if there is an inspection period before starting
         * the solve timer (as there is no hold-to-start action before
         * inspection).
         */
        TIMER_PENDING,

        /**
         * The user has interacted with the timer and the timer <i>will</i>
         * start soon. The timer is still stopped, but should present the
         * appearance of being ready to start by showing (and holding) either
         * the inspection time or the "0.00" solve time (if inspection is
         * disabled). Any hold-to-start period that was pending completion
         * The next user action will (almost certainly) start the timer
         *
         * If the start is aborted (e.g., if there is a
         * hold-to-start action and the "hold" is for too short a duration), then the state will
         * return to {@link #NO_SOLVE} or {@link #SOLVE_IN_EDIT}, whichever is appropriate.
         */
        TIMER_READY,

        /**
         * The timer is running. The timer is running when the inspection
         * countdown starts, or, if inspection is not enabled, when the solve
         * timer starts. It ceases running if the inspection countdown times
         * out, the solve timer is stopped, or the user cancels the solve
         * attempt (usually with the "Back" button). The next state will be
         * {@link #NO_SOLVE} if the timer was cancelled and there was no
         * previous solve to restore, or will be {@link #SOLVE_IN_EDIT} if
         * there is a new solve reported, or if the timer is cancelled and
         * the previous solve restored.
         */
        // FIXME: Is there any need for a separation of "TIMER_READY" and
        // "TIMER_RUNNING"?
        TIMER_RUNNING,

        /**
         * There is a current solve available and edit controls can be
         * presented. The timer is stopped. If the timer was cancelled after
         * it was started, this will be the same solve that was "in edit"
         * before the timer was started. If the timer ran to completion, this
         * will be a new solve. In either case, the solve will already have
         * been saved to the database. If the solve is deleted, the state
         * will transition to {@link #NO_SOLVE}. If the user starts to
         * interact with the timer, the state will transition to
         * {@link #TIMER_PENDING}.
         */
        SOLVE_IN_EDIT;

        // FIXME: What about the brief period between stopping the timer and
        // waiting for the DB operation to complete? Would it be better,
        // perhaps, to let that remain synchronous until the new, first-draft
        // FSM layer is bedded in?

        /**
         * <p>
         * Indicates if the timer is in an "active" state. In an "active"
         * state, the user is interacting with the timer and it is in a state
         * that can be cancelled. If cancelled, it will return to an inactive
         * state. This may be used to determine if a "Back" button press
         * event should be consumed to cancel the timer. If not active, such
         * an event should be passed to a different component or ignored.
         * </p>
         * <p>
         * When active, the timer is not necessarily running or being shown
         * full-screen. To test if the timer is running, simply check if the
         * state is {@link #TIMER_RUNNING}. To test if the timer is being
         * shown full-screen, see {@link #isTimerDominant()}.
         * </p>
         *
         * @return
         *     {@code true} if the timer is active; or {@code false} if it is
         *     inactive.
         */
        public boolean isTimerActive() {
            return this == TIMER_PENDING
                   || this == TIMER_READY
                   || this == TIMER_RUNNING;
        }

        /**
         * <p>
         * Indicates if the timer is the dominant user-interface element.
         * While the timer is dominant (or "full screen"), defer updates to
         * other user-interface elements, so that they do not intrude on the
         * dominant presentation of the timer.
         * </p>
         * <p>
         * For example, when the timer is running, it will be dominant. If
         * the scramble generator has been running in the background and
         * completes its task, it must not intrude on the timer by displaying
         * its scramble sequence and scramble image. Instead, these should be
         * displayed when the timer is no longer dominant, i.e., when it is
         * stopped or cancelled.
         * </p>
         *
         * @return
         *     {@code true} if the timer is dominant (or "full screen");
         *     or {@code false} if
         */
        public boolean isTimerDominant() {
            return this == TIMER_READY || this == TIMER_RUNNING;
        }
    }

    /**
     * The current "state" of this fragment's finite-state machine that it
     * uses to track the current state of the user interface and the
     * currently-presented solve.
     */
    // NOTE: Persisted as part of the instance state of this fragment.
    private State mState = State.NO_SOLVE;

    /**
     * The puzzle timer that manages the inspection countdown and solve
     * timing. This timer object does not have any user interface; it
     * notifies this fragment via call-back methods when the user interface
     * needs to be updated.
     */
    // NOTE: Persisted as part of the instance state of this fragment.
    private PuzzleTimer mTimer;

    // Locks the chronometer so it cannot start before a scramble sequence is
    // generated (if one is being generated), or so it does not respond to
    // touche events just after it has been stopped and the tool-bar and
    // other views are being restored.
    private boolean mIsGeneratingScramble = true; // Assume until confirmed.
    private boolean mIsRestoringToolBar   = false;

    /**
     * The next available scramble that has been generated, but has not yet
     * been solved. This is updated each time a new scramble is generated.
     * When the timer starts, this is moved to {@link #mActiveSolve}
     * (allowing it to be saved when the timer stops) and generation of a new
     * scramble begins in the background that will update this field before
     * the next solve. The scramble data also records the puzzle type for
     * which it was generated.
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

    private final TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_TOOLBAR_RESTORED:
                    showItems(mState == State.SOLVE_IN_EDIT);
                    mIsRestoringToolBar = false;
                    break;

                case ACTION_GENERATE_SCRAMBLE:
                    // Action here is not in the same intent category that
                    // "ScrambleLoader" is monitoring. Relay this request
                    // from the tool-bar button to that loader. Safe to
                    // access state, as "isAdded()" is a given.
                    generateScramble(getMainState().getPuzzleType());
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

        final View.OnClickListener buttonClickListener
            = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
                final MainState mainState = getMainState();
                final TimerState timerState = mTimer.getTimerState();

                // On most of these edits to the current solve, the
                // Statistics and ChartStatistics need to be updated to
                // reflect the change. It would probably be too complicated
                // to add facilities to "AverageCalculator" to handle
                // modification of the last added time or an "undo" facility
                // and then to integrate that into the loaders. Therefore, a
                // full reload will probably be required.

                // FIXME: What if the solve is deleted from the
                // "TimerListFragment"? It will also need to be deleted here,
                // otherwise the updates will fail.

                switch (view.getId()) {
                    case R.id.button_delete:
                        new MaterialDialog.Builder(getContext())
                                .content(R.string.delete_dialog_confirmation_title)
                                .positiveText(R.string.delete_dialog_confirmation_button)
                                .negativeText(R.string.delete_dialog_cancel_button)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog,
                                                        @NonNull DialogAction which) {
                                        mTimer.reset(); // Reset to "0.00".
                                        mvCongratsText.setVisibility(View.GONE);
                                        dbHandler.deleteSolveAndNotifyAsync(mSavedSolve, mainState);
                                        hideQuickActionButtons(); // Deleted, so nothing to edit.
                                    }
                                })
                                .show();
                        break;

                    case R.id.button_dnf:
    //                    if (mTimer.getTimerState().incurPostStartPenalty(Penalty.DNF)) {
    //                        mSavedSolve.applyPenalty(Penalty.DNF);
    //                        dbHandler.updateSolveAndNotifyAsync(mSavedSolve, mainState);
    //                        showQuickActionButtons(mSavedSolve);
    //                    }
                        break;

                    case R.id.button_plus_two:
                        // Do not allow a second "+2" to be added. (TODO: In reality, multiple "+2"
                        // penalties could be incurred and should accumulate.)
    //                    if (mCurrentPenalty != Penalty.PLUS_TWO) {
    //                        mTimer.getTimerState().incurPostStartPenalty(Penalty.PLUS_TWO);
    //                        mSavedSolve.applyPenalty(Penalty.PLUS_TWO);
    //                        dbHandler.updateSolveAndNotifyAsync(mSavedSolve, mainState);
    //                        showQuickActionButtons(mSavedSolve);
    //                    }
                        break;

                    case R.id.button_comment:
                        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                                .title(R.string.add_comment)
                                .input("", "", new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(@NonNull MaterialDialog dialog,
                                                        CharSequence input) {
                                        mSavedSolve.setComment(input.toString());
                                        dbHandler.updateSolveAndNotifyAsync(
                                                mSavedSolve, mainState);
                                        Toast.makeText(getContext(),
                                                getString(R.string.added_comment),
                                                Toast.LENGTH_SHORT).show();
                                        showQuickActionButtons(mSavedSolve);
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
                        break;

                    case R.id.cross_hints_panel:
                        break;
                }
            }
        };

        mvDeleteButton.setOnClickListener(buttonClickListener);
        mvDNFButton.setOnClickListener(buttonClickListener);
        mvPlusTwoButton.setOnClickListener(buttonClickListener);
        mvMinusTwoButton.setOnClickListener(buttonClickListener);
        mvCommentButton.setOnClickListener(buttonClickListener);

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

        // Relay (filtered) touch events from the "proxy" that accepts
        // touches through to the "PuzzleTimer" (not a view). The
        // "TimerWidget" view itself is not touch sensitive.
        final float touchMargin
            = getResources().getDimension(R.dimen.timer_touch_proxy_margin);

        mvTimerTouchProxy.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mIsGeneratingScramble || mIsRestoringToolBar) {
                    // Not ready to start the timer, yet. May be waiting on
                    // the animation of the restoration of the tool-bars
                    // after the timer was stopped, or waiting on the
                    // generation of a scramble ("mIsGeneratingScramble"
                    // flag). In these states, the timer cannot be running or
                    // counting down, so touches should be ignored.
                    return false;
                }

                // When the timer is not running, ignore touches along the
                // left margin of the view, as the user is probably trying to
                // open the side drawer menu. Otherwise, pass on those
                // touches, so there is no "dead zone" when trying to stop
                // the timer.
                if (mState != State.TIMER_RUNNING
                    && motionEvent.getX() <= touchMargin) {
                    return false;
                }

                // Just need to notify the PuzzleTimer of these events and it
                // will do the rest.
                if (DEBUG_ME) Log.d(TAG,
                    "MotionEvent: action=" + motionEvent.getAction());
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTimer.onTouchDown();
                        return true;

                    case MotionEvent.ACTION_UP:
                        mTimer.onTouchUp();
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        // The parent view has intercepted the recent
                        // sequence of touch events and has cancelled the
                        // touches for this view. Most likely, a "ViewPager"
                        // has interpreted the touches as a "swipe" gesture
                        // that it will respond to by changing the current
                        // page (or tab).
                        //
                        // "ACTION_CANCEL" is similar to "ACTION_UP", but the
                        // normal operation performed for an
                        // "ACTION_DOWN"..."ACTION_UP" sequence must *not* be
                        // performed. "PuzzleTimer" understands this and will
                        // revert whatever it did for the last "onTouchDown"
                        // if it was not followed by "onTouchUp".
                        //
                        // IMPORTANT: "PuzzleTimer" will not revert the last
                        // "onTouchDown" if that touch stopped the timer. The
                        // timer will remain stopped. This ensures that even
                        // "sloppy" touches will always stop the timer
                        // immediately.
                        mTimer.onTouchCancelled();
                        return true;
                }

                return false;
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated(savedInstanceState="
                + savedInstanceState + ')');
        super.onActivityCreated(savedInstanceState);

        if (DEBUG_ME) Log.d(TAG,
            "  onActivityCreated -> initLoader: SCRAMBLE LOADER");
        getLoaderManager().initLoader(MainActivity.SCRAMBLE_LOADER_ID, null,
            new LoggingLoaderCallbacks<ScrambleData>(
                    TAG, "SCRAMBLE LOADER") {
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
        getLoaderManager().initLoader(MainActivity.SCRAMBLE_IMAGE_LOADER_ID,
            null, new LoggingLoaderCallbacks<ScrambleImageData>(
                    TAG, "SCRAMBLE IMAGE LOADER") {
                @Override
                public Loader<ScrambleImageData> onCreateLoader(
                    int id, Bundle args) {
                    if (DEBUG_ME) super.onCreateLoader(id, args);
                    return new ScrambleImageLoader(getContext());
                }

                @Override
                public void onLoadFinished(Loader<ScrambleImageData> loader,
                                           ScrambleImageData data) {
                    if (DEBUG_ME) super.onLoadFinished(loader, data);
                    onScrambleImageGenerated(data);
                }

                @Override
                public void onLoaderReset(Loader<ScrambleImageData> loader) {
                    if (DEBUG_ME) super.onLoaderReset(loader);
                }
            });

        // The view has been created, so restore the instance state and link
        // the timer to the UI. From API 17, this could be done in
        // "onViewStateRestored", but support API 16, for now.

        // Create the puzzle timer. When created, the timer will be in the
        // "sleeping" state. It will call the listeners until after
        // "PuzzleTimer.wake()" is called in "onResume()".
        mTimer = new PuzzleTimer(this);
        if (DEBUG_ME) mTimer.addOnTimerEventListener(new OnTimerEventLogger());
        mTimer.addOnTimerEventListener(this);
        mTimer.addOnTimerEventListener(mvTimerWidget);
        mTimer.setOnTimerRefreshListener(mvTimerWidget);

        if (savedInstanceState != null) {
            mState = State.valueOf(
                savedInstanceState.getString(KEY_FRAGMENT_STATE));

            // If the "Parcelable" is "null", "onRestoreInstanceState" will
            // have no effect.
            mTimer.onRestoreInstanceState(
                savedInstanceState.getParcelable(KEY_PUZZLE_TIMER_STATE));
        }

        // If the statistics are already loaded, the update notification will
        // have been missed, so fire that notification now. If the statistics
        // are non-null, they will be displayed. If they are null (i.e., not
        // yet loaded), nothing will be displayed until this fragment, as a
        // registered observer, is notified when loading is complete.
        onStatisticsUpdated(StatisticsCache.getInstance().getStatistics());
        StatisticsCache.getInstance().registerObserver(this); // Unregistered in "onDestroyView".

        // Register a receiver to update if something has changed. Unregister
        // in "onDetach()".
        registerReceiver(mUIInteractionReceiver);

        // Do not generate a scramble now. The loaders may not be ready and
        // listening until after this method returns and the message queue is
        // processed. Wait until "onResume()".
    }

    @Override
    public void onStart() {
        if (DEBUG_ME) Log.d(TAG, "onStart()");
        super.onStart();

        // The view and all of the instance state has been restored. However,
        // this may not be the first time that "onStart" has been called.
        // "SettingsActivity" may have been started and changed some
        // preferences, so this is the time to apply the latest values of the
        // shared preferences to the views and other components.

        // Inject the up-to-date inspection duration and hold-to-start flag.
        // If the instance state was restored in "onActivityCreated()", the
        // up-to-date values used for any *new* solve attempts. If the timer
        // was running when its state was saved, the old values will be used
        // for that solve attempt until it is stopped.
        //
        // If inspection is not enabled, pass zero to the timer to make it
        // skip the inspection countdown.
        final boolean isInspectionEnabled = Prefs.getBoolean(
            R.string.pk_inspection_enabled, R.bool.default_inspection_enabled);
        final long inspTimeMS = Prefs.getInspectionTime() * 1_000L; // s -> ms
        final boolean isHoldToStart = Prefs.getBoolean(
            R.string.pk_hold_to_start_enabled,
            R.bool.default_hold_to_start_enabled);

        mTimer.setInspectionDuration(isInspectionEnabled ? inspTimeMS : 0);
        mTimer.setHoldToStartEnabled(isHoldToStart);

        // Apply most of the "advanced timer appearance settings".
        if (Prefs.getBoolean(
                R.string.pk_advanced_timer_settings_enabled,
                R.bool.default_advanced_timer_settings_enabled)) {

            mvTimerWidget.setDisplayScale(Prefs.getTimerDisplayScale());

            if (Prefs.getBoolean(
                    R.string.pk_large_quick_actions_enabled,
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

            final int timerTextOffsetPx = Prefs.getInt(
                R.string.pk_timer_text_offset_px,
                R.integer.default_timer_text_offset_px);

            mvTimerWidget.setTranslationY(-timerTextOffsetPx);
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

        setUpCrossHints(puzzleType);    // ... also sets the "mShowCrossHints" field.
        setUpScrambleImage(puzzleType); // ... if enabled.

        if (Prefs.isScrambleEnabled()) {
            if (!Prefs.showScrambleImage()) {
                mvScrambleImg.setVisibility(View.GONE);
            }
            // "mIsGeneratingScramble" stays raised to lock out the timer
            // until a scramble arrives.
        } else {
            mvScrambleText.setVisibility(View.GONE);
            mvScrambleImg.setVisibility(View.GONE);
            // No scramble will be generated automatically, so no need to
            // lock out the timer.
            mIsGeneratingScramble = false;
        }

        mvScrambleText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mvScrambleText.getTextSize() * Prefs.getScrambleTextScale());

        if (!Prefs.showSessionStats()) {
            mvStatsPanel.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume()");
        super.onResume();

        mvCrossHintsSlider.setPanelState(PanelState.HIDDEN);

        // TODO: Only if "mUnsolvedScramble" is null or not?
        generateScramble(getMainState().getPuzzleType());   // ... if enabled.

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
        // point in updating the UI if the timer display is not (fully)
        // visible. Re-awaken it in "onResume()".
        // FIXME: Does the call to "onTouchCancelled" belong here or in
        // "PuzzleTimer"?
        // FIXME: Explain why the touch needs to be cancelled (comments
        // available elsewhere).
        mTimer.onTouchCancelled();
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

        outState.putString(KEY_FRAGMENT_STATE,   mState.name()); // Never null.

        if (mTimer != null) {
            outState.putParcelable(
                KEY_PUZZLE_TIMER_STATE, mTimer.onSaveInstanceState());
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
        super.onDestroyView();

        mUnbinder.unbind();
        StatisticsCache.getInstance().unregisterObserver(this);
        mRecentStatistics = null;
    }

    @Override
    public void onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()");
        super.onDetach();

        // To fix memory leaks
        unregisterReceiver(mUIInteractionReceiver);
    }

    @Override
    public void onMainStateChanged(
            @NonNull MainState newMainState, @NonNull MainState oldMainState) {
        super.onMainStateChanged(newMainState, oldMainState); // For logging.

        // If the puzzle type changes, any displayed scramble and image will
        // become invalid.
        if (newMainState.getPuzzleType() != oldMainState.getPuzzleType()) {
            generateScramble(getMainState().getPuzzleType()); // ... if enabled.
        }
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
    public void onTimerCue(@NonNull TimerCue cue,
                           @NonNull TimerState timerState) {
        // IMPORTANT: One or more calls to "onTimerCue()" are always "bracketed"
        // by calls to "onTimerSet", one when the timer is started (or restored)
        // and one when the timer is stopped (or cancelled). The state of the
        // UI is mostly managed from "onTimerSet", but there are a few
        // transitions and other actions that are managed from "onTimerCue".

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
            case CUE_INSPECTION_SOLVE_HOLDING_FOR_START:
            case CUE_INSPECTION_SOLVE_READY_TO_START:
            case CUE_INSPECTION_STOPPED:
                break;

            case CUE_INSPECTION_7S_REMAINING:
            case CUE_INSPECTION_3S_REMAINING:
            case CUE_INSPECTION_TIME_OVERRUN:
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

            case CUE_STOPPING:
                // "onSolveAttemptStop" (if not cancelled) and "onTimerSet"
                // will be called after this cue is notified, so the work will
                // happen there.
                break;
        }
    }

    @Override
    public void onTimerSet(@NonNull TimerState timerState) {
        // This may be called when the timer has recorded a previous solve
        // and "onTouchDown()" is called. The call-back allows the timer
        // widget to be updated to show the "0.00" time of a new timer state
        // that was "pushed" in readiness for the start of a new solve
        // attempt. However, the call-back is *not* made if the timer had not
        // recorded a previous solve.
        //
        // This may also be called after "PuzzleTimer.wake()" is called. It
        // allows the timer widget to restore the display of the time after a
        // period of sleep when the time may have changed. In that case,
        // "onTimerRestored" will be called whether or not it has recorded a
        // previous solve.
        //
        // "TimerWidget.showSolve(Solve)" is generally used after
        // "mSavedSolve" has been recorded. That is the solve that will be
        // presented. However, if "mActiveSolve" is non-null, then there is
        // already a new solve attempt under way. (FIXME: Also test mState, I
        // suppose.)
        //
        // FIXME: Should "showSolve" or "onTimerRestored" be called on
        // "mvTimerWidget"? When call one and not the other? How much state
        // will "TimerWidget" save and restore itself?
        //
        // IMPORTANT: Avoid the temptation to "push" the instance state into
        // the "TimerWidget" to restore it. If instance state is passed, then
        // let the widget restore its own state, which it should also be
        // responsible for saving.
        //
        // Fragment "onCreate" is passed ...
        //
        // 1. ... no saved instance state.
        //
        // 2. ... saved instance state that is an unused timer.
        //
        // 3. ... saved instance state that is a stopped (not unused) timer.
        //
        // 4. ... saved instance state that is a running (not unused or stopped) timer.
        //
        // Fragment "onSaveInstanceState" is called ...
        //
        //

        //
        // 1. If the timer stage is "UNUSED", hide the editing controls and
        //    transition this fragment to "NO_SOLVE".
        //
        // 2. If the timer stage is "STOPPED", then:
        //    (This assumes a synchronous save has been performed.)
        //    a) If "TimerState.getSolve()" is not null, then:
        //       i.   If that solve has an ID, transition to "SOLVE_IN_EDIT".
        //       ii.  If that solve has no ID, then WTF? FIXME?
        //    b) If "TimerState.getSolve()" is null, then WTF?
        //
        // FIXME: To what extent are further transitions required? Won't the
        // saved instance state handle this for the most part?
        //
        // 3. If the timer stage is other than those stages, then transition
        //    this fragment to "TIMER_???".

        if (timerState.isUnused()) {
            // An unused timer is typical if there was no saved timer state to
            // be restored, or if the timer has been reset (e.g. if the solve
            // was deleted via the editing controls, or if other changes to the
            // solve data were notified and the solve may have been deleted or
            // edited other than via the timer API).
            //
            // An unused timer is also the state of the timer that is presented
            // immediately after the user's first touch down on the timer, just
            // before cues, such as "CUE_INSPECTION_HOLDING_FOR_START" are
            // notified to "onTimerCue". The behaviour has been to show "0.00"
            // and only go "full-screen" when the timer is ready to start, so
            // that will be done from "onTimerCue".
            hideQuickActionButtons();
            showFullScreenTimer(false);
        } else if (timerState.isStopped()) {
            // NOTE: If a solve attempt is cancelled, the cancelled timer state
            // will not be notified to "onTimerSet()". The cancelled state is
            // only notified to "onTimerCue()" for "CUE_CANCELLING". The cue is
            // then followed by a call to "onTimerSet()" with the state that
            // was restored after the cancelled state was discarded. Therefore,
            // if "isStopped()" is true, then there should be a valid solve
            // present.
            //
            // IMPORTANT: As "onSolveAttemptStop(Solve)" saves the solve to the
            // database synchronously and "onTimerSet()" is called *after* that
            // method, the solve will already be saved and should be ready to
            // be edited. "onTimerSet()" will also be called after each
            // subsequent edit that affects the timer, i.e., if there are any
            // changes to the penalties. This also allows the buttons that
            // control the editing of penalties to be enabled/disabled as
            // appropriate.
            if (timerState.getSolve() != null) {
                showQuickActionButtons(timerState.getSolve());
            } else {
                // This is not expected to happen. Probably a bug or a false
                // assumption about when this method is or is not called.
                throw new IllegalStateException(
                    "onTimerSet: State of stopped timer is unexpected: "
                    + timerState);
            }
            showFullScreenTimer(false);
        } else {
            // Timer is running, so it should be shown full-screen.
            //
            // "onTimerSet()" is never called when the timer is in a
            // "hold-to-start" state. Just before such a state, "onTimerSet()"
            // is passed an unused timer state. If the timer state is saved to
            // and then restored from instance state, it will not be restored
            // into a "hold-to-start" state. Therefore, there will never be a
            // need to restore the UI to the brief state where the timer is
            // waiting for a hold-to-start period to be completed and is not
            // yet showing the timer full-screen. That transition is always
            // driven from "onTimerCue()".
            hideQuickActionButtons();
            showFullScreenTimer(true);
        }
    }

    @Override
    @NonNull
    public Solve onSolveAttemptStart() {
        // FIXME: Should this be in sync with "CUE_STARTING"? Is that cue
        // even needed? Perhaps it would be handy for triggering sounds or
        // flashes.... Hmm, I think that cue fires a bit later in the
        // sequence that this call-back might.

        // FIXME: IMPORTANT: Should this be called even when just
        // holding-for-start? In that case, "CUE_STARTING" is not yet fired.
        // However, I think it might be OK to just fire the early cues before
        // calling this method, as the "TimerState" will just be reporting
        // all zeros and there is no problem displaying the early timer state
        // in the absence of a "Solve". What about "onTimerRestored"? Should
        // that be called or not, or is it moot for the case where
        // "onTimerStarted" would be called?

        // FIXME: Create a new "Solve" and set the puzzle type, category and
        // scramble.

        // FIXME: What is the procedure if the attempt is cancelled? Should
        // the scramble be consumed?
        final MainState mainState = getMainState();
        final Solve newSolve = new Solve(
            mainState.getPuzzleType(), mainState.getSolveCategory(),
            mUnsolvedScramble != null ? mUnsolvedScramble.scramble : null);

        // Generate a new scramble to replace the now-consumed scramble. This
        // does nothing if scrambles are not enabled. An early result from the
        // generator will not be displayed until after the timer stops.
        mUnsolvedScramble = null;
        generateScramble(mainState.getPuzzleType());

        return newSolve;
    }

    @Override
    public void onSolveAttemptStop(@NonNull Solve solve) {
        // To keep things simple for now, this save is synchronous. The ID is
        // set before returning/broadcasting the new solve and transitioning to
        // "SOLVE_IN_EDIT".
        solve.setID(TwistyTimer.getDBHandler().addSolve(solve));

        if (!solve.getPenalties().hasDNF()) {
            proclaimRecordTimes(solve);
        }

        // FIXME: What if there is an inspection time-out when the fragment is
        // not added? Is a guard needed around "getMainState()"? Would it make
        // more sense to capture that information when the timer is added
        // (storing it in a "Solve") and then updating that solve later?
        // FIXME: UPDATE: Most of the necessary details are on "solve", now.
        // FIXME: What if the main state of the solve does not match the
        // current main state? Would that confuse one of the loaders?
        TTIntent.builder(CATEGORY_TIME_DATA_CHANGES, ACTION_TIME_ADDED)
                .mainState(getMainState())
                .solve(solve)
                .broadcast();

        // FIXME: If "TimerState.isSaved() == true", then if
        // "TimerListFragment" is used to delete the solve (either just the
        // single solve, or all from the session, or whatever), then the
        // solve-in-edit in "TimerFragment" will also need to be deleted.
        // That might take a little bit of coordination, or maybe some more
        // reliable DB update notification approach.
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

        final long newTime = solve.getTime();

        if (solve.getPenalties().hasDNF() || newTime <= 0
                || mRecentStatistics == null
                || mRecentStatistics.getAllTimeNumSolves()
                   - mRecentStatistics.getAllTimeNumDNFSolves() < 4) {
            // Not a valid time, or there are no previous statistics, or not
            // enough previous times to make reporting meaningful (or
            // non-annoying), so cannot check for a new PB.
            return;
        }

        if (Prefs.proclaimBestTime()) {
            final long previousBestTime = mRecentStatistics.getAllTimeBestTime();

            // If "previousBestTime" is a DNF or UNKNOWN, it will be less
            // than zero, so the new solve time cannot better (i.e., lower).
            if (newTime < previousBestTime ) {
                mvCongratsRipple.startRippleAnimation();
                mvCongratsText.setText(getString(R.string.personal_best_message,
                        formatTimeStatistic(previousBestTime - newTime)));
                mvCongratsText.setVisibility(View.VISIBLE);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mvCongratsRipple != null)
                            mvCongratsRipple.stopRippleAnimation();
                    }
                }, 2940);
            }
        }

        if (Prefs.proclaimWorstTime()) {
            final long previousWorstTime = mRecentStatistics.getAllTimeWorstTime();

            // If "previousWorstTime" is a DNF or UNKNOWN, it will be less
            // than zero. Therefore, make sure it is at least greater than
            // zero before testing against the new time.
            if (previousWorstTime > 0 && newTime > previousWorstTime) {
                mvCongratsText.setText(getString(R.string.personal_worst_message,
                        formatTimeStatistic(newTime - previousWorstTime)));

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
                        *= calculateScrambleImageHeightMultiplier(scrambleImageScale, puzzleType);
            } else {
                mvScrambleImg.getLayoutParams().height
                        *= calculateScrambleImageHeightMultiplier(1, puzzleType); // 1 == 100%
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

        // Save these for later. The best and worst times can be retrieved
        // and compared to the next new solve time to be added via
        // "addNewSolve".
        mRecentStatistics = stats; // May be null.

        if (stats == null) {
            return;
        }

        // "TIME_UNKNOWN" times will be formatted as "--", not "0.00". This
        // is useful when an average has yet to be calculated. For example,
        // the Ao50 value after only 49 solves are added. The values are
        // returned from "stats" already rounded appropriately.
        String sessionCount
                = String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves());
        String sessionMeanTime = formatTimeStatistic(stats.getSessionMeanTime());
        String sessionBestTime = formatTimeStatistic(stats.getSessionBestTime());
        String sessionWorstTime = formatTimeStatistic(stats.getSessionWorstTime());

        String sessionCurrentAvg5 = formatTimeStatistic(
                stats.getAverageOf(5, true).getCurrentAverage());
        String sessionCurrentAvg12 = formatTimeStatistic(
                stats.getAverageOf(12, true).getCurrentAverage());
        String sessionCurrentAvg50 = formatTimeStatistic(
                stats.getAverageOf(50, true).getCurrentAverage());
        String sessionCurrentAvg100 = formatTimeStatistic(
                stats.getAverageOf(100, true).getCurrentAverage());

        mvStatsTimesAvg.setText(
                sessionCurrentAvg5 + "\n" +
                        sessionCurrentAvg12 + "\n" +
                        sessionCurrentAvg50 + "\n" +
                        sessionCurrentAvg100);

        mvStatsTimesMore.setText(
                sessionMeanTime + "\n" +
                        sessionBestTime + "\n" +
                        sessionWorstTime + "\n" +
                        sessionCount);
    }

    private void showFullScreenTimer(boolean isFullScreen) {
        if (DEBUG_ME) Log.d(TAG, "showFullScreenTimer(" + isFullScreen + ')');

        if (isFullScreen) {
            lockOrientation(getActivity());
            // FIXME: Should probably broadcast this from "onTimerCue"...
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_HIDE_TOOLBAR);

            hideItems();
        } else {
            // "TimerMainFragment" hosts the tool-bar, so it will show it.
            // After the animation is done, "ACTION_TOOLBAR_RESTORED" will be
            // fired back to this fragment and it will show all of its own
            // views ("showItems()") that were hidden when the toolbar was
            // hidden.
            mIsRestoringToolBar = true;
            unlockOrientation(getActivity());
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SHOW_TOOLBAR);
        }
    }

    private void showItems(boolean isSolveEditable) {
        if (DEBUG_ME) Log.d(TAG, "showItems(" + isSolveEditable + ')');

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

        if (isSolveEditable && Prefs.showQuickActions()) {
            mvQuickActionPanel.setEnabled(true);
            mvQuickActionPanel.setVisibility(View.VISIBLE);
        }
    }

    private void hideItems() {
        if (DEBUG_ME) Log.d(TAG, "hideItems()");

        // FIXME: It looks like the issue with the strange fade in/out is
        // caused by the "GONE" state of the views. Changing from "GONE" to
        // "INVISIBLE" maybe makes them appear for the purpose of fading them
        // out.

        // FIXME: Is this right? It is not shown by "showItems", but that
        // would be conditionally, anyway. The issue is that if a new solve
        // is cancelled, should the old items be returned to the way there
        // were or not?
        mvCongratsText.setVisibility(View.INVISIBLE);
        mvCongratsText.setCompoundDrawables(null, null, null, null);

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

        if (Prefs.showQuickActions()) {
            mvQuickActionPanel.setEnabled(false);
            mvQuickActionPanel.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Hides the quick action buttons. This affects the visibility of the
     * whole panel that contains the buttons.
     */
    private void hideQuickActionButtons() {
        // Use "INVISIBLE", not "GONE", otherwise a layout transition from
        // "INVISIBLE" to "GONE" might be animated visibly, which looks
        // really bad.
        mvQuickActionPanel.setVisibility(View.INVISIBLE);
    }

    /**
     * Shows the quick action buttons. The enabled state of the buttons (and
     * the text of some of the buttons) is matched to the state of the solve
     * and its penalties.
     *
     * @param solve
     *     The solve for which the quick action buttons should be presented.
     */
    private void showQuickActionButtons(@NonNull Solve solve) {
        final Penalties p = solve.getPenalties();

        if (p.hasPostStartDNF()) {
            // This DNF can be annulled. Show "-DNF".
            mvDNFButton.setText("-DNF");
            mvDNFButton.setEnabled(true);
        } else if (p.hasPreStartDNF()) {
            // A pre-start DNF cannot be annulled as it was not added by the
            // user. Show "-DNF", but disable it.
            mvDNFButton.setText("-DNF");
            mvDNFButton.setEnabled(true);
        } else if (!p.hasDNF()) {
            // A post-start DNF can be incurred. Show "+DNF".
            mvDNFButton.setText("+DNF");
            mvDNFButton.setEnabled(true);
        }

        // Only post-start "+2" penalties can be edited. If there is a
        // pre-start DNF, there can be no post-start penalties.
        mvPlusTwoButton.setEnabled(!p.hasPreStartDNF()
                && p.getPostStartPlusTwoCount() < Penalties.MAX_POST_START_PLUS_TWOS);
        mvMinusTwoButton.setEnabled(p.getPostStartPlusTwoCount() > 0);

        mvDNFButton.setAlpha(mvDNFButton.isEnabled() ? 1f : 0.5f);
        mvPlusTwoButton.setAlpha(mvPlusTwoButton.isEnabled() ? 1f : 0.5f);
        mvMinusTwoButton.setAlpha(mvMinusTwoButton.isEnabled() ? 1f : 0.5f);

        mvQuickActionPanel.setVisibility(View.VISIBLE);
    }

    public static void lockOrientation(Activity activity) {
        int rotation = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        switch (tempOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
        activity.setRequestedOrientation(orientation);
    }

    private static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /**
     * Generates a new scramble <i>if scrambles are enabled</i>. The caller
     * does not need to check if scramble images are enabled before calling
     * this method. The scramble will be generated on a background thread by
     * the {@code ScrambleLoader}. {@link #onScrambleGenerated(ScrambleData)}
     * will be called when that generation task is complete. A scramble image
     * can be generated at that time, if required.
     *
     * @param puzzleType
     *     The puzzle type for which a new scramble is required.
     *
     * @return
     *     {@code true} if a new scramble will be generated; or {@code false}
     *     if scrambles are not enabled, so no scramble will be generated.
     */
    public boolean generateScramble(@NonNull PuzzleType puzzleType) {
        if (DEBUG_ME) Log.d(TAG, "generateScramble(puzzleType=" + puzzleType.typeName() + ')');

        // NOTE: This is the same behaviour as before: scrambles are either
        // enabled and generated automatically when required, or they are
        // disabled and never generated...even if the scramble button on the
        // tool-bar is pressed. That probably needs to change.
        if (Prefs.isScrambleEnabled()) {
            TTIntent.broadcastNewScrambleRequest(puzzleType);

            if (mShowCrossHints) {
                mvCrossHintsSlider.setPanelState(PanelState.HIDDEN);
            }

            mvScrambleText.setText(R.string.generating_scramble);
            mvScrambleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mvScrambleText.setClickable(false);
            mvScrambleImg.setVisibility(View.INVISIBLE);

            if (!mState.isTimerDominant()) {
                // Scrambles and images can be generated in the background
                // while the timer is running/dominant. In that case, the
                // progress spinner should not be displayed, as it would
                // intrude on the timer.
                mvScrambleProgress.setVisibility(View.VISIBLE);
            }

            mIsGeneratingScramble = true;
            return true;
        }
        return false;
    }

    /**
     * Generates a new scramble image <i>if scramble images are enabled</i>.
     * The caller does not need to check if scramble images are enabled
     * before calling this method. The scramble image will be generated on a
     * background thread by the {@code ScrambleImageLoader}.
     * {@link #onScrambleImageGenerated(ScrambleImageData)} will be called
     * when that generation task is complete.
     *
     * @param scrambleData
     *     The scramble sequence and puzzle type for which a new scramble
     *     image is required.
     *
     * @return
     *     {@code true} if a new scramble image will be generated; or {@code
     *     false} if scramble images are not enabled, so no scramble image
     *     will be generated.
     */
    public boolean generateScrambleImage(@NonNull ScrambleData scrambleData) {
        if (DEBUG_ME) Log.d(TAG, "generateScrambleImage(scrambleData=" + scrambleData + ')');

        if (Prefs.showScrambleImage()) { // Implies scramble generation is also enabled.
            TTIntent.broadcastNewScrambleImageRequest(
                    scrambleData.puzzleType, scrambleData.scramble);

            // "hideScrambleImage()" is called from "generateScramble". It
            // still remains hidden.
            if (!mState.isTimerDominant()) {
                // Scrambles and images can be generated in the background
                // while the timer is running. In that case, the progress
                // spinner should not be displayed.
                mvScrambleProgress.setVisibility(View.VISIBLE);
            }
            return true;
        }
        return false;
    }

    /**
     * Notifies this fragment that the {@link ScrambleLoader} has completed
     * the generation of a new scramble sequence. The sequence may now be
     * displayed and, if required, a corresponding image can be generated.
     *
     * @param scrambleData
     *     The scramble data describing the generated scramble sequence and
     *     the puzzle type for which it was generated.
     */
    private void onScrambleGenerated(final ScrambleData scrambleData) {
        if (DEBUG_ME) Log.d(TAG, "onScrambleGenerated(scrambleData=" + scrambleData + ')');

        if (!isAdded() || getView() == null) {
            if (DEBUG_ME) Log.d(TAG, "  Cannot display scramble! Not attached or no view.");
            // May have received the data too late for it to be displayed (or
            // too early).
            return;
        }

        mUnsolvedScramble = scrambleData;

        if (!generateScrambleImage(scrambleData)) {
            // No need to keep showing the progress spinner, as no image will
            // be generated.
            mvScrambleProgress.setVisibility(View.GONE);
        }

        mvScrambleText.setVisibility(View.INVISIBLE);
        mvScrambleText.setText(scrambleData.scramble);
        mvScrambleText.post(new Runnable() {
            @Override
            public void run() {
                if (mvScrambleText == null) {
                    // Runnable was executed after unbinding the views, perhaps.
                    return;
                }

                Rect scrambleRect = new Rect(mvScrambleText.getLeft(), mvScrambleText.getTop(),
                        mvScrambleText.getRight(), mvScrambleText.getBottom());
                Rect chronometerRect = new Rect(mvTimerWidget.getLeft(), mvTimerWidget.getTop(),
                        mvTimerWidget.getRight(), mvTimerWidget.getBottom());
                Rect congratsRect = new Rect(mvCongratsText.getLeft(), mvCongratsText.getTop(),
                        mvCongratsText.getRight(), mvCongratsText.getBottom());

                if (Rect.intersects(scrambleRect, chronometerRect) ||
                        (mvCongratsText.getVisibility() == View.VISIBLE
                                && Rect.intersects(scrambleRect, congratsRect))) {
                    mvScrambleText.setClickable(true);
                    mvScrambleText.setText(R.string.scramble_text_tap_hint);
                    mvScrambleText.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_dice_white_24dp, 0, 0, 0);
                    mvScrambleText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mvCrossHintsText.setText(scrambleData.scramble);
                            mvCrossHintsText.setTextSize(
                                    TypedValue.COMPLEX_UNIT_PX, mvScrambleText.getTextSize());
                            mvCrossHintsText.setGravity(Gravity.CENTER);
                            mvCrossHintsProgress.setVisibility(View.GONE);
                            mvCrossHintsProgressText.setVisibility(View.GONE);
                            mvCrossHintsSlider.setPanelState(PanelState.EXPANDED);
                        }
                    });
                } else {
                    mvScrambleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    mvScrambleText.setClickable(false);
                }

                if (!mState.isTimerDominant()) {
                    // Scrambles and images can be generated in the
                    // background while the timer is running. In that case,
                    // the scramble text should not be displayed yet.
                    mvScrambleText.setVisibility(View.VISIBLE);
                }
            }
        });

        // No need to lock the UI waiting for the scramble image to be generated....
        mIsGeneratingScramble = false;
    }

    /**
     * Notifies this fragment that the {@link ScrambleImageLoader} has
     * completed the generation of a new scramble image for a scramble
     * sequence. The image may now be displayed.
     *
     * @param scrambleImageData
     *     The scramble image data describing the generated scramble image,
     *     its scramble sequence and the puzzle type for which it was generated.
     */
    private void onScrambleImageGenerated(ScrambleImageData scrambleImageData) {
        if (DEBUG_ME) Log.d(TAG, "onScrambleImageGenerated(scrambleData="
                + scrambleImageData + ')');

        if (!isAdded() || getView() == null) {
            if (DEBUG_ME) Log.d(TAG, "  Cannot display scramble image! Not attached or no view.");
            // May have received the data too late for it to be displayed (or
            // too early).
            return;
        }

        mvScrambleProgress.setVisibility(View.INVISIBLE);
        mvScrambleImg.setImageDrawable(scrambleImageData.image);
        mvBigScrambleImg.setImageDrawable(scrambleImageData.image);
        if (!mState.isTimerDominant()) {
            mvScrambleImg.setVisibility(View.VISIBLE);
        } else {
            if (DEBUG_ME) Log.d(TAG, "  Will not display scramble image while timer is dominant.");
        }
    }

    private class GetOptimalCross extends AsyncTask<ScrambleData, Void, String> {
        @Override
        protected String doInBackground(ScrambleData... scrambles) {
            // Increase the thread priority, as the user is waiting
            // impatiently for the "tip" to appear. The default priority is
            // probably the "background" priority, which is set by
            // "AsyncTask". This priority may be subject to special handling,
            // such as restricting the thread to no more than 5-10% of the
            // CPU if any other threads are busy. However, this task is part
            // of the foreground application, so it should not have to
            // compete with background apps for CPU time. Therefore, keep the
            // priority low enough to avoid stalling the UI, but just high
            // enough to keep this thread out of the specially restricted
            // "background" group. Use the "android.os.Process" methods to
            // set the Linux thread priorities, as those are what matter.
            final int threadID = Process.myTid();
            final int savedThreadPriority = Process.getThreadPriority(threadID);

            Process.setThreadPriority(threadID,
                    Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

            try {
                final ScrambleData scrambleData = scrambles[0];
                String tip = new RubiksCubeOptimalCross(getString(R.string.optimal_cross))
                        .getTip(scrambleData.scramble);

                if (Prefs.showXCrossHints(scrambleData.puzzleType)) {
                    tip += "\n\n";
                    tip += new RubiksCubeOptimalXCross(getString(R.string.optimal_x_cross))
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

            // Do not intrude on the timer if it is "dominant".
            if (mvCrossHintsProgressText != null && mvCrossHintsText != null
                    && !mState.isTimerDominant()) {
                mvCrossHintsText.setText(text);
                mvCrossHintsText.setVisibility(View.VISIBLE);

                mvCrossHintsProgress.setVisibility(View.GONE);
                mvCrossHintsProgressText.setVisibility(View.GONE);
            }
        }
    }

    private void zoomScrambleImage(
            final View smallImg, final View bigImg, View boundary, final int animationDuration) {
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

        growAnim.play(ObjectAnimator.ofFloat(bigImg, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(bigImg, View.Y, startBounds.top, finalBounds.top))
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
                        .play(ObjectAnimator.ofFloat(bigImg, View.X, startBounds.left))
                        .with(ObjectAnimator.ofFloat(bigImg, View.Y, startBounds.top))
                        .with(ObjectAnimator.ofFloat(bigImg, View.SCALE_X, startScale))
                        .with(ObjectAnimator.ofFloat(bigImg, View.SCALE_Y, startScale));

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
