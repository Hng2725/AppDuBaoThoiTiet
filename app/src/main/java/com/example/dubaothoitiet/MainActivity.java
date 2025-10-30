package com.example.dubaothoitiet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    private EditText cityEditText;
    private Button searchButton, viewMoreButton;
    private TextView cityNameTextView, tempTextView, conditionTextView, humidityTextView, pressureTextView;
    private TextView tempRangeTextView, aqiTextView;
    private ImageView weatherIconImageView;
    private LinearLayout detailsLayout, conditionLayout, aqiLayout, searchSection;
    private android.widget.RelativeLayout rootLayout;
    private androidx.cardview.widget.CardView forecastCard;
    private androidx.recyclerview.widget.RecyclerView forecastRecyclerView;
    private ForecastAdapter forecastAdapter;
    private List<Forecast> forecastList;
    private String currentCityName;
    private double currentLat, currentLon;
    private double currentTempMin, currentTempMax;

    private final String API_KEY = "b0ecf12a1f927381cd92f75e03a07904";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_main);

        cityEditText = findViewById(R.id.cityEditText);
        searchButton = findViewById(R.id.searchButton);
        viewMoreButton = findViewById(R.id.viewMoreButton);
        cityNameTextView = findViewById(R.id.cityNameTextView);
        tempTextView = findViewById(R.id.tempTextView);
        conditionTextView = findViewById(R.id.conditionTextView);
        tempRangeTextView = findViewById(R.id.tempRangeTextView);
        aqiTextView = findViewById(R.id.aqiTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        pressureTextView = findViewById(R.id.pressureTextView);
        weatherIconImageView = findViewById(R.id.weatherIconImageView);
        detailsLayout = findViewById(R.id.detailsLayout);
        conditionLayout = findViewById(R.id.conditionLayout);
        aqiLayout = findViewById(R.id.aqiLayout);
        searchSection = findViewById(R.id.searchSection);
        forecastCard = findViewById(R.id.forecastCard);
        forecastRecyclerView = findViewById(R.id.forecastRecyclerView);
        rootLayout = findViewById(R.id.rootLayout);

        // Initialize forecast list
        forecastList = new ArrayList<>();
        forecastAdapter = new ForecastAdapter(forecastList);
        forecastRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        forecastRecyclerView.setAdapter(forecastAdapter);

        searchButton.setOnClickListener(v -> {
            String cityName = cityEditText.getText().toString().trim();
            if (cityName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập tên thành phố", Toast.LENGTH_SHORT).show();
            } else {
                new FetchWeatherTask().execute(cityName);
            }
        });

        viewMoreButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForecastActivity.class);
            intent.putExtra("CITY_NAME", currentCityName);
            intent.putExtra("LAT", currentLat);
            intent.putExtra("LON", currentLon);
            startActivity(intent);
        });

        // Add button click handler
        ImageView addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            // Toggle search section visibility
            if (searchSection.getVisibility() == View.VISIBLE) {
                searchSection.setVisibility(View.GONE);
            } else {
                searchSection.setVisibility(View.VISIBLE);
            }
        });

        // Menu button click handler
        ImageView menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            // Open About activity
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }



    private class FetchWeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String cityName = params[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String weatherJsonStr = null;

            try {
                final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?";
                String urlString = BASE_URL + "q=" + cityName + "&appid=" + API_KEY + "&units=metric" + "&lang=vi";
                URL url = new URL(urlString);

                // Tạo HTTP
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
                weatherJsonStr = buffer.toString();
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
                    }
                }
            }
            return weatherJsonStr;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(MainActivity.this, "Không tìm thấy thành phố hoặc có lỗi mạng", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                JSONObject weatherJson = new JSONObject(result);
                JSONArray weatherArray = weatherJson.getJSONArray("weather");
                JSONObject weatherData = weatherArray.getJSONObject(0);
                JSONObject mainData = weatherJson.getJSONObject("main");
                JSONObject coordData = weatherJson.getJSONObject("coord");

                currentCityName = weatherJson.getString("name");
                currentLat = coordData.getDouble("lat");
                currentLon = coordData.getDouble("lon");

                double temp = mainData.getDouble("temp");
                currentTempMin = mainData.getDouble("temp_min");
                currentTempMax = mainData.getDouble("temp_max");
                String description = weatherData.getString("description");
                int humidity = mainData.getInt("humidity");
                int pressure = mainData.getInt("pressure");
                String iconCode = weatherData.getString("icon");

                cityNameTextView.setText(currentCityName);
                tempTextView.setText(String.format(Locale.getDefault(), "%.0f°", temp));
                conditionTextView.setText(description.substring(0, 1).toUpperCase() + description.substring(1));
                tempRangeTextView.setText(String.format(Locale.getDefault(), "  %.0f° %.0f°", currentTempMax, currentTempMin));
                aqiTextView.setText("AQI 102"); // Mock AQI data
                humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", humidity));
                pressureTextView.setText(String.format(Locale.getDefault(), "%d hPa", pressure));

                // Hide search section and show weather data
                searchSection.setVisibility(View.GONE);
                cityNameTextView.setVisibility(View.VISIBLE);
                tempTextView.setVisibility(View.VISIBLE);
                conditionLayout.setVisibility(View.VISIBLE);
                aqiLayout.setVisibility(View.VISIBLE);
                forecastCard.setVisibility(View.VISIBLE);

                // Fetch 5-day forecast
                new FetchForecastTask().execute(currentLat, currentLon);

                String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@4x.png";
                new DownloadImageTask().execute(iconUrl);

                updateBackground(iconCode);

            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show();
            }
        }

        private void updateBackground(String iconCode) {
            int backgroundResource;
            switch (iconCode) {
                case "01d":
                    backgroundResource = R.drawable.bg_sunny;
                    break;
                case "01n":
                    backgroundResource = R.drawable.bg_night;
                    break;
                case "02d":
                case "03d":
                case "04d":
                    backgroundResource = R.drawable.bg_cloudy;
                    break;
                case "02n":
                case "03n":
                case "04n":
                    backgroundResource = R.drawable.bg_night;
                    break;
                case "09d":
                case "10d":
                case "11d":
                    backgroundResource = R.drawable.bg_rainy;
                    break;
                case "09n":
                case "10n":
                case "11n":
                    backgroundResource = R.drawable.bg_rainy;
                    break;
                default:
                    backgroundResource = R.drawable.bg_cloudy;
                    break;
            }
            rootLayout.setBackgroundResource(backgroundResource);
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
                return;
            }

            try {
                JSONObject forecastJson = new JSONObject(result);
                JSONArray listArray = forecastJson.getJSONArray("list");

                forecastList.clear();
                Set<String> processedDays = new HashSet<>();
                for (int i = 0; i < listArray.length() && forecastList.size() < 5; i++) {
                    JSONObject forecastItem = listArray.getJSONObject(i);
                    long dt = forecastItem.getLong("dt");
                    String dt_txt = forecastItem.getString("dt_txt");

                    if (dt_txt.contains("12:00:00") || forecastList.isEmpty()) {
                        String dayName = getDayOfWeek(dt, forecastList.isEmpty());
                        if (processedDays.add(dayName)) {
                            JSONObject main = forecastItem.getJSONObject("main");
                            double tempMin = main.getDouble("temp_min");
                            double tempMax = main.getDouble("temp_max");
                            JSONObject weather = forecastItem.getJSONArray("weather").getJSONObject(0);
                            String icon = weather.getString("icon");
                            String tempMinString = String.format(Locale.getDefault(), "%.0f°", tempMin);
                            String tempMaxString = String.format(Locale.getDefault(), "%.0f°", tempMax);
                            forecastList.add(new Forecast(dayName, icon, tempMinString, tempMaxString));
                        }
                    }
                }
                forecastAdapter.notifyDataSetChanged();

            } catch (JSONException e) {
                // Ignored
            }
        }

        private String getDayOfWeek(long time, boolean isToday) {
            if (isToday) {
                return "Hôm nay";
            }
            Date date = new Date(time * 1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String day = sdf.format(date);
            // Capitalize first letter
            if (day.equals("thứ hai")) return "Ngày mai";
            return day.substring(0, 1).toUpperCase() + day.substring(1);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bmp;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                weatherIconImageView.setImageBitmap(result);
            }
        }
    }
}
