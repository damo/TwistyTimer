package com.aricneto.twistytimer.scramble;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.aricneto.twistytimer.items.PuzzleType;

/**
 * A simple holder for a scramble sequence string, the puzzle type for which
 * it was generated and the image representing the scramble.
 *
 * @author damo
 */
public class ScrambleImageData extends ScrambleData {
    /**
     * The scramble image.
     */
    @NonNull
    public final Drawable image;

    /**
     * Creates a new holder for a scramble image its applicable scramble
     * sequence and puzzle type.
     *
     * @param puzzleType
     *     The type of the puzzle for which this is a scramble image.
     * @param scramble
     *     The scramble sequence used to generate the image.
     * @param image
     *     The scramble image for that scramble sequence.
     */
    public ScrambleImageData(@NonNull PuzzleType puzzleType,
                             @NonNull String scramble,
                             @NonNull Drawable  image) {
        super(puzzleType, scramble);
        this.image = image;
    }

    /**
     * Gets the string representation of this scramble image data. This is
     * suitable for logging or debugging.
     *
     * @return The string representation of this scramble image data.
     */
    @Override
    public String toString() {
        return super.toString() + " (" + image + ')';
    }
}
