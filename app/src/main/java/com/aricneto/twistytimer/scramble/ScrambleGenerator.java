package com.aricneto.twistytimer.scramble;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.support.annotation.NonNull;

import com.aricneto.twistytimer.utils.FaceColor;
import com.aricneto.twistytimer.items.PuzzleType;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;
import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.svglite.Color;

import java.util.HashMap;

import puzzle.ClockPuzzle;
import puzzle.CubePuzzle;
import puzzle.MegaminxPuzzle;
import puzzle.NoInspectionFiveByFiveCubePuzzle;
import puzzle.NoInspectionFourByFourCubePuzzle;
import puzzle.PyraminxPuzzle;
import puzzle.SkewbPuzzle;
import puzzle.SquareOneUnfilteredPuzzle;
import puzzle.ThreeByThreeCubePuzzle;
import puzzle.TwoByTwoCubePuzzle;

/**
 * Util for generating and drawing scrambles
 */
public class ScrambleGenerator {
    /**
     * The TNoodle puzzle.
     */
    private final Puzzle mPuzzle;

    /**
     * The selected puzzle type at the time this scramble generator was created.
     */
    @NonNull
    private final PuzzleType mPuzzleType;

    /**
     * The color scheme to apply if generating an image of the scrambled puzzle.
     */
    // The TNoodle API takes a "HashMap", not a "Map". Fail!
    private final HashMap<String, Color> mColorScheme;

    /**
     * The color scheme for the puzzle.
     *
     * @param puzzleType The type of puzzle for which to generate a scramble.
     */
    public ScrambleGenerator(@NonNull PuzzleType puzzleType) {
        mPuzzleType = puzzleType;

        switch (puzzleType) {
            default:
            case TYPE_333:   mPuzzle = new ThreeByThreeCubePuzzle(); break;
            case TYPE_222:   mPuzzle = new TwoByTwoCubePuzzle(); break;
            case TYPE_444:   mPuzzle = new NoInspectionFourByFourCubePuzzle(); break;
            case TYPE_555:   mPuzzle = new NoInspectionFiveByFiveCubePuzzle(); break;
            case TYPE_666:   mPuzzle = new CubePuzzle(6); break;
            case TYPE_777:   mPuzzle = new CubePuzzle(7); break;
            case TYPE_MEGA:  mPuzzle = new MegaminxPuzzle(); break;
            case TYPE_PYRA:  mPuzzle = new PyraminxPuzzle(); break;
            case TYPE_SKEWB: mPuzzle = new SkewbPuzzle(); break;
            case TYPE_CLOCK: mPuzzle = new ClockPuzzle(); break;
            case TYPE_SQ_1:  mPuzzle = new SquareOneUnfilteredPuzzle(); break;
        }

        mColorScheme = getCustomColorScheme(puzzleType, mPuzzle);
    }

    /**
     * Gets the type of puzzle for which scrambles will be generated.
     */
    public PuzzleType getPuzzleType() {
        return mPuzzleType;
    }

    /**
     * Gets the color scheme to use for the given type of puzzle.
     *
     * @param puzzleType The TwistyTimer puzzle type.
     * @param puzzle     The TNoodle puzzle.
     */
    // As noted for the field, TNoodle wants "HashMap", not "Map".
    private static HashMap<String, Color> getCustomColorScheme(
            @NonNull PuzzleType puzzleType, Puzzle puzzle) {
        // NOTE: This is not much different from the previous implementation, so it has the same
        // issues:
        //
        //  o "Puzzle.parseColorScheme" accepts the color values in a comma-separated values (CSV)
        //    list. It copies the default color scheme and changes the colors in that scheme to
        //    match the colors given in the CSV. The names of the faces (internal to TNoodle) are
        //    sorted into alphabetical order and then the first color in the CSV is applied to the
        //    first face, and so on. However, the face names, and the number of faces, are not
        //    consistent across all puzzle types. If the number of CSV entries does not match the
        //    number of faces, the custom color scheme will be "null". "Puzzle.drawScramble" will
        //    use the default scheme if a "null" color scheme is passed to it. Therefore, this
        //    custom color scheme will only work for puzzles with six faces and will have no effect
        //    on any others. To make this clear, a condition has been added to this method, though
        //    it would work without it.
        //
        //  o When generating an image, the faces are "unfolded" in different ways for different
        //    puzzle types, so the face names cannot be interpreted consistently.
        //
        //  o The default color schemes are inconsistent across puzzle types that have the same
        //    number of faces and face names.

        // Only the six-faced puzzles are supported: all of the NxNxN cube puzzles, "Skewb" and
        // "Square-1". For others, a "null" custom color scheme will cause the default color
        // scheme to be used.
        if (puzzle.getDefaultColorScheme().size() != 6) {
            return null;
        }

        // The supported puzzles have the same 'U', 'D', 'L', 'R', 'F', 'B' face names, but may
        // have different default colors assigned to those faces. Therefore, some juggling is
        // required to ensure that the custom colors are applied consistently. For example, NxNxN
        // cube puzzles (the reference for the custom colors) use green for the 'F' face, but
        // "Skewb" uses green for the 'L' face. The custom colors are defined in the CSV in
        // alphabetical order of the face names, so the CSV order is 'B', 'D', 'F', 'L', 'R', 'U'.
        // For example, when setting the fourth CSV field, the 'L' face color, for a "Skewb" puzzle,
        // the value used should be the "custom green color", because 'L' is normally green for
        // "Skewb". However, because NxNxN cube puzzles are the reference for custom colors, the
        // "custom green color" is stored in the preferences as the "cubeFront" color. Therefore,
        // FaceColor.LEFT must be mapped to FaceColor.FRONT before the hex-color value is retrieved.
        // This is mapped by, and described in more detail in, "FaceColor.toCubeFaceWithSameColor".

        // Order is alphabetical: 'B', 'D', 'F', 'L', 'R', 'U'. Do not depend on the face names
        // being as given (letter-case could be different, etc.). Therefore let "parseColorScheme"
        // do the mapping, rather than just put the values into the HashMap.
        return puzzle.parseColorScheme(
                FaceColor.BACK.toCubeFaceWithSameColor(puzzleType).getHexColor() + ","
                + FaceColor.DOWN.toCubeFaceWithSameColor(puzzleType).getHexColor() + ","
                + FaceColor.FRONT.toCubeFaceWithSameColor(puzzleType).getHexColor() + ","
                + FaceColor.LEFT.toCubeFaceWithSameColor(puzzleType).getHexColor() + ","
                + FaceColor.RIGHT.toCubeFaceWithSameColor(puzzleType).getHexColor() + ","
                + FaceColor.UP.toCubeFaceWithSameColor(puzzleType).getHexColor());
    }

    /**
     * Generate a new scramble for the defined puzzle type.
     *
     * @return A string describing the scramble sequence in the standard notation.
     */
    public String generateScramble() {
        return mPuzzle.generateScramble();
    }

    /**
     * Gets a drawable showing the puzzled in its scrambled state.
     *
     * @param scramble The scramble string in conventional notation.
     * @return The drawable to present the scrambled image.
     */
    public Drawable generateImageFromScramble(String scramble) {
        String cubeImgSVG = null;

        try {
            cubeImgSVG = mPuzzle.drawScramble(scramble, mColorScheme).toString();
        } catch (InvalidScrambleException e) {
            e.printStackTrace();
        }

        Drawable pic = null;

        if (cubeImgSVG != null) {
            try {
                pic = new PictureDrawable(SVG.getFromString(cubeImgSVG).renderToPicture());
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
        }

        return pic;
    }
}
