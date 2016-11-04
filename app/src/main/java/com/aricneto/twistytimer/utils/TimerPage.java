package com.aricneto.twistytimer.utils;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.fragment.BaseMainFragment;
import com.aricneto.twistytimer.fragment.TimerFragment;
import com.aricneto.twistytimer.fragment.TimerGraphFragment;
import com.aricneto.twistytimer.fragment.TimerListFragment;

import java.lang.reflect.Method;

/**
 * An enumeration of the different "pages" displayed in the tabs presented in the main activity.
 * These values include extra details for each page, such as the icon to be used. The order of the
 * values in this {@code enum} is the order in which the pages should be displayed (left-to-right,
 * each in its own tab).
 *
 * @author damo
 */
public enum TimerPage {
    /**
     * The page showing the puzzle timer.
     */
    TIMER(TimerFragment.class, R.drawable.ic_timer_white_24dp),

    /**
     * The page showing a list of solve times.
     */
    TIMES_LIST(TimerListFragment.class, R.drawable.ic_format_list_bulleted_white_24dp),

    /**
     * The page showing a chart of solve times and a table of solve-time statistics.
     */
    CHART_AND_STATS(TimerGraphFragment.class, R.drawable.ic_timeline_white_24dp);

    /**
     * All of the values in this {@code enum}. This is used internally for more efficient conversion
     * of a tab position (zero-based) to a {@code TimerPage} value.
     */
    private static final TimerPage[] VALUES = TimerPage.values();

    /**
     * The resource ID of the drawable used for the tab icon.
     */
    @DrawableRes
    private final int mIconResID;

    /**
     * The fragment class used to represent this timer page.
     */
    @NonNull
    private final Class<? extends BaseMainFragment> mFragmentClass;

    /**
     * Creates a new timer page value.
     *
     * @param fragmentClass
     *     The fragment class used to represent this timer page. The class must implement a public,
     *     static, no-argument {@code newInstance} method.
     * @param iconResID
     *     The resource ID of a drawable to use as the icon representing this page.
     */
    TimerPage(@NonNull Class<? extends BaseMainFragment> fragmentClass,
              @DrawableRes int iconResID) {
        mFragmentClass = fragmentClass;
        mIconResID = iconResID;
    }

    /**
     * Gets the drawable resource ID for the icon that represents this page.
     *
     * @return The icon resource ID.
     */
    @DrawableRes
    public int getIconResID() {
        return mIconResID;
    }

    /**
     * Creates a new instance of the fragment used to present the user interface for this timer
     * page.
     *
     * @return A new fragment for this page.
     */
    @NonNull
    public BaseMainFragment createPageFragment() {
        try {
            final Method newInstanceMethod
                    = mFragmentClass.getMethod("newInstance", (Class[]) null);

            //noinspection unchecked
            return (BaseMainFragment) newInstanceMethod.invoke(null, (Object[]) null);
        } catch (Exception e) {
            throw new RuntimeException("Fragment for TimerPage is not defined properly.", e);
        }
    }

    /**
     * Gets the timer page that corresponds to the given tab position. An error will occur if the
     * tab position is outside of the valid range.
     *
     * @param tabPosition The zero-based tab position.
     *
     * @return The timer page corresponding to the tab position.
     */
    public static TimerPage forTabPosition(int tabPosition) {
        return VALUES[tabPosition];
    }

    /**
     * Gets the number of pages defined by this {@code enum}.
     *
     * @return The number of pages.
     */
    public static int getNumPages() {
        return VALUES.length;
    }
}
