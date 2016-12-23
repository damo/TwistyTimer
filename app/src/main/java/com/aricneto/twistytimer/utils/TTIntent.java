package com.aricneto.twistytimer.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.aricneto.twistify.BuildConfig;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.scramble.ScrambleData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The actions for the broadcast intents that notify listeners of changes to
 * the data or to the state of the application.
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
     * The name prefix for all categories and actions to ensure that their
     * names do not clash with any system names.
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
     * The category for intents that communicate interactions with, or changes
     * to the state of, the timer and other user-interface elements.
     */
    public static final String CATEGORY_UI_INTERACTIONS
        = CATEGORY_PREFIX + "UI_INTERACTIONS";

    /**
     * The category for intents that communicate changes to the solve time data,
     * or to the selection of the set of solve time data to be presented.
     */
    public static final String CATEGORY_SOLVE_DATA_CHANGES
        = CATEGORY_PREFIX + "SOLVE_DATA_CHANGES";

    /**
     * The category for intents that communicate changes to the algorithm data,
     * or to the selection of the set of data to be presented.
     */
    public static final String CATEGORY_ALG_DATA_CHANGES
        = CATEGORY_PREFIX + "ALG_DATA_CHANGES";

    /**
     * The category for intents that communicate alerts to the scramble loader,
     * such as change to the puzzle type or the need to prepare a new scramble.
     */
    public static final String CATEGORY_SCRAMBLE_ALERTS
        = CATEGORY_PREFIX + "SCRAMBLE_ALERTS";

    /**
     * The main application state has been changed. Receivers should apply the
     * new main state, which is added as an extra to the intent and can be
     * retrieved using {@link #getMainState(Intent)}.
     */
    public static final String ACTION_MAIN_STATE_CHANGED
        = ACTION_PREFIX + "MAIN_STATE_CHANGED";

    /**
     * One new solve time has been added to the database. The broadcast intent
     * will include a {@link Solve} instance representing the database record
     * that was added. The {@code Solve} instance will report its newly-assigned
     * database record ID.
     */
    public static final String ACTION_ONE_SOLVE_ADDED
        = ACTION_PREFIX + "ONE_SOLVE_ADDED";

    /**
     * One or more new solve times have been added. This is notified for bulk
     * operations. It is possible for such an operation to contain only a single
     * new solve, but {@link #ACTION_ONE_SOLVE_ADDED} is <i>not</i> notified in
     * the case of a bulk operation. The intent should include a puzzle type and
     * solve category, but only if these are the same for all of the solves that
     * were added, otherwise neither value should be provided. The intent will
     * <i>not</i> include any {@code Solve} instances or any reference to the
     * database IDs of the added solve records.
     */
    public static final String ACTION_MANY_SOLVES_ADDED
        = ACTION_PREFIX + "MANY_SOLVES_ADDED";

    /**
     * One solve time has been updated with changes made to unspecified
     * properties. This may include an update to the "history" flag of the
     * solve, but {@link #ACTION_SOLVES_MOVED_TO_HISTORY} may alternatively be
     * notified in that case if no other properties of the solve are updated.
     * The broadcast intent will include a {@link Solve} instance representing
     * the database record that was updated. The {@code Solve} instance will
     * report its (unchanged) database record ID.
     */
    public static final String ACTION_ONE_SOLVE_UPDATED
        = ACTION_PREFIX + "ONE_SOLVE_UPDATED";

    /**
     * One solve time has been deleted from the database. The broadcast intent
     * will include a {@link Solve} instance representing the database record
     * that was deleted. The {@code Solve} instance may still report its old
     * database record ID, but, as that record was deleted, the ID should not
     * be used for any attempt to subsequently re-create or update the deleted
     * solve record. However, the solve record ID may be useful when updating
     * the user interface to reflect the deletion of that specific record.
     */
    public static final String ACTION_ONE_SOLVE_DELETED
        = ACTION_PREFIX + "ONE_SOLVE_DELETED";

    /**
     * One or more solve times have been deleted. This is notified for bulk
     * operations. It is possible for such an operation to delete only a single
     * solve, but {@link #ACTION_ONE_SOLVE_DELETED} is <i>not</i> notified in
     * the case of a bulk operation that affects only one solve. The intent
     * should include a puzzle type and solve category, but only if these are
     * the same for all of the solves that were deleted, otherwise neither
     * value should be provided. The intent will <i>not</i> include any
     * {@code Solve} instances or any reference to the database IDs of the
     * deleted solve records. If the user interface is displaying the details
     * of a solve, it must be assumed that that solve may have been deleted,
     * unless the puzzle type and solve category are given and do not match
     * those of the displayed solve.
     */
    public static final String ACTION_MANY_SOLVES_DELETED
        = ACTION_PREFIX + "MANY_SOLVES_DELETED";

    /**
     * One or more solve times have been moved from the current session to the
     * history of all sessions. This is notified for bulk operations. It is
     * possible for such an operation to delete only a single solve, but
     * {@link #ACTION_ONE_SOLVE_UPDATED} is <i>not</i> notified in the case of
     * a bulk operation that affects only one solve. The broadcast intent will
     * include the puzzle type and solve category of the solves that were moved.
     * All solves will be assigned to the same puzzle type and category. The
     * intent will <i>not</i> include any {@code Solve} instances or any
     * reference to the database IDs of the updated solve records. If the user
     * interface is displaying the details of a solve, it must be assumed that
     * that solve may have been moved, unless the puzzle type and solve category
     * do not match those of the displayed solve.
     */
    public static final String ACTION_SOLVES_MOVED_TO_HISTORY
        = ACTION_PREFIX + "SOLVES_MOVED_TO_HISTORY";

    /**
     * One solve has been verified to exist in the database. The broadcast
     * intent will include a {@link Solve} instance representing the up-to-date
     * contents of the database record that was verified to exist.
     */
    public static final String ACTION_SOLVE_VERIFIED
        = ACTION_PREFIX + "ACTION_SOLVE_VERIFIED";

    /**
     * One solve has <i>not</i> been verified to exist in the database. Either
     * there is no solve record matching the requested ID, or the record is a
     * "fake" solve record that defines a category name and is not a real solve
     * result. The broadcast intent will include the solve ID that was provided
     * when the verification was requested.
     */
    public static final String ACTION_SOLVE_NOT_VERIFIED
        = ACTION_PREFIX + "ACTION_SOLVE_NOT_VERIFIED";

    /**
     * Bootstrap the statistics loader, now that main state information is
     * available and the fragments are ready to receive the data. If the loader
     * is already bootstrapped, this may be ignored unless there is a change to
     * the main state. An intent extra will identify the main state at the time
     * of the request. Access that value with {@link #getMainState(Intent)}.
     */
    public static final String ACTION_BOOT_STATISTICS_LOADER
        = ACTION_PREFIX + "BOOT_STATISTICS_LOADER";

    /**
     * Bootstrap the chart statistics loader, now that main state information
     * is available the fragments are ready to receive the data. If the loader
     * is already bootstrapped, this may be ignored unless there is a change to
     * the main state. An intent extra will identify the main state at the time
     * of the request. Access that value with {@link #getMainState(Intent)}.
     */
    public static final String ACTION_BOOT_CHART_STATISTICS_LOADER
        = ACTION_PREFIX + "BOOT_CHART_STATISTICS_LOADER";

    /**
     * The tool-bar and tab strip should be hidden. No intent extras are
     * required.
     */
    public static final String ACTION_HIDE_TOOLBAR
        = ACTION_PREFIX + "HIDE_TOOLBAR";

    /**
     * The tool-bar and tab strip should be shown. No intent extras are
     * required.
     */
    public static final String ACTION_SHOW_TOOLBAR
        = ACTION_PREFIX + "SHOW_TOOLBAR";

    /**
     * The tool-bar and tab strip have been restored and are now shown. This
     * action corresponds to the end of the animation that shows those views
     * if they have been hidden. No intent extras are required.
     */
    public static final String ACTION_TOOLBAR_RESTORED
        = ACTION_PREFIX + "TOOLBAR_RESTORED";

    /**
     * The tool-bar button to generate a new scramble has been pressed, or the
     * scrambles are being generated automatically by the timer, and the
     * receiver should perform that action. An intent extra will identify the
     * puzzle type for which to generate a scramble. The value of the extra can
     * be retrieved by calling {@link #getPuzzleType(Intent)}.
     */
    public static final String ACTION_GENERATE_SCRAMBLE
        = ACTION_PREFIX + "GENERATE_SCRAMBLE";

    /**
     * A new scramble has been generated and the image for that scramble should
     * now be generated. An intent extra will identify the puzzle type and
     * scramble sequence to be represented by the generated image. Retrieve the
     * scramble data with {@link #getScrambleData(Intent)}.
     */
    public static final String ACTION_GENERATE_SCRAMBLE_IMAGE
        = ACTION_PREFIX + "GENERATE_SCRAMBLE_IMAGE";

    /**
     * Selection mode has been turned on for the list of times. No intent
     * extras are required.
     */
    public static final String ACTION_SELECTION_MODE_ON
        = ACTION_PREFIX + "SELECTION_MODE_ON";

    /**
     * Selection mode has been turned off for the list of times. No intent
     * extras are required.
     */
    public static final String ACTION_SELECTION_MODE_OFF
        = ACTION_PREFIX + "SELECTION_MODE_OFF";

    /**
     * An item in the list of solve times has been selected. No intent extras
     * are required.
     */
    public static final String ACTION_SOLVE_SELECTED
        = ACTION_PREFIX + "SOLVE_SELECTED";

    /**
     * An item in the list of solve times has been unselected. No intent extras
     * are required.
     */
    public static final String ACTION_SOLVE_UNSELECTED
        = ACTION_PREFIX + "SOLVE_UNSELECTED";

    /**
     * The user has chosen the action to delete all of the selected solve
     * times. The receiver should perform that operation and broadcast
     * {@link #ACTION_MANY_SOLVES_DELETED}. No intent extras are required.
     */
    public static final String ACTION_DELETE_SELECTED_SOLVES
        = ACTION_PREFIX + "DELETE_SELECTED_SOLVES";

    /**
     * One or more algorithms have been added, deleted or otherwise modified.
     * No intent extras are required.
     */
    public static final String ACTION_ALGS_MODIFIED
        = ACTION_PREFIX + "ALGS_MODIFIED";

    /**
     * The name of an intent extra that can hold the main state information for
     * the application.
     */
    private static final String EXTRA_MAIN_STATE = EXTRA_PREFIX + "MAIN_STATE";

    /**
     * The name of an intent extra that can hold the puzzle type associated
     * with the action notified by the intent.
     */
    private static final String EXTRA_PUZZLE_TYPE
        = EXTRA_PREFIX + "PUZZLE_TYPE";

    /**
     * The name of an intent extra that can hold the solve category associated
     * with the action notified by the intent.
     */
    private static final String EXTRA_SOLVE_CATEGORY
        = EXTRA_PREFIX + "SOLVE_CATEGORY";

    /**
     * The name of an intent extra that can hold the last generated scramble
     * data including the scramble sequence and puzzle type. Note that the
     * scramble image is not preserved if the scramble data is passed as an
     * intent extra.
     */
    private static final String EXTRA_SCRAMBLE_DATA
        = EXTRA_PREFIX + "SCRAMBLE_DATA";

    /**
     * The name of an intent extra that can be used to record a {@link Solve}.
     */
    private static final String EXTRA_SOLVE = EXTRA_PREFIX + "SOLVE";

    /**
     * The name of an intent extra that can be used to record the ID of a solve
     * record.
     */
    private static final String EXTRA_SOLVE_ID = EXTRA_PREFIX + "SOLVE_ID";

    /**
     * The actions that are allowed under each category. The category name is
     * the key and the corresponding entry is a collection of action names that
     * are supported for that category. An action may be supported by more than
     * one category.
     */
    // NOTE: To match an "Intent", it is not sufficient for an "IntentFilter"
    // to simply match all categories defined on the intent, it must also
    // match the action on the "Intent" (unless the intent action is null, in
    // which case it is always matched). For the purposes of receiving local
    // broadcast intents in this app, it is no harm to ensure that intents
    // are not broadcast with the wrong category, so requiring each category
    // to have a defined list of supported actions (for use when creating the
    // "IntentFilter") makes things clearer. It also allows some defensive
    // checks in the "broadcast" methods that might highlight bugs in the code.
    private static final Map<String, String[]> ACTIONS_SUPPORTED_BY_CATEGORY
        = new HashMap<String, String[]>() {{
        put(CATEGORY_SOLVE_DATA_CHANGES, new String[] {
            ACTION_ONE_SOLVE_ADDED,
            ACTION_MANY_SOLVES_ADDED,
            ACTION_ONE_SOLVE_UPDATED,
            ACTION_ONE_SOLVE_DELETED,
            ACTION_MANY_SOLVES_DELETED,
            ACTION_SOLVES_MOVED_TO_HISTORY,
            ACTION_MAIN_STATE_CHANGED,
            ACTION_BOOT_STATISTICS_LOADER,
            ACTION_BOOT_CHART_STATISTICS_LOADER,
            ACTION_SOLVE_VERIFIED,
            ACTION_SOLVE_NOT_VERIFIED,
            });

        put(CATEGORY_ALG_DATA_CHANGES, new String[] {
            ACTION_ALGS_MODIFIED,
            });

        put(CATEGORY_UI_INTERACTIONS, new String[] {
            ACTION_SOLVE_SELECTED,
            ACTION_SOLVE_UNSELECTED,
            ACTION_DELETE_SELECTED_SOLVES,
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
     * Private constructor to prevent instantiation of this class containing
     * only constants and utility methods.
     */
    private TTIntent() {
    }

    /**
     * A convenient wrapper for a broadcast receiver that holds the category of
     * intents (groups of actions) for which the receiver will be notified.
     */
    // NOTE: The goal of this class is to make a more obvious connection
    // between the categories and the receivers, as the category will be
    // given in the code at the point where it instantiates an instance of
    // this receiver class. This places the category and actions close together.
    public abstract static class TTCategoryBroadcastReceiver extends
        BroadcastReceiver {
        /**
         * The intent category.
         */
        private final String mCategory;

        /**
         * Creates a new broadcast receiver for a single category of intent
         * actions.
         *
         * @param category
         *     The category of intent actions to be notified to this receiver.
         *     Intent actions not in this category will not be notified.
         */
        public TTCategoryBroadcastReceiver(String category) {
            mCategory = category;
        }

        /**
         * Gets the category of the intent actions that will be matched by this
         * broadcast receiver.
         *
         * @return The category.
         */
        public String getCategory() {
            return mCategory;
        }
    }

    /**
     * A convenient wrapper for fragments that use a broadcast receiver that
     * will only notify the fragment of an intent when the fragment is currently
     * added to its activity. Only intent actions within the specified category
     * will be notified.
     */
    public abstract static class TTFragmentBroadcastReceiver extends
        TTCategoryBroadcastReceiver {
        /**
         * The fragment that is receiving the broadcasts.
         */
        private final Fragment mFragment;

        /**
         * Creates a new broadcast receiver to be used by a fragment. Matching
         * broadcast intents will only be notified to the fragment via
         * {@link #onReceiveWhileAdded} if the fragment is added to its activity
         * at the time of the broadcast.
         *
         * @param fragment
         *     The fragment that will be receiving the broadcast intents.
         * @param category
         *     The category of intent actions to be notified to this receiver.
         *     Intent actions not in this category will not be notified.
         */
        public TTFragmentBroadcastReceiver(Fragment fragment, String category) {
            super(category);
            mFragment = fragment;
        }

        /**
         * Notifies the receiver of a matching broadcast intent that is received
         * while the fragment is added to its activity. The receiver will only
         * be notified of intents that require the category configured, or
         * intents that require no category. (The latter is not a use-case that
         * is expected in this application.)
         *
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        public abstract void onReceiveWhileAdded(
            Context context, Intent intent);

        /**
         * Notifies the receiver of a matching broadcast intent. This
         * implementation calls {@link #onReceiveWhileAdded(Context, Intent)}
         * only while the fragment is currently added to its activity,
         * otherwise the intent will be ignored.
         *
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        // "final" to ensure sub-classes only override "onReceiveWhileAdded".
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
     * Registers a broadcast receiver. The receiver will only be notified of
     * intents that require the category given and only for the actions that
     * are supported for that category.
     *
     * @param receiver
     *     The broadcast receiver to be registered.
     * @param category
     *     The category for the actions to be received. Must not be {@code null}
     *     and must be a supported category.
     *
     * @throws IllegalArgumentException
     *     If the category is {@code null}, or is not one of the supported
     *     categories.
     */
    private static void registerReceiver(
            @NonNull BroadcastReceiver receiver, @NonNull String category) {
        final String[] actions = ACTIONS_SUPPORTED_BY_CATEGORY.get(category);

        if (actions.length == 0) {
            throw new IllegalArgumentException(
                "Category is not supported: " + category);
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
     * Registers a category broadcast receiver. The receiver will only be
     * notified of intents that require the category defined for the
     * {@code TTCategoryBroadcastReceiver} and only for the actions supported
     * by that category. This can also be used for the fragment-specific
     * {@link TTFragmentBroadcastReceiver}.
     *
     * @param receiver The category broadcast receiver to be registered.
     *
     * @throws IllegalArgumentException
     *     If the receiver does not define the name of a supported category.
     */
    public static void registerReceiver(
            @NonNull TTCategoryBroadcastReceiver receiver) {
        registerReceiver(receiver, receiver.getCategory());
    }

    /**
     * Unregisters a broadcast receiver. Any further broadcast intent will be
     * ignored.
     *
     * @param receiver The receiver to be unregistered.
     */
    public static void unregisterReceiver(@NonNull BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext())
                             .unregisterReceiver(receiver);
    }

    /**
     * <p>
     * Broadcasts an intent for the given category and action. To add more
     * details to the intent (via intent extras), use a {@link BroadcastBuilder}
     * created by calling {@link #builder(String, String)}.
     * </p>
     * <p>
     * This method does not add any extras to the intent that it broadcasts.
     * The actions in some categories require extras such as the main state,
     * so some categories are not supported by this method.
     * </p>
     *
     * @param category
     *     The category of the action.
     * @param action
     *     The action.
     *
     * @throws IllegalArgumentException
     *      If the category is {@link #CATEGORY_SOLVE_DATA_CHANGES}, as all
     *      actions in that category require various intent extras to be set.
     */
    public static void broadcast(
            @NonNull String category, @NonNull String action)
            throws IllegalArgumentException {
        if (CATEGORY_SOLVE_DATA_CHANGES.equals(category)) {
            throw new IllegalArgumentException(
                "Intent category '" + category
                + "' does not support broadcasts with no extras.");
        }
        builder(category, action)
            .broadcast();
    }

    /**
     * Broadcasts a request notifying receivers that the main application state
     * has changed. The broadcast is for receivers monitoring actions in
     * {@link #CATEGORY_SOLVE_DATA_CHANGES}. The intent action is set to
     * {@link #ACTION_MAIN_STATE_CHANGED} and the intent includes the extras
     * that are documented for that action.
     *
     * @param newMainState The new value for the main application state.
     */
    public static void broadcastMainStateChanged(
            @NonNull MainState newMainState) {
        builder(CATEGORY_SOLVE_DATA_CHANGES, ACTION_MAIN_STATE_CHANGED)
            .mainState(newMainState)
            .broadcast();
    }

    /**
     * Broadcasts a request to generate a new scramble sequence. The broadcast
     * is for receivers monitoring actions in {@link #CATEGORY_SCRAMBLE_ALERTS}.
     * The intent action is set to {@link #ACTION_GENERATE_SCRAMBLE} and the
     * intent includes the extras that are documented for that action.
     *
     * @param puzzleType
     *     The type of the puzzle for which a sequence is required.
     */
    public static void broadcastNewScrambleRequest(
            @NonNull PuzzleType puzzleType) {
        builder(CATEGORY_SCRAMBLE_ALERTS, ACTION_GENERATE_SCRAMBLE)
            .puzzleType(puzzleType)
            .broadcast();
    }

    /**
     * Broadcasts a request to generate a new scramble image from a scramble
     * sequence.
     *
     * @param scrambleData
     *     The scramble data defining the puzzle type and scramble sequence for
     *     which a corresponding scramble image is required.
     */
    public static void broadcastNewScrambleImageRequest(
            @NonNull ScrambleData scrambleData) {
        builder(CATEGORY_SCRAMBLE_ALERTS, ACTION_GENERATE_SCRAMBLE_IMAGE)
            .scrambleData(scrambleData)
            .broadcast();
    }

    /**
     * Broadcasts a request to bootstrap the statistics loader, providing the
     * initial main state information. If the loader is already bootstrapped
     * and is consistent with the given main state, it may ignore this request.
     *
     * @param mainState
     *     The main state information with which to boot the loader.
     */
    public static void broadcastBootStatisticsLoader(
            @NonNull MainState mainState) {
        builder(CATEGORY_SOLVE_DATA_CHANGES, ACTION_BOOT_STATISTICS_LOADER)
            .mainState(mainState)
            .broadcast();
    }

    /**
     * Broadcasts a request to bootstrap the chart statistics loader, providing
     * the initial main state information. If the loader is already bootstrapped
     * and is consistent with the given main state, it may ignore this request.
     *
     * @param mainState
     *     The main state information with which to boot the loader.
     */
    public static void broadcastBootChartStatisticsLoader(
            @NonNull MainState mainState) {
        builder(CATEGORY_SOLVE_DATA_CHANGES,
                ACTION_BOOT_CHART_STATISTICS_LOADER)
            .mainState(mainState)
            .broadcast();
    }

    /**
     * Gets the main state from an intent extra.
     *
     * @param intent
     *     The intent from which to get the main state.
     *
     * @return
     *     The main state, or {@code null} if the intent does not specify any
     *     main state.
     */
    @Nullable
    public static MainState getMainState(@NonNull Intent intent) {
        return (MainState) intent.getParcelableExtra(EXTRA_MAIN_STATE);
    }

    /**
     * Gets the puzzle type from an intent extra.
     *
     * @param intent The intent from which to get the puzzle type.
     *
     * @return
     *     The puzzle type, or {@code null} if the intent does not specify any
     *     puzzle type extra.
     */
    @Nullable
    public static PuzzleType getPuzzleType(@NonNull Intent intent) {
        if (intent.hasExtra(EXTRA_PUZZLE_TYPE)) {
            return PuzzleType.forTypeName(
                intent.getStringExtra(EXTRA_PUZZLE_TYPE));
        }

        return null;
    }

    /**
     * Gets the solve category from an intent extra.
     *
     * @param intent The intent from which to get the solve category.
     *
     * @return
     *     The solve category, or {@code null} if the intent does not specify
     *     any solve category extra.
     */
    @Nullable
    public static String getSolveCategory(@NonNull Intent intent) {
        return intent.getStringExtra(EXTRA_SOLVE_CATEGORY);
    }

    /**
     * Gets the scramble data containing the puzzle type and scramble sequence
     * from an intent extra. Any corresponding scramble image is not preserved
     * when scramble data is passed in an intent extra.
     *
     * @param intent
     *     The intent from which to get the scramble data.
     *
     * @return
     *     The scramble data (with no image), or {@code null} if the intent
     *     does not specify any scramble data.
     */
    @Nullable
    public static ScrambleData getScrambleData(@NonNull Intent intent) {
        return (ScrambleData) intent.getParcelableExtra(EXTRA_SCRAMBLE_DATA);
    }

    /**
     * Gets the solve specified in an intent extra.
     *
     * @param intent
     *     The intent from which to get the solve.
     *
     * @return
     *     The solve, or {@code null} if the intent does not specify a solve.
     */
    @Nullable
    public static Solve getSolve(@NonNull Intent intent) {
        return (Solve) intent.getParcelableExtra(EXTRA_SOLVE);
    }

    /**
     * Gets the solve ID specified in an intent extra.
     *
     * @param intent
     *     The intent from which to get the solve ID.
     *
     * @return
     *     The solve ID, or {@code Solve.NO_ID} if the intent does not specify
     *     a solve ID.
     */
    public static long getSolveID(@NonNull Intent intent) {
        return intent.getLongExtra(EXTRA_SOLVE_ID, Solve.NO_ID);
    }

    /**
     * Creates a new broadcast builder for the given intent category and
     * action.
     *
     * @param category The category of the action.
     * @param action The action.
     *
     * @return The new broadcast builder.
     */
    public static BroadcastBuilder builder(
            @NonNull String category, @NonNull String action) {
        return new BroadcastBuilder(category, action);
    }

    /**
     * <p>
     * Validates that an intent is defined correctly. The intent must define a
     * category and an action supported by that category. The intent must have
     * all of the intent extras specified for its action. See the description
     * of each action for details of the required extras.
     * </p>
     * <p>
     * Validation is only performed for debug builds. For release builds, no
     * validation is performed, as a validation failure always indicates a bug
     * that needs to be fixed before release.
     * </p>
     *
     * @param intent The intent to validate.
     *
     * @throws IllegalArgumentException
     *     If the intent is not valid.
     */
    public static void validate(@NonNull Intent intent) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        final String action = intent.getAction();

        if (action == null) {
            throw new IllegalStateException("Missing intent action: " + intent);
        }
        if (intent.getCategories().size() != 1) {
            throw new IllegalStateException(
                "Exactly one intent category is expected: " + intent);
        }

        final String category = intent.getCategories().iterator().next(); // 1st
        final String[] actions = ACTIONS_SUPPORTED_BY_CATEGORY.get(category);

        if (!Arrays.asList(actions).contains(intent.getAction())) {
            throw new IllegalStateException(
                "Action '" + action + "' not allowed for category '"
                + category + "'.");
        }

        switch (action) {
            case ACTION_ONE_SOLVE_ADDED:
            case ACTION_ONE_SOLVE_UPDATED:
            case ACTION_ONE_SOLVE_DELETED:
            case ACTION_SOLVE_VERIFIED:
                if (getSolve(intent) == null) {
                    throw new IllegalStateException(
                        "Missing solve extra: " + intent);
                }
                break;

            case ACTION_SOLVE_NOT_VERIFIED:
                // Verifying a solve with ID "NO_ID" is not allowed. If there
                // is no extra, "NO_ID" is the default value. If there is an
                // intent extra explicitly set to "NO_ID", that is also wrong.
                if (getSolveID(intent) == Solve.NO_ID) {
                    throw new IllegalStateException(
                        "Missing or invalid solve ID extra: " + intent);
                }
                break;

            case ACTION_MANY_SOLVES_ADDED:
            case ACTION_MANY_SOLVES_DELETED:
                // Must both be null or both be non-null, not a mixture.
                if ((getPuzzleType(intent) == null)
                    != (getSolveCategory(intent) == null)) {
                    throw new IllegalArgumentException(
                        "Intent must have both puzzle type and category "
                        + "or must have neither puzzle type nor category: "
                        + intent);
                }
                break;

            case ACTION_SOLVES_MOVED_TO_HISTORY:
                if (getPuzzleType(intent) == null
                    || getSolveCategory(intent) == null) {
                    throw new IllegalArgumentException(
                        "Missing puzzle type or solve category: " + intent);
                }
                break;

            case ACTION_MAIN_STATE_CHANGED:
            case ACTION_BOOT_STATISTICS_LOADER:
            case ACTION_BOOT_CHART_STATISTICS_LOADER:
                if (getMainState(intent) == null) {
                    throw new IllegalArgumentException(
                        "Missing main state extra: " + intent);
                }
                break;

            case ACTION_ALGS_MODIFIED:
            case ACTION_SOLVE_SELECTED:
            case ACTION_SOLVE_UNSELECTED:
            case ACTION_DELETE_SELECTED_SOLVES:
            case ACTION_SELECTION_MODE_ON:
            case ACTION_SELECTION_MODE_OFF:
            case ACTION_HIDE_TOOLBAR:
            case ACTION_SHOW_TOOLBAR:
            case ACTION_TOOLBAR_RESTORED:
                // There are no mandatory extras for these intents.
                break;

            case ACTION_GENERATE_SCRAMBLE:
                // Depends on whether this is the action that is sent when the
                // button is pressed (no extras required), or the action that
                // is sent to the loader to generate the scramble (puzzle type
                // extra required).
                if (CATEGORY_SCRAMBLE_ALERTS.equals(category)
                    && getPuzzleType(intent) == null) {
                    throw new IllegalArgumentException(
                        "Missing puzzle type extra: " + intent);
                }
                break;

            case ACTION_GENERATE_SCRAMBLE_IMAGE:
                if (getScrambleData(intent) == null) {
                    throw new IllegalArgumentException(
                        "Missing scramble data extra: " +  intent);
                }
                break;
        }
    }

    /**
     * A builder for local broadcasts.
     *
     * @author damo
     */
    @SuppressWarnings("WeakerAccess") // Code inspector is wrong!
    public static class BroadcastBuilder {
        /**
         * The intent that will be broadcast when building is complete.
         */
        private Intent mIntent;

        /**
         * Creates a new broadcast builder for the given intent category and
         * action.
         *
         * @param category The category of the action.
         * @param action   The action.
         */
        private BroadcastBuilder(@NonNull String category,
                                 @NonNull String action) {
            mIntent = new Intent(action);
            // Will throw NPE if category is null, but that is OK...
            mIntent.addCategory(category);
        }

        /**
         * Broadcasts the intent configured by this builder.
         *
         * @throws IllegalArgumentException
         *     If the intent is not valid. See {@link #validate(Intent)} for
         *     more details.
         */
        public void broadcast() throws IllegalArgumentException {
            validate(mIntent);

            if (DEBUG_ME) Log.d(TAG, "broadcast(): -> " + mIntent);
            LocalBroadcastManager.getInstance(TwistyTimer.getAppContext())
                                 .sendBroadcast(mIntent);
        }

        /**
         * Sets extra that identifies the main state of the application. Changes
         * to the state can be broadcast and the receiver can retrieve the state
         * by calling {@link TTIntent#getMainState(Intent)}.
         *
         * @param state
         *     The main state for the application. If {@code null}, no intent
         *     extra will be added.
         *
         * @return
         *     {@code this} broadcast builder, allowing method calls to be
         *     chained.
         */
        public BroadcastBuilder mainState(@Nullable MainState state) {
            if (state != null) {
                mIntent.putExtra(EXTRA_MAIN_STATE, state);
            }

            return this;
        }

        /**
         * Sets extra that identifies the puzzle type associated with the action
         * notified in an intent. Retrieve the puzzle type from the intent by
         * calling {@link TTIntent#getPuzzleType(Intent)}.
         *
         * @param puzzleType
         *     The puzzle type. If {@code null}, no intent extra will be added.
         *
         * @return
         *     {@code this} broadcast builder, allowing method calls to be
         *     chained.
         */
        public BroadcastBuilder puzzleType(@Nullable PuzzleType puzzleType) {
            if (puzzleType != null) {
                mIntent.putExtra(EXTRA_PUZZLE_TYPE, puzzleType.typeName());
            }

            return this;
        }

        /**
         * Sets extra that identifies the puzzle type associated with the action
         * notified in an intent. Retrieve the puzzle type from the intent by
         * calling {@link TTIntent#getSolveCategory(Intent)}.
         *
         * @param solveCategory
         *     The solve category. If {@code null}, no intent extra will be
         *     added.
         *
         * @return
         *     {@code this} broadcast builder, allowing method calls to be
         *     chained.
         */
        public BroadcastBuilder solveCategory(@Nullable String solveCategory) {
            if (solveCategory != null) {
                mIntent.putExtra(EXTRA_SOLVE_CATEGORY, solveCategory);
            }

            return this;
        }

        /**
         * Sets an optional extra that identifies a scramble data (puzzle type
         * and scramble sequence) related to the action of the intent that will
         * be broadcast. To retrieve the scramble data from the intent, the
         * receiver can call {@link TTIntent#getScrambleData(Intent)}. Any
         * scramble image on the scramble data will <i>not</i> be preserved in
         * the intent extra.
         *
         * @param scrambleData
         *     The scramble data to be added to the broadcast intent. If
         *     {@code null}, no intent extra will be added.
         *
         * @return
         *     {@code this} broadcast builder, allowing method calls to be
         *     chained.
         */
        public BroadcastBuilder scrambleData(
                @Nullable ScrambleData scrambleData) {
            if (scrambleData != null) {
                mIntent.putExtra(EXTRA_SCRAMBLE_DATA, scrambleData);
            }

            return this;
        }

        /**
         * Sets an optional extra that identifies a solve result related to the
         * action of the intent that will be broadcast. The receiver can call
         * {@link TTIntent#getSolve(Intent)} to retrieve the solve from the
         * intent.
         *
         * @param solve
         *     The solve to be added to the broadcast intent. If {@code null},
         *     no intent extra will be added.
         *
         * @return
         *     {@code this} broadcast builder, allowing method calls to be
         *     chained.
         */
        public BroadcastBuilder solve(@Nullable Solve solve) {
            if (solve != null) {
                // "Solve" implements "Parcelable" to allow it to be passed
                // in an intent extra.
                mIntent.putExtra(EXTRA_SOLVE, solve);
            }

            return this;
        }

        /**
         * Sets an optional extra to the ID of a solve record related to the
         * action of the intent that will be broadcast. The receiver can call
         * {@link TTIntent#getSolveID(Intent)} to retrieve the solve ID from
         * the intent.
         *
         * @param solveID
         *     The ID of the solve to be added to the broadcast intent.
         *
         * @return
         *     {@code this} broadcast builder, allowing method calls to be
         *     chained.
         */
        public BroadcastBuilder solveID(long solveID) {
            mIntent.putExtra(EXTRA_SOLVE_ID, solveID);
            return this;
        }
    }
}
