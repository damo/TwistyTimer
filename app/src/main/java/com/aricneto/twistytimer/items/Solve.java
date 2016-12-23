package com.aricneto.twistytimer.items;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aricneto.twistytimer.utils.TimeUtils;
import com.aricneto.twistytimer.utils.WCAMath;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * The object representation of the result of a solve attempt. Solve instances
 * can be saved to and loaded from a database using the appropriate methods of
 * {@link com.aricneto.twistytimer.database.DatabaseHandler DatabaseHandler}.
 * When saved, solve instances are assigned a database ID. Before being saved,
 * the ID is set to {@link #NO_ID}.
 * </p>
 * <p>
 * Solves can be created manually or by importing solve data from external
 * files or database back-ups files. However, solves are typically created in
 * the context of running a {@link com.aricneto.twistytimer.timer.PuzzleTimer
 * PuzzleTimer}. On completing a solve attempt, the new solve may be saved and
 * edited. If edited, the puzzle timer may need to be kept up-to-date with
 * respect to any changes made to the elapsed time or penalties, so that the
 * changes are reflected in the timer's display. See the description of that
 * class for details on how that might be manged.
 * <p>
 * {@code Solve} implements the {@code android.os.Parcelable} interface,
 * allowing the state of a solve to be saved and restored in the context of
 * managing the state of activities and fragments. However, when a solve is
 * created in the context of a {@code PuzzleTimer}, the instance state of the
 * solve is included in the state of the timer, so it does not need to be
 * managed directly before it is ready to be saved to the database.
 * </p>
 * <p>
 * Instances of a {@code Solve} are immutable. The "setter" methods do not
 * modify the instance on which they are called; they return a new instance
 * with the property changed as directed. To avoid confusion with the normal
 * semantics of {@code setX} methods, the naming convention used for these
 * methods is {@code withX}, "Create a {@code Solve} <i>with</i> a new value
 * for property X". A "Builder Pattern" API is not provided, as it is not
 * expected that more than one property will be changed in most typical use
 * cases.
 * </p>
 * <p>
 * Immutability makes it simpler for a timer to manage a {@code Solve} instance
 * after the timer stops. Subsequent edits to that solve result do not affect
 * the {@code Solve} instance held by the timer, so the timer requires an
 * explicit notification of any changes made to the corresponding solve record,
 * thus avoiding any ambiguity. Also, when a {@code Solve} ({@code Parcelable})
 * is passed via an intent extra, there can be no accidental expectation that
 * a change to the original instance that was marshaled will be reflected in
 * the new instance created when the intent extra was unmarshaled.
 * </p>
 */
public class Solve implements Parcelable {
    /** The ID value that indicates no ID has been set on this solve. */
    // NOTE: AFAIK, Android's SQLite implementation will auto-increment the
    // "_id" column value when a record is inserted. The first automatic
    // value is 1, so any value less than 1 means that the ID is not set.
    public static final int NO_ID = 0;

    /** The database ID of the saved solve. */
    private final long id;

    /**
     * The exact elapsed time (in milliseconds) of the solve attempt
     * <i>including</i> any time penalties. This is the value stored in the
     * database. Rounding is applied when it is retrieved by {@link #getTime()}.
     */
    private final long time;

    /** The type of the puzzle that was solved. */
    @NonNull
    private final PuzzleType puzzleType;

    /** The solve category for the puzzle that was solved. */
    @NonNull
    private final String category;

    /**
     * The date-time stamp for the solve attempt. The value is in milliseconds
     * since the Unix epoch. The result of {@code System.currentTimeMillis()}
     * is an appropriate value.
     */
    private final long date;

    /**
     * The penalties incurred for the solve attempt. Any time penalties are
     * included in the reported elapsed time value.
     */
    @NonNull
    private final Penalties penalties;

    /**
     * The optional scramble sequence that was applied before the solve attempt.
     * When not set, it is represented by an empty string, as that is more
     * convenient than a {@code null} value when integrating the value with the
     * user-interface.
     */
    @NonNull
    private final String scramble;

    /**
     * The optional comment that describes the solve attempt. When not set, it
     * is represented by an empty string, as that is more convenient than a
     * {@code null} value when integrating the value with the user-interface.
     */
    @NonNull
    private final String comment;

    /**
     * The "history" flag. {@code true} if the solve is assigned to the current
     * "session", or {@code false} if it is assigned to the history of past
     * sessions.
     */
    private final boolean history;

    /**
     * The cached hash code for this {@code Solve} instance. This is calculated
     * on demand, but then never changes, as solve instances are immutable. A
     * value of zero indicates that the hash code has not been calculated yet.
     * Zero will never be used as a valid hash code value.
     */
    private int mHashCode = 0;

    /**
     * The cached string form of this solve instance. This is the form that is
     * useful for debugging and logging. It will be defined on demand.
     */
    private String mAsString;

    /**
     * The cached result string form of this solve instance. This is the form
     * that is useful when presenting the solve result to a user. It will be
     * defined on demand.
     */
    private String mAsStringResult;

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
     *     The scramble sequence applied before starting the solve. If the
     *     given value is {@code null}, the scramble will be stored as an empty.
     *     If the given value is non-{@code null}, it will be trimmed of any
     *     leading and trailing whitespace and stored as the scramble sequence
     *     value.
     */
    public Solve(@NonNull PuzzleType puzzleType, @NonNull String category,
                 @Nullable String scramble) {
        this(NO_ID, 0L, puzzleType, category, 0L, scramble,
             Penalties.NO_PENALTIES, null, false);
    }

    public Solve(long time, @NonNull PuzzleType puzzleType,
                 @NonNull String category, long date, @Nullable String scramble,
                 @Nullable Penalties penalties, @Nullable String comment,
                 boolean history) {
        this(NO_ID, time, puzzleType, category, date, scramble, penalties,
             comment, history);
    }

    public Solve(long id, long time, @NonNull PuzzleType puzzleType,
                 @NonNull String category, long date, @Nullable String scramble,
                 @Nullable Penalties penalties, @Nullable String comment,
                 boolean history) {
        this.id         = id;
        this.time       = time;
        this.puzzleType = puzzleType;
        this.category   = category;
        this.date       = date;
        this.history    = history;

        // It is assumed that any "+2" penalties are already included in the
        // "time" value.
        this.penalties = penalties == null ? Penalties.NO_PENALTIES : penalties;

        // Allow "scramble" and "comment" to be given as null, but then store
        // an empty string. This simplifies handling in the database, the UI
        // and the "Parcelable" implementation. The setter methods will set
        // the values appropriately and "trim" any whitespace, too.
        this.scramble = scramble == null ? "" : scramble.trim();
        this.comment  = comment  == null ? "" : comment.trim();
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
     * Creates a new {@code Solve} instance with the value of the database
     * record ID that was assigned to the solve when it was inserted into the
     * database. All other properties of the new instance are copied from this
     * instance.
     *
     * @param newID
     *     The new ID value to assign to the solve.
     *
     * @return
     *     A {@code Solve} instance with the ID set to the given value. This
     *     instance will be a newly-created instance if the ID is not already
     *     set to the given value, or {@code this} instance if the ID is already
     *     set to the given value.
     */
    public Solve withID(long newID) {
        if (id == newID) {
            return this;
        }

        return new Solve(
            newID, time, puzzleType, category, date, scramble, penalties,
            comment, history);
    }

    /**
     * Creates a new {@code Solve} instance with the given time value that
     * includes any time penalties already incurred on this solve. The time
     * will be stored as-is; it will not be adjusted for penalties. All other
     * properties of the new instance are copied from this instance. The time
     * should not be rounded; it is the value that will be stored in the
     * database; rounding will be applied when retrieved by {@link #getTime()}.
     *
     * @param newTime
     *     The new time to assign to the solve. The value is in milliseconds
     *     and must <i>include</i> any incurred time penalties.
     *
     * @return
     *     A {@code Solve} instance with the time set to the given value. If
     *     the time is already set to the given value, {@code this} instance is
     *     returned.
     */
    public Solve withTimeIncludingPenalties(long newTime) {
        if (time == newTime) {
            return this;
        }

        return new Solve(
            id, newTime, puzzleType, category, date, scramble, penalties,
            comment, history);
    }

    /**
     * Creates a new {@code Solve} instance with the given time value that does
     * not include any time penalties already incurred on this solve. The new
     * time value is adjusted automatically to include any time penalties
     * already incurred on this {@code Solve} instance. All other properties of
     * the new instance are copied from this instance. The time should not be
     * rounded; it is the value that will be stored in the database <i>after
     * any penalties are added</i>; rounding will be applied when retrieved by
     * {@link #getTime()}.
     *
     * @param newTime
     *     The new time to assign to the solve. The value is in milliseconds
     *     and must <i>exclude</i> any incurred penalties. Time penalties will
     *     be added automatically.
     *
     * @return
     *     A {@code Solve} instance with the time set to the given value, with
     *     appropriate adjustments for any time penalties. If the time is
     *     already set to the given value (adjusted for penalties), {@code this}
     *     instance is returned.
     */
    public Solve withTimeExcludingPenalties(long newTime) {
        return withTimeIncludingPenalties(newTime + penalties.getTimePenalty());
    }

    /**
     * Creates a new {@code Solve} instance with the given "history" flag value.
     * All other properties of the new instance are copied from this instance.
     *
     * @param newHistory
     *     The new value for the "history" flag. Use {@code true} if the solve
     *     is archived to the history of past sessions; or {@code false} if the
     *     solve is assigned to the current session.
     *
     * @return
     *     A {@code Solve} instance with the "history" flag set to the given
     *     value. If the flag is already set to the given value, {@code this}
     *     instance is returned.
     */
    public Solve withHistory(boolean newHistory) {
        if (history == newHistory) {
            return this;
        }

        return new Solve(
            id, time, puzzleType, category, date, scramble, penalties,
            comment, newHistory);
    }

    /**
     * Creates a new {@code Solve} instance with the given date-time stamp
     * value. All other properties of the new instance are copied from this
     * instance.
     *
     * @param newDate
     *     The new value for the date-time stamp. The value is in milliseconds
     *     from the Unix epoch instant (i.e., the system real time).
     *
     * @return
     *     A {@code Solve} instance with the date set to the given value. If
     *     the date is already set to the given value, {@code this} instance is
     *     returned.
     */
    public Solve withDate(long newDate) {
        if (date == newDate) {
            return this;
        }

        return new Solve(
            id, time, puzzleType, category, newDate, scramble, penalties,
            comment, history);
    }

    /**
     * Creates a new {@code Solve} instance with the given penalties without
     * adjusting the time value if there is a change to the time penalties. All
     * other properties of the new instance are copied from this instance. Call
     * {@link #withPenaltiesAdjustingTime(Penalties)} if automatic adjustment
     * of the time value is required.
     *
     * @param newPenalties
     *     The penalties to assign to the solve. If {@code null}, the penalties
     *     will be set to {@link Penalties#NO_PENALTIES}.
     *
     * @return
     *     A {@code Solve} instance with the penalties set to the given value.
     *     If the penalties value is already set to the given value (assuming
     *     {@code NO_PENALTIES} where the value is {@code null}), {@code this}
     *     instance is returned.
     */
    public Solve withPenaltiesNotAdjustingTime(
            @Nullable Penalties newPenalties) {
        if (penalties.equals(newPenalties)
            || (penalties == Penalties.NO_PENALTIES && newPenalties == null)) {
            return this;
        }

        return new Solve(
            id, time, puzzleType, category, date, scramble, newPenalties,
            comment, history);
    }

    /**
     * Creates a new {@code Solve} instance with the given penalties and adjusts
     * the time value for any <i>change</i> to the total time penalties. All
     * other properties of the new instance are copied from this instance. Call
     * {@link #withPenaltiesNotAdjustingTime(Penalties)} if adjustment of the
     * time value is not required.
     *
     * @param newPenalties
     *     The penalties to assign to the solve. If {@code null}, the penalties
     *     will be set to {@link Penalties#NO_PENALTIES}.
     *
     * @return
     *     A {@code Solve} instance with the penalties set to the given value
     *     and the time adjusted to reflect the <i>difference</i> in the time
     *     penalties before and after the change. If the penalties value is
     *     already set to the given value (assuming {@code NO_PENALTIES} where
     *     the value is {@code null}), {@code this} instance is returned.
     */
    public Solve withPenaltiesAdjustingTime(@Nullable Penalties newPenalties) {
        // NOTE: Not the most efficient implementation, as two "Solve" objects
        // are created if a time adjustment is made, but this is not expected
        // to be a high-volume op, so simplicity is preferred.
        final Solve newSolveNotAdjusted
            = withPenaltiesNotAdjustingTime(newPenalties);

        if (newSolveNotAdjusted == this) { // Penalties did not change.
            return this;
        }

        final Penalties nonNullPenalties = newSolveNotAdjusted.getPenalties();
        final long timeAdjustment
            = nonNullPenalties.getTimePenalty() - penalties.getTimePenalty();

        if (timeAdjustment != 0L) {
            return new Solve(
                id, time + timeAdjustment, puzzleType, category, date,
                scramble, nonNullPenalties, comment, history);
        }

        // Only the DNF penalties changed, so time adjustment is not needed.
        return newSolveNotAdjusted;
    }

    /**
     * Creates a new {@code Solve} instance with the given comment. All other
     * properties of the new instance are copied from this instance.
     *
     * @param newComment
     *     The comment to assign to the solve. If {@code null}, the comment will
     *     be stored as an empty string. If non-{@code null}, it will be trimmed
     *     of any leading and trailing whitespace and stored as the new comment
     *     value.
     *
     * @return
     *     A {@code Solve} instance with the comment set to the given value.
     *     If the comment value is already set to the given value (assuming
     *     and empty string where the value is {@code null}), {@code this}
     *     instance is returned.
     */
    public Solve withComment(@Nullable String newComment) {
        if (comment.equals(newComment)
            || (comment.equals("") && newComment == null)) {
            return this;
        }

        return new Solve(
            id, time, puzzleType, category, date, scramble, penalties,
            newComment, history);
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

    @NonNull
    public PuzzleType getPuzzleType() {
        return puzzleType;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public long getDate() {
        return date;
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

    /**
     * Indicates if all properties of another solve are equal to the properties
     * of this solve.
     *
     * @param obj
     *     The object to be tested against this solve for equality.
     *
     * @return
     *     {@code true} if the given object is a {@code Solve} with the same
     *     values for all of its properties as this solve; or {@code false} if
     *     the object is {@code null}, is not a {@code Solve}, or has different
     *     values for any of its properties.
     */
    @Override
    public boolean equals(Object obj) {
        return
            this == obj
            || (obj instanceof Solve
                && id == ((Solve) obj).id
                && time == ((Solve) obj).time
                && puzzleType == ((Solve) obj).puzzleType
                && category.equals(((Solve) obj).category)
                && date == ((Solve) obj).date
                && penalties.equals(((Solve) obj).penalties)
                && scramble.equals(((Solve) obj).scramble)
                && comment.equals(((Solve) obj).comment)
                && history == ((Solve) obj).history);
    }

    /**
     * Gets the hash code for this solve. The has code is consistent with the
     * {@link #equals(Object)} method: two {@code Solve} objects that are equal
     * will have the same hash code.
     *
     * @return The hash code for this solve instance. Never zero.
     */
    @Override
    public int hashCode() {
        if (mHashCode == 0) {
            int code = (int) id;

            code = 37 * code + (int) time;
            code = 37 * code + puzzleType.ordinal();
            code = 37 * code + category.hashCode();
            code = 37 * code + (int) date;
            code = 37 * code + penalties.hashCode();
            code = 37 * code + scramble.hashCode();
            code = 37 * code + comment.hashCode();
            code = 37 * code + (history ? 1 : 2);

            mHashCode = code != 0 ? code : 1;
        }

        return mHashCode;
    }

    /**
     * Gets a short string representation of the solve. This is intended to
     * be useful only for logging and debugging.
     *
     * @return A string representation of this solve.
     */
    @Override
    public String toString() {
        if (mAsString == null) {
            // The code inspections suggest *not* using "StringBuilder" (as the
            // compiler will do it, one assumes). "StringUtils.abbreviate()"
            // will shorten strings with an ellipsis if too long and cumbersome.
            // Database-format puzzle type name is more useful for debugging.
            mAsString
                = "Solve[id=" + (getID() != NO_ID ? getID() : "<none>")
                  + ", xtm=" + getExactTime()
                  + ", tm="  + getTime()
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

        return mAsString;
    }

    /**
     * Gets a string representation of this solve in "result" form. A result
     * shows the time penalties before starting the timer, the elapsed time and
     * the time penalties after stopping the timer. "DNF" penalties are also
     * indicated. For example, if the elapsed time is "24.29" seconds and there
     * is one 2-second penalty during inspection and two 2-second penalties
     * after stopping the timer before the user decides this is a "DNF", the
     * result will be represented as "2s + 24.29 + 2x2s + DNF". No total is
     * included, as that is expected to be presented separately.
     *
     * @return The string representation of this solve result.
     */
    public String toStringResult() {
        if (mAsStringResult == null) {
            final StringBuilder s = new StringBuilder(50);
            final int preStartPlus2s = penalties.getPreStartPlusTwoCount();

            if (preStartPlus2s > 0) {
                if (preStartPlus2s > 1) {
                    // "\u00d7" (multiply sign) looks nicer than "x" (letter).
                    s.append(preStartPlus2s).append('\u00d7');
                }
                // FIXME? Not using "Penalty.PLUS_TWO.getDescription()" for now.
                s.append("2s + ");
            }

            if (penalties.hasPreStartDNF()) {
                // There will be no solve time (well, it is zero), but there may
                // be "+2" penalties (appended above) before this "DNF".
                s.append(Penalty.DNF.getDescription());
                // There cannot be post-start penalties after a pre-start DNF.
            } else {
                // The solve time in the result string excludes time penalties.
                s.append(TimeUtils.formatResultTime(
                    time - penalties.getTimePenalty()));

                final int postStartPlus2s
                    = penalties.getPostStartPlusTwoCount();

                if (postStartPlus2s > 0) {
                    s.append(" + ");
                    if (postStartPlus2s > 1) {
                        s.append(postStartPlus2s).append('\u00d7');
                    }
                    s.append("2s");
                }

                if (penalties.hasPostStartDNF()) {
                    s.append(" + ").append(Penalty.DNF.getDescription());
                }
            }

            mAsStringResult = s.toString();
        }

        return mAsStringResult;
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
