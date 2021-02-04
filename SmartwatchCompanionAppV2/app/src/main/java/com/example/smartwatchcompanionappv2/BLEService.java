package com.example.smartwatchcompanionappv2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BLEService extends Service {


    private static String TAG = "BLEService";
    private BLEGATT blegatt;
    private static BLEService reference;
    public static final String CHANNEL_ID = "com.companionApp.UPDATE_SERVICE";

    public static Boolean isRunning = false;

    public void onCreate() {
        super.onCreate();
        reference = this;
        Log.i(TAG, "onCreate: Called");


        createNotificationChannel();

        Intent notificationIntent = new Intent(this.getApplicationContext(), BLEService.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this.getApplicationContext(), 300, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this.getApplicationContext(), CHANNEL_ID)
                .setContentTitle("ESP32 Smartwatch")
                .setContentText("Device Connected")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);




    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Started BLE Handler Service with ID:" + startId);
        isRunning = true;


        MainActivity.updateStatusText();

        blegatt = new BLEGATT(this.getApplicationContext());
        blegatt.connect(MainActivity.currentDevice);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "Device Disconnected, BLESend service is now ending");

        isRunning = false;

//        Log.i(TAG, "Restarting Scan");
//        BLEScanner.startScan(getApplicationContext());
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void updateServer(){
        while (blegatt.update()) ;
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}