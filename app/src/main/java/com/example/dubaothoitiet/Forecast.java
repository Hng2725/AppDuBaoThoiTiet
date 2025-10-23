package com.example.dubaothoitiet;

public class Forecast {
    private String day;
    private String icon;
    private String tempRange;

    public Forecast(String day, String icon, String tempRange) {
        this.day = day;
        this.icon = icon;
        this.tempRange = tempRange;
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
}
