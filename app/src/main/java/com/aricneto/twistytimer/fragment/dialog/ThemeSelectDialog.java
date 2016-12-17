package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.ThemeUtils.TTTheme;

/**
 * A dialog fragment for selecting the preferred theme of the user-interface.
 */
public class ThemeSelectDialog extends DialogFragment {
    public static ThemeSelectDialog newInstance() {
        return new ThemeSelectDialog();
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final TTTheme oldTheme = Prefs.getTheme();
            final TTTheme newTheme = TTTheme.forViewID(view.getId());

            // If the theme has been changed, then the activity will need to be
            // recreated. The theme can only be applied properly during the
            // inflation of the layouts, so it has to go all they way back to
            // "Activity.onCreate()" to do that.
            if (newTheme != oldTheme) {
                Prefs.edit().putTheme(newTheme).apply();
                ((MainActivity) getActivity()).onRecreateRequired();
            }

            dismiss();
        }
    };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View dialogView = inflater.inflate(
            R.layout.dialog_theme_select, container);

        for (TTTheme theme : TTTheme.values()) {
            final View blob = dialogView.findViewById(theme.getViewID());

            if (blob instanceof TextView) {
                setBlob((TextView) blob, theme.getColorResID())
                    .setOnClickListener(clickListener);
            } else {
                Log.e(ThemeSelectDialog.class.getSimpleName(),
                        "Cannot find 'blob' view for theme: " + theme);
            }
        }

        final Window window = getDialog().getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }

        return dialogView;
    }

    private View setBlob(TextView view, @ColorRes int colorRes) {
        // "wrap" the drawable to support tinting at all API levels and make
        // it mutable, so that changes do not affect other views that are using
        // the same drawable resource.
        final Drawable drawable = DrawableCompat.wrap(
                ContextCompat.getDrawable(
                    getContext(), R.drawable.thumb_circle)).mutate();

        DrawableCompat.setTint(drawable,
            ContextCompat.getColor(getContext(), colorRes));
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
        view.setCompoundDrawablesWithIntrinsicBounds(
            drawable, null, null, null);

        return view;
    }
}
