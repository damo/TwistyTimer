package com.aricneto.twistytimer.items;

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;

import com.aricneto.twistytimer.utils.TimeUtils;

/**
 * <p>
 * A collection of penalties that can be encoded to a single integer. The
 * penalties are separated into those incurred before a solve attempt starts
 * (e.g., before or during an inspection period) and those incurred during or
 * after the solve attempt starts. This separation allows reporting of solve
 * times in the manner described in WCA Regulations A7b1.
 * </p>
 * <p>
 * Under the WCA Regulations, time penalties are typically cumulative. This
 * class allows penalties to be accumulated and recorded in a single integer
 * for easy storage in the database. There is a limit on the number of
 * 2-second penalties that may be incurred in the pre- and post-start phases.
 * This is based on the maximum number of regulations that can be infringed
 * without incurring a "DNF" penalty. It is not clear if some penalties could
 * be incurred repeatedly, such as the +2 penalty for touching the puzzle
 * after releasing it (A4d1 and A6e). However, it is assumed that such a
 * penalty will only be incurred once, which should be a reasonable
 * assumption in the context of a simple puzzle-timing app.
 * </p>
 * <p>
 * Instances of this class are immutable. When an additional penalty is
 * incurred, a new instance is created to represent the new penalty values.
 * </p>
 *
 * @author damo
 */
// NOTE: "final" to encourage inlining of method calls.
public final class Penalties {
    // NOTE: Immutability greatly simplifies the use of this class within a
    // "TimerState". If the instances were not immutable, they would need to
    // be protected through an extra API layer to avoid unintentional changes
    // via a "Penalties" reference returned by a "TimerState". This also
    // allows all modifications to be controlled through "PuzzleTimer", which
    // can then notify events to the user interface to ensure it is updated
    // to reflect any incurred penalties.
    //
    // NOTE: WCA Regulations A7b1:
    //
    //      'If penalties are assigned, the judge records the original
    // recorded time displayed on the timer, along with any penalties. The
    // format should be "X + T + Y = F", where X represents the sum of time
    // penalties before/starting the solve, T represents the time displayed
    // on the timer (the "original recorded time"), Y represents a sum of
    // time penalties during/after the solve, and F represents the final
    // result (e.g. 2 + 17.65 + 2 = 21.65). If X and/or Y is 0, the 0 terms
    // are omitted (e.g. 17.65 + 2 = 19.65).'

    /**
     * The duration of a {@link Penalty#PLUS_TWO} ("+2") penalty in
     * milliseconds.
     */
    public static final long PLUS_TWO_DURATION_MS = 2_000L;

    /**
     * The maximum number of "+2" penalties that can be incurred in the
     * pre-start phase of a solve attempt under the WCA Regulations.
     */
    // A3d:  +2 if the puzzle is not placed on the timer's mat on ending
    //     inspection.
    // A4b:  +2 if hands not placed properly on the timer's pads before
    //     starting the timer.
    // A4b1: +2 if the puzzle is touched again after placing it on the mat.
    //     [Repeatable?]
    // A4d1: +2 for overrunning the (15 seconds) inspection time.
    // A4e:  These pre-start penalties are cumulative.
    // Total count is 4, unless A4b1 can be repeated.
    static final int MAX_PRE_START_PLUS_TWOS = 4;

    /**
     * The maximum number of "+2" penalties that can be incurred in the
     * post-start phase of a solve attempt under the WCA Regulations.
     */
    // 10e3: +2 if the timer is stopped but one further move is required to
    //     solve the puzzle.
    // A6c:  +2 if the puzzle is not released fully before stopping the timer.
    // A6d:  +2 if hands not placed properly on the timer's pads to stop the
    //     timer.
    // A6e:  +2 if the puzzle is touched again after releasing it. [Repeatable?]
    // A6i:  These post-start penalties are cumulative.
    // Total count is 4, unless A6e can be repeated.
    static final int MAX_POST_START_PLUS_TWOS = 4;

    /**
     * <p>
     * The penalties incurred before the solve timer was started. The value
     * encodes the "DNF" and "+2" penalties that have been incurred. If there
     * are no penalties, the value is zero. If a DNF is incurred, the value
     * is incremented by one and becomes odd. Only one DNF penalty can be
     * incurred. If a "+2" is incurred, the value is incremented by 2.
     * Therefore, if the remainder when the value is divided by 2 is 1, there
     * is a DNF; and if the value is divided by 2 (integer division with
     * truncation), the result is the number of "+2" penalties incurred.
     * </p>
     * <p>
     * At present, these are only the automatic "+2" or "DNF" penalties that
     * are applied if the inspection period is overrun or times out. In
     * future, extra penalties could be applied during this period.
     * </p>
     */
    private final int mPreStartPenalties;

    /**
     * The penalties incurred after the solve timer was started, including after
     * the timer was subsequently stopped. See {@link #mPreStartPenalties} for
     * an explanation of the encoding scheme used.
     */
    private final int mPostStartPenalties;

    /**
     * The cached string representation of these penalties.
     */
    // NOTE: "Penalties" is immutable, so this is created on demand and cached
    // by "toString()".
    private String mAsString;

    /**
     * The cache of previously-created {@code Penalties} instances. The cache
     * can accommodate the universe of all possible {@code Penalties} instances.
     */
    // NOTE: As "Penalties" instances may be created rapidly during large
    // database reads, it is quite simple to cache the universe of all
    // possible instances, all of which are immutable, and simply return an a
    // cached instance when matched.
    //
    // At the time of writing, there are two phases in which penalties can be
    // incurred: pre-start and post-start. In each phase, there is a limit of
    // four "+2" penalties. Between both phases, there is a limit of one DNF
    // penalty. If the pre-start phase has a DNF, then the post-start phase
    // must have no penalties at all.
    //
    // The "universe" of all possible "Penalties" is small. With no
    // restrictions on DNFs, the total number of unique byte values for a
    // single phase is ten: from 0 to 4 "+2" penalties with or without a DNF.
    // The total number of combinations for the two bytes of the two phases
    // is 100. Using "mPostStartPenalties * 10 + mPreStartPenalties" to index
    // an array of 100 entries from "00" to "99" is sufficient to index the
    // universe of "Penalties" instances.
    //
    // However, if there is a pre-start DNF, there can be no post-start
    // penalties of any kind, so that eliminates nearly half of the possible
    // combinations. A more complicated formula for calculating the index
    // could be used, but it is simpler to accept that not all entries in the
    // array will be populated. "Penalties.obtain()" will ensure that all
    // entries are valid.
    //
    // The calculations in the code use the constants rather than just
    // hard-coding "10", as that allows for changes to the "+2" limits.
    // Different limits effectively change the indexing number base, but the
    // approach remains just as valid. The "+1" accounts for the "no +2s"
    // case and multiplying by 2 accounts for the with/without DNF cases.
    private static final Penalties[] PENALTIES_UNIVERSE
            = new Penalties[(MAX_POST_START_PLUS_TWOS + 1) * 2
                            * (MAX_PRE_START_PLUS_TWOS + 1) * 2];

    /**
     * A collection containing no penalties. As {@code Penalties} is
     * immutable, this can be used as the default value anywhere {@code null}
     * is not appropriate.
     */
    // NOTE: "obtain()" cannot be called until *after* "PENALTIES_UNIVERSE" is
    // defined above.
    public static final Penalties NO_PENALTIES = obtain(0, 0);

    /**
     * Creates a new penalties collection for the given pre- and post-start
     * penalties.
     *
     * @param preStartPenalties
     *     The penalties incurred before the solve attempt started.
     * @param postStartPenalties
     *     The penalties incurred after the solve attempt started.
     */
    private Penalties(int preStartPenalties, int postStartPenalties) {
        mPreStartPenalties  = preStartPenalties;
        mPostStartPenalties = postStartPenalties;
    }

    /**
     * Obtains a new penalties collection for the given pre- and post-start
     * penalties. The penalties collection will be retrieved from the cache
     * of known (immutable) instances, or created on demand (and cached) if
     * not already in the cache.
     *
     * @param preStartPenalties
     *     The penalties incurred before the solve attempt started.
     * @param postStartPenalties
     *     The penalties incurred after the solve attempt started.
     *
     * @return
     *     A {@code Penalties} instance for the given pre- and post-start
     *     penalties values.
     *
     * @throws IllegalArgumentException
     *     If the penalties value are invalid. Penalties are invalid if any
     *     of these conditions arise: a value is negative; there is a
     *     pre-start DNF penalty and also a post-start penalty of any kind;
     *     or the number of "+2" penalties in either the pre- or post-start
     *     phase exceeds the maximum number permitted for that phase.
     */
    private static Penalties obtain(
            int preStartPenalties, int postStartPenalties)
            throws IllegalArgumentException {
        // If "preStartPenalties" is negative, it could result in an index
        // that gives a false cache hit and bypasses the validations. For
        // example, if "postStartPenalties" is 3 (+2x1 + DNF) and
        // "preStartPenalties" is -2, then (3 * 10 - 2 = 28), which
        // represents the incorrect, but still valid combination of four
        // pre-start +2s and one post-start +2. Alternatively, if
        // "postStartPenalties" is negative, then "i" will be negative and
        // cause an array index out-of-bounds exception. These checks also
        // mean there is no need to check the lower bounds (i.e., count < 0)
        // of "getPre/PostStartPlusTwoCount()" later in this method.
        if (preStartPenalties < 0 || postStartPenalties < 0) {
            throw new IllegalArgumentException(
                "Penalties values cannot be negative: "
                + preStartPenalties + ", " + postStartPenalties);
        }

        // See comment on "PENALTIES_UNIVERSE" about the indexing scheme.
        final int i = postStartPenalties * (MAX_PRE_START_PLUS_TWOS + 1) * 2
                      + preStartPenalties;

        if (i < PENALTIES_UNIVERSE.length && PENALTIES_UNIVERSE[i] != null) {
            return PENALTIES_UNIVERSE[i]; // Already *validated* and cached.
        }

        final Penalties p // Not validated.
            = new Penalties(preStartPenalties, postStartPenalties);

        if (p.hasPreStartDNF() && p.hasPostStartPenalties()) {
            throw new IllegalArgumentException(
                "Pre-start DNF bars post-start penalties: " + p);
        }

        if (p.getPreStartPlusTwoCount() > MAX_PRE_START_PLUS_TWOS) {
            throw new IllegalArgumentException(
                "Too many pre-start +2 penalties: " + p);
        }

        if (p.getPostStartPlusTwoCount() > MAX_POST_START_PLUS_TWOS) {
            throw new IllegalArgumentException(
                "Too many post-start +2 penalties: " + p);
        }

        PENALTIES_UNIVERSE[i] = p;

        return p;
    }

    /**
     * Creates a new penalties collection from the pre- and post-start
     * penalties encoded in a single integer. If a {@code Penalties} instance
     * that defines no penalties is required, it may be simpler to use
     * {@link #NO_PENALTIES}.
     *
     * @param encodedPenalties
     *     The encoded penalties, as previously encoded by {@link #encode()}.
     *
     * @return
     *     The penalties collection decoded from the given encoded integer
     *     value.
     *
     * @throws IllegalArgumentException
     *     If the encoded penalties are invalid. Penalties are invalid if any
     *     of these conditions arise: the encoded value is negative; there is
     *     a pre-start DNF penalty and also a post-start penalty of any kind;
     *     the number of "+2" penalties in either the pre- or post-start
     *     phase exceeds the maximum number permitted for that phase; or
     *     there are unexpected bits set in the given encoded value.
     */
    // NOTE: A factory method avoids the need to throw exceptions from a
    // constructor, which is not a good idea in general.
    public static Penalties decode(int encodedPenalties)
            throws IllegalArgumentException {
        if (encodedPenalties < 0) {
            // "DatabaseHandler" uses -1 as the "fake" penalty code for a
            // "fake" solve that defines a category name. "Fakes" should have
            // been filtered out before getting here. Bug!
            throw new IllegalArgumentException(
                "Encoding is negative: " + encodedPenalties);
        }

        if ((encodedPenalties & 0xFFFF) != encodedPenalties) {
            // Stray 1's outside of bits 0-15 are not expected, so this might
            // point to a bug.
            throw new IllegalArgumentException(
                "Encoding is invalid: " + encodedPenalties);
        }

        // "obtain()" does the rest of the validation.
        return obtain(encodedPenalties & 0xFF, (encodedPenalties >>> 8) & 0xFF);
    }

    /**
     * Encodes the penalties collection into a single integer. This may be
     * stored in the database and used to recreate the penalties on retrieval
     * by calling {@link #decode(int)}.
     *
     * @return The penalties encoded to a single integer.
     */
    public int encode() {
        // NOTE: While the maximum penalty value for each phase is currently
        // 9 (+2 x 4 + DNF) and this would fit in a 4-bit field, 8-bit fields
        // are used to provide room for future expansion. 16-bit fields are
        // not used, as it might be useful in the future to have room for
        // more than two fields.
        return mPreStartPenalties | mPostStartPenalties << 8;
    }

    /**
     * Indicates if the solve attempt incurred a did-not-finish (DNF) penalty
     * in the phase before the solve timer started. At present, this is only
     * possible if the inspection period timed out after overrunning by two
     * seconds. If this is a pre-start DNF attempt, then additional penalties
     * cannot be incurred for the post-start phase. Pre-start penalties
     * cannot be annulled. A pre-start DNF will always be accompanied by at
     * least one pre-start "+2" penalty, which is incurred when the
     * inspection period overruns two seconds before it times out with a DNF.
     *
     * @return
     *     {@code true} if this attempt is a pre-start DNF; or {@code false}
     *     if it is not (yet).
     */
    public boolean hasPreStartDNF() {
        return (mPreStartPenalties & 1) == 1;
    }

    /**
     * Gets the number of "+2" penalties incurred in the phase before the
     * solve timer started. At present, only a single pre-start "+2" penalty
     * is incurred automatically if the inspection period overruns. Pre-start
     * penalties cannot be annulled.
     *
     * @return
     *     The number of "+2 penalties incurred in the pre-start phase. This
     *     is <i>not</i> the number of seconds (or milliseconds). For
     *     example, if the returned value is 4, then the additional penalty
     *     time is 4 x 2 = +8 seconds (+8,000 ms).
     */
    public int getPreStartPlusTwoCount() {
        return mPreStartPenalties >>> 1;
    }

    /**
     * Indicates if one or more pre-start penalties have been incurred.
     *
     * @return
     *     {@code true} if a pre-start "DNF" penalty and/or one or more
     *     pre-start "+2" penalties have been incurred; or {@code false} if
     *     no pre-start penalties have been incurred.
     */
    public boolean hasPreStartPenalties() {
        return mPreStartPenalties != 0;
    }

    /**
     * Indicates if the solve attempt incurred a did-not-finish (DNF) penalty
     * in the phase after the solve timer started. At present, this is only
     * possible if the user manually applied a DNF after stopping the timer.
     * Post-start penalties can be annulled.
     *
     * @return
     *     {@code true} if this attempt is a pre-start DNF; or {@code false}
     *     if it is not (yet).
     */
    public boolean hasPostStartDNF() {
        return (mPostStartPenalties & 1) == 1;
    }

    /**
     * Gets the number of "+2" penalties incurred in the phase after the
     * solve timer started. At present, post-start "+2" penalties are only
     * incurred manually by the user. If there is no pre-start DNF penalty,
     * then additional "+2" penalties can be incurred for the post-start
     * phase, but any pre-start "+2" penalty cannot be annulled.
     *
     * @return
     *     The number of "+2 penalties incurred in the post-start phase. This
     *     is <i>not</i> the number of seconds (or milliseconds). For
     *     example, if the returned value is 4, then the additional penalty
     *     time is 4 x 2 = +8 seconds (+8,000 ms).
     */
    public int getPostStartPlusTwoCount() {
        return mPostStartPenalties >>> 1;
    }

    /**
     * Indicates if one or more post-start penalties have been incurred.
     *
     * @return
     *     {@code true} if a post-start "DNF" penalty and/or one or more
     *     post-start "+2" penalties have been incurred; or {@code false} if
     *     no post-start penalties have been incurred.
     */
    public boolean hasPostStartPenalties() {
        return mPostStartPenalties != 0;
    }

    /**
     * Indicates if the solve attempt has incurred a did-not-finish (DNF)
     * penalty in either the pre-start or post-start phases.
     *
     * @return
     *     {@code true} if this attempt is a DNF; or {@code false} if it is not.
     */
    public boolean hasDNF() {
        return hasPreStartDNF() || hasPostStartDNF();
    }

    /**
     * Indicates if the solve attempt has incurred any penalties of any kind
     * in either the pre- or post-start phases.
     *
     * @return
     *     {@code true} if any penalties have been incurred; or {@code false}
     *     if no penalties have been incurred.
     */
    public boolean hasPenalties() {
        return hasPreStartPenalties() || hasPostStartPenalties();
    }

    /**
     * Gets the total time penalty to be added to the solve time. This is the
     * sum of all "+2" (2-second) penalties in the pre- and post-start phases.
     * The presence or absence of "DNF" penalties does not affect the validity
     * of any time penalty.
     *
     * @return
     *     The total time penalty (in milliseconds) to add to the solve time.
     */
    public long getTimePenalty() {
        return (getPreStartPlusTwoCount() + getPostStartPlusTwoCount())
               * PLUS_TWO_DURATION_MS;
    }

    /**
     * Incurs an additional penalty in the phase of the attempt before the
     * solve starts. This can be before, during, or after the inspection
     * period. Once the solve timer starts, these penalties are fixed and
     * cannot be annulled. At present, these are incurred automatically for
     * two conditions: a "+2" penalty for overrunning the inspection time by
     * less than two seconds, and a "DNF" penalty if the inspection period is
     * overrun by two seconds. There is no restriction on the order in which
     * the penalties may be incurred.
     *
     * @param penalty
     *     The penalty to be incurred. If {@link Penalty#NONE} or {@code null},
     *     the penalty will be ignored. Only one DNF penalty can be incurred in
     *     total across both the pre- and post-start penalties; further DNF
     *     penalties will be ignored. More than one "+2" penalty may be added
     *     and will be accumulated, but there is a maximum limit on the number
     *     of "+2" penalties. A DNF penalty does not replace a "+2" penalty
     *     already incurred. If any post-start penalties have already been
     *     incurred, even a "+2" penalty, then a pre-start DNF penalty cannot
     *     be incurred without first annulling all of the post-start penalties.
     *
     * @return
     *     A new {@code Penalties} instance with additional incurred penalty;
     *     or {@code this} instance if the penalty was not incurred.
     */
    @CheckResult // ... because *this* "Penalties" instance is not changed.
    public Penalties incurPreStartPenalty(@Nullable Penalty penalty) {
        if (penalty == Penalty.PLUS_TWO
                && getPreStartPlusTwoCount() < MAX_PRE_START_PLUS_TWOS) {
            return obtain(mPreStartPenalties + 2, mPostStartPenalties);
        }

        if (penalty == Penalty.DNF
                && !hasPreStartDNF() && !hasPostStartPenalties()) {
            return obtain(mPreStartPenalties + 1, mPostStartPenalties);
        }

        return this;
    }

    /**
     * <p>
     * Incurs an additional penalty in the phase of the attempt after the solve
     * timer starts. This can be while the solve timer is running, or after
     * the solve timer has stopped. These penalties are not fixed and can be
     * annulled by {@link #annulPostStartPenalty(Penalty)}. At present, the
     * user interface allows these to be incurred "manually" by the user after
     * the solve timer is stopped. There is no restriction on the order in
     * which the penalties may be incurred.
     * </p>
     * <p>
     * See also {@link #canIncurPostStartPenalty(Penalty)}.
     * </p>
     *
     * @param penalty
     *     The penalty to be incurred. If {@link Penalty#NONE} or {@code null},
     *     the penalty will be ignored. Only one DNF penalty can be incurred in
     *     total across both the pre- and post-start penalties; further DNF
     *     penalties will be ignored. More than one "+2" penalty may be added
     *     and will be accumulated, but there is a maximum limit on the number
     *     of "+2" penalties. A DNF penalty does not replace a "+2" penalty
     *     already incurred. If a pre-start DNF has been incurred, no post-start
     *     DNF or "+2" penalties may be incurred.
     *
     * @return
     *     A new {@code Penalties} instance with additional incurred penalty;
     *     or {@code this} instance if the penalty was not incurred.
     */
    @CheckResult // ... because *this* "Penalties" instance is not changed.
    public Penalties incurPostStartPenalty(@Nullable Penalty penalty) {
        if (!hasPreStartDNF()) {
            if (penalty == Penalty.PLUS_TWO
                    && getPostStartPlusTwoCount() < MAX_POST_START_PLUS_TWOS) {
                return obtain(mPreStartPenalties, mPostStartPenalties + 2);
            }

            // Checked pre-start DNF above, just check post-start DNF here.
            if (penalty == Penalty.DNF && !hasPostStartDNF()) {
                return obtain(mPreStartPenalties, mPostStartPenalties + 1);
            }
        }

        return this;
    }

    /**
     * Indicates if a further post-start penalty can be incurred. See the
     * description of {@link #incurPostStartPenalty(Penalty)} for details of
     * the conditions under which post-start penalties may or may not be
     * incurred. No changes are made to this {@code Penalties} instance.
     *
     * @param penalty
     *     The penalty proposed as the extra post-start penalty.
     *
     * @return
     *     {@code true} if the given post-start penalty can be incurred; or
     *     {@code false} if the penalty cannot be incurred or is {@code null}.
     */
    public boolean canIncurPostStartPenalty(@Nullable Penalty penalty) {
        // This is the simplest implementation, though it has the overhead of
        // creating and discarding objects when the result is "true".
        return incurPostStartPenalty(penalty) != this;
    }

    /**
     * <p>
     * Annuls a penalty incurred in the phase of the attempt after the solve
     * timer starts. This can be while the solve timer is running, or after
     * the solve timer has stopped. There is no restriction on the order in
     * which the penalties may be annulled. If there are multiple "+2"
     * penalties, only one will be annulled with each call to this method.
     * </p>
     * <p>
     * See also {@link #canAnnulPostStartPenalty(Penalty)}.
     * </p>
     *
     * @param penalty
     *     The penalty to be annulled. If the penalty was not previously
     *     incurred, or all penalties of that type have already been annulled,
     *     it will be ignored. If {@link Penalty#NONE} or {@code null}, no
     *     penalty will be annulled.
     *
     * @return
     *     A new {@code Penalties} instance with one penalty annulled; or
     *     {@code this} instance if no penalty was annulled.
     */
    @CheckResult // ... because *this* "Penalties" instance is not changed.
    public Penalties annulPostStartPenalty(@Nullable Penalty penalty) {
        if (penalty == Penalty.PLUS_TWO && getPostStartPlusTwoCount() > 0) {
            return obtain(mPreStartPenalties, mPostStartPenalties - 2);
        }

        if (penalty == Penalty.DNF && hasPostStartDNF()) {
            return obtain(mPreStartPenalties, mPostStartPenalties - 1);
        }

        return this;
    }

    /**
     * Indicates if a post-start penalty can be annulled. See the description
     * of {@link #annulPostStartPenalty(Penalty)} for details of the conditions
     * under which post-start penalties may or may not be annulled. No changes
     * are made to this {@code Penalties} instance.
     *
     * @param penalty
     *     The penalty proposed as the extra post-start penalty.
     *
     * @return
     *     {@code true} if the given post-start penalty can be incurred; or
     *     {@code false} if the penalty cannot be incurred or is {@code null}.
     */
    public boolean canAnnulPostStartPenalty(@Nullable Penalty penalty) {
        return annulPostStartPenalty(penalty) != this;
    }

    /**
     * Gets the hash code for this collection of penalties. The hash code is
     * consistent with {@link #equals(Object)}: penalties with different hash
     * codes will not be equal.
     *
     * @return The hash code for these penalties.
     */
    @Override
    public int hashCode() {
        return encode();
    }

    /**
     * Indicates if these penalties are equal to another object. Penalties
     * are equal if the other object is not {@code null}, is an instance of
     * {@code Penalties}, if the pre-start penalties are equal in both
     * objects and if the post-start penalties are equal in both objects. The
     * order in which the penalties were incurred does not matter, but the
     * phase in which they were incurred does. For example, if a single "+2"
     * penalty was incurred in the pre-start phase for this object, it will
     * not be equal to another object where a "+2" penalty was incurred in
     * the post-start phase.
     *
     * @param obj The object to test for equality with this object.
     *
     * @return
     *     {@code true} if the objects are equal, or {@code false} if they are
     *     not.
     */
    @Override
    public boolean equals(Object obj) {
        // NOTE: While the "PENALTIES_UNIVERSE" cache guarantees that equal
        // penalties objects are the *same* instance, that may not hold
        // forever, so it is not relied upon here.
        return this == obj
                || (obj instanceof Penalties // so not null
                    // Two birds, one stone:
                    && encode() == ((Penalties) obj).encode());
    }

    /**
     * Gets a compact string representation of this collection of penalties.
     * The penalties are described in the form: "{pre-start,post-start}", for
     * example, "{+2x1,DNF}".
     *
     * @return A string representation of these penalties.
     */
    @Override
    public String toString() {
        if (mAsString == null) {
            final StringBuilder s = new StringBuilder(32);

            s.append('{');

            if (getPreStartPlusTwoCount() > 0) {
                s.append("+2x").append(getPreStartPlusTwoCount());
                if (hasPreStartDNF()) {
                    s.append("+DNF");
                }
            } else if (hasPreStartDNF()) {
                s.append("DNF");
            } else {
                s.append("none");
            }

            s.append(',');

            if (getPostStartPlusTwoCount() > 0) {
                s.append("+2x").append(getPostStartPlusTwoCount());
                if (hasPostStartDNF()) {
                    s.append("+DNF");
                }
            } else if (hasPostStartDNF()) {
                s.append("DNF");
            } else {
                s.append("none");
            }

            s.append('}');
            mAsString = s.toString();
        }

        return mAsString;
    }
}
