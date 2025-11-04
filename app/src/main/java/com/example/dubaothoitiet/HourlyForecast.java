package com.example.dubaothoitiet;

public class HourlyForecast {
    private String time;
    private String icon;
    private String temperature;
    private String description;

    public HourlyForecast(String time, String icon, String temperature, String description) {
        this.time = time;
        this.icon = icon;
        this.temperature = temperature;
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public String getIcon() {
        return icon;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getDescription() {
        return description;
    }
}
