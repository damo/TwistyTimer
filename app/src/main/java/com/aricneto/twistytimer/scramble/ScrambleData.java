package com.aricneto.twistytimer.scramble;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aricneto.twistytimer.items.PuzzleType;

/**
 * A simple holder for a scramble sequence string, the puzzle type for which
 * it was generated and a corresponding scramble image, if an image is
 * available. If an instance of this class is saved to a parcel, the image will
 * <i>not</i> be saved; it should be re-generated from the restored scramble
 * sequence if necessary.
 *
 * @author damo
 */
public class ScrambleData implements Parcelable {
    /** The puzzle type for which the scramble was generated. */
    @NonNull
    public final PuzzleType puzzleType;

    /** The scramble sequence string that was generated. */
    @NonNull
    public final String scramble;

    /** An image corresponding to the scramble sequence. */
    @Nullable
    public final Drawable image;

    /**
     * Creates a new holder for a scramble sequence and its applicable puzzle
     * type. The scramble data will contain no scramble image.
     *
     * @param puzzleType The type of the puzzle for which this is a scramble.
     * @param scramble   The scramble sequence.
     */
    public ScrambleData(@NonNull PuzzleType puzzleType,
                        @NonNull String scramble) {
        this(puzzleType, scramble, null);
    }

    /**
     * Creates a new holder for a scramble sequence and its applicable puzzle
     * type and corresponding scramble image.
     *
     * @param puzzleType The type of the puzzle for which this is a scramble.
     * @param scramble   The scramble sequence.
     * @param image      The optional scramble image. May be {@code null}.
     */
    public ScrambleData(@NonNull PuzzleType puzzleType,
                        @NonNull String scramble, @Nullable Drawable image) {
        this.puzzleType = puzzleType;
        this.scramble   = scramble;
        this.image      = image;
    }

    /**
     * Creates a new holder with the given scramble image and copies the
     * scramble sequence and its applicable puzzle type from this instance.
     * If the image is the same as that already set (including {@code null}),
     * {@code this} instance will be returned unchanged.
     *
     * @param newImage The image to include in the new scramble data instance.
     *
     * @return The new instance, or {@code this} if no change was required.
     */
    public ScrambleData withImage(@Nullable Drawable newImage) {
        if (image == newImage) {
            // Same "Drawable" or both null, so nothing changes.
            return this;
        }

        return new ScrambleData(puzzleType, scramble, newImage);
    }

    /**
     * Creates a holder for a scramble sequence from a parcel.
     *
     * @param in The parcel containing the scramble information.
     */
    protected ScrambleData(Parcel in) {
        this(PuzzleType.forTypeName(in.readString()), in.readString());
    }

    /**
     * Gets the string representation of this scramble data. This is suitable
     * for logging or debugging.
     *
     * @return The string representation of this scramble data.
     */
    @Override
    public String toString() {
        return "ScrambleData=[" + puzzleType.typeName() + ", '" + scramble
               + "', " + (image == null ? "<no image>" : image) + ']';
    }

    /**
     * Writes the scramble data to a parcel. The scramble image is not written.
     *
     * @param dest
     *     The destination parcel to which to add the scramble data.
     * @param flags
     *     Ignored.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Could use "name()" but "typeName()" will be more consistent with the
        // database, preferences and "MainState", so it may avoid confusion.
        dest.writeString(puzzleType.typeName());
        dest.writeString(scramble);
    }

    /**
     * Describes the kinds of special objects in this {@code Parcelable}. There
     * are no "special objects" (e.g., file descriptors).
     *
     * @return Always zero: no special objects.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@code CREATOR} factory to satisfy the canonical Android
     * {@code Parcelable} implementation pattern.
     */
    public static final Creator<ScrambleData> CREATOR
        = new Creator<ScrambleData>() {
        @Override
        public ScrambleData createFromParcel(Parcel in) {
            return new ScrambleData(in);
        }

        @Override
        public ScrambleData[] newArray(int size) {
            return new ScrambleData[size];
        }
    };
}
