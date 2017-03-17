package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.StrikeoutTextView;
import com.aricneto.twistytimer.scramble.ScrambleGenerator;
import com.aricneto.twistytimer.utils.FireAndForgetExecutor;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.TimeUtils;

import org.joda.time.DateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_ONE_SOLVE_UPDATED;
import static com.aricneto.twistytimer.utils.TTIntent
    .CATEGORY_SOLVE_DATA_CHANGES;

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
    @BindView(R.id.timeText)       StrikeoutTextView timeText;
    @BindView(R.id.penaltyText)    TextView          penaltyText;
    @BindView(R.id.penaltyButton)  ImageView         penaltyButton;
    @BindView(R.id.dateText)       TextView          dateText;
    @BindView(R.id.commentText)    TextView          commentText;
    @BindView(R.id.commentButton)  ImageView         commentButton;
    @BindView(R.id.scrambleText)   TextView          scrambleText;
    @BindView(R.id.overflowButton) ImageView         overflowButton;

    /**
     * The solve to be displayed. Solves are immutable, so this reference will
     * be updated if changes are applied.
     */
    // NOTE: This is set when the dialog is opened (from a fragment argument)
    // and then only when an "ACTION_ONE_SOLVE_UPDATED" broadcast is received.
    // The pattern is that changes to the solve create a new instance of the
    // solve; this new solve is saved to the database asynchronously; and this
    // field is updated when notification is received that saving is complete.
    private Solve mSolve;

    /**
     * Receives broadcasts announcing that a solve has been updated. If the
     * updated solve matches the one being edited, the fields displaying the
     * details of that solve are updated.
     */
    private final TTIntent.TTFragmentBroadcastReceiver mSolveDataChangedReceiver
        = new TTIntent.TTFragmentBroadcastReceiver(
            this, CATEGORY_SOLVE_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            TTIntent.validate(intent);

            if (ACTION_ONE_SOLVE_UPDATED.equals(intent.getAction())) {
                final Solve newSolve = TTIntent.getSolve(intent);

                if (newSolve != null
                    && (mSolve == null || newSolve.getID() == mSolve.getID())) {
                    mSolve = newSolve;
                    updateSolveDetails(newSolve);
                }
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

        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.overflowButton: showPopupMenu(); break;
                    case R.id.penaltyButton:  showEditPenaltiesDialog(); break;
                    case R.id.commentButton:  showEditCommentDialog(); break;
                    case R.id.scrambleText:   showScrambleImageDialog(); break;
                }
            }
        };

        commentButton.setOnClickListener(listener);
        overflowButton.setOnClickListener(listener);
        penaltyButton.setOnClickListener(listener);
        scrambleText.setOnClickListener(listener); // View might be invisible.

        mSolve = getArguments().getParcelable(ARG_SOLVE);

        return dialogView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onStart() {
        TTIntent.registerReceiver(mSolveDataChangedReceiver);
        super.onStart();

        // It is a bug if the fragment argument is null, so let it crash.
        updateSolveDetails(mSolve);
    }

    @Override
    public void onStop() {
        TTIntent.unregisterReceiver(mSolveDataChangedReceiver);
        super.onStop();
    }

    /**
     * Updates the fields of this dialog fragment with the details of the solve
     * attempt.
     *
     * @param solve The solve attempt whose details are to be presented.
     */
    private void updateSolveDetails(@NonNull Solve solve) {
        final Penalties penalties = solve.getPenalties();

        // The solve time is pretty-formatted with a smaller text size for the
        // smallest time units.
        timeText.setText(TimeUtils.prettyFormatResultTime(solve.getTime()));
        timeText.setChecked(penalties.hasDNF());
        dateText.setText(
            new DateTime(solve.getDate()).toString("d MMM y'\n'H':'mm"));

        if (penalties.hasPenalties()) {
            penaltyText.setText(solve.toStringResult());
            penaltyText.setVisibility(View.VISIBLE);
        } else {
            penaltyText.setVisibility(View.GONE);
        }

        if (solve.hasScramble()) {
            scrambleText.setText(solve.getScramble());
            scrambleText.setVisibility(View.VISIBLE);
        } else {
            scrambleText.setVisibility(View.GONE);
        }

        if (solve.hasComment()) {
            commentText.setText(solve.getComment());
            commentText.setVisibility(View.VISIBLE);
        } else {
            commentText.setVisibility(View.GONE);
        }
    }

    /**
     * <p>
     * Displays a dialog to edit the penalties incurred during the solve
     * attempt.
     * </p>
     * <p>
     * The {@link Penalties} API supports pre-start penalties (those before and
     * during the inspection period) and post-start penalties (those after the
     * solve timer starts, including after it eventually stops). Both sets of
     * penalties support a "did-not-finish" (DNF) penalty and a number of
     * "+2 SECONDS" (+2s) penalties (up to a maximum limit). If the inspection
     * period is overrun, a +2s penalty is incurred automatically. If that short
     * overrun period elapses, a DNF penalty is incurred automatically and the
     * solve attempt is terminated before the solve timer is ever started, so
     * these are both pre-start penalties. If there is a pre-start DNF penalty,
     * then there cannot be any post-start penalties, as the solve attempt
     * never started.
     * </p>
     * <p>
     * The incurring of penalties conforms to the WCA Regulations and the
     * procedure is quite clear, but users are not expected to have committed
     * such regulations to memory and may be confused if a strict interpretation
     * is implemented in the user interface. Therefore, some simplifications
     * are applied to improve usability while providing sufficient functionality
     * for use of the app as an informal means of timing solve attempts. The
     * simplifications are as follows:
     * </p>
     * <ul>
     *     <li>
     *         Pre-start penalties cannot be edited. Pre-start penalties are
     *         incurred automatically; they are not incurred at the discretion
     *         of the user. There is no justification for annulling them.
     *     </li>
     *     <li>
     *         If a pre-start DNF penalty has been incurred, the post-start
     *         penalties cannot be edited, as a pre-start DNF dictates that
     *         the solve attempt never started. If users do not want to record
     *         DNF solves, they can delete the solve records rather than
     *         annulling a DNF penalty that would result in a solve time of 2s
     *         (just the +2s penalty for overrunning the inspection period).
     *     </li>
     *     <li>
     *         If users wish to incur other pre-start penalties for infractions
     *         that occurred before the solve timer started, they can simply
     *         incur them as post-start penalties (unless a pre-start DNF
     *         penalty was incurred).
     *     </li>
     *     <li>
     *         Post-start penalties are incurred solely at the discretion of
     *         the user. Therefore, users have the same discretion to annul
     *         post-start penalties that they have incurred on their solve
     *         attempts. A post-start DNF penalty does not preclude incurring
     *         post-start +2s penalties.
     *     </li>
     * </ul>
     * <p>
     * Note that these simplifications are not imposed by the penalties API;
     * they exist only to avoid a confusing user interface. If they are found
     * to be problematic, the user interface can be altered to remedy the
     * issues; there is no need to change the underlying handling of penalties.
     * </p>
     * <p>
     * The dialog presented by this method displays the current solve time
     * inclusive of penalties (it will be struck out if there is a DNF). The
     * pre-start penalties are displayed as text and there are no options to
     * edit these penalties. The post-start penalties are similarly displayed,
     * but editing controls are provided to toggle a single DNF penalty and to
     * increment or decrement the number of +2s penalties. As the post-start
     * penalties are edited, the solve time is updated to reflect the change to
     * the penalties. The time exclusive of penalties is not displayed to avoid
     * cluttering the presentation.
     * </p>
     */
    private void showEditPenaltiesDialog() {
        final MaterialDialog dialog = new MaterialDialog.Builder(getContext())
            .customView(R.layout.dialog_penalty_details, false)
            .negativeText(R.string.action_cancel)
            .positiveText(R.string.action_ok)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog,
                                    @NonNull DialogAction which) {
                    //noinspection ConstantConditions (Set above, so not null.)
                    final Penalties penalties
                        = (Penalties) dialog.getCustomView().getTag();

                    if (!penalties.equals(mSolve.getPenalties())) {
                        dialog.dismiss(); // ...this penalties dialog.
                        // "mSolve" and the views of the main dialog are updated
                        // on notification that the asynchronous update is done.
                        getEditingActivity().onUpdateSolveTime(
                            mSolve.withPenaltiesAdjustingTime(penalties));
                    }
                }
            })
            .build();

        final View customView = dialog.getCustomView();

        // Use the view's "tag" to hold the penalties. It will be updated as
        // the penalties are edited and can then be retrieved when "OK" is
        // clicked and applied to the solve before updating the database.
        //noinspection ConstantConditions (Set above, so not null.)
        customView.setTag(mSolve.getPenalties());
        updatePenaltiesDialog(customView);

        dialog.show();
    }

    /**
     * Updates the display of the penalties in the penalties dialog.
     *
     * @param view
     *     The custom content view of the dialog for editing penalties. The
     *     current state of the penalties in edit are taken from the "tag" of
     *     this view.
     */
    private void updatePenaltiesDialog(final View view) {
        final Penalties penalties = (Penalties) view.getTag();

        // Display the solve time including any penalties by creating a new,
        // proposed solve reflecting any edits made to the penalties.
        final Solve newSolve = mSolve.withPenaltiesAdjustingTime(penalties);
        final StrikeoutTextView timeTV
            = (StrikeoutTextView) view.findViewById(R.id.time);

        timeTV.setText(TimeUtils.prettyFormatResultTime(newSolve.getTime()));
        timeTV.setChecked(penalties.hasDNF()); // Strike out if a DNF.

        // Display the penalties.
        final TextView preStartTV
            = (TextView) view.findViewById(R.id.pre_start_penalties);

        preStartTV.setText(formatPreStartPenalties(penalties));

        final TextView postStartTV
            = (TextView) view.findViewById(R.id.post_start_penalties);

        postStartTV.setText(formatPostStartPenalties(penalties));

        // Configure the editing buttons.
        if (penalties.hasPreStartDNF()) {
            // If there is a pre-start DNF, then hide the post-start penalty
            // editing buttons. As they are hidden, there is no need to bother
            // disabling them or updating their text.
            view.findViewById(R.id.penalty_button_panel)
                .setVisibility(View.GONE);
        } else {
            // The listener will be re-added on each call to this method, but
            // that should not cause any problems.
            final View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Get the penalties from the tag on the custom view.
                    final Penalties p = (Penalties) view.getTag();

                    switch (v.getId()) {
                        case R.id.button_dnf:
                            if (p.canIncurPostStartPenalty(Penalty.DNF)) {
                                view.setTag(
                                    p.incurPostStartPenalty(Penalty.DNF));
                            } else {
                                view.setTag(
                                    p.annulPostStartPenalty(Penalty.DNF));
                            }
                            break;

                        case R.id.button_plus_two:
                            view.setTag(
                                p.incurPostStartPenalty(Penalty.PLUS_TWO));
                            break;

                        case R.id.button_minus_two:
                            view.setTag(
                                p.annulPostStartPenalty(Penalty.PLUS_TWO));
                            break;
                    }

                    updatePenaltiesDialog(view);
                }
            };

            final TextView dnfBtn
                = (TextView) view.findViewById(R.id.button_dnf);
            final TextView plus2Btn
                = (TextView) view.findViewById(R.id.button_plus_two);
            final TextView minus2Btn
                = (TextView) view.findViewById(R.id.button_minus_two);

            dnfBtn.setOnClickListener(listener);
            plus2Btn.setOnClickListener(listener);
            minus2Btn.setOnClickListener(listener);

            if (penalties.canIncurPostStartPenalty(Penalty.DNF)) {
                dnfBtn.setText(R.string.edit_penalties_plus_dnf);
            } else if (penalties.canAnnulPostStartPenalty(Penalty.DNF)) {
                dnfBtn.setText(R.string.edit_penalties_minus_dnf);
            }

            plus2Btn.setEnabled(
                penalties.canIncurPostStartPenalty(Penalty.PLUS_TWO));
            minus2Btn.setEnabled(
                penalties.canAnnulPostStartPenalty(Penalty.PLUS_TWO));
        }
    }

    /**
     * Formats the pre-start penalties to a string.
     *
     * @return The pre-start penalties in string form.
     */
    private String formatPreStartPenalties(@NonNull Penalties penalties) {
        if (penalties.hasPreStartPenalties()) {
            final StringBuilder s = new StringBuilder(50);
            final int preStartPlus2s = penalties.getPreStartPlusTwoCount();

            if (preStartPlus2s > 0) {
                if (preStartPlus2s > 1) {
                    // "\u00d7" (multiply sign) looks nicer than "x" (letter).
                    s.append(preStartPlus2s).append('\u00d7');
                }
                // FIXME? Not using "Penalty.PLUS_TWO.getDescription()" for now.
                // Need to use "+2" in some places and "2s" in others.
                s.append("2s");
            }

            if (penalties.hasPreStartDNF()) {
                if (preStartPlus2s > 0) {
                    s.append(" + ");
                }
                s.append(Penalty.DNF.getDescription());
            }

            return s.toString();
        } else {
            return getString(R.string.no_penalty);
        }
    }

    /**
     * Formats the post-start penalties to a string.
     *
     * @return The post-start penalties in string form.
     */
    public String formatPostStartPenalties(@NonNull Penalties penalties) {
        if (penalties.hasPostStartPenalties()) {
            final StringBuilder s = new StringBuilder(50);
            final int postStartPlus2s = penalties.getPostStartPlusTwoCount();

            if (postStartPlus2s > 0) {
                if (postStartPlus2s > 1) {
                    s.append(postStartPlus2s).append('\u00d7');
                }
                s.append("2s");
            }

            if (penalties.hasPostStartDNF()) {
                if (postStartPlus2s > 0) {
                    s.append(" + ");
                }
                s.append(Penalty.DNF.getDescription());
            }

            return s.toString();
        } else {
            return getString(R.string.no_penalty);
        }
    }

    private void showEditCommentDialog() {
        final MaterialDialog editDlg = new MaterialDialog.Builder(getContext())
            .title(R.string.edit_comment)
            .input("", mSolve.getComment(), new MaterialDialog.InputCallback() {
                @Override
                public void onInput(@NonNull MaterialDialog dialog,
                                    CharSequence input) {
                    getEditingActivity().onUpdateSolveTime(
                        mSolve.withComment(input.toString()));
                    showToast(R.string.added_comment);
                    dialog.dismiss(); // ...this edit comment dialog.
                }
            })
            .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            .positiveText(R.string.action_done)
            .negativeText(R.string.action_cancel)
            .build();

        final EditText editText = editDlg.getInputEditText();

        if (editText != null) {
            editText.setSingleLine(false);
            editText.setLines(3);
            editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        }

        editDlg.show();
    }

    private void showScrambleImageDialog() {
        final MaterialDialog imgDlg = new MaterialDialog.Builder(getContext())
            .customView(R.layout.item_scramble_img, false)
            .show();

        final ImageView imageView
            = (ImageView) imgDlg.findViewById(R.id.scramble_image);

        // Generate the scramble image on a background thread and show the image
        // as soon as it becomes available. This should be quick enough not to
        // require a progress indicator.
        FireAndForgetExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Drawable scrambleImg
                    = new ScrambleGenerator(mSolve.getPuzzleType())
                            .generateImage(mSolve.getScramble());

                // Must update image view on the main UI thread.
                FireAndForgetExecutor.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageDrawable(scrambleImg);
                    }
                });
            }
        });
    }

    private void showPopupMenu() {
        final PopupMenu menu = new PopupMenu(getActivity(), overflowButton);

        menu.getMenuInflater().inflate(mSolve.isHistory()
            ? R.menu.menu_list_detail_history : R.menu.menu_list_detail,
            menu.getMenu());

        menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // NOTE: If dismissing the dialog, do so first, as that should
                // ensure the broadcast listener is unregistered before the
                // delete/update operation is notified. The notification is not
                // necessary, as there is no point in updating fields in the
                // dialog when it is about to be closed.
                switch (item.getItemId()) {
                    case R.id.share:
                        // Do not dismiss: may want to share multiple times.
                        getEditingActivity().onShareSolveTime(mSolve);
                        break;

                    case R.id.remove:
                        dismiss();
                        getEditingActivity().onDeleteSolveTime(mSolve);
                        break;

                    case R.id.history_to:
                        dismiss();
                        getEditingActivity().onUpdateSolveTime(
                            mSolve.withHistory(true));
                        showToast(R.string.sent_to_history); // Speculative.
                        break;

                    case R.id.history_from:
                        dismiss();
                        getEditingActivity().onUpdateSolveTime(
                            mSolve.withHistory(false));
                        showToast(R.string.sent_to_session); // Speculative.
                        break;
                }
                return true;
            }
        });

        menu.show();
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
