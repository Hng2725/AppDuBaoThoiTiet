package com.example.dubaothoitiet;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
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

        // Detail button click handler - Navigate to weather details
        ImageView detailButton = findViewById(R.id.detailButton);
        detailButton.setOnClickListener(v -> {
            if (currentCityName != null && currentLat != 0 && currentLon != 0) {
                Intent intent = new Intent(MainActivity.this, WeatherDetailActivity.class);
                intent.putExtra("CITY_NAME", currentCityName);
                intent.putExtra("LAT", currentLat);
                intent.putExtra("LON", currentLon);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Vui lòng tìm kiếm thành phố trước", Toast.LENGTH_SHORT).show();
            }
        });

        // Add button click handler - Show dialog to input city name
        ImageView addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            showAddCityDialog();
        });

        // Menu button click handler - Manual location input for testing
        ImageView menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            showLocationTestDialog();
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
        Toast.makeText(this, String.format("Testing với tọa độ: %.6f, %.6f", lat, lon), Toast.LENGTH_SHORT).show();
        new FetchWeatherByCoordinatesTask().execute(lat, lon);
    }

    private void showAddCityDialog() {
        // Create an EditText for the dialog
        final EditText input = new EditText(this);
        input.setHint("Ví dụ: Hanoi, Tokyo, Paris");
        input.setPadding(50, 30, 50, 30);
        
        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm thành phố");
        builder.setMessage("Nhập tên thành phố bạn muốn xem thời tiết:");
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("Tìm kiếm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String cityName = input.getText().toString().trim();
                if (cityName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Vui lòng nhập tên thành phố", Toast.LENGTH_SHORT).show();
                } else {
                    // Search for the city weather
                    new FetchWeatherTask().execute(cityName);
                }
            }
        });
        
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }

    private void showLocationTestDialog() {
        // Create a custom layout for lat/lon input
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);
        
        final EditText latInput = new EditText(this);
        latInput.setHint("Latitude (VD: 21.028511)");
        latInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        
        final EditText lonInput = new EditText(this);
        lonInput.setHint("Longitude (VD: 105.804817)");
        lonInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        
        layout.addView(latInput);
        layout.addView(lonInput);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test vị trí GPS");
        builder.setMessage("Nhập tọa độ để test:\n\nMột số vị trí phổ biến:\n• Hà Nội: 21.028511, 105.804817\n• TP.HCM: 10.762622, 106.660172\n• Đà Nẵng: 16.047079, 108.206230");
        builder.setView(layout);
        
        builder.setPositiveButton("Test", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String latStr = latInput.getText().toString().trim();
                String lonStr = lonInput.getText().toString().trim();
                
                if (latStr.isEmpty() || lonStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Vui lòng nhập đầy đủ tọa độ", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        double lat = Double.parseDouble(latStr);
                        double lon = Double.parseDouble(lonStr);
                        testLocationWithCoordinates(lat, lon);
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Tọa độ không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
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
                    float accuracy = location.getAccuracy();

                    // Only use location if accuracy is reasonable (less than 500m)
                    if (accuracy > 500) {
                        Toast.makeText(MainActivity.this,
                                String.format("Độ chính xác thấp (%.0fm), đang chờ GPS tốt hơn...", accuracy),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Stop location updates after getting first accurate location
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

            // Request location updates from both providers for better accuracy
            boolean requestedUpdate = false;
            
            // Prefer GPS for better accuracy
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, locationListener);
                requestedUpdate = true;
            }
            
            // Also use network as fallback
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, locationListener);
                requestedUpdate = true;
            }
            
            if (!requestedUpdate) {
                Toast.makeText(this, "Vui lòng bật GPS hoặc kết nối mạng", Toast.LENGTH_SHORT).show();
                searchSection.setVisibility(View.VISIBLE);
                return;
            }

            // Get last known location as immediate fallback
            Location lastKnownGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            // Choose the best available location
            Location bestLocation = null;
            if (lastKnownGPS != null && lastKnownNetwork != null) {
                // Prefer GPS if it's more recent or more accurate
                long gpsAge = System.currentTimeMillis() - lastKnownGPS.getTime();
                long networkAge = System.currentTimeMillis() - lastKnownNetwork.getTime();
                
                if (gpsAge < networkAge && gpsAge < 5 * 60 * 1000) {
                    bestLocation = lastKnownGPS;
                } else if (networkAge < 5 * 60 * 1000) {
                    bestLocation = lastKnownNetwork;
                }
            } else if (lastKnownGPS != null) {
                long gpsAge = System.currentTimeMillis() - lastKnownGPS.getTime();
                if (gpsAge < 5 * 60 * 1000) {
                    bestLocation = lastKnownGPS;
                }
            } else if (lastKnownNetwork != null) {
                long networkAge = System.currentTimeMillis() - lastKnownNetwork.getTime();
                if (networkAge < 5 * 60 * 1000) {
                    bestLocation = lastKnownNetwork;
                }
            }

            if (bestLocation != null) {
                double lat = bestLocation.getLatitude();
                double lon = bestLocation.getLongitude();
                new FetchWeatherByCoordinatesTask().execute(lat, lon);
            } else {
                Toast.makeText(this, "Đang lấy vị trí hiện tại...", Toast.LENGTH_SHORT).show();
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
                // Try to get location name using Geocoder
                Geocoder geocoder = new Geocoder(MainActivity.this, new Locale("vi", "VN"));
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    
                    // Get all available location components
                    String subLocality = address.getSubLocality();  // Quận/Huyện
                    String locality = address.getLocality();        // Thành phố
                    String subAdminArea = address.getSubAdminArea(); // Tỉnh/Thành phố cấp cao hơn
                    String adminArea = address.getAdminArea();      // Vùng
                    String countryName = address.getCountryName();  // Quốc gia
                    
                    // Build location name intelligently
                    String locationName = "";
                    
                    // For cities: show district + city (e.g., "Cầu Giấy, Hà Nội")
                    if (subLocality != null && !subLocality.isEmpty()) {
                        locationName = subLocality;
                        if (locality != null && !locality.isEmpty() && !locality.equals(subLocality)) {
                            locationName += ", " + locality;
                        }
                    }
                    // If no district, show city
                    else if (locality != null && !locality.isEmpty()) {
                        locationName = locality;
                    }
                    // If no city, show province/state
                    else if (subAdminArea != null && !subAdminArea.isEmpty()) {
                        locationName = subAdminArea;
                    }
                    // If no province, show admin area
                    else if (adminArea != null && !adminArea.isEmpty()) {
                        locationName = adminArea;
                    }
                    // Last resort: country name
                    else if (countryName != null && !countryName.isEmpty()) {
                        locationName = countryName;
                    }
                    
                    if (!locationName.isEmpty()) {
                        return locationName;
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