package com.example.dubaothoitiet;

public class Forecast {
    private String day;
    private String icon;
    private String tempRange;
    private String tempMin;
    private String tempMax;


    public Forecast(String day, String icon, String tempRange) {
        this.day = day;
        this.icon = icon;
        this.tempRange = tempRange;
        this.tempMin = tempRange;
        this.tempMax = tempRange;
    }


    public Forecast(String day, String icon, String tempMin, String tempMax) {
        this.day = day;
        this.icon = icon;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.tempRange = tempMax;
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
