package com.example.dubaothoitiet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dubaothoitiet.R;
import com.example.dubaothoitiet.models.Forecast;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private List<Forecast> forecastList;

    public ForecastAdapter(List<Forecast> forecastList) {
        this.forecastList = forecastList;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_item, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        Forecast forecast = forecastList.get(position);
        holder.dayTextView.setText(forecast.getDay());
        holder.tempMinTextView.setText(forecast.getTempMin());
        holder.tempRangeTextView.setText(forecast.getTempMax());
        Picasso.get().load("https://openweathermap.org/img/w/" + forecast.getIcon() + ".png").into(holder.weatherIconImageView);
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView;
        ImageView weatherIconImageView;
        TextView tempMinTextView;
        TextView tempRangeTextView;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.dayTextView);
            weatherIconImageView = itemView.findViewById(R.id.weatherIconImageView);
            tempMinTextView = itemView.findViewById(R.id.tempMinTextView);
            tempRangeTextView = itemView.findViewById(R.id.tempRangeTextView);
        }
    }
}
