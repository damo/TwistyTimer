package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A view pager with the option to disable swiping to change pages.
 */
public class LockedViewPager extends ViewPager {
    private boolean isPagingEnabled = true;

    public LockedViewPager(Context context) {
        super(context);
    }

    public LockedViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return isPagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return isPagingEnabled && super.onInterceptTouchEvent(event);
    }

    public void setPagingEnabled(boolean enabled) {
        isPagingEnabled = enabled;
    }
}
