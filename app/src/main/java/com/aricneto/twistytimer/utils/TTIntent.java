package com.aricneto.twistytimer.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.aricneto.twistify.BuildConfig;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The actions for the broadcast intents that notify listeners of changes to the data or to the
 * state of the application.
 *
 * @author damo
 */
public final class TTIntent {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TTIntent.class.getSimpleName();

    /**
     * The name prefix for all categories and actions to ensure that their names do not clash with
     * any system names.
     */
    private static final String BASE_PREFIX = "com.aricneto.twistytimer.";

    /**
     * The name prefix for all categories.
     */
    private static final String CATEGORY_PREFIX = BASE_PREFIX + "category.";

    /**
     * The name prefix for all actions.
     */
    private static final String ACTION_PREFIX = BASE_PREFIX + "action.";

    /**
     * The name prefix for all extras.
     */
    private static final String EXTRA_PREFIX = BASE_PREFIX + "extra.";

    /**
     * The category for intents that communicate interactions with, or changes to the state of, the
     * timer and other user-interface elements.
     */
    public static final String CATEGORY_UI_INTERACTIONS = CATEGORY_PREFIX + "UI_INTERACTIONS";

    /**
     * The category for intents that communicate changes to the solve time data, or to the
     * selection of the set of solve time data to be presented.
     */
    public static final String CATEGORY_TIME_DATA_CHANGES = CATEGORY_PREFIX + "TIME_DATA_CHANGES";

    /**
     * The category for intents that communicate changes to the algorithm data, or to the selection
     * of the set of data to be presented.
     */
    public static final String CATEGORY_ALG_DATA_CHANGES = CATEGORY_PREFIX + "ALG_DATA_CHANGES";

    /**
     * The category for intents that communicate alerts to the scramble loader, such as change to
     * the puzzle type or the need to prepare a new scramble.
     */
    public static final String CATEGORY_SCRAMBLE_ALERTS = CATEGORY_PREFIX + "SCRAMBLE_ALERTS";

    /**
     * The main application state has been changed. Receivers should apply the new state. The state
     * information can be retrieved using {@link #getMainState(Intent)}.
     */
    public static final String ACTION_MAIN_STATE_CHANGED = ACTION_PREFIX + "MAIN_STATE_CHANGED";

    /**
     * One new solve time has been added.
     */
    public static final String ACTION_TIME_ADDED = ACTION_PREFIX + "TIME_ADDED";

    /**
     * One or more solve times have been modified in unspecified ways. Modifications include adding
     * times (bulk import), deleting selected times, or changing the penalties, comments, history
     * status or other properties of one or more times. A full refresh of any displayed time data
     * may be required.
     */
    public static final String ACTION_TIMES_MODIFIED = ACTION_PREFIX + "TIMES_MODIFIED";

    /**
     * One or more solves have been moved from the current session to the history of all sessions.
     */
    public static final String ACTION_TIMES_MOVED_TO_HISTORY
            = ACTION_PREFIX + "TIMES_MOVED_TO_HISTORY";

    /**
     * Bootstrap the statistics loader, now that main state information is available and the
     * fragments are ready to receive the data. If the loader is already bootstrapped, this may be
     * ignored unless there is a change to the main state.
     */
    public static final String ACTION_BOOT_STATISTICS_LOADER
            = ACTION_PREFIX + "BOOT_STATISTICS_LOADER";

    /**
     * Bootstrap the chart statistics loader, now that main state information is available the
     * fragments are ready to receive the data. If the loader is already bootstrapped, this may be
     * ignored unless there is a change to the main state.
     */
    public static final String ACTION_BOOT_CHART_STATISTICS_LOADER
            = ACTION_PREFIX + "BOOT_CHART_STATISTICS_LOADER";

    /**
     * The tool-bar and tab strip should be hidden.
     */
    public static final String ACTION_HIDE_TOOLBAR = ACTION_PREFIX + "HIDE_TOOLBAR";

    /**
     * The tool-bar and tab strip should be shown.
     */
    public static final String ACTION_SHOW_TOOLBAR = ACTION_PREFIX + "SHOW_TOOLBAR";

    /**
     * The tool-bar and tab strip have been restored and are now shown. This action corresponds to
     * the end of the animation that shows those views if they have been hidden.
     */
    public static final String ACTION_TOOLBAR_RESTORED = ACTION_PREFIX + "TOOLBAR_RESTORED";

    /**
     * The tool bar button to generate a new scramble has been pressed, or the scrambles are being
     * generated automatically by the timer, and the receiver should perform that action.
     */
    public static final String ACTION_GENERATE_SCRAMBLE = ACTION_PREFIX + "GENERATE_SCRAMBLE";

    /**
     * A new scramble has been generated and the image for that scramble should now be generated.
     */
    public static final String ACTION_GENERATE_SCRAMBLE_IMAGE
            = ACTION_PREFIX + "GENERATE_SCRAMBLE_IMAGE";

    /**
     * Selection mode has been turned on for the list of times.
     */
    public static final String ACTION_SELECTION_MODE_ON = ACTION_PREFIX + "SELECTION_MODE_ON";

    /**
     * Selection mode has been turned off for the list of times.
     */
    public static final String ACTION_SELECTION_MODE_OFF = ACTION_PREFIX + "SELECTION_MODE_OFF";

    /**
     * An item in the list of times has been selected.
     */
    public static final String ACTION_TIME_SELECTED = ACTION_PREFIX + "TIME_SELECTED";

    /**
     * An item in the list of times has been unselected.
     */
    public static final String ACTION_TIME_UNSELECTED = ACTION_PREFIX + "TIME_UNSELECTED";

    /**
     * The user has chosen the action to delete all of the selected times. The receiver should
     * perform that operation and broadcast {@link #ACTION_TIMES_MODIFIED}.
     */
    public static final String ACTION_DELETE_SELECTED_TIMES
            = ACTION_PREFIX + "DELETE_SELECTED_TIMES";

    /**
     * One or more algorithms has been added, deleted or otherwise modified.
     */
    public static final String ACTION_ALGS_MODIFIED = ACTION_PREFIX + "ALGS_MODIFIED";

    /**
     * The name of an intent extra that can hold the main state information for the application.
     */
    public static final String EXTRA_MAIN_STATE = EXTRA_PREFIX + "MAIN_STATE";

    /**
     * The name of an intent extra that can hold the currently selected puzzle type.
     */
    public static final String EXTRA_PUZZLE_TYPE = EXTRA_PREFIX + "PUZZLE_TYPE";

    /**
     * The name of an intent extra that can hold the last generated scramble sequence string.
     */
    public static final String EXTRA_SCRAMBLE = EXTRA_PREFIX + "SCRAMBLE";

    /**
     * The name of an intent extra that can be used to record a {@link Solve}.
     */
    public static final String EXTRA_SOLVE = EXTRA_PREFIX + "SOLVE";

    /**
     * The actions that are allowed under each category. The category name is the key and the
     * corresponding entry is a collection of action names that are supported for that category.
     * An action may be supported by more than one category.
     */
    // NOTE: To match an "Intent", it is not sufficient for an "IntentFilter" to simply match all
    // categories defined on the intent, it must also match the action on the "Intent" (unless the
    // intent action is null, in which case it is always matched). For the purposes of receiving
    // local broadcast intents in this app, it is no harm to ensure that intents are not broadcast
    // with the wrong category, so requiring each category to have a defined list of supported
    // actions (for use when creating the "IntentFilter") makes things clearer. It also allows some
    // defensive checks in the "broadcast" methods that might highlight bugs in the code.
    private static final Map<String, String[]> ACTIONS_SUPPORTED_BY_CATEGORY
            = new HashMap<String, String[]>() {{
        put(CATEGORY_TIME_DATA_CHANGES, new String[] {
                ACTION_TIME_ADDED,
                ACTION_TIMES_MODIFIED,
                ACTION_TIMES_MOVED_TO_HISTORY,
                ACTION_MAIN_STATE_CHANGED,
                ACTION_BOOT_STATISTICS_LOADER,
                ACTION_BOOT_CHART_STATISTICS_LOADER,
        });

        put(CATEGORY_ALG_DATA_CHANGES, new String[] {
                ACTION_ALGS_MODIFIED,
        });

        put(CATEGORY_UI_INTERACTIONS, new String[] {
                ACTION_TIME_SELECTED,
                ACTION_TIME_UNSELECTED,
                ACTION_DELETE_SELECTED_TIMES,
                ACTION_SELECTION_MODE_ON,
                ACTION_SELECTION_MODE_OFF,
                ACTION_HIDE_TOOLBAR,
                ACTION_SHOW_TOOLBAR,
                ACTION_TOOLBAR_RESTORED,
                ACTION_GENERATE_SCRAMBLE,
        });

        put(CATEGORY_SCRAMBLE_ALERTS, new String[] {
                ACTION_GENERATE_SCRAMBLE,
                ACTION_GENERATE_SCRAMBLE_IMAGE,
        });
    }};

    /**
     * Private constructor to prevent instantiation of this class containing only constants and
     * utility methods.
     */
    private TTIntent() {
    }

    /**
     * A convenient wrapper for a broadcast receiver that holds the category of intents (groups of
     * actions) for which the receiver will be notified.
     */
    // NOTE: The goal of this class is to make a more obvious connection between the categories and
    // the receivers, as the category will be given in the code at the point where it instantiates
    // an instance of this receiver class. This places the category and actions close together.
    public abstract static class TTCategoryBroadcastReceiver extends BroadcastReceiver {
        /**
         * The intent category.
         */
        private final String mCategory;

        /**
         * Creates a new broadcast receiver for a single category of intent actions.
         *
         * @param category
         *     The category of intent actions to be notified to this receiver. Intent actions not
         *     in this category will not be notified.
         */
        public TTCategoryBroadcastReceiver(String category) {
            mCategory = category;
        }

        /**
         * Gets the category of the intent actions that will be matched by this broadcast receiver.
         *
         * @return The category.
         */
        public String getCategory() {
            return mCategory;
        }
    }

    /**
     * A convenient wrapper for fragments that use a broadcast receiver that will only notify the
     * fragment of an intent when the fragment is currently added to its activity. Only intent
     * actions within the specified category will be notified.
     */
    public abstract static class TTFragmentBroadcastReceiver extends TTCategoryBroadcastReceiver {
        /**
         * The fragment that is receiving the broadcasts.
         */
        private final Fragment mFragment;

        /**
         * Creates a new broadcast receiver to be used by a fragment. Matching broadcast intents
         * will only be notified to the fragment via {@link #onReceiveWhileAdded} if the fragment is
         * added to its activity at the time of the broadcast.
         *
         * @param fragment
         *     The fragment that will be receiving the broadcast intents.
         * @param category
         *     The category of
         */
        public TTFragmentBroadcastReceiver(Fragment fragment, String category) {
            super(category);
            mFragment = fragment;
        }

        /**
         * Notifies the receiver of a matching broadcast intent that is received while the fragment
         * is added to its activity. The receiver will only be notified of intents that require the
         * category configured, or intents that require no category. (The latter is not a use-case
         * that is expected in this application.)
         *
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        public abstract void onReceiveWhileAdded(Context context, Intent intent);

        /**
         * Notifies the receiver of a matching broadcast intent. This implementation will call
         * {@link #onReceiveWhileAdded(Context, Intent)} only while the fragment is currently added
         * to its activity, otherwise the intent will be ignored.
         *
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        // Make this final to make sure extensions only override "onReceiveWhileAdded".
        @Override
        public final void onReceive(Context context, Intent intent) {
            if (mFragment.isAdded()) {
                if (DEBUG_ME) Log.d(TAG, mFragment.getClass().getSimpleName()
                        + ": onReceiveWhileAdded: " + intent);
                onReceiveWhileAdded(context, intent);
            }
        }
    }

    /**
     * Registers a broadcast receiver. The receiver will only be notified of intents that require
     * the category given and only for the actions that are supported for that category.
     *
     * @param receiver
     *     The broadcast receiver to be registered.
     * @param category
     *     The category for the actions to be received. Must not be {@code null} and must be a
     *     supported category.
     *
     * @throws IllegalArgumentException
     *     If the category is {@code null}, or is not one of the supported categories.
     */
    private static void registerReceiver(BroadcastReceiver receiver, String category) {
        final String[] actions = ACTIONS_SUPPORTED_BY_CATEGORY.get(category);

        if (category == null || actions.length == 0) {
            throw new IllegalArgumentException("Category is not supported: " + category);
        }

        final IntentFilter filter = new IntentFilter();

        filter.addCategory(category);

        for (String action : actions) {
            // IntentFilter will only match Intents with one of these actions.
            filter.addAction(action);
        }

        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext())
                .registerReceiver(receiver, filter);
    }

    /**
     * Registers a category broadcast receiver. The receiver will only be notified of intents that
     * require the category defined for the {@code TTCategoryBroadcastReceiver} and only for the
     * actions supported by that category. This can also be used for the fragment-specific
     * {@link TTFragmentBroadcastReceiver}.
     *
     * @param receiver
     *     The category broadcast receiver to be registered.
     *
     * @throws IllegalArgumentException
     *     If the receiver does not define the name of a supported category.
     */
    public static void registerReceiver(TTCategoryBroadcastReceiver receiver) {
        registerReceiver(receiver, receiver.getCategory());
    }

    /**
     * Unregisters a broadcast receiver. Any further broadcast intent will be ignored.
     *
     * @param receiver The receiver to be unregistered.
     */
    public static void unregisterReceiver(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext()).unregisterReceiver(receiver);
    }

    /**
     * Broadcasts an intent for the given category and action. To add more details to the intent
     * (via intent extras), use a {@link BroadcastBuilder}.
     *
     * @param category The category of the action.
     * @param action   The action.
     */
    public static void broadcast(String category, String action) {
        builder(category, action).broadcast();
    }

    /**
     * Broadcasts a request notifying receivers that the main application state has changed.
     * The broadcast is for receivers monitoring actions for {@link #CATEGORY_TIME_DATA_CHANGES}.
     *
     * @param newMainState The new value for the main application state.
     */
    public static void broadcastMainStateChanged(@NonNull MainState newMainState) {
        builder(CATEGORY_TIME_DATA_CHANGES, ACTION_MAIN_STATE_CHANGED)
                .mainState(newMainState)
                .broadcast();
    }

    /**
     * Broadcasts a request to generate a new scramble sequence.
     *
     * @param puzzleType The type of the puzzle for which a sequence is required.
     */
    public static void broadcastNewScrambleRequest(@NonNull PuzzleType puzzleType) {
        builder(CATEGORY_SCRAMBLE_ALERTS, ACTION_GENERATE_SCRAMBLE)
                .puzzleType(puzzleType)
                .broadcast();
    }

    /**
     * Broadcasts a request to generate a new scramble image from a scramble sequence.
     *
     * @param puzzleType The type of the puzzle for which a scramble image is required.
     * @param scramble   The scramble sequence to be represented in the image.
     */
    public static void broadcastNewScrambleImageRequest(
            @NonNull PuzzleType puzzleType, @NonNull String scramble) {
        builder(CATEGORY_SCRAMBLE_ALERTS, ACTION_GENERATE_SCRAMBLE_IMAGE)
                .puzzleType(puzzleType)
                .scramble(scramble)
                .broadcast();
    }

    /**
     * Broadcasts a request to bootstrap the statistics loader, providing the initial main state
     * information. If the loader is already bootstrapped and it is consistent with the given main
     * state, it may ignore this request.
     *
     * @param mainState The main state information with which to boot the loader.
     */
    public static void broadcastBootStatisticsLoader(@NonNull MainState mainState) {
        builder(CATEGORY_TIME_DATA_CHANGES, ACTION_BOOT_STATISTICS_LOADER)
                .mainState(mainState)
                .broadcast();
    }

    /**
     * Broadcasts a request to bootstrap the chart statistics loader, providing the initial main
     * state information. If the loader is already bootstrapped and it is consistent with the given
     * main state, it may ignore this request.
     *
     * @param mainState The main state information with which to boot the loader.
     */
    public static void broadcastBootChartStatisticsLoader(@NonNull MainState mainState) {
        builder(CATEGORY_TIME_DATA_CHANGES, ACTION_BOOT_CHART_STATISTICS_LOADER)
                .mainState(mainState)
                .broadcast();
    }

    /**
     * Gets the main state from an intent extra.
     *
     * @param intent The intent from which to get the main state.
     * @return The main state, or {@code null} if the intent does not specify any main state.
     */
    public static MainState getMainState(Intent intent) {
        return (MainState) intent.getParcelableExtra(EXTRA_MAIN_STATE);
    }

    /**
     * Gets the puzzle type from an intent extra. If the main state was added as an extra, the main
     * state will be used as the source of the puzzle type if no specific extra for the puzzle type
     * can be found. The full main state can be retrieved by calling {@link #getMainState(Intent)}.
     *
     * @param intent
     *     The intent from which to get the puzzle type.
     *
     * @return
     *     The puzzle type, or {@code null} if the intent does not specify any puzzle type extra,
     *     either explicitly or via a {@code MainState} intent extra.
     */
    public static PuzzleType getPuzzleType(Intent intent) {
        PuzzleType puzzleType = null;

        // "EXTRA_PUZZLE_TYPE" takes priority over "EXTRA_MAIN_STATE" if both are set.
        if (intent.hasExtra(EXTRA_PUZZLE_TYPE)) {
            puzzleType = PuzzleType.forTypeName(intent.getStringExtra(EXTRA_PUZZLE_TYPE));
        }

        if (puzzleType == null) {
            final MainState mainState = getMainState(intent);

            if (mainState != null) {
                puzzleType = mainState.getPuzzleType();
            }
        }

        return puzzleType; // May still be null.
    }

    /**
     * Gets the scramble sequence from an intent extra.
     *
     * @param intent The intent from which to get the scramble.
     * @return The scramble, or {@code null} if the intent does not specify any scramble.
     */
    public static String getScramble(Intent intent) {
        return intent.getStringExtra(EXTRA_SCRAMBLE);
    }

    /**
     * Gets the solve specified in an intent extra.
     *
     * @param intent The intent from which to get the solve.
     * @return The solve, or {@code null} if the intent does not specify a solve.
     */
    public static Solve getSolve(Intent intent) {
        final Parcelable solve = intent.getParcelableExtra(EXTRA_SOLVE);

        return solve == null ? null : (Solve) solve;
    }

    /**
     * Creates a new broadcast builder for the given intent category and action.
     *
     * @param category The category of the action.
     * @param action   The action.
     *
     * @return The new broadcast builder.
     */
    public static BroadcastBuilder builder(@NonNull String category, @NonNull String action) {
        return new BroadcastBuilder(category, action);
    }

    /**
     * A builder for local broadcasts.
     *
     * @author damo
     */
    public static class BroadcastBuilder {
        /**
         * The intent that will be broadcast when building is complete.
         */
        private Intent mIntent;

        /**
         * Creates a new broadcast builder for the given intent category and action.
         *
         * @param category The category of the action.
         * @param action   The action.
         */
        private BroadcastBuilder(@NonNull String category, @NonNull String action) {
            mIntent = new Intent(action);
            mIntent.addCategory(category); // Will throw NPE if category is null, but that is OK.
        }

        /**
         * Broadcasts the intent configured by this builder.
         *
         * @throws IllegalStateException
         *     If the category specified on the intent does not support the defined action.
         */
        public void broadcast() {
            // For sanity, check that the category and action on the intent are supported. This
            // will unearth bugs in the code where actions have the wrong category and will not
            // end up where they are expected. Only do this if this is a debug build, as it would
            // be a waste of time in a release build.
            if (BuildConfig.DEBUG) {
                final String action = mIntent.getAction();

                if (action == null) {
                    throw new IllegalStateException("An intent action is expected.");
                }
                if (mIntent.getCategories().size() != 1) {
                    throw new IllegalStateException("Exactly one intent category is expected.");
                }

                final String category = mIntent.getCategories().iterator().next(); // First entry.
                final String[] actions = ACTIONS_SUPPORTED_BY_CATEGORY.get(category);

                if (!Arrays.asList(actions).contains(mIntent.getAction())) {
                    throw new IllegalStateException(
                            "Action '" + action + "' not allowed for category '" + category + "'.");
                }
            }

            if (DEBUG_ME) Log.d(TAG, "Broadcasting: " + mIntent);
            LocalBroadcastManager.getInstance(TwistyTimer.getAppContext()).sendBroadcast(mIntent);
        }

        /**
         * Sets extra that identifies the main state of the application. Changes to the state can
         * be broadcast and the receiver can retrieve the state by calling
         * {@link TTIntent#getMainState(Intent)}.
         *
         * @param state The new main state for the application.
         *
         * @return {@code this} broadcast builder, allowing method calls to be chained.
         */
        public BroadcastBuilder mainState(MainState state) {
            if (state != null) {
                mIntent.putExtra(EXTRA_MAIN_STATE, state);
            }

            return this;
        }

        /**
         * Sets extra that identifies the current puzzle type. If other main state information is
         * required, use {@link #mainState(MainState)} instead. Changes to the puzzle type can be
         * broadcast and the receiver can retrieve the state by calling
         * {@link TTIntent#getPuzzleType(Intent)}.
         *
         * @param puzzleType The new puzzle type.
         *
         * @return {@code this} broadcast builder, allowing method calls to be chained.
         */
        public BroadcastBuilder puzzleType(PuzzleType puzzleType) {
            if (puzzleType != null) {
                mIntent.putExtra(EXTRA_PUZZLE_TYPE, puzzleType.typeName());
            }

            return this;
        }

        /**
         * Sets an optional extra that identifies a scramble sequence related to the action of the
         * intent that will be broadcast. The receiver can call {@link TTIntent#getScramble(Intent)}
         * to retrieve the scramble from the intent.
         *
         * @param scramble The scramble sequence to be added to the broadcast intent.
         *
         * @return {@code this} broadcast builder, allowing method calls to be chained.
         */
        public BroadcastBuilder scramble(String scramble) {
            if (scramble != null) {
                mIntent.putExtra(EXTRA_SCRAMBLE , scramble);
            }

            return this;
        }

        /**
         * Sets an optional extra that identifies a solve time related to the action of the intent
         * that will be broadcast. The receiver can call {@link TTIntent#getSolve(Intent)} to
         * retrieve the solve from the intent.
         *
         * @param solve The solve to be added to the broadcast intent.
         *
         * @return {@code this} broadcast builder, allowing method calls to be chained.
         */
        public BroadcastBuilder solve(Solve solve) {
            if (solve != null) {
                // "Solve" implements "Parcelable" to allow it to be passed in an intent extra.
                mIntent.putExtra(EXTRA_SOLVE, solve);
            }

            return this;
        }
    }
}
