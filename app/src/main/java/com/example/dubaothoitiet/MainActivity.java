package com.example.dubaothoitiet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Khai báo các biến
    private EditText cityEditText;
    private Button searchButton, viewMoreButton;
    private TextView cityNameTextView, tempTextView, conditionTextView, humidityTextView, pressureTextView;
    private ImageView weatherIconImageView;
    private LinearLayout detailsLayout;
    private LinearLayout rootLayout;
    private String currentCityName;
    private double currentLat, currentLon;

    private final String API_KEY = "b0ecf12a1f927381cd92f75e03a07904";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityEditText = findViewById(R.id.cityEditText);
        searchButton = findViewById(R.id.searchButton);
        viewMoreButton = findViewById(R.id.viewMoreButton);
        cityNameTextView = findViewById(R.id.cityNameTextView);
        tempTextView = findViewById(R.id.tempTextView);
        conditionTextView = findViewById(R.id.conditionTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        pressureTextView = findViewById(R.id.pressureTextView);
        weatherIconImageView = findViewById(R.id.weatherIconImageView);
        detailsLayout = findViewById(R.id.detailsLayout);
        rootLayout = findViewById(R.id.rootLayout);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_weather) {
            // Already on the weather screen
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                String description = weatherData.getString("description");
                int humidity = mainData.getInt("humidity");
                int pressure = mainData.getInt("pressure");
                String iconCode = weatherData.getString("icon");

                cityNameTextView.setText(currentCityName);
                tempTextView.setText(String.format(Locale.getDefault(), "%.0f°C", temp));
                conditionTextView.setText(description.substring(0, 1).toUpperCase() + description.substring(1));
                humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", humidity));
                pressureTextView.setText(String.format(Locale.getDefault(), "%d hPa", pressure));

                cityNameTextView.setVisibility(View.VISIBLE);
                weatherIconImageView.setVisibility(View.VISIBLE);
                tempTextView.setVisibility(View.VISIBLE);
                conditionTextView.setVisibility(View.VISIBLE);
                detailsLayout.setVisibility(View.VISIBLE);
                viewMoreButton.setVisibility(View.VISIBLE);

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
