package com.aricneto.twistytimer.items;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aricneto.twistytimer.timer.TimerState;
import com.aricneto.twistytimer.utils.WCAMath;

import org.apache.commons.lang3.StringUtils;

/**
 * Stores a solve. Solves can be converted to parcels, allowing their state
 * to be saved and restored in the context of managing the user-interface
 * elements.
 */
public class Solve implements Parcelable {
    /**
     * The ID value that indicates no ID has been set on this solve.
     */
    // NOTE: AFAIK, Android's SQLite implementation will auto-increment the
    // "_id" column value when a record is inserted. The first automatic
    // value is 1, so any value less than 1 means that the ID is not set.
    public static final int NO_ID = 0;

    private long       id = NO_ID;

    private long       time;

    @NonNull
    private PuzzleType puzzleType;

    @NonNull
    private String     category;

    private long       date;

    @NonNull
    private String     scramble = "";

    @NonNull
    private Penalties  penalties;

    @NonNull
    private String     comment = "";

    private boolean    history;

    /**
     * Creates a new solve ready to record a solve attempt. The solve will
     * maintain information while the puzzle timer is running. The details will
     * be completed when the timer stops. This does not set any ID or elapsed
     * time and sets the comment to an empty string. The "history" flag is not
     * set, so the solve will be created for the current session. The date-time
     * stamp is set to zero and will be set when the timer stops.
     *
     * @param puzzleType
     *     The puzzle type for this solve attempt.
     * @param category
     *     The solve category for this solve attempt.
     * @param scramble
     *     The scramble sequence applied before starting the solve. May be
     *     {@code null} if no scramble sequence was applied.
     */
    public Solve(@NonNull PuzzleType puzzleType, @NonNull String category,
                 @Nullable String scramble) {
        this(NO_ID, 0L, puzzleType, category, 0L, scramble,
            Penalties.NO_PENALTIES, null, false);
    }

    public Solve(long time, @NonNull PuzzleType puzzleType,
                 @NonNull String category, long date, @Nullable String scramble,
                 @NonNull Penalties penalties, @Nullable String comment,
                 boolean history) {
        this(NO_ID, time, puzzleType, category, date, scramble, penalties,
            comment, history);
    }

    public Solve(long id, long time, @NonNull PuzzleType puzzleType,
                 @NonNull String category, long date, @Nullable String scramble,
                 @NonNull Penalties penalties, @Nullable String comment,
                 boolean history) {
        this.id         = id;
        this.time       = time;
        this.puzzleType = puzzleType;
        this.category   = category;
        this.date       = date;
        // It is assumed that any "+2" penalties are already included in the
        // "time" value.
        this.penalties  = penalties;
        this.history    = history;

        // Allow "scramble" and "comment" to be given as null, but then store
        // an empty string. This simplifies handling in the database, the UI
        // and the "Parcelable" implementation. The setter methods will set
        // the values appropriately and "trim" any whitespace, too.
        setScramble(scramble);
        setComment(comment);
    }

    /**
     * Creates a solve from a {@code Parcel}.
     *
     * @param in The parcel from which to read the details of the solve.
     */
    protected Solve(@NonNull Parcel in) {
        this(in.readLong(),          // "id"
                in.readLong(),       // "time"
                PuzzleType.forTypeName(in.readString()),
                in.readString(),     // "category" is never null.
                in.readLong(),       // "date"
                in.readString(),     // "scramble" may be empty, but never null.
                Penalties.decode(in.readInt()),
                in.readString(),     // "comment" may be empty, but never null.
                in.readByte() != 0); // "history"
    }

    /**
     * Sets the value of the database record ID that was assigned to this
     * solve when it was inserted into the database.
     *
     * @param id The ID value to assign to this solve.
     */
    public void setID(long id) {
        this.id = id;
    }

    /**
     * Gets the value of the database record ID that was assigned to this
     * solve when it was inserted into the database. The {@link #hasID()}
     * method may be more convenient if testing whether or not an ID has been
     * set.
     *
     * @return
     *     The ID of this solve, or {@link #NO_ID} if the ID has not been set.
     */
    public long getID() {
        return id;
    }

    /**
     * Indicates if this solve has been assigned a database record ID.
     *
     * @return
     *     {@code true} if an ID has been assigned; or {@code false} if no ID
     *     has been assigned (i.e., if the value of the ID is {@link #NO_ID}).
     */
    public boolean hasID() {
        return id != NO_ID;
    }

    /**
     * Gets the elapsed time for this solve, including penalties. The time
     * will be rounded or truncated in accordance with WCA Regulations 9f1
     * and 9f2 (April 18, 2016). This time is suitable for use in
     * calculations of averages and means and for presentation in the user
     * interface. To get the exact time to record in the database, use
     * {@link #getExactTime()}.
     *
     * @return The elapsed time (in milliseconds), rounded per WCA Regulations.
     */
    public long getTime() {
        // NOTE: Round or truncate the time as it is recalled, not when it is
        // recorded. This ensures that the time is recalled in a manner that
        // conforms to the WCA Regulations as they applied on the date on
        // which the time was recorded. It does not matter if this code or
        // the app was up-to-date with the WCA Regulations when the solve was
        // recorded. Once the code and app are updated, the Regulations
        // effective on the "date" of the solve can be applied to the
        // recorded time.
        //
        // This approach requires that if the WCA Regulations change, that
        // this code will be updated to apply the new regulations to new
        // times and the old regulations to old times. (Alternatively, add
        // "date" as a parameter to "WCAMath.roundResult" and put the logic
        // there.) If that never happens, then this approach will still work
        // well enough.
        //
        // Probably most important, this approach means that any errors in
        // the rounding code can be corrected, as the time values recorded in
        // the database are never "corrupted" by incorrect rounding and
        // always retain their original precision to 1 ms.
        return WCAMath.roundResult(time);
    }

    /**
     * Gets the exact recorded solve time before any rounding. This should
     * only be used when recording the time in the database. For calculations
     * using times, or for the presentation of times in the user interface,
     * {@link #getTime()} should be called to ensure that the time is rounded
     * correctly in accordance with WCA Regulations.
     *
     * @return
     *     The exact recorded solve time (in milliseconds) with millisecond
     *     precision preserved.
     */
    public long getExactTime() {
        return time;
    }

    /**
     * Sets the result time for this solve including time penalties. The
     * given time will be recorded and stored in the database.
     *
     * @param time
     *     The time (in milliseconds) recorded for this solve, including any
     *     applied penalties.
     */
    public void setTime(long time) {
        this.time = time;
    }

    @NonNull
    public PuzzleType getPuzzleType() {
        return puzzleType;
    }

    public void setPuzzleType(@NonNull PuzzleType puzzleType) {
        this.puzzleType = puzzleType;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    /**
     * Gets the value of the scramble sequence for this solve. If no scramble
     * was set, or it was explicitly set to {@code null}, the returned
     * scramble will be an empty string. If testing whether or not a scramble
     * exists, {@link #hasScramble()} may be more convenient.
     *
     * @return
     *     The scramble sequence, or an empty string if there is no scramble.
     */
    @NonNull
    public String getScramble() {
        return scramble;
    }

    /**
     * Sets the value of the scramble sequence. If the given value is {@code
     * null}, the scramble will be stored as an empty string. If the given
     * value is non-{@code null}, it will be trimmed of any leading and
     * trailing whitespace and stored as the scramble sequence value.
     *
     * @param scramble The scramble sequence.
     */
    public void setScramble(@Nullable String scramble) {
        this.scramble = scramble == null ? "" : scramble.trim();
    }

    /**
     * Indicates if this solve has a scramble sequence or not. The scramble
     * sequence will be returned as an empty string if it was not set, or was
     * set to {@code null}. This is a convenience method that will perform
     * the necessary checks.
     *
     * @return
     *     {@code true} if this solve has a scramble; or {@code false} if it
     *     has no scramble.
     */
    public boolean hasScramble() {
        return !scramble.isEmpty();
    }

    /**
     * Gets the penalties that were incurred in this solve attempt.
     *
     * @return The penalties incurred for this solve.
     */
    @NonNull
    public Penalties getPenalties() {
        return penalties;
    }

    /**
     * Sets the penalties for this solve. No corrections will be made to the
     * time if the new penalty, or the replaced penalty, is a "+2" penalty.
     *
     * @param penalties
     *     The penalties to set on this solve. If {@code null}, the penalties
     *     will be set to {@link Penalties#NO_PENALTIES}.
     */
    // FIXME: Not sure where this fits in any more. Is it here to support the
    // EditSolveDialog?
    public void setPenalties(@Nullable Penalties penalties) {
        this.penalties = penalties == null ? Penalties.NO_PENALTIES : penalties;
    }

    /**
     * Gets the value of the comment for this solve. If no comment was set,
     * or it was explicitly set to {@code null}, the returned comment will be
     * an empty string. If testing whether or not a comment exists,
     * {@link #hasComment()} may be more convenient.
     *
     * @return The comment, or an empty string if there is no comment.
     */
    @NonNull
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment. If the given value is {@code null}, the
     * comment will be stored as an empty string. If the given value is
     * non-{@code null}, it will be trimmed of any leading and trailing
     * whitespace and stored as the comment value.
     *
     * @param comment
     *     The comment. If {@code null}, the comment will be set to an empty
     *     string.
     */
    public void setComment(@Nullable String comment) {
        this.comment = comment == null ? "" : comment.trim();
    }

    /**
     * Indicates if this solve has a comment or not. The comment will be
     * returned as an empty string if it was not set, or was set to {@code
     * null}. This is a convenience method that will perform the necessary
     * checks.
     *
     * @return
     *     {@code true} if this solve has a comment; or {@code false} if it has
     *     no comment.
     */
    public boolean hasComment() {
        return !comment.isEmpty();
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }

    /**
     * Completes the details of this solve from a timer state. This updates
     * the time, date-time stamp and penalties of the solve; the other
     * properties remain unchanged. This can be called more than once on a
     * solve to update the affected properties. The return value can be used
     * to determine if any changes were made that will require the solve to
     * be re-saved.
     *
     * @param timerState
     *     The timer state from which to update this solve.
     * @param newDate
     *     The new date-time stamp to be recorded for this solve attempt.
     *
     * @return
     *     {@code true} if a new value is assigned to any of the updated
     *     properties; or {@code false} if this solve is already up-to-date
     *     with respect to the timer state.
     */
    public boolean completeFrom(@NonNull TimerState timerState, long newDate) {
        final Penalties newPenalties = timerState.getPenalties();
        // "getResultTime()" includes any and all "+2" penalties.
        final long newTime = timerState.getResultTime();

        if (time != newTime || date != newDate
            || !penalties.equals(newPenalties)) {
            time = newTime;
            date = newDate;
            penalties = newPenalties;

            return true; // Something changed.
        }

        return false; // Nothing changed.
    }

    /**
     * Gets a short string representation of the solve. This is intended to
     * be useful only for logging and debugging.
     *
     * @return A string representation of this solve.
     */
    @Override
    public String toString() {
        // The code inspections suggest *not* using "StringBuilder" (as the
        // compiler will do it, one assumes). "StringUtils.abbreviate()" will
        // shorten strings with an ellipsis if too long and cumbersome.
        return "Solve[id=" + getID()
                + ", xtm=" + getExactTime()
                + ", tm="  + getTime()
               // Database-format puzzle type name is more useful for debugging.
                + ", puz=" + getPuzzleType().typeName()
                + ", cat=" + getCategory()
                + ", dt="  + getDate() // Nothing fancy.
                + ", pen=" + getPenalties()
                + ", scr=" + (getScramble().length() > 15
                        ? StringUtils.abbreviate(getScramble(), 15)
                        : hasScramble() ? getScramble() : "<none>")
                + ", cmt=" + (getComment().length() > 15
                        ? StringUtils.abbreviate(getComment(), 15)
                        : hasComment() ? getComment() : "<none>")
                + ", hst=" + (isHistory() ? 'y' : 'n')
                + ']';
    }

    /**
     * Writes this solve to a parcel.
     *
     * @param dest  The parcel to which to write the solve data.
     * @param flags Ignored. Use zero.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(time);
        dest.writeString(puzzleType.typeName()); // Never null.
        dest.writeString(category);              // Never null.
        dest.writeLong(date);
        dest.writeString(scramble);              // Never null; may be empty.
        dest.writeInt(penalties.encode());       // Never null.
        dest.writeString(comment);               // Never null; may be empty.
        dest.writeByte((byte) (history ? 1 : 0));
    }

    /**
     * Describes any special contents of this parcel. There are none.
     *
     * @return Zero, as there are no special contents.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Standard creator for parcels containing a {@link Solve}.
     */
    public static final Creator<Solve> CREATOR = new Creator<Solve>() {
        @Override
        public Solve createFromParcel(Parcel in) {
            return new Solve(in);
        }

        @Override
        public Solve[] newArray(int size) {
            return new Solve[size];
        }
    };
}
