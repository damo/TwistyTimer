package com.aricneto.twistytimer.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.utils.Prefs;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @BindView(R.id.actionbar) Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mToolbar.setTitle(R.string.title_activity_settings);
        mToolbar.setTitleTextColor(Color.WHITE);
        mToolbar.setNavigationIcon(R.drawable.ic_action_arrow_back_white_24);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            // Add the main "parent" settings fragment. It is not added to be back stack, so that
            // when "Back" is pressed, the "SettingsActivity" will exit, which is appropriate.
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * Finds the preference whose key matches the string value of the given preference key
     * string resource ID.
     *
     * @param fragment
     *     The preference fragment in which to search for the preference with the given key.
     * @param prefKeyResID
     *     The string resource ID of the preference key.
     * @return
     *     The preference that matches that key; or {@code null} if no such preference is found.
     */
    private static Preference find(PreferenceFragment fragment, int prefKeyResID) {
        return fragment.getPreferenceScreen().findPreference(fragment.getString(prefKeyResID));
    }

    // TODO: Should this be using "android.support.v7.preference.PreferenceFragmentCompat" or
    // "android.support.v14.preference.PreferenceFragment" instead? Those implementations have
    // more support for new API features and Material Design themes.
    public static class SettingsFragment extends PreferenceFragment {
        private final android.preference.Preference.OnPreferenceClickListener clickListener
                = new android.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                switch (Prefs.keyToResourceID(preference.getKey(),
                        R.string.pk_inspection_time_s,
                        R.string.pk_show_x_cross_hints,
                        R.string.pk_open_timer_appearance_settings)) {

                    case R.string.pk_inspection_time_s:
                        createInspectionTimeDialog();
                        break;

                    case R.string.pk_show_x_cross_hints:
                        // If the preference is now enabled, warn about its performance issues.
                        if (Prefs.getBoolean(R.string.pk_show_x_cross_hints,
                                R.bool.default_show_x_cross_hints)) {
                            new MaterialDialog.Builder(getActivity())
                                    .title(R.string.warning)
                                    .content(R.string.showHintsXCrossSummary)
                                    .positiveText(R.string.action_ok)
                                    .show();
                        }
                        break;

                    case R.string.pk_open_timer_appearance_settings:
                        // Open the new "child" settings fragment and add it to be back stack, so
                        // that if "Back" is pressed, this "parent" fragment will be restored.
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.main_activity_container,
                                        new TimerAppearanceSettingsFragment())
                                .addToBackStack(null)
                                .commit();
                        break;
                }
                return false;
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);

            find(this, R.string.pk_inspection_time_s)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_open_timer_appearance_settings)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_show_x_cross_hints)
                    .setOnPreferenceClickListener(clickListener);
        }

        private void createInspectionTimeDialog() {
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.inspection_time)
                    .input("", String.valueOf(Prefs.getInspectionTime()),
                            new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            try {
                                final int time = Integer.parseInt(input.toString());

                                Prefs.edit().putInt(R.string.pk_inspection_time_s, time).apply();
                            } catch (NumberFormatException e) {
                                Toast.makeText(getActivity(),
                                        R.string.invalid_time, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Remove the value and its default will be used when needed.
                            Prefs.edit().remove(R.string.pk_inspection_time_s).apply();
                        }
                    })
                    .show();
        }
    }

    public static class TimerAppearanceSettingsFragment extends PreferenceFragment {
        private final android.preference.Preference.OnPreferenceClickListener clickListener
                = new android.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                switch (Prefs.keyToResourceID(preference.getKey(),
                        R.string.pk_timer_display_scale_pc,
                        R.string.pk_timer_text_offset_px,
                        R.string.pk_scramble_image_scale_pc,
                        R.string.pk_scramble_text_scale_pc,
                        R.string.pk_advanced_timer_settings_enabled)) {

                    case R.string.pk_timer_display_scale_pc:
                        createTextScaleDialog(R.string.pk_timer_display_scale_pc,
                                R.integer.default_timer_display_scale_pc, "12.34", 60, true);
                        break;

                    case R.string.pk_timer_text_offset_px:
                        createTextOffsetDialog();
                        break;

                    case R.string.pk_scramble_text_scale_pc:
                        createTextScaleDialog(R.string.pk_scramble_text_scale_pc,
                                R.integer.default_scramble_text_scale_pc,
                                "R U R' U' R' F R2 U' R' U' R U R' F'", 14, false);
                        break;

                    case R.string.pk_scramble_image_scale_pc:
                        createScrambleImageScaleDialog();
                        break;

                    case R.string.pk_advanced_timer_settings_enabled:
                        if (Prefs.getBoolean(R.string.pk_advanced_timer_settings_enabled,
                                R.bool.default_advanced_timer_settings_enabled)) {
                            new MaterialDialog.Builder(getActivity())
                                    .title(R.string.warning)
                                    .content(R.string.advanced_pref_summary)
                                    .positiveText(R.string.action_ok)
                                    .show();
                        }
                        break;
                }
                return false;
            }
        };

        @SuppressLint("CommitPrefEdits")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.prefs_timer_appearance);

            find(this, R.string.pk_timer_display_scale_pc)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_timer_text_offset_px)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_scramble_text_scale_pc)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_scramble_image_scale_pc)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_advanced_timer_settings_enabled)
                    .setOnPreferenceClickListener(clickListener);
        }

        /**
         * Creates a dialog for setting the text scale. The preference value is a percentage of the
         * default text size for the respective component, not an absolute text size. For example,
         * if the default text size for the timer is 60 sp, the preference can be set to a value
         * such as 150, to indicate that the preferred text size is 150% of 60 sp, or 90 sp. The
         * scale value 150 is then stored in the preferences.
         *
         * @param prefKeyResID
         *     The string resource ID of the integer preference that holds the current value and
         *     that will be updated with the new value.
         * @param prefDefaultResID
         *     The integer resource ID of the default value for this integer preference.
         * @param sampleText
         *     The sample text to be displayed to guide the user when setting the scale.
         * @param sampleTextSize
         *     The size (in SIP) at which to display the sample text (i.e., the size that
         *     corresponds to 100% on the scale).
         * @param sampleIsBold
         *     {@code true} to show the sample text in a bold typeface, or {@code false} to use
         *     the default typeface.
         */
        private void createTextScaleDialog(
                @StringRes final int prefKeyResID, @IntegerRes int prefDefaultResID,
                String sampleText, final int sampleTextSize, boolean sampleIsBold) {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar scaleBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView sample = (TextView) dialogView.findViewById(R.id.text);

            scaleBar.setMax(300);
            scaleBar.setProgress(Prefs.getInt(prefKeyResID, prefDefaultResID));

            sample.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP, sampleTextSize * scaleBar.getProgress() / 100f);
            if (sampleIsBold) {
                sample.setTypeface(Typeface.DEFAULT_BOLD);
            }
            sample.setText(sampleText);

            scaleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    sample.setTextSize(
                            TypedValue.COMPLEX_UNIT_SP, sampleTextSize * progress / 100f);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            new MaterialDialog.Builder(getActivity())
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Save the scale, but ensure it is at least 10%.
                            Prefs.edit().putInt(prefKeyResID,
                                    Math.max(scaleBar.getProgress(), 10)).apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Remove the value and its default will be used when needed.
                            Prefs.edit().remove(prefKeyResID).apply();
                        }
                    })
                    .show();
        }

        private void createTextOffsetDialog() {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView text = (TextView) dialogView.findViewById(R.id.text);

            // The offset is presented so that the center point on the scale represents an offset
            // value of zero.
            final int toCenter = 250;

            seekBar.setMax(toCenter * 2);
            seekBar.setProgress(
                    Prefs.getInt(R.string.pk_timer_text_offset_px,
                            R.integer.default_timer_text_offset_px) + toCenter);

            final float defaultY = text.getY();

            text.setY(defaultY - seekBar.getProgress() - toCenter);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setText("12.34");

            text.getLayoutParams().height += 650;

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    text.setY(defaultY - (i - toCenter));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            new MaterialDialog.Builder(getActivity())
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(R.string.pk_timer_text_offset_px,
                                    seekBar.getProgress() - toCenter)
                                    .apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Remove the preference and its default will be used when needed.
                            Prefs.edit().remove(R.string.pk_timer_text_offset_px).apply();
                        }
                    })
                    .show();
        }

        private void createScrambleImageScaleDialog() {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress_image, null);
            final AppCompatSeekBar scaleBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final View image = dialogView.findViewById(R.id.image);
            final int currentScale = Prefs.getInt(
                    R.string.pk_scramble_image_scale_pc, R.integer.default_scramble_image_scale_pc);

            scaleBar.setMax(300);
            scaleBar.setProgress(currentScale);

            final int defaultWidth  = image.getLayoutParams().width;
            final int defaultHeight = image.getLayoutParams().height;

            image.getLayoutParams().width  *= currentScale / 100f;
            image.getLayoutParams().height *= currentScale / 100f;

            scaleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    final LinearLayout.LayoutParams params
                            = (LinearLayout.LayoutParams) image.getLayoutParams();

                    params.width  = Math.round(defaultWidth  * progress / 100f);
                    params.height = Math.round(defaultHeight * progress / 100f);

                    image.setLayoutParams(params);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            new MaterialDialog.Builder(getActivity())
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(R.string.pk_scramble_image_scale_pc,
                                    Math.max(scaleBar.getProgress(), 10))
                                    .apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Remove value, so default will be used the next time it is retrieved.
                            Prefs.edit().remove(R.string.pk_scramble_image_scale_pc).apply();
                        }
                    })
                    .show();
        }
    }
}
