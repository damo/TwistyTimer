package com.aricneto.twistytimer.scramble;

import android.support.annotation.NonNull;

import com.aricneto.twistytimer.items.PuzzleType;

/**
 * A simple holder for a scramble sequence string and the puzzle type for which
 * it was generated.
 *
 * @author damo
 */
public class ScrambleData {
    /** The puzzle type for which the scramble was generated. */
    @NonNull
    public final PuzzleType puzzleType;

    /** The scramble sequence string. */
    @NonNull
    public final String scramble;

    /**
     * Creates a new holder for a scramble sequence and its applicable puzzle
     * type.
     *
     * @param puzzleType The type of the puzzle for which this is a scramble.
     * @param scramble   The scramble sequence.
     */
    public ScrambleData(@NonNull PuzzleType puzzleType,
                        @NonNull String scramble) {
        this.puzzleType = puzzleType;
        this.scramble = scramble;
    }

    /**
     * Gets the string representation of this scramble data. This is suitable
     * for logging or debugging.
     *
     * @return The string representation of this scramble data.
     */
    @Override
    public String toString() {
        return puzzleType.typeName() + ": " + scramble;
    }
}
