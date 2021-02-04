package com.example.smartwatchcompanionappv2;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

/* Initiates a pendingIntent scan in the background, allowing the android device to locate a BLE device without
the screen even being on, uses the nordic semiconductor library Android Scanner Compat

 For the most part this is just the example code provided by the library documentation on github
 */
public class BLEScanner {

    private static String TAG = "BLE";
    private static int code = 4000;

    public static void startScan(Context con) {
        Log.i(TAG, "----------------- Starting BLE Scan ---------------------------");
        Intent intent = new Intent(con, BLEScanReceiver.class); // explicite intent
        intent.setAction(BLEScanReceiver.ACTION_SCANNER_FOUND_DEVICE);
//        intent.putExtra("some.extra", value); // optional
        PendingIntent pendingIntent = PendingIntent.getBroadcast(con, code, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setUseHardwareBatchingIfSupported(true)
                .setReportDelay(100)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(MainActivity.SERVICE_UUID))
                .build());
        scanner.startScan(filters, settings, con, pendingIntent);
    }


    public static void stopScan(Context con) {
        // To stop scanning use the same or an equal PendingIntent (check PendingIntent documentation)
        Intent intent = new Intent(con, BLEScanReceiver.class);
        intent.setAction("com.smartwatchCompanion.bleReciever.ACTION_SCANNER_FOUND_DEVICE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(con, code, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(con, pendingIntent);
    }
}
