package com.aricneto.twistytimer.utils;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.PuzzleType;

import java.util.Locale;

/**
 * The colors of the faces of a puzzle. Only 6-faced puzzles are supported.
 * This enum also supports translation of the default face colors for Skewb
 * and Square-1 puzzles to match the default colors for all cube puzzles and
 * the management of custom colors.
 *
 * @author damo
 */
public enum FaceColor {
    /** The color of the upper face. */
    UP   (R.string.pk_cube_color_up,    R.id.up,    0xffffff), // white

    /** The color of the bottom face. */
    DOWN (R.string.pk_cube_color_down,  R.id.down,  0xfdd835), // yellow

    /** The color of the left face. */
    LEFT (R.string.pk_cube_color_left,  R.id.left,  0xff8b24), // orange

    /** The color of the right face. */
    RIGHT(R.string.pk_cube_color_right, R.id.right, 0xec0000), // red

    /** The color of the front face. */
    FRONT(R.string.pk_cube_color_front, R.id.front, 0x02d040), // green

    /** The color of the back face. */
    BACK (R.string.pk_cube_color_back,  R.id.back,  0x304ffe); // blue

    /**
     * The faces of a "Skewb" puzzle in the order in which their TNoodle
     * default colors match the default colors of an NxNxN cube, where the
     * latter are ordered by the values of this enum.
     */
    // See https://github.com/cubing/tnoodle/blob/master/scrambles/src/puzzle/SkewbPuzzle.java
    private static final FaceColor[] SKEWB_FACE_COLOR_MAP = {
            UP,    // white  (Skewb UP    same as NxNxN cube UP)
            DOWN,  // yellow (Skewb DOWN  same as NxNxN cube DOWN)
            BACK,  // orange (Skewb BACK  same as NxNxN cube LEFT)
            FRONT, // red    (Skewb FRONT same as NxNxN cube RIGHT)
            LEFT,  // green  (Skewb LEFT  same as NxNxN cube FRONT)
            RIGHT, // blue   (Skewb RIGHT same as NxNxN cube BACK)
    };

    /**
     * The faces of a "Square-1" puzzle in the order in which their TNoodle
     * default colors match the default colors of an NxNxN cube, where the
     * latter are ordered by the values of this enum.
     */
    // See https://github.com/cubing/tnoodle/blob/master/scrambles/src/puzzle/SquareOnePuzzle.java
    private static final FaceColor[] SQ_1_FACE_COLOR_MAP = {
            DOWN,  // white  (Square-1 DOWN  same as NxNxN cube UP)
            UP,    // yellow (Square-1 UP    same as NxNxN cube DOWN)
            BACK,  // orange (Square-1 BACK  same as NxNxN cube LEFT)
            FRONT, // red    (Square-1 FRONT same as NxNxN cube RIGHT)
            RIGHT, // green  (Square-1 RIGHT same as NxNxN cube FRONT)
            LEFT,  // blue   (Square-1 RIGHT same as NxNxN cube BACK)
    };

    /**
     * The string resource ID for the key used to access the custom color value
     * for this face in the shared preferences.
     */
    @StringRes
    private final int mColorKeyResID;

    /**
     * The resource ID of the view used to present this face color when
     * customising the color through the user interface.
     */
    @IdRes
    private final int mViewID;

    /**
     * The default color of this face as a hexadecimal color string. The value
     * has six hexadecimal digits (letters are upper-case), two each for red,
     * green and blue. There is no leading '#'.
     */
    private final String mDefaultHexColor;

    /**
     * Creates a new enum value for the color of a face.
     *
     * @param colorKeyResID
     *     The string resource ID for the key to the custom color value in the
     *     shared preferences.
     * @param viewID
     *     The ID of the view used to present this face color when customising
     *     the color in the UI.
     * @param defaultColor
     *     The default color to use for the face if there is no custom color.
     */
    FaceColor(@StringRes int colorKeyResID, @IdRes int viewID,
              @ColorInt int defaultColor) {
        mColorKeyResID = colorKeyResID;
        mViewID = viewID;
        mDefaultHexColor = toHexColor(defaultColor);
    }

    /**
     * Converts the given color value to a hexadecimal color string. The string
     * will have six hexadecimal digits, two each for reg, green and blue.
     * There will be <i>no</i> leading '#' character.
     *
     * @param color The color value to be formatted as a hex string.
     * @return The color hex-string.
     */
    private static String toHexColor(@ColorInt int color) {
        return String.format(Locale.US, "%06X", color);
    }

    /**
     * Gets the face color that is presented by the view with the given ID when
     * customising the colors through the user interface.
     *
     * @param viewID The ID of the view for which the face color is required.
     *
     * @return
     *     The face color for that view, or {@code FaceColor.UP} if no match is
     *     found.
     */
    public static FaceColor forViewID(@IdRes int viewID) {
        // The use cases for this method do not require anything too fancy.
        // A linear search is OK.
        for (FaceColor faceColor : FaceColor.values()) {
            if (faceColor.getViewID() == viewID) {
                return faceColor;
            }
        }
        // This is not likely to be reached, so a guaranteed non-null return
        // value makes it easier to use this method elsewhere.
        return UP;
    }

    /**
     * Gets the resource ID of the view used to present this color in the user
     * interface.
     *
     * @return The view resource ID.
     */
    @IdRes
    public int getViewID() {
        return mViewID;
    }

    /**
     * Gets the color to use for this face expressed as a hexadecimal string.
     * The value will be the current custom value from the shared preferences,
     * or the default value if no custom value is defined. The string will have
     * six hexadecimal digits, two each for reg, green and blue. There will be
     * <i>no</i> leading '#' character.
     *
     * @return The hex-string color value for this face.
     */
    public String getHexColor() {
        return Prefs.getStringRawDefault(mColorKeyResID, mDefaultHexColor);
    }

    /**
     * Gets the color value to use for this face. The value will be the current
     * custom value from the shared preferences, or the default value if no
     * custom value is defined.
     *
     * @return The integer color value for this face.
     */
    @ColorInt
    public int getColor() {
        return Color.parseColor('#' + getHexColor());
    }

    /**
     * Sets a new custom color value to use for this face. The value will saved
     * to the shared preferences, replacing any value already there.
     *
     * @param color The new color value for this face.
     */
    public void putColor(@ColorInt int color) {
        Prefs.edit().putString(mColorKeyResID, toHexColor(color)).apply();
    }

    /**
     * Resets the custom color value for this face, restoring the default color
     * value. If the custom color value was previously saved to the shared
     * preferences, it will be deleted.
     */
    public void resetColor() {
        Prefs.edit().remove(mColorKeyResID).apply();
    }

    /**
     * Resets the custom color values for all faces, restoring the default
     * color value to each face. Any custom color values previously saved to
     * the shared preferences will be deleted.
     */
    public static void resetAllColors() {
        final Prefs.Editor editor = Prefs.edit();

        // Do this all in one "transaction", as it will be a bit more efficient
        // than calling "resetColor" for each face.
        for (FaceColor faceColor : FaceColor.values()) {
            editor.remove(faceColor.mColorKeyResID);
        }

        editor.apply();
    }

    /**
     * <p>
     * Gets the face for a given puzzle type that has the same default color as
     * an NxNxN cube puzzle has for this face.
     * </p>
     * <p>
     * This is required due to inconsistencies in the TNoodle library. For
     * example, for all TNoodle NxNxN cube puzzles, the default front face color
     * is green. Therefore, if users customises the shade of green, they would
     * expect that the change would apply to the green face of other types of
     * puzzles. However, TNoodle defines the default face colors in different
     * ways for the other supported six-faced puzzles: "Skewb" and "Square-1".
     * For example, TNoodle assigns green to the left face of "Skewb" and to
     * the right face of "Square-1".
     * </p>
     * <p>
     * For consistency, the NxNxN cube puzzles are used as a reference. When
     * applying a custom color to a "Skewb" or "Square-1" faces, the color
     * should be applied not to the face that is identified by the custom color
     * key, but to the face whose default color matched the default color of
     * the NxNxN cube puzzles. For example, if this is {@code FaceColor.FRONT},
     * then this method will return {@code FaceColor.LEFT} for the puzzle type
     * {@link PuzzleType#TYPE_SKEWB}, as the default color of the front face of
     * an NxNxN cube puzzle (green) is the same as the default color of the
     * left face of a "Skewb" puzzle.
     * </p>
     *
     * @param puzzleType
     *     The type of the puzzle for which to identify the face.
     *
     * @return
     *     The face of the given type of puzzle that has the same default color
     *     as this face of an NxNxN cube puzzle. If there is no known mapping
     *     between faces, this face will be returned.
     */
    public FaceColor toCubeFaceWithSameColor(@NonNull PuzzleType puzzleType) {
        //noinspection EnumSwitchStatementWhichMissesCases
        switch (puzzleType) {
            case TYPE_SKEWB: return SKEWB_FACE_COLOR_MAP[ordinal()];
            case TYPE_SQ_1:  return SQ_1_FACE_COLOR_MAP[ordinal()];
            default:         return this;
        }
    }
}
