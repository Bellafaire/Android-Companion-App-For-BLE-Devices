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
    boolean writeInProgress = false;
    int mtuSize = 16;

    public static MessageClipper currentMessage = new MessageClipper("");
    public static String currentUUID = MainActivity.COMMAND_UUID;

    private static String TAG = "BLEGATT";

    private BluetoothGatt bluetoothGatt;
    private Context con;

    public BLEGATT(Context c) {
        con = c;
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
        bluetoothGatt = sr.connectGatt(con, true, gattCallback);

        Log.i(TAG, "Connected to BLEGatt Server");
    }

    public static boolean isConnected() {
        return mConnected;
    }

    public boolean update() {
        if (writeInProgress) {
            return true;
        }
        if (!currentMessage.messageComplete()) {
            write(currentMessage.getNextMessage(), currentUUID);
            return true;
        }

        return false;
    }

    public boolean write(String str, String uuid) {
        writeInProgress = true;
        BluetoothGattCharacteristic bgc = bluetoothGatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getCharacteristic(UUID.fromString(uuid));
        if (bgc != null) {
            bgc.setValue(str);
            if (bluetoothGatt.writeCharacteristic(bgc)) {
                Log.d(TAG, "transmitted:" + str);
            } else {
                Log.e(TAG, "Failed to transmit data");
            }
        } else {
            Log.e(TAG, "Characteristic is null");
        }
        return false;
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


                bluetoothGatt.requestMtu(512);
//                        broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        bluetoothGatt.discoverServices());


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                lastConnected = getDateAndTime();

                Log.i(TAG, "Disconnected from GATT server.");
                con.stopService(new Intent(con, BLESend.class));
                bluetoothGatt = null;
            }

            MainActivity.updateStatusText();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);


            Log.d(TAG, bluetoothGatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getUuid().toString());
            Log.i(TAG, "Obtained service");

            List<BluetoothGattCharacteristic> chars = gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getCharacteristics();

            for (int a = 0; a < chars.size(); a++) {
                if ((chars.get(a).getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    bluetoothGatt.setCharacteristicNotification(chars.get(a), true);
                    Log.i(TAG, "Subscribed to characteristic: " + new String(chars.get(a).getUuid().toString()));
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

            writeInProgress = false;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "BLE Write success");
            } else {
                Log.e(TAG, "BLE Write failed");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic.getUuid().equals(UUID.fromString(MainActivity.COMMAND_UUID))) {
                String charVal = new String(characteristic.getValue(), StandardCharsets.US_ASCII);
                Log.i(TAG, "Command characteristic changed to:" + charVal);
                if (charVal.equals("/notifications")) {
                    currentMessage = new MessageClipper(MainActivity.notificationData, mtuSize);
                    currentUUID = MainActivity.COMMAND_UUID;
                    Intent i = new Intent(BLESend.BLE_UPDATE);
                    con.sendBroadcast(i);
                }else if(charVal.equals("/calendar")){
                    currentMessage = new MessageClipper(CalendarReader.getDataFromEventTable(), mtuSize);
                    currentUUID = MainActivity.COMMAND_UUID;
                    Intent i = new Intent(BLESend.BLE_UPDATE);
                    con.sendBroadcast(i);
                }

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
            Log.i(TAG, "Write Complete");
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
            Log.d(TAG, "MTU changed to: " + mtu);
            mtuSize = mtu;
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
