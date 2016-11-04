package com.aricneto.twistytimer.utils;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

/**
 * A simple base implementation of {@code LoaderManager.LoaderCallbacks} that logs messages about
 * each method call. This is useful when debugging loaders. Just call the super-class method from
 * each method overridden to emit the log message. The call to the super-class method can be
 * guarded with a condition that tests if logging is enabled.
 *
 * @param <D> The type of the data being loaded.
 *
 * @author damo
 */
public abstract class LoggingLoaderCallbacks<D> implements LoaderManager.LoaderCallbacks<D> {
    /**
     * The log tag for the class for which the logging will be performed.
     */
    @NonNull
    private final String mTag;

    /**
     * A name to identify the loader making the call-backs.
     */
    @NonNull
    private final String mLoaderName;

    /**
     * Creates a new loader call-back receiver that will log all methods called.
     *
     * @param tag
     *     The log tag for the class for which the logging will be performed.
     * @param loaderName
     *     A name to identify the loader making the call-backs.
     */
    public LoggingLoaderCallbacks(@NonNull String tag, @NonNull String loaderName) {
        mTag = tag;
        mLoaderName = loaderName;
    }

    @Override
    public Loader<D> onCreateLoader(int id, Bundle args) {
        Log.d(mTag, mLoaderName + " -> onCreateLoader(id=" + id + ", args=" + args + ')');
        return null;
    }

    @Override
    public void onLoadFinished(Loader<D> loader, D data) {
        Log.d(mTag, mLoaderName + " -> onLoadFinished(loader=" + loader + ", data=" + data + ')');
    }

    @Override
    public void onLoaderReset(Loader<D> loader) {
        Log.d(mTag, mLoaderName + " -> onLoadFinished(loader=" + loader + ')');
    }
}
