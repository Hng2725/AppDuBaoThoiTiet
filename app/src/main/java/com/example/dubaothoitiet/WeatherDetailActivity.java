package com.example.dubaothoitiet;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class WeatherDetailActivity extends AppCompatActivity {

    private TextView cityNameTextView;
    private TextView uvIndexTextView, humidityTextView, pressureTextView, windSpeedTextView;
    private TextView feelsLikeTextView, visibilityTextView, cloudinessTextView, sunriseTextView, sunsetTextView;
    private RelativeLayout rootLayout;
    
    private String cityName;
    private double lat, lon;
    private final String API_KEY = "b0ecf12a1f927381cd92f75e03a07904";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_weather_detail);

        rootLayout = findViewById(R.id.rootLayout);
        cityNameTextView = findViewById(R.id.cityNameTextView);
        uvIndexTextView = findViewById(R.id.uvIndexTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        pressureTextView = findViewById(R.id.pressureTextView);
        windSpeedTextView = findViewById(R.id.windSpeedTextView);
        feelsLikeTextView = findViewById(R.id.feelsLikeTextView);
        visibilityTextView = findViewById(R.id.visibilityTextView);
        cloudinessTextView = findViewById(R.id.cloudinessTextView);
        sunriseTextView = findViewById(R.id.sunriseTextView);
        sunsetTextView = findViewById(R.id.sunsetTextView);

        cityName = getIntent().getStringExtra("CITY_NAME");
        lat = getIntent().getDoubleExtra("LAT", 0);
        lon = getIntent().getDoubleExtra("LON", 0);

        cityNameTextView.setText(cityName != null ? cityName : "Chi tiết thời tiết");

        new FetchWeatherDetailsTask().execute(lat, lon);
    }

    private class FetchWeatherDetailsTask extends AsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... params) {
            double latitude = params[0];
            double longitude = params[1];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String weatherJsonStr = null;

            try {
                // Fetch current weather data
                final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?";
                String urlString = BASE_URL + "lat=" + latitude + "&lon=" + longitude + 
                                   "&appid=" + API_KEY + "&units=metric&lang=vi";
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
                        // Ignored
                    }
                }
            }
            return weatherJsonStr;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(WeatherDetailActivity.this, "Không thể lấy dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject weatherJson = new JSONObject(result);
                org.json.JSONArray weatherArray = weatherJson.getJSONArray("weather");
                JSONObject weatherData = weatherArray.getJSONObject(0);
                String iconCode = weatherData.getString("icon");
                
                JSONObject mainData = weatherJson.getJSONObject("main");
                JSONObject windData = weatherJson.getJSONObject("wind");
                JSONObject cloudsData = weatherJson.getJSONObject("clouds");
                JSONObject sysData = weatherJson.getJSONObject("sys");

                // Extract data
                int humidity = mainData.getInt("humidity");
                int pressure = mainData.getInt("pressure");
                double windSpeed = windData.getDouble("speed");
                double feelsLike = mainData.getDouble("feels_like");
                int visibility = weatherJson.optInt("visibility", 0);
                int cloudiness = cloudsData.getInt("all");
                long sunrise = sysData.getLong("sunrise");
                long sunset = sysData.getLong("sunset");
                
                // Update background based on weather
                updateBackground(iconCode);

                // Update UI
                humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", humidity));
                pressureTextView.setText(String.format(Locale.getDefault(), "%d hPa", pressure));
                windSpeedTextView.setText(String.format(Locale.getDefault(), "%.1f m/s", windSpeed));
                feelsLikeTextView.setText(String.format(Locale.getDefault(), "%.1f°C", feelsLike));
                visibilityTextView.setText(String.format(Locale.getDefault(), "%.1f km", visibility / 1000.0));
                cloudinessTextView.setText(String.format(Locale.getDefault(), "%d%%", cloudiness));
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
                sunriseTextView.setText(sdf.format(new java.util.Date(sunrise * 1000)));
                sunsetTextView.setText(sdf.format(new java.util.Date(sunset * 1000)));

                new FetchUVIndexTask().execute(lat, lon);

            } catch (JSONException e) {
                Toast.makeText(WeatherDetailActivity.this, "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FetchUVIndexTask extends AsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... params) {
            double latitude = params[0];
            double longitude = params[1];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String uvJsonStr = null;

            try {
                final String BASE_URL = "https://api.openweathermap.org/data/2.5/uvi?";
                String urlString = BASE_URL + "lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY;
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
                uvJsonStr = buffer.toString();
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
            return uvJsonStr;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                uvIndexTextView.setText("N/A");
                return;
            }

            try {
                JSONObject uvJson = new JSONObject(result);
                double uvIndex = uvJson.getDouble("value");
                String uvLevel = getUVLevel(uvIndex);
                uvIndexTextView.setText(String.format(Locale.getDefault(), "%.1f (%s)", uvIndex, uvLevel));
            } catch (JSONException e) {
                uvIndexTextView.setText("N/A");
            }
        }
    }

    private String getUVLevel(double uvIndex) {
        if (uvIndex <= 2) return "Thấp";
        else if (uvIndex <= 5) return "Trung bình";
        else if (uvIndex <= 7) return "Cao";
        else if (uvIndex <= 10) return "Rất cao";
        else return "Cực cao";
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
