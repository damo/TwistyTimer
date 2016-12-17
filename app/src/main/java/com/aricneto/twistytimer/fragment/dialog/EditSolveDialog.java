package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.PopupMenu;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.scramble.ScrambleGenerator;
import com.aricneto.twistytimer.utils.FireAndForgetExecutor;
import com.aricneto.twistytimer.utils.TimeUtils;

import org.joda.time.DateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * <p>
 * The dialog that presents details of a recorded solve time. The dialog
 * presents the solve time data and options to edit or delete the solve time.
 * </p>
 * <p>
 * <i>This dialog fragment <b>must</b> be used in the context of an activity
 * that implements the {@link EditSolveCallbacks} interface and extends, or
 * exceptions will occur.</i>
 * </p>
 */
public class EditSolveDialog extends DialogFragment {
    /**
     * The call-back methods that advise the owning activity of edits that have
     * been made.
     */
    public interface EditSolveCallbacks {
        /**
         * Notifies the activity that a solve time should be deleted.
         *
         * @param solve The solve time to be deleted.
         */
        void onDeleteSolveTime(@NonNull Solve solve);

        /**
         * Notifies the activity that a solve time should be updated.
         *
         * @param solve The solve time to be updated.
         */
        void onUpdateSolveTime(@NonNull Solve solve);

        /**
         * Notifies the activity that a solve time should be shared.
         *
         * @param solve The solve time to be shared.
         */
        void onShareSolveTime(@NonNull Solve solve);
    }

    /**
     * The name of the fragment argument that contains the solve details.
     */
    private static final String ARG_SOLVE = Solve.class.getName();

    private Unbinder mUnbinder;
    @BindView(R.id.timeText)       TextView  timeText;
    @BindView(R.id.penaltyText)    TextView  penaltyText;
    @BindView(R.id.penaltyButton)  ImageView penaltyButton;
    @BindView(R.id.dateText)       TextView  dateText;
    @BindView(R.id.commentText)    TextView  commentText;
    @BindView(R.id.commentButton)  ImageView commentButton;
    @BindView(R.id.scramble_text)   TextView  scrambleText;
    @BindView(R.id.overflowButton) ImageView overflowButton;

    private Solve mSolve;

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.overflowButton:
                    final PopupMenu popupMenu
                        = new PopupMenu(getActivity(), overflowButton);

                    popupMenu.getMenuInflater().inflate(
                        mSolve.isHistory()
                            ? R.menu.menu_list_detail_history
                            : R.menu.menu_list_detail,
                        popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.share:
                                    getEditingActivity()
                                        .onShareSolveTime(mSolve);
                                    // Do not dismiss the dialog: may want to
                                    // share multiple times.
                                    break;

                                case R.id.remove:
                                    getEditingActivity()
                                        .onDeleteSolveTime(mSolve);
                                    dismiss();
                                    break;

                                case R.id.history_to:
                                    mSolve = mSolve.withHistory(true);
                                    getEditingActivity()
                                        .onUpdateSolveTime(mSolve);
                                    showToast(R.string.sent_to_history);
                                    dismiss();
                                    break;

                                case R.id.history_from:
                                    mSolve = mSolve.withHistory(false);
                                    getEditingActivity()
                                        .onUpdateSolveTime(mSolve);
                                    showToast(R.string.sent_to_session);
                                    dismiss();
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                    break;

                case R.id.penaltyButton:
                    // The indices of the values in "R.array.array_penalties"
                    // must match the penalty values defined in "Solve".
/*FIXME!!!
                    new MaterialDialog.Builder(getContext())
                            .title(R.string.select_penalty)
                            .items(Penalty.getDescriptions())
                            .itemsCallbackSingleChoice(mSolve.getPenalty().ordinal(),
                                    new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View itemView,
                                                           int which, CharSequence text) {
                                    // The penalties are displayed in enum value order (by ordinal),
                                    // so "which" is the ordinal of the selected penalty.
                                    mSolve.applyPenalty(Penalty.forOrdinal(which));
                                    getEditingActivity().onUpdateSolveTime(
                                        mSolve);
                                    dismiss();
                                    return true;
                                }
                            })
                            .negativeText(R.string.action_cancel)
                            .show();
*/
                    break;

                case R.id.commentButton:
                    MaterialDialog editCommentDialog
                        = new MaterialDialog.Builder(getContext())
                        .title(R.string.edit_comment)
                        .input("", mSolve.getComment(),
                            new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog,
                                                CharSequence input) {
                                mSolve = mSolve.withComment(input.toString());
                                getEditingActivity().onUpdateSolveTime(mSolve);
                                showToast(R.string.added_comment);
                                dismiss();
                            }
                        })
                        .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                        .positiveText(R.string.action_done)
                        .negativeText(R.string.action_cancel)
                        .build();

                    final EditText editText
                        = editCommentDialog.getInputEditText();

                    if (editText != null) {
                        editText.setSingleLine(false);
                        editText.setLines(3);
                        editText.setImeOptions(
                            EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                        editText.setImeOptions(
                            EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    }

                    editCommentDialog.show();
                    break;

                case R.id.scramble_text:
                    final MaterialDialog scrambleDialog
                        = new MaterialDialog.Builder(getContext())
                            .customView(R.layout.item_scramble_img, false)
                            .show();
                    final ImageView imageView = (ImageView) scrambleDialog
                        .findViewById(R.id.scramble_image);

                    // Generate the scramble image on a background thread and
                    // update it as soon as it becomes available. TODO: Perhaps
                    // add a progress indicator; might need to "upgrade" to an
                    // "AsyncTask".
                    FireAndForgetExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            final Drawable scrambleImg
                                = new ScrambleGenerator(mSolve.getPuzzleType())
                                     .generateImageFromScramble(
                                         mSolve.getScramble());

                            // Must update image view on the main UI thread.
                            FireAndForgetExecutor.executeOnMainThread(
                                new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageDrawable(scrambleImg);
                                }
                            });
                        }
                    });
                    break;
            }
        }
    };

    public static EditSolveDialog newInstance(@NonNull Solve solve) {
        // Best-practice passes the values through "setArguments", as that
        // ensures the default constructor takes no parameters and the fragment
        // can be restored properly from saved instance state.
        final EditSolveDialog dialog = new EditSolveDialog();
        final Bundle args = new Bundle();

        args.putParcelable(ARG_SOLVE, solve);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View dialogView = inflater.inflate(
            R.layout.dialog_time_details, container);
        mUnbinder = ButterKnife.bind(this, dialogView);
        //this.setEnterTransition(R.anim.activity_slide_in);

        final Window window = getDialog().getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }

        mSolve = getArguments().getParcelable(ARG_SOLVE);

        // The solve time is formatted with a smaller text size for the
        // smallest time units. The correct rounding of the time is performed
        // by "Solve.getTime()".

        // "mSolve" will not be null if "newInstance" was called. If null, it
        // is a bug.
        //noinspection ConstantConditions
        timeText.setText(TimeUtils.formatResultPretty(mSolve.getTime()));
        dateText.setText(new DateTime(mSolve.getDate())
            .toString("d MMM y'\n'H':'mm"));

        if (!mSolve.getPenalties().hasPenalties()) {
            penaltyText.setVisibility(View.GONE);
        } else {
            // Penalty is "+2" or "DNF".
            // FIXME: Penalties are "+2"s AND/OR "DNF".
//            penaltyText.setText(mSolve.getPenalty().getDescriptionResID());
            penaltyButton.setOnClickListener(mClickListener);

            if (mSolve.getPenalties().hasDNF()) {
                // For a "DNF", "strike-through" the time text.
                timeText.setPaintFlags(timeText.getPaintFlags()
                                       | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }

        if (mSolve.hasScramble()) {
            scrambleText.setText(mSolve.getScramble());
            scrambleText.setVisibility(View.VISIBLE);
            scrambleText.setOnClickListener(mClickListener);
        }

        if (mSolve.hasComment()) {
            commentText.setText(mSolve.getComment());
            commentText.setVisibility(View.VISIBLE);
            commentButton.setOnClickListener(mClickListener);
        }

        overflowButton.setOnClickListener(mClickListener);

        return dialogView;
    }

    /**
     * Shows a brief "toast" message.
     *
     * @param messageResID
     *     The string resource ID for the message to be displayed.
     */
    private void showToast(@StringRes int messageResID) {
        Toast.makeText(getContext(), getString(messageResID),
            Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    /**
     * Gets the activity reference type cast to support the required interfaces
     * and base classes for solve-time editing operations.
     *
     * @return
     *     The attached activity, or {@code null} if no activity is attached.
     */
    private <A extends Activity & EditSolveCallbacks> A getEditingActivity() {
        //noinspection unchecked
        return (A) getActivity();
    }
}
