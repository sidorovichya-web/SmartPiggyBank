package com.example.smartpiggybank;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {
    private PieChart pieChart;
    private BarChart barChart;
    private LinearLayout statsContainer;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        statsContainer = findViewById(R.id.statsContainer);

        loadStatistics();
    }

    private void loadStatistics() {
        Map<Float, Integer> stats = dbHelper.getStatistics();

        // Сортировка номиналов
        Float[] denominations = {10.0f, 5.0f, 2.0f, 1.0f, 0.5f};

        // Круговая диаграмма
        List<PieEntry> pieEntries = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int barIndex = 0;
        for (float denom : denominations) {
            int count = stats.getOrDefault(denom, 0);
            if (count > 0) {
                float total = denom * count;
                pieEntries.add(new PieEntry(total, String.format(Locale.getDefault(), "%.2f ₽", denom)));
                barEntries.add(new BarEntry(barIndex, count));
                labels.add(String.format(Locale.getDefault(), "%.2f ₽", denom));
                barIndex++;
            }
        }

        // Настройка PieChart
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Распределение");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pieDataSet.setValueTextSize(12f);
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Всего");
        pieChart.animateY(1000);
        pieChart.getLegend().setEnabled(true);

        // Настройка BarChart
        BarDataSet barDataSet = new BarDataSet(barEntries, "Количество монет");
        barDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.7f);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        barChart.getXAxis().setGranularity(1f);
        barChart.animateY(1000);
        barChart.getLegend().setEnabled(false);

        // Текстовая статистика
        statsContainer.removeAllViews();
        float totalAmount = 0;
        int totalCount = 0;

        for (float denom : denominations) {
            int count = stats.getOrDefault(denom, 0);
            if (count > 0) {
                float sum = denom * count;
                totalAmount += sum;
                totalCount += count;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);

                TextView tvDenom = new TextView(this);
                tvDenom.setText(String.format(Locale.getDefault(), "%.2f ₽", denom));
                tvDenom.setTextSize(16);
                tvDenom.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                TextView tvCount = new TextView(this);
                tvCount.setText("× " + count);
                tvCount.setTextSize(16);
                tvCount.setGravity(Gravity.CENTER);
                tvCount.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                TextView tvSum = new TextView(this);
                tvSum.setText(String.format(Locale.getDefault(), "= %.2f ₽", sum));
                tvSum.setTextSize(16);
                tvSum.setGravity(Gravity.END);
                tvSum.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                row.addView(tvDenom);
                row.addView(tvCount);
                row.addView(tvSum);
                statsContainer.addView(row);
            }
        }

        // Итого
        LinearLayout totalRow = new LinearLayout(this);
        totalRow.setOrientation(LinearLayout.HORIZONTAL);
        totalRow.setPadding(0, 16, 0, 8);

        TextView tvTotalLabel = new TextView(this);
        tvTotalLabel.setText("ИТОГО:");
        tvTotalLabel.setTextSize(18);
        tvTotalLabel.setTextColor(Color.parseColor("#6200EE"));
        tvTotalLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotalLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvTotalCount = new TextView(this);
        tvTotalCount.setText(totalCount + " шт.");
        tvTotalCount.setTextSize(18);
        tvTotalCount.setTextColor(Color.parseColor("#6200EE"));
        tvTotalCount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotalCount.setGravity(Gravity.CENTER);
        tvTotalCount.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvTotalSum = new TextView(this);
        tvTotalSum.setText(String.format(Locale.getDefault(), "%.2f ₽", totalAmount));
        tvTotalSum.setTextSize(18);
        tvTotalSum.setTextColor(Color.parseColor("#6200EE"));
        tvTotalSum.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotalSum.setGravity(Gravity.END);
        tvTotalSum.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        totalRow.addView(tvTotalLabel);
        totalRow.addView(tvTotalCount);
        totalRow.addView(tvTotalSum);
        statsContainer.addView(totalRow);
    }
}