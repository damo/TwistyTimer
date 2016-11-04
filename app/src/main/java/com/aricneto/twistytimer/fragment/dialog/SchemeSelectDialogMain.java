package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.utils.FaceColor;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A dialog fragment for the customisation of the color-scheme of the cube images generated to
 * show the scrambles.
 */
public class SchemeSelectDialogMain extends DialogFragment {

    private Unbinder mUnbinder;

    @BindViews({
            R.id.up, R.id.down, R.id.left, R.id.right, R.id.front, R.id.back
    }) View[] faces;

    @BindView(R.id.reset) TextView reset;
    @BindView(R.id.done)  TextView done;

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View colorView) {
            final FaceColor faceColor = FaceColor.forViewID(colorView.getId());
            final int color = faceColor.getColor();
            final ColorPicker picker = new ColorPicker(getActivity(),
                    // Extract the R, G and B components of the color.
                    (color >>> 16) & 0xff, (color >>> 8) & 0xff, color & 0xff);

            // Show color picker dialog
            picker.show();

            // On Click listener for the dialog, when the user select the color
            picker.findViewById(R.id.okColorButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View ignored) {
                    final int color = picker.getColor();

                    setColor(colorView, color); // Set the new color of the view.
                    faceColor.putColor(color);  // Save the new color to the shared preferences.
                    picker.dismiss();
                }
            });
        }
    };

    public static SchemeSelectDialogMain newInstance() {
        return new SchemeSelectDialogMain();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_scheme_select_main, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        for (View face : faces) {
            setColor(face, FaceColor.forViewID(face.getId()).getColor());
            face.setOnClickListener(clickListener);
        }

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.reset_colorscheme)
                        .positiveText(R.string.action_reset_colorscheme)
                        .negativeText(R.string.action_cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(
                                    @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                FaceColor.resetAllColors(); // Remove all the shared preferences.

                                for (View face : faces) {
                                    setColor(face, FaceColor.forViewID(face.getId()).getColor());
                                }
                            }
                        })
                        .show();
            }
        });

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onRecreateRequired();
                }
                dismiss();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }

    private void setColor(View view, int color) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.square);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, color);
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();
        view.setBackground(wrap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
