package com.aricneto.twistytimer.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.AppRater;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.fragment.BaseMainFragment;
import com.aricneto.twistytimer.fragment.TimerMainFragment;
import com.aricneto.twistytimer.fragment.dialog.EditSolveDialog;
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog;
import com.aricneto.twistytimer.fragment.dialog.PuzzleChooserDialog;
import com.aricneto.twistytimer.fragment.dialog.SchemeSelectDialogMain;
import com.aricneto.twistytimer.fragment.dialog.ThemeSelectDialog;
import com.aricneto.twistytimer.items.PuzzleType;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.utils.ExImUtils;
import com.aricneto.twistytimer.utils.ExImUtils.FileFormat;
import com.aricneto.twistytimer.utils.FireAndForgetExecutor;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.StoreUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.aricneto.twistytimer.database.DatabaseHandler
    .ProgressListener;
import static com.aricneto.twistytimer.database.DatabaseHandler.SUBSET_OLL;
import static com.aricneto.twistytimer.database.DatabaseHandler.SUBSET_PLL;
import static com.aricneto.twistytimer.utils.TTIntent.*;

public class MainActivity extends AppCompatActivity
        implements BillingProcessor.IBillingHandler,
                   FileChooserDialog.FileCallback,
                   ExportImportDialog.ExportImportCallbacks,
                   PuzzleChooserDialog.PuzzleCallback,
        EditSolveDialog.EditSolveCallbacks {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    // IDs for the items in the "drawer".
    private static final int TIMER_ID         = 1;
    private static final int THEME_ID         = 2;
    private static final int ABOUT_ID         = 4;
    private static final int SETTINGS_ID      = 5;
    private static final int OLL_ID           = 6;
    private static final int PLL_ID           = 7;
    private static final int DONATE_ID        = 8;
    private static final int SCHEME_ID        = 9;
    private static final int EXPORT_IMPORT_ID = 10;
//    private static final int DEBUG_ID         = 11;

    private static final int REQUEST_SETTING           = 42;
    private static final int REQUEST_ABOUT             = 23;
    private static final int STORAGE_PERMISSION_CODE   = 11;

    /**
     * The fragment tag identifying the export/import dialog fragment.
     */
    private static final String FRAG_EXIM_DIALOG = "export_import_dialog";

    /**
     * The tag used to identify the main timer fragment that manages the tabs
     * and the other fragments contained in those tabs.
     */
    private static final String FRAG_TIMER_MAIN = "timer_main_fragment";

    /**
     * The tag used to identify the fragment displaying the list of OLL
     * algorithms.
     */
    private static final String FRAG_OLL_ALGS_LIST = "oll_algs_list_fragment";

    /**
     * The tag used to identify the fragment displaying the list of PLL
     * algorithms.
     */
    private static final String FRAG_PLL_ALGS_LIST = "pll_algs_list_fragment";

    /**
     * The tag used to identify the dialog fragment for choosing the theme for
     * the user interface.
     */
    private static final String FRAG_THEME_DIALOG = "theme_dialog";

    /**
     * The tag used to identify the dialog fragment for choosing the color
     * scheme of the puzzles.
     */
    private static final String FRAG_COLOR_SCHEME_DIALOG
        = "color_scheme_dialog";

    /**
     * The tag used to identify the dialog fragment for editing a solve time.
     */
    private static final String FRAG_EDIT_SOLVE_DIALOG = "edit_solve_dialog";

    // NOTE: Loader IDs used by fragments need to be unique within the context
    // of an activity that creates those fragments. Therefore, it is safer to
    // define all of the IDs in the same place.

    /**
     * The loader ID for the loader that loads data presented in the statistics
     * table on the timer graph fragment and the summary statistics on the timer
     * fragment.
     */
    public static final int STATISTICS_LOADER_ID = 101;

    /**
     * The loader ID for the loader that loads chart data presented in on the
     * timer graph fragment.
     */
    public static final int CHART_DATA_LOADER_ID = 102;

    /**
     * The loader ID for the loader that loads (i.e., generates) scramble
     * sequences for the timer fragment.
     */
    public static final int SCRAMBLE_LOADER_ID = 103;

    /**
     * The loader ID for the loader that loads (i.e., generates) scramble image
     * sequences for the timer fragment.
     */
    public static final int SCRAMBLE_IMAGE_LOADER_ID = 104;

    /**
     * The loader ID for the loader that loads the list of solve times for the
     * timer list fragment. */
    public static final int TIME_LIST_LOADER_ID = 105;

    /**
     * The loader ID for the loader that loads the list of algorithms for the
     * algorithm list fragment.
     */
    public static final int ALG_LIST_LOADER_ID = 106;

    /**
     * The main state information for the application. This is persisted when
     * the application is closed and maintained in the saved instance state
     * across configuration changes.
     */
    private MainState mMainState;

    private BillingProcessor mBillingProcessor;

    private SmoothActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout                mDrawerLayout;
    private Drawer                      mDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState="
                + savedInstanceState + "): " + this);

        // Set theme before "super.onCreate".
        setTheme(ThemeUtils.getPreferredThemeStyleResID());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        AppRater.app_launched(this);

        mBillingProcessor = new BillingProcessor(this, null, this);

        if (savedInstanceState != null) {
            // If no main state was saved, it will be restored as "null"; that
            // is fixed below.
            mMainState = MainState.restoreFromBundle(savedInstanceState);
        }

        if (mMainState == null) {
            // Main state was not restored from the instance state (or there was
            // no instance state), so restore it from the shared preferences.
            // The "Prefs" class will restore a sensible default state if none
            // was previously saved.
            mMainState = Prefs.getLastUsedMainState();
        }

        if (savedInstanceState == null) {
            getMainFragManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container,
                            TimerMainFragment.newInstance(), FRAG_TIMER_MAIN)
                    .commit();
        }

        setUpDrawer(savedInstanceState);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState(): " + this);

        mDrawer.saveInstanceState(outState);
        mMainState.saveToBundle(outState);
        super.onSaveInstanceState(outState);
    }

    /* NOTE: Leaving this here (commented out) as it may be useful again
       (probably soon).
    @Override
    protected void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume(): " + this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onPause(): " + this);
        super.onPause();
    }

    @Override
    protected void onStart() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onStart(): " + this);
        super.onStart();
    }
    */

    @Override
    protected void onStop() {
        if (DEBUG_ME) Log.d(TAG, "onStop(): " + this);
        super.onStop();

        // Persist the main state in case the app is about to exit. This will
        // be restored in "onCreate" the next time the app is started from
        // scratch.
        Prefs.edit().putLastUsedMainState(mMainState).apply();
    }

    /**
     * Gets the main state information for this activity and its dependent
     * fragments. It is an error to attempt to retrieve this state before it
     * has been initialised by {@code onCreate}.
     *
     * @return
     *     The main state information.
     *
     * @throws IllegalStateException
     *     If the main state has not yet been initialised.
     */
    @NonNull
    public MainState getMainState() {
        if (mMainState == null) {
            throw new IllegalStateException(
                "Attempt to access MainState before it has been initialised.");
        }

        return mMainState;
    }

    /**
     * Notifies all relevant fragments that the main state information has
     * changed. This should be called by this activity or one of its fragments
     * when the state is changed via the user interface. All fragments attached
     * to this activity that extend {@link BaseMainFragment} will be notified
     * of the change. It is an error to attempt to fire a change of state before
     * the activity's main state has been initialised by {@code onCreate}. No
     * notification will be sent if the new state is equal to the old state.
     *
     * @param newMainState
     *     The new main state information.
     *
     * @throws IllegalStateException
     *     If this activity's main state has not yet been initialised.
     */
    public void fireOnMainStateChanged(@NonNull MainState newMainState) {
        if (DEBUG_ME) Log.d(TAG, "fireOnMainStateChanged("
                                 + newMainState + "): " + this);

        final MainState oldMainState = getMainState();

        if (!newMainState.equals(oldMainState)) {
            // Persist the new main state. If the user switches from, say,
            // "3x3x3:MyCategory" to "2x2x2:Normal", "MyCategory" will be
            // remembered on returning to "3x3x3".
            Prefs.edit().putLastUsedMainState(newMainState).apply();
            mMainState = newMainState;

            for (Fragment fragment : getMainFragManager().getFragments()) {
                if (fragment instanceof BaseMainFragment
                        && fragment.isAdded()) {
                    ((BaseMainFragment) fragment).onMainStateChanged(
                        newMainState, oldMainState);
                }
            }
        }
    }

    /**
     * Gets the fragment manager (which may be the "support" fragment manager,
     * if necessary).
     *
     * @return The fragment manager.
     */
    // NOTE: This method avoid the need for an extra field, which may cause
    // confusion about which state information needs to be persisted; and it
    // abstracts the support/non-support decision.
    FragmentManager getMainFragManager() {
        return getSupportFragmentManager();
    }

    private void setUpDrawer(Bundle savedInstanceState) {
        final ImageView headerView = (ImageView) View.inflate(
            this, R.layout.drawer_header, null);

        headerView.setImageDrawable(ThemeUtils.tintDrawable(
            this, R.drawable.header, R.attr.colorPrimary));

        // Set up sliding drawer
        mDrawer = new DrawerBuilder()
            .withActivity(this)
            .withDelayOnDrawerClose(- 1)
            .withHeader(headerView)
            .addDrawerItems(
                // Selectable, so not "createPrimaryDrawerItem".
                new PrimaryDrawerItem()
                    .withName(R.string.drawer_title_timer)
                    .withIcon(R.drawable.ic_timer_black_24dp)
                    .withIconTintingEnabled(true)
                    .withIdentifier(TIMER_ID),

                new ExpandableDrawerItem()
                    .withName(R.string.drawer_title_reference)
                    .withIcon(R.drawable.ic_cube_unfolded_black_24dp)
                    .withSelectable(false)
                    .withIconTintingEnabled(true)
                    .withSubItems(
                        new SecondaryDrawerItem()
                            .withName(R.string.drawer_title_oll)
                            .withLevel(2)
                            .withIcon(R.drawable.ic_oll_black_24dp)
                            .withIconTintingEnabled(true)
                            .withIdentifier(OLL_ID),
                        new SecondaryDrawerItem()
                            .withName(R.string.drawer_title_pll)
                            .withLevel(2)
                            .withIcon(R.drawable.ic_pll_black_24dp)
                            .withIconTintingEnabled(true)
                            .withIdentifier(PLL_ID)),

                new SectionDrawerItem()
                    .withName(R.string.drawer_title_other),

                createPrimaryDrawerItem(EXPORT_IMPORT_ID,
                    R.string.drawer_title_export_import,
                    R.drawable.ic_folder_open_black_24dp),
                createPrimaryDrawerItem(THEME_ID,
                    R.string.drawer_title_changeTheme,
                    R.drawable.ic_brush_black_24dp),
                createPrimaryDrawerItem(SCHEME_ID,
                    R.string.drawer_title_changeColorScheme,
                    R.drawable.ic_scheme_black_24dp),

                new DividerDrawerItem(),

                createPrimaryDrawerItem(SETTINGS_ID,
                    R.string.action_settings,
                    R.drawable.ic_action_settings_black_24),
                createPrimaryDrawerItem(DONATE_ID,
                    R.string.action_donate, R.drawable.ic_mood_black_24dp),
                createPrimaryDrawerItem(ABOUT_ID,
                    R.string.drawer_about, R.drawable.ic_action_help_black_24)

//                ,new PrimaryDrawerItem()
//                        .withName("DEBUG OPTION - ADD 10000 SOLVES")
//                        .withIcon(R.drawable.ic_action_help_black_24)
//                        .withIconTintingEnabled(true)
//                        .withSelectable(false)
//                        .withIdentifier(DEBUG_ID)

            )
            .withOnDrawerItemClickListener(
                new Drawer.OnDrawerItemClickListener() {
                @Override
                public boolean onItemClick(View view, int position,
                                           IDrawerItem drawerItem) {
                    boolean closeDrawer = true;

                    switch ((int) drawerItem.getIdentifier()) {
                        default:
                            closeDrawer = false;
                            // fall through
                        case TIMER_ID:
                            replaceMainFragmentWhenIdle(
                                TimerMainFragment.newInstance(),
                                FRAG_TIMER_MAIN);
                            break;

                        case OLL_ID:
                            replaceMainFragmentWhenIdle(
                                AlgListFragment.newInstance(SUBSET_OLL),
                                FRAG_OLL_ALGS_LIST);
                            break;

                        case PLL_ID:
                            replaceMainFragmentWhenIdle(
                                AlgListFragment.newInstance(SUBSET_PLL),
                                FRAG_PLL_ALGS_LIST);
                            break;

                        case EXPORT_IMPORT_ID:
                            // Will check permissions first.
                            startExportImportDialog();
                            break;

                        case THEME_ID:
                            ThemeSelectDialog.newInstance()
                                .show(getMainFragManager(), FRAG_THEME_DIALOG);
                            break;

                        case SCHEME_ID:
                            SchemeSelectDialogMain.newInstance()
                                .show(getMainFragManager(),
                                      FRAG_COLOR_SCHEME_DIALOG);
                            break;

                        case SETTINGS_ID:
                            startActivityForResultWhenIdle(
                                SettingsActivity.class, REQUEST_SETTING);
                            break;

                        case DONATE_ID:
                            startDonationDialog();
                            break;

                        case ABOUT_ID:
                            startActivityForResultWhenIdle(
                                AboutActivity.class, REQUEST_ABOUT);
                            break;

                        //case DEBUG_ID:
                        //    Random rand = new Random();
                        //    DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
                        //    for (int i = 0; i < 10000; i++) {
                        //        dbHandler.addSolve(new Solve(30000 + rand.nextInt(2000),
                        //                PuzzleType.TYPE_333, PuzzleUtils.CATEGORY_NORMAL,
                        //                165165l, "", 0, "", rand.nextBoolean()));
                        //    }
                        //    break;
                    }

                    if (closeDrawer) {
                        mDrawerLayout.closeDrawers();
                    }

                    return false;
                }
            })
            .withSavedInstance(savedInstanceState)
            .build();

        mDrawerLayout = mDrawer.getDrawerLayout();
        mDrawerToggle = new SmoothActionBarDrawerToggle(
            this, mDrawerLayout, null, R.string.drawer_open,
            R.string.drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    /**
     * Opens the drawer at the side of the screen. This is called by the main
     * fragment when the menu button (top left) is clicked.
     */
    public void openDrawer() {
        mDrawer.openDrawer();
    }

    /**
     * Creates a new primary drawer item that is not selectable and with a
     * tinted icon.
     *
     * @param itemID
     *     The ID identifying this drawer item when it is clicked.
     * @param nameResID
     *     The string resource ID for the name of the drawer item.
     * @param drawableResID
     *     The drawable resource ID for the icon of the drawer item.
     *
     * @return The new drawer item.
     */
    private PrimaryDrawerItem createPrimaryDrawerItem(
            int itemID, @StringRes int nameResID,
            @DrawableRes int drawableResID) {
        return new PrimaryDrawerItem()
                .withName(nameResID)
                .withIcon(drawableResID)
                .withIconTintingEnabled(true)
                .withSelectable(false)
                .withIdentifier(itemID);
    }

    /**
     * Replaces the current main fragment with a new main fragment when the
     * drawer becomes idle. This ensures a smoother transition.
     *
     * @param newFragment
     *     The new fragment instance.
     * @param fragmentTag
     *     The tag that identifies this fragment in the fragment manager.
     */
    private void replaceMainFragmentWhenIdle(
            @NonNull final Fragment newFragment, final String fragmentTag) {
        // Wait for the drawer to close before replacing the main fragment.
        mDrawerToggle.runWhenIdle(new Runnable() {
            @Override
            public void run() {
                getMainFragManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container,
                        newFragment, fragmentTag)
                    .commit();
            }
        });
    }

    /**
     * Starts a new activity when the drawer becomes idle. This ensures a
     * smoother transition.
     *
     * @param activityClass
     *     The class of the activity to start.
     * @param requestCode
     *     The request code to identify the activity when its result is
     *     returned.
     */
    private void startActivityForResultWhenIdle(
            @NonNull final Class<?> activityClass, final int requestCode) {
        // Wait for the drawer to close before starting the activity.
        mDrawerToggle.runWhenIdle(new Runnable() {
            @Override
            public void run() {
                startActivityForResult( new Intent(
                    getApplicationContext(), activityClass), requestCode);
            }
        });
    }

    private void startExportImportDialog() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, WRITE_EXTERNAL_STORAGE)) {

                new MaterialDialog.Builder(this)
                    .content(R.string.permission_denied_explanation)
                    .positiveText(R.string.action_ok)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[] { WRITE_EXTERNAL_STORAGE },
                                STORAGE_PERMISSION_CODE);
                        }
                    })
                    .show();
            } else {
                ActivityCompat.requestPermissions(
                    this, new String[] { WRITE_EXTERNAL_STORAGE },
                    STORAGE_PERMISSION_CODE);
            }
        } else {
            ExportImportDialog.newInstance()
                .show(getMainFragManager(), FRAG_EXIM_DIALOG);
        }
    }

    private void startDonationDialog() {
        if (BillingProcessor.isIabServiceAvailable(this)) {
            new MaterialDialog.Builder(this)
                .title(R.string.choose_donation_amount)
                .items(R.array.donation_tiers)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog,
                                            View itemView, int which,
                                            CharSequence text) {
                        mBillingProcessor.purchase(
                            MainActivity.this, text.toString());
                    }
                })
                .show();
        } else {
            Toast.makeText(this, "Google Play not available.",
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when BillingProcessor was initialized and it is ready to purchase.
     */
    @Override
    public void onBillingInitialized() {
    }

    /**
     * Called when requested PRODUCT ID was successfully purchased.
     */
    @Override
    public void onProductPurchased(String productId,
                                   TransactionDetails details) {
        mBillingProcessor.consumePurchase(productId);
    }

    /**
     * Called when some error occurred. See Constants class for more details.
     */
    @Override
    public void onBillingError(int errorCode, Throwable error) {
    }

    /**
     * Called when purchase history was restored and the list of all owned
     * PRODUCT ID's was loaded from Google Play.
     */
    @Override
    public void onPurchaseHistoryRestored() {
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (DEBUG_ME) Log.d(TAG, "onActivityResult(requestCode=" + requestCode
            + ", resultCode=" + resultCode + ", data=" + data + "): " + this);

        if (!mBillingProcessor.handleActivityResult(
                requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_SETTING) {
                if (DEBUG_ME) Log.d(TAG,
                    "  Returned from 'Settings'. Will recreate activity.");
                onRecreateRequired();
            }
        }
    }

    /**
     * Handles the need to recreate this activity due to a major change
     * affecting the activity and its fragments. For example, if the theme is
     * changed by {@link ThemeSelectDialog}, or if unknown changes have been
     * made to the preferences in {@link SettingsActivity}.
     */
    public void onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): " + this);

        // IMPORTANT: If this is not posted to the message queue, i.e., if
        // "recreate()" is simply called directly from "onRecreateRequired()"
        // (or even if a flag is set here and "recreate()" is called later
        // from "onResume()", the recreation goes very wrong. After the newly
        // created activity instance calls "onResume()" it immediately calls
        // "onPause()" for no apparent reason whatsoever. The activity is
        // clearly not "paused", as it the UI is perfectly responsive.
        // However, the next time it actually needs to pause, an exception is
        // logged complaining, "Performing pause of activity that is not
        // resumed".
        //
        // Perhaps the issue is caused by an incorrect synchronisation of the
        // destruction of the old activity and the creation of the new
        // activity. Whatever, simply posting the "recreate()" call here
        // seems to fix this. After posting, the (old) activity will continue
        // on and reach "onResume()" before then going through an orderly
        // shutdown and the new activity will be created and settle properly
        // at "onResume()".
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: " + this);
                recreate();
            }
        });
    }

    /**
     * Handles the pressing of the back button. If the drawer is open, it is
     * closed. Otherwise, each fragment is given the opportunity to perform a
     * relevant task when the back button is pressed, such as stop the timer,
     * or close an expanded menu. If no fragment "consumes" the event, then
     * this activity will exit.
     */
    @Override
    public void onBackPressed() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressed()");

        if (mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
            return;
        }

        final Fragment mainFragment
            = getMainFragManager().findFragmentByTag(FRAG_TIMER_MAIN);

        if (mainFragment instanceof OnBackPressedInFragmentListener) {
            // If the main fragment is open, let it and its "child" fragments
            // consume the "Back" button press if necessary.
            if (((OnBackPressedInFragmentListener) mainFragment)
                    .onBackPressedInFragment()) {
                // Button press was consumed. Stop here.
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG_ME) Log.d(TAG, "onDestroy(): " + this);
        if (mBillingProcessor != null)
            mBillingProcessor.release();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_overview, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Opens a dialog to allow a solve time to be viewed in detail, edited or
     * deleted. If no solve record exists for the given solve ID, the dialog
     * will not be opened.
     *
     * @param solveID The ID of the solve previously saved in the database.
     */
    public void startEditSolveTimesDialog(final long solveID) {
        // Perform the database read on another thread, then launch the dialog
        // from the UI thread if the initial read is successful.
        FireAndForgetExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Solve solve
                    = TwistyTimer.getDBHandler().getSolve(solveID);

                if (solve != null) {
                    FireAndForgetExecutor.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            // The dialog will call back via the
                            // "EditSolveCallbacks" interface.
                            EditSolveDialog.newInstance(solve)
                                .show(getMainFragManager(),
                                      FRAG_EDIT_SOLVE_DIALOG);

                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDeleteSolveTime(@NonNull final Solve solve) {
        TwistyTimer.getDBHandler().deleteSolveAndNotifyAsync(solve);
    }

    @Override
    public void onUpdateSolveTime(@NonNull Solve solve) {
        TwistyTimer.getDBHandler().updateSolveAndNotifyAsync(solve);
    }

    @Override
    public void onShareSolveTime(@NonNull final Solve solve) {
        PuzzleUtils.shareSolveTime(solve, this);
    }

    @Override
    public void onImportSolveTimes(
            @NonNull File file, @NonNull FileFormat fileFormat,
            PuzzleType puzzleType, String solveCategory) {
        new ImportSolves(this, file, fileFormat, puzzleType, solveCategory)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onExportSolveTimes(
            @NonNull FileFormat fileFormat, PuzzleType puzzleType,
            String solveCategory) {
        if (!StoreUtils.isExternalStorageWritable()) {
            return;
        }

        if (fileFormat == FileFormat.BACKUP) {
            // Expect that all other parameters are null, otherwise something
            // is very wrong.
            if (puzzleType != null || solveCategory != null) {
                throw new RuntimeException(
                    "Bug in the export code for the back-up format!");
            }

            final File file = ExImUtils.getBackupFileForExport();

            if (ExImUtils.ensureBackupExportDir()) {
                new ExportSolves(this, file, fileFormat, null, null)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                // Unlikely, so just log it for now.
                Log.e(TAG,
                    "Could not create output directory for back-up file: "
                    + file);
            }
        } else if (fileFormat == FileFormat.EXTERNAL) {
            // Expect that all other parameters are non-null, otherwise
            // something is very wrong.
            if (puzzleType == null || solveCategory == null) {
                throw new RuntimeException(
                    "Bug in the export code for the external format!");
            }

            final File file = ExImUtils.getExternalFileForExport(
                puzzleType, solveCategory);

            if (ExImUtils.ensureExternalExportDir()) {
                new ExportSolves(
                    this, file, fileFormat, puzzleType, solveCategory)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                // Unlikely, so just log it for now.
                Log.e(TAG,
                    "Could not create output directory for back-up file: "
                    + file);
            }
        } else {
            Log.e(TAG, "Unknown export file format: " + fileFormat);
        }
    }

    /**
     * Handles the call-back from a file chooser dialog when a file is selected.
     * This is used for communication between the {@code FileChooserDialog} and
     * the export/import fragments. The originating fragment that opened the
     * file chooser dialog should set the "tag" of the file chooser dialog to
     * the value of the fragment tag that this activity uses to identify that
     * originating fragment. This activity will then forward this notification
     * to that fragment, which is expected to implement this same interface
     * method.
     *
     * @param dialog
     *     The file chooser dialog that has reported the file selection.
     * @param file
     *     The file that was chosen.
     */
    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog,
                                @NonNull File file) {
        // This "relay" scheme ensures that this activity is not embroiled in
        // the gory details of what the "destinationFrag" wanted with the file.
        final Fragment destinationFrag
            = getMainFragManager().findFragmentByTag(dialog.getTag());

        if (destinationFrag instanceof FileChooserDialog.FileCallback) {
            ((FileChooserDialog.FileCallback) destinationFrag)
                .onFileSelection(dialog, file);
        } else {
            // This is not expected unless there is a bug to be fixed.
            Log.e(TAG, "onFileSelection(): Unknown or incompatible fragment: "
                       + dialog.getTag());
        }
    }

    /**
     * Handles the call-back from a fragment when a puzzle type and/or category
     * are selected. This is used for communication between the export/import
     * fragments. The "source" fragment should set the {@code tag} to the value
     * of the fragment tag that this activity uses to identify the "destination"
     * fragment. This activity will then forward this notification to that
     * fragment, which is expected to implement this same interface method.
     */
    @Override
    public void onPuzzleSelected(
            @NonNull String tag, @NonNull PuzzleType puzzleType,
            @NonNull String solveCategory) {
        // This "relay" scheme ensures that this activity is not embroiled in
        // the gory details of what the "destinationFrag" wanted with the
        // puzzle type/category.
        final Fragment destinationFrag
            = getMainFragManager().findFragmentByTag(tag);

        if (destinationFrag instanceof PuzzleChooserDialog.PuzzleCallback) {
            ((PuzzleChooserDialog.PuzzleCallback) destinationFrag)
                    .onPuzzleSelected(tag, puzzleType, solveCategory);
        } else {
            // This is not expected unless there is a bug to be fixed.
            Log.e(TAG, "onPuzzleSelected(): Unknown or incompatible fragment: "
                       + tag);
        }
    }

    private static class ExportSolves
                   extends AsyncTask<Void, Integer, Boolean> {

        private final Context    mContext;
        private final File       mFile;
        private final FileFormat mFileFormat;
        private final PuzzleType mPuzzleType;
        private final String     mSolveCategory;

        private MaterialDialog mProgressDialog;

        /**
         * Creates a new task for exporting solve times to a file.
         *
         * @param context
         *     The context required to access resources and to report progress.
         * @param file
         *     The file to which to export the solve times.
         * @param fileFormat
         *     The solve file format.
         * @param puzzleType
         *     The type of the puzzle whose times will be exported. This is
         *     required if the {@code fileFormat} is {@code EXTERNAL}; it may
         *     be {@code null} if the format is {@code BACKUP}, as it will
         *     not be used.
         * @param solveCategory
         *     The solve category for the solve times to be exported. This is
         *     required if the {@code fileFormat} is {@code EXTERNAL}; it may
         *     be {@code null} if the format is {@code BACKUP}, as it will
         *     not be used.
         */
        ExportSolves(Context context,
                     @NonNull File file, @NonNull FileFormat fileFormat,
                     PuzzleType puzzleType, String solveCategory) {
            mContext       = context;
            mFile          = file;
            mFileFormat    = fileFormat;
            mSolveCategory = solveCategory;
            mPuzzleType    = puzzleType;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new MaterialDialog.Builder(mContext)
                .content(R.string.export_progress_title)
                .progress(false, 0, true)
                .cancelable(false)
                .show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressDialog.isShowing()) {
                if (values.length > 1) {
                    // values[1] is the number of solve times, which could
                    // legitimately be zero. Do not set max. to zero or it will
                    // display "NaN".
                    mProgressDialog.setMaxProgress(Math.max(values[1], 1));
                }
                mProgressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            final ProgressListener listener = new ProgressListener() {
                @Override
                public void onProgress(int numCompleted, int total) {
                    publishProgress(numCompleted, total);
                }
            };

            try {
                final Writer out = new BufferedWriter(new FileWriter(mFile));
                // "try-with-resources" not supported at the current minimum
                // of API 16.
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (mFileFormat == FileFormat.BACKUP) {
                        TwistyTimer.getDBHandler()
                                   .writeCSVBackup(out, listener);
                    } else { // "EXTERNAL"
                        TwistyTimer.getDBHandler().writeCSVExternal(
                            out, mPuzzleType, mSolveCategory, listener);
                    }
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // Logging it is preferable to re-throwing it, as it
                        // could mask a "real" exception thrown from above.
                        Log.e(TAG, "Unexpected error closing CSV file.", e);
                    }
                }

                return true; // Export succeeded.
            } catch (IOException e) {
                Log.e(TAG, "Unexpected error writing CSV file.", e);
            }

            return false; // Export failed.
        }

        // "Html.fromHtml(String)" is deprecated in API 24, but this app
        // supports API 16+.
        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(Boolean isExported) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.setActionButton(
                    DialogAction.POSITIVE, R.string.action_done);

                if (isExported) {
                    mProgressDialog.setContent(Html.fromHtml(
                        mContext.getString(R.string.export_progress_complete)
                        + "<br><br>" + "<small><tt>" + mFile.getAbsolutePath()
                        + "</tt></small>"));
                } else {
                    mProgressDialog.setContent(R.string.export_progress_error);
                }
            }
        }
    }

    private static class ImportSolves extends AsyncTask<Void, Integer, Void> {

        private final Context    mContext;
        private final File       mFile;
        private final FileFormat mFileFormat;
        private final PuzzleType mPuzzleType;
        private final String     mSolveCategory;

        private MaterialDialog mProgressDialog;
        private int parseErrors = 0;
        private int duplicates  = 0;
        private int successes   = 0;

        /**
         * Creates a new task for importing solve times from a file.
         *
         * @param context
         *     The context required to access resources and to report progress.
         * @param file
         *     The file from which to import the solve times.
         * @param fileFormat
         *     The solve file format.
         * @param puzzleType
         *     The type of the puzzle whose times will be imported. This is
         *     required if the {@code fileFormat} is {@code EXTERNAL}; it may
         *     be {@code null} if the format is {@code BACKUP}, as it will
         *     not be used.
         * @param solveCategory
         *     The solve category for the solve times to be imported. This is
         *     required if the {@code fileFormat} is {@code EXTERNAL}; it may
         *     be {@code null} if the format is {@code BACKUP}, as it will
         *     not be used.
         */
        ImportSolves(@NonNull Context context,
                     @NonNull File file, @NonNull FileFormat fileFormat,
                     PuzzleType puzzleType, String solveCategory) {
            mContext       = context;
            mFile          = file;
            mFileFormat    = fileFormat;
            mPuzzleType    = puzzleType;
            mSolveCategory = solveCategory;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new MaterialDialog.Builder(mContext)
                .content(R.string.import_progress_title)
                .progress(false, 0, true)
                .cancelable(false)
                .show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressDialog.isShowing()) {
                if (values.length > 1) {
                    // values[1] is the number of solve times, which could
                    // legitimately be zero. Do not set max. to zero or it will
                    // display "NaN".
                    mProgressDialog.setMaxProgress(Math.max(values[1], 1));
                }
                mProgressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                final List<Solve> parsedSolves = new ArrayList<>();
                final Reader in = new BufferedReader(new FileReader(mFile));

                // "try-with-resources" not supported at the current minimum
                // of API 16.
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (mFileFormat == FileFormat.BACKUP) {
                        parseErrors = DatabaseHandler.parseCSVBackup(
                            in, parsedSolves);
                    } else { // "EXTERNAL"
                        parseErrors = DatabaseHandler.parseCSVExternal(
                            in, mPuzzleType, mSolveCategory, parsedSolves);
                    }

                    // Perform a bulk insertion of the solves.
                    successes = TwistyTimer.getDBHandler()
                        .addImportedSolves(parsedSolves,
                            new ProgressListener() {
                                @Override
                                public void onProgress(
                                        int numCompleted, int total) {
                                    publishProgress(numCompleted, total);
                                }
                            });

                    duplicates = parsedSolves.size() - successes;
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // Should not happen for a "FileReader", but log it just
                        // in case. This is preferable to re-throwing it, as it
                        // could mask a "real" exception thrown from above.
                        Log.e(TAG, "Unexpected error closing CSV file.", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error reading CSV file.", e);
            }

            return null;
        }

        // "Html.fromHtml(String)" is deprecated in API 24, but this app
        // supports API 16+.
        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(Void aVoid) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.setActionButton(
                    DialogAction.POSITIVE, R.string.action_done);
                mProgressDialog.setContent(Html.fromHtml(
                    mContext.getString(R.string.import_progress_content)
                    + "<br><br><small><tt>"
                    + "<b>" + successes + "</b> "
                    + mContext.getString(
                        R.string.import_progress_content_successful_imports)
                    + "<br><b>" + duplicates + "</b> "
                    + mContext.getString(
                        R.string.import_progress_content_ignored_duplicates)
                    + "<br><b>" + parseErrors + "</b> "
                    + mContext.getString(
                        R.string.import_progress_content_errors)
                    + "</small></tt>"));
            }

            // The puzzle type and category may be null if a back-up file was
            // imported, as it can contain a mix of types/categories. That is
            // not a problem, as receivers should just assume that the change
            // may affect any type/category.
            TTIntent
                .builder(CATEGORY_SOLVE_DATA_CHANGES,
                         ACTION_MANY_SOLVES_ADDED)
                .puzzleType(mPuzzleType)        // may be null.
                .solveCategory(mSolveCategory); // may be null.
        }
    }

    // So the drawer doesn't lag when closing
    private class SmoothActionBarDrawerToggle extends ActionBarDrawerToggle {

        private Runnable runnable;

        SmoothActionBarDrawerToggle(
                Activity activity, DrawerLayout drawerLayout, Toolbar toolbar,
                int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes,
                    closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            super.onDrawerStateChanged(newState);
            if (runnable != null && newState == DrawerLayout.STATE_IDLE) {
                runnable.run();
                runnable = null;
            }
        }

        void runWhenIdle(Runnable runnable) {
            this.runnable = runnable;
        }
    }
}
