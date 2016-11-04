package com.aricneto.twistytimer.database;

import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import com.aricneto.twistytimer.TwistyTimer;

public class AlgTaskLoader extends CursorLoader {
    private final String subset;

    public AlgTaskLoader(String subset) {
        super(TwistyTimer.getAppContext());
        this.subset = subset;
    }

    @Override
    public Cursor loadInBackground() {
        return TwistyTimer.getDBHandler().getAllAlgorithmsForSubset(subset);
    }
}
