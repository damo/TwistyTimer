package com.aricneto.twistytimer.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A simple executor for "fire-and-forget" background tasks. This is useful for tasks that should
 * not run on the main UI thread and where no result is required and no errors are expected (or
 * cared about). Only a single thread of execution is maintained; tasks will be queued if necessary
 * and will be executed sequentially in the order they are submitted for execution.
 *
 * @author damo
 */
public final class FireAndForgetExecutor {
    // NOTE: A special "Runnable"-like class or interface could be supported here. For example,
    // it could run a background task and then run a foreground task on completion, or run one
    // foreground task on success and a different foreground task on failure. However, such an
    // elaboration of this intentionally simple fire-and-forget API would just be a reinvention
    // of "AsyncTask", which can do all that already and should probably be used instead.

    /**
     * The executor for the tasks.
     */
    private static final Executor TASK_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FireAndForgetExecutor() {
    }

    /**
     * <p>
     * Executes a {@code Runnable} task on a background thread as soon as the executor thread
     * becomes idle. Tasks will be queued for execution if the thread is currently busy. If
     * multiple tasks are submitted for execution by this method, they will be executed in the
     * order in which they are submitted.
     * </p>
     * <p>
     * <i>The task is not executed on the main UI thread. Therefore, care should be taken if the
     * UI needs to be updated at the end of the task.</i> If the task is complex, consider using
     * {@code android.os.AsyncTask} instead. Otherwise, {@link #executeOnMainThread(Runnable)} may
     * be used as a convenience to execute a runnable on the main UI thread for simpler use cases.
     * </p>
     * <p>
     * If using a {@code LocalBroadcastManager} (LBR) to broadcast an intent from the task, the
     * LBR will ensure that all registered broadcast receivers will receive the broadcast on the
     * main UI thread, so no special action is required from the {@code bgTask} to ensure this.
     * </p>
     *
     * @param bgTask The task to be executed on a background thread.
     */
    public static void execute(Runnable bgTask) {
        TASK_EXECUTOR.execute(bgTask);
    }

    /**
     * Executes a {@code Runnable} task on the main UI thread. This is a convenience that may be
     * useful for the {@code bgTask} passed to {@link #execute(Runnable)} if it needs to update the
     * user interface, or otherwise perform some action in the context of the main UI thread, when
     * its background operations are complete. If the use case gets complicated, consider using
     * {@code android.os.AsyncTask} instead.
     *
     * @param fgTask The task to be executed on the foreground (main UI) thread.
     */
    public static void executeOnMainThread(Runnable fgTask) {
        new Handler(Looper.getMainLooper()).post(fgTask);
    }
}
