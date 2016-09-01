package com.aricneto.twistytimer.utils;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.aricneto.twistytimer.items.Solve;

/**
 * Stores the state of a component (an activity or a fragment). This provides a convenient class
 * that implements the {@code android.os.Parcelable} interface to make it easy to use as the basis
 * for saving and restoring the state of activities or fragments. The state information supported
 * is a union of the state information required for all fragments. Where state information is not
 * applicable to a fragment, it can be ignored.
 *
 * @author damo
 */
public class ComponentState implements Parcelable {

    // NOTE: Use the fully-qualified name to make accidental clashes with other keys very unlikely.
    private static final String COMPONENT_STATE_KEY = ComponentState.class.getName();

    /**
     * The name of the puzzle type.
     */
    public String puzzleType;

    /**
     * The name of the puzzle subtype.
     */
    public String puzzleSubtype;

    /**
     * {@code true} if the full history of all solve times should be displayed, or {@code false} if
     * only the times for the current session should be displayed.
     */
    public boolean isHistoryEnabled;

    /**
     * The most recently completed solve of the current session, if any.
     */
    public Solve solve;

    /**
     * The currently displayed scramble. This may be a scramble generated after the latest solve,
     * so it is not necessarily the same as any scramble recorded in {@link #solve}.
     */
    public String scramble;

    public ComponentState() {
    }

    public ComponentState(String puzzleType, String puzzleSubtype, boolean isHistoryEnabled,
                          Solve solve, String scramble) {
        this.puzzleType = puzzleType;
        this.puzzleSubtype = puzzleSubtype;
        this.isHistoryEnabled = isHistoryEnabled;
        this.solve = solve;
        this.scramble = scramble;
    }

    protected ComponentState(Parcel in) {
        final ClassLoader cl = Solve.class.getClassLoader();

        // Use "readValue", not "readString" or "readParcelable", to allow that values may be null.
        puzzleType = (String) in.readValue(cl);
        puzzleSubtype = (String) in.readValue(cl);
        isHistoryEnabled = in.readByte() != 0;
        solve = (Solve) in.readValue(cl);
        scramble = (String) in.readValue(cl);
    }

    public void saveTo(Bundle bundle) {
        bundle.putParcelable(COMPONENT_STATE_KEY, this);
    }

    public static ComponentState restoreFrom(Bundle bundle) {
        if (bundle != null && bundle.containsKey(COMPONENT_STATE_KEY)) {
            return (ComponentState) bundle.getParcelable(COMPONENT_STATE_KEY);
        }
        return null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Use "writeValue" to allow that values to be written may be null.
        dest.writeValue(puzzleType);
        dest.writeValue(puzzleSubtype);
        dest.writeByte((byte) (isHistoryEnabled ? 1 : 0));
        dest.writeValue(solve);
        dest.writeValue(scramble);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ComponentState> CREATOR = new Creator<ComponentState>() {
        @Override
        public ComponentState createFromParcel(Parcel in) {
            return new ComponentState(in);
        }

        @Override
        public ComponentState[] newArray(int size) {
            return new ComponentState[size];
        }
    };
}
