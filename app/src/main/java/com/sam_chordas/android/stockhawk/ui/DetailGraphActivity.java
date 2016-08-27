package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
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


public class DetailGraphActivity extends Activity {


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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_graph);
        ButterKnife.bind(this);

        mSymbol = getIntent().getStringExtra("symbol");

        fetchStockHistory();
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
                "env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=" + "&"+query;


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
                        JSONObject query= responseObject.getJSONObject("query");
                        JSONObject results= query.getJSONObject("results");
                        final JSONArray quote= results.getJSONArray("quote");

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


                    /*try {
                        // Trim response string
                        String result = response.body().string();
                        if (result.startsWith("finance_charts_json_callback( ")) {
                            result = result.substring(29, result.length() - 2);
                        }

                        // Parse JSON
                        JSONObject object = new JSONObject(result);
                        String companyName = object.getJSONObject("meta").getString("Company-Name");
                        if (companyName != null) {
                            name.setText(companyName);
                        }
                        List<String> labels = new ArrayList<>();
                        List<Float> values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject seriesItem = series.getJSONObject(i);
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyyMMdd");
                            SimpleDateFormat destFormat = new SimpleDateFormat("dd MMM");
                            String date = destFormat.format(srcFormat.parse(seriesItem.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(seriesItem.getString("close")));
                        }

                        onDownloadCompleted(labels, values);
                    } catch (Exception e) {
                        onDownloadFailed();
                        e.printStackTrace();
                    }*/
                } else {
                    onDownloadFailed();
                }
            }
        });

    }

    private void onDownloadFailed() {

    }

    private void onDownloadCompleted(List<String> labels, List<Float> values) {

    }

    private void updateChart(JSONArray results) throws JSONException {

        List<AxisValue> axisValuesX = new ArrayList<>();
        List<PointValue> pointValues = new ArrayList<>();


        for(int counter=0;counter<results.length();counter++) {
            JSONObject data = results.getJSONObject(counter);
            String date = data.getString("Date");
            String bidPrice = data.getString("Close");


            // We have to show chart in right order.
            int x = results.length() - 1 - counter;

            // Point for line chart (date, price).
            PointValue pointValue = new PointValue(x, Float.valueOf(bidPrice));
            pointValue.setLabel(date);
            pointValues.add(pointValue);

            // Set labels for x-axis (we have to reduce its number to avoid overlapping text).
            if (counter != 0 && counter % (data.length() / 3) == 0) {
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

}
