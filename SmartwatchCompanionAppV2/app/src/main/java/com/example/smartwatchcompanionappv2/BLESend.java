package com.example.smartwatchcompanionappv2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class BLESend extends Service {

    public static final String BLE_UPDATE = "com.companionApp.BLE_UPDATE";
    private static String TAG = "BLEService";
    private BLEGATT blegatt;
    private BLEUpdateReceiver nReceiver;

    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: Called");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Started BLE Handler Service with ID:" + startId);

        blegatt = new BLEGATT(this.getApplicationContext(), (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE));
        blegatt.connect(MainActivity.currentDevice);

        //init notification receiver
        nReceiver = new BLEUpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLE_UPDATE);
        registerReceiver(nReceiver, filter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Device Disconnected, BLESend service is now ending");
        unregisterReceiver(nReceiver);
        super.onDestroy();
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

}