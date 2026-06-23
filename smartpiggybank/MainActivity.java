package com.example.smartpiggybank;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // UI элементы
    private TextView connectionStatus, totalAmount, totalCoins, lastOperation;
    private ImageView bluetoothIcon;
    private MaterialButton connectButton, syncButton, resetButton;
    private MaterialButton statsButton, historyButton;
    private MaterialButton btnCoin50, btnCoin1, btnCoin2, btnCoin5, btnCoin10;

    // Данные
    private DatabaseHelper dbHelper;
    private BluetoothManager bluetoothManager;
    private float totalAmountValue = 0;
    private int totalCoinsCount = 0;

    // Константы
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String DEVICE_NAME = "HC-05";

    // Номиналы монет
    private final float[] coinValues = {0.5f, 1.0f, 2.0f, 5.0f, 10.0f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        bluetoothManager = new BluetoothManager();

        initViews();
        setupBluetooth();
        setupClickListeners();
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectionStatus = findViewById(R.id.connectionStatus);
        totalAmount = findViewById(R.id.totalAmount);
        totalCoins = findViewById(R.id.totalCoins);
        lastOperation = findViewById(R.id.lastOperation);
        bluetoothIcon = findViewById(R.id.bluetoothIcon);

        connectButton = findViewById(R.id.connectButton);
        syncButton = findViewById(R.id.syncButton);
        resetButton = findViewById(R.id.resetButton);
        statsButton = findViewById(R.id.statsButton);
        historyButton = findViewById(R.id.historyButton);

        btnCoin50 = findViewById(R.id.btnCoin50);
        btnCoin1 = findViewById(R.id.btnCoin1);
        btnCoin2 = findViewById(R.id.btnCoin2);
        btnCoin5 = findViewById(R.id.btnCoin5);
        btnCoin10 = findViewById(R.id.btnCoin10);
    }

    private void setupBluetooth() {
        bluetoothManager.setListener(new BluetoothManager.OnDataReceivedListener() {
            @Override
            public void onDataReceived(String data) {
                parseArduinoData(data);
            }

            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    connectionStatus.setText("Подключено к " + DEVICE_NAME);
                    connectionStatus.setTextColor(getResources().getColor(R.color.success));
                    Toast.makeText(MainActivity.this, "Подключено!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    connectionStatus.setText("Не подключено");
                    connectionStatus.setTextColor(getResources().getColor(R.color.error));
                });
            }
        });
    }

    private void setupClickListeners() {
        // Кнопка подключения Bluetooth
        connectButton.setOnClickListener(v -> {
            if (!bluetoothManager.isBluetoothSupported()) {
                Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_LONG).show();
                return;
            }

            if (!bluetoothManager.isBluetoothEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return;
            }

            // Проверяем разрешения для Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                                != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions();
                    return;
                }
            } else {
                // Android 6-11
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions();
                    return;
                }
            }

            // Показываем список сопряжённых устройств
            showPairedDevicesDialog();
        });

        // Кнопка синхронизации
        syncButton.setOnClickListener(v -> {
            if (bluetoothManager.isConnected()) {
                bluetoothManager.sendData("GET_DATA\n");
                Toast.makeText(this, "Запрос данных...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Сначала подключитесь к копилке", Toast.LENGTH_SHORT).show();
            }
        });

        // Кнопка сброса
        resetButton.setOnClickListener(v -> showResetDialog());

        // Кнопки навигации
        statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StatsActivity.class);
            startActivity(intent);
        });

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        // КНОПКИ ИМИТАЦИИ КОПИЛКИ
        btnCoin50.setOnClickListener(v -> addCoinEmulation(0));
        btnCoin1.setOnClickListener(v -> addCoinEmulation(1));
        btnCoin2.setOnClickListener(v -> addCoinEmulation(2));
        btnCoin5.setOnClickListener(v -> addCoinEmulation(3));
        btnCoin10.setOnClickListener(v -> addCoinEmulation(4));
    }

    // Показывает диалог с выбором сопряжённых устройств
    private void showPairedDevicesDialog() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();

        if (bondedDevices == null || bondedDevices.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Нет устройств")
                    .setMessage("Сначала выполните сопряжение с HC-05 в настройках Bluetooth телефона")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] deviceNames = new String[bondedDevices.size()];
        final BluetoothDevice[] devicesArray = bondedDevices.toArray(new BluetoothDevice[0]);

        for (int i = 0; i < devicesArray.length; i++) {
            String name = devicesArray[i].getName();
            String address = devicesArray[i].getAddress();
            deviceNames[i] = (name != null ? name : "Без имени") + "\n" + address;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Выберите устройство")
                .setItems(deviceNames, (dialog, which) -> {
                    BluetoothDevice selectedDevice = devicesArray[which];
                    bluetoothManager.connectToDevice(selectedDevice);
                    Toast.makeText(this, "Подключение к " + selectedDevice.getName() + "...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (включая Android 14)
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        } else {
            // Android 6-11
            String[] permissions = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                showPairedDevicesDialog();
            } else {
                Toast.makeText(this, "Разрешения необходимы для работы Bluetooth", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Добавление монеты через имитацию
    private void addCoinEmulation(int index) {
        float value = coinValues[index];
        dbHelper.addCoin(value, "emulation");
        totalAmountValue += value;
        totalCoinsCount++;
        updateUI();

        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        lastOperation.setText(String.format(Locale.getDefault(),
                "Добавлено %.2f ₽ (имитация) в %s", value, time));

        Toast.makeText(this, String.format(Locale.getDefault(), "+%.2f ₽", value),
                Toast.LENGTH_SHORT).show();
    }

    // Парсинг данных от Arduino
    private void parseArduinoData(String data) {
        // Формат: "COIN:50:1" или "STATS:10,5,3,2,1"
        if (data.startsWith("COIN:")) {
            String[] parts = data.split(":");
            if (parts.length == 3) {
                try {
                    float coinValue = Float.parseFloat(parts[1]) / 100f; // 50 -> 0.50
                    int count = Integer.parseInt(parts[2]);

                    dbHelper.addCoin(coinValue, "real");
                    totalAmountValue += coinValue * count;
                    totalCoinsCount += count;
                    updateUI();

                    String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                    lastOperation.setText(String.format(Locale.getDefault(),
                            "Добавлено %.2f ₽ (Bluetooth) в %s", coinValue * count, time));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } else if (data.startsWith("STATS:")) {
            // Полный сброс и синхронизация
            dbHelper.clearHistory();
            totalAmountValue = 0;
            totalCoinsCount = 0;

            String stats = data.substring(6);
            String[] counts = stats.split(",");
            for (int i = 0; i < Math.min(5, counts.length); i++) {
                try {
                    int count = Integer.parseInt(counts[i]);
                    for (int j = 0; j < count; j++) {
                        dbHelper.addCoin(coinValues[i], "real");
                    }
                    totalAmountValue += coinValues[i] * count;
                    totalCoinsCount += count;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            updateUI();
            Toast.makeText(this, "Данные синхронизированы", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        totalAmount.setText(String.format(Locale.getDefault(), "%.2f ₽", totalAmountValue));
        totalCoins.setText("Всего монет: " + totalCoinsCount);
    }

    private void loadData() {
        totalAmountValue = dbHelper.getTotalAmount();
        totalCoinsCount = dbHelper.getTotalCoins();
        updateUI();
    }

    private void showResetDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Сброс статистики")
                .setMessage("Вы уверены, что хотите удалить всю историю накоплений?")
                .setPositiveButton("Сбросить", (dialog, which) -> {
                    dbHelper.clearHistory();
                    totalAmountValue = 0;
                    totalCoinsCount = 0;
                    updateUI();
                    lastOperation.setText("Нет данных");

                    if (bluetoothManager.isConnected()) {
                        bluetoothManager.sendData("RESET\n");
                    }

                    Toast.makeText(this, "Статистика сброшена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(); // Обновляем данные при возврате с других экранов
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager.disconnect();
    }
}