package com.example.dubaothoitiet;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class ForecastActivity extends AppCompatActivity {

    private TextView forecastTitleTextView;
    private RecyclerView forecastRecyclerView;
    private ForecastAdapter forecastAdapter;
    private List<Forecast> forecastList;
    private BarChart temperatureChart;

    // --- VUI LÒNG THAY API KEY CỦA BẠN VÀO ĐÂY ---
    private final String API_KEY = "b0ecf12a1f927381cd92f75e03a07904";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        forecastTitleTextView = findViewById(R.id.forecastTitleTextView);
        forecastRecyclerView = findViewById(R.id.forecastRecyclerView);
        temperatureChart = findViewById(R.id.temperatureChart);
        forecastRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        String cityName = getIntent().getStringExtra("CITY_NAME");
        double lat = getIntent().getDoubleExtra("LAT", 0);
        double lon = getIntent().getDoubleExtra("LON", 0);

        forecastTitleTextView.setText("Dự báo 5 ngày cho " + cityName);

        forecastList = new ArrayList<>();
        forecastAdapter = new ForecastAdapter(forecastList);
        forecastRecyclerView.setAdapter(forecastAdapter);

        if (API_KEY.equals("YOUR_API_KEY_HERE")) {
            Toast.makeText(this, "Vui lòng thêm API Key của bạn vào file ForecastActivity.java", Toast.LENGTH_LONG).show();
        } else {
            new FetchForecastTask().execute(lat, lon);
        }
    }

    private class FetchForecastTask extends AsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... params) {
            double lat = params[0];
            double lon = params[1];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;

            try {
                final String FORECAST_BASE_URL = "https://api.openweathermap.org/data/2.5/forecast?";
                String urlString = FORECAST_BASE_URL + "lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric" + "&lang=vi";
                URL url = new URL(urlString);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        // Ignored
                    }
                }
            }
            return forecastJsonStr;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(ForecastActivity.this, "Lỗi lấy dữ liệu dự báo", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject forecastJson = new JSONObject(result);
                JSONArray listArray = forecastJson.getJSONArray("list");

                // --- Xử lý dữ liệu cho dự báo 5 ngày ---
                Set<String> processedDays = new HashSet<>();
                for (int i = 0; i < listArray.length(); i++) {
                    JSONObject forecastItem = listArray.getJSONObject(i);
                    long dt = forecastItem.getLong("dt");
                    String dt_txt = forecastItem.getString("dt_txt");

                    if (dt_txt.contains("12:00:00")) {
                        String dayName = getDayOfWeek(dt);
                        if (processedDays.add(dayName) && forecastList.size() < 5) {
                            JSONObject main = forecastItem.getJSONObject("main");
                            double temp = main.getDouble("temp");
                            JSONObject weather = forecastItem.getJSONArray("weather").getJSONObject(0);
                            String icon = weather.getString("icon");
                            String tempString = String.format(Locale.getDefault(), "%.0f°C", temp);
                            forecastList.add(new Forecast(dayName, icon, tempString));
                        }
                    }
                }
                forecastAdapter.notifyDataSetChanged();

                // --- Xử lý dữ liệu cho biểu đồ nhiệt độ hôm nay ---
                List<BarEntry> chartEntries = new ArrayList<>();
                List<String> chartLabels = new ArrayList<>();
                int dataPoints = Math.min(8, listArray.length()); // 8*3h = 24h

                for (int i = 0; i < dataPoints; i++) {
                    JSONObject forecastItem = listArray.getJSONObject(i);
                    JSONObject main = forecastItem.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    String time = forecastItem.getString("dt_txt").substring(11, 16); // "12:00"

                    chartEntries.add(new BarEntry(i, (float) temp));
                    chartLabels.add(time);
                }
                setupTemperatureChart(chartEntries, chartLabels);

            } catch (JSONException e) {
                Toast.makeText(ForecastActivity.this, "Lỗi phân tích dữ liệu dự báo", Toast.LENGTH_SHORT).show();
            }
        }

        private String getDayOfWeek(long time) {
            Date date = new Date(time * 1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String day = sdf.format(date);
            return day.substring(0, 1).toUpperCase() + day.substring(1);
        }
    }

    private void setupTemperatureChart(List<BarEntry> entries, final List<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Nhiệt độ");
        dataSet.setColor(Color.WHITE);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        temperatureChart.setData(barData);
        temperatureChart.setFitBars(true);
        temperatureChart.getDescription().setEnabled(false);
        temperatureChart.getLegend().setEnabled(false);
        temperatureChart.setDrawValueAboveBar(true);
        temperatureChart.setTouchEnabled(false);
        temperatureChart.setDrawGridBackground(false);

        // Trục X
        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(12f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        // Trục Y
        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#55FFFFFF")); // Lưới ngang màu trắng mờ
        leftAxis.setAxisLineColor(Color.WHITE);
        leftAxis.setLabelCount(5, true);
        leftAxis.setAxisMinimum(0f);

        temperatureChart.getAxisRight().setEnabled(false); // Ẩn trục Y bên phải

        temperatureChart.animateY(1500);

        temperatureChart.invalidate();
    }
}
