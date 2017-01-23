package com.aricneto.twistytimer.timer;

import android.support.annotation.NonNull;

/**
 * <p>
 * An external clock (or "time source"), that drives the operation of a
 * {@link PuzzleTimer} (though though the {@link OnTickListener} abstraction).
 * The puzzle timer requests that the clock notify it of "tick events", so
 * that it can track the passing of time. The clock calls back to
 * {@link OnTickListener#onTick(int)} to report each tick event back to the
 * listening timer. The clock assigns an ID to each tick event, so that the
 * puzzle timer can cancel any tick event before it happens (if it is not
 * already too late).
 * </p>
 * <p>
 * A clock provides a monotonically increasing "current time" value with
 * millisecond resolution. All "tick" periods and delays are also specified
 * in milliseconds. The current time does not necessarily start at zero; the
 * timer should record the current time at the start and at the end of its
 * timing phase and use the difference to calculate the elapsed time.
 * Periodic "tick events" are not guaranteed to be notified exactly in phase
 * with the expected period and they may be subject to cumulative drift.
 * However the "current time" will not be subject to any induced cumulative
 * drift, though it may be subject to small inaccuracies due to system load
 * that cause delays in the notification of the event from the instant that
 * the current time is recorded.
 * </p>
 *
 * @author damo
 */
// "package" scope for use by "PuzzleClock" and for unit tests; should not be
// used elsewhere.
interface PuzzleClock {
    /**
     * A listener for "tick events" that have been scheduled via the methods
     * of the {@code PuzzleClock}.
     */
    interface OnTickListener {
        /**
         * Notifies the listener of a "tick event" that was previously
         * scheduled.
         *
         * @param tickID
         *     The ID of the tick event. This is the ID that was assigned to
         *     the tick event when it was scheduled.
         */
        void onTick(int tickID);
    }

    /**
     * <p>
     * Request a single tick event at a specific instant of time in the
     * future. If the "future time" has already passed before this request is
     * received, the tick event will be scheduled to occur immediately,
     * though not until after this method has returned. The tick event may be
     * cancelled by passing the same ID to
     * {@link #cancelTick(OnTickListener, int)} before the scheduled event
     * time has elapsed.
     * </p>
     * <p>
     * If the {@code futureTime} has already passed (i.e., is negative) when
     * the ticks are scheduled, they will be notified in the order in which
     * they were requested from this method, not in chronological order of
     * their negative times. For example, tick B is scheduled to occur in the
     * past at time -100 and tick A is scheduled to occur at a time further
     * in the past at time -200. As both times are in the past, they are
     * scheduled to occur immediately and the order will be B-then-A, as that
     * was the order in which they were requested. The chronological order
     * A-then-B is not used when both times are in the past.
     * </p>
     * <p>
     * This request-ordering of ticks at past times makes it easy to cancel
     * "expired" ticks. If ticks are requested in reverse chronological, then
     * they caller need not check if ticks have expired before making the
     * request. If some ticks have expired, then the one that expired most
     * recently will be notified first. On handling that tick, the earlier
     * "expired" ticks can then be cancelled. If there is no desire to cancel
     * ticks this way, then simply request the ticks in chronological order
     * and they will be notified in the same order.
     * </p>
     *
     * @param listener
     *     The listener to be notified of the tick event.
     * @param tickID
     *     The ID to assign to the scheduled tick event, allowing the event
     *     to be identified to the listener or cancelled.
     * @param futureTime
     *     The future time at which to send the tick event. The time source
     *     used to identify this instant will be the same as that used for
     *     the "current time".
     */
    // NOTE: A request for a tick at a future instant in time may be made
    // when the current instant in time is very close to that future time,
    // say, within 1 ms. It is very easy to imagine that the time source
    // (e.g., system clock) may itself "tick" while the request is being made.
    // There may be more than one such "tick" mid-request if the method call
    // is preempted by a GC pass, or other system load. It is also possible
    // for the system clock's "ticks" to occur at intervals greater than 1
    // ms, perhaps at 100 Hz (every 10 ms). Therefore, tight scheduling could
    // legitimately result in "futureTime" being in the past, so leniency is
    // necessary.
    //
    // Negative times are not excluded, as the time source for the "current
    // time" is not dictated and could report a negative value. The time
    // source is more likely to be an "uptime" source rather than the system
    // realtime clock, as that will be less likely to change erratically.
    void tickAt(@NonNull OnTickListener listener, int tickID, long futureTime);

    /**
     * Request a single tick event to be sent after a delay. If the delay is
     * zero or negative, the tick event will be scheduled to occur immediately,
     * though not until after this method has returned. The tick event may be
     * cancelled by passing the same ID to
     * {@link #cancelTick(OnTickListener, int)} before the time delay has
     * elapsed.
     *
     * @param listener
     *     The listener to be notified of the tick event.
     * @param tickID
     *     The ID to assign to the scheduled tick event, allowing the event
     *     to be identified to the listener or cancelled.
     * @param delay
     *     The delay (in milliseconds) from the time this request is received
     *     to the time that the tick event should be notified to the listener.
     *     All zero or negative delays are taken to mean "immediately".
     */
    // NOTE: A negative delay may be the result of the caller calculating
    // that a tick event is required at a time that has already passed. The
    // caller could just correct the negative value to a zero, or could even
    // just call its own "onTick" method directly, but it is more convenient
    // if this method accepts negative delays and just assumes "immediately",
    // as the caller then does not need to code nearly as much logic.
    void tickIn(@NonNull OnTickListener listener, int tickID, long delay);

    /**
     * <p>
     * Request tick events to be notified periodically. The first tick event
     * will be scheduled to occur immediately, though not until after this
     * method has returned. Thereafter, the tick events will be notified at
     * intervals specified by the {@code period} that are scheduled at
     * instants in time that are in phase with the {@code originTime}. Ticks
     * continue to be notified until cancelled by passing the same ID to
     * {@link #cancelTick(OnTickListener, int)}, or until
     * {@link #cancelAllTicks(OnTickListener)} is called.
     * </p>
     * <p>
     * The {@code originTime} allows notifications to be synchronised to the
     * required refresh phase. For example, say the tick events are used to
     * trigger periodic refreshes of the display of a countdown timer. The
     * countdown timer must "tick" every whole second (1,000 ms). The timer
     * started when {@link #now()} reported 234 ms, but the current time is
     * now 384 ms. Therefore, to ensure that the display is refreshed at the
     * right instants, the first tick is fired immediately and the second tick
     * is scheduled to fire when the current time is 1,234, exactly 1,000 ms
     * after the time when the countdown started, though only 850 ms after the
     * first tick. The next tick will be scheduled to fire at 2,234 ms, and so
     * on. This instant in time (234 ms) is given as the {@code originTime}.
     * It ensures that, after the first refresh that is necessary to initialise
     * the display as soon as possible, the later ticks are in phase with the
     * required 1-second period of the countdown timer: each tick event is
     * notified is set as close as possible to the instant when the countdown
     * display should "tick" down by one second.
     * </p>
     * <p>
     * The origin time provides the reference instant in time required to
     * ensure the tick events are scheduled in phase with that instant. The
     * tick events will be notified at future instants in time that
     * correspond to the origin time plus or minus some number of whole
     * multiples of the period. Therefore, the {@code originTime} may be in
     * the past or in the future, even by much more than the duration of the
     * {@code period}, without affecting the phase synchronisation.
     * </p>
     * <p>
     * On each "tick", the next tick is scheduled for a future instant in time
     * (relative to the instant that the current tick is handled) that must be
     * in phase with the origin time. A best effort is made to schedule
     * notification of the tick events in phase with the origin time. However,
     * if there are system delays (or other activity on the main UI thread) that
     * cause ticks to be notified late, some tick events may be skipped. For
     * example, if the origin time is 234 ms and the period is 1,000 ms, then
     * the first tick will be notified immediately and the second tick at
     * 1,234 ms (instant). However, if the second tick is delayed until, say,
     * 2,284 ms, then it will be notified at that instant, but a third tick that
     * would have been scheduled for 2,234 ms will be skipped, as that time has
     * already passed. The next tick will then be notified 950 ms later.
     * Therefore, ticks can be skipped, or notified at intervals greater than
     * or less than the given {@code period}. However, a best effort is made to
     * ensure that, skipped ticks aside, the mean interval between ticks is
     * close to the {@code period} and that the ticks are notified at instances
     * in time that are "in phase" with the origin time.
     * </p>
     *
     * @param listener
     *     The listener to be notified of each tick event.
     * @param tickID
     *     The ID to assign to the scheduled tick events, allowing the events
     *     to be identified to the listener or cancelled by
     *     {@link #cancelTick(OnTickListener, int)}.
     * @param period
     *     The period (in milliseconds) for the repeated tick events. Must be
     *     positive. Note that some implementations may place restrictions on
     *     the maximum value of the period.
     * @param originTime
     *     The instant in time (originally reported by {@code now()}, typically)
     *     that provides the phase reference for the scheduling of the periodic
     *     tick events. The value may be negative.
     *
     * @throws IllegalArgumentException
     *     If the period is not positive (greater than zero), or if the
     *     period exceeds an implementation-dependent maximum value.
     */
    void tickEvery(@NonNull OnTickListener listener, int tickID, long period,
                   long originTime) throws IllegalArgumentException;

    /**
     * Cancels the notification of tick events scheduled for a tick listener.
     * Tick events scheduled by other listeners will not be affected, even if
     * they used the same tick IDs. See the descriptions of the "tick"
     * methods for the effect of cancelling each type of tick event.
     *
     * @param listener
     *     The listener set to be be notified of each tick event when the
     *     ticks were scheduled.
     * @param tickID
     *     The ID of the scheduled tick event(s) to be cancelled. The ID is
     *     given by the client when tick events are scheduled. All scheduled
     *     tick events that match the given ID (for the same listener) will
     *     be cancelled; all those not matching the ID will be unaffected.
     */
    void cancelTick(@NonNull OnTickListener listener, int tickID);

    /**
     * Cancels the notification of <i>all</i> scheduled tick events for all
     * IDs that were submitted for the given tick listener. Tick events
     * scheduled by other listeners will not be affected.
     *
     * @param listener
     *     The listener set to be be notified of each tick event when the
     *     ticks were scheduled.
     */
    void cancelAllTicks(@NonNull OnTickListener listener);

    /**
     * <p>
     * Gets the current time value. The timer may request the current time in
     * preference to the current time value notified in the {@code onTick}
     * events, as it may want to avoid inaccuracies caused by delays in the
     * notification of the event. The timer may also request the current time
     * from the clock when it receives events from other sources, such as
     * from the user-interface, that may not include any indication of the
     * current time.
     * </p>
     * <p>
     * Typically, this should <i>not</i> be the system real time clock, as that
     * may result in inaccurate timing due to intermittent NTP synchronisation
     * events or other changes to the system clock. The system "uptime" is more
     * appropriate, as it is not subject to such unpredictable changes. Where
     * possible, the source should include the time elapsed while the device is
     * in sleep mode, e.g., use {@code android.os.SystemClock.elapsedRealtime()}
     * rather than {@code android.os.SystemClock.uptimeMillis()}, as the latter
     * does not include time elapsed while the system was in deep sleep.
     * </p>
     *
     * @return The monotonically increasing current time value in milliseconds.
     */
    long now();

    /**
     * Gets the current real time ("wall time") value. This is required for
     * some operations, such as adding a date-time stamp to completed solve
     * attempts and managing long-term persistence of data that may span a
     * reboot of the device and the resetting of the system "uptime" that is
     * provided by {@link #now()}. {@code System.currentTimeMillis()} is an
     * appropriate source for this value.
     *
     * @return The current real time value in milliseconds since the Unix epoch.
     */
    long nowRealTime();
}
