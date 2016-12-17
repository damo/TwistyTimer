package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.spans.TimeFormatter;
import com.aricneto.twistytimer.stats.ChartStatistics;
import com.aricneto.twistytimer.stats.ChartStatisticsLoader;
import com.aricneto.twistytimer.stats.ChartStyle;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.utils.LoggingLoaderCallbacks;
import com.aricneto.twistytimer.utils.MainState;
import com.aricneto.twistytimer.utils.TTIntent;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import java.util.Locale;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.utils.TimeUtils.formatTimeStatistic;

/**
 * A simple {@link Fragment} subclass. Use the
 * {@link TimerGraphFragment#newInstance} factory method to create an instance
 * of this fragment.
 */
public class TimerGraphFragment extends BaseMainFragment
            implements StatisticsCache.StatisticsObserver {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerGraphFragment.class.getSimpleName();

    private Unbinder mUnbinder;
    @BindView(R.id.linechart)           LineChart lineChartView;
    @BindView(R.id.personalBestTimes)   TextView  personalBestTimes;
    @BindView(R.id.sessionBestTimes)    TextView  sessionBestTimes;
    @BindView(R.id.sessionCurrentTimes) TextView  sessionCurrentTimes;
    @BindView(R.id.progress_spinner)    MaterialProgressBar progressBar;

    // Things that must be hidden/shown when refreshing the card.
    @BindViews({
            R.id.personalBestTitle, R.id.sessionBestTitle,
            R.id.sessionCurrentTitle, R.id.horizontalDivider02,
            R.id.verticalDivider02, R.id.verticalDivider03,
    }) View[] statisticsTableViews;

    public TimerGraphFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change
    // puzzles.
    public static TimerGraphFragment newInstance() {
        final TimerGraphFragment fragment = new TimerGraphFragment();
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState="
                                 + savedInstanceState + ")");
        final View root = inflater.inflate(
            R.layout.fragment_timer_graph, container, false);
        mUnbinder = ButterKnife.bind(this, root);

        // The color for the text in the legend and for the values along the
        // chart's axes.
        final int chartTextColor = Color.WHITE;

        lineChartView.setPinchZoom(true);
        lineChartView.setBackgroundColor(Color.TRANSPARENT);
        lineChartView.setDrawGridBackground(false);
        lineChartView.getAxisRight().setEnabled(false);
        lineChartView.getLegend().setTextColor(chartTextColor);
        lineChartView.setDescription("");

        final YAxis axisLeft = lineChartView.getAxisLeft();
        final XAxis xAxis = lineChartView.getXAxis();
        final int axisColor = ContextCompat.getColor(
            getActivity(), R.color.white_secondary_icon);

        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(axisColor);
        xAxis.setDrawAxisLine(true);
        xAxis.setTextColor(chartTextColor);
        xAxis.setAvoidFirstLastClipping(true);

        axisLeft.setDrawGridLines(true);
        axisLeft.setDrawAxisLine(true);
        axisLeft.setTextColor(chartTextColor);
        axisLeft.enableGridDashedLine(10f, 8f, 0f);
        axisLeft.setAxisLineColor(axisColor);
        axisLeft.setGridColor(axisColor);
        axisLeft.setValueFormatter(new TimeFormatter());
        axisLeft.setDrawLimitLinesBehindData(true);

        // Setting for landscape mode. The chart and statistics table need to be
        // scrolled, as the statistics table will likely almost fill the screen.
        // The automatic layout causes the chart to use only the remaining space
        // after the statistics table takes its space. However, this may lead to
        // the whole chart being squeezed into a few vertical pixels. Therefore,
        // set a fixed height for the chart that will force the statistics table
        // to be scrolled down to allow the chart to fit.
        if (getActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            root.post(new Runnable() {
                @Override
                public void run() {
                    if (container != null && lineChartView != null) {
                        final LineChart.LayoutParams params
                            = lineChartView.getLayoutParams();

                        if (params != null) {
                            params.height = container.getHeight();
                            lineChartView.setLayoutParams(params);
                            lineChartView.requestLayout();
                        }
                    }
                }
            });
        }

        // If the statistics are already loaded, the update notification was
        // missed, so notify that now. If the statistics are non-null, they will
        // be displayed. If they are null (i.e., not yet loaded), the progress
        // bar will be displayed until this fragment, as a registered observer,
        // is notified when loading is complete. Post the firing of the event,
        // so that it is received after "onCreateView" returns.
        root.post(new Runnable() {
            @Override
            public void run() {
                onStatisticsUpdated(
                    StatisticsCache.getInstance().getStatistics());
            }
        });
        // Unregistered in "onDestroyView".
        StatisticsCache.getInstance().registerObserver(this);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated(savedInstanceState="
                + savedInstanceState + ')');
        super.onActivityCreated(savedInstanceState);

        // The loader does not load anything immediately on being started, it
        // just begins listening for broadcast events affecting the data.
        // Broadcast the "bootstrap" intent from "onResume()", when it is
        // certain that this loader will be ready and listening. Starting
        // loaders here in "onActivityCreated" ensures that "onCreateView" is
        // complete. An anonymous inner class is neater than implementing
        // "LoaderCallbacks".
        if (DEBUG_ME) Log.d(TAG,
            "onActivityCreated() -> initLoader: CHART DATA LOADER");
        getLoaderManager().initLoader(MainActivity.CHART_DATA_LOADER_ID, null,
            new LoggingLoaderCallbacks<ChartStatistics>(
                TAG, "CHART DATA LOADER") {
                @Override
                public Loader<ChartStatistics> onCreateLoader(
                        int id, Bundle args) {
                    if (DEBUG_ME) super.onCreateLoader(id, args);
                    // "ChartStyle" allows the Loader to be executed without
                    // the need to hold a reference to an Activity context
                    // (required to access theme attributes), which would be
                    // likely to cause memory leaks and crashes.
                    return new ChartStatisticsLoader(
                        getContext(), new ChartStyle(getActivity()));
                }

                @Override
                public void onLoadFinished(Loader<ChartStatistics> loader,
                                           ChartStatistics data) {
                    if (DEBUG_ME) super.onLoadFinished(loader, data);
                    updateChart(data);
                }

                @Override
                public void onLoaderReset(Loader<ChartStatistics> loader) {
                    if (DEBUG_ME) super.onLoaderReset(loader);
                    // Nothing to do here, as the "ChartStatistics" object was
                    // never retained. The view is most likely destroyed at
                    // this time, so no need to update it.
                }
            });
    }

    @Override
    public void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume()");
        super.onResume();

        // Will have not effect if already booted and main state is in sync....
        TTIntent.broadcastBootChartStatisticsLoader(getMainState());
    }

    /**
     * Reloads the chart statistics if the main state changes and the chart
     * statistics are not already in sync with the new main state.
     *
     * @param newMainState The new main state.
     * @param oldMainState The old main state.
     */
    @Override
    public void onMainStateChanged(
            @NonNull MainState newMainState, @NonNull MainState oldMainState) {
        super.onMainStateChanged(newMainState, oldMainState); // For logging.

        // Send a signal to the chart statistics loader to ensure it is in sync
        // with the main state. If it is already in sync (perhaps due to another
        // fragment broadcasting the same intent), it will ignore the request.
        TTIntent.broadcastMainStateChanged(newMainState);
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        mUnbinder.unbind();
        StatisticsCache.getInstance().unregisterObserver(this);
    }

    /**
     * Sets the visibility of the statistics table values columns. When
     * loading starts, the columns should be hidden and a progress bar shown.
     * When loading finishes, the columns should be populated with values and
     * be shown and the progress bar hidden. No action will be taken if this
     * fragment does not yet have a view, or if its view has been destroyed.
     *
     * @param visibility
     *     The visibility to set on the statistics table columns. Use
     *     {@code View.GONE} or {@code View.VISIBLE}. The opposite visibility
     *     will be applied to the progress bar.
     */
    private void setStatsTableVisibility(final int visibility) {
        if (getView() == null) {
            // Called before "onCreateView" or after "onDestroyView", so do
            // nothing.
            return;
        }

        ButterKnife.apply(statisticsTableViews, new ButterKnife.Action<View>() {
            @Override
            public void apply(@NonNull View view, int index) {
                view.setVisibility(visibility);
            }
        });

        personalBestTimes.setVisibility(visibility);
        sessionBestTimes.setVisibility(visibility);
        sessionCurrentTimes.setVisibility(visibility);

        // NOTE: Use "INVISIBLE", not "GONE", or there will be problems with
        // vertical centering.
        progressBar.setVisibility(
            visibility == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * Called when the chart statistics loader has completed loading the chart
     * data. The loader listens for changes to the data and this method will be
     * called each time the display of the chart needs to be refreshed. If this
     * fragment has no view, no update will be attempted.
     *
     * @param chartStats The chart statistics populated by the loader.
     */
    private void updateChart(ChartStatistics chartStats) {
        if (DEBUG_ME) Log.d(TAG, "updateChart()");

        if (getView() == null) {
            // Must have arrived after "onDestroyView" was called. Do nothing.
            return;
        }

        // Add all times line, best times line, average-of-N times lines (with
        // highlighted best AoN times) and mean limit line and identify them
        // all using a custom legend.
        chartStats.applyTo(lineChartView);

        // Animate and refresh the chart.
        lineChartView.animateY(1_000);
    }

    /**
     * Refreshes the display of the statistics. If this fragment has no view,
     * no update will be attempted.
     *
     * @param stats
     *     The updated statistics. These will not be modified. If {@code null},
     *     a progress bar will be displayed until non-{@code null} statistics
     *     are passed to this method in a later call.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onStatisticsUpdated(Statistics stats) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated(" + stats + ")");

        if (getView() == null) {
            // Must have arrived after "onDestroyView" was called. Do nothing.
            return;
        }

        if (stats == null) {
            // Hide the statistics and show the progress bar until the
            // statistics become ready.
            setStatsTableVisibility(View.GONE);
            return;
        }

        // "stats.get*" methods will return the time appropriately (WCA) rounded
        // and then "formatTimeStatistic" will format the given time to the
        // correct (WCA) resolution.

        String allTimeBestAvg3 = formatTimeStatistic(
            stats.getAverageOf(3, false).getBestAverage());
        String allTimeBestAvg5 = formatTimeStatistic(
            stats.getAverageOf(5, false).getBestAverage());
        String allTimeBestAvg12 = formatTimeStatistic(
            stats.getAverageOf(12, false).getBestAverage());
        String allTimeBestAvg50 = formatTimeStatistic(
            stats.getAverageOf(50, false).getBestAverage());
        String allTimeBestAvg100 = formatTimeStatistic(
            stats.getAverageOf(100, false).getBestAverage());
        String allTimeBestAvg1000 = formatTimeStatistic(
            stats.getAverageOf(1_000, false).getBestAverage());

        String allTimeMeanTime
            = formatTimeStatistic(stats.getAllTimeMeanTime());
        String allTimeBestTime
            = formatTimeStatistic(stats.getAllTimeBestTime());
        String allTimeWorstTime
            = formatTimeStatistic(stats.getAllTimeWorstTime());
        // Format count using appropriate grouping separators, e.g., "1,234",
        // not "1234".
        String allTimeCount = String.format(
            Locale.getDefault(), "%,d", stats.getAllTimeNumSolves());

        String sessionBestAvg3 = formatTimeStatistic(
            stats.getAverageOf(3, true).getBestAverage());
        String sessionBestAvg5 = formatTimeStatistic(
            stats.getAverageOf(5, true).getBestAverage());
        String sessionBestAvg12 = formatTimeStatistic(
            stats.getAverageOf(12, true).getBestAverage());
        String sessionBestAvg50 = formatTimeStatistic(
            stats.getAverageOf(50, true).getBestAverage());
        String sessionBestAvg100 = formatTimeStatistic(
            stats.getAverageOf(100, true).getBestAverage());
        String sessionBestAvg1000 = formatTimeStatistic(
            stats.getAverageOf(1_000, true).getBestAverage());

        String sessionMeanTime
            = formatTimeStatistic(stats.getSessionMeanTime());
        String sessionBestTime
            = formatTimeStatistic(stats.getSessionBestTime());
        String sessionWorstTime
            = formatTimeStatistic(stats.getSessionWorstTime());
        String sessionCount = String.format(
            Locale.getDefault(), "%,d", stats.getSessionNumSolves());

        String sessionCurrentAvg3 = formatTimeStatistic(
            stats.getAverageOf(3, true).getCurrentAverage());
        String sessionCurrentAvg5 = formatTimeStatistic(
            stats.getAverageOf(5, true).getCurrentAverage());
        String sessionCurrentAvg12 = formatTimeStatistic(
            stats.getAverageOf(12, true).getCurrentAverage());
        String sessionCurrentAvg50 = formatTimeStatistic(
            stats.getAverageOf(50, true).getCurrentAverage());
        String sessionCurrentAvg100 = formatTimeStatistic(
            stats.getAverageOf(100, true).getCurrentAverage());
        String sessionCurrentAvg1000 = formatTimeStatistic(
            stats.getAverageOf(1_000, true).getCurrentAverage());

        personalBestTimes.setText(
            allTimeBestAvg3 + "\n" +
            allTimeBestAvg5 + "\n" +
            allTimeBestAvg12 + "\n" +
            allTimeBestAvg50 + "\n" +
            allTimeBestAvg100 + "\n" +
            allTimeBestAvg1000 + "\n" +
            allTimeMeanTime + "\n" +
            allTimeBestTime + "\n" +
            allTimeWorstTime + "\n" +
            allTimeCount);
        sessionBestTimes.setText(
            sessionBestAvg3 + "\n" +
            sessionBestAvg5 + "\n" +
            sessionBestAvg12 + "\n" +
            sessionBestAvg50 + "\n" +
            sessionBestAvg100 + "\n" +
            sessionBestAvg1000 + "\n" +
            sessionMeanTime + "\n" +
            sessionBestTime + "\n" +
            sessionWorstTime + "\n" +
            sessionCount);
        sessionCurrentTimes.setText(
            sessionCurrentAvg3 + "\n" +
            sessionCurrentAvg5 + "\n" +
            sessionCurrentAvg12 + "\n" +
            sessionCurrentAvg50 + "\n" +
            sessionCurrentAvg100 + "\n" +
            sessionCurrentAvg1000 + "\n" +
            // Last few are the same for "Session Best" and "Session Current".
            sessionMeanTime + "\n" +
            sessionBestTime + "\n" +
            sessionWorstTime + "\n" +
            sessionCount);

        // Display the statistics and hide the progress bar.
        setStatsTableVisibility(View.VISIBLE);
    }
}
