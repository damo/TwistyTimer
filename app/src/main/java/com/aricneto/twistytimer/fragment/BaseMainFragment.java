package com.aricneto.twistytimer.fragment;

import android.app.Activity;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.items.PuzzleType;

/**
 * <p>
 * A base class for fragments under the {@link MainActivity}. This helps to
 * manage the access to the activity's {@link MainState} and notification
 * about changes to that state. The fragments should not keep a copy of the
 * main state, as that could lead to confusion and problems with state being
 * inconsistent across fragments. Instead, fragments should access the state
 * through {@link #getMainState()}. This must only be called when the
 * fragment is added to its parent activity, otherwise the state will not be
 * available.
 * </p>
 * <p>
 * If a fragment provides the user interface that allows changes to be made to
 * the main state, that fragment should call
 * {@link #fireOnMainStateChanged(MainState)} to report any change to the
 * {@code MainActivity}. The change will be notified to all relevant fragments
 * with a call back to {@link #onMainStateChanged(MainState, MainState)}. The
 * fragment calling the former method should wait until it is notified via the
 * latter method before applying the changes to its own state.
 * </p>
 * <p>
 * The main state is not passed to the fragments as a fragment argument. This
 * would set the initial state, but it might cause confusion later, as it
 * would be out-of-date if the state changed while the fragment was running.
 * Arguments are available once the fragment is instantiated, but the main
 * state from the activity can only be accessed while the fragment is
 * attached. It is safer to provide access only through the activity, so that
 * there is no temptation to access an out-of-date main state argument when
 * the activity is not available. If there is code that requires such access,
 * the could should be moved to a point in the life-cycle when the activity
 * is attached.
 * </p>
 */
public class BaseMainFragment extends Fragment {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" to identify this class in log messages. The class name of the
     * instance (i.e., the subclass name) is used for clarity.
     */
    // Use it like any *static* TAG field.
    private final String TAG = getClass().getSimpleName();

    /**
     * Gets the current main state information maintained by the
     * {@link MainActivity} for its fragments.
     *
     * @return
     *     The main state information.
     *
     * @throws IllegalStateException
     *     If this fragment is not currently attached to the
     *     {@code MainActivity}, or if the activity has yet to initialise its
     *     own main state.
     */
    @NonNull
    public MainState getMainState() {
        MainState mainState = null;

        if (isAdded() && getActivity() instanceof MainActivity) {
            mainState = ((MainActivity) getActivity()).getMainState();
        }

        if (mainState == null) {
            // *Only* attempt to access the main state when attached to an
            // activity. If the activity returns a null state, then the bug
            // is in the activity.
            throw new IllegalStateException(
                "BUG! Attempt to access MainState activity is detached.");
        }

        return mainState;
    }

    /**
     * Notifies the activity that the main state has been changed through a
     * fragment. Typically, the fragment that provides a user interface to
     * change the main state should call this method when a change is
     * detected. The activity will update its state an then notify all
     * attached fragments though a call-back to
     * {@link #onMainStateChanged(MainState, MainState)}. The fragment that
     * calls this method should wait for that call-back before taking any
     * action itself. If the activity detects no change in the main state, it
     * will not trigger any call-backs.
     *
     * @param newMainState
     *     The new main state.
     *
     * @throws IllegalStateException
     *     If this fragment is not currently attached to the
     *     {@link MainActivity}, or if the activity has yet to initialise its
     *     own main state.
     */
    public void fireOnMainStateChanged(
            @NonNull MainState newMainState) {
        if (DEBUG_ME) Log.d(TAG,
            "fireOnMainStateChanged(" + newMainState + ')');

        if (isAdded() && getActivity() instanceof MainActivity) {
            // The activity will not "return fire" to each fragments'
            // "onMainStateChanged" method if the new main state is the same
            // as the old main state.
            ((MainActivity) getActivity()).fireOnMainStateChanged(newMainState);
        } else {
            // *Only* attempt to access the main state when attached to an
            // activity.
            throw new IllegalStateException(
                "BUG! Notifying MainState change when activity is detached.");
        }
    }

    /**
     * <p>
     * Notifies the activity that some main state has been changed piecemeal
     * through a fragment. This methods constructs a new main state by taking
     * the current main state and changing only those fields that are given
     * to this method as non-{@code null} values. The result is handled as by
     * {@link #fireOnMainStateChanged(MainState)} for more details.
     * </p>
     * <p>
     * If the current puzzle type is changed, then the current solve category
     * may also need to change. Solve categories are specific to a puzzle type,
     * so a valid solve category must be identified when the puzzle type is
     * changed. The caller of this method is not expected to determine the new
     * solve category, so, if {@code newPuzzleType} is non-{@code null}, then
     * the {@code newSolveCategory} must be given as {@code null} to allow this
     * method to find a new solve category that is valid for the new puzzle
     * type. The new solve category will be the last-used solve category for
     * that puzzle type, or the default solve category (either of which will
     * be created in the database, if necessary).
     * </p>
     *
     * @param newPuzzleType
     *     The new value for the puzzle type, or {@code null} if unchanged.
     * @param newSolveCategory
     *     The new value for the solve category, or {@code null} if unchanged.
     *     Must be {@code null} if {@code newPuzzleType} is non-{@code null}.
     *     The new solve category will be set automatically, in that case.
     * @param newIsHistoryEnabled
     *     The new value for the history flag, or {@code null} if unchanged.
     *
     * @throws IllegalStateException
     *     If this fragment is not currently attached to the
     *     {@link MainActivity}, or if the activity has yet to initialise its
     *     own main state.
     * @throws IllegalArgumentException
     *     If the new puzzle type and the new solve category are both
     *     non-{@code null}. If the puzzle type is non-{@code null}, then the
     *     solve category must be {@code null}.
     */
    // NOTE: "CharSequence" for solve category makes use in input dialogs a bit
    // easier.
    public void fireOnMainStateChangedPiecemeal(
            @Nullable final PuzzleType newPuzzleType,
            @Nullable final CharSequence newSolveCategory,
            @Nullable final Boolean newIsHistoryEnabled) {

        if (DEBUG_ME) Log.d(TAG,
            "fireOnMainStateChangedPiecemeal(newPuzzleType="
            + newPuzzleType + ", newSolveCategory=" + newSolveCategory
            + ", newIsHistoryEnabled=" + newIsHistoryEnabled + ')');

        final CharSequence newValidSolveCategory;

        if (newPuzzleType != null) {
            if (newSolveCategory == null) {
                // "Prefs.getLastUsedSolveCategory" *guarantees* that, if the
                // category it returns does not exist in the database (i.e.,
                // does not have a "fake" solve record to hold the category
                // name), that the category will be created "real soon now".
                newValidSolveCategory
                    = Prefs.getLastUsedSolveCategory(newPuzzleType);
            } else {
                throw new IllegalArgumentException(
                    "Solve category ('" + newSolveCategory
                    + "') must be null if puzzle type ('" + newPuzzleType
                    +  "') is non-null.");
            }
        } else {
            newValidSolveCategory = newSolveCategory;
        }

        // Ensures attached and state not null.
        final MainState oldMainState = getMainState();

        fireOnMainStateChanged(
            new MainState(
                newPuzzleType != null
                    ? newPuzzleType : oldMainState.getPuzzleType(),
                newValidSolveCategory != null
                    ? newValidSolveCategory : oldMainState.getSolveCategory(),
                newIsHistoryEnabled != null
                    ? newIsHistoryEnabled : oldMainState.isHistoryEnabled()));
    }

    /**
     * Notifies the fragment that the main state maintained by the activity
     * has been changed. The default behaviour is to do nothing (except log
     * the event if debugging). Fragments should override this method if a
     * change in main state affect the fragment (and call the super method if
     * logging is to be retained). This method will only be called if there
     * is a change, so the new and old state will not be equal. However, a
     * fragment may elect to take no action if the only elements of the main
     * state that have changed do not affect the fragment.
     *
     * @param newMainState The new main state.
     * @param oldMainState The old main state.
     */
    public void onMainStateChanged(
            @NonNull MainState newMainState, @NonNull MainState oldMainState) {
        if (DEBUG_ME) Log.d(TAG, "onMainStateChanged(new=" + newMainState
                + ", old=" + oldMainState + ')');
        // Do nothing (else) unless overridden.
    }

    protected int getActionBarSize() {
        Activity activity = getActivity();
        if (activity == null) {
            return 0;
        }

        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = activity.obtainStyledAttributes(
            typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

    /**
     * This function should be called in every fragment that needs a toolbar
     * Every fragment has its own toolbar, and this function executes the
     * necessary steps to ensure the toolbar is correctly bound to the main
     * activity, which handles the rest (drawer and options menu)
     * <p/>
     * Also, a warning: always bind the toolbar title BEFORE calling this
     * function otherwise, it won't work.
     *
     * @param toolbar The toolbar present in the fragment
     */
    protected void setUpToolbarForFragment(Toolbar toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        getMainActivity().setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().openDrawer();
            }
        });
    }

    protected MainActivity getMainActivity() {
        return ((MainActivity) getActivity());
    }
}
