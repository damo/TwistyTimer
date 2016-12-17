package com.aricneto.twistytimer.timer;

/**
 * An interface for components requiring regular refresh notifications while
 * a {@link PuzzleTimer} is running. Refresh notification are separate from
 * the UI cues and life-cycle events notified to a
 * {@link OnTimerEventListener}, as they are likely only to be relevant to a
 * widget displaying the current timer value, rather than to a higher-level
 * "controller" component that manages the wider user interface (e.g., a
 * fragment, or activity). This separation allows the "controller" to connect
 * the display widget directly to the {@link PuzzleTimer} without the need to
 * "relay" otherwise irrelevant refresh events from one to the other.
 *
 * @author damo
 */
// Only used for now by "TimerView", which is in the same package, but "public"
// access allows for other view implementations in other packages if needed.
@SuppressWarnings("WeakerAccess")
public interface OnTimerRefreshListener {
    /**
     * <p>
     * Notifies the listener that the display of the elapsed solve time
     * should be refreshed to display the up-to-date value.
     * </p>
     * <p>
     * The target of this refresh notification can feed back its own
     * preferred refresh period, setting it to a rate that is appropriate for
     * the displayed precision of the time and the state of the
     * user-interface. The returned value sets the new timer refresh period,
     * the interval (in milliseconds) between notifications of the current
     * time. A value of -1 can be returned to indicate that the refresh
     * period should be reverted to its default value, while a value of zero
     * can be returned to indicate that the refresh period should remain
     * unchanged.
     * </p>
     * <p>
     * Refresh notifications are scheduled for notification "in phase" with
     * the current time value. For example, if the refresh period is set to
     * 1,000 ms, then notifications will be set every second <i>on the whole
     * second</i> from the instant that the timer was started. The refresh
     * notifications will not be sent "early". For example, if the refresh
     * period is 1,000, then the notification scheduled "in phase" with the
     * instant that three seconds have elapsed will be sent no earlier than
     * 3,000 ms after the timer started; it may arrive "late" (e.g., at
     * 3,042 ms), but never "early" (e.g., at 2,999 ms). This ensures that if
     * the time is <i>truncated</i> to a whole second, it will truncate to the
     * expected whole second value, not to the previous value.
     * </p>
     * <p>
     * Consideration should be given to choosing a refresh period that
     * ensures the timer display is updated in an appropriate and efficient
     * manner. If the refresh period is shorter than the resolution of the
     * time being displayed, e.g., a period of 10 ms when the display is
     * showing whole seconds, then effort will be wasted. If the refresh
     * period is too long, then the display may appear to have staled, e.g.,
     * a period of 500 ms when the display is showing 1/10th or 1/100ths of a
     * second. The phase of the refresh notifications should also be
     * considered. For example, if the display is showing whole seconds and
     * truncates the current time to a whole second before displaying it,
     * then a refresh period of, say, 900 ms would not be appropriate. If 750
     * ms were used, the notifications would be sent at 0, 900, 1,800, 2,700,
     * 3,600 ms from the start of the timer. Truncation to a whole second
     * would result in the displaying showing "0" at the start, not changing
     * to "1" until 1.8 s, not changing to "2" until 2.7 s, and so on.
     * Because the timer schedules refresh notifications "in phase" with the
     * current time, there is probably no need for the period to be any
     * shorter than the "ticking" period of the display. However, if the
     * "ticking" is very fast, i.e., is displaying the time with high
     * precision, the refresh period may be set longer than the "ticking"
     * period, as it may not be necessary to refresh the display at full
     * precision to preserve the illusion that it is running at "full speed".
     * </p>
     * <p>
     * If the refresh period that is returned is the same as the current
     * refresh period (including if zero is returned to leave it unchanged,
     * or if -1 is returned and the refresh period is already set to the
     * default), then the refresh notifications will continue on the same
     * schedule as before. However, if the refresh period is changed, then
     * the notifications will be re-scheduled and the first notification on
     * the new schedule will fire <i>immediately</i> (and potentially out of
     * phase with the time) before the subsequent notifications begin to
     * arrive in phase with the time. Constantly changing the refresh period
     * could result in a "storm" of refresh notifications, each one causing
     * the next one to fire immediately after it. Therefore, try to avoid
     * changing the refresh period on every call by making only occasional,
     * discrete changes at key stages. For example, changing the refresh once
     * when the time exceeds 10 minutes and the displayed precision is
     * switched discretely from 1/100ths of a second to whole seconds. To aid
     * in identifying the need to change the refresh period, and to determine
     * if it was already changed by a previous notification, the current
     * refresh period is given in the call-back.
     * </p>
     *
     * @param elapsedTime
     *     The current elapsed solve time (in milliseconds).
     * @param refreshPeriod
     *     The current refresh period (in milliseconds). This may be useful
     *     if the listener needs to decide if a change to the refresh period
     *     needs to be made, or if the refresh period is already suitable.
     *
     * @return
     *     The new refresh period (in milliseconds). Use {@code -1} to
     *     restore the default refresh period, or {@code 0} to leave the
     *     current refresh period unchanged. Otherwise the value must be
     *     positive and may be limited automatically to maximum and minimum
     *     bounds.
     */
    long onTimerRefreshSolveTime(long elapsedTime, long refreshPeriod);

    /**
     * Notifies the listener that the display of the remaining inspection
     * time should be refreshed to display the up-to-date value. See
     * {@link #onTimerRefreshSolveTime(long, long)} for more details on the
     * feed back of new refresh periods to the timer.
     *
     * @param remainingTime
     *     The remaining inspection time (in milliseconds). This value will
     *     be negative if the inspection period has been overrun and is
     *     within two seconds of timing out. The listener might use that as a
     *     cue to change the precision of the displayed time and to change
     *     the refresh period to something suitable for that precision.
     * @param refreshPeriod
     *     The current refresh period (in milliseconds). This may be useful
     *     if the listener needs to decide if a change to the refresh period
     *     needs to be made, or if the refresh period is already suitable.
     *
     * @return
     *     The new refresh period (in milliseconds). Use -1 to restore the
     *     default refresh period or zero to leave the current refresh period
     *     unchanged. Otherwise the value must be positive and may be limited
     *     automatically to maximum and minimum bounds.
     */
    long onTimerRefreshInspectionTime(long remainingTime, long refreshPeriod);
}
