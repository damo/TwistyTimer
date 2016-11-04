package com.aricneto.twistytimer.items;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;

/**
 * An enumeration of the different penalties that can be applied to a
 * {@link Solve} time. Utility methods are provided to make it easy to
 * present penalties in a selection list in the user interface and to
 * interpret the selected values, or simply to get the human-readable
 * description of the penalty.
 *
 * @author damo
 */
public enum Penalty {
    // NOTE: The order of the penalties given here is the order in which they
    // will be presented in lists in the UI when users select a penalty. To
    // change that order, simply change the order of these values.

    /** No penalty. */
    NONE(R.string.no_penalty),

    /** A plus-two-seconds penalty. */
    PLUS_TWO(R.string.plus_two_penalty),

    /** A did-not-finish penalty. */
    DNF(R.string.dnf_penalty);

    /**
     * All of the penalty values. This is used internally as it is more
     * efficient than creating a new array each time {@code Enum.values()} is
     * called. The contents are never modified.
     */
    private static final Penalty[] VALUES = Penalty.values();

    /**
     * The string resource ID for the human-readable description of the penalty.
     * */
    // NOTE: Calling this "name" would confuse it with "Enum.name()".
    @StringRes
    private final int mDescriptionResID;

    /**
     * Creates a new penalty value.
     *
     * @param descriptionResID
     *     The string resource ID for the human-readable description of the
     *     penalty.
     */
    Penalty(@StringRes int descriptionResID) {
        mDescriptionResID = descriptionResID;
    }

    /**
     * Gets the string resource ID for the human-readable description of the
     * penalty.
     *
     * @return The string resource ID of the description.
     */
    @StringRes
    public int getDescriptionResID() {
        return mDescriptionResID;
    }

    /**
     * Gets the human-readable description of the penalty. The description is
     * resolved from the resource ID.
     *
     * @return The description.
     */
    public String getDescription() {
        // Do not cache this; it would not reflect run-time changes to the
        // system default locale.
        return TwistyTimer.getAppContext().getString(getDescriptionResID());
    }

    /**
     * Gets the penalty value that corresponds to the given ordinal value.
     * This is useful when presenting penalty values in a list using the
     * order provided by {@link #getDescriptions()} and then converting the
     * selected index value back to a penalty value.
     *
     * @param ord The ordinal value to be converted to a penalty code.
     *
     * @return The penalty for the given ordinal value.
     *
     * @throws IllegalArgumentException If the ordinal value is out of range.
     */
    @NonNull
    public static Penalty forOrdinal(int ord) throws IllegalArgumentException {
        if (ord < 0 || ord >= VALUES.length) {
            throw new IllegalArgumentException(
                "Ordinal value is out of range: " + ord);
        }

        return VALUES[ord];
    }

    /**
     * Gets an array of the human-readable descriptions of all of the penalty
     * code. The order is the same as the order of the values of this enum.
     * This is useful when presenting a list of penalty values from which a
     * value can be selected. Assuming zero-based indexing, the index of the
     * selected value can be converted to a {@code Penalty} by
     * {@link #forOrdinal(int)}.
     *
     * @return An array of penalty descriptions.
     */
    @NonNull
    public static String[] getDescriptions() {
        final String[] descriptions = new String[VALUES.length];
        int i = 0;

        for (Penalty penalty : VALUES) {
            descriptions[i++] = penalty.getDescription();
        }

        return descriptions;
    }
}
