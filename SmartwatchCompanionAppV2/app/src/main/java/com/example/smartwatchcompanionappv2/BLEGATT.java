package com.example.smartwatchcompanionappv2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BLEGATT {
    static private boolean mConnected = false;
    static private boolean readyToSend = false;
    static private String lastConnected = "";

    public static String currentMessage = "";
    public static String currentUUID = MainActivity.COMMAND_UUID;

    private static String TAG = "BLEGATT";

    private BluetoothAdapter bluetoothAdapter;
    private static BluetoothGatt bluetoothGatt;
    private Context con;
    private BluetoothGattService deviceService;

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

    public void connect(BluetoothDevice sr) {
//        Log.i(TAG, "attempting to connect to device: "+ sr.getName() + " With UUID " + sr.getUuids().toString());
        if (!mConnected) {
            bluetoothGatt = sr.connectGatt(con, true, gattCallback);
            Log.i(TAG, "Connected to BLEGatt Server");
        }
    }

    public boolean write(String str, String uuid) {
        BluetoothGattCharacteristic bgc = deviceService.getCharacteristic(UUID.fromString(uuid));
        bgc.setValue(str);
        bluetoothGatt.writeCharacteristic(bgc);
        return true;
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

            if(gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)) != null) {
                deviceService = gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID));
                Log.d(TAG, deviceService.getUuid().toString());
                Log.i(TAG, "Obtained service");
            }
            List<BluetoothGattCharacteristic> chars = gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getCharacteristics();

            for (int a = 0; a < chars.size(); a++) {
                if ((chars.get(a).getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    bluetoothGatt.setCharacteristicNotification(chars.get(a), true);
                    Log.i(TAG, "Subscribed to characteristic: " + chars.get(a).getUuid().toString());
                }
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
            Log.i(TAG, "Wrote Characteristic with status of result: " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            gatt.readCharacteristic(characteristic);
            Log.i(TAG, "Characteristic " + characteristic.getUuid().toString() + " Has changed");
            if (characteristic.getUuid().equals(UUID.fromString(MainActivity.COMMAND_UUID))) {
                gatt.readCharacteristic(characteristic);

                String command = new String(characteristic.getValue(), StandardCharsets.US_ASCII);
                Log.i(TAG, "Command Characteristic Changed to: " + command);


                if (command.contains("/notifications")) {
                    currentMessage = MainActivity.notificationData;
                    currentUUID = MainActivity.NOTIFICATION_UUID;
                }

            } else if (characteristic.getUuid().equals(UUID.fromString(currentUUID))) {
                readyToSend = true;
            } else {
                Log.e(TAG, "Unidentified write operation on characteristic of:" + new String(characteristic.getValue(), StandardCharsets.US_ASCII));
            }


//            if (gatt.readCharacteristic(characteristic)) {
//                Log.i(TAG, "characteristic changed to: " + new String(characteristic.getValue(), StandardCharsets.US_ASCII));
//            }
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
            Log.i(TAG, "Write Complete");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);

            Log.i(TAG, "ReliableWrite Characteristic with status of result: " + status);
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
