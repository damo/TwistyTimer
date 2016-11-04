package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.PuzzleType;

/**
 * An adapter for listing the values of the {@link PuzzleType} {@code enum} in a {@code Spinner}
 * view. The layouts are expected to contain a {@code android.widget.TextView} with the view ID
 * {@code android.R.id.text1}.
 */
public class PuzzleTypeSpinnerAdapter extends BaseAdapter {
    /**
     * The context for this adapter. This is required when inflating layouts and resolving string
     * resources.
     */
    private final Context mContext;

    /**
     * The layout resource ID for the layout used to present the selected item when the spinner is
     * collapsed.
     */
    @LayoutRes
    private final int mResource; // Named like in Android's "ArrayAdapter"

    /**
     * The layout resource ID for the layout used to present an item in the drop-down list shown
     * when the spinner is expanded.
     */
    @LayoutRes
    private final int mDropDownResource; // Named like in Android's "ArrayAdapter"

    /**
     * Indicates if an arrow drawable should be added to the text view when the spinner is
     * collapsed. If not shown, it is expected that the spinner style or layout will include the
     * necessary visual cue.
     */
    private final boolean mShowDropDownArrow;

    /**
     * Creates a new adapter for presenting a list of puzzle types. Use one of the factory methods
     * to ensure the layouts are properly chosen for the context in which the spinner will be used.
     *
     * @param context The context for this adapter.
     * @param resource
     *     The layout resource ID for the layout that will present the selected item in the spinner
     *     when the spinner is collapsed.
     * @param dropDownResource
     *     The layout resource ID for the layout that will present each item in the drop-down list
     *     of the spinner when the spinner is expanded. May be the same as {@code resource}.
     */
    private PuzzleTypeSpinnerAdapter(
            @NonNull Context context, @LayoutRes int resource, @LayoutRes int dropDownResource,
            boolean showDropDownArrow) {
        mContext = context;
        mResource = resource;
        mDropDownResource = dropDownResource;
        mShowDropDownArrow = showDropDownArrow;
    }

    /**
     * Creates a spinner adapter for the display of the list of puzzle types that is suitable for
     * use in the action bar at the top of the timer screen.
     *
     * @param context The context required to create the adapter.
     * @return The puzzle type adapter.
     */
    public static PuzzleTypeSpinnerAdapter createForActionBar(@NonNull Context context) {
        return new PuzzleTypeSpinnerAdapter(
                context, R.layout.toolbar_spinner_item_actionbar,
                R.layout.toolbar_spinner_item_dropdown, true);
    }

    /**
     * Creates a spinner adapter for the display of the list of puzzle types that is suitable for
     * use in dialog used to choose the puzzle type when importing or exporting solve times.
     *
     * @param context The context required to create the adapter.
     * @return The puzzle type adapter.
     */
    public static PuzzleTypeSpinnerAdapter createForChooser(@NonNull Context context) {
        return new PuzzleTypeSpinnerAdapter(
                context, android.R.layout.simple_spinner_dropdown_item,
                android.R.layout.simple_spinner_dropdown_item, false);
    }

    /**
     * Gets the count of puzzle types.
     *
     * @return The number of puzzle types.
     */
    @Override
    public int getCount() {
        return PuzzleType.size();
    }

    /**
     * Gets the puzzle type at the given position. {@link #getPuzzleType(int)} will likely be more
     * convenient, as it will not require a type cast.
     *
     * @param position The position for which to get the puzzle type.
     * @return The puzzle type in that position.
     */
    @Override
    public Object getItem(int position) {
        return PuzzleType.forOrdinal(position);
    }

    /**
     * Gets the puzzle type at the given position. This may be more convenient than calling
     * {@link #getItem(int)}, as the return value does not need to be cast from an {@code Object}.
     *
     * @param position The position for which to get the puzzle type.
     * @return The puzzle type in that position.
     */
    public PuzzleType getPuzzleType(int position) {
        return PuzzleType.forOrdinal(position);
    }

    /**
     * Gets the notional ID of the item at the given position. Puzzle types do not have an ID, so
     * the given value is simply returned.
     *
     * @param position The position for which to get the ID.
     * @return The given {@code position} value as a surrogate for an ID.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }
    /**
     * Creates a view to display the selected puzzle type when the spinner is collapsed.
     *
     * @param position The position of the selected puzzle type in the list.
     * @param view     A view that may be recycled to present the puzzle type.
     * @param parent   The parent to use if creating a new view.
     * @return The view to show the selected puzzle type.
     */
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null || ! view.getTag().toString().equals("NON_DROPDOWN")) {
            view = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            view.setTag("NON_DROPDOWN");
        }

        setFullName(view, position);
        addDropDownArrow(view);

        return view;
    }

    /**
     * Creates a view to display one puzzle type in the drop-down list shown when the spinner is
     * expanded.
     *
     * @param position The position of the puzzle type in the list.
     * @param view     A view that may be recycled to present the puzzle type.
     * @param parent   The parent to use if creating a new view.
     * @return The view to show the puzzle type in the drop-down list.
     */
    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        if (view == null || ! view.getTag().toString().equals("DROPDOWN")) {
            view = LayoutInflater.from(mContext).inflate(mDropDownResource, parent, false);
            view.setTag("DROPDOWN");
        }

        setFullName(view, position);

        return view;
    }

    /**
     * Sets the full, human-readable name of the puzzle type at the given position on the given
     * view. The view is expected to be, or to contain, a {@code TextView} with the view ID
     * {@code android.R.id.text1}.
     *
     * @param view     The text view, or a parent of the text view.
     * @param position The position of the puzzle type.
     */
    private void setFullName(View view, int position) {
        ((TextView) view.findViewById(android.R.id.text1))
                .setText(PuzzleType.forOrdinal(position).getFullName());
    }

    /**
     * Adds a drop-down arrow to the right of the selected puzzle type name. The arrow is only
     * added if it is enabled in the configuration of this adapter. The view is expected to be,
     * or expected to contain, a {@code TextView} with the view ID {@code android.R.id.text1}.
     *
     * @param view The text view, or a parent of the text view.
     */
    private void addDropDownArrow(View view) {
        if (mShowDropDownArrow) {
            ((TextView) view.findViewById(android.R.id.text1))
                    .setCompoundDrawablesWithIntrinsicBounds(
                            null, null,
                            ContextCompat.getDrawable(
                                    mContext, R.drawable.ic_action_arrow_drop_down_white_24),
                            null);
        }
    }
}
