package com.aricneto.twistytimer.utils;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.activity.SettingsActivity;
import com.aricneto.twistytimer.items.PuzzleType;

/**
 * <p>
 * The main state information for the application that is persisted when the application is
 * restarted. The fields of the main state are limited to those required to access solve times in
 * the database for the saving of new solve times and the presentation of statistics and charts
 * showing solve time data. As such, the {@link MainActivity} and its various fragments nearly
 * all require all of this main state information and it must be synchronised across all of those
 * components.
 * </p>
 * <p>
 * There are other persistent values, such as the theme, puzzle color scheme and other values from
 * the {@link SettingsActivity}, but those values are not treated as main state. They are persisted
 * and accessed separately, typically through the {@link Prefs} utility class.
 * </p>
 * <p>
 * The {@code MainActivity} is responsible for maintaining the main state, persisting it when
 * required, and notifying its fragments of any changes. It also provides an accessor that the
 * fragments can use to retrieve the sate. While the user interface elements that change the main
 * state may be managed by other fragments, these fragments must notify the activity of these
 * changes and wait for notification of the change before applying the changes to their own
 * elements.
 * </p>
 * <p>
 * The {@code Parcelable} interface makes it easy to persist the main state in the saved instance
 * state, to pass it via local broadcast {@code Intent}s, or to pass it in fragment or loader
 * argument bundles, if necessary. Immutability makes it safer to pass references to other
 * fragments without concerns that the fragments will change the state inadvertently and affect
 * other components that hold a reference to the same object.
 * </p>
 * <p>
 * An {@link #equals(Object)} method allows the {@code MainActivity} to determine if a notification
 * of a change originating from a fragment is any different from the current state. If there is no
 * difference, then the "change" does not need to be notified to any other components.
 * </p>
 * <p>
 * A custom {@link #toString()} method supports simpler logging and debugging of the main state.
 * </p>
 *
 * @author damo
 */
public class MainState implements Parcelable {
    /**
     * The built-in solve category that applies before any other solve category is created or
     * selected.
     */
    public static final String CATEGORY_NORMAL = "Normal";

    /**
     * The default puzzle type to use in the absence of any previously saved puzzle type being
     * available.
     */
    public static final PuzzleType DEFAULT_PUZZLE_TYPE = PuzzleType.TYPE_333;

    /**
     * The default solve category to use in the absence of any previously saved puzzle type being
     * available for the known puzzle type.
     */
    public static final String DEFAULT_SOLVE_CATEGORY = CATEGORY_NORMAL;

    /**
     * The default setting of the "history" switch in the absence of any previously saved setting
     * being available.
     */
    public static final boolean DEFAULT_IS_HISTORY_ENABLED = false;

    /**
     * The key used when saving this state to a {@code Bundle}.
     */
    private static final String BUNDLE_KEY = MainState.class.getName();

    /**
     * The currently selected puzzle type.
     */
    @NonNull
    private final PuzzleType mPuzzleType;

    /**
     * The currently selected solve category.
     */
    @NonNull
    private final String mSolveCategory;

    /**
     * Indicates if viewing of the full history of all solves is enabled.
     */
    private final boolean mIsHistoryEnabled;

    /**
     * The cached hash code value.
     */
    private int mHashCode;

    /**
     * The cached string representation of this immutable state.
     */
    private String mAsString;

    /**
     * Creates a new holder for the main state information.
     *
     * @param puzzleType
     *     The currently selected puzzle type. A non-{@code null} default value must be used if
     *     no puzzle type has been explicitly selected.
     * @param solveCategory
     *     The currently selected solve category. A non-{@code null} default value must be used
     *     if no solve category has been explicitly selected.
     * @param isHistoryEnabled
     *     {@code true} if the viewing of the full history of all solve times has been enabled;
     *     or {@code false} it is is not enabled.
     */
    public MainState(@NonNull PuzzleType puzzleType, @NonNull CharSequence solveCategory,
                     boolean isHistoryEnabled) {
        mPuzzleType       = puzzleType;
        mSolveCategory    = solveCategory.toString(); // *Must* store an *immutable* "String".
        mIsHistoryEnabled = isHistoryEnabled;
    }

    /**
     * Creates a holder for the main state information from a parcel.
     *
     * @param in The parcel containing the state information.
     */
    protected MainState(Parcel in) {
        this(PuzzleType.forTypeName(in.readString()), in.readString(), in.readByte() != 0);
    }

    /**
     * Gets the puzzle type.
     *
     * @return The puzzle type.
     */
    @NonNull
    public PuzzleType getPuzzleType() {
        return mPuzzleType;
    }

    /**
     * Gets the solve category.
     *
     * @return The solve category.
     */
    @NonNull
    public String getSolveCategory() {
        return mSolveCategory;
    }

    /**
     * Indicates if the "history" switch is (or should be) enabled.
     *
     * @return
     *    {@code true} if the history switch is/should be checked; or {@code false} if the switch
     *    is/should be unchecked.
     */
    public boolean isHistoryEnabled() {
        return mIsHistoryEnabled;
    }

    /**
     * Saves this main state to a bundle. This is convenient when saving instance state for an
     * application component. The same key is always used to identify the state, so only one
     * instance of {@code MainState} can be saved to any one {@code Bundle}. The saved state should
     * be restored using {@link #restoreFromBundle(Bundle)}, as it will identify the state saved
     * by this method.
     *
     * @param bundle The bundle to which to save the state.
     * @return The given bundle.
     */
    @NonNull
    public Bundle saveToBundle(@NonNull Bundle bundle) {
        bundle.putParcelable(BUNDLE_KEY, this);
        return bundle;
    }

    /**
     * Restores an instance of the main state from a bundle. This is convenient when restoring
     * instance state for an application component. The same key is always used to identify the
     * state, so only one instance of {@code MainState} can be saved to any one {@code Bundle}.
     * The restored state should be saved previously using {@link #saveToBundle(Bundle)}, so that
     * it can be identified by this method.
     *
     * @param bundle
     *     The bundle from which to restore the state. If {@code null} no state can be restored.
     *
     * @return
     *     The restored main state; or {@code null} if the bundle is {@code null}, or if the bundle
     *     contained no identifiable saved main state.
     */
    public static MainState restoreFromBundle(@Nullable Bundle bundle) {
        if (bundle != null && bundle.containsKey(BUNDLE_KEY)) {
            return (MainState) bundle.getParcelable(BUNDLE_KEY);
        }
        return null;
    }

    /**
     * Indicates if this main state is equal to another object. The main state is only equal if it
     * is also a main state object with the same state values.
     *
     * @param obj
     *     The object to be compared to this main state.
     *
     * @return
     *     {@code true} if the object is equal to this main state; or {@code false} if it is not
     *     equal.
     */
    // Parameter class check is done in "equalsIgnoreHistory" on behalf of this method.
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return equalsIgnoreHistory(obj)
                && ((MainState) obj).isHistoryEnabled() == isHistoryEnabled();
    }

    /**
     * Indicates if this main state is equal to another object, ignoring the value of the
     * {@link #isHistoryEnabled()} state. The main state is only equal if it is also a main state
     * object with the same state values, history excluded. This comparison is useful for some
     * cases, such as loading statistics, where changes to the history status is not relevant.
     *
     * @param obj
     *     The object to be compared to this main state.
     *
     * @return
     *     {@code true} if the object is equal to this main state in all respects except for the
     *     history status (which is not compared); or {@code false} if it is not equal.
     */
    public boolean equalsIgnoreHistory(Object obj) {
        return this == obj
                || (obj instanceof MainState
                    && ((MainState) obj).getPuzzleType() == getPuzzleType()
                    && ((MainState) obj).getSolveCategory().equals(getSolveCategory()));
    }

    /**
     * Gets the hash code for this main state. The hash code is consistent with {@code equals}:
     * if two {@code MainState} objects are equal, they will have the same hash code; however, if
     * two {@code MainState} objects have the same hash code, they are not necessarily equal.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        if (mHashCode == 0) {
            int code = isHistoryEnabled() ? 1 : 2;

            code = 37 * code + getPuzzleType().ordinal();
            code = 37 * code + getSolveCategory().hashCode();

            mHashCode = code;
        }

        return mHashCode;
    }

    /**
     * Gets a string representation of this main state. This is intended only to support simple
     * debugging and logging tasks.
     *
     * @return A string representation of the state.
     */
    @Override
    public String toString() {
        if (mAsString == null) {
            mAsString = getClass().getSimpleName() + "[type=" + mPuzzleType.typeName()
                    + ", category=" + getSolveCategory() + ", history=" + mIsHistoryEnabled + ']';
        }

        return mAsString;
    }

    /**
     * Writes the state information to a parcel.
     *
     * @param dest  The destination parcel to which to add the state information.
     * @param flags Ignored.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Could use "name()" but "typeName()" will be more consistent with the database and
        // preferences, so it may avoid confusion.
        dest.writeString(mPuzzleType.typeName());
        dest.writeString(mSolveCategory);
        dest.writeByte((byte) (mIsHistoryEnabled ? 1 : 0));
    }

    /**
     * Describes the kinds of special objects in this {@code Parcelable}. There are no "special
     * objects" (e.g., file descriptors).
     *
     * @return Always zero: no special objects.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@code CREATOR} factory to satisfy the canonical Android {@code Parcelable} implementation
     * pattern.
     */
    public static final Creator<MainState> CREATOR = new Creator<MainState>() {
        @Override
        public MainState createFromParcel(Parcel in) {
            return new MainState(in);
        }

        @Override
        public MainState[] newArray(int size) {
            return new MainState[size];
        }
    };
}
