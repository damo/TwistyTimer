package com.aricneto.twistytimer.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.items.Penalties;
import com.aricneto.twistytimer.items.Penalty;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.stats.ChartStatistics;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.FireAndForgetExecutor;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.TimeUtils;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;

/**
 * The main database manager with helper methods for performing high-level database operations.
 * Most (or all) of these methods should not be called from the main UI thread; they should be
 * called from background threads instead. For example, from {@code AsyncTask}s or {@code Loader}s.
 */
// Suppress suggestions from the IDE to use the Java 7 "try-with-resources" language feature. It
// requires API 19 to work on Android, but, at the time of writing, this app supports API 16+.
@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DatabaseHandler extends SQLiteOpenHelper {
    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = DatabaseHandler.class.getSimpleName();

    /**
     * The ID column common to all tables. This special column, distinct from the SQLite in-built
     * "ROWID" column, is required if using Android content providers or cursor adapters.
     */
    private static final String COL_ID = "_id";

    // Table containing all solve times.
    //
    // ACHTUNG: Rows where COL_PENALTY equals PENALTY_CAT_NAME are not real solve times, just
    // placeholders to preserve a solve category name before any solves are created under that
    // category, or after all solves for that category name have been deleted.
    private static final String TABLE_TIMES     = "times";

    private static final String COL_PUZZLE_TYPE = "type";    // From "PuzzleType.typeName()".
    private static final String COL_CATEGORY    = "subtype"; // e.g., "Normal". (Historical name.)
    private static final String COL_TIME        = "time";    // Milliseconds.
    private static final String COL_DATE        = "date";    // Milliseconds since Unix epoch time.
    private static final String COL_SCRAMBLE    = "scramble";
    private static final String COL_PENALTIES   = "penalty"; // Encoded "Penalties" value.
    private static final String COL_COMMENT     = "comment";
    private static final String COL_HISTORY     = "history";

    private static final String CREATE_TABLE_TIMES =
        "CREATE TABLE " + TABLE_TIMES + "("
            + COL_ID          + " INTEGER PRIMARY KEY,"
            + COL_PUZZLE_TYPE + " TEXT,"
            + COL_CATEGORY    + " TEXT,"
            + COL_TIME        + " INTEGER,"
            + COL_DATE        + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),"
            + COL_SCRAMBLE    + " TEXT,"
            + COL_PENALTIES   + " INTEGER,"
            + COL_COMMENT     + " TEXT,"
            + COL_HISTORY     + " BOOLEAN"
            + ")";

    /**
     * The penalty value used to indicate that a solve row is only "fake" solve acting as a
     * placeholder for a solve category name. This is not a valid value for creation of an instance
     * of {@link Penalties}; it must be filtered out before creating {@link Solve} instances.
     */
    private static final int CAT_NAME_PENALTY_CODE = -1;

    /**
     * The old penalty value that was used to indicate that a solve row is only "fake" solve acting
     * as a placeholder for a solve category name prior to database version 10.
     */
    private static final int OLD_CAT_NAME_PENALTY_CODE = 10;

    /**
     * The old penalty value that was used to indicate a 2-second penalty ("+2") incurred by a
     * solve attempt prior to database version 10. From version 10, the encoded value from
     * {@link Penalties} is used.
     */
    private static final int OLD_PLUS_TWO_PENALTY_CODE = 1;

    /**
     * The old penalty value that was used to indicate a did-not-finish (DNF) solve result prior
     * to database version 10. From version 10, the encoded value from {@link Penalties} is used.
     */
    private static final int OLD_DNF_PENALTY_CODE = 2;

    // Algs table
    private static final String TABLE_ALGS   = "algs";

    private static final String COL_SUBSET   = "subset";
    private static final String COL_NAME     = "name";
    private static final String COL_STATE    = "state";
    private static final String COL_ALGS     = "algs";
    private static final String COL_PROGRESS = "progress";

    private static final String CREATE_TABLE_ALGS  =
        "CREATE TABLE " + TABLE_ALGS + "("
            + COL_ID       + " INTEGER PRIMARY KEY,"
            + COL_SUBSET   + " TEXT,"
            + COL_NAME     + " TEXT,"
            + COL_STATE    + " TEXT,"
            + COL_ALGS     + " TEXT,"
            + COL_PROGRESS + " INTEGER"
            + ")";

    public static final String SUBSET_OLL = "OLL";
    public static final String SUBSET_PLL = "PLL";

    private static final String DATABASE_NAME      = "databaseManager";
    private static final int    DATABASE_VERSION   = 10;

    /**
     * The header value used in the back-up CSV format that will identify the version of the
     * database from which the CSV was exported. Prior to database version 10, exported data will
     * not include this field in the CSV.
     */
    private static final String CSV_HEADER_DB_VERSION = "TwistyTimerDatabaseVersion";

    /**
     * The separator character used for CSV output.
     */
    private static final char CSV_SEPARATOR = ';';

    /**
     * The string used to identify a did-not-finish (DNF) solve in the export/import of the
     * external CSV text file format.
     */
    private static final String CSV_EXTERNAL_FORMAT_DNF_MARKER = "DNF";

    /**
     * An interface for notification of the progress of bulk database operations.
     */
    public interface ProgressListener {
        /**
         * Notifies the listener of the progress of a bulk operation. This may be called many
         * times during the operation.
         *
         * @param numCompleted
         *     The number of sub-operations of the bulk operation that have been completed.
         * @param total
         *     The total number of sub-operations that must be completed before the the bulk
         *     operation is complete.
         */
        void onProgress(int numCompleted, int total);
    }

    public DatabaseHandler() {
        super(TwistyTimer.getAppContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TIMES);
        db.execSQL(CREATE_TABLE_ALGS);
        createInitialAlgs(db);

        // Add "fake" category name records for the default solve category of each puzzle type.
        // See comments in "upgrade9to10" for more details on the need for these records.
        for (PuzzleType puzzleType : PuzzleType.values()) {
            addSolveCategoryInternal(db, puzzleType, MainState.DEFAULT_SOLVE_CATEGORY);
        }
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        Log.d("Database upgrade", "Upgrading from"
            + Integer.toString(oldVersion) + " to " + Integer.toString(newVersion));

        switch (oldVersion) {
            case 6:
                db.execSQL("ALTER TABLE " + TABLE_TIMES + " ADD COLUMN "
                        + COL_HISTORY + " BOOLEAN DEFAULT 0");
                // Fall through to the next upgrade step.
            case 8: // Upgrade to "9".
                // Convert the old time text size value to a percentage scale (from whatever it
                // was before, a 1-to-10 scale, maybe).
                Prefs.edit()
                        .putInt(R.string.pk_timer_display_scale_pc,
                                Prefs.getIntRawDefault(R.string.pk_timer_display_scale_pc, 10) * 10)
                        .apply();
                // Fall through to the next upgrade step.
            case 9: // Upgrade to "10".
                upgrade9to10(db);
        }
    }

    private void upgrade9to10(SQLiteDatabase db) {
        // ------------------------------------------------------------------------------
        // CHANGE VALUES OF "times.penalty" TO CONFORM TO NEW "Penalties" ENCODING SCHEME
        // ------------------------------------------------------------------------------
        // For "fake" solves that represent only a category names, the old value (10) must be
        // changed to the new value (-1). The new value is safer, as the encoded value of a
        // "Penalties" instance is never negative.
        //
        // For "DNF" and "+2" penalties, the changes are more difficult. The "Penalties" encoding
        // allows a distinction to be made between those penalties incurred before the solve timer
        // started (i.e., before, during and after an inspection period) and those penalties
        // incurred after the solve timer started (i.e., during and after the solve attempt). The
        // database "times.penalty" column currently only records a single 1=>"+2" or 2=>"DNF" value
        // (ignoring the other "fake" value). For a DNF penalty, it can be inferred from the solve
        // time if the penalty is a pre-start or a post-start DNF: the solve time will be zero if
        // the penalty is a pre-start DNF and non-zero if the penalty is a post-start DNF. However,
        // no such inference is possible for a "+2" penalty. The penalty could have been incurred
        // automatically by overrunning the inspection period, but not timing out, or manually by
        // editing the solve after it was first created and adding a "+2". If one assumes that most
        // users are unlikely to incur a "+2" penalty manually, then, perhaps, any "+2" is more
        // likely to have been incurred automatically during the inspection period. However, if the
        // user does not have an inspection period enabled, one could assume that this preference
        // was likely to have been the same for past solve attempts, so, perhaps, any "+2" is more
        // likely to have been incurred manually after the solve attempt.
        //
        // While these assumptions may be reasonable, there is a change in the behaviour of the
        // editing of penalties that supports a simpler approach: the app only allows pre-start
        // penalties to be incurred automatically during the inspection period and does not allow
        // these to be edited after the solve attempt ends. For example, if the user incurred a
        // "+2" penalty for overrunning the inspection period, then the user cannot "cheat" and
        // annul that penalty after the solve is complete. The assumption (above) that all "+2"
        // penalties are automatic pre-start penalties if an inspection period is enabled would
        // prevent a user from remove a "+2" penalty that may indeed have been incurred manually
        // in the post-start phase. Therefore, it is probably best to simply assign all "+2"
        // penalties to the post-start phase.
        //
        // The previous implementation only allowed one penalty per solve, though that did not
        // support the WCA Regulations that stipulate that penalties be cumulative. One further
        // inference can be made to correct this historically: if a DNF penalty was incurred
        // automatically because inspection timed out (which is assumed when the solve time is
        // zero), then the regulation "+2" penalty for overrunning the inspection period just
        // before the time-out should also be imposed and the solve time incremented to two
        // seconds. This is easy to do and is probably more correct. It will also avoid the case
        // in the future where some historical pre-start DNF solves differ from new solves in
        // their recorded time (zero vs. 2,000 ms) and penalties (DNF vs +2 & DNF).
        //
        // There is no pressing case for renaming the column from "penalty" to "penalties", so the
        // old name, though a slight misnomer, will be retained. The constant for this column name
        // is renamed to "COL_PENALTIES", so that should be clear enough.
        //
        // --------------------------------------------------
        // CREATE MISSING "FAKE" RECORDS FOR SOLVE CATEGORIES
        // --------------------------------------------------
        // The semantics of "deleteSolveCategory" become confusing in the absence of a "fake"
        // category name solve record for the default category. The lack of this record also means
        // that "getAllCategoriesForType" needs to read all solves, not just the "fake" solves, so
        // it can detect if the default category exists. The purpose of this "upgrade" is to create
        // one "fake" category name record for the default category for each puzzle type. This
        // ensures that it will later be possible to distinguish between the case where the default
        // category was explicitly deleted and the case where no solves exist in the default
        // category. This may not correctly reflect what occurred in the past (*), but it makes
        // things easier to handle in the future.
        //
        // With this record in place, getting all solve categories just requires getting all the
        // "fake" solve records. The only special case is that "deleteSolveCategory" will re-create
        // the "fake" record for the default solve category if the last category is deleted.
        //
        // (*) For example, a user may have create a new category "C" and explicitly deleted the
        // default category, but this upgrade will re-create that category. However, this is
        // probably unlikely and is not really that much of an issue.

        final int preStartDNFAndPlus2Code = Penalties.NO_PENALTIES
                .incurPreStartPenalty(Penalty.PLUS_TWO).incurPreStartPenalty(Penalty.DNF).encode();
        final int postStartDNFCode = Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.DNF).encode();
        final int postStartPlus2Code = Penalties.NO_PENALTIES
                .incurPostStartPenalty(Penalty.PLUS_TWO).encode();
        final ContentValues values = new ContentValues();

        // Wrap the update in a transaction, as it will likely be much faster.
        try {
            db.beginTransaction();

            // Upgrade the penalty code for all "fake" category name records to the new code value.
            values.clear();
            values.put(COL_PENALTIES, CAT_NAME_PENALTY_CODE);
            db.update(TABLE_TIMES, values, COL_PENALTIES + "=?",
                    new String[] { String.valueOf(OLD_CAT_NAME_PENALTY_CODE) });

            // Upgrade the penalty codes for all "DNF" and "+2" solves.
            //
            // NOTE: There are three old values 0 => NONE, 1 => "+2", or 2 => "DNF" (ignoring
            // the old "fake" value of 10, which was set to -1 above). There are three possible new
            // values based on the encoding scheme of "Penalties". The scheme uses bits 0-7 for the
            // pre-start penalties and bits 8-15 for the post-start penalties. Each "+2" adds two
            // to the byte value and a single DNF adds one (so a DNF penalty code is always odd).
            // The code values that will be applied here are: 0 => NONE (unchanged), 3 => pre-start
            // DNF *and* new pre-start "+2" (where the old code is 2 => DNF and time is zero),
            // 512 => post-start "+2" (where the old code is 1 => "+2"), or 256 => post-start DNF
            // (where the old code is 2 => DNF and time is non-zero). As none of the new values can
            // be the same as the old values (except for zero, which will not be updated), there is
            // no issue with the order in which these updates are made, as the effect of one update
            // will not interfere with the selection made for the next update.
            //
            // A) If the solve time is zero and the penalty is a DNF, then incur a pre-start DNF
            // *and* a new pre-start "+2" penalty, for the overrun that must have occurred before
            // the DNF. The time must also be updated to include the "+2" penalty (+2,000 ms).
            values.clear();
            values.put(COL_PENALTIES, preStartDNFAndPlus2Code);
            values.put(COL_TIME, Penalties.PLUS_TWO_DURATION_MS); // *zero* + 2,000 ms.
            db.update(TABLE_TIMES, values, COL_PENALTIES + "=? AND " + COL_TIME + "=0",
                    new String[] { String.valueOf(OLD_DNF_PENALTY_CODE) });

            // FIXME: May want to deal with the case where there is no DNF and the time is zero.
            // Perhaps just delete those records.

            // B) If the solve time is non-zero and the penalty is a DNF, then incur a post-start
            // DNF penalty. The time is not changed (so be sure to call "value.clear()").
            values.clear();
            values.put(COL_PENALTIES, postStartDNFCode);
            db.update(TABLE_TIMES, values, COL_PENALTIES + "=? AND " + COL_TIME + ">0",
                    new String[] { String.valueOf(OLD_DNF_PENALTY_CODE) });

            // C) If the penalty is a "+2", then convert it to a post-start "+2" penalty. The time
            // value is not changed.
            values.clear();
            values.put(COL_PENALTIES, postStartPlus2Code);
            db.update(TABLE_TIMES, values, COL_PENALTIES + "=?",
                    new String[] { String.valueOf(OLD_PLUS_TWO_PENALTY_CODE) });

            // Add any missing "fake" category name records for the default solve category (they
            // will already exist for any non-default solve category).
            for (PuzzleType puzzleType : PuzzleType.values()) {
                addSolveCategoryInternal(db, puzzleType, MainState.DEFAULT_SOLVE_CATEGORY);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Gets the new-style penalties to be applied given an old penalty code. If the old penalty
     * code is a "DNF" and the time is zero, the new code will be a pre-start DNF <i>and</i> a new
     * pre-start "+2"; the caller is then responsible for adding the "+2" penalty to the time value.
     *
     * @param oldPenaltyCode
     *     The old-style penalty code. The "fake" penalty code for a category name is not allowed.
     * @param time
     *     The time taken to solve the puzzle (in milliseconds).
     *
     * @return
     *     The new-style penalties value.
     *
     * @throws IllegalArgumentException
     *     If the penalty code is not one of the known old penalty codes or is the old "fake" code
     *     used to define a category name.
     */
    private static Penalties upgradePenalties(int oldPenaltyCode, long time)
            throws IllegalArgumentException {
        // NOTE: See comments in "upgrade9to10" for details on the justification for this scheme.
        switch (oldPenaltyCode) {
            case OLD_DNF_PENALTY_CODE:
                // If the solve time is zero, incur a pre-start "+2" and a pre-start "DNF". The
                // caller must add the new "+2" penalty to the time value.
                if (time == 0) {
                    return Penalties.NO_PENALTIES
                            .incurPreStartPenalty(Penalty.DNF)
                            .incurPreStartPenalty(Penalty.PLUS_TWO);
                }
                // Else if the time is non-zero, incur only a post-start DNF.
                return Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.DNF);

            case OLD_PLUS_TWO_PENALTY_CODE:
                // A "+2" becomes a post-start "+2" and the time value does not change.
                return Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.PLUS_TWO);

            case 0:
                return Penalties.NO_PENALTIES;

            default:
            case OLD_CAT_NAME_PENALTY_CODE:
                throw new IllegalArgumentException("Invalid old penalty code: " + oldPenaltyCode);
        }
    }

    /**
     * Gets the value of a column as a Boolean for the row at the current cursor position. The
     * column must be an {@code INTEGER} or {@code BOOLEAN} SQLite type.
     *
     * @param cursor      The cursor from which to get the column value.
     * @param columnIndex The index of the column to interpret as a Boolean value.
     *
     * @return {@code true} if the column value is not null or zero; otherwise {@code false}.
     */
    private static boolean colToBoolean(@NonNull Cursor cursor, int columnIndex) {
        return ! (cursor.isNull(columnIndex) || cursor.getInt(columnIndex) == 0);
    }

    /**
     * Gets the value of a Boolean in a form suitable for insertion into a database column through
     * an SQL statement. In the database, a Boolean value is represented as an {@code INTEGER} or
     * {@code BOOLEAN} SQLite type: {@code 1} for {@code true} and {@code 0} for {@code false}.
     * This method returns the value as a string, which is more convenient when constructing SQL
     * statements and selection argument values.
     *
     * @param value The Boolean value to be converted to its database representation.
     *
     * @return An SQL string value: "1" for {@code true} or "0" for {@code false}.
     */
    @NonNull
    private static String booleanToCol(boolean value) {
        return value ? "1" : "0";
    }

    private static void createAlg(
            SQLiteDatabase db, String subset, String name, String state, String algs) {
        final ContentValues values = new ContentValues();

        values.put(COL_SUBSET, subset);
        values.put(COL_NAME, name);
        values.put(COL_STATE, state);
        values.put(COL_ALGS, algs);
        values.put(COL_PROGRESS, 0);

        db.insert(TABLE_ALGS, null, values);
    }

    /**
     * Loads an algorithm from the database for the given algorithm ID.
     *
     * @param algID
     *     The ID of the algorithm to be loaded.
     *
     * @return
     *     An {@link Algorithm} object created from the details loaded from the database for the
     *     algorithm record matching the given ID, or {@code null} if no algorithm matching the
     *     given ID was found.
     */
    public Algorithm getAlgorithm(long algID) {
        final Cursor cursor = getReadableDatabase().query(TABLE_ALGS,
                new String[] { COL_ID, COL_SUBSET, COL_NAME, COL_STATE, COL_ALGS, COL_PROGRESS },
                COL_ID + "=?", new String[] { String.valueOf(algID) }, null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return new Algorithm(
                        cursor.getLong(0),   // id
                        cursor.getString(1), // subset
                        cursor.getString(2), // name
                        cursor.getString(3), // state
                        cursor.getString(4), // algs
                        cursor.getInt(5));   // progress
            }

            // No algorithm matched the given ID.
            return null;
        } finally {
            cursor.close();
        }
    }

    /**
     * Gets a cursor over all algorithms from the PLL or OLL subset.
     *
     * @param subset {@link #SUBSET_PLL} or {@link #SUBSET_OLL}.
     *
     * @return A cursor over all matching algorithms.
     */
    public Cursor getAllAlgorithmsForSubset(String subset) {
        return getReadableDatabase().query(
                TABLE_ALGS, null, COL_SUBSET + "=?",
                new String[] { subset }, null, null, null, null);
    }

    public int updateAlgorithmAlg(long id, String alg) {
        final ContentValues values = new ContentValues();

        values.put(COL_ALGS, alg);

        return getWritableDatabase().update(
                TABLE_ALGS, values, COL_ID + "=?", new String[] { String.valueOf(id) });
    }

    public int updateAlgorithmProgress(long id, int progress) {
        final ContentValues values = new ContentValues();

        values.put(COL_PROGRESS, progress);

        return getWritableDatabase().update(
                TABLE_ALGS, values, COL_ID + "=?", new String[] { String.valueOf(id) });
    }

    /**
     * Gets an SQL relational expression that requires each of the given column names in
     * {@link #TABLE_TIMES} to have a value equal to a placeholder "?". The "fake" solve times are
     * automatically excluded. For example, if the columns are {@code ("A", "B", "C")}, then the
     * returned expression is {@code "penalty!=10 AND A=? AND B=? AND C=?"}.
     *
     * @param colNames
     *     The names of the columns to include in the expression. If {@code null} or empty, all
     *     solves except the "fake" solve will be matched. {@link #COL_PENALTIES} must not be used
     *     as a column name, as it will be added automatically.
     *
     * @return
     *     An SQL expression matching all rows where the solve is not a "fake" solve for a category
     *     name and the value of every named column matches a placeholder value. The placeholder
     *     values are in the same order as the given column names.
     */
    private static String sqlMatchColsNotFakes(String... colNames) {
        return sqlMatchColsWithPrefix(COL_PENALTIES + "!=" + CAT_NAME_PENALTY_CODE, colNames);
    }

    /**
     * Gets an SQL relational expression that requires each of the given column names in
     * {@link #TABLE_TIMES} to have a value equal to a placeholder "?". <i>Only "fake" solve time
     * records acting as persistent holders for solve category names, are included.</i> For
     * example, if the columns are {@code ("A", "B", "C")}, then the returned expression is
     * {@code "penalty=10 AND A=? AND B=? AND C=?"}.
     *
     * @param colNames
     *     The names of the columns to include in the expression. If {@code null} or empty, all
     *     "fake" solves will be matched. {@link #COL_PENALTIES} must not be used as a column name,
     *     as it will be added automatically.
     *
     * @return
     *     An SQL expression matching all rows where the solve <i>is</i> a "fake" solve for a
     *     category name and the value of every named column matches a placeholder value. The
     *     placeholder values are in the same order as the given column names.
     */
    private static String sqlMatchColsFakes(String... colNames) {
        return sqlMatchColsWithPrefix(COL_PENALTIES + "=" + CAT_NAME_PENALTY_CODE, colNames);
    }

    /**
     * Gets an SQL relational expression that requires each of the given column names in to have a
     * value equal to a placeholder "?". For example, if the columns are {@code ("A", "B", "C")},
     * then the returned expression is {@code "A=? AND B=? AND C=?"}.
     *
     * @param firstTerm
     *     An optional first term to place before the other terms. May be {@code null} or empty if
     *     no term should be placed before the other column-matching terms. Otherwise the term will
     *     be included and separated from the column-matching terms with an "AND" operator.
     * @param colNames
     *     The names of the columns to include in the expression. If {@code null} or empty, the
     *     returned expression will be an empty string. {@link #COL_PENALTIES} must not be used as a
     *     column name, though it will <i>not</i> be added automatically.
     *
     * @return
     *     An SQL expression matching all rows where the value of every named column matches a
     *     placeholder value. The placeholder values are in the same order as the given column
     *     names.
     */
    private static String sqlMatchColsWithPrefix(String firstTerm, String... colNames) {
        final StringBuilder sql = new StringBuilder(200);

        if (firstTerm != null) {
            sql.append(firstTerm);
        }

        if (colNames != null) {
            for (String colName : colNames) {
                if (colName != null) {
                    if (colName.equals(COL_PENALTIES)) {
                        throw new IllegalArgumentException(
                                "COL_PENALTY cannot be passed as a column name!");
                    }

                    if (sql.length() > 0) {
                        sql.append(" AND ");
                    }
                    sql.append(colName).append("=?");
                }
            }
        }

        return sql.toString();
    }

    /**
     * Gets a cursor over all solves times for a single combination of puzzle type and category.
     * Times from the history of past sessions only, or times from the current session only are
     * selected. This supports the presentation of the list of times in {@code TimerListFragment}.
     * The times are returned in reverse chronological order, newest times first. The companion
     * method {@link #getCurrentSolve(Cursor)} can be used to create a {@link Solve} object from
     * the row at the current position of the cursor returned by this method.
     *
     * @param puzzleType
     *     The type of the puzzle.
     * @param solveCategory
     *     The category of the puzzle.
     * @param isHistory
     *     {@code true} if the times should only be for past sessions (the "history" or "archive"),
     *     or {@code false} if the times should only be for the current session. <i>Note that this
     *     is different from the behaviour of some other methods that include the current session
     *     times in the list of the full history of all times. Here, the two sets of times do not
     *     intersect.</i>
     *
     * @return
     *     A cursor over all of the selected solve times. May be empty if no times match.
     */
    public Cursor getAllSolvesFor(
            PuzzleType puzzleType, String solveCategory, boolean isHistory) {
        return getReadableDatabase().query(
                TABLE_TIMES,
                // Guarantee the order (index) of the columns by selecting them explicitly.
                new String[] { COL_ID, COL_TIME, COL_PUZZLE_TYPE, COL_CATEGORY, COL_DATE,
                               COL_SCRAMBLE, COL_PENALTIES, COL_COMMENT, COL_HISTORY },
                sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY, COL_HISTORY),
                new String[] {
                        puzzleType.typeName(), solveCategory, booleanToCol(isHistory)
                },
                null, null, COL_DATE + " DESC", null);
    }

    /**
     * Gets the solve for the current position of the given cursor. <i>This should only be used
     * on a cursor returned from {@link #getAllSolvesFor(PuzzleType, String, boolean)}.</i>
     *
     * @param cursor The cursor positioned at the required solve row.
     *
     * @return The solve at that current cursor position.
     *
     * @throws IllegalStateException
     *     If the solve at the current cursor position is a "fake" solve defining a category name.
     */
    public static Solve getCurrentSolve(Cursor cursor) {
        // NOTE: The column indices follow the order of the columns selected in "getAllSolvesFor()".
        final int penaltyCode = cursor.getInt(6);

        if (penaltyCode == CAT_NAME_PENALTY_CODE) {
            throw new IllegalStateException("Attempted to get a 'fake' solve record.");
        }

        return new Solve(
                cursor.getInt(0),         // "id"
                cursor.getInt(1),         // "time"
                PuzzleType.forTypeName(cursor.getString(2)),
                cursor.getString(3),      // "category"
                cursor.getLong(4),        // "date"
                cursor.getString(5),      // "scramble"
                Penalties.decode(penaltyCode),
                cursor.getString(7),      // "comment"
                colToBoolean(cursor, 8)); // "history"
    }

    /**
     * Moves all current solves for one puzzle type and category from the session to the history.
     *
     * @param puzzleType    The puzzle type.
     * @param solveCategory The solve category.
     *
     * @return The number of time rows updated.
     */
    public int moveAllSolvesToHistory(PuzzleType puzzleType, String solveCategory) {
        final ContentValues values = new ContentValues();

        values.put(COL_HISTORY, true);

        return getWritableDatabase().update(
                TABLE_TIMES, values, sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY),
                new String[] { puzzleType.typeName(), solveCategory });
    }

    /**
     * Adds a new solve to the database.
     *
     * @param solve The solve to be added to the database.
     * @return The new ID of the stored solve record.
     */
    public long addSolve(Solve solve) {
        return addSolveInternal(getWritableDatabase(), solve, false);
    }

    /**
     * Adds a new solve to the given database. "Fake" solve records that represent only the name
     * of a solve category may also be added by raising the {@code isFake} flag argument.
     *
     * @param db
     *     The database to which to add the solve.
     * @param solve
     *     The solve to be added to the database.
     * @param isFake
     *     {@code true} if this is a "fake" solve intended only to represent the name of a solve
     *     category in the absence of any real solve time records for that solve category; or
     *     {@code false} if this is a normal solve record that records the result of a puzzle solve
     *     attempt. If {@code true}, any penalties on the {@code solve} are ignored and the internal
     *     {@link #CAT_NAME_PENALTY_CODE} code is used as the value of the penalty column instead.
     *
     * @return
     *     The new ID of the stored solve record.
     */
    private static long addSolveInternal(SQLiteDatabase db, Solve solve, boolean isFake) {
        final ContentValues values = new ContentValues();

        values.put(COL_PUZZLE_TYPE, solve.getPuzzleType().typeName());
        values.put(COL_CATEGORY, solve.getCategory());
        // Rounding/truncation happens in "Solve.getTime()"; the precise time is recorded here.
        values.put(COL_TIME, solve.getExactTime());
        values.put(COL_DATE, solve.getDate());
        values.put(COL_SCRAMBLE, solve.getScramble());
        values.put(COL_PENALTIES, isFake ? CAT_NAME_PENALTY_CODE : solve.getPenalties().encode());
        values.put(COL_COMMENT, solve.getComment());
        values.put(COL_HISTORY, solve.isHistory());

        return db.insert(TABLE_TIMES, null, values);
    }

    /**
     * Adds a collection of newly-imported solves to the given database. The solves are added in a
     * single transaction, so this operation is much faster than adding them one-by-one using the
     * {@link #addSolve(Solve)} method. Any given solve that matches a solve already in the
     * database will not be inserted. The "fake" solve records that hold only the category names
     * are not expected to be exported, so these will be recreated as necessary while the imported
     * solves are being added to the database.
     *
     * @param solves
     *     The collection of solves to be added to the database. Must not be {@code null}, but may
     *     be empty.
     * @param listener
     *     An optional progress listener that will be notified as each new solve is inserted into
     *     the database. Before the first new solve is added, this will be called to report that
     *     zero of the total number of solves have been inserted (even if {@code solves} is empty).
     *     Thereafter, it will be notified after each insertion. May be {@code null} if no progress
     *     updates are required.
     *
     * @return
     *     The number of unique solves inserted. Solves that are deemed duplicates of existing
     *     solves are not inserted.
     */
    public int addImportedSolves(
            @NonNull Collection<Solve> solves, @Nullable ProgressListener listener) {

        final int total = solves.size();
        int numProcessed = 0; // Whether inserted or not (i.e., includes duplicates).

        if (listener != null) {
            listener.onProgress(numProcessed, total);
        }

        // The "fake" solve records that represent category names are not backed up. However, these
        // *MUST* be re-created on importing the solves. For each puzzle type, maintain a set of
        // observed category names and re-create the "fake" record for each new category name.
        final Map<PuzzleType, Set<String>> categoriesByPuzzleType = new EnumMap<>(PuzzleType.class);
        Set<String> categories;

        int numInserted = 0; // Only those actually inserted (i.e., excludes duplicates).

        if (total > 0) {
            final SQLiteDatabase db = getWritableDatabase();

            try {
                // Wrapping the insertions in a transaction is about 50x faster!
                db.beginTransaction();

                for (Solve solve : solves) {
                    if (!isImportedSolveDuplicated(solve)) {
                        addSolveInternal(db, solve, false);
                        numInserted++;
                    }

                    if (listener != null) {
                        listener.onProgress(++numProcessed, total);
                    }

                    categories = categoriesByPuzzleType.get(solve.getPuzzleType());

                    if (categories == null) { // First category for this puzzle type.
                        categories = new HashSet<>();
                        categoriesByPuzzleType.put(solve.getPuzzleType(), categories);
                    }

                    if (!categories.contains(solve.getCategory())) {
                        categories.add(solve.getCategory()); // Will not re-create this one again.
                        addSolveCategory(solve.getPuzzleType(), solve.getCategory());
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        return numInserted;
    }

    /**
     * Tests if a solve, restored from an imported file, already exists in the database.
     *
     * @param solve
     *     The imported solve for which to find duplicates.
     *
     * @return
     *     {@code true} if a duplicate solve appears to be present in the database; or
     *     {@code false} if the solve appears to be unique.
     */
    private boolean isImportedSolveDuplicated(Solve solve) {
        //
        // BACK-UP FORMAT
        // --------------
        // The "back-up" format includes all columns except "COL_ID" and "COL_HISTORY". The time
        // and date are written with full precision using just the integer value as stored in the
        // database. These should be easy to identify in the database. It might seem enough to just
        // test "COL_DATE", but that might be duplicated for times that were added manually.
        //
        // EXTERNAL FORMAT
        // ---------------
        // The "external" format includes only three columns "COL_TIME", "COL_DATE" and
        // "COL_SCRAMBLE". It may or may not include "COL_PENALTY", but only for "Penalty.DNF"
        // if at all ("Penalty.NONE" is implied for all other solves).
        //
        // The time value is formatted to a human-readable form when it is exported. It is assumed
        // that it will have been recorded with millisecond accuracy (that appears to be to default
        // behaviour, though no specific format is given for the conversion). It is also assumed
        // that the time zone will be stable over the export/import round trip, so the date reported
        // by "solve" will be identical to whatever is in the database, if that solve is present.
        //
        // "COL_TIME" used to be truncated truncated to the nearest (not greater) multiple of
        // 10 milliseconds before it was stored in the database and also when it was exported.
        // However, the formatting routine rounded times over 1 hour to the nearest second (and
        // then the parsing routine could not parse times over 1 hour). In this latest iteration,
        // the database times are *not* truncated. Therefore, it is much simpler to ensure that
        // the file format specifies times to the millisecond, so they can be matched directly to
        // the database values. The wrinkle around the 1-hour mark in formatting and parsing is
        // also fixed. This change will not be a problem for matching older imported files with
        // 0.01 s precision, as the times in the database were also truncated in the same manner.
        // Old times will still match old times and new times will now match new times.
        //
        return 0 < DatabaseUtils.queryNumEntries(getReadableDatabase(), TABLE_TIMES,
                sqlMatchColsNotFakes(
                        COL_PUZZLE_TYPE, COL_CATEGORY, COL_TIME, COL_SCRAMBLE, COL_DATE),
                new String[] {
                        solve.getPuzzleType().typeName(),
                        solve.getCategory(),
                        String.valueOf(solve.getExactTime()), // Not the rounded "Solve.getTime()"!
                        solve.getScramble(),
                        String.valueOf(solve.getDate())
                });
    }

    /**
     * Updates an existing solve in the database. The solve must be identified by its database
     * record ID.
     *
     * @param solve
     *     The solve to be updated.
     *
     * @return
     *     The number of records updated. If the details of the given solve are the same as those
     *     already in the database, of the if the solve has an ID, but the ID does not match any
     *     solve record in the database, the result may be zero, as no update will be performed.
     *
     * @throws IllegalArgumentException
     *     If the solve has no ID set (per {@link Solve#hasID()}).
     */
    public int updateSolve(@NonNull Solve solve) throws IllegalArgumentException {
        if (!solve.hasID()) {
            throw new IllegalArgumentException("Solve has no ID and cannot be updated: " + solve);
        }

        final ContentValues values = new ContentValues();

        values.put(COL_PUZZLE_TYPE, solve.getPuzzleType().typeName());
        values.put(COL_CATEGORY, solve.getCategory());
        values.put(COL_TIME, solve.getExactTime());
        values.put(COL_DATE, solve.getDate());
        values.put(COL_SCRAMBLE, solve.getScramble());
        values.put(COL_PENALTIES, solve.getPenalties().encode());
        values.put(COL_COMMENT, solve.getComment());
        values.put(COL_HISTORY, solve.isHistory());

        return getWritableDatabase().update(
                TABLE_TIMES, values, COL_ID + "=?", new String[] { String.valueOf(solve.getID()) });
    }

    /**
     * Loads a solve from the database for the given solve ID.
     *
     * @param solveID
     *     The ID of the solve to be loaded.
     *
     * @return
     *     A {@link Solve} object created from the details loaded from the database for the solve
     *     time matching the given ID; or {@code null} if no solve time matching the given ID was
     *     found, or if the matched record was a "fake" solve record used to define a category name.
     */
    public Solve getSolve(long solveID) {
        final Cursor cursor = getReadableDatabase().query(
                TABLE_TIMES,
                new String[] { COL_ID, COL_TIME, COL_PUZZLE_TYPE, COL_CATEGORY, COL_DATE,
                               COL_SCRAMBLE, COL_PENALTIES, COL_COMMENT, COL_HISTORY },
                COL_ID + "=?", new String[] { String.valueOf(solveID) }, null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                final int penaltyCode = cursor.getInt(6);

                if (penaltyCode != CAT_NAME_PENALTY_CODE) {
                    return new Solve(
                            cursor.getInt(0),         // "id"
                            cursor.getLong(1),         // "time"
                            PuzzleType.forTypeName(cursor.getString(2)),
                            cursor.getString(3),      // "category"
                            cursor.getLong(4),        // "date"
                            cursor.getString(5),      // "scramble"
                            Penalties.decode(penaltyCode),
                            cursor.getString(7),      // "comment"
                            colToBoolean(cursor, 8)); // "history"
                }
            }

            // No (non-fake) solve matched the given ID.
            return null;
        } finally {
            cursor.close();
        }
    }

    /**
     * Updates the values for an existing, saved solve record asynchronously and broadcasts a
     * notification of the change when the task is complete. The given solve must have a database
     * record ID set, so that specific record can be identified and updated. Notification of the
     * update will be broadcast, so local broadcast receivers can act upon the change. For example,
     * to update statistics or charts.
     *
     * @param solve
     *     The solve to be updated.
     * @param mainState
     *     The main state information current at the time of this call.
     *
     * @throws IllegalArgumentException
     *     If the given solve has no database ID.
     */
    public void updateSolveAndNotifyAsync(
            @NonNull final Solve solve, @NonNull final MainState mainState)
            throws IllegalArgumentException {

        if (solve.getID() == Solve.NO_ID) {
            throw new IllegalArgumentException("Cannot update solve with no ID: " + solve.getID());
        }

        FireAndForgetExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateSolve(solve);
                TTIntent.builder(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                        .mainState(mainState)
                        .broadcast();
            }
        });
    }

    /**
     * Deletes an existing, saved solve record asynchronously and broadcasts a notification of the
     * change when the task is complete. The given solve must have a database record ID set, so
     * that specific record can be identified and deleted. Notification of the deletion will be
     * broadcast, so local broadcast receivers can act upon the change. For example, to update
     * statistics or charts.
     *
     * @param solve
     *     The solve to delete.
     * @param mainState
     *     The main state information current at the time of this call.
     *
     * @throws IllegalArgumentException
     *     If the given solve has no database ID.
     */
    public void deleteSolveAndNotifyAsync(
            @NonNull final Solve solve, @NonNull final MainState mainState)
            throws IllegalArgumentException {

        if (solve.getID() == Solve.NO_ID) {
            throw new IllegalArgumentException("Cannot delete solve with no ID: " + solve.getID());
        }

        FireAndForgetExecutor.execute(new Runnable() {
            @Override
            public void run() {
                deleteSolve(solve);
                TTIntent.builder(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                        .mainState(mainState)
                        .broadcast();
            }
        });
    }

    /**
     * Deletes zero or more existing, saved solve records asynchronously and broadcasts a
     * notification of the change when the task is complete. Notification of the deletion will be
     * broadcast, so local broadcast receivers can act upon the change. For example, to update
     * statistics or charts.
     *
     * @param solveIDs
     *     A collection of solve record IDs for the solves to delete.
     * @param mainState
     *     The main state information current at the time of this call.
     */
    public void deleteSolvesByIDAndNotifyAsync(
            @NonNull final Collection<Long> solveIDs, @NonNull final MainState mainState) {
        FireAndForgetExecutor.execute(new Runnable() {
            @Override
            public void run() {
                deleteSolvesByID(solveIDs, null); // Ignore progress reports.
                TTIntent.builder(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
                        .mainState(mainState)
                        .broadcast();
            }
        });
    }

    /**
     * Populates the collection of statistics (average calculators) with the solve times recorded
     * in the database. The statistics will manage the segregation of solves for the current session
     * only from those from all past and current sessions. If all average calculators are for the
     * current session only, only the times for the current session will be read from the database.
     *
     * @param statistics
     *     The statistics in which to record the solve times. This may contain any mix of average
     *     calculators for all sessions or only the current session. The database read will be
     *     adapted automatically to read the minimum number of rows to satisfy the collection of
     *     the required statistics.
     */
    public void populateStatistics(Statistics statistics) {
        // The main state's "isHistoryEnabled()" is not relevant here: the average-of-N calculators
        // in the statistics may all be set for the current session only, regardless of that flag.
        final boolean isStatisticsForCurrentSessionOnly = statistics.isForCurrentSessionOnly();
        final String sql;

        // Sort into ascending order of date (oldest solves first), so that the "current"
        // average is, in the end, calculated to be that of the most recent solves.
        if (isStatisticsForCurrentSessionOnly) {
            sql = "SELECT " + COL_TIME + ", " + COL_PENALTIES
                    + " FROM " + TABLE_TIMES
                    + " WHERE " + sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY)
                    // Solves for the current session only.
                    + " AND " + COL_HISTORY + "=" + booleanToCol(false)
                    + " ORDER BY " + COL_DATE + " ASC";
        } else {
            sql = "SELECT " + COL_TIME + ", " + COL_PENALTIES + ", " + COL_HISTORY
                    + " FROM " + TABLE_TIMES
                    + " WHERE " + sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY)
                    + " ORDER BY " + COL_DATE + " ASC";
        }

        final Cursor cursor = getReadableDatabase().rawQuery(sql,
                new String[] {
                        statistics.getMainState().getPuzzleType().typeName(),
                        statistics.getMainState().getSolveCategory()
                });

        try {
            final int timeCol = cursor.getColumnIndex(COL_TIME);
            final int penaltyCol = cursor.getColumnIndex(COL_PENALTIES);
            final int historyCol
                    = isStatisticsForCurrentSessionOnly ? -1 : cursor.getColumnIndex(COL_HISTORY);

            while (cursor.moveToNext()) {
                final boolean isForCurrentSession
                        = isStatisticsForCurrentSessionOnly || !colToBoolean(cursor, historyCol);

                // "Penalties.decode(int)" throws an exception if penalty is invalid, e.g., if a
                // "fake" category name penalty code is encountered. These should not be selected.
                if (Penalties.decode(cursor.getInt(penaltyCol)).hasDNF()) {
                    statistics.addDNF(isForCurrentSession);
                } else {
                    // "Statistics" will perform any necessary rounding/truncation on the time.
                    statistics.addTime(cursor.getLong(timeCol), isForCurrentSession);
                }
            }
        } finally {
            // As elsewhere in this class, assume "cursor" is not null.
            cursor.close();
        }
    }

    /**
     * Populates the chart statistics with the solve times recorded in the database. If all
     * statistics are for the current session only, only the times for the current session will be
     * read from the database.
     *
     * @param chartStatistics
     *     The chart statistics in which to record the solve times. This may require solve times for
     *     all sessions or only the current session. The database read will be adapted automatically
     *     to read the minimum number of rows to satisfy the collection of the required statistics.
     */
    public void populateChartStatistics(ChartStatistics chartStatistics) {
        final String sql;

        // Sort into ascending order of date (oldest solves first), so that the "current" average
        // is, after the last solve is added, calculated to be that of the most recent solves.
        if (chartStatistics.getMainState().isHistoryEnabled()) {
            // NOTE: A change from the old approach: the "all time" (history) option include those
            // from the current session, too. This is consistent with the way "all time statistics"
            // are calculated for the table of statistics.
            sql = "SELECT " + COL_TIME + ", " + COL_PENALTIES + ", " + COL_DATE
                    + " FROM " + TABLE_TIMES
                    + " WHERE " + sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY)
                    + " ORDER BY " + COL_DATE + " ASC";
        } else {
            sql = "SELECT " + COL_TIME + ", " + COL_PENALTIES + ", " + COL_DATE
                    + " FROM " + TABLE_TIMES
                    + " WHERE " + sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY)
                    // Solves for the current session only.
                    + " AND " + COL_HISTORY + "=" + booleanToCol(false)
                    + " ORDER BY " + COL_DATE + " ASC";
        }

        final Cursor cursor = getReadableDatabase().rawQuery(sql,
                new String[] {
                        chartStatistics.getMainState().getPuzzleType().typeName(),
                        chartStatistics.getMainState().getSolveCategory()
                });

        try {
            final int timeCol = cursor.getColumnIndex(COL_TIME);
            final int penaltyCol = cursor.getColumnIndex(COL_PENALTIES);
            final int dateCol = cursor.getColumnIndex(COL_DATE);

            while (cursor.moveToNext()) {
                // "Penalties.decode(int)" throws an exception if penalty is invalid, e.g., if a
                // "fake" category name penalty code is encountered. These should not be selected.
                if (Penalties.decode(cursor.getInt(penaltyCol)).hasDNF()) {
                    chartStatistics.addDNF(cursor.getLong(dateCol));
                } else {
                    // "ChartStatistics" will perform any necessary rounding/truncation on the time.
                    chartStatistics.addTime(cursor.getLong(timeCol), cursor.getLong(dateCol));
                }
            }
        } finally {
            // As elsewhere in this class, assume "cursor" is not null.
            cursor.close();
        }
    }

    /**
     * Deletes a single solve from the database.
     *
     * @param solve
     *     The solve to be deleted. The corresponding database record to be deleted from the "times"
     *     table is matched using the ID returned from {@link Solve#getID()}.
     *
     * @return
     *     The number of records deleted. If no record matches the ID of the solve, the result is
     *     zero.
     */
    public int deleteSolve(Solve solve) {
        return deleteSolveByIDInternal(getWritableDatabase(), solve.getID());
    }

    /**
     * Deletes multiple solves from the database that match the solve record IDs in the given
     * collection. The solves are deleted in the context of a single database transaction.
     *
     * @param solveIDs
     *     The IDs of the solve records in the "times" table of the database to be deleted. Must
     *     not be {@code null}, but may be empty.
     * @param listener
     *     An optional progress listener that will be notified as each solve is deleted from the
     *     database. Before the first solve is deleted, this will be called to report that zero of
     *     the total number of solves have been deleted (even if {@code solveIDs} is empty).
     *     Thereafter, it will be notified after each attempted deletion by ID, whether a matching
     *     solve was found or not. May be {@code null} if no progress reports are required.
     *
     * @return
     *     The number of records deleted. If an ID from {@code solveIDs} does not match any record,
     *     or if an ID is a duplicate of an ID that has already been deleted, that ID is ignored,
     *     so the result may be less than the number of solve IDs in the collection.
     */
    public int deleteSolvesByID(Collection<Long> solveIDs, ProgressListener listener) {
        final int total = solveIDs.size();
        int numProcessed = 0; // Whether deleted or not (i.e., includes RNF and duplicates).

        if (listener != null) {
            listener.onProgress(numProcessed, total);
        }

        int numDeleted = 0; // Only those actually deleted (i.e., excludes RNF and duplicates).

        if (total > 0) {
            final SQLiteDatabase db = getWritableDatabase();

            try {
                // Wrap the bulk delete operations in a transaction; it is *much* faster,
                db.beginTransaction();

                for (long id : solveIDs) {
                    // May not change if RNF or if ID is a duplicate and is already deleted.
                    numDeleted += deleteSolveByIDInternal(db, id);

                    if (listener != null) {
                        listener.onProgress(++numProcessed, total);
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        return numDeleted;
    }

    /**
     * Deletes a single solve matching the given ID from the database.
     *
     * @param db
     *     The database from which to delete the solve.
     * @param solveID
     *     The ID of the solve record in the "times" table of the database.
     *
     * @return
     *     The number of records deleted. If no record matches {@code solveID}, the result is zero.
     */
    private static int deleteSolveByIDInternal(SQLiteDatabase db, long solveID) {
        return db.delete(TABLE_TIMES, COL_ID + "=?", new String[] { Long.toString(solveID) });
    }

    // Delete entries from session
    public int deleteAllFromSession(PuzzleType puzzleType, String solveCategory) {
        return getWritableDatabase().delete(
                TABLE_TIMES,
                sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY)
                        + " AND " + COL_HISTORY + "=" + booleanToCol(false),
                new String[] { puzzleType.typeName(), solveCategory });
    }

    /**
     * Adds a new solve category. If the new category is the same as an existing category, then no
     * new category is added.
     *
     * @param puzzleType
     *     The type of the puzzle for which to create the solve category.
     * @param newSolveCategory
     *     The name of the new solve category.
     *
     * @return
     *     The number of solve categories added.
     */
    public int addSolveCategory(
            @NonNull PuzzleType puzzleType, @NonNull CharSequence newSolveCategory) {
        return addSolveCategoryInternal(getWritableDatabase(), puzzleType, newSolveCategory);
    }

    /**
     * Adds a "fake" solve time record to hold a solve category name. This ensures that the name
     * exists in the database before the first solve time is created for that category, or after
     * the last solve time is deleted for that category.
     *
     * @param db
     *     A writable SQLite database.
     * @param puzzleType
     *     The type of the puzzle for which to create the solve category.
     * @param newSolveCategory
     *     The name of the new solve category.
     *
     * @return
     *     The number of solve categories added. This will be zero if the solve category already
     *     existed, otherwise it will be one.
     */
    private static int addSolveCategoryInternal(
           @NonNull SQLiteDatabase db, @NonNull PuzzleType puzzleType,
           @NonNull CharSequence newSolveCategory) {
        // Get a count of the number of existing "fake" solve records matching the new category
        // name. If there are none, add the new category. If there is one, do nothing. If there
        // is more than one, it might be nice to delete the redundant "fake" records, but that is
        // not strictly necessary, as "getAllCategoriesForType" uses "SELECT DISTINCT".
        //
        // This would be better done using proper database constraints to effect an insert-if-not-
        // exists operation. Triggers, or an upgrade step, could be used for any clean-up tasks.
        // All that is a bit too complicated after the fact, though. Perhaps it could be done as
        // part of a revamp and normalisation of the DB schema.
        if (0 == DatabaseUtils.queryNumEntries(
                db, TABLE_TIMES, sqlMatchColsFakes(COL_PUZZLE_TYPE, COL_CATEGORY),
                new String[] { puzzleType.typeName(), newSolveCategory.toString() })) {
            // Add a "fake" solve time record for the category name. Mark it as a "history" solve,
            // so it will not be deleted if all solves for the current session are deleted, and it
            // will not be affected by "moveAllSolvesToHistory". Raise the "isFake" flag parameter
            // to "addSolveInternal", so it will ignore the given penalty and use the special
            // penalty code for fake records instead.
            addSolveInternal(
                    db, new Solve(1, puzzleType, newSolveCategory.toString(),
                            0L, "", Penalties.NO_PENALTIES, "", true),
                    true); // Mark this as a "fake" record.
            return 1;
        }

        // ... else a "fake" already exists for this solve category name, so none is created.
        return 0;
    }

    /**
     * Gets a list of all of the solve categories for the given puzzle type. There will be at least
     * one solve category in the list.
     *
     * @param puzzleType
     *     The puzzle type for which to get the solve categories.
     *
     * @return
     *     A list of all of the solve categories defined for the given puzzle type. The list will
     *     contain at least one element, as the default category will be added if it is not already
     *     present.
     */
    public List<String> getAllCategoriesForType(@NonNull PuzzleType puzzleType) {
        final List<String> categoriesList = new ArrayList<>();
        final Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT DISTINCT " + COL_CATEGORY + " FROM " + TABLE_TIMES
                        + " WHERE " + sqlMatchColsFakes(COL_PUZZLE_TYPE) // "fakes" only.
                        + " ORDER BY " + COL_CATEGORY + " ASC",
                new String[] { puzzleType.typeName() });

        try {
            while (cursor.moveToNext()) {
                categoriesList.add(cursor.getString(cursor.getColumnIndex(COL_CATEGORY)));
            }
        } finally {
            cursor.close();
        }

        if (categoriesList.isEmpty()) {
            // The default category should always have a "fake" record. It is added in "onUpgrade"
            // and it will be re-added by "deleteSolveCategory" if the last solve category is
            // deleted. Therefore, log this as suspicious, but add the required record anyway, so
            // that life can go on.
            Log.w(DatabaseHandler.class.getSimpleName(),
                    "Adding missing default solve category for: " + puzzleType.typeName());
            addSolveCategory(puzzleType, MainState.DEFAULT_SOLVE_CATEGORY);
            categoriesList.add(MainState.DEFAULT_SOLVE_CATEGORY);
        }

        return categoriesList;
    }

    /**
     * Deletes all solves times from a solve category, thus removing the category itself. If there
     * is only one solve category for the puzzle type and it is deleted, the default solve category
     * will be created, so at least one category exists.
     *
     * @param puzzleType
     *     The puzzle type for the solve category to be deleted.
     * @param solveCategoryToDelete
     *     The name of the solve category to be deleted.
     * @param currentSolveCategory
     *     The name of the currently-selected solve category. This may be the same as the category
     *     to be deleted.
     *
     * @return
     *     If the currently-selected solve category is not the same as the deleted solve category,
     *     then the currently-selected solve category is returned. If the two categories are the
     *     same, then the name of another, existing solve category will be returned. If the deleted
     *     category is the only remaining category, then the default category will be re-created
     *     and returned.
     */
    public String deleteSolveCategory(
            @NonNull PuzzleType puzzleType, @NonNull CharSequence solveCategoryToDelete,
            @NonNull String currentSolveCategory) {
        final SQLiteDatabase db = getWritableDatabase();

        // "fakes" *and* non-"fakes" are deleted, so do not use "sqlMatchCols*Fakes".
        db.delete(TABLE_TIMES, COL_PUZZLE_TYPE + "=? AND " + COL_CATEGORY + "=?",
                new String[] { puzzleType.typeName(), solveCategoryToDelete.toString() });

        // If there are no remaining solve categories for this puzzle type, create the default one.
        if (0 == DatabaseUtils.queryNumEntries(
                db, TABLE_TIMES, sqlMatchColsFakes(COL_PUZZLE_TYPE),
                new String[] { puzzleType.typeName() })) {
            addSolveCategory(puzzleType, MainState.DEFAULT_SOLVE_CATEGORY);

            return MainState.DEFAULT_SOLVE_CATEGORY;
        }

        if (currentSolveCategory.equals(solveCategoryToDelete)) {
            // If the currently-selected solve category has been deleted, then find a different,
            // existing category to use in its stead. At least one category will be returned.
            return getAllCategoriesForType(puzzleType).get(0);
        }

        return currentSolveCategory;
    }

    /**
     * Renames a solve category.
     *
     * @param puzzleType
     *     The puzzle type.
     * @param oldSolveCategory
     *     The old (existing) name of the solve category.
     * @param newSolveCategory
     *     The new name of the solve category.
     *
     * @return
     *     The number of solve times updated. If not zero, this may be one more than the number of
     *     times updated, as a special solve time record is used to hold the category name when no
     *     solve times have yet been recorded for that category.
     */
    public int renameSolveCategory(
            @NonNull PuzzleType puzzleType, @NonNull CharSequence oldSolveCategory,
            @NonNull CharSequence newSolveCategory) {
        // If the default category is renamed, then a "fake" category must be created to hold the
        // new non-default name, as a fake record is not used for the default category name. If not
        // added, and if all of the solves were deleted, then the new category name would be lost.
        if (MainState.DEFAULT_SOLVE_CATEGORY.equals(oldSolveCategory)) {
            addSolveCategory(puzzleType, newSolveCategory);
        }

        final ContentValues contentValues = new ContentValues();

        contentValues.put(COL_CATEGORY, newSolveCategory.toString());

        // "fake" solves are also updated, so do not use "sqlMatchCols", which would exclude them.
        return getWritableDatabase().update(
                TABLE_TIMES, contentValues, COL_PUZZLE_TYPE + "=? AND " + COL_CATEGORY + "=?",
                new String[] { puzzleType.typeName(), oldSolveCategory.toString() });
    }

    /**
     * Writes solves times in CSV format to the given output for all puzzle types and solve
     * categories. A CSV header is included and all values are quoted.
     *
     * @param out
     *     The output to which the CSV will be written. This will not be closed by this method. A
     *     buffered writer is recommended.
     * @param listener
     *     An optional progress listener that will be notified as each solve is written to the CSV
     *     output. Before the first solve is written, this will be called to report that zero of
     *     the total number of solves have been written (even if no solves exist to be written).
     *     Thereafter, it will be notified after each insertion. May be {@code null} if no progress
     *     updates are required.
     *
     * @return
     *     The total number of solve times written to the output.
     *
     * @throws IOException
     *     If there is an error writing to the output.
     */
    public int writeCSVBackup(@NonNull Writer out, ProgressListener listener) throws IOException {
        final CSVWriter csvOut = new CSVWriter(out, CSV_SEPARATOR);

        // Write a line before the header that contains the database version number. Just write it
        // as two CSV fields for easy parsing.
        csvOut.writeNext(
                new String[] { CSV_HEADER_DB_VERSION, Integer.toString(DATABASE_VERSION) }, false);

        // Write the header. No quotes needed. This is written even if there are no other rows.
        final String[] values = new String[] {
                "Puzzle", "Category", "Time(ms)", "Date(ms)", "Scramble", "Penalty", "Comment"
        };
        csvOut.writeNext(values, false);

        // NOTE: The behaviour is the same as before: "fake" times are not returned. If there is
        // a solve category defined by a "fake" time, but no real times with that solve category,
        // the the solve category will be lost in the "back-up". No big deal, really. However, when
        // restoring a back-up, the "fake" records must be created for each solve category (see
        // "addImportedSolves()").
        //
        // NOTE: Also the same as before, the "history" column is not included. Is that deliberate
        // or an oversight when "history" was added for the v.6 DB schema? No solves will be lost,
        // but the export/import return trip will archive all current session times to the history.
        final Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT " + COL_PUZZLE_TYPE + ", " + COL_CATEGORY + ", " + COL_TIME + ", "
                        + COL_DATE + ", " + COL_SCRAMBLE + ", " + COL_PENALTIES + ", " + COL_COMMENT
                        + " FROM " + TABLE_TIMES
                        + " WHERE " + sqlMatchColsNotFakes(), null);
        int numDumped = 0;

        try {
            final int total = cursor.getCount();

            if (listener != null) {
                listener.onProgress(numDumped, total);
            }

            if (total > 0) {
                while (cursor.moveToNext()) {
                    values[0] = cursor.getString(0);               // puzzle type
                    values[1] = cursor.getString(1);               // solve category
                    values[2] = String.valueOf(cursor.getInt(2));  // time (ms)
                    values[3] = String.valueOf(cursor.getLong(3)); // date (ms since epoch)
                    values[4] = cursor.getString(4);               // scramble
                    values[5] = String.valueOf(cursor.getInt(5));  // penalties (encoded integer)
                    values[6] = cursor.getString(6);               // comment
                    // "history" column is not written.

                    csvOut.writeNext(values, true); // Quote all the values.
                    numDumped++;

                    if (listener != null) {
                        listener.onProgress(numDumped, total);
                    }
                }
            }
        } finally {
            cursor.close();
        }

        return numDumped;
    }

    /**
     * Parses a back-up stream in CSV format to a list of {@code Solve} objects.
     *
     * @param in
     *     The input reader. This will not be closed by this method. A buffered reader is
     *     recommended.
     * @param parsedSolves
     *     The list to be populated with parsed solve objects. Parsed solves will be appended to
     *     any solves already in this list.
     *
     * @return
     *     The number of parse errors that occurred, with no more than one parse error reported
     *     per line of the CSV file, not including header lines.
     *
     * @throws IOException
     *     If there is an unexpected error reading the input, other than a simple parsing error.
     */
    @SuppressWarnings({"UnusedAssignment", "unused"}) // Database version is not used, for now.
    public static int parseCSVBackup(
            @NonNull Reader in, @NonNull List<Solve> parsedSolves) throws IOException {

        final CSVReader csvIn = new CSVReader(in, CSV_SEPARATOR);
        String[] values;

        // Parse the optional database version line (not present in back-up from older app versions
        // prior to DB version 10). If the DB version line it is not present, then the first line
        // is the header line. If the version is present, the second line is the header line. Either
        // way, the header line is always assumed to be present and is always skipped.
        //
        // All database versions before 10 are essentially the same. The "history" field may not be
        // in the database, but it is not in the CSV file either. If the version is missing, assume
        // 9, as it only started to be written to the CSV from version 10 onwards.
        int databaseVersion = 9;

        values = csvIn.readNext();

        if (values != null && values.length > 0) {
            // First (optional) line may be "TwistyTimerDatabaseVersion,123", or something.
            if (CSV_HEADER_DB_VERSION.equals(values[0])) {
                try {
                    databaseVersion = Integer.parseInt(values[1]);
                } catch (Exception e) {
                    // Could be out-of-bounds or parsing error, but the version header is there,
                    // so assume the earliest version that would have been written to the file.
                    // Do not use "DATABASE_VERSION", as it might change; just use a literal 10.
                    databaseVersion = 10;
                }
                // Skip the header line that comes after the version line.
                csvIn.readNext();
            } // else that was probably the header line, so consider it skipped.
        } // else first line was blank or file is empty. Not expected, but just keep going.

        int numParseErrors = 0;

        while ((values = csvIn.readNext()) != null) {
            try {
                final PuzzleType puzzleType = PuzzleType.forTypeName(values[0]);
                final String solveCategory = values[1];
                long time = Long.parseLong(values[2]); // time (ms)
                final int penaltyCode = Integer.parseInt(values[5]);

                // It will be old data if the DB version is not set in the header. In that case,
                // apply the same old-penalty-code fixes as in "upgrade9to10".
                final Penalties penalties;

                if (databaseVersion < 10) {
                    penalties = upgradePenalties(penaltyCode, time); // May throw IAE.
                    // Complete the upgrade by adding the "+2" penalty that *must* have occurred
                    // before an inspection time-out DNF penalty.
                    if (penalties.hasPreStartDNF() && time == 0) {
                        time += Penalties.PLUS_TWO_DURATION_MS;
                    }
                } else {
                    penalties = Penalties.decode(penaltyCode); // May throw IAE.
                }

                // Indices are same as those used in "toCSVBackup".
                parsedSolves.add(new Solve(
                        time,
                        puzzleType,
                        solveCategory,
                        Long.parseLong(values[3]), // date (ms since epoch)
                        values[4],                 // scramble
                        penalties,
                        values[6],                 // comment
                        true));                    // history (not written, assume true)
            } catch (Exception e) {
                // Problem parsing integers, or wrong number of fields on a line.
                numParseErrors++;
            }
        }

        return numParseErrors;
    }

    /**
     * Writes solves times in CSV format to the given output. No CSV header is included and all
     * values are quoted. This CVS includes a short summary of the key fields. For a full database
     * dump suitable for back-up purposes, use {@link #writeCSVBackup(Writer, ProgressListener)}
     * instead.
     *
     * @param out
     *     The output to which the CSV will be written. This will not be closed by this method. A
     *     buffered writer is recommended.
     * @param puzzleType
     *     The type of the puzzle for which solve times should be written to the CSV.
     * @param solveCategory
     *     The solve category for which solve times should be written to the CSV.
     * @param listener
     *     An optional progress listener that will be notified as each solve is written to the CSV
     *     output. Before the first solve is written, this will be called to report that zero of
     *     the total number of solves have been written (even if no solves exist to be written).
     *     Thereafter, it will be notified after each insertion. May be {@code null} if no progress
     *     updates are required.
     *
     * @return
     *     The total number of solve times written to the output.
     *
     * @throws IOException
     *     If there is an error writing to the output.
     */
    public int writeCSVExternal(
            @NonNull Writer out, @NonNull PuzzleType puzzleType, @NonNull String solveCategory,
            ProgressListener listener) throws IOException {
        final CSVWriter csvOut = new CSVWriter(out, CSV_SEPARATOR);
        final String[] valuesWithoutDNF = new String[3];
        final String[] valuesWithDNF = new String[4];
        String[] values;
        int numDumped = 0;

        final Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT " + COL_TIME + ", " + COL_SCRAMBLE + ", " + COL_DATE + ", " + COL_PENALTIES
                        + " FROM " + TABLE_TIMES
                        + " WHERE " + sqlMatchColsNotFakes(COL_PUZZLE_TYPE, COL_CATEGORY),
                new String[] { puzzleType.typeName(), solveCategory });

        try {
            final int total = cursor.getCount();

            if (listener != null) {
                listener.onProgress(numDumped, total);
            }

            if (total > 0) {
                while (cursor.moveToNext()) {
                    // Add optional "DNF" in fourth field and pick appropriate size values array.
                    // Any record of "+2" penalties are lost, but the extra time is preserved.
                    if (Penalties.decode(cursor.getInt(3)).hasDNF()) {
                        values = valuesWithDNF;
                        values[3] = CSV_EXTERNAL_FORMAT_DNF_MARKER;
                    } else {
                        values = valuesWithoutDNF;
                    }

                    // Format times to a precision of 1 ms, the same as in the database.
                    values[0] = TimeUtils.formatTimeExternal(cursor.getInt(0));
                    values[1] = cursor.getString(1);
                    // By default "DateTime.toString()" applies the ISO 8601 format specified as
                    // "yyyy-MM-ddTHH:mm:ss.SSSZZ". This has the required millisecond precision.
                    // However, it is safer (and easier to follow) if the format is chosen
                    // explicitly in this code rather than relying on the default.
                    values[2] = new DateTime(cursor.getLong(2))
                            .toString(ISODateTimeFormat.dateTime());

                    csvOut.writeNext(values, true);
                    numDumped++;

                    if (listener != null) {
                        listener.onProgress(numDumped, total);
                    }
                }
            }
        } finally {
            cursor.close();
        }

        return numDumped;
    }

    /**
     * Parses an external-format stream in CSV format to a list of {@code Solve} objects.
     *
     * @param in
     *     The input reader. This will not be closed by this method. A buffered reader is
     *     recommended.
     * @param puzzleType
     *     The type of the puzzle to which solve times should be assigned when parsed.
     * @param solveCategory
     *     The solve category to which solve times should be assigned when parsed.
     * @param parsedSolves
     *     The list to be populated with parsed solve objects. Parsed solves will be appended to
     *     any solves already in this list.
     *
     * @return
     *     The number of parse errors that occurred, with no more than one parse error reported
     *     per line of the CSV file.
     *
     * @throws IOException
     *     If there is an unexpected error reading the input, other than a simple parsing error.
     */
    @SuppressWarnings({"UnusedAssignment", "unused"}) // Database version is not used, for now.
    public static int parseCSVExternal(
            @NonNull Reader in, @NonNull PuzzleType puzzleType, @NonNull String solveCategory,
            @NonNull List<Solve> parsedSolves) throws IOException {

        final CSVReader csvIn = new CSVReader(in, CSV_SEPARATOR);
        final long now = DateTime.now().getMillis();
        int numParseErrors = 0;
        String[] values;

        // There is no database version or header line expected. However, it is known that a "DNF"
        // marker was first added at database version 10, the same time there was a change to using
        // "Penalties.encode()/decode()".
        while ((values = csvIn.readNext()) != null) {
            if (values.length <= 4) {
                try {
                    // Parse the time to the nearest millisecond, so duplicates can be detected.
                    long time = TimeUtils.parseTimeExternal(values[0]);
                    final String scramble = values.length >= 2 ? values[1] : null;

                    long date = now;

                    if (values.length >= 3) {
                        try {
                            // A strict format is defined for exporting. However, for importing,
                            // just let the parser handle whatever approximation to ISO 8601 is
                            // given. It will be more lenient, allowing seconds to be optional,
                            // for example, which makes hand-written files easier to manage.
                            date = DateTime.parse(values[2]).getMillis();
                        } catch (Exception e) {
                            // "date" remains equal to "now".
                            Log.e(TAG, "Unexpected error parsing date: " + values[2], e);
                        }
                    }

                    // Optional fourth field may contain "DNF". If it is something else, ignore it.
                    final Penalties penalties;

                    if (values.length >= 4 && CSV_EXTERNAL_FORMAT_DNF_MARKER.equals(values[3])) {
                        // If there is a "DNF" and the time is exactly 2,000 ms, that is probably
                        // a pre-start DNF for an inspection time-out that also incurred a +2
                        // penalty (i.e., it was exported from a version 10+ database). However,
                        // that is not certain, as the time might be just coincidentally that value
                        // and the user may have incurred the DNF manually at the post-start phase.
                        // If the time is zero and there is a DNF, then the inference should be
                        // safe to make and the new +2 penalty can be added to the solve and the
                        // zero time also incremented to 2,000 ms.
                        if (time == 0) {
                            penalties = Penalties.NO_PENALTIES
                                    .incurPreStartPenalty(Penalty.DNF)
                                    .incurPreStartPenalty(Penalty.PLUS_TWO);
                            time += Penalties.PLUS_TWO_DURATION_MS;
                        } else {
                            penalties = Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.DNF);
                        }
                    } else {
                        penalties = Penalties.NO_PENALTIES;
                    }

                    parsedSolves.add(new Solve(time, puzzleType, solveCategory,
                            date, scramble, penalties, null, true));
                } catch (Exception e) {
                    numParseErrors++;
                }
            } else {
                // Expected no more than four fields.
                numParseErrors++;
            }
        }

        return numParseErrors;
    }

    // TODO: this info should REALLY be in a separate file. I'll get to it when I add other alg sets.

    private static void createInitialAlgs(SQLiteDatabase db) {
        // OLL
        createAlg(db, SUBSET_OLL, "OLL 01", "NNNNYNNNNNYNYYYNYNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 01"));
        createAlg(db, SUBSET_OLL, "OLL 02", "NNNNYNNNNNYYNYNYYNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 02"));
        createAlg(db, SUBSET_OLL, "OLL 03", "NNNNYNYNNYYNYYNYYNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 03"));
        createAlg(db, SUBSET_OLL, "OLL 04", "NNNNYNNNYNYYNYNNYYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 04"));
        createAlg(db, SUBSET_OLL, "OLL 05", "NNNNYYNYYYYNYNNNNNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 05"));
        createAlg(db, SUBSET_OLL, "OLL 06", "NYYNYYNNNNNNNNYNYYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 06"));
        createAlg(db, SUBSET_OLL, "OLL 07", "NYNYYNYNNYNNYYNYYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 07"));
        createAlg(db, SUBSET_OLL, "OLL 08", "NYNNYYNNYNNYNNNNYYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 08"));
        createAlg(db, SUBSET_OLL, "OLL 09", "NNYYYNNYNNYNNYYNNYNNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 09"));
        createAlg(db, SUBSET_OLL, "OLL 10", "NNYYYNNYNYYNNYNYNNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 10"));
        createAlg(db, SUBSET_OLL, "OLL 11", "NNNNYYYYNYYNYNNYNNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 11"));
        createAlg(db, SUBSET_OLL, "OLL 12", "NNYNYYNYNNYNNNYNNYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 12"));
        createAlg(db, SUBSET_OLL, "OLL 13", "NNNYYYYNNYYNYNNYYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 13"));
        createAlg(db, SUBSET_OLL, "OLL 14", "NNNYYYNNYNYYNNNNYYNNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 14"));
        createAlg(db, SUBSET_OLL, "OLL 15", "NNNYYYNNYYYNYNNNYNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 15"));
        createAlg(db, SUBSET_OLL, "OLL 16", "NNYYYYNNNNYNNNYNYYNNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 16"));
        createAlg(db, SUBSET_OLL, "OLL 17", "YNNNYNNNYNYYNYNNYNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 17"));
        createAlg(db, SUBSET_OLL, "OLL 18", "YNYNYNNNNNYNNYNYYYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 18"));
        createAlg(db, SUBSET_OLL, "OLL 19", "YNYNYNNNNNYNNYYNYNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 19"));
        createAlg(db, SUBSET_OLL, "OLL 20", "YNYNYNYNYNYNNYNNYNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 20"));
        createAlg(db, SUBSET_OLL, "OLL 21", "NYNYYYNYNNNNYNYNNNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 21"));
        createAlg(db, SUBSET_OLL, "OLL 22", "NYNYYYNYNNNYNNNYNNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 22"));
        createAlg(db, SUBSET_OLL, "OLL 23", "YYYYYYNYNNNNNNNYNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 23"));
        createAlg(db, SUBSET_OLL, "OLL 24", "NYYYYYNYYYNNNNNNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 24"));
        createAlg(db, SUBSET_OLL, "OLL 25", "YYNYYYNYYNNNYNNNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 25"));
        createAlg(db, SUBSET_OLL, "OLL 26", "YYNYYYNYNNNYNNYNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 26"));
        createAlg(db, SUBSET_OLL, "OLL 27", "NYNYYYYYNYNNYNNYNNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 27"));
        createAlg(db, SUBSET_OLL, "OLL 28", "YYYYYNYNYNNNNYNNYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 28"));
        createAlg(db, SUBSET_OLL, "OLL 29", "YNYYYNNYNNYNNYYNNNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 29"));
        createAlg(db, SUBSET_OLL, "OLL 30", "YNYNYYNYNNYNNNYNNNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 30"));
        createAlg(db, SUBSET_OLL, "OLL 31", "NYYNYYNNYYNNNNNNYYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 31"));
        createAlg(db, SUBSET_OLL, "OLL 32", "NNYNYYNYYYYNNNNNNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 32"));
        createAlg(db, SUBSET_OLL, "OLL 33", "NNYYYYNNYYYNNNNNYYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 33"));
        createAlg(db, SUBSET_OLL, "OLL 34", "YNYYYYNNNNYNNNYNYNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 34"));
        createAlg(db, SUBSET_OLL, "OLL 35", "YNNNYYNYYNYNYNNNNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 35"));
        createAlg(db, SUBSET_OLL, "OLL 36", "YNNYYNNYYNYNYYNNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 36"));
        createAlg(db, SUBSET_OLL, "OLL 37", "YYNYYNNNYNNNYYNNYYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 37"));
        createAlg(db, SUBSET_OLL, "OLL 38", "NYYYYNYNNYNNNYYNYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 38"));
        createAlg(db, SUBSET_OLL, "OLL 39", "YYNNYNNYYNNYNYNNNNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 39"));
        createAlg(db, SUBSET_OLL, "OLL 40", "NYYNYNYYNNNNNYNYNNNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 40"));
        createAlg(db, SUBSET_OLL, "OLL 41", "YNYNYYNYNNYNNNNYNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 41"));
        createAlg(db, SUBSET_OLL, "OLL 42", "YNYYYNNYNNYNNYNYNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 42"));
        createAlg(db, SUBSET_OLL, "OLL 43", "YNNYYNYYNNYNYYYNNNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 43"));
        createAlg(db, SUBSET_OLL, "OLL 44", "NNYNYYNYYNYNNNNNNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 44"));
        createAlg(db, SUBSET_OLL, "OLL 45", "NNYYYYNNYNYNNNNNYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 45"));
        createAlg(db, SUBSET_OLL, "OLL 46", "YYNNYNYYNNNNYYYNNNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 46"));
        createAlg(db, SUBSET_OLL, "OLL 47", "NYNNYYNNNYNNYNYNYYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 47"));
        createAlg(db, SUBSET_OLL, "OLL 48", "NYNYYNNNNNNYNYNYYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 48"));
        createAlg(db, SUBSET_OLL, "OLL 49", "NNNYYNNYNYYNYYYNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 49"));
        createAlg(db, SUBSET_OLL, "OLL 50", "NNNNYYNYNNYYNNNYNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 50"));
        createAlg(db, SUBSET_OLL, "OLL 51", "NNNYYYNNNNYYNNNYYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 51"));
        createAlg(db, SUBSET_OLL, "OLL 52", "NYNNYNNYNYNNYYYNNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 52"));
        createAlg(db, SUBSET_OLL, "OLL 53", "NNNNYYNYNNYNYNYNNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 53"));
        createAlg(db, SUBSET_OLL, "OLL 54", "NYNNYYNNNNNNYNYNYNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 54"));
        createAlg(db, SUBSET_OLL, "OLL 55", "NYNNYNNYNNNNYYYNNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 55"));
        createAlg(db, SUBSET_OLL, "OLL 56", "NNNYYYNNNNYNYNYNYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 56"));
        createAlg(db, SUBSET_OLL, "OLL 57", "YNYYYYYNYNYNNNNNYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 57"));

        // PLL
        createAlg(db, SUBSET_PLL, "H", "YYYYYYYYYOROGBGRORBGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "H"));
        createAlg(db, SUBSET_PLL, "Ua", "YYYYYYYYYOBOGOGRRRBGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ua"));
        createAlg(db, SUBSET_PLL, "Ub", "YYYYYYYYYOGOGBGRRRBOB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ub"));
        createAlg(db, SUBSET_PLL, "Z", "YYYYYYYYYOBOGRGRGRBOB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Z"));
        createAlg(db, SUBSET_PLL, "Aa", "YYYYYYYYYGOGRGBORRBBO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Aa"));
        createAlg(db, SUBSET_PLL, "Ab", "YYYYYYYYYOORBGOGRGRBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Aa"));
        createAlg(db, SUBSET_PLL, "E", "YYYYYYYYYGOBOGRBRGRBO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "E"));
        createAlg(db, SUBSET_PLL, "F", "YYYYYYYYYGOBOBGRRRBGO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "F"));
        createAlg(db, SUBSET_PLL, "Ga", "YYYYYYYYYRBOGGRBOBORG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ga"));
        createAlg(db, SUBSET_PLL, "Gb", "YYYYYYYYYBROGGBOBGROR", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gb"));
        createAlg(db, SUBSET_PLL, "Gc", "YYYYYYYYYOGRBROGOGRBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gc"));
        createAlg(db, SUBSET_PLL, "Gd", "YYYYYYYYYORGRORBGOGBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gd"));
        createAlg(db, SUBSET_PLL, "Ja", "YYYYYYYYYBOOGGGRBBORR", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ja"));
        createAlg(db, SUBSET_PLL, "Jb", "YYYYYYYYYOOGRROGGRBBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Jb"));
        createAlg(db, SUBSET_PLL, "Na", "YYYYYYYYYOORBBGRROGGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Na"));
        createAlg(db, SUBSET_PLL, "Nb", "YYYYYYYYYROOGBBORRBGG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Nb"));
        createAlg(db, SUBSET_PLL, "Ra", "YYYYYYYYYOGOGORBRGRBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ra"));
        createAlg(db, SUBSET_PLL, "Rb", "YYYYYYYYYGOBORGRGRBBO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Rb"));
        createAlg(db, SUBSET_PLL, "T", "YYYYYYYYYOOGRBOGRRBGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "T"));
        createAlg(db, SUBSET_PLL, "V", "YYYYYYYYYRGOGOBORRBBG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "V"));
        createAlg(db, SUBSET_PLL, "Y", "YYYYYYYYYRBOGGBORRBOG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Y"));
    }
}
