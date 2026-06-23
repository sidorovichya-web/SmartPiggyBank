package com.example.smartpiggybank;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private MaterialButton clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v -> showClearDialog());

        loadHistory();
    }

    private void loadHistory() {
        List<Map<String, Object>> history = dbHelper.getHistory();
        adapter = new HistoryAdapter(history);
        recyclerView.setAdapter(adapter);
    }

    private void showClearDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Очистка истории")
                .setMessage("Удалить все записи? Это действие нельзя отменить.")
                .setPositiveButton("Очистить", (dialog, which) -> {
                    dbHelper.clearHistory();
                    loadHistory();
                    Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}