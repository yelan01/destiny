package com.example.destiny;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class RunningService extends Service {

    private static final String TAG = "RunningService";
    private static final String NOTIFICATION_CHANNEL_ID = "RunningChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final float DISTANCE_NOISE_THRESHOLD = 5.0f;

    public static final String ACTION_BROADCAST = RunningService.class.getName() + "Broadcast";
    public static final String EXTRA_DISTANCE = "extra_distance";
    public static final String EXTRA_ELAPSED_TIME = "extra_elapsed_time";
    public static final String EXTRA_LOCATION_NAME = "extra_location_name";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location previousLocation = null;

    private float totalDistance = 0f;
    private long startTime = 0;
    private String currentLocationName = "獲取中...";

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private Geocoder geocoder;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        RunningService getService() {
            return RunningService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.TAIWAN);
        createNotificationChannel();
        createLocationCallback();
        Log.d(TAG, "Service onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // *** 核心修改：接收從 Activity 傳來的初始地名 ***
        if (intent != null && intent.hasExtra("INITIAL_LOCATION_NAME")) {
            currentLocationName = intent.getStringExtra("INITIAL_LOCATION_NAME");
        }

        startForegroundService();
        startLocationUpdates();
        startTime = System.currentTimeMillis();
        startTimer();
        Log.d(TAG, "Running Service Started");
        return START_STICKY;
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                broadcastUpdate();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, SecondActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("跑步追蹤中")
                .setContentText("距離: 0.00 km")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Running Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 秒
                .setMinUpdateIntervalMillis(5000) // 最快 5 秒
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted.", e);
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        processNewLocation(location);
                    }
                }
            }
        };
    }

    private void processNewLocation(Location newLocation) {
        // *** 核心修改：不再從這裡解析地名 ***
        // updateLocationName(newLocation); // <--- 註解掉這一行

        if (previousLocation != null) {
            float distance = previousLocation.distanceTo(newLocation);
            if (distance > DISTANCE_NOISE_THRESHOLD) {
                totalDistance += distance;
                previousLocation = newLocation;
                updateNotification();
            }
        } else {
            previousLocation = newLocation;
        }
    }

    // 這個方法現在已經不再被呼叫，但可以保留以備不時之需
    private void updateLocationName(Location location) {
        // ...
    }

    private void updateNotification() {
        String distanceText = String.format(Locale.getDefault(), "距離: %.2f km", totalDistance / 1000.0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, SecondActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("跑步追蹤中")
                .setContentText(distanceText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build();

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void broadcastUpdate() {
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_DISTANCE, totalDistance);
        intent.putExtra(EXTRA_ELAPSED_TIME, elapsedTime);
        intent.putExtra(EXTRA_LOCATION_NAME, currentLocationName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "Service onDestroy");
    }
}

