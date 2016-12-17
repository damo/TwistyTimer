package com.aricneto.twistytimer.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.TimeCursorAdapter;
import com.aricneto.twistytimer.database.TimeListLoader;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.Fab;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.aricneto.twistytimer.utils.TimeUtils;
import com.gordonwong.materialsheetfab.DimOverlayFrameLayout;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;

import org.joda.time.DateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import co.mobiwise.materialintro.shape.FocusGravity;
import co.mobiwise.materialintro.view.MaterialIntroView;

import static com.aricneto.twistytimer.utils.TTIntent.*;

public class TimerListFragment extends BaseMainFragment
       implements LoaderManager.LoaderCallbacks<Cursor>,
                  OnBackPressedInFragmentListener,
                  StatisticsCache.StatisticsObserver {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerListFragment.class.getSimpleName();

    private static final String SHOWCASE_FAB_ID = "SHOWCASE_FAB_ID";

    private MaterialSheetFab<Fab> materialSheetFab;

    private Unbinder mUnbinder;
    @BindView(R.id.list)                 RecyclerView          recyclerView;
    @BindView(R.id.nothing_here)         ImageView             nothingHere;
    @BindView(R.id.nothing_text)         TextView              nothingText;
    @BindView(R.id.send_to_history_card) CardView              moveToHistory;
    @BindView(R.id.clear_button)         TextView              clearButton;
    @BindView(R.id.divider01)            View                  dividerView;
    @BindView(R.id.archive_button)       TextView              archiveButton;
    @BindView(R.id.fab_button)           Fab                   fabButton;
    @BindView(R.id.overlay)              DimOverlayFrameLayout overlay;
    @BindView(R.id.fab_sheet)            CardView              fabSheet;
    @BindView(R.id.fab_share_ao12)       TextView              fabShareAo12;
    @BindView(R.id.fab_share_ao5)        TextView              fabShareAo5;
    @BindView(R.id.fab_share_histogram)  TextView              fabShareHistogram;
    @BindView(R.id.fab_add_time)         TextView              fabAddTime;
    @BindView(R.id.fab_scroll)           ScrollView            fabScroll;

    private TimeCursorAdapter timeCursorAdapter;

    /**
     * The most recently notified solve time statistics. These may be used when
     * sharing averages.
     */
    // Does not need to be saved to the instance state, as it will be restored
    // automatically.
    private Statistics mRecentStatistics;

    private final View.OnClickListener clickListener
        = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Should be attached if being clicked.
            final MainState mainState = getMainState();

            switch (view.getId()) {
                case R.id.fab_share_ao12:
                    if (!PuzzleUtils.shareAverageOf(12, mRecentStatistics,
                            getActivity())) {
                        Toast.makeText(getContext(), R.string.fab_share_error,
                            Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.fab_share_ao5:
                    if (!PuzzleUtils.shareAverageOf(5, mRecentStatistics,
                            getActivity())) {
                        Toast.makeText(getContext(), R.string.fab_share_error,
                            Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.fab_share_histogram:
                    if (!PuzzleUtils.shareHistogram(mRecentStatistics,
                            getActivity())) {
                        Toast.makeText(getContext(), R.string.fab_share_error,
                            Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.fab_add_time:
                    new MaterialDialog.Builder(getContext())
                        .title(R.string.add_time)
                        .input(getString(R.string.add_time_hint), "", false,
                            new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(
                                    @NonNull MaterialDialog dialog,
                                    CharSequence input) {
                                final long time = TimeUtils.parseTimeExternal(
                                    input.toString());

                                if (time == 0) {
                                    return; // Time not in a valid format.
                                }

                                final Solve solve = new Solve(time,
                                    mainState.getPuzzleType(),
                                    mainState.getSolveCategory(),
                                    new DateTime().getMillis(), null,
                                    Penalties.NO_PENALTIES, null, false);

                                // Run in background to avoid stalling the UI.
                                TwistyTimer.getDBHandler()
                                           .addSolveAndNotifyAsync(solve);
                            }
                        })
                        .positiveText(R.string.action_add)
                        .negativeText(R.string.action_cancel)
                        .show();
                    break;
            }
        }
    };

    // Receives broadcasts after changes have been made to time data or the
    // selection of that data.
    private final TTFragmentBroadcastReceiver mSolveDataChangedReceiver
        = new TTFragmentBroadcastReceiver(this, CATEGORY_SOLVE_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            final MainState mainState = getMainState();

            TTIntent.validate(intent);

            switch (intent.getAction()) {
                case ACTION_ONE_SOLVE_ADDED:
                case ACTION_ONE_SOLVE_DELETED:
                    // When "history" is enabled, the list of solves does not
                    // include solves from the current session. Only reload the
                    // list when showing the same set of solves (current session
                    // vs. past sessions) as the solve that was added/deleted.
                    // This does not work for updated solves, as the update may
                    // have changed the history flag. The puzzle type and solve
                    // category are assumed to be the same.
                    //noinspection ConstantConditions (validated above)
                    if (mainState.isHistoryEnabled()
                            == TTIntent.getSolve(intent).isHistory()) {
                        reloadList(mainState);
                    }
                    break;

                case ACTION_ONE_SOLVE_UPDATED:
                case ACTION_MANY_SOLVES_DELETED:
                case ACTION_MANY_SOLVES_ADDED:
                case ACTION_SOLVES_MOVED_TO_HISTORY:
                    reloadList(mainState);
                    break;
            }
        }
    };

    // Receives broadcasts for UI interactions that require actions to be taken.
    private final TTFragmentBroadcastReceiver mUIInteractionReceiver
        = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_DELETE_SELECTED_SOLVES:
                    // Operation will delete times and then broadcast an intent.
                    timeCursorAdapter.deleteAllSelected();
                    break;
            }
        }
    };

    public TimerListFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change
    // puzzles.
    public static TimerListFragment newInstance() {
        final TimerListFragment fragment = new TimerListFragment();
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState="
                                 + savedInstanceState + ")");
        View rootView = inflater.inflate(
            R.layout.fragment_time_list, container, false);
        mUnbinder = ButterKnife.bind(this, rootView);

        materialSheetFab = new MaterialSheetFab<>(
                fabButton, fabSheet, overlay, Color.WHITE,
                ThemeUtils.fetchAttrColor(getActivity(), R.attr.colorPrimary));

        materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
            @Override
            public void onSheetShown() {
                super.onSheetShown();
                fabScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        fabScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        fabShareAo12.setOnClickListener(clickListener);
        fabShareAo5.setOnClickListener(clickListener);
        fabShareHistogram.setOnClickListener(clickListener);
        fabAddTime.setOnClickListener(clickListener);

        // The "Clear" and "Archive" buttons are not shown if the history
        // switch is enabled, or if the solve list is empty. See
        // "setEmptyState", which is called from "onLoadFinished".
        if (Prefs.getBoolean(R.string.pk_show_clear_button,
                             R.bool.default_show_clear_button)) {
            dividerView.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
            archiveButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_timer_sand_black_18dp, 0, 0, 0);
        }

        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Spannable text = new SpannableString(
                    getString(R.string.move_solves_to_history_content) + "  ");

                text.setSpan(new ImageSpan(
                        getContext(), R.drawable.ic_icon_history_demonstration),
                    text.length() - 1, text.length(), 0);

                // Do not get the main state in the "onCreateView" method, wait
                // for this call-back.
                final MainState mainState = getMainState();

                new MaterialDialog.Builder(getContext())
                    .title(R.string.move_solves_to_history)
                    .content(text)
                    .positiveText(R.string.action_move)
                    .negativeText(R.string.action_cancel)
                    .neutralColor(ContextCompat.getColor(
                        getContext(), R.color.black_secondary))
                    .negativeColor(ContextCompat.getColor(
                        getContext(), R.color.black_secondary))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            // The list will be reloaded when the broadcast
                            // receiver is notified that the task is complete.
                            TwistyTimer
                                .getDBHandler()
                                .moveAllSolvesToHistoryAndNotifyAsync(
                                    mainState.getPuzzleType(),
                                    mainState.getSolveCategory());
                        }
                    })
                    .show();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Do not get the main state in the "onCreateView" method, wait
                // for this call-back.
                final MainState mainState = getMainState();

                new MaterialDialog.Builder(getContext())
                    .title(R.string.remove_session_title)
                    .content(R.string.remove_session_confirmation_content)
                    .positiveText(R.string.action_remove)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            // The list will be reloaded when the broadcast
                            // receiver is notified that the task is complete.
                            TwistyTimer
                                .getDBHandler()
                                .deleteAllSolvesFromSessionAndNotifyAsync(
                                    mainState.getPuzzleType(),
                                    mainState.getSolveCategory());
                        }
                    })
                    .show();
            }
        });

        setUpRecyclerView();

        registerReceiver(mSolveDataChangedReceiver);
        registerReceiver(mUIInteractionReceiver);

        // If the statistics are already loaded, the update notification will
        // have been missed, so fire that notification now and start observing
        // further updates.
        onStatisticsUpdated(StatisticsCache.getInstance().getStatistics());
        StatisticsCache.getInstance().registerObserver(this);
        // Unregistered in "onDestroyView".

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated(savedInstanceState="
                + savedInstanceState + ')');

        super.onActivityCreated(savedInstanceState);

        // Wait until the activity is available, so "getMainState()" will work.
        getLoaderManager().initLoader(
            MainActivity.TIME_LIST_LOADER_ID, null, this);
    }

    /**
     * Handles a change in the main state by triggering a re-load of the list
     * of solve times to ensure they match the new state.
     *
     * @param newMainState The new main state.
     * @param oldMainState The old main state.
     */
    @Override
    public void onMainStateChanged(
            @NonNull MainState newMainState, @NonNull MainState oldMainState) {
        super.onMainStateChanged(newMainState, oldMainState); // For logging.
        reloadList(newMainState);
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (DEBUG_ME) Log.d(TAG, "setUserVisibleHint(isVisibleToUser="
                                 + isVisibleToUser + ")");
        super.setUserVisibleHint(isVisibleToUser);

        if (isResumed()) {
            if (isVisibleToUser) {
                if (fabButton != null) {
                    // Show FAB and intro (if intro was not already dismissed
                    // by the user in a previous session) if the fragment has
                    // become visible.
                    fabButton.show();
                    new MaterialIntroView.Builder(getActivity())
                        .enableDotAnimation(false)
                        .setFocusGravity(FocusGravity.CENTER)
                        .setDelayMillis(600)
                        .enableFadeAnimation(true)
                        .enableIcon(false)
                        .performClick(true)
                        .dismissOnTouch(true)
                        .setInfoText(getString(R.string.showcase_fab_average))
                        .setTarget(fabButton)
                        .setUsageId(SHOWCASE_FAB_ID)
                        .show();
                }
            } else if (materialSheetFab != null) {
                // Hide sheet and FAB if the fragment is no longer visible.
                materialSheetFab.hideSheetThenFab();
            }
        }
    }

    /**
     * Hides the sheet for the floating action button, if the sheet is currently
     * open.
     *
     * @return
     *     {@code true} if the "Back" button press was consumed to close the
     *     sheet; or {@code false} if the sheet is not showing and the "Back"
     *     button press was ignored.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");
        if (isResumed() && materialSheetFab != null
                && materialSheetFab.isSheetVisible()) {
            materialSheetFab.hideSheet();
            return true;
        }
        return false;
    }

    @Override
    public void onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()");
        super.onDetach();
        // To fix memory leaks
        unregisterReceiver(mSolveDataChangedReceiver);
        unregisterReceiver(mUIInteractionReceiver);
        getLoaderManager().destroyLoader(MainActivity.TIME_LIST_LOADER_ID);
    }

    /**
     * Records the latest statistics for use when sharing such information.
     *
     * @param stats
     *     The updated statistics. These will not be modified. May be
     *     {@code null}.
     */
    @Override
    public void onStatisticsUpdated(Statistics stats) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated(" + stats + ")");
        mRecentStatistics = stats;
    }

    public void reloadList(@NonNull MainState mainState) {
        if (DEBUG_ME) Log.d(TAG, "reloadList(mainState=" + mainState + ')');
        // Pass the main state as arguments to the loader. This keeps things
        // nicely decoupled.
        getLoaderManager().restartLoader(
            MainActivity.TIME_LIST_LOADER_ID,
            mainState.saveToBundle(new Bundle()), this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, @NonNull Bundle args) {
        if (DEBUG_ME) Log.d(TAG, "onCreateLoader(id=" + id
                                 + ", args=" + args + ')');
        return new TimeListLoader(getMainState());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (DEBUG_ME) Log.d(TAG, "onLoadFinished()");
        timeCursorAdapter.swapCursor(cursor);
        setEmptyState(cursor, ((TimeListLoader) loader).getMainState());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (DEBUG_ME) Log.d(TAG, "onLoaderReset()");
        timeCursorAdapter.swapCursor(null);
    }

    public void setEmptyState(Cursor cursor, @NonNull MainState mainState) {
        if (cursor.getCount() == 0) {
            nothingHere.setVisibility(View.VISIBLE);
            nothingText.setVisibility(View.VISIBLE);
            moveToHistory.setVisibility(View.GONE);
            if (mainState.isHistoryEnabled()) {
                nothingHere.setImageDrawable(ContextCompat.getDrawable(
                    getContext(), R.drawable.notherehistory));
                nothingText.setText(R.string.list_empty_state_message_history);
            } else {
                nothingHere.setImageDrawable(ContextCompat.getDrawable(
                    getContext(), R.drawable.nothere2));
                nothingText.setText(R.string.list_empty_state_message);
            }
        } else {
            nothingHere.setVisibility(View.INVISIBLE);
            nothingText.setVisibility(View.INVISIBLE);
            if (mainState.isHistoryEnabled())
                moveToHistory.setVisibility(View.GONE);
            else
                moveToHistory.setVisibility(View.VISIBLE);
        }
    }

    private void setUpRecyclerView() {
        Activity parentActivity = getActivity();

        timeCursorAdapter = new TimeCursorAdapter(getActivity(), null, this);

        // Set different managers to support different orientations
        StaggeredGridLayoutManager gridLayoutManagerHorizontal =
            new StaggeredGridLayoutManager(
                6, StaggeredGridLayoutManager.VERTICAL);
        StaggeredGridLayoutManager gridLayoutManagerVertical =
            new StaggeredGridLayoutManager(
                3, StaggeredGridLayoutManager.VERTICAL);

        // Adapt to orientation
        if (parentActivity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT)
            recyclerView.setLayoutManager(gridLayoutManagerVertical);
        else
            recyclerView.setLayoutManager(gridLayoutManagerHorizontal);

        recyclerView.setAdapter(timeCursorAdapter);
    }
}
