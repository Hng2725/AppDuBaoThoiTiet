package com.example.dubaothoitiet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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


    // Location
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isLocationPermissionGranted = false;

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

        // Initialize forecast lists
        forecastList = new ArrayList<>();
        forecastAdapter = new ForecastAdapter(forecastList);
        forecastRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        forecastRecyclerView.setAdapter(forecastAdapter);


        // Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

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

        // Add button click handler - Force use real Hanoi coordinates
        ImageView addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Toast.makeText(this, "🎯 Sử dụng tọa độ Hà Nội thực tế", Toast.LENGTH_SHORT).show();
            testLocationWithCoordinates(20.977111, 105.781444);
        });

        // Menu button click handler
        ImageView menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            // Test with your exact coordinates
            testLocationWithCoordinates(20.977111, 105.781444);
        });

        // Check and request location permission
        checkLocationPermission();

        // Try to get current location weather on app start
        if (isLocationPermissionGranted) {
            refreshByCurrentLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only refresh location weather if no weather data is currently displayed
        if (isLocationPermissionGranted && cityNameTextView.getVisibility() == View.GONE) {
            refreshByCurrentLocation();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true;
                refreshByCurrentLocation();
            } else {
                isLocationPermissionGranted = false;
                // Show search section as fallback
                searchSection.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Cần quyền truy cập vị trí để hiển thị thời tiết hiện tại", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void refreshByCurrentLocation() {
        if (!isLocationPermissionGranted) {
            checkLocationPermission();
            return;
        }
        getCurrentLocation();
    }

    private void testLocationWithCoordinates(double lat, double lon) {
        Toast.makeText(this, "Testing với tọa độ Hà Nội: " + lat + ", " + lon, Toast.LENGTH_SHORT).show();
        new FetchWeatherByCoordinatesTask().execute(lat, lon);
    }

    private boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }

    private void getCurrentLocation() {
        if (!isLocationPermissionGranted) return;



        try {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Debug: Show coordinates and location info
                    String provider = location.getProvider();
                    float accuracy = location.getAccuracy();
                    long time = location.getTime();
                    long age = System.currentTimeMillis() - time;

                    Toast.makeText(MainActivity.this,
                            String.format("GPS: %.6f, %.6f\nProvider: %s\nAccuracy: %.1fm\nAge: %ds",
                                    latitude, longitude, provider, accuracy, age/1000),
                            Toast.LENGTH_LONG).show();

                    // Check if coordinates are suspicious (Mountain View area)
                    if (latitude >= 37.3 && latitude <= 37.5 && longitude >= -122.2 && longitude <= -122.0) {
                        Toast.makeText(MainActivity.this,
                                "⚠️ GPS trả về tọa độ Mountain View! Có thể đang dùng emulator hoặc mock location",
                                Toast.LENGTH_LONG).show();

                        // Use your real coordinates instead
                        latitude = 21.028511;
                        longitude = 105.804817;

                        Toast.makeText(MainActivity.this,
                                "✅ Đã chuyển sang tọa độ Hà Nội thực tế",
                                Toast.LENGTH_SHORT).show();
                    }

                    // Stop location updates after getting first location
                    locationManager.removeUpdates(this);

                    // Call weather API with coordinates
                    new FetchWeatherByCoordinatesTask().execute(latitude, longitude);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            // Try GPS first, then network
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            } else {
                Toast.makeText(this, "Vui lòng bật GPS hoặc kết nối mạng", Toast.LENGTH_SHORT).show();
                searchSection.setVisibility(View.VISIBLE);
            }

            // Get last known location as backup
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation == null) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnownLocation != null) {
                // Check if location is recent (within 10 minutes)
                long locationAge = System.currentTimeMillis() - lastKnownLocation.getTime();
                if (locationAge < 10 * 60 * 1000) { // 10 minutes
                    double lat = lastKnownLocation.getLatitude();
                    double lon = lastKnownLocation.getLongitude();

                    Toast.makeText(this,
                            String.format("Last known: %.6f, %.6f (Age: %ds)",
                                    lat, lon, locationAge/1000),
                            Toast.LENGTH_SHORT).show();

                    // Check if last known location is also Mountain View
                    if (lat >= 37.3 && lat <= 37.5 && lon >= -122.2 && lon <= -122.0) {
                        Toast.makeText(this,
                                "⚠️ Last known location cũng là Mountain View! Sử dụng tọa độ Hà Nội",
                                Toast.LENGTH_LONG).show();
                        lat = 20.977111;
                        lon = 105.781444;
                    }

                    new FetchWeatherByCoordinatesTask().execute(lat, lon);
                }
            }

        } catch (SecurityException e) {
            Toast.makeText(this, "Không thể truy cập vị trí", Toast.LENGTH_SHORT).show();
            searchSection.setVisibility(View.VISIBLE);
        }
    }

    private class FetchWeatherByCoordinatesTask extends AsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... params) {
            double lat = params[0];
            double lon = params[1];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String weatherJsonStr = null;

            try {
                // Use standard weather API with coordinates
                final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?";
                String urlString = BASE_URL + "lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&lang=vi";
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
                // Fallback to regular weather API if OneCall fails
                Toast.makeText(MainActivity.this, "Không thể lấy dữ liệu thời tiết chi tiết", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject weatherJson = new JSONObject(result);
                JSONArray weatherArray = weatherJson.getJSONArray("weather");
                JSONObject weatherData = weatherArray.getJSONObject(0);
                JSONObject mainData = weatherJson.getJSONObject("main");
                JSONObject coordData = weatherJson.getJSONObject("coord");

                double temp = mainData.getDouble("temp");
                String description = weatherData.getString("description");
                String iconCode = weatherData.getString("icon");
                double tempMin = mainData.getDouble("temp_min");
                double tempMax = mainData.getDouble("temp_max");

                // Use location data to populate existing UI with better data
                cityNameTextView.setText("Vị trí hiện tại");
                tempTextView.setText(String.format(Locale.getDefault(), "%.0f°", temp));
                conditionTextView.setText(description.substring(0, 1).toUpperCase() + description.substring(1));
                tempRangeTextView.setText(String.format(Locale.getDefault(), "  %.0f° %.0f°", tempMax, tempMin));
                aqiTextView.setText("AQI 102"); // Mock AQI data

                // Show weather data in existing UI
                searchSection.setVisibility(View.GONE);
                cityNameTextView.setVisibility(View.VISIBLE);
                tempTextView.setVisibility(View.VISIBLE);
                conditionLayout.setVisibility(View.VISIBLE);
                aqiLayout.setVisibility(View.VISIBLE);

                // Load weather icon
                String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@4x.png";
                new DownloadImageTask().execute(iconUrl);

                // Get location name using reverse geocoding
                new ReverseGeocodeTask().execute(coordData.getDouble("lat"), coordData.getDouble("lon"));

                // Fetch 5-day forecast
                new FetchForecastTask().execute(coordData.getDouble("lat"), coordData.getDouble("lon"));

                // Show forecast card
                forecastCard.setVisibility(View.VISIBLE);

                // Update background
                updateBackground(iconCode);

                // Store current coordinates for forecast activity
                currentLat = coordData.getDouble("lat");
                currentLon = coordData.getDouble("lon");

            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Lỗi phân tích dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ReverseGeocodeTask extends AsyncTask<Double, Void, String> {
        @Override
        protected String doInBackground(Double... params) {
            double lat = params[0];
            double lon = params[1];

            try {
                // Try Vietnamese locale first
                Geocoder geocoder = new Geocoder(MainActivity.this, new Locale("vi", "VN"));
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 3);
                if (addresses != null && !addresses.isEmpty()) {
                    for (Address address : addresses) {
                        String locationName = "";

                        // Priority order: Locality -> SubAdminArea -> AdminArea -> CountryName
                        if (address.getLocality() != null && !address.getLocality().isEmpty()) {
                            locationName = address.getLocality();
                        } else if (address.getSubAdminArea() != null && !address.getSubAdminArea().isEmpty()) {
                            locationName = address.getSubAdminArea();
                        } else if (address.getAdminArea() != null && !address.getAdminArea().isEmpty()) {
                            locationName = address.getAdminArea();
                        } else if (address.getCountryName() != null && !address.getCountryName().isEmpty()) {
                            locationName = address.getCountryName();
                        }

                        // Filter out unwanted results and prioritize Vietnamese locations
                        if (!locationName.isEmpty() &&
                                !locationName.toLowerCase().contains("mountain view") &&
                                !locationName.toLowerCase().contains("california") &&
                                !locationName.toLowerCase().contains("united states")) {

                            // Special handling for Hanoi area
                            if (lat >= 20.9 && lat <= 21.1 && lon >= 105.7 && lon <= 105.9) {
                                if (locationName.toLowerCase().contains("hanoi") ||
                                        locationName.toLowerCase().contains("hà nội") ||
                                        locationName.toLowerCase().contains("ha noi")) {
                                    return "Hà Nội";
                                }
                            }

                            return locationName;
                        }
                    }
                }

                // Fallback to default locale if Vietnamese doesn't work
                geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    if (address.getLocality() != null) {
                        return address.getLocality();
                    } else if (address.getSubAdminArea() != null) {
                        return address.getSubAdminArea();
                    } else if (address.getAdminArea() != null) {
                        return address.getAdminArea();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Fallback: Use OpenWeatherMap reverse geocoding
            try {
                String url = "https://api.openweathermap.org/geo/1.0/reverse?lat=" + lat + "&lon=" + lon + "&limit=1&appid=" + API_KEY;
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject location = jsonArray.getJSONObject(0);
                    String name = location.optString("name", "");
                    String state = location.optString("state", "");
                    String country = location.optString("country", "");

                    // Special handling for Hanoi coordinates
                    if (lat >= 20.9 && lat <= 21.1 && lon >= 105.7 && lon <= 105.9) {
                        if (name.toLowerCase().contains("hanoi") || name.toLowerCase().contains("hà nội")) {
                            return "Hà Nội";
                        }
                        // If in Hanoi area but name doesn't contain Hanoi, force return Hanoi
                        return "Hà Nội";
                    }

                    if (!name.isEmpty()) {
                        return name;
                    } else if (!state.isEmpty()) {
                        return state;
                    } else if (!country.isEmpty()) {
                        return country;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return "Vị trí hiện tại";
        }

        @Override
        protected void onPostExecute(String locationName) {
            // Update city name in existing UI
            cityNameTextView.setText(locationName);
            currentCityName = locationName;
        }
    }

    private class DownloadCurrentWeatherImageTask extends AsyncTask<String, Void, Bitmap> {
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