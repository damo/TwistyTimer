package com.aricneto.twistytimer.spans;

import com.aricneto.twistytimer.utils.TimeUtils;
import com.aricneto.twistytimer.utils.WCAMath;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

/**
 * Formatter for time values shown on the Y-axis of the solve times chart.
 */
public class TimeFormatter implements YAxisValueFormatter{

    public TimeFormatter() {
    }

    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        // Truncate the fractional time to whole milliseconds, then apply the
        // WCA rounding.
        return TimeUtils.formatTimeLoRes(
            WCAMath.roundResult((long) (value * 1_000L)));
    }
}
