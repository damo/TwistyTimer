package com.aricneto.twistytimer.scramble;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.utils.TTIntent;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;

/**
 * <p>
 * A loader that generates scramble sequences in the background and delivers
 * them in a call-back to a component. The generated scramble sequences are
 * text strings describing the moves required to scramble a puzzle starting
 * from a solved state. The typical cubing notation is used for the sequence,
 * for example, "F2 B' D R L' U L2 [...]". The sequences will meet the
 * requirements of the WCA for scrambles in official competition, according
 * to the <i>TNoodle</i> library used to generate the scrambles for this loader.
 * </p>
 * <p>
 * This is a <i>passive</i> loader. When first created, it registers a local
 * broadcast listener and then waits until it receives a broadcast intent
 * instructing it to generate a new scramble. The loader will then begin to
 * generate a scramble on a background thread and will deliver the result to
 * the component via the {@code onLoadFinished} call-back.
 * </p>
 * <p>
 * An intent with the action {@link TTIntent#ACTION_GENERATE_SCRAMBLE} and an
 * extra that identifies the puzzle type will trigger the generation of a new
 * scramble. {@link TTIntent#broadcastNewScrambleRequest(PuzzleType)} can be
 * used as a convenience to construct the intent.
 * </p>
 * <p>
 * Once a scramble sequence has been generated, the {@link ScrambleImageLoader}
 * can be used in a similar manner to generate an image of the scrambled puzzle.
 * See the description of that class for more details.
 * </p>
 *
 * @author damo
 */
public class ScrambleLoader extends AsyncTaskLoader<ScrambleData> {
    //
    // IMPLEMENTATION NOTE
    // ===================
    //
    // The "passive" nature of this loader makes it much easier to bootstrap
    // and restart. Because it does nothing when started except to register a
    // broadcast receiver, it does not need to be passed the puzzle type in
    // its constructor and store that state. Therefore it cannot become
    // out-of-sync with the currently selected puzzle type and can easily
    // handle changes to the puzzle type.
    //
    // Passivity, and the same approach in "ScrambleImageLoader", means that
    // both loaders can be started by the "TimerFragment" without worrying
    // about the scramble and scramble image enabling preferences. Those
    // checks only need to be performed at the point where the intents are
    // broadcast.
    //
    // These loaders are (mostly) stateless. They cache a "ScrambleGenerator"
    // instance for a little efficiency and to communicate its "PuzzleType" to
    // the "loadInBackground" methods, but that cached state can be overridden
    // with each received "ACTION_GENERATE_SCRAMBLE". This allows the main state
    // of the activity/fragment to change without requiring the loaders to be
    // restarted. This makes it simpler to implement the change-of-state
    // handling in the activity and fragments, as it does not necessarily
    // require those components to be re-created.
    //
    // When "LoaderManager.initLoader()" (or "LoaderManager.restartLoader()")
    // is called from the "TimerFragment", it triggers a series of synchronous
    // calls that result in "onStartLoading" being called on this loader.
    // Therefore, once those methods have returned, this loader is guaranteed
    // to have registered its broadcast receiver and be ready to receive
    // broadcast intents. At any time after this, the "TimerFragment" can
    // broadcast its requests to generate new scrambles. The loader will
    // never "miss" a broadcast intent between the instant that "initLoader"
    // (or "restartLoader") has returned and the instant that "destroyLoader"
    // is called, both of which calls are under the control of the fragment.

    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG = ScrambleLoader.class.getSimpleName();

    /**
     * The generator that will "load" the scrambles. The generator can report
     * the puzzle type for which it is configured.
     */
    private ScrambleGenerator mScrambleGenerator;

    /**
     * The broadcast receiver that is notified when the scramble generator
     * needs to perform a new task.
     */
    private TTIntent.TTCategoryBroadcastReceiver mScrambleAlertReceiver;

    /**
     * A broadcast receiver that is notified when a new scramble is required.
     */
    private static class ScrambleAlertReceiver
                   extends TTIntent.TTCategoryBroadcastReceiver {
        /**
         * The loader to be alerted to changes.
         */
        private final ScrambleLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader when a
         * scramble is required.
         *
         * @param loader
         *     The loader to be notified.
         */
        ScrambleAlertReceiver(ScrambleLoader loader) {
            super(TTIntent.CATEGORY_SCRAMBLE_ALERTS);
            mLoader = loader;
        }

        /**
         * Receives notification of a request to generate a new scramble.
         *
         * @param context
         *     The context of the receiver.
         * @param intent
         *     The intent detailing the action that is requested.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceive: " + intent);

            if (ACTION_GENERATE_SCRAMBLE.equals(intent.getAction())) {
                if (DEBUG_ME) Log.d(TAG,
                    "  Requesting generation of new scramble!");

                TTIntent.validate(intent);

                // The puzzle type is validated as non-null by "validate()", but
                // a null-check below avoids compiler/code-inspection warnings.
                final PuzzleType puzzleType = TTIntent.getPuzzleType(intent);

                if (puzzleType != null) {
                    mLoader.generateScramble(puzzleType);
                }
            }
        }
    }

    /**
     * Creates a new scramble loader that will wait for local broadcast intent
     * to instruct it to generate scrambles.
     *
     * @param context The context for this loader.
     */
    public ScrambleLoader(@NonNull Context context) {
        super(context);
        if (DEBUG_ME) Log.d(TAG, "new ScrambleLoader()");
    }

    /**
     * Starts the generation of a new scramble sequence. If the
     * {@code newPuzzleType} is {@code null}, no action will be taken.
     *
     * @param newPuzzleType
     *     The new type of the puzzle for which a scramble is required.
     */
    private void generateScramble(@NonNull PuzzleType newPuzzleType) {
        // The goal here is to ensure that a "ScrambleGenerator" exists for the
        // correct puzzle type and then to raise "onContentChanged" to trigger
        // the generation of a new scramble. "onContentChanged" will trigger a
        // call to "onStartLoading", which will then call "forceLoad()" if it
        // detects the changed content flag is raised. "forceLoad()" will
        // automatically cancel the previous load.
        //
        // The TNoodle API has no means to cancel its tasks, but "cancelLoad()"
        // (called from "forceLoad()") will make sure that, if a cancelled task
        // finishes, its result will be discarded. That ensures a result for
        // the wrong puzzle type will not be delivered. However, any new load
        // request cannot start until TNoodle finishes its current task, as
        // "AsyncTaskLoader"/"LoaderManager" schedules tasks one-at-a-time.
        //
        // If an active scramble task is generating a scramble for the same
        // puzzle type as "newPuzzleType", this sequence could be optimised by
        // ignoring the new request and waiting for the valid result from the
        // active task to be delivered. However, the Loader API is not of much
        // help in determining if any task is active, so it would require much
        // extra code in this class to implement it.
        //
        // Repeatedly and rapidly requesting scrambles will not cause too much
        // grief. Each new request will cancel the active load and will replace
        // any other request that was already waiting on that cancellation. The
        // requests will *not* cause a backlog of tasks to build up that need
        // to be processed before things settle down.
        if (mScrambleGenerator == null
              || mScrambleGenerator.getPuzzleType() != newPuzzleType) {
            mScrambleGenerator = new ScrambleGenerator(newPuzzleType);
        } // else use existing "mScrambleGenerator", as puzzle types match.

        onContentChanged();
    }

    /**
     * Starts generating a scramble if a request has been received to do so. If
     * no request has been received, then no scramble will be generated.
     */
    @Override
    protected void onStartLoading() {
        if (DEBUG_ME) Log.d(TAG, "onStartLoading()");

        // If not already listening for alerts, start listening now.
        if (mScrambleAlertReceiver == null) {
            if (DEBUG_ME) Log.d(TAG,
                "  Monitoring scramble generation requests.");
            mScrambleAlertReceiver = new ScrambleAlertReceiver(this);
            // Register here and unregister in "onReset()".
            TTIntent.registerReceiver(mScrambleAlertReceiver);
        }

        // If the broadcast receiver has notified a "change", generate a new
        // scramble. The initial state of the content-changed flag for any
        // loader is "false", so the first time into this "onStartLoading"
        // method, just after this loader is created, nothing will be
        // generated. The loader will just sit passively and await the
        // receipt of a broadcast intent.
        if (takeContentChanged()) {
            if (DEBUG_ME) Log.d(TAG,
                "  forceLoad() to generate a new scramble....");
            forceLoad();
        }
    }

    /**
     * Generates a new scramble on a background thread.
     *
     * @return The new scramble.
     */
    @Override
    public ScrambleData loadInBackground() {
        if (DEBUG_ME) Log.d(TAG,
            "loadInBackground(): Generating new scramble....");
        final ScrambleData scrambleData;

        if (mScrambleGenerator != null) {
            // Because "DEBUG_ME" is either always "true" or always "false"....
            @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
            final long startTime
                = DEBUG_ME ? SystemClock.elapsedRealtime() : 0L;

            scrambleData = new ScrambleData(
                mScrambleGenerator.getPuzzleType(),
                mScrambleGenerator.generateScramble());

            if (DEBUG_ME) Log.d(TAG, String.format(
                "  Generated scramble in %,d ms.",
                SystemClock.elapsedRealtime() - startTime));
        } else {
            // The generator is only set to "null" in "onReset" and it is not
            // expected that this "loadInBackground" method would then be
            // called. This is suspicious, warn about it.
            Log.w(TAG, "  Unable to generate a scramble: generator is null!");
            scrambleData = null;
        }

        return scrambleData;
    }

    /**
     * Resets the loader by cancelling any background generation tasks and
     * destroying the scramble generator. This ensures that no stray result
     * will be delivered when the loader is next started, as that result may
     * not be for the expected puzzle type. The broadcast receiver is also
     * unregistered, so no further scrambles will be generated until the
     * loader is started again.
     */
    @Override
    protected void onReset() {
        if (DEBUG_ME) Log.d(TAG, "onReset()");

        cancelLoad();

        if (mScrambleAlertReceiver != null) {
            if (DEBUG_ME) Log.d(TAG,
                "  NOT monitoring scramble generation requests.");
            // Receiver will be re-registered in "onStartLoading", if that is
            // called again.
            TTIntent.unregisterReceiver(mScrambleAlertReceiver);
            mScrambleAlertReceiver = null;
        }
    }
}
