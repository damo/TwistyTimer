package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.utils.ThemeUtils;

/**
 * A customisation of the normal toggle switch to support enabling or disabling the history of all
 * solve times.
 *
 * @author damo
 */
public class HistorySwitch extends SwitchCompat {
    // NOTE: This class was created to remove some "bulk" from "TimerMainFragment".

    /**
     * The thumb drawable used when the switch is "on".
     */
    private Drawable mThumbDrawableOn;

    /**
     * The thumb drawable used when the switch is "off".
     */
    private Drawable mThumbDrawableOff;

    /**
     * The track drawable used when the switch is "on".
     */
    private Drawable mTrackDrawableOn;

    /**
     * The track drawable used when the switch is "off".
     */
    private Drawable mTrackDrawableOff;

    /**
     * Creates a new switch with the default styling.
     *
     * @param context The context for the new switch.
     */
    public HistorySwitch(Context context) {
        // Not using "this(context, null)", as the next constructor would then be expected to use
        // "this(context, attrs, ???)". The super class has a specific default style for "???", and
        // it is not desired to reference it from this class. It may not even be public.
        super(context);
        init();
    }

    /**
     * Creates a new switch with the default styling, but overriding the given style attributes.
     *
     * @param context The context for the new switch.
     * @param attrs   The attributes that differ from the default styles.
     */
    public HistorySwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     *
     * @param context The context for the new switch.
     * @param attrs        Specification of attributes that should deviate from the default styling.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     */
    public HistorySwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Can these be moved out to a state list drawable XML resource? Also, the "positive"
        // and "negative" states are confusingly the reverse of what would be expected: "positive"
        // is used when the switch is off and "negative" when the switch is on. Huh?
        mThumbDrawableOff = ThemeUtils.tintPositiveThumb(getContext(),
                R.drawable.thumb_history_positive, R.attr.colorPrimaryDark);
        mThumbDrawableOn = ThemeUtils.tintNegativeThumb(getContext(),
                R.drawable.thumb_history_negative, R.attr.colorPrimaryDark);
        mTrackDrawableOff = ThemeUtils.tintDrawable(getContext(),
                R.drawable.track_positive, R.attr.colorPrimaryDark);
        mTrackDrawableOn = getResources().getDrawable(R.drawable.track_negative);
    }

    /**
     * Sets the checked state of the switch and applies the custom drawables, as appropriate.
     *
     * @param checked {@code true} if the switch is "on"; or {@code false} if it is "off".
     */
    @Override
    public void setChecked(boolean checked) {
        setThumbDrawable(checked ? mThumbDrawableOn : mThumbDrawableOff);
        setTrackDrawable(checked ? mTrackDrawableOn : mTrackDrawableOff);

        super.setChecked(checked);
    }
}
