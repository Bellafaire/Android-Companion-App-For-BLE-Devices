package com.example.smartwatchcompanionappv2;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class BLEScanner {

    private static String TAG = "BLE";

    public static void startScan(Context con) {
        Log.i(TAG, "----------------- Starting BLE Scan ---------------------------");
        Intent intent = new Intent(con, BLEReceiver.class); // explicite intent
        intent.setAction(BLEReceiver.ACTION_SCANNER_FOUND_DEVICE);
//        intent.putExtra("some.extra", value); // optional
        PendingIntent pendingIntent = PendingIntent.getBroadcast(con, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setUseHardwareBatchingIfSupported(true)
                .setReportDelay(100)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(MainActivity.serviceUUID))
                .build());
        scanner.startScan(filters, settings, con, pendingIntent);
    }


    public static void stopScan(Context con) {
        // To stop scanning use the same or an equal PendingIntent (check PendingIntent documentation)
        Intent intent = new Intent(con, BLEReceiver.class);
        intent.setAction("com.smartwatchCompanion.bleReciever.ACTION_SCANNER_FOUND_DEVICE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(con, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(con, pendingIntent);
    }

    static ScanCallback scb = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "Found Device with name:" + result.getDevice().getName());
        }
    };


}
