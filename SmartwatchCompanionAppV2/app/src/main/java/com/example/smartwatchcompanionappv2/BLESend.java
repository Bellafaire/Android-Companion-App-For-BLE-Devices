package com.example.smartwatchcompanionappv2;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BLESend extends Service {

    public static final String BLE_UPDATE = "com.companionApp.BLE_UPDATE";
    public static final String CHANNEL_ID = "com.companionApp.UPDATE_SERVICE";
    private static String TAG = "BLEService";
    private BLEGATT blegatt;
    private BLEUpdateReceiver nReceiver;
    public static Boolean isRunning = false;

    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: Called");


        createNotificationChannel();

        Intent notificationIntent = new Intent(this.getApplicationContext(), BLESend.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this.getApplicationContext(), 300, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this.getApplicationContext(), CHANNEL_ID)
                .setContentTitle("ESP32 Smartwatch")
                .setContentText("Device Connected")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Started BLE Handler Service with ID:" + startId);
        isRunning = true;

        //so basically, see if we can unregister the broadcast receiver
        //if we can't its no big deal because it probably doesn't exist yet
        try {
            unregisterReceiver(nReceiver);
        } catch(Exception e) {
            //this is basically designed to crash so eh whatever
        }


        //try and create a new one, if the last block failed then we will have no problems
        try {
            //init notification receiver
            nReceiver = new BLEUpdateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BLE_UPDATE);
            registerReceiver(nReceiver, filter);
            Log.i(TAG, "Re-registered broadcast reciever");
        } catch(IllegalArgumentException e) {
            //this is basically designed to crash so eh whatever
        }


        MainActivity.updateStatusText();

        blegatt = new BLEGATT(this.getApplicationContext());
        blegatt.connect(MainActivity.currentDevice);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "Device Disconnected, BLESend service is now ending");
//        unregisterReceiver(nReceiver);
        isRunning = false;

        Log.i(TAG, "Restarting Scan");
        BLEScanner.startScan(getApplicationContext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BLEUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Bluetooth update request received");

            while (blegatt.update()) ;
        }
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