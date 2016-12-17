package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.aricneto.twistytimer.utils.TimerPage;

/**
 * Wrapper class for {@code TabLayout} that intercepts {@code addTab} calls
 * and adds an icon to the tab. This class also highlights the icon on
 * selected tabs and dims the icon on unselected tabs. The responsiveness of
 * tabs to clicks that switch to the clicked tab can also be disabled and
 * enabled (see {@link #setEnabled(boolean)}). This may be useful during
 * animated transitions, when switching away the tab selected at the start of
 * the transition might cause problems.
 */
// NOTE: Android Support Library 23.2.0 changed the behaviour when setting
// tabs from a view pager. If the view pager changes, the tabs are removed
// and re-created, so this is the only way to keep adding back the icons
// short of dispensing with "ViewPager" and using something else. See
// https://code.google.com/p/android/issues/detail?id=202402 for more.
public class TimerTabLayout extends TabLayout
             implements TabLayout.OnTabSelectedListener {
    /**
     * The alpha transparency applied to a tab's icon when the tab is selected.
     */
    private static final int SELECTED_ALPHA = 255;

    /**
     * The alpha transparency applied to a tab's icon when the tab is not
     * selected.
     */
    private static final int UNSELECTED_ALPHA = 153;

    public TimerTabLayout(Context context) {
        super(context);
        init();
    }

    public TimerTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimerTabLayout(Context context, AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initialises this tab layout to listen for selection events.
     */
    private void init() {
        addOnTabSelectedListener(this);
    }

    @Override
    public void addTab(@NonNull Tab tab, int position, boolean setSelected) {
        tab.setIcon(TimerPage.forTabPosition(position).getIconResID());
        // Just set the icon, so it cannot be null!
        //noinspection ConstantConditions
        tab.getIcon().setAlpha(setSelected ? SELECTED_ALPHA : UNSELECTED_ALPHA);

        super.addTab(tab, position, setSelected);
    }

    /**
     * Called when a tab enters the selected state. The icon for the selected
     * tab will be highlighted.
     *
     * @param tab The tab that was selected
     */
    @Override
    public void onTabSelected(Tab tab) {
        if (tab != null && tab.getIcon() != null) {
            tab.getIcon().setAlpha(SELECTED_ALPHA);
        }
    }

    /**
     * Called when a tab exits the selected state. The icon for the unselected
     * tab will be dimmed.
     *
     * @param tab The tab that was unselected
     */
    @Override
    public void onTabUnselected(Tab tab) {
        if (tab != null && tab.getIcon() != null) {
            tab.getIcon().setAlpha(UNSELECTED_ALPHA);
        }
    }

    /**
     * Called when a tab that is already selected is chosen again by the user.
     *
     * @param tab The tab that was reselected.
     */
    @Override
    public void onTabReselected(Tab tab) {
        // Do nothing.
    }

    /**
     * Sets the enabled state of this tab layout. This tab layout is enabled by
     * default. If disabled, tabs can still be switched programmatically.
     *
     * @param enabled
     *     {@code true} to respond to a click on a tab by switching to that
     *     tab; or {@code false} to ignore clicks on tabs and stay on the
     *     currently-selected tab.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        final ViewGroup tabStrip = (ViewGroup) getChildAt(0);

        if (tabStrip != null) {
            tabStrip.setEnabled(enabled);

            for (int i = 0; i < tabStrip.getChildCount(); i++) {
                tabStrip.getChildAt(i).setClickable(enabled);
            }
        }
    }
}
