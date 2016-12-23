package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.PuzzleTypeSpinnerAdapter;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.layout.LockedViewPager;
import com.aricneto.twistytimer.layout.TimerMainLayout;
import com.aricneto.twistytimer.layout.TimerTabLayout;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.stats.StatisticsLoader;
import com.aricneto.twistytimer.utils.FireAndForgetExecutor;
import com.aricneto.twistytimer.utils.LoggingLoaderCallbacks;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.aricneto.twistytimer.utils.TimerPage;
import com.github.ksoichiro.android.observablescrollview
    .CacheFragmentStatePagerAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.*;

public class TimerMainFragment extends BaseMainFragment
       implements OnBackPressedInFragmentListener {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerMainFragment.class.getSimpleName();

    private Unbinder mUnbinder;
    @BindView(R.id.timer_main_root)   TimerMainLayout mvMainRoot;
    @BindView(R.id.toolbar_container) ViewGroup       mvToolbarContainer;
    @BindView(R.id.toolbar)           Toolbar         mvToolbar;
    @BindView(R.id.pager)             LockedViewPager mvViewPager;
    @BindView(R.id.main_tabs)         TimerTabLayout  mvTabLayout;

    private ActionMode mActionMode;

    private int mSelectedSolvesCount = 0;

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            mode.getMenuInflater().inflate(R.menu.menu_list_callback, menu);

            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but may be called multiple times if the mode is
        // invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getActivity().getWindow().setStatusBarColor(
                    ThemeUtils.fetchAttrColor(
                        getContext(), R.attr.colorPrimaryDark));
            }
            return true; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    // Receiver will delete times and then broadcast an intent.
                    broadcast(CATEGORY_UI_INTERACTIONS,
                              ACTION_DELETE_SELECTED_SOLVES);
                    mode.finish();
                    return true;

                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    };

    // Receives broadcasts about changes to the time user interface.
    private TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceiveWhileAdded(): " + intent);

            switch (intent.getAction()) {
                case ACTION_HIDE_TOOLBAR:
                    setTabSwitchingEnabled(false);
                    mvMainRoot.hideToolbar(null);
                    break;

                case ACTION_SHOW_TOOLBAR:
                    // NOTE: If "ACTION_HIDE_TOOLBAR" is received before
                    // "showToolbar()" completes its animation, this "Runnable"
                    // will not be executed and "ACTION_TOOLBAR_RESTORED" will
                    // not be broadcast.
                    mvMainRoot.showToolbar(new Runnable() {
                        @Override
                        public void run() {
                            broadcast(CATEGORY_UI_INTERACTIONS,
                                      ACTION_TOOLBAR_RESTORED);
                            setTabSwitchingEnabled(true);
                        }
                    });
                    break;

                case ACTION_SELECTION_MODE_ON:
                    mSelectedSolvesCount = 0;
                    mActionMode = mvToolbar.startActionMode(actionModeCallback);
                    break;

                case ACTION_SELECTION_MODE_OFF:
                    mSelectedSolvesCount = 0;
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                    break;

                case ACTION_SOLVE_SELECTED:
                case ACTION_SOLVE_UNSELECTED:
                    mSelectedSolvesCount
                        += intent.getAction().equals(ACTION_SOLVE_SELECTED)
                           ?  1 : -1;
                    mActionMode.setTitle(mSelectedSolvesCount + " "
                                         + getString(R.string.selected_list));
                    break;
            }
        }
    };

    public static TimerMainFragment newInstance() {
        final TimerMainFragment fragment = new TimerMainFragment();
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(" + savedInstanceState + ')');
        final View root = inflater.inflate(
            R.layout.fragment_timer_main, container, false);
        mUnbinder = ButterKnife.bind(this, root);

        setUpToolbarForFragment(mvToolbar);

        mvViewPager.setAdapter(new NavigationAdapter(getFragmentManager()));
        // Keep all fragments alive when not currently shown on the screen.
        mvViewPager.setOffscreenPageLimit(TimerPage.getNumPages() - 1);
        mvTabLayout.setupWithViewPager(mvViewPager);

        mvViewPager.addOnPageChangeListener(
            new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(
                int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setUpPage(TimerPage.forTabPosition(position),
                    getMainState(), inflater);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // NOTE: A "swipe" gesture was detected and the page changed.
                // In "TimerFragment", the view that relayed touches to the
                // puzzle timer at the beginning of the "swipe" gesture will
                // next receive "MotionEvent.ACTION_CANCEL" automatically and
                // react appropriately, so nothing needs to be done here.
            }
        });

        // Register a receiver to update if something has changed
        registerReceiver(mUIInteractionReceiver);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated("
                                 + savedInstanceState + ')');
        super.onActivityCreated(savedInstanceState);

        // The "StatisticsLoader" is managed from this fragment. The loader
        // will not perform the initial load until it receives a broadcast
        // request to do so. That initial "boot" request will be sent from
        // "onResume()" (if another request does not get there first).
        if (DEBUG_ME) Log.d(TAG,
            "onActivityCreated() -> initLoader: STATISTICS LOADER");
        getLoaderManager().initLoader(MainActivity.STATISTICS_LOADER_ID, null,
            new LoggingLoaderCallbacks<Statistics>(TAG, "STATISTICS LOADER") {
                @Override
                public Loader<Statistics> onCreateLoader(int id, Bundle args) {
                    if (DEBUG_ME) super.onCreateLoader(id, args);
                    return new StatisticsLoader(getContext());
                }

                @Override
                public void onLoadFinished(Loader<Statistics> loader,
                                           Statistics data) {
                    if (DEBUG_ME) super.onLoadFinished(loader, data);
                    // Other fragments can get the statistics from the cache
                    // when they are created and can register themselves as
                    // observers of further updates.
                    StatisticsCache.getInstance().updateAndNotify(data);
                }

                @Override
                public void onLoaderReset(Loader<Statistics> loader) {
                    if (DEBUG_ME) super.onLoaderReset(loader);
                    // Clear the cache and notify all observers that the
                    // statistics are "null".
                    StatisticsCache.getInstance().updateAndNotify(null);
                }
            });

        // Sets up the toolbar with the icons appropriate to the current page.
        mvToolbar.post(new Runnable() {
            @Override
            public void run() {
                final MainState mainState = getMainState();

                setUpPuzzleTypeToolbarItem(mainState);
                setUpPage(TimerPage.forTabPosition(
                    mvViewPager.getCurrentItem()), mainState,
                    LayoutInflater.from(getActivity()));
            }
        });
    }

    @Override
    public void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume()");
        super.onResume();

        broadcastBootStatisticsLoader(getMainState());
    }

    /**
     * Passes on the "Back" button press event to subordinate fragments and
     * indicates if any fragment consumed the event.
     *
     * @return
     *     {@code true} if the "Back" button press was consumed and no further
     *     action should be taken; or {@code false} if the "Back" button press
     *     was ignored and the caller should propagate it to the next interested
     *     party.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");

        if (mvViewPager != null) {
            final NavigationAdapter pagerAdapter
                = (NavigationAdapter) mvViewPager.getAdapter();

            return pagerAdapter != null
                   && pagerAdapter.dispatchOnBackPressedInFragment();
        }
        return false;
    }

    @Override
    public void onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()");
        super.onDetach();
        unregisterReceiver(mUIInteractionReceiver);
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onMainStateChanged(
            @NonNull MainState newMainState, @NonNull MainState oldMainState) {
        super.onMainStateChanged(newMainState, oldMainState); // For logging.

        // Show toast with puzzle type and solve category (if either has
        // changed). This will allow the category editing and selection dialogs
        // to have their changes reported automatically. A change to the history
        // switch will not be notified.
        if (!newMainState.equalsIgnoreHistory(oldMainState)) {
            Toast.makeText(
                    getContext(),
                    newMainState.getPuzzleType().getFullName()
                            + ": " + newMainState.getSolveCategory(),
                    Toast.LENGTH_SHORT)
                    .show();
        }

        // Reload the loaders, refresh the UI elements, etc.
        // FIXME: The calls usually issued after a state change. Are these both
        // necessary?
//        viewPager.setAdapter(viewPagerAdapter);
//        viewPager.setCurrentItem(currentPage.ordinal());

        // Send a signal to the chart statistics loader to ensure it is in sync
        // with the main state. It it is already in sync (perhaps due to another
        // fragment broadcasting the same intent), it will ignore the request.
        broadcastMainStateChanged(newMainState);
    }

    /**
     * Enables or disables the ability to switch between tabs, either by
     * clicking on a tab or by swiping across the page. The swiping behaviour
     * will only be enabled if the corresponding user preference is also
     * enabled.
     *
     * @param enable
     *     {@code true} to enable switching between tabs; or {@code false} to
     *     disable switching between tabs.
     */
    private void setTabSwitchingEnabled(boolean enable) {
        mvViewPager.setPagingEnabled(
            enable && Prefs.getBoolean(
                R.string.pk_tab_swiping_enabled,
                R.bool.default_tab_swiping_enabled));
        mvToolbar.setEnabled(enable);
    }

    /**
     * Sets up the tool-bar items to match the selected page.
     *
     * @param timerPage
     *     The selected timer page.
     * @param inflater
     *     The layout inflater to use to create the history switch, if
     *     applicable.
     */
    private void setUpPage(
            @NonNull TimerPage timerPage, @NonNull MainState mainState,
            LayoutInflater inflater) {
        if (DEBUG_ME) Log.d(TAG, "setUpPage(timerPage=" + timerPage + ")");

        if (mActionMode != null) {
            mActionMode.finish();
        }

        if (mvToolbar == null) {
            return;
        }

        mvToolbar.getMenu().clear();

        switch (timerPage) {
            case TIMER:
                // Scramble icon.
                // REVIEW: This was still shown when scrambles were disabled.
                // That would seem to have been OK, as a scramble could be
                // requested "manually" at any time, rather than relying on
                // the "automatic" scramble before each new solve. However,
                // what did not make sense was that "manually" requested
                // scrambles did not work (i.e., the menu item did nothing)
                // unless "automatic" scrambles were enabled. Therefore,
                // either this menu item should trigger a scramble on
                // request, overriding the setting for "automatic" scrambles,
                // or else it should be hidden if scrambles are disabled. For
                // now, it will be hidden, as that is the simplest change
                // that just removes the confusingly inert button and neither
                // adds nor removes functionality.
                if (Prefs.isScrambleEnabled()) {
                    mvToolbar
                        .getMenu()
                        .add(0, 5, 0, R.string.scramble_action)
                        .setIcon(R.drawable.ic_dice_white_24dp)
                        .setOnMenuItemClickListener(
                            new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                broadcast(CATEGORY_UI_INTERACTIONS,
                                          ACTION_GENERATE_SCRAMBLE);
                                return true;
                            }
                        })
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
                break;

            case TIMES_LIST:
            case CHART_AND_STATS:
                // Add menu icons
                setUpHistorySwitchToolbarItem(mainState, inflater);
                break;
        }

        setUpSolveCategoryToolbarItem(mainState);
    }

    private void setUpSolveCategoryToolbarItem(
            @NonNull final MainState mainState) {
        mvToolbar
            .getMenu()
            .add(0, 6, 0, R.string.type)
            .setIcon(R.drawable.ic_tag_outline_white_24dp)
            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    showSolveCategoryDialog(mainState);
                    return true;
                }
            })
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    private void showSolveCategoryDialog(@NonNull MainState mainState) {
        // Spell these out to make it clearer what is going on later.
        final PuzzleType currentPuzzleType = mainState.getPuzzleType();
        final String currentSolveCategory = mainState.getSolveCategory();

        final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
        final List<String> solveCategories
            = dbHandler.getAllCategoriesForType(currentPuzzleType);
        final int currentSolveCategoryIndex
            = solveCategories.indexOf(mainState.getSolveCategory());

        final MaterialDialog createCategoryDialog
            = new MaterialDialog.Builder(getContext())
            .title(R.string.enter_type_name)
            .inputRange(1, 16) // Show that at least one character is required.
            .input(R.string.enter_type_name, 0, false,
                   new MaterialDialog.InputCallback() {
                @Override
                public void onInput(@NonNull MaterialDialog materialDialog,
                                    final CharSequence newSolveCategory) {
                    // This can run in the background, as successful creation
                    // can be assumed.
                    FireAndForgetExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            dbHandler.addSolveCategory(
                                currentPuzzleType, newSolveCategory);
                        }
                    });
                    fireOnMainStateChangedPiecemeal(
                        null, newSolveCategory, null);
                }
            })
            .build();

        final MaterialDialog removeCategoryDialog
            = new MaterialDialog.Builder(getContext())
            .title(R.string.remove_category_title)
            .negativeText(R.string.action_cancel)
            .autoDismiss(true)
            .items(solveCategories)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog materialDialog,
                                        View view, int i,
                                        final CharSequence categoryToDelete) {
                    new MaterialDialog.Builder(getContext())
                        .title(R.string.remove_category_confirmation)
                        .content(getString(
                            R.string.remove_category_confirmation_content)
                                 + " \"" + categoryToDelete + "\"?\n"
                                 + getString(
                            R.string.remove_category_confirmation_content_continuation))
                        .positiveText(R.string.action_remove)
                        .negativeText(R.string.action_cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog,
                                                @NonNull DialogAction which) {
                                // If the deleted category is the same as the
                                // current category, then a new current category
                                // must be set. The suggested category returned
                                // from "deleteSolveCategory" is used as the new
                                // current category, but the DB access must be
                                // synchronous.
                                if (currentSolveCategory.equals(
                                        categoryToDelete.toString())) {
                                    // Changes to the main state will be
                                    // notified, so the deletion of solves in
                                    // this category will be notified to
                                    // loaders, etc.
                                    fireOnMainStateChangedPiecemeal(
                                        null,
                                        dbHandler.deleteSolveCategory(
                                            currentPuzzleType, categoryToDelete,
                                            currentSolveCategory),
                                        null);
                                } else {
                                    // The current solve category was not
                                    // deleted, so there is no change to the
                                    // main state and the delete can be
                                    // asynchronous.
                                    FireAndForgetExecutor.execute(
                                        new Runnable() {
                                        @Override
                                        public void run() {
                                            dbHandler.deleteSolveCategory(
                                                currentPuzzleType,
                                                categoryToDelete,
                                                currentSolveCategory);
                                        }
                                    });
                                }
                            }
                        })
                        .show();
                }
            })
            .build();

        final MaterialDialog renameCategoryDialog
            = new MaterialDialog.Builder(getContext())
            .title(R.string.rename_category_title)
            .negativeText(R.string.action_cancel)
            .autoDismiss(true)
            .items(solveCategories)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog materialDialog,
                                        View view, int i,
                                        final CharSequence oldCategoryName) {
                    new MaterialDialog.Builder(getContext())
                        .title(R.string.enter_new_name_dialog)
                        .positiveText(R.string.action_done)
                        .negativeText(R.string.action_cancel)
                        // Show that at least one character is required.
                        .inputRange(1, 16)
                        .input("", oldCategoryName,
                                false, // Do not allow empty input.
                                new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(
                                    @NonNull MaterialDialog dialog,
                                    final CharSequence newCategoryName) {
                                // This can run in the background, as no result
                                // is needed.
                                FireAndForgetExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        dbHandler.renameSolveCategory(
                                            currentPuzzleType, oldCategoryName,
                                            newCategoryName);
                                    }
                                });

                                // If the currently-selected category has
                                // been renamed, then that will need to be
                                // notified. If not, then there is no state
                                // change, as the renamed category was not
                                // the selected category.
                                if (currentSolveCategory.equals(
                                        oldCategoryName)) {
                                    fireOnMainStateChangedPiecemeal(
                                        null, newCategoryName, null);
                                }
                            }
                        })
                        .show();
                }
            })
            .build();

        // The main solve category selection dialog. This dialog is used to
        // select a category or to launch the "New", "Rename" or "Delete"
        // dialogs. When any category or button is pressed, this "main"
        // dialog is dismissed automatically. Therefore, if any changes are
        // made, such as deleting a category, the flow does not return to
        // this dialog, so there is no need to update "solveCategories" to
        // keep it in sync with the changes. That reload will happen before
        // the next time this dialog is opened anew (above, in this method).
        new MaterialDialog.Builder(getContext())
            .title(R.string.select_solve_type)
            .autoDismiss(true) // Same as the default, but make it clearer.
            .positiveText(R.string.w_new_category)
            .negativeText(R.string.action_rename)
            .neutralText(R.string.action_remove)
            .neutralColor(ContextCompat.getColor(
                getContext(), R.color.black_secondary))
            .negativeColor(ContextCompat.getColor(
                getContext(), R.color.black_secondary))
            .items(solveCategories)
            .alwaysCallSingleChoiceCallback()
            .itemsCallbackSingleChoice(currentSolveCategoryIndex,
                    new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog,
                                           View itemView, int which,
                                           CharSequence newSolveCategory) {
                    fireOnMainStateChangedPiecemeal(
                        null, newSolveCategory, null);
                    dialog.dismiss();
                    return true;
                }
            })
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog,
                                    @NonNull DialogAction which) {
                    createCategoryDialog.show();
                }
            })
            .onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog,
                                    @NonNull DialogAction which) {
                    renameCategoryDialog.show();
                }
            })
            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog,
                                    @NonNull DialogAction which) {
                    removeCategoryDialog.show();
                }
            })
            .show();
    }

    private void setUpHistorySwitchToolbarItem(
            @NonNull MainState mainState, LayoutInflater inflater) {
        @SuppressLint("InflateParams") // Will pass to "setActionView" below.
        final CompoundButton historySwitch = (CompoundButton) inflater.inflate(
            R.layout.toolbar_pin_switch, null);

        mvToolbar
            .getMenu()
            .add(0, 7, 0, "Scope")
            .setActionView(historySwitch)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        historySwitch.setChecked(mainState.isHistoryEnabled());
        historySwitch.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                fireOnMainStateChangedPiecemeal(null, null, isChecked);
            }
        });
    }

    private void setUpPuzzleTypeToolbarItem(
            @NonNull final MainState mainState) {
        final View spinnerContainer = LayoutInflater.from(getActivity())
                .inflate(R.layout.toolbar_spinner, mvToolbar, false);
        final ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);

        final Spinner spinner
            = (Spinner) spinnerContainer.findViewById(R.id.toolbar_spinner);

        spinner.setAdapter(
            PuzzleTypeSpinnerAdapter.createForActionBar(getActivity()));
        mvToolbar.addView(spinnerContainer, layoutParams);

        // Set the selected position before setting the listener. If the
        // selected position is not set, it will be set later during layout
        // and fire the listener. That will cause the three fragments nested
        // in the ViewPager to be destroyed and re-created, together with all
        // of their loaders and background tasks, slowing down the start-up
        // of the application.
        //
        // If "[Abs]Spinner.setSelection(int)" is called, this problem is not
        // solved. Therefore, call "[Abs]Spinner.setSelection(int, boolean)" and
        // pass "false" to disable animation. AFAIK, the former method will post
        // a layout request, which will be handled after this method
        // ("handleHeaderSpinner") returns, but the latter method will perform
        // a layout directly before it returns to this method. It is the layout
        // that triggers the unwanted call to "onItemSelected", so the latter
        // method ensures the layout completes before the listener is added in
        // the next statement. See http://stackoverflow.com/a/17336944.
        //
        // To see all this in action, enable debug logging in the fragments
        // by setting "DEBUG_ME" to true in each and then watch the log to
        // see fragments being created twice when the application starts up
        // if the following "setSelection" call is commented out.
        spinner.setSelection(mainState.getPuzzleType().ordinal(), false);

        spinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                if (DEBUG_ME) Log.d(TAG, "onItemSelected(" + position + ")");
                fireOnMainStateChangedPiecemeal(
                    (PuzzleType) parent.getItemAtPosition(position),
                    null, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    protected class NavigationAdapter extends CacheFragmentStatePagerAdapter {

        NavigationAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        protected Fragment createItem(int position) {
            if (DEBUG_ME) Log.d(TAG,
                "NavigationAdapter.createItem(" + position + ")");

            return TimerPage.forTabPosition(position).createPageFragment();
        }

        /**
         * Notifies each fragment (that is listening) that the "Back" button
         * has been pressed. Stops when the first fragment consumes the event.
         *
         * @return
         *     {@code true} if any fragment consumed the "Back" button press
         *     event; or {@code false} if the event was not consumed by any
         *     fragment.
         */
        boolean dispatchOnBackPressedInFragment() {
            if (DEBUG_ME) Log.d(TAG,
                "NavigationAdapter.dispatchOnBackPressedInFragment()");
            boolean isConsumed = false;

            for (int p = 0; p < TimerPage.getNumPages() && !isConsumed; p++) {
                final Fragment fragment = getItemAt(p);

                if (fragment instanceof OnBackPressedInFragmentListener) {
                    isConsumed = ((OnBackPressedInFragmentListener) fragment)
                        .onBackPressedInFragment();
                }
            }

            return isConsumed;
        }

        @Override
        public int getCount() {
            return TimerPage.getNumPages();
        }
    }
}
