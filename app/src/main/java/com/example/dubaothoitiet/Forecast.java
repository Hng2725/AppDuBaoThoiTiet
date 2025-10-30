package com.example.dubaothoitiet;

public class Forecast {
    private String day;
    private String icon;
    private String tempRange;
    private String tempMin;
    private String tempMax;

    // Constructor for old format (backward compatibility)
    public Forecast(String day, String icon, String tempRange) {
        this.day = day;
        this.icon = icon;
        this.tempRange = tempRange;
        this.tempMin = tempRange;
        this.tempMax = tempRange;
    }

    // Constructor for new format with separate min/max
    public Forecast(String day, String icon, String tempMin, String tempMax) {
        this.day = day;
        this.icon = icon;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.tempRange = tempMax; // For backward compatibility
    }

    public String getDay() {
        return day;
    }

    public String getIcon() {
        return icon;
    }

    public String getTempRange() {
        return tempRange;
    }

    public String getTempMin() {
        return tempMin;
    }

    public String getTempMax() {
        return tempMax;
    }
}
