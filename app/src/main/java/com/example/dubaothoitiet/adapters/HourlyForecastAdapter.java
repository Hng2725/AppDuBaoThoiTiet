package com.example.dubaothoitiet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dubaothoitiet.R;
import com.example.dubaothoitiet.models.HourlyForecast;
import com.squareup.picasso.Picasso;
import java.util.List;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.HourlyViewHolder> {

    private List<HourlyForecast> hourlyList;

    public HourlyForecastAdapter(List<HourlyForecast> hourlyList) {
        this.hourlyList = hourlyList;
    }

    @NonNull
    @Override
    public HourlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hourly_item, parent, false);
        return new HourlyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourlyViewHolder holder, int position) {
        HourlyForecast hourly = hourlyList.get(position);
        holder.timeTextView.setText(hourly.getTime());
        holder.tempTextView.setText(hourly.getTemperature());
        Picasso.get().load("https://openweathermap.org/img/w/" + hourly.getIcon() + ".png").into(holder.iconImageView);
    }

    @Override
    public int getItemCount() {
        return hourlyList.size();
    }

    static class HourlyViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        ImageView iconImageView;
        TextView tempTextView;

        public HourlyViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.hourlyTimeTextView);
            iconImageView = itemView.findViewById(R.id.hourlyIconImageView);
            tempTextView = itemView.findViewById(R.id.hourlyTempTextView);
        }
    }
}
