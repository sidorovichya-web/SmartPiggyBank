package com.example.smartpiggybank;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PiggyBank.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DENOMINATION = "denomination";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_SOURCE = "source"; // "real" или "emulation"

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DENOMINATION + " REAL NOT NULL, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                COLUMN_SOURCE + " TEXT NOT NULL)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // Добавить монету в историю
    public void addCoin(float denomination, String source) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DENOMINATION, denomination);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        values.put(COLUMN_SOURCE, source);
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    // Получить всю историю
    public List<Map<String, Object>> getHistory() {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null,
                COLUMN_TIMESTAMP + " DESC");

        while (cursor.moveToNext()) {
            Map<String, Object> item = new HashMap<>();
            item.put(COLUMN_ID, cursor.getInt(0));
            item.put(COLUMN_DENOMINATION, cursor.getDouble(1));
            item.put(COLUMN_TIMESTAMP, cursor.getLong(2));
            item.put(COLUMN_SOURCE, cursor.getString(3));
            list.add(item);
        }
        cursor.close();
        db.close();
        return list;
    }

    // Получить статистику по номиналам
    public Map<Float, Integer> getStatistics() {
        Map<Float, Integer> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_DENOMINATION +
                ", COUNT(*) as count FROM " + TABLE_HISTORY +
                " GROUP BY " + COLUMN_DENOMINATION, null);

        while (cursor.moveToNext()) {
            float denom = cursor.getFloat(0);
            int count = cursor.getInt(1);
            stats.put(denom, count);
        }
        cursor.close();
        db.close();
        return stats;
    }

    // Получить общую сумму
    public float getTotalAmount() {
        float total = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_DENOMINATION +
                ") FROM " + TABLE_HISTORY, null);
        if (cursor.moveToFirst()) {
            total = cursor.getFloat(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    // Получить общее количество монет
    public int getTotalCoins() {
        int total = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    // Очистить историю
    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }
}