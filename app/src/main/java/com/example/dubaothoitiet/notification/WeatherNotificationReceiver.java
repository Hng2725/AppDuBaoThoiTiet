package com.example.dubaothoitiet.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class WeatherNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "WeatherNotification";
    private static final String PREFS_NAME = "WeatherPrefs";
    private static final String PREF_CITY = "last_city";
    private static final String PREF_LAT = "last_lat";
    private static final String PREF_LON = "last_lon";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Notification receiver triggered");
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule notification after device reboot
            scheduleDailyNotification(context);
        } else {
            // Fetch weather and show notification
            fetchWeatherAndNotify(context);
        }
    }

    private void fetchWeatherAndNotify(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String city = prefs.getString(PREF_CITY, "");
        double lat = Double.longBitsToDouble(prefs.getLong(PREF_LAT, Double.doubleToLongBits(0.0)));
        double lon = Double.longBitsToDouble(prefs.getLong(PREF_LON, Double.doubleToLongBits(0.0)));

        if (!city.isEmpty() && lat != 0.0 && lon != 0.0) {
            // Fetch weather data in background
            new FetchWeatherTask(context, city).execute(lat, lon);
        } else {
            Log.d(TAG, "No location data available for notification");
        }
    }

    public static void scheduleDailyNotification(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        Intent intent = new Intent(context, WeatherNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 16);
        calendar.set(Calendar.MINUTE, 23);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "Exact daily notification scheduled for 7:00 AM");
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "Inexact daily notification scheduled for around 7:00 AM");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Exact daily notification scheduled for 7:00 AM");
            } else {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
                Log.d(TAG, "Repeating daily notification scheduled for 7:00 AM");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to schedule notification: " + e.getMessage());
        }
    }

    public static void saveLocationData(Context context, String city, double lat, double lon) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_CITY, city);
        editor.putLong(PREF_LAT, Double.doubleToLongBits(lat));
        editor.putLong(PREF_LON, Double.doubleToLongBits(lon));
        editor.apply();
        Log.d(TAG, "Location data saved: " + city);
    }

    public static void testNotificationNow(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        Intent intent = new Intent(context, WeatherNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                999, // Different request code for test notification
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + 5000;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Test notification scheduled in 5 seconds (exact)");
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Test notification scheduled in ~5 seconds (inexact)");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Test notification scheduled in 5 seconds");
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Test notification scheduled in 5 seconds");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to schedule test notification: " + e.getMessage());
        }
    }

    private static class FetchWeatherTask extends android.os.AsyncTask<Double, Void, String> {
        private Context context;
        private String cityName;
        private final String API_KEY = "b0ecf12a1f927381cd92f75e03a07904";

        FetchWeatherTask(Context context, String cityName) {
            this.context = context;
            this.cityName = cityName;
        }

        @Override
        protected String doInBackground(Double... params) {
            double lat = params[0];
            double lon = params[1];

            try {
                String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + 
                                   "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&lang=vi";
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching weather: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    org.json.JSONObject jsonObject = new org.json.JSONObject(result);
                    org.json.JSONObject main = jsonObject.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    int tempInt = (int) Math.round(temp);
                    
                    org.json.JSONArray weatherArray = jsonObject.getJSONArray("weather");
                    org.json.JSONObject weather = weatherArray.getJSONObject(0);
                    String description = weather.getString("description");

                    String message = "Chúc ngày mới tốt lành! Nhiệt độ hôm nay là " + tempInt + "°C, " + description + ".";
                    
                    NotificationHelper.showWeatherNotification(context, cityName, message);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error parsing weather data: " + e.getMessage());
                }
            }
        }
    }
}
