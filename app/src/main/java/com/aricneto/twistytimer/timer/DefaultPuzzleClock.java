package com.aricneto.twistytimer.timer;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

/**
 * <p>
 * An implementation of a {@link PuzzleClock}, that drives the operation of a
 * {@link PuzzleTimer} in a production context on an Android device. See the
 * interface description for more details on the purpose and contract of
 * clock implementations in general and their typical limitations.
 * </p>
 * <p>
 * This implementation uses {@code android.os.SystemClock.elapsedRealtime()}
 * as the time source. This is the system uptime including time spent in
 * sleep states; in essence, the time since the device was last booted.
 * Unlike the system "wall-time" clock, this time source is not subject to
 * unwanted adjustments, such as DST change-overs, NTP clock synchronisation,
 * or leap seconds. This makes it a more suitable time source for measuring
 * the elapsed time when solving puzzles.
 * </p>
 * <p>
 * The "tick" events generated by this clock are scheduled on the message
 * queue of the main (UI) thread's {@code Looper}. This ensures that the
 * events are notified on a thread that allows the UI to be updated. However,
 * because there can be many other messages in that queue from other sources
 * (e.g., UI interactions), and the handling of each message is synchronous
 * (e.g., if a message triggers a call to {@code Activity.onResume()} that
 * performs a database read from the main thread) there may be unavoidable
 * delays incurred when delivering "tick" notifications. The clients of this
 * clock should allow for such possibilities. For example, if the instant at
 * which a "tick" was expected to be notified has passed, it should not be
 * assumed that that "tick" was already notified or will not be notified, it
 * may have been delayed and will yet be notified.
 * </p>
 * <p>
 * Unfortunately, messages queue use {@code SystemClock.uptimeMillis()} to
 * schedule messages. That time source does not include time spent in "deep
 * sleep". Therefore, if, while a puzzle timer is running, the system goes
 * into deep sleep, the scheduled "ticks" will be delayed further when the
 * system reawakens. However, this is unlikely to cause problems in practice,
 * as the timer UI can ensure that the screen stays on and the device does
 * not sleep.
 * </p>
 *
 * @author damo
 */
class DefaultPuzzleClock implements PuzzleClock, Handler.Callback {
    /**
     * The handler used to queue messages and act when messages are processed.
     */
    private final Handler mHandler;

    /**
     * Creates a new puzzle clock.
     */
    DefaultPuzzleClock() {
        // At this time, "PuzzleTimer" creates the "DefaultPuzzleClock"
        // instance, so the "Looper" should be on the "PuzzleTimer" thread,
        // which is what is needed.
        mHandler = new Handler(this);
    }

    @Override
    public void tickAt(@NonNull OnTickListener listener, int tickID,
                       long futureTime) {
        // Unfortunately, while the puzzle timer needs to use "SystemClock
        // .elapsedRealtime()" and that is the time base for "futureTime",
        // "Handler" uses "uptimeMillis()", which may not be the same time
        // base. Calculating the delay and calling "Handler.sendMessageDelayed"
        // instead of "Handler.sendMessageAtTime" will avoid that problem.
        // However, the message will be delayed further if the system goes into
        // deep sleep, which is unavoidable without resorting to the Android
        // "AlarmManager" and requesting extra app permissions to use it.

        // Pass "-1, -1" for "arg1, arg2", so "handleMessage" knows this is
        // not a periodic tick.
        // NOTE: "Math.max(0, <delay>)" ensures that messages scheduled in
        // the past will be notified in the order in which they are requested.
        // This satisfies the requirements of the interface. If not clipped
        // to zero, they would be notified in chronological order.
        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(tickID, -1, -1, listener),
            Math.max(0, futureTime - now()));
    }

    @Override
    public void tickIn(@NonNull OnTickListener listener, int tickID,
                       long delay) {
        // Pass "-1, -1" for "arg1, arg2", so "handleMessage" knows this is
        // not a periodic tick.
        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(tickID, -1, -1, listener),
            Math.max(0, delay));
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation imposes a maximum value on {@code period}.
     * Although a {@code long} type, the value must not exceed
     * {@code Integer.MAX_VALUE} (2^31 - 1).
     * </p>
     *
     * @throws IllegalArgumentException
     *     {@inheritDoc}
     *     <p>
     *     If the maximum value of {@code period} permitted by this
     *     implementation is exceeded.
     *     </p>
     */
    @Override
    public void tickEvery(@NonNull OnTickListener listener, int tickID,
                          long period, long originTime)
            throws IllegalArgumentException {

        if (period <= 0 || period > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "Value of 'period' is out of range: " + period);
        }

        // ____________________________________________________________________
        //
        // Scheduling messages "in phase" is important. See the method
        // description on the interface for the reason why. The goal here is
        // to try to pack all the required information into the two "int"
        // values of "Message.arg1" and "Message.arg2". An "int" can hold
        // less than 25 days in milliseconds, so the "originTime" could
        // easily be too large to hold in "Message.arg2". However, only the
        // effect of the origin time on the "phase" of the ticks is important
        // and that can be distilled and fit into an "int", as "period" also
        // fits in an "int".
        //
        // Packing everything into the available fields of a "Message" has a
        // couple of benefits: for rapid-fire "ticks", such as when updating
        // the screen every 1/100th of a second, there is no need to
        // instantiate a separate "holder"/"helper" object for each
        // "Message", so there will be less GC activity; avoiding GC activity
        // would add complication to a "holder"/"helper" class, while
        // "Message" already has a pool ("obtain"/"recycle") feature built in.
        //
        // To understand this calculation, start by imagining that
        // "originTime" is zero. The first "tick" fires *now* and the
        // following ticks are scheduled to align with whole multiples of
        // "period". For example, say the period is 10, so the events fire
        // when the time is 0, 10, 20, 30, 40, 50, .... Say, "now" is 123
        // ("elapsedRealtime()" in ms). Send one message *now* and say it is
        // handled within 1 ms, at time 124, so the next message needs to
        // fire at time 130, the next whole multiple of 10 that is greater
        // than 124. Therefore the delay to the next message will be 6 ms, as
        // 124 + 6 = 130 (calculated in "handleMessage"), as that delay
        // ensures that the next message is scheduled for a time that is "in
        // phase" with the origin time (zero, for now) and is a whole
        // multiple of "period".
        //
        // If a message is received at time T, the delay to the next message
        // is given by:
        //
        //     delay = (T / period + 1) * period - T
        //
        // where "/" is integer division that truncates the result towards
        // negative infinity (i.e., a "floorDiv()").
        //
        // For example, if T=126 and period=10, then 126 / 10 = 12;
        // 12 + 1 = 13; 13 * 10 = 130; 130 - 126 = 4, so the delay is 4 ms.
        // If "T" is received exactly on time, say the next T is received at
        // 130, then the formula gives 130 / 10 = 13; 13 + 1 = 14;
        // 14 * 10 = 140; 140 - 130 = 10. So, it handles that edge case.
        //
        // "T" is given by "SystemClock.elapsedRealtime()", so it is always
        // positive. (The number of milliseconds since boot, including sleep
        // time, cannot be negative.) (*)
        //
        // Now, assume that "originTime" (call it "To") is not zero. If, say,
        // To=30 and period=10, then "ticks" must be scheduled to fire "in
        // phase" with a progression that increases by 10 ms and would
        // include To. However, this is no different than if To=0, as the
        // progression 0, 10, 20, 30, 40, 50, ..., is already "in phase" with
        // To=30. This is because To ("originTime") is an even multiple of
        // "period".
        //
        // Now, assume that To=33 and period=10. To be "in phase", the
        // required progression is 3, 13, 23, 33, 43, 53, ..., i.e., it
        // increases by 10 (the period) and includes 33 (the "originTime", To).
        // The thing to note is that each target time in this sequence is
        // offset from the progression for To=0 by +3. The reason is that
        // "33 mod 10 = 3" (i.e., the origin time modulo the period is +3).
        // Call this the "phaseOffset" (denote it "Po"). It is the offset from
        // each value in the trivial progression when To=0 to each value in
        // the actual progression when To=33. It is given as:
        // "Po = To mod period".
        //
        // There is no requirement imposed that "originTime" be positive. It
        // could be negative, too. Say To=-3, then the progression is -3, 7,
        // 17, 27, 37, 47, 57, .... However, in Java, the "%" operator is a
        // "remainder" operator, rather than a "modulus" operator. A
        // mathematical modulus operation gives "-3 mod 10 = 7", but Java's
        // remainder gives "-3 % 10 = -3" (this has to do with whether the
        // truncated division rounds towards zero or towards negative
        // infinity). To get the right result, define a "floorMod(a, b)"
        // method (see that method for details).
        //
        // The new and improved formula for the delay that schedules the next
        // message "in phase" with To, where To may be any integer value is:
        //
        //        Po = floorMod(To, period)
        //     delay = ((T - Po) / period + 1) * period - (T - Po)
        //
        // where "/" is a "floorDiv()".
        //
        // Note that Po (the "phaseOffset") is calculated from To (the
        // "originTime") and period and that the delay formula does not need
        // To after that. By definition, Po ("phaseOffset") is less than
        // "period", so if "period" can fit in a 32-bit "int", so can
        // "phaseOffset". This means that all the information required to
        // calculate the delay is the "phaseOffset", the "period" and the
        // current time ("T"). The former two values will always be positive
        // and will fit in "Message.arg1" and "Message.arg2", both "int"
        // values. The current time can be retrieved from "SystemClock
        // .elapsedRealtime()" when handling that message and calculating the
        // delay required to schedule the next message "in phase".
        //
        // (*) The "elapsedRealtime" is always positive unless the system is
        // running for a long, long time since booting. Assuming that the
        // uptime is counted in nanoseconds and that the milliseconds since
        // boot are actually the nanoseconds since boot divided by 10^6, then
        // a 64-bit signed "long" will not overflow (and turn negative) until
        // 2^63 - 1 nanoseconds after booting, or about 292 years. If it
        // actually stores milliseconds internally, then it be 292 MILLION
        // years before it overflows and turns negative.
        // ____________________________________________________________________

        // The first tick is immediate (zero delay). "handleMessage" with
        // then chain the ticks one after another until they are cancelled.
        // AFAIK, a message cannot be fired after all message for that tickID
        // have been removed from the queue, as the "removeMessage" is
        // synchronous and everything runs on one thread. Therefore, the way
        // that the next tick message is posted as each tick message is
        // handled should not cause a problem and there is no need to record
        // what has and has not been cancelled in the past.
        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(tickID, (int) period,
                (int) floorMod(originTime, period), // The "phaseOffset".
                listener),
            0); // No delay. Fire this first "tick" as soon as possible.
    }

    @Override
    public void cancelTick(@NonNull OnTickListener listener, int tickID) {
        // NOTE: There is only one "MessageQueue" associated with one
        // "Looper" for any one thread. However, when removing messages using
        // a "what", only the messages queued by a "Handler" (the "target" of
        // those messages) can be removed via that "Handler" . There is no
        // need to worry about clashing "what" values, etc. Here, there is
        // also a further refinement of the "selection" of messages to
        // remove: "Message.obj" must also be the same "listener" object.
        mHandler.removeMessages(tickID, listener);
    }

    @Override
    public void cancelAllTicks(@NonNull OnTickListener listener) {
        // Only deletes messages where "Message.obj" is the same "listener"
        // object.
        mHandler.removeCallbacksAndMessages(listener);
    }

    @Override
    public long now() {
        // NOTE: This is not "wall time", it is the "uptime" including real time
        // elapsed while the system was in deep sleep, which is important. The
        // value from "SystemClock.uptimeMillis()" does not include sleep time.
        return SystemClock.elapsedRealtime();
    }

    @Override
    public long nowRealTime() {
        return System.currentTimeMillis();
    }

    /**
     * Handles a message by notifying the listener of a "tick" event. If
     * {@link #tickEvery(OnTickListener, int, long, long)} created the message,
     * a new message is scheduled for the next periodic tick.
     *
     * @param msg
     *     The message to be handled. {@code Message.obj} must be the
     *     {@code OnTickListener} and must not be {@code null}. For a one-off
     *     "tick" event, {@code Message.arg1} and {@code Message.arg2} must
     *     be -1. For a periodic "tick" event, {@code Message.arg1} must be
     *     the period and {@code Message.arg2} must be the "phase offset".
     *
     * @return
     *     {@code true}, always, to indicate that the message has been handled.
     */
    @Override
    public boolean handleMessage(Message msg) {
        final OnTickListener listener
            = (OnTickListener) msg.obj; // Assume not null.
        final int tickID = msg.what;

        listener.onTick(tickID);

        if (msg.arg1 != -1) {
            // This is a periodic tick set up by "tickEvery()". See the
            // comment there for details on this calculation of the "delay"
            // required for the next tick to be "in phase".
            final int period = msg.arg1;
            final int phaseOffset = msg.arg2;

            // See comment in "tickAt()" for reason why "sendMessageDelayed"
            // must be used.
            mHandler.sendMessageDelayed(
                mHandler.obtainMessage(tickID, period, phaseOffset, listener),
                getDelayToNextTick(now(), period, phaseOffset));
        } // else this was a "one-shot" tick from "tickIn()" or "tickAt()".

        // Handling complete. ("Looper" will recycle the "Message".)
        return true;
    }

    /**
     * <p>
     * Gets the floor modulus of two values. This differs from the behaviour
     * of the Java remainder or modulo operator "{@code %}" when the values
     * have opposite signs.
     * </p>
     * <p>
     * In Java, {@code a % b} yields the same result as
     * {@code a - b * (a / b)}, for integer values {@code a} and {@code b},
     * where the division operation truncates the result towards zero. Where
     * the signs are different, the result would be, for example,
     * {@code -3 % 10 == -3}, as in {@code -3 - 10 * (-3 / 10)} the integer
     * division term {@code -3 / 10} ({@code -0.3} in decimal form) is
     * truncated to zero. The floor modulus differs from the Java operator by
     * truncating that division term towards negative infinity, so
     * {@code floorDiv(-3, 10) == -1} (i.e., {@code -0.3} is rounded
     * <i>down</i> to {@code -1}) and, therefore, when that division is applied
     * for the modulus calculation {@code floorMod(-3, 10) == 7}.
     * </p>
     * <p>
     * <i>NOTE: This method has the limitation that the divisor must be
     * positive or the result is not defined. This should not be a problem
     * for its intended <i>ad hod</i> use in this {@code DefaultPuzzleClock}
     * class. However, if a similar operation is required in other contexts,
     * this implementation may not be suitable for re-use.</i>
     * </p>
     *
     * @param a
     *     The first value (the dividend). May be negative.
     * @param b
     *     The second value (the divisor). Must be positive. (Not checked.)
     *
     * @return
     *     The floor modulus of the two values, or an undefined result if the
     *     divisor is not a positive value.
     */
    // NOTE: Default "package" access only to allow unit testing.
    @VisibleForTesting
    static long floorMod(long a, long b) {
        // Note: Java 8 introduced "Math.floorMod" and "Math.floorDiv", but
        // those methods are not available if supporting Android API 16+.

        // The "positive divisor" limitation makes the calculation amenable
        // to a simple trick.
        return ((a % b) + b) % b;
    }

    /**
     * Divides one integer by another integer and returns the result
     * truncated towards negative infinity. This differs from the normal Java
     * integer division operator which truncates the result towards zero. The
     * distinction only matters where the quotient (before truncation) is
     * negative, i.e., if the signs of {@code a} and {@code b} are different.
     * For example, the Java expression {@code 10 / -4} yields -2, as the
     * unrounded result is -2.5, which is truncated towards zero; but
     * {@code floorDiv(10, -4)} yields -3, as -2.5 is truncated towards
     * negative infinity.
     *
     * @param a
     *     The first value (the dividend).
     * @param b
     *     The second value (the divisor). Must be non-zero. (Not checked.)
     *
     * @return The quotient of the floor division of the two values.
     */
    // NOTE: Default "package" access only to allow unit testing.
    @VisibleForTesting
    static long floorDiv(long a, long b) {
        // Do the truncated division, then, if the unrounded result is
        // negative (i.e., if the signs of "a" and "b" are different) and the
        // unrounded result is not an integer (i.e., there is a remainder
        // from the division), subtract 1 to truncate towards negative infinity.
        return a / b - (a % b != 0 && a < 0 != b < 0 ? 1 : 0);
    }

    /**
     * Calculates the delay required to schedule the next "tick" in phase
     * with the origin time. The origin time's phase is defined by the
     * {@code phaseOffset}.
     *
     * @param now
     *     The time now, when a periodic tick has been received.
     * @param period
     *     The period (in milliseconds) for the repeated tick events. Must be
     *     positive. Note that some implementations may place restrictions on
     *     the maximum value of the period.
     * @param phaseOffset
     *     The phase offset (in milliseconds) to use to schedule the next
     *     tick to be "in phase" with the origin time for these ticks.
     *
     * @return
     *     The delay (in milliseconds) before the next periodic tick should
     *     be notified.
     */
    // NOTE: Default "package" access only to allow unit testing.
    @VisibleForTesting
    static long getDelayToNextTick(long now, long period, long phaseOffset) {
        // See comment in "tickEvery" for details about this calculation.
        final long t = now - phaseOffset;

        return (floorDiv(t, period) + 1) * period - t;
    }
}
