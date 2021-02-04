package com.example.smartwatchcompanionappv2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

/* Receives broadcasts that indicate a device has been found by the background BLE scan
when a device is found it then starts a foreground service to handle the communication to the BLE
device.  */
public class BLEScanReceiver extends BroadcastReceiver {
    private static final String TAG = "BLEReceiver";

    public static final String ACTION_SCANNER_FOUND_DEVICE = "com.smartwatchCompanion.bleReceiver.ACTION_SCANNER_FOUND_DEVICE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Broadcast Receiver Triggered " + intent.toString());

        switch (intent.getAction()) {

            // Look whether we find our device
            case ACTION_SCANNER_FOUND_DEVICE: {
                Bundle extras = intent.getExtras();

                if (extras != null) {
                    Object o = extras.get(BluetoothLeScannerCompat.EXTRA_LIST_SCAN_RESULT);
                    if (o instanceof ArrayList) {
                        ArrayList<ScanResult> scanResults = (ArrayList<ScanResult>) o;
                        Log.v(TAG, "There are " + scanResults.size() + " results");

                        for (ScanResult result : scanResults) {
                            if (result.getScanRecord() == null) {
                                Log.d(TAG, "getScanRecord is null");
                                continue;
                            }

                            BluetoothDevice device = result.getDevice();
                            ScanRecord scanRecord = result.getScanRecord();
                            String scanName = scanRecord.getDeviceName();
                            String deviceName = device.getName();
                            int rssi = result.getRssi();
//                            mHeader.setText("Single device found: " + device.getName() + " RSSI: " + result.getRssi() + "dBm");
                            Log.i(TAG, "Found: " + device.getAddress()
                                    + " scan name: " + scanName
                                    + " device name: " + deviceName
                                    + " RSSI: " + result.getRssi() + "dBm");

                            MainActivity.currentDevice = result.getDevice();
                            if (MainActivity.currentDevice.getName() != null) {
                                try {
                                    if (!BLEService.isRunning) {
//                                        BLEScanner.stopScan(MainActivity.reference);
                                        context.startForegroundService(new Intent(context, BLEService.class));
                                    }
                                } catch (IllegalStateException e) {
                                    Log.e(TAG, "Could not register service");
                                }
                            }
                        }
                    } else {
                        // Received something, but not a list of scan results...
                        Log.d(TAG, "   no ArrayList but " + o);
                    }
                } else {
                    Log.d(TAG, "no extras");
                }

                break;
            }

            case BluetoothAdapter.ACTION_STATE_CHANGED: {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "BLE off");
                        // Need to take some action or app will fail...
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "BLE turning off");
//                        stopScan();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "BLE on");
//                        startScan();    // restart scanning (provided the activity wants this to happen)
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "BLE turning on");
                        break;
                }
                break;
            }
            default:
                // should not happen
                Log.d(TAG, "Received unexpected action " + intent.getAction());

        }
    }
}
