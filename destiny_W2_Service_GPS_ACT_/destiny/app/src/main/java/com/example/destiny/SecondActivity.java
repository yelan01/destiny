package com.example.destiny;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SecondActivity extends AppCompatActivity {

    private TextView tvDistance, tvTime, tvLocationName;
    private Button btnStartStop, btnNextStep;

    private boolean isTracking = false;
    private float totalDistance = 0f;
    private long elapsedTime = 0;
    private int consecutiveClickCount = 0;
    private long lastClickTime = 0;

    // 解鎖條件
    private static final int CLICK_THRESHOLD_MS = 1500;
    private static final int CLICK_COUNT_TO_UNLOCK = 5;
    private static final float TARGET_DISTANCE_KM = 0.1f;
    private static final int TARGET_TIME_SECONDS = 10;

    private FusedLocationProviderClient fusedLocationClient;

    private ActivityResultLauncher<String[]> permissionRequest;

    private final BroadcastReceiver runningUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(RunningService.ACTION_BROADCAST)) {
                totalDistance = intent.getFloatExtra(RunningService.EXTRA_DISTANCE, 0);
                elapsedTime = intent.getLongExtra(RunningService.EXTRA_ELAPSED_TIME, 0);
                String locationNameFromService = intent.getStringExtra(RunningService.EXTRA_LOCATION_NAME);

                updateUI(locationNameFromService);
                checkUnlockConditions();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initPermissionLauncher();

        tvDistance = findViewById(R.id.tv_distance);
        tvTime = findViewById(R.id.tv_time);
        tvLocationName = findViewById(R.id.tv_location_name);
        btnStartStop = findViewById(R.id.btn_start_stop);
        btnNextStep = findViewById(R.id.btn_next_step);

        btnStartStop.setOnClickListener(v -> handleStartStopClick());
        btnNextStep.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ThirdActivity.class);
            intent.putExtra("RUN_DISTANCE", totalDistance);
            intent.putExtra("RUN_TIME", elapsedTime);
            startActivity(intent);
        });

        fetchInitialLocation();
    }

    @SuppressLint("MissingPermission")
    private void fetchInitialLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            updateLocationNameFromCoords(location.getLatitude(), location.getLongitude());
                        } else {
                            tvLocationName.setText("無法獲取位置");
                        }
                    });
        } else {
            tvLocationName.setText("沒有定位權限");
        }
    }

    private void updateLocationNameFromCoords(double lat, double lon) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.TAIWAN);
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                final String locationName;
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String city = address.getAdminArea();
                    String district = address.getLocality();
                    StringBuilder nameBuilder = new StringBuilder();
                    if (city != null) nameBuilder.append(city);
                    if (district != null) nameBuilder.append(" ").append(district);

                    if (nameBuilder.length() > 0) {
                        locationName = nameBuilder.toString();
                    } else {
                        locationName = "未知區域";
                    }
                } else {
                    locationName = "無法解析地名";
                }

                runOnUiThread(() -> tvLocationName.setText(locationName));

            } catch (IOException e) {
                Log.e("SecondActivity", "Geocoder failed", e);
                runOnUiThread(() -> tvLocationName.setText("位置服務錯誤"));
            }
        }).start();
    }


    private void initPermissionLauncher() {
        permissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean postNotificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permissions.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);

                    if (fineLocationGranted && (postNotificationsGranted != null && postNotificationsGranted)) {
                        toggleTracking();
                    } else {
                        Toast.makeText(this, "需要定位和通知權限才能開始跑步", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleStartStopClick() {
        if (isTracking) {
            toggleTracking();
            handleConsecutiveClicks();
        } else {
            checkPermissionsAndStart();
        }
    }

    private void checkPermissionsAndStart() {
        String[] requiredPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS};
        } else {
            requiredPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            toggleTracking();
        } else {
            permissionRequest.launch(requiredPermissions);
        }
    }

    private void toggleTracking() {
        Intent serviceIntent = new Intent(this, RunningService.class);
        isTracking = !isTracking;
        if (isTracking) {
            // *** 核心修改：在啟動 Service 前，將地名放入 Intent ***
            String initialLocationName = tvLocationName.getText().toString();
            serviceIntent.putExtra("INITIAL_LOCATION_NAME", initialLocationName);

            btnStartStop.setText("停止");
            resetUIForNewRun();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            btnStartStop.setText("開始");
            stopService(serviceIntent);
        }
    }

    private void resetUIForNewRun() {
        totalDistance = 0f;
        elapsedTime = 0;
        tvDistance.setText("0.00");
        tvTime.setText("00:00:00");
        btnNextStep.setEnabled(false);
    }

    private void handleConsecutiveClicks() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_THRESHOLD_MS) {
            consecutiveClickCount++;
        } else {
            consecutiveClickCount = 1;
        }
        lastClickTime = currentTime;

        if (consecutiveClickCount >= CLICK_COUNT_TO_UNLOCK) {
            unlockNextButton("連續點擊解鎖");
        }
    }

    private void updateUI(String locationName) {
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", totalDistance / 1000.0));

        long hours = elapsedTime / 3600;
        long minutes = (elapsedTime % 3600) / 60;
        long seconds = elapsedTime % 60;
        tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));

        if (locationName != null) {
            tvLocationName.setText(locationName);
        }
    }

    private void checkUnlockConditions() {
        if (!btnNextStep.isEnabled()) {
            if ((totalDistance / 1000.0) >= TARGET_DISTANCE_KM) {
                unlockNextButton("跑步距離達標");
            }
            else if (elapsedTime >= TARGET_TIME_SECONDS) {
                unlockNextButton("跑步時間達標");
            }
        }
    }

    private void unlockNextButton(String reason) {
        if (!btnNextStep.isEnabled()) {
            btnNextStep.setEnabled(true);
            Toast.makeText(this, "已解鎖下一步 (" + reason + ")", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(runningUpdateReceiver,
                new IntentFilter(RunningService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(runningUpdateReceiver);
    }
}
