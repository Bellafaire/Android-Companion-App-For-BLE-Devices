package com.example.smartwatchcompanionappv2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class BLEGATT {
    static private boolean mConnected = false;
    static private String lastConnected = "";


    private static String TAG = "BLEGATT";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Context con;

    public BLEGATT(Context c, BluetoothManager bm) {
        con = c;
        bluetoothAdapter = bm.getAdapter();
    }

    public static String getStatusText() {
        String ret = "";

        if (mConnected) {
            ret += "Connection Status: Connected";
        } else {
            ret += "Connection Status: Disconnected";
        }
        ret += "\n";

        ret += "Last Connected: " + lastConnected + "\n";

        Log.v(TAG, ret);

        return ret;
    }

    public void connect(ScanResult sr) {
        bluetoothGatt = sr.getDevice().connectGatt(con, true, gattCallback);
    }

    public void write(String str) {
        if (bluetoothGatt != null) {
            BluetoothGattService bgs = bluetoothGatt.getService(UUID.fromString(MainActivity.serviceUUID));
            if (bgs != null) {
                Log.d(TAG, "Accessed service" + bgs.toString());
                BluetoothGattCharacteristic bgc = bgs.getCharacteristic(UUID.fromString(MainActivity.charUUID));
                bgc.setValue(str);
                Log.d(TAG, "Writing " + MainActivity.charUUID + " with: " + str);
                bluetoothGatt.writeCharacteristic(bgc);
            } else {
                Log.e(TAG, "bluetooth gatt service is null");
            }
        } else {
            Log.e(TAG, "bluetoothGatt is NULL");
        }
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "Connection State is now: " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {

                mConnected = true;
                lastConnected = getDateAndTime();

//                        broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        bluetoothGatt.discoverServices());


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                lastConnected = getDateAndTime();

                Log.i(TAG, "Disconnected from GATT server.");
            }

            MainActivity.updateStatusText();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            //check if we can get notifications from the device
            if ((bluetoothGatt.getService(UUID.fromString(MainActivity.serviceUUID))
                    .getCharacteristic(UUID.fromString(MainActivity.charUUID))
                    .getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                Log.d(TAG, "Device can notify");
                bluetoothGatt.setCharacteristicNotification(bluetoothGatt.getService(UUID.fromString(MainActivity.serviceUUID))
                        .getCharacteristic(UUID.fromString(MainActivity.charUUID)), true);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (gatt.readCharacteristic(characteristic)) {
                Log.v(TAG, "characteristic changed to: " + new String(characteristic.getValue(), StandardCharsets.US_ASCII));
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    //returns a string representing the date and time
    public static String getDateAndTime() {
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        String formattedDate = df.format(c);
        return formattedDate;

    }
}
