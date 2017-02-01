package com.aricneto.twistytimer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.BoolRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.utils.ThemeUtils.TTTheme;

/**
 * <p>
 * Utility class to access the default shared preferences. This support access
 * to shared preferences using string resource IDs for the preference names
 * (see {@code res/values/pref_keys.xml}). It also provides many higher-level
 * methods for easier access to common preferences and their default values
 * (see {@code res/values/pref_defaults.xml}).
 * </p>
 * <p>
 * <i>NOTE: If accessing preference values repeatedly, or in a loop, it is
 * recommended that they first be copied to a local variable, as accessing
 * their values incurs some overhead.</i>
 * </p>
 *
 * @author damo
 */
public final class Prefs {
    /**
     * The preferences instance. There is only one shared preferences instance
     * per preferences file for each process, so this can be cached safely and
     * will reflect any changes made by any other code that makes changes to
     * the preferences.
     */
    private static SharedPreferences sPrefs;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Prefs() {
    }

    /**
     * Gets the string value of a shared preference.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See
     *     {@code values/pref_keys.xml}.
     * @param defaultValueResID
     *     The string resource ID for the default preference value to return if
     *     the preference is not defined. See {@code values/pref_defaults.xml}.
     *
     * @return
     *     The value of the preference, or the given default value (resolved
     *     from its resource ID) if the preference is not defined.
     */
    public static String getString(@StringRes int prefKeyResID,
                                   @StringRes int defaultValueResID) {
        return getPrefs().getString(
            getStringRes(prefKeyResID), getStringRes(defaultValueResID));
    }

    /**
     * Gets the string value of a shared preference, using a raw string value
     * as the default value if the preference is not defined. Prefer the
     * {@link #getString(int, int)} method that takes the string resource ID of
     * the default value, where possible.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See
     *     {@code values/pref_keys.xml}.
     * @param defaultValue
     *     The default preference value to return if the preference is not
     *     defined.
     *
     * @return
     *     The value of the preference, or the given default value if the
     *     preference is not defined.
     */
    public static String getStringRawDefault(@StringRes int prefKeyResID,
                                             String defaultValue) {
        return getPrefs().getString(getStringRes(prefKeyResID), defaultValue);
    }

    /**
     * Gets the integer value of a shared preference.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See
     *     {@code values/pref_keys.xml}.
     * @param defaultValueResID
     *     The string resource ID for the default preference value to return if
     *     the preference is not defined. See {@code values/pref_defaults.xml}.
     *
     * @return
     *     The value of the preference, or the given default value (resolved
     *     from its resource) if the preference is not defined.
     */
    public static int getInt(@StringRes int prefKeyResID,
                             @IntegerRes int defaultValueResID) {
        return getPrefs().getInt(
            getStringRes(prefKeyResID), getIntegerRes(defaultValueResID));
    }

    /**
     * Gets the integer value of a shared preference, using a raw integer
     * value as the default value if the preference is not defined. Prefer the
     * {@link #getInt(int, int)} method that takes the integer resource ID of
     * the default value, where possible.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See
     *     {@code values/pref_keys.xml}.
     * @param defaultValue
     *     The default preference value to return if the preference is not
     *     defined.
     *
     * @return The value of the preference, or the given default value if the
     * preference is not defined.
     */
    public static int getIntRawDefault(@StringRes int prefKeyResID,
                                       int defaultValue) {
        return getPrefs().getInt(getStringRes(prefKeyResID), defaultValue);
    }

    /**
     * Gets the Boolean value of a shared preference.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See
     *     {@code values/pref_keys.xml}.
     * @param defaultValueResID
     *     The Boolean resource ID for the default preference value to return if
     *     the preference is not defined. See {@code values/pref_defaults.xml}.
     *
     * @return
     *     The value of the preference, or the given default value (resolved
     *     from its resource) if the preference is not defined.
     */
    public static boolean getBoolean(@StringRes int prefKeyResID,
                                     @BoolRes int defaultValueResID) {
        return getPrefs().getBoolean(
            getStringRes(prefKeyResID), getBooleanRes(defaultValueResID));
    }

    /**
     * Gets the Boolean value of a shared preference, using a raw Boolean
     * value as the default value if the preference is not defined. Prefer the
     * {@link #getBoolean(int, int)} method that takes the integer resource ID
     * of the default value, where possible.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See
     *     {@code values/pref_keys.xml}.
     * @param defaultValue
     *     The default preference value to return if the preference is not
     *     defined.
     *
     * @return
     *     The value of the preference, or the given default value if the
     *     preference is not defined.
     */
    public static boolean getBooleanRawDefault(@StringRes int prefKeyResID,
                                               boolean defaultValue) {
        return getPrefs().getBoolean(getStringRes(prefKeyResID), defaultValue);
    }

    /**
     * Gets the user's preferred theme, or the default theme if no preferred
     * theme has been set.
     *
     * @return The user's preferred theme.
     */
    @NonNull
    public static TTTheme getTheme() {
        // Historically, the theme name was a hard-coded string. For
        // simplicity, the enum value name is now written. However, there may
        // be installations where the old name is used. The two outliers are
        // "deepPurple" for "DEEP_PURPLE" and "blueGray" for "BLUE_GRAY".
        // All others old names can be matched by simply converting to
        // upper-case first.
        final String themeName = getStringRawDefault(R.string.pk_theme,
            ThemeUtils.DEFAULT_THEME.name()).toUpperCase();

        switch (themeName) {
            case "deepPurple":
                return TTTheme.DEEP_PURPLE;
            case "blueGray":
                return TTTheme.BLUE_GRAY;
            default:
                try {
                    return TTTheme.valueOf(themeName);
                } catch (IllegalArgumentException ignore) {
                    // Name is not recognised. Unlikely, but just recover and
                    // use the default name.
                    return ThemeUtils.DEFAULT_THEME;
                }
        }
    }

    /**
     * Indicates if the automatic generation of a scramble sequence is enabled
     * when the app is started and each time the timer is stopped.
     *
     * @return
     *     {@code true} if scramble generation is enabled; or {@code false} if
     *     it is disabled.
     */
    public static boolean isScrambleEnabled() {
        return getBoolean(
            R.string.pk_scramble_enabled, R.bool.default_scramble_enabled);
    }

    /**
     * Indicates if hints for the solution of the "cross" should be shown after
     * the generation of a scramble sequence. Cross hints will not be shown if
     * scrambles are not first enabled (i.e., this method checks
     * {@link #isScrambleEnabled()} first). If cross hints are disabled,
     * X-cross hints will also be disabled (see
     * {@link #showXCrossHints(PuzzleType)}).
     *
     * @param puzzleType
     *     The type of the puzzle for which hints may be shown. Hints can only
     *     be shown for 3x3x3 cube puzzles. For all other puzzle types, this
     *     method will return {@code false}.
     *
     * @return
     *     {@code true} if cross hints may be shown; or {@code false} if they
     *     should not be shown.
     */
    public static boolean showCrossHints(@NonNull PuzzleType puzzleType) {
        return puzzleType == PuzzleType.TYPE_333 && isScrambleEnabled()
               && getBoolean(R.string.pk_show_cross_hints,
            R.bool.default_show_cross_hints);
    }

    /**
     * Indicates if hints for the solution of the "X-cross" (or "extended
     * cross") should be shown after the generation of a scramble sequence.
     * X-cross hints will not be shown if basic cross hints are not first
     * enabled (i.e., this method checks both {@link #isScrambleEnabled()} and
     * {@link #showCrossHints(PuzzleType)} first).
     *
     * @param puzzleType
     *     The type of the puzzle for which hints may be shown. Hints can only
     *     be shown for 3x3x3 cube puzzles. For all other puzzle types, this
     *     method will return {@code false}.
     *
     * @return
     *     {@code true} if X-cross hints may be shown along with the cross
     *     hints; or {@code false} if they should not be shown.
     */
    public static boolean showXCrossHints(@NonNull PuzzleType puzzleType) {
        return showCrossHints(puzzleType)
               // Scramble enabled & show cross & puzzle == 3x3x3
               && getBoolean(
                    R.string.pk_show_x_cross_hints,
                    R.bool.default_show_x_cross_hints);
    }

    /**
     * Indicates if a scramble image should be shown for a corresponding
     * scramble sequence. Scramble images will not be shown if scrambles are not
     * first enabled (i.e., this method checks {@link #isScrambleEnabled()}
     * first).
     *
     * @return
     *     {@code true} to show scramble images (if scrambles are enabled);
     *     or {@code false} if scramble images are should not be shown.
     */
    public static boolean showScrambleImage() {
        return isScrambleEnabled() && getBoolean(
            R.string.pk_show_scramble_image,
            R.bool.default_show_scramble_image);
    }

    /**
     * Indicates if the "quick action" buttons should be shown below the solve
     * time when the timer is stopped.
     *
     * @return
     *     {@code true} to show the quick action buttons; or {@code false} to
     *     hide them.
     */
    public static boolean showQuickActions() {
        return getBoolean(
            R.string.pk_show_quick_actions, R.bool.default_show_quick_actions);
    }

    /**
     * Indicates if the short summary of the session statistics should be shown
     * on the timer page.
     *
     * @return
     *     {@code true} to show the session statistics; or {@code false} to
     *     hide them.
     */
    public static boolean showSessionStats() {
        return getBoolean(
            R.string.pk_show_session_stats, R.bool.default_show_session_stats);
    }

    /**
     * Indicates if a new record best solve time should be proclaimed to the
     * user as the best-ever solve time when the timer is stopped.
     *
     * @return
     *     {@code true} to proclaim record best times; or {@code false} to
     *     ignore them.
     */
    public static boolean proclaimBestTime() {
        return getBoolean(
            R.string.pk_proclaim_best_time, R.bool.default_proclaim_best_time);
    }

    /**
     * Indicates if a new record worst solve time should be proclaimed to the
     * users as the worst-ever solve time when the timer is stopped.
     *
     * @return
     *     {@code true} to proclaim record worst times; or {@code false} to
     *     ignore them.
     */
    public static boolean proclaimWorstTime() {
        return getBoolean(
            R.string.pk_proclaim_worst_time,
            R.bool.default_proclaim_worst_time);
    }

    /**
     * Gets the duration of the inspection period before starting a new solve.
     *
     * @return The inspection time (in seconds).
     */
    public static int getInspectionTime() {
        return getInt(
            R.string.pk_inspection_time_s, R.integer.default_inspection_time_s);
    }

    /**
     * Gets the scale factor for the size of the timer display. The preference
     * is stored as an integer percentage value, which will be converted to a
     * scale factor by this method. For example, if the preference value is
     * {@code 75} (percent), this method returns {@code 0.75}.
     *
     * @return The scale factor for the size of the timer display.
     */
    public static float getTimerDisplayScale() {
        return getInt(
            R.string.pk_timer_display_scale_pc,
            R.integer.default_timer_display_scale_pc) / 100f;
    }

    /**
     * Gets the scale factor for the size of the scramble text. The preference
     * is stored as an integer percentage value, which will be converted to a
     * scale factor by this method. For example, if the preference value is
     * {@code 75} (percent), this method will return {@code 0.75}.
     *
     * @return The scale factor for the size of the scramble text.
     */
    public static float getScrambleTextScale() {
        return getInt(
            R.string.pk_scramble_text_scale_pc,
            R.integer.default_scramble_text_scale_pc) / 100f;
    }

    /**
     * Gets the scale factor for the size of the scramble image. The preference
     * is stored as an integer percentage value, which will be converted to a
     * scale factor by this method. For example, if the preference value is
     * {@code 75} (percent), this method will return {@code 0.75}.
     *
     * @return The scale factor for the size of the scramble image.
     */
    public static float getScrambleImageScale() {
        return getInt(
            R.string.pk_scramble_image_scale_pc,
            R.integer.default_scramble_image_scale_pc) / 100f;
    }

    /**
     * Gets the last used main activity instance state. This is the state that
     * was saved before last quitting the app. If no state was previously saved,
     * a sensible default state will be returned. The main state includes the
     * last used puzzle type, the last used solve category and the last used
     * setting of the "history" switch.
     *
     * @return The last used main state information for the application.
     */
    @NonNull
    public static MainState getLastUsedMainState() {
        final PuzzleType puzzleType = getLastUsedPuzzleType(); // or default.

        return new MainState(puzzleType,
            getLastUsedSolveCategory(puzzleType), // or default.
            getLastUsedIsHistoryEnabled());       // or default.
    }

    /**
     * Gets the last used (selected) puzzle type.
     *
     * @return
     *     The last used puzzle type, or the default puzzle type if none was
     *     previously recorded.
     */
    @NonNull
    private static PuzzleType getLastUsedPuzzleType() {
        return PuzzleType.forTypeName(
            getStringRawDefault(
                R.string.pk_last_used_puzzle_type,
                MainState.DEFAULT_PUZZLE_TYPE.typeName()));
    }

    /**
     * <p>
     * Gets the last used (selected) solve category for the given puzzle type.
     * Solve categories are specific to each puzzle type. There may be many
     * "last used" categories, but there will only be one for each puzzle type.
     * </p>
     * <p>
     * Solve categories are user-defined. While it is unlikely that the
     * last-used solve category will not be already defined in the database,
     * this method guarantees that the solve category it returns will exist
     * in the database. If it does not already exist, it will be created. The
     * creation is performed as a background task, so the category may not be
     * available from the database for a brief time after this method returns.
     * </p>
     *
     * @param puzzleType
     *     The puzzle type for which the last used solve category is required.
     *
     * @return
     *     The solve category that was last used for the given puzzle type, or
     *     the default category if none was previously recorded.
     */
    @NonNull
    public static String getLastUsedSolveCategory(
            @NonNull final PuzzleType puzzleType) {
        // When the user selects a new puzzle type, the system must select a
        // valid solve category for that new puzzle type. The solve category
        // selected for the "old" puzzle type cannot be assumed to exist for
        // the new puzzle type. Solve categories are specific to each puzzle
        // type: a solve category that exists for one puzzle type may not
        // exist for another.
        //
        // A "valid" solve category is one for which a "fake" solve record
        // exists in the database recording only the name of that category.
        // This allows the solve category to exist even when no solve times
        // have been added for that category. It will also appear in the list
        // of categories that can be selected and edited. (Of course, it
        // would be nicer if solve categories existed in their own database
        // table, but that is for another day.)
        //
        // It is *very probably* safe to assume that the "last-used" solve
        // category for the new puzzle type will exist in the database (i.e.,
        // it will have a "fake" solve record). This assumption *should*
        // hold, as a category can only be deleted when its puzzle type is
        // selected, and then a new category will be selected and become the
        // "last-used" category for that type. Even if the "last-used"
        // category is not saved until quitting the app, it must exist if it
        // was selected at that time. If the last existing solve category for
        // a puzzle type is deleted via "DatabaseHandler.deleteSolveCategory",
        // that method will re-create the default category before it returns.
        //
        // For database version 10, the "fake" records for the default solve
        // category ("Normal") are created as part of the database upgrade.
        // If the "last-used" solve category is not defined because that
        // puzzle type was never previously selected, then when the default
        // solve category is returned by this method, and that default
        // category should also exist in the database for the puzzle type.
        //
        // It is hard, therefore, to see how the solve category returned by
        // this method could not exist in the database already. Nevertheless,
        // if it did not exist, there would be a bit of an issue. When the user
        // lists the available solve categories, only the "fake" solve records
        // holding the category names are read; the "real" solve records, also
        // containing the category names, are not read. Therefore, even if
        // solve times exist for a solve category, that category will be
        // "invisible" (i.e., not selectable or editable) until its "fake"
        // solve record is created. A user could change to a new puzzle type,
        // have the solve category selected for that type and then find that
        // there is no way to change to a different category and back again, as
        // the original category does not really exist. This would be a problem
        // if solve times had been recorded for the "non-existent" category, as
        // they could no longer be accessed. (Actually, the user could manually
        // create the "invisible" category and then access its solve times.)
        //
        // Accessing the database on the main UI thread from this method to
        // ensure that the "last-used" solve category exists before returning
        // it--and selecting a different, extant category if the "last-used"
        // or default category does not exist--would slow down the app for a
        // possibility that is very remote at best. Therefore, the solution
        // is to proceed on the very reasonable assumption that the "last-used"
        // (or default) solve category exists in the database and return that
        // category, but then to fire off a little background task to create
        // that category on the remote possibility that it does not already
        // exist. If the category already exists, the background task will
        // have no effect. This will ensure this method does not slow down
        // the UI thread while also ensuring that any selected solve category
        // cannot be "invisible" for more than an instant.
        final String lastUsedSolveCategory = getPrefs()
            .getString(getLastUsedSolveCategoryKey(puzzleType),
                MainState.DEFAULT_SOLVE_CATEGORY);

        FireAndForgetExecutor.execute(new Runnable() {
            @Override
            public void run() {
                TwistyTimer.getDBHandler()
                           .addSolveCategory(puzzleType, lastUsedSolveCategory);
            }
        });

        return lastUsedSolveCategory;
    }

    /**
     * Gets the last used setting of the "history" switch.
     *
     * @return The last used history switch setting, or the default setting if
     * none was previously recorded.
     */
    private static boolean getLastUsedIsHistoryEnabled() {
        return getBooleanRawDefault(R.string.pk_last_used_is_history_enabled,
            MainState.DEFAULT_IS_HISTORY_ENABLED);
    }

    /**
     * <p>
     * Identifies the resource ID from those given whose string value matches
     * the given shared preferences key. This is useful when constructing
     * {@code switch} statements. For example:
     * </p>
     * <pre>
     * String key = preference.getKey();
     *
     * switch (keyToResourceID(
     *     key, R.string.pk_1, R.string.pk_2, R.string.pk_3)) {
     *     case R.string.pk_1:
     *         // Do something.
     *         break;
     *     case R.string.pk_2:
     *         // Do something else.
     *         break;
     *     case R.string.pk_3:
     *         // Do something entirely different.
     *         break;
     *     default:
     *         // "key" did not match any of the string value of any of the
     *         // given resource IDs.
     *         break;
     * }
     * </pre>
     *
     * @param key
     *     The string value of the shared preferences key to be matched to a
     *     resource ID.
     * @param prefKeyResIDs
     *     Any number of string resource IDs. See {@code values/pref_keys.xml}.
     *
     * @return
     *     The first resource ID whose string value matches the given key; or
     *     zero if the key is {@code null}, there are no resource IDs given,
     *     or the key does not match the string value of any of the given
     *     string resources.
     */
    @StringRes
    public static int keyToResourceID(@Nullable String key,
                                      @StringRes int... prefKeyResIDs) {
        if (key != null && prefKeyResIDs != null && prefKeyResIDs.length > 0) {
            final Context context = TwistyTimer.getAppContext();

            for (int resID : prefKeyResIDs) {
                if (key.equals(context.getString(resID))) {
                    return resID;
                }
            }
        }
        return 0;
    }

    /**
     * Gets an editor for the default shared preferences. When editing is
     * complete, call {@link Editor#apply()} on the editor to save the changes.
     */
    @SuppressLint("CommitPrefEdits")
    public static Editor edit() {
        return new Editor(getPrefs().edit());
    }

    /**
     * Gets the default shared preferences for this application.
     *
     * @return The default shared preferences.
     */
    private static SharedPreferences getPrefs() {
        if (sPrefs == null) {
            sPrefs = PreferenceManager
                .getDefaultSharedPreferences(TwistyTimer.getAppContext());
        }

        return sPrefs;
    }

    /**
     * Gets the string value of the given string resource ID. Use this to
     * resolve the names of the preference keys and the default values of
     * string preferences in {@code values/pref_keys.xml} and
     * {@code values/pref_defaults.xml}.
     *
     * @param stringResID The string resource ID.
     *
     * @return The string value defined in the resources.
     */
    private static String getStringRes(@StringRes int stringResID) {
        return TwistyTimer.getAppContext().getString(stringResID);
    }

    /**
     * Gets the integer value of the given integer resource ID. Use this
     * to resolve the default values of integer preferences in
     * {@code values/pref_defaults.xml}.
     *
     * @param intResID The integer resource ID.
     *
     * @return The integer value defined in the resources.
     */
    private static int getIntegerRes(@IntegerRes int intResID) {
        return TwistyTimer.getAppContext().getResources().getInteger(intResID);
    }

    /**
     * Gets the Boolean value of the given Boolean resource ID. Use this
     * to resolve the default values of Boolean preferences in
     * {@code values/pref_defaults.xml}.
     *
     * @param boolResID The Boolean resource ID.
     *
     * @return The Boolean value defined in the resources.
     */
    private static boolean getBooleanRes(@BoolRes int boolResID) {
        return TwistyTimer.getAppContext().getResources().getBoolean(boolResID);
    }

    /**
     * Gets the preference key name for the last used solve category. The key
     * depends on the puzzle type.
     *
     * @param puzzleType
     *     The puzzle type for which the solve category key is required.
     *
     * @return
     *     The last used solve category.
     */
    private static String getLastUsedSolveCategoryKey(
            @NonNull PuzzleType puzzleType) {
        // The preference key for a solve category is a prefix to which the
        // puzzle type must be appended to find the correct last used solve
        // category. Historically, there was never any separator between the
        // prefix and the puzzle type, so it is too late to add one now.
        return getStringRes(R.string.pk_prefix_last_used_category)
               + puzzleType.typeName();
    }

    /**
     * A simple wrapper for the shared preference editor that provides a easy
     * way to use string resource IDs when setting preference values. The
     * {@code put*} operations do not commit the changes. Several changes can
     * be made via one editor, but {@link #apply()} must be called after the
     * changes are complete to ensure that the changes are saved.
     */
    public static class Editor {
        /**
         * The shared preferences editor wrapped by this helper class.
         */
        private final SharedPreferences.Editor mSPEditor;

        /**
         * Creates a new editor helper that wraps the given shared preferences
         * editor.
         *
         * @param spEditor
         *     The shared preferences editor to be wrapped.
         */
        private Editor(@NonNull SharedPreferences.Editor spEditor) {
            mSPEditor = spEditor;
        }

        /**
         * Commits any changes made using this editor.
         */
        public void apply() {
            mSPEditor.apply();
        }

        /**
         * Sets the value of a shared preference to the given string.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key. See
         *     {@code values/pref_keys.xml}.
         * @param value
         *     The new value of the preference. If {@code null}, this is the
         *     equivalent of calling {@link #remove(int)}.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putString(@StringRes int prefKeyResID,
                                @Nullable String value) {
            mSPEditor.putString(getStringRes(prefKeyResID), value);
            return this;
        }

        /**
         * Sets the value of a shared preference to the given integer.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key. See
         *     {@code values/pref_keys.xml}.
         * @param value
         *     The new value of the preference.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putInt(@StringRes int prefKeyResID, int value) {
            mSPEditor.putInt(getStringRes(prefKeyResID), value);
            return this;
        }

        /**
         * Sets the value of a shared preference to the given Boolean.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key. See
         *     {@code values/pref_keys.xml}.
         * @param value
         *     The new value of the preference.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putBoolean(@StringRes int prefKeyResID, boolean value) {
            mSPEditor.putBoolean(getStringRes(prefKeyResID), value);
            return this;
        }

        /**
         * Sets the value of the user's preferred theme.
         *
         * @param value
         *     The new value of the theme. If {@code null}, this is the
         *     equivalent of calling {@link #remove(int)} passing
         *     {@code R.string.pk_theme}.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putTheme(@Nullable TTTheme value) {
            // NOTE: For historical reasons, reading back the theme is a
            // little more complicated.
            putString(R.string.pk_theme, value == null ? null : value.name());
            return this;
        }

        /**
         * Sets the values for the elements of the last used main state
         * information. This allows basic information about the puzzle type and
         * some other active settings to be saved before quitting the app and
         * then restored the next time the app is started.
         *
         * @param mainState
         *     The main state information to be saved. If {@code null}, this is
         *     the equivalent of removing all of the main state values,
         *     including the last used solve category for every puzzle type.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putLastUsedMainState(@Nullable MainState mainState) {
            if (mainState != null) {
                putBoolean(
                    R.string.pk_last_used_is_history_enabled,
                    mainState.isHistoryEnabled());
                putLastUsedPuzzleTypeAndSolveCategory(
                    mainState.getPuzzleType(),
                    mainState.getSolveCategory());
            } else {
                remove(R.string.pk_last_used_is_history_enabled);
                putLastUsedPuzzleTypeAndSolveCategory(null, null);
            }

            return this;
        }

        /**
         * Sets the values of the last used puzzle type and last used solve
         * category. The last used solve category is specific to the given
         * puzzle type. There can exist a last used solve category for each
         * puzzle type that was previously saved as a last used puzzle type.
         * Only the last used solve category for the given puzzle type will be
         * modified by this method, unless the puzzle type is {@code null}.
         *
         * @param puzzleType
         *     The puzzle type. If {@code null}, the puzzle type will be reset
         *     <i>and every last used solve category for every puzzle type will
         *     also be reset</i>; the default puzzle type, and default solve
         *     category, will be returned when they are next requested.
         * @param solveCategory
         *     The solve category. Ignored if {@code puzzleType} is
         *     {@code null}. If {@code null}, and {@code puzzleType} is
         *     non-{@code null}, the last used solve category for that puzzle
         *     type will be reset; the default solve category will be returned
         *     when it is next requested.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        private Editor putLastUsedPuzzleTypeAndSolveCategory(
            @Nullable PuzzleType puzzleType, @Nullable String solveCategory) {
            putString(R.string.pk_last_used_puzzle_type,
                puzzleType == null ? null : puzzleType.typeName());

            if (puzzleType == null) {
                // NOTE: This may seem like "make-work", but it ensures that
                // the behaviour of these "Prefs.Editor" methods is consistent
                // with the normal behaviour of the "SharedPreferences.Editor"
                // methods, i.e., "null" means "reset".
                final String scKeyPrefix = getStringRes(
                    R.string.pk_prefix_last_used_category);

                // Reset every solve category for every puzzle type.
                for (String key : getPrefs().getAll().keySet()) {
                    if (key.startsWith(scKeyPrefix)) {
                        mSPEditor.remove(key);
                    }
                }
            } else {
                // If "solveCategory" is null, it is reset for just this one
                // non-null "puzzleType".
                mSPEditor.putString(getLastUsedSolveCategoryKey(puzzleType),
                    solveCategory);
            }

            return this;
        }

        /**
         * Removes the value of a shared preference.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key. See
         *     {@code values/pref_keys.xml}.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor remove(@StringRes int prefKeyResID) {
            mSPEditor.remove(getStringRes(prefKeyResID));
            return this;
        }
    }
}
