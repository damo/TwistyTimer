package com.aricneto.twistytimer.spans;

import com.aricneto.twistytimer.utils.TimeUtils;
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
        // Round the value to whole milliseconds to avoid precision errors.
        // For example, 24.29999999 s becomes 24,300 ms.
        return TimeUtils.formatResultTimeLoRes(Math.round(value * 1_000.0));
    }
}
