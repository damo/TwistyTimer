package com.aricneto.twistytimer.scramble;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.utils.TTIntent;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE_IMAGE;

/**
 * <p>
 * A loader that generates scramble images demonstrating the state of a puzzle when a scramble
 * sequence is applied to a solved puzzle. The loader generates images in the background and
 * delivers them to the listening component (e.g., a fragment). The scramble sequences are those
 * produced by {@link ScrambleLoader}.
 * </p>
 * <p>
 * This is a <i>passive</i> loader. When first created, it registers a local broadcast listener and
 * then waits until it receives a broadcast intent instructing it to generate a new scramble image.
 * The loader will then begin to generate a scramble on a background thread and will deliver the
 * result to the component via the {@code onLoadFinished} call-back.
 * </p>
 * <p>
 * An intent with the action {@link TTIntent#ACTION_GENERATE_SCRAMBLE_IMAGE} and extras that
 * identify the puzzle type and scramble sequence will trigger the generation of a new scramble.
 * {@link TTIntent#broadcastNewScrambleImageRequest(PuzzleType, String)} should be used to
 * construct the intent.
 * </p>
 *
 * @author damo
 */
public class ScrambleImageLoader extends AsyncTaskLoader<ScrambleImageData> {
    // NOTE: See the "IMPLEMENTATION NOTE" in "ScrambleLoader"; the same approach is used here.

    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG = ScrambleImageLoader.class.getSimpleName();

    /**
     * The generator that will "load" the scramble images.
     */
    private ScrambleGenerator mScrambleImageGenerator;

    /**
     * The scramble sequence for which the next image will be generated.
     */
    private String mNextScramble;

    /**
     * The broadcast receiver that is notified when the scramble image generator needs to perform
     * a new task.
     */
    private TTIntent.TTCategoryBroadcastReceiver mScrambleAlertReceiver;

    /**
     * A broadcast receiver that is notified when a new scramble image is required.
     */
    private static class ScrambleAlertReceiver extends TTIntent.TTCategoryBroadcastReceiver {
        /**
         * The loader to be alerted to changes.
         */
        private final ScrambleImageLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader when a scramble image is
         * required.
         *
         * @param loader The loader to be notified.
         */
        ScrambleAlertReceiver(ScrambleImageLoader loader) {
            super(TTIntent.CATEGORY_SCRAMBLE_ALERTS);
            mLoader = loader;
        }

        /**
         * Receives notification of a request to generate a new scramble image.
         *
         * @param context The context of the receiver.
         * @param intent  The intent detailing the action that is requested.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceive: " + intent);

            if (ACTION_GENERATE_SCRAMBLE_IMAGE.equals(intent.getAction())) {
                if (DEBUG_ME) Log.d(TAG, "  Requesting generation of new scramble image!");
                mLoader.generateScrambleImage(
                        TTIntent.getPuzzleType(intent), TTIntent.getScramble(intent));
            }
        }
    }

    /**
     * Creates a new scramble image loader that will wait for local broadcast intent to instruct
     * it to generate scramble images.
     *
     * @param context The context for this loader.
     */
    public ScrambleImageLoader(@NonNull Context context) {
        super(context);
        if (DEBUG_ME) Log.d(TAG, "new ScrambleImageLoader()");
    }

    /**
     * Starts the generation of a new scramble image from a scramble sequence. If either parameter
     * is {@code null}, no action will be taken.
     *
     * @param newPuzzleType
     *     The new type of the puzzle for which a scramble image is required.
     * @param newScramble
     *     The new scramble sequence to be represented in the image.
     */
    private void generateScrambleImage(PuzzleType newPuzzleType, String newScramble) {
        if (newPuzzleType != null && newScramble != null) {
            if (mScrambleImageGenerator == null
                    || mScrambleImageGenerator.getPuzzleType() != newPuzzleType) {
                mScrambleImageGenerator = new ScrambleGenerator(newPuzzleType);
            } // else use the existing "mScrambleImageGenerator", as the puzzle types match.

            if (DEBUG_ME) Log.d(TAG, "Accepted scramble image generation request for: "
                    + newPuzzleType.typeName());

            mNextScramble = newScramble;
            onContentChanged();
        } else {
            // The broadcaster of the intent forgot to set the puzzle type and/or scramble.
            Log.e(TAG, "BUG! Request for new scramble image has no puzzle type or scramble.");
        }
    }

    /**
     * Starts generating a scramble image if a request has been received to do so. If no request
     * has been received, then no scramble image will be generated.
     */
    @Override
    protected void onStartLoading() {
        if (DEBUG_ME) Log.d(TAG, "onStartLoading()");

        // If not already listening for alerts, start listening now.
        if (mScrambleAlertReceiver == null) {
            if (DEBUG_ME) Log.d(TAG, "  Monitoring scramble image generation requests.");
            mScrambleAlertReceiver = new ScrambleAlertReceiver(this);
            // Register here and unregister in "onReset()".
            TTIntent.registerReceiver(mScrambleAlertReceiver);
        }

        // If any pertinent change was detected by the receiver, or if no scramble has been
        // generated, then generate a new scramble now.
        if (takeContentChanged()) {
            if (mNextScramble != null) {
                if (DEBUG_ME) Log.d(TAG, "  forceLoad() to generate a new scramble image....");
                forceLoad();
            } else {
                // This is not expected to happen!
                Log.e(TAG, "Expected a scramble for a new scramble image, but it was missing!");
                commitContentChanged(); // Swallow this change request.
            }
        }
    }

    /**
     * Generates a new scramble image on a background thread.
     *
     * @return The new scramble image.
     */
    @Override
    public ScrambleImageData loadInBackground() {
        if (DEBUG_ME) Log.d(TAG, "loadInBackground(): Generating new scramble image....");
        final ScrambleImageData scrambleImageData;

        if (mScrambleImageGenerator != null && mNextScramble != null) {
            // Because "DEBUG_ME" is either always "true" or always "false"....
            @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
            final long startTime = DEBUG_ME ? SystemClock.elapsedRealtime() : 0L;

            scrambleImageData = new ScrambleImageData(
                    mScrambleImageGenerator.getPuzzleType(), mNextScramble,
                    mScrambleImageGenerator.generateImageFromScramble(mNextScramble));

            if (DEBUG_ME) Log.d(TAG, String.format("  Generated scramble image in %,d ms.",
                    SystemClock.elapsedRealtime() - startTime));
        } else {
            // The generator and scramble are only set to "null" in "onReset" and it is not
            // expected that this "loadInBackground" method would then be called. This is
            // suspicious, warn about it.
            Log.w(TAG, "  Unable to generate a scramble image: generator or scramble are null!");
            scrambleImageData = null;
        }

        return scrambleImageData;
    }

    /**
     * Resets the loader by cancelling any background generation tasks and destroying the scramble
     * image generator. This ensures that no stray result will be delivered when the loader is next
     * started, as that result may not be for the expected puzzle type or scramble. The broadcast
     * receiver is also unregistered, so no further scramble images will be generated until the
     * loader is started again.
     */
    @Override
    protected void onReset() {
        if (DEBUG_ME) Log.d(TAG, "onReset()");

        cancelLoad();
        mNextScramble = null;

        if (mScrambleAlertReceiver != null) {
            if (DEBUG_ME) Log.d(TAG, "  NOT monitoring scramble image generation requests.");
            // Receiver will be re-registered in "onStartLoading", if that is called again.
            TTIntent.unregisterReceiver(mScrambleAlertReceiver);
            mScrambleAlertReceiver = null;
        }
    }
}
