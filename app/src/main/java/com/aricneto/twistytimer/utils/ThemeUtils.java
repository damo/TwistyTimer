package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;

import com.aricneto.twistify.R;

/**
 * Utility class to make themeing easier.
 */
public final class ThemeUtils {
    /**
     * The default theme.
     */
    public static final TTTheme DEFAULT_THEME = TTTheme.INDIGO;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ThemeUtils() {
    }

    /**
     * An enumeration of the supported application themes for <i>Twisty Timer</i>. Each value can
     * be queried to retrieve useful related information, such as the style resource IDs for the
     * theme (the normal theme and the associated no-background theme), the representative color
     * resource ID for the theme, and the ID of the view used to present the theme color in the
     * theme selection dialog.
     */
    public enum TTTheme {
        INDIGO(R.style.DefaultTheme, R.style.DefaultTheme_NoBackground,
                R.id.indigo, R.color.md_indigo_500),

        PURPLE(R.style.PurpleTheme, R.style.PurpleTheme_NoBackground,
                R.id.purple, R.color.md_purple_500),

        TEAL(R.style.TealTheme, R.style.TealTheme_NoBackground,
                R.id.teal, R.color.md_teal_500),

        PINK(R.style.PinkTheme, R.style.PinkTheme_NoBackground,
                R.id.pink, R.color.md_pink_500),

        RED(R.style.RedTheme, R.style.RedTheme_NoBackground,
                R.id.red, R.color.md_red_500),

        BROWN(R.style.BrownTheme, R.style.BrownTheme_NoBackground,
                R.id.brown, R.color.md_brown_500),

        BLUE(R.style.BlueTheme, R.style.BlueTheme_NoBackground,
                R.id.blue, R.color.md_blue_500),

        BLACK(R.style.BlackTheme, R.style.BlackTheme_NoBackground,
                R.id.black, R.color.md_black_1000),

        ORANGE(R.style.OrangeTheme, R.style.OrangeTheme_NoBackground,
                R.id.orange, R.color.md_deep_orange_500),

        GREEN(R.style.GreenTheme, R.style.GreenTheme_NoBackground,
                R.id.green, R.color.md_green_500),

        DEEP_PURPLE(R.style.DeepPurpleTheme, R.style.DeepPurpleTheme_NoBackground,
                R.id.deepPurple, R.color.md_deep_purple_500),

        BLUE_GRAY(R.style.BlueGrayTheme, R.style.BlueGrayTheme_NoBackground,
                R.id.blueGray, R.color.md_blue_grey_500);

        /**
         * The resource ID of the theme's main style.
         */
        @StyleRes
        private final int mMainStyleResID;

        /**
         * The resource ID of the theme's no-background style.
         */
        @StyleRes
        private final int mNoBGStyleResID;

        /**
         * The ID of the view used to present the theme in the theme selection dialog.
         */
        @IdRes
        private final int mViewID;

        /**
         * The resource ID of the theme's representative color used in the theme selection dialog.
         */
        @ColorRes
        private final int mColorResID;

        /**
         * Creates a new theme value.
         *
         * @param mainStyleResID
         *     The resource ID of the theme's main style.
         * @param noBGStyleResID
         *     The resource ID of the theme's no-background style.
         * @param viewID
         *     The ID of the view used to present the theme in the theme selection dialog.
         * @param colorResID
         *     The resource ID of the theme's representative color used in the theme selection
         *     dialog.
         */
        TTTheme(@StyleRes int mainStyleResID, @StyleRes int noBGStyleResID,
                @IdRes int viewID, @ColorRes int colorResID) {
            mMainStyleResID = mainStyleResID;
            mNoBGStyleResID = noBGStyleResID;
            mViewID = viewID;
            mColorResID = colorResID;
        }

        /**
         * Gets the theme that is represented by the given view ID.
         *
         * @param viewID
         *     The ID of the view that is representing a theme.
         *
         * @return
         *     The theme that is represented by that view, or the default theme if the view ID is
         *     not recognised.
         */
        public static TTTheme forViewID(@IdRes int viewID) {
            TTTheme result = DEFAULT_THEME;

            for (TTTheme theme : TTTheme.values()) {
                if (theme.mViewID == viewID) {
                    result = theme;
                    break;
                }
            }

            return result;
        }

        /**
         * Gets the resource ID for this theme's main style.
         *
         * @return The style of this theme.
         */
        @StyleRes
        public int getMainStyleResID() {
            return mMainStyleResID;
        }

        /**
         * Gets the resource ID for this theme's no-background style.
         *
         * @return The no-background style of this theme.
         */
        @StyleRes
        public int getNoBackgroundStyleResID() {
            return mNoBGStyleResID;
        }

        /**
         * Gets the view ID for the view that represents this theme in the theme chooser dialog.
         *
         * @return The ID of the view that represents this theme.
         */
        @IdRes
        public int getViewID() {
            return mViewID;
        }

        /**
         * Gets the resource ID for the representative color of this theme.
         *
         * @return The color of this theme.
         */
        @ColorRes
        public int getColorResID() {
            return mColorResID;
        }
    }

    /**
     * Gets the style resource ID for the user's preferred theme. This is the theme that has been
     * selected and saved to the preferences (or the default theme); it is not necessarily the same
     * as the theme that is currently applied to the user interface. For each preferred theme, the
     * style resource ID depends on whether or not a colored background has been enabled.
     *
     * @return
     *     The style resource ID of the user's chosen preferred theme.
     */
    @StyleRes
    public static int getPreferredThemeStyleResID() {
        final TTTheme theme = Prefs.getTheme(); // Returns the default theme, if not saved before.

        return Prefs.getBoolean(R.string.pk_timer_bg_enabled, R.bool.default_timer_bg_enabled)
                ? theme.getMainStyleResID() : theme.getNoBackgroundStyleResID();
    }

    /**
     * Gets a color from an attr resource value
     * @param context Context
     * @param attrRes The attribute resource (ex. R.attr.colorPrimary)
     * @return @ColorRes
     */
    public static int fetchAttrColor(Context context, @AttrRes int attrRes) {
        final TypedValue value = new TypedValue ();
        context.getTheme().resolveAttribute(attrRes, value, true);
        return value.data;
    }

    public static Drawable tintDrawable(
            Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();

        return wrap;
    }

    // The following two functions are used to tint the history switch

    public static Drawable tintPositiveThumb(
            Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.thumb_circle);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();

        Drawable[] layers = new Drawable[2];
        layers[0] = wrap;
        layers[1] = ContextCompat.getDrawable(context, drawableRes);

        return new LayerDrawable(layers);
    }

    public static Drawable tintNegativeThumb(
            Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();

        Drawable circle = ContextCompat.getDrawable(context, R.drawable.thumb_circle);
        Drawable circleWrap = DrawableCompat.wrap(circle);
        DrawableCompat.setTint(circleWrap, Color.WHITE);
        DrawableCompat.setTintMode(circleWrap, PorterDuff.Mode.MULTIPLY);
        circleWrap = circleWrap.mutate();

        Drawable[] layers = new Drawable[2];
        layers[0] = circleWrap;
        layers[1] = wrap;

        return new LayerDrawable(layers);
    }
}
