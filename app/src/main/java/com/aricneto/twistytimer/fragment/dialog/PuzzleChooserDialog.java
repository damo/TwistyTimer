package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.adapter.PuzzleTypeSpinnerAdapter;
import com.aricneto.twistytimer.items.PuzzleType;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * <p>
 * A dialog fragment that chooses a puzzle type and category. Once the desired
 * options are chosen, the dialog relays the selection (via the parent activity)
 * to the "consumer" fragment that initiated this fragment. The "consumer"
 * fragment is identified by its fragment tag, which it passes to this chooser
 * in {@link #newInstance(int, String)} and which this chooser passes back to
 * the activity, so the activity can find that "consumer" fragment.
 * </p>
 * <p>
 * <i>This dialog fragment <b>must</b> be used in the context of an activity
 * that implements the {@link PuzzleCallback} interface, or exceptions will
 * occur.</i>
 * </p>
 */
public class PuzzleChooserDialog extends DialogFragment {
    /**
     * An interface that allows fragments to communicate changes to the selected
     * puzzle type and/or category. An activity implements this interface and
     * relays the details from one fragment to the other. While a receiving
     * fragment does not need to implement this interface, it makes the relay
     * operation in the activity a bit simpler.
     */
    public interface PuzzleCallback {
        /**
         * Notifies the listener that a new puzzle type and/or category have
         * been selected.
         *
         * @param tag
         *     The tag identifying the callback. The tag should be the fragment
         *     tag that identifies the fragment (known to the activity) to which
         *     the activity should relay the message.
         * @param puzzleType
         *     The newly-selected puzzle type.
         * @param solveCategory
         *     The name of the newly-selected solve category.
         */
        void onPuzzleSelected(
            @NonNull String tag, @NonNull PuzzleType puzzleType,
            @NonNull String solveCategory);
    }

    private Unbinder mUnbinder;

    @BindView(R.id.puzzleSpinner)   Spinner  puzzleSpinner;
    @BindView(R.id.categorySpinner) Spinner  categorySpinner;
    @BindView(R.id.selectButton)    TextView selectButton;

    /**
     * The name of the fragment argument holding a string resource ID for the
     * text to be displayed on the selection button that closes this chooser
     * dialog.
     */
    private static final String ARG_BUTTON_TEXT_RES_ID = "buttonTextResourceID";

    /**
     * The name of the fragment argument holding the fragment tag of the
     * fragment that instantiated this puzzle chooser.
     */
    private static final String ARG_CONSUMER_TAG = "consumerTag";

    /**
     * The selected puzzle type.
     */
    private PuzzleType mSelectedPuzzleType;

    /**
     * The selected solve category.
     */
    private String mSelectedSolveCategory;

    /**
     * Creates a new instance of this fragment.
     *
     * @param buttonTextResID
     *     The string resource ID of the string to be displayed on the select
     *     button that closes this fragment and reports the selection. If zero,
     *     the default "OK" will be shown.
     * @param consumerTag
     *     The fragment tag that identifies the fragment that instantiated this
     *     puzzle chooser. This "consumer" fragment will be informed, via the
     *     parent activity, of the selected puzzle type and category before
     *     this chooser is dismissed.
     *
     * @return
     *     The new instance of this puzzle chooser.
     */
    public static PuzzleChooserDialog newInstance(
            @StringRes int buttonTextResID, String consumerTag) {
        final PuzzleChooserDialog fragment = new PuzzleChooserDialog();
        final Bundle args = new Bundle();

        args.putInt(ARG_BUTTON_TEXT_RES_ID, buttonTextResID);
        args.putString(ARG_CONSUMER_TAG, consumerTag);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View dialogView = inflater.inflate(
            R.layout.dialog_puzzle_chooser_dialog, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        final @StringRes int buttonTextResID = getArguments() != null
            ? getArguments().getInt(ARG_BUTTON_TEXT_RES_ID, 0) : 0;

        if (buttonTextResID != 0) {
            // Override the default text.
            selectButton.setText(buttonTextResID);
        }

        // TODO: Default the initial value to whatever is in the "main state".
        puzzleSpinner.setAdapter(
            PuzzleTypeSpinnerAdapter.createForChooser(getContext()));
        mSelectedPuzzleType = (PuzzleType) puzzleSpinner.getSelectedItem();
        puzzleSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                mSelectedPuzzleType = PuzzleType.forOrdinal(position);
                categorySpinner.setAdapter(
                    getCategoryAdapterForType(mSelectedPuzzleType));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // TODO: Default the initial value to whatever is in the "main state".
        categorySpinner.setAdapter(
            getCategoryAdapterForType(mSelectedPuzzleType));
        mSelectedSolveCategory = (String) categorySpinner.getSelectedItem();
        categorySpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                mSelectedSolveCategory
                    = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Relay this information back to the fragment/activity that
                // opened this chooser.
                getRelayActivity().onPuzzleSelected(
                    getArguments().getString(ARG_CONSUMER_TAG, "Not set!"),
                    mSelectedPuzzleType, mSelectedSolveCategory);
                dismiss();
            }
        });

        final Window window = getDialog().getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialogView;
    }

    private SpinnerAdapter getCategoryAdapterForType(
            @NonNull PuzzleType puzzleType) {
        return new ArrayAdapter<>(
            getContext(), android.R.layout.simple_spinner_dropdown_item,
            TwistyTimer.getDBHandler().getAllCategoriesForType(puzzleType));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    /**
     * Gets the activity reference type cast to support the required interface
     * for relaying the puzzle type/category selection to another fragment.
     *
     * @return
     *     The attached activity, or {@code null} if no activity is attached.
     */
    private <A extends PuzzleCallback> A getRelayActivity() {
        //noinspection unchecked
        return (A) getActivity();
    }
}
