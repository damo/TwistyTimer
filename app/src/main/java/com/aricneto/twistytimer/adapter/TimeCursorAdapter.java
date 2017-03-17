package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.StrikeoutTextView;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.aricneto.twistytimer.utils.TimeUtils;

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.HashSet;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.aricneto.twistytimer.utils.TTIntent
    .ACTION_SOLVES_SELECTION_CHANGED;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;

/**
 * A cursor adapter to support the presentation of the solve times in the
 * {@link TimerListFragment}. This cursor adapter handles the binding between
 * the data and the "time cards" displayed in the list/grid of selected solve
 * times. When a time-card is clicked.
 */
public class TimeCursorAdapter
        extends CursorRecyclerAdapter<RecyclerView.ViewHolder> {
    private final FragmentManager mFragmentManager;

    private int mCardColor;
    private int mSelectedCardColor;

    // A Set will be more efficient than a List, as "contains(Long)" is used
    // a lot.
    private Collection<Long> mSelectedSolveIDs = new HashSet<>();

    public TimeCursorAdapter(Context context, Cursor cursor,
                             TimerListFragment listFragment) {
        super(cursor);
        mFragmentManager = listFragment.getFragmentManager();
        mCardColor = ThemeUtils.fetchAttrColor(
            context, R.attr.colorItemListBackground);
        mSelectedCardColor = ThemeUtils.fetchAttrColor(
            context, R.attr.colorItemListBackgroundSelected);

        // Indicate that each solve record has a unique ID. This is needed for
        // "RecyclerView.findViewHolderForItemId()" to work in "unselectAll()".
        setHasStableIds(true);
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        super.swapCursor(cursor);

        // The data has changed, so "bindSolveToViewHolder()" will set up each
        // view holder afresh and there is no need to restore background colors.
        mSelectedSolveIDs.clear();
        broadcastSelectionChanged();

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

    public void deleteAllSelected() {
        // Perform this operation on another thread. It will broadcast the
        // required action intent to notify interested parties after the change
        // has been made. The expected parent "TimerListFragment" is identified
        // as the source of the operation.
        TwistyTimer.getDBHandler().deleteSolvesByIDAndNotifyAsync(
            mSelectedSolveIDs, null, null);
    }

    public void unselectAll(RecyclerView recyclerView) {
        for (long solveID : mSelectedSolveIDs) {
            final TimeHolder holder
                = (TimeHolder) recyclerView.findViewHolderForItemId(solveID);

            // A selected solve that is not visible in the viewport might not
            // be bound to any view holder, so check first.
            if (holder != null) {
                holder.card.setCardBackgroundColor(mCardColor);
            }
        }

        mSelectedSolveIDs.clear();
        broadcastSelectionChanged();
    }

    private void toggleSelection(long id, CardView card) {
        if (! mSelectedSolveIDs.contains(id)) {
            mSelectedSolveIDs.add(id);
            card.setCardBackgroundColor(mSelectedCardColor);
        } else {
            mSelectedSolveIDs.remove(id);
            card.setCardBackgroundColor(mCardColor);
        }

        broadcastSelectionChanged();
    }

    private void broadcastSelectionChanged() {
        TTIntent.builder(
            CATEGORY_UI_INTERACTIONS, ACTION_SOLVES_SELECTION_CHANGED)
                .selectionCount(mSelectedSolveIDs.size())
                .broadcast();
    }

    private void bindSolveToViewHolder(
            final TimeHolder holder, final Solve solve) {
        holder.card.setCardBackgroundColor(
            mSelectedSolveIDs.contains(solve.getID())
                ? mSelectedCardColor : mCardColor);

        // Normally, clicking a solve time view will open the dialog, but a
        // long-click will *select* that solve time and then further (normal)
        // clicks will add/remove (toggle) solves from that selection. Once
        // the long-click selects the first solve, long-clicking will have no
        // effect until the selection count reaches zero again. Once back at
        // zero, clicks will open the dialog again until the next long-click.
        //
        // Changes to the selection are broadcast (to "TimerMainFragment") and
        // it maintains an action mode in its title bar that shows the count.
        // If that action mode is exited directly via its back button, it will
        // broadcast "ACTION_CLEAR_SELECTED_SOLVES" back to "TimerListFragment",
        // which will notify this adapter to clear the selection.

        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mSelectedSolveIDs.size() == 0) {
                    toggleSelection(solve.getID(), holder.card);
                }
                return true;
            }
        });

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("TimeCursorAdapter", "Time record clicked!");
                Log.d("TimeCursorAdapter",
                    "  selection count=" + mSelectedSolveIDs.size());

                if (mSelectedSolveIDs.size() > 0) {
                    Log.d("TimeCursorAdapter", "  Toggling selection...");
                    toggleSelection(solve.getID(), holder.card);
                } else {
                    Log.d("TimeCursorAdapter", "  Opening edit dialog...");
                    EditSolveDialog.newInstance(solve)
                                   .show(mFragmentManager, "time_dialog");
                }
            }
        });

        // IMPORTANT: "holder" may be recycled, so be sure to bind values to
        // *all* of the sub-views to override all values from any other solve
        // that may previously have used the same holder.
        holder.dateText.setText(
            new DateTime(solve.getDate()).toString("dd'/'MM"));
        // "time" is formatted "pretty" with smaller text for field with
        // smallest units. The time given must be already rounded (which is
        // done in "Solve.getTime()").
        holder.timeText.setText(
            TimeUtils.prettyFormatResultTime(solve.getTime()));
        // Clear the strike-out (checked) state.
        holder.timeText.setChecked(false);

        // If there are penalties, format them compactly to fit the small amount
        // of space available. Merge the pre- and post-start penalty counts and
        // show the total penalty time as a single value (e.g., "DNF+8s").
        final Penalties penalties = solve.getPenalties();

        if (penalties.hasPenalties()) {
            final StringBuilder s = new StringBuilder();

            if (penalties.hasDNF()) {
                s.append(Penalty.DNF.getDescription());

                // For a DNF, strike out the *time* value. This allows the user
                // to see what the elapsed time was for the DNF, which may help
                // to find "rogue" times. For example, if a total time played is
                // shown in the stats and DNF solve attempts are included, then
                // a stray DNF result of, say, "27:34:12.18" could be found and
                // deleted to avoid it skewing the total time.
                holder.timeText.setChecked(true);
            }

            final int timePenalties
                = (int) penalties.getTimePenalty() / 1_000; // Convert ms to s.

            if (timePenalties > 0) {
                s.append('+').append(timePenalties).append('s');
            }

            holder.penaltyText.setText(s);
            holder.penaltyText.setVisibility(View.VISIBLE);
        } else {
            holder.penaltyText.setVisibility(View.GONE);
        }

        if (solve.hasComment()) {
            holder.commentIcon.setVisibility(View.VISIBLE);
        } else {
            // This else is needed because the view recycles.
            holder.commentIcon.setVisibility(View.GONE);
        }
    }

    static class TimeHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card)        CardView          card;
        @BindView(R.id.root)        RelativeLayout    root;
        @BindView(R.id.timeText)    StrikeoutTextView timeText;
        @BindView(R.id.penaltyText) TextView          penaltyText;
        @BindView(R.id.date)        TextView          dateText;
        @BindView(R.id.commentIcon) ImageView         commentIcon;

        TimeHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
