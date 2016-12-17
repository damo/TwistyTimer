package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.TimerListFragment;
import com.aricneto.twistytimer.fragment.dialog.EditSolveDialog;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.aricneto.twistytimer.utils.TimeUtils;

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.HashSet;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.aricneto.twistytimer.utils.TTIntent.*;

/**
 * A cursor adapter to support the presentation of the solve times in the
 * {@link TimerListFragment}. This cursor adapter handles the binding between
 * the data and the "time cards" displayed in the list/grid of selected solve
 * times. When a time-card is clicked.
 */
public class TimeCursorAdapter
        extends CursorRecyclerAdapter<RecyclerView.ViewHolder>
        implements DialogListener {
    private final FragmentManager mFragmentManager;

    private int mCardColor;
    private int mSelectedCardColor;

    private boolean mIsInSelectionMode;

    // A Set will be more efficient than a List, as "contains(Long)" is used
    // a lot.
    private Collection<Long> mSelectedSolveIDs = new HashSet<>();

    // Locks opening new windows until the last one is dismissed
    private boolean mIsLocked;

    public TimeCursorAdapter(Context context, Cursor cursor,
                             TimerListFragment listFragment) {
        super(cursor);
        mFragmentManager = listFragment.getFragmentManager();
        mCardColor = ThemeUtils.fetchAttrColor(
            context, R.attr.colorItemListBackground);
        mSelectedCardColor = ThemeUtils.fetchAttrColor(
            context, R.attr.colorItemListBackgroundSelected);
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        super.swapCursor(cursor);
        unselectAll();
        return cursor;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {

        return new TimeHolder(
            LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.item_time_list, parent, false));
    }

    @Override
    public void onBindViewHolderCursor(
            RecyclerView.ViewHolder viewHolder, Cursor cursor) {
        // Assume that "DatabaseHandler.getAllSolvesFor()" was used to load the
        // cursor. If so, "DatabaseHandler.getCurrentSolve()" is a convenient
        // way to avoid all the drama around column indices and column names.
        bindSolveToViewHolder(
            (TimeHolder) viewHolder, DatabaseHandler.getCurrentSolve(cursor));
    }

    @Override
    public void onDismissDialog() {
        setLocked(false);
    }

    public void unselectAll() {
        mSelectedSolveIDs.clear();
        mIsInSelectionMode = false;
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_OFF);
    }

    public void deleteAllSelected() {
        // Perform this operation on another thread. It will broadcast the
        // required action intent to notify interested parties after the change
        // has been made. The expected parent "TimerListFragment" is identified
        // as the source of the operation.
        TwistyTimer.getDBHandler().deleteSolvesByIDAndNotifyAsync(
            mSelectedSolveIDs, null, null);
    }

    private void toggleSelection(long id, CardView card) {
        // TODO: Have this class track the number of selections and add that to
        // the intent. There will be no need for separate actions for selection
        // mode and selected times.
        if (! mSelectedSolveIDs.contains(id)) {
            mSelectedSolveIDs.add(id);
            card.setCardBackgroundColor(mSelectedCardColor);
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SOLVE_SELECTED);
        } else {
            mSelectedSolveIDs.remove(id);
            card.setCardBackgroundColor(mCardColor);
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SOLVE_UNSELECTED);
        }

        if (mSelectedSolveIDs.size() == 0) {
            mIsInSelectionMode = false;
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_OFF);
        }
    }

    private void bindSolveToViewHolder(
            final TimeHolder holder, final Solve solve) {
        holder.card.setCardBackgroundColor(
            mSelectedSolveIDs.contains(solve.getID())
                ? mSelectedCardColor : mCardColor);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsInSelectionMode) {
                    toggleSelection(solve.getID(), holder.card);
                } else if (!isLocked()) {
                    setLocked(true);
                    EditSolveDialog timeDialog
                        = EditSolveDialog.newInstance(solve);
//                    timeDialog.setDialogListener(TimeCursorAdapter.this);
                    timeDialog.show(mFragmentManager, "time_dialog");
                }
            }
        });

        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!mIsInSelectionMode) {
                    mIsInSelectionMode = true;
                    broadcast(CATEGORY_UI_INTERACTIONS,
                              ACTION_SELECTION_MODE_ON);
                    toggleSelection(solve.getID(), holder.card);
                }
                return true;
            }
        });

        // IMPORTANT: "holder" may be recycled, so be sure to bind the sub-views
        // completely to override all fields of the solve that may previously
        // have used the same holder.
        holder.dateText.setText(
            new DateTime(solve.getDate()).toString("dd'/'MM"));
        // "time" is formatted "pretty" with smaller text for field with
        // smallest units. The time given must be already rounded (which is
        // done in "Solve.getTime()").
        holder.timeText.setText(TimeUtils.formatResultPretty(solve.getTime()));
        // Clear the strike-through flags.
        holder.timeText.setPaintFlags(
            holder.timeText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
/* FIXME!!!
        switch (solve.getPenalty()) {
            default:
            case NONE:
                holder.penaltyText.setVisibility(View.GONE);
                break;

            case DNF:
                // For a DNF, strike out the time value. This allows the user to see what the
                // elapsed time was for the DNF, which may help to find "rogue" times.
                holder.timeText.setPaintFlags(
                        holder.timeText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Set.
                // fall through
            case PLUS_TWO:
                holder.penaltyText.setText(solve.getPenalty().getDescriptionResID());
                holder.penaltyText.setVisibility(View.VISIBLE);
                break;
        }
*/
        if (solve.hasComment()) {
            holder.commentIcon.setVisibility(View.VISIBLE);
        } else {
            // This else is needed because the view recycles.
            holder.commentIcon.setVisibility(View.GONE);
        }
    }

    private boolean isLocked() {
        return mIsLocked;
    }

    private void setLocked(boolean isLocked) {
        mIsLocked = isLocked;
    }

    static class TimeHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card)        CardView       card;
        @BindView(R.id.root)        RelativeLayout root;
        @BindView(R.id.timeText)    TextView       timeText;
        @BindView(R.id.penaltyText) TextView       penaltyText;
        @BindView(R.id.date)        TextView       dateText;
        @BindView(R.id.commentIcon) ImageView      commentIcon;

        public TimeHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
