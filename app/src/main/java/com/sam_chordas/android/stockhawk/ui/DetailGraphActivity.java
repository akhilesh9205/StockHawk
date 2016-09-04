package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;


public class DetailGraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    private static final int CURSOR_LOADER_ID = 101;

    private String mSymbol;

    @BindView(R.id.stock_name)
    TextView mNameView;
    @BindView(R.id.stock_symbol)
    TextView mSymbolView;
    @BindView(R.id.stock_bidprice)
    TextView mEbitdaView;
    @BindView(R.id.stock_chart)
    LineChartView mChart;
    @BindView(R.id.stock_change)
    TextView mChange;
    @BindView(R.id.swipe_layout)
    SwipeRefreshLayout swipeLayout;
    @BindView(R.id.empty_text)
    TextView emptyTextView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_graph);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mSymbol = getIntent().getStringExtra("symbol");

        fetchStockHistory();
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        swipeLayout.setOnRefreshListener(this);
    }


    private void fetchStockHistory() {
        OkHttpClient client = new OkHttpClient();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = new Date();

        Calendar oneMonthOlder = Calendar.getInstance();
        oneMonthOlder.setTime(currentDate);
        oneMonthOlder.add(Calendar.MONTH, -1);

        String startDate = dateFormat.format(oneMonthOlder.getTime());
        String endDate = dateFormat.format(currentDate.getTime());

        String query = "select * from yahoo.finance.historicaldata where symbol=\"" + mSymbol +
                "\" and startDate=\"" + startDate + "\" and endDate=\"" + endDate + "\"";

        try {
            query = "q=" + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String QUERY_URL = "https://query.yahooapis.com/v1/public/yql?" +
                "format=json&diagnostics=true&" +
                "env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=" + "&" + query;


        Request request = new Request.Builder()
                .url(QUERY_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                onDownloadFailed();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) {
                    String responseString = response.body().string();

                    try {
                        JSONObject responseObject = new JSONObject(responseString);
                        JSONObject query = responseObject.getJSONObject("query");
                        JSONObject results = query.getJSONObject("results");
                        final JSONArray quote = results.getJSONArray("quote");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updateChart(quote);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    onDownloadFailed();
                }
            }
        });

    }

    private void onDownloadFailed() {
        onRefreshComlete();
        emptyTextView.setVisibility(View.VISIBLE);
        mChart.setVisibility(View.GONE);
    }

    private void onDownloadSuccess() {
        onRefreshComlete();
        emptyTextView.setVisibility(View.GONE);
        mChart.setVisibility(View.VISIBLE);
    }


    private void updateChart(JSONArray results) throws JSONException {
        onDownloadSuccess();

        List<AxisValue> axisValuesX = new ArrayList<>();
        List<PointValue> pointValues = new ArrayList<>();


        for (int counter = 0; counter < results.length(); counter++) {
            JSONObject data = results.getJSONObject(counter);
            String date = data.getString("Date");
            String bidPrice = data.getString("Close");


            int x = results.length() - 1 - counter;

            // Point for line chart (date, price).
            PointValue pointValue = new PointValue(x, Float.valueOf(bidPrice));
            pointValue.setLabel(date);
            pointValues.add(pointValue);

            // Set labels for x-axis (we have to reduce its number to avoid overlapping text).
            if (counter != 0 && counter % (results.length() / 3) == 0) {
                AxisValue axisValueX = new AxisValue(x);
                axisValueX.setLabel(date);
                axisValuesX.add(axisValueX);
            }
        }

        // Prepare data for chart
        Line line = new Line(pointValues).setColor(Color.WHITE).setCubic(false);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        LineChartData lineChartData = new LineChartData();
        lineChartData.setLines(lines);

        // Init x-axis
        Axis axisX = new Axis(axisValuesX);
        axisX.setHasLines(true);
        axisX.setMaxLabelChars(4);
        lineChartData.setAxisXBottom(axisX);

        // Init y-axis
        Axis axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(4);
        lineChartData.setAxisYLeft(axisY);

        // Update chart with new data.
        mChart.setInteractive(false);
        mChart.setLineChartData(lineChartData);

        // Show chart
        mChart.setVisibility(View.VISIBLE);

    }


    public static double maxInArray(float[] decMax) {
        double max = 0.0;
        if (decMax.length > 0) {
            max = decMax[0];
            for (int counter = 1; counter < decMax.length; counter++) {
                if (decMax[counter] > max) {
                    max = decMax[counter];
                }
            }
        }
        return max;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE, QuoteColumns.NAME,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.SYMBOL + " = ?",
                new String[]{mSymbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        setCurrentDataUI(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void setCurrentDataUI(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }
        String symbol = cursor.getString(cursor.getColumnIndex("symbol"));
        String name = cursor.getString(cursor.getColumnIndex("name"));
        String bid_price = cursor.getString(cursor.getColumnIndex("bid_price"));
        String p_change = cursor.getString(cursor.getColumnIndex("percent_change"));
        String change = cursor.getString(cursor.getColumnIndex("change"));

        mNameView.setText(name);
        toolbar.setTitle(name);
        mSymbolView.setText(symbol);
        mEbitdaView.setText(bid_price);
        mChange.setText(String.format("%s(%s)", change, p_change));
    }

    @Override
    public void onRefresh() {
        fetchStockHistory();
    }

    private void onRefreshComlete() {
        swipeLayout.setRefreshing(false);
    }
}
