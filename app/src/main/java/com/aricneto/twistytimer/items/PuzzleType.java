package com.aricneto.twistytimer.items;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * An enumeration of the different puzzle types. Utility methods to access
 * other information related to the puzzle types, such as string resource IDs
 * for human-readable puzzle names, are also provided.
 * </p>
 * <p>
 * A key feature of this enumeration is that the order of the values matches
 * the preferred order in which the puzzle types should be presented in the
 * user interface. When listed in a UI control such as a {@code Spinner}, the
 * selected index of the spinner item can be mapped
 * </p>
 *
 * @author damo
 */
public enum PuzzleType {

    /** The classic 3x3x3 Rubik's cube. **/
    // NOTE: "333" comes first, as it is (probably) the most popular type.
    TYPE_333  ("333",   R.string.cube_333,   R.string.cube_333_informal),

    /** A 2x2x2 cube. **/
    TYPE_222  ("222",   R.string.cube_222,   R.string.cube_222_informal),

    /** A 4x4x4 cube. **/
    TYPE_444  ("444",   R.string.cube_444,   R.string.cube_444_informal),

    /** A 5x5x5 cube. **/
    TYPE_555  ("555",   R.string.cube_555,   R.string.cube_555_informal),

    /** A 6x6x6 cube. **/
    TYPE_666  ("666",   R.string.cube_666,   R.string.cube_666_informal),

    /** A 7x7x7 cube. **/
    TYPE_777  ("777",   R.string.cube_777,   R.string.cube_777_informal),

    /** The Rubik's Clock puzzle. **/
    TYPE_CLOCK("clock", R.string.cube_clock, R.string.cube_clock),

    /** The Megaminx puzzle. **/
    TYPE_MEGA ("mega",  R.string.cube_mega,  R.string.cube_mega),

    /** The Pyraminx puzzle. **/
    TYPE_PYRA ("pyra",  R.string.cube_pyra,  R.string.cube_pyra),

    /** The Skewb puzzle. **/
    TYPE_SKEWB("skewb", R.string.cube_skewb, R.string.cube_skewb),

    /** The Square One puzzle. **/
    TYPE_SQ_1 ("sq1",   R.string.cube_sq1,   R.string.cube_sq1);

    /**
     * All of the puzzle values. This is used internally as it is more efficient
     * than creating a new array each time {@code Enum.values()} is called. The
     * contents are never modified.
     */
    private static final PuzzleType[] VALUES = PuzzleType.values();

    /**
     * A look-up table for associating canonical puzzle type names with
     * {@code PuzzleType} values.
     */
    private static final Map<String, PuzzleType> PUZZLE_TYPES_BY_NAME
            = new HashMap<String, PuzzleType>() {{
        for (PuzzleType puzzleType : VALUES) {
            put(puzzleType.typeName(), puzzleType);
        }
    }};

    /**
     * The name of the puzzle type as it is stored in the database and the
     * shared preferences.
     */
    @NonNull
    private final String mTypeName;

    /**
     * The string resource ID for the full, human-readable name of this puzzle
     * type.
     */
    @StringRes
    private final int mFullNameResID;

    /**
     * The string resource ID for the short, human-readable name of this puzzle
     * type. This short name may be used when sharing puzzle statistics. It may
     * be the same as the full name, if no specific short name is required.
     */
    @StringRes
    private final int mShortNameResID;

    /**
     * Creates a new puzzle type.
     *
     * @param typeName
     *     The canonical puzzle type name used when recording this puzzle type
     *     in the database or in the shared preferences.
     * @param fullNameResID
     *     The string resource ID for the full, human-readable name of this
     *     puzzle type.
     * @param shortNameResID
     *     The string resource ID for the short, human-readable name of this
     *     puzzle type. This short name may be used when sharing puzzle
     *     statistics. If there is no specific short name, use the full name
     *     here, too.
     */
    PuzzleType(@NonNull String typeName, @StringRes int fullNameResID,
               @StringRes int shortNameResID) {
        mTypeName       = typeName;
        mFullNameResID  = fullNameResID;
        mShortNameResID = shortNameResID;
    }

    /**
     * Gets the name of the puzzle type that should be used when recording
     * the puzzle type in the database or in the shared preferences.
     * <i>The {@code enum} value name should not be used!</i> To get a
     * {@code PuzzleType} value for a known type name, call
     * {@link #forTypeName(String)}.
     *
     * @return
     *     The canonical name of this puzzle type for use in the application.
     */
    // NOTE: "typeName()" rather than "getTypeName()", as it is more consistent
    // with "Enum.name()".
    @NonNull
    public String typeName() {
        return mTypeName;
    }

    /**
     * Gets the string resource ID for the full, human-readable name of this
     * type of puzzle.
     *
     * @return The string resource ID for the full name.
     */
    @StringRes
    public int getFullNameResID() {
        return mFullNameResID;
    }

    /**
     * Gets the full, human-readable name of this type of puzzle. This is resolved from the full
     * name resource ID.
     *
     * @return The full name.
     */
    public String getFullName() {
        return TwistyTimer.getAppContext().getString(getFullNameResID());
    }

    /**
     * Gets the string resource ID for the short, informal, human-readable name
     * of this type of puzzle. This shorter name may be preferred when sharing
     * solve statistics in a more compact format. For some types, this may be
     * the same as the full name.
     *
     * @return The string resource ID for the short name.
     */
    @StringRes
    public int getShortNameResID() {
        return mShortNameResID;
    }

    /**
     * Gets the short, informal, human-readable name of this type of puzzle.
     * This is resolved from the short name resource ID.
     *
     * @return The short name.
     */
    public String getShortName() {
        return TwistyTimer.getAppContext().getString(getShortNameResID());
    }

    /**
     * Gets the puzzle type that matches a given puzzle type name. See
     * {@link #typeName()} for more details.
     *
     * @param typeName
     *     The canonical name of the puzzle type.
     *
     * @return
     *     The {@code PuzzleType} value matching the given puzzle type name.
     *
     * @throws IllegalArgumentException
     *     If the given puzzle type name is not one of the supported names.
     */
    @NonNull
    public static PuzzleType forTypeName(@NonNull String typeName)
            throws IllegalArgumentException {
        final PuzzleType result = PUZZLE_TYPES_BY_NAME.get(typeName);

        // Throw an IAE, just like "Enum.valueOf(String)" would for an unknown
        // value name. This is preferable to returning null, which would require
        // logic to handle it, or returning a default value, which might mask
        // genuine bugs.
        if (result == null) {
            throw new IllegalArgumentException(
                "Unsupported puzzle type name: " + typeName);
        }

        return result;
    }

    /**
     * <p>
     * Gets the puzzle type for the given ordinal value. This performs the
     * reverse operation to the {@code ordinal()} method. When presenting lists
     * of puzzle types in the user interface, list them in their natural order
     * (per this {@code enum}), and then use this method to identify the
     * selected item when given its position in the list. Both indexing systems
     * are assumed to be zero-based.
     * </p>
     * <p>
     * This method is more efficient than {@code PuzzleType.values()[ord]}, as
     * the values array is not created each time it is called.
     * </p>
     *
     * @param ord
     *     The ordinal value of the puzzle type.
     *
     * @return
     *     The puzzle type for that ordinal value.
     *
     * @throws IllegalArgumentException
     *     If the given puzzle type ordinal is outside of the valid range.
     */
    @NonNull
    public static PuzzleType forOrdinal(int ord)
            throws IllegalArgumentException {
        if (ord < 0 || ord >= VALUES.length) {
            throw new IllegalArgumentException(
                "Ordinal value is invalid: " + ord);
        }

        return VALUES[ord];
    }

    /**
     * Gets the size of this {@code enum}. The size is the number of puzzle
     * types.
     *
     * @return The size of this {@code enum}.
     */
    public static int size() {
        return VALUES.length;
    }
}
