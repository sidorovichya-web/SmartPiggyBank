package com.example.smartpiggybank;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<Map<String, Object>> historyList;

    public HistoryAdapter(List<Map<String, Object>> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = historyList.get(position);
        float denomination = ((Number) item.get("denomination")).floatValue();
        long timestamp = (Long) item.get("timestamp");
        String source = (String) item.get("source");

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        String dateStr = sdf.format(new Date(timestamp));

        holder.dateText.setText(dateStr);
        holder.typeText.setText(source.equals("real") ? "Bluetooth" : "Имитация");
        holder.descriptionText.setText(String.format(Locale.getDefault(),
                "Монета %.2f ₽", denomination));
        holder.amountText.setText(String.format(Locale.getDefault(), "+%.2f ₽", denomination));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateData(List<Map<String, Object>> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, typeText, descriptionText, amountText;

        ViewHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.historyDate);
            typeText = itemView.findViewById(R.id.historyType);
            descriptionText = itemView.findViewById(R.id.historyDescription);
            amountText = itemView.findViewById(R.id.historyAmount);
        }
    }
}