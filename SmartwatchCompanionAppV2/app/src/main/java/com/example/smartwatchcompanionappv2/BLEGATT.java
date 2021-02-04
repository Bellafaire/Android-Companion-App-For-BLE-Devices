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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BLEGATT {
    static private boolean mConnected = false;
    private boolean readyToSend = false;
    static private String lastConnected = "";
    boolean writeInProgress = false;
    int mtuSize = 16;


    public static final String BLE_UPDATE = "com.companionApp.BLE_UPDATE";

    private static BLEUpdateReceiver nReceiver;

    public MessageClipper currentMessage = new MessageClipper("");
    public String currentUUID = MainActivity.COMMAND_UUID;

    private static String TAG = "BLEGATT";

    private static BluetoothGatt bluetoothGatt;
    private Context con;

    public BLEGATT(Context c) {
        con = c;


        //try and create a new one, if the last block failed then we will have no problems
        try {
            //init notification receiver
            nReceiver = new BLEUpdateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BLE_UPDATE);
            con.registerReceiver(nReceiver, filter);
            Log.i(TAG, "Re-registered broadcast reciever");
        } catch (IllegalArgumentException e) {
            //this is basically designed to crash so eh whatever
            Log.e(TAG, "Failed to register broadcast reciever in BLESend: " + e.getLocalizedMessage());
        }

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
        Log.i(TAG, "attempting to connect to device: " + sr.getName() + " With Address " + sr.getAddress());
        bluetoothGatt = sr.connectGatt(con, true, gattCallback);
    }

    public static boolean isConnected() {
        return mConnected;
    }

    public boolean update() {
        if(!mConnected){
            return false;
        }
        if (writeInProgress) {
            return true;
        }
        if (!currentMessage.messageComplete() && mConnected) {
            write(currentMessage.getNextMessage(), currentUUID);
            return true;
        }else if(currentMessage.messageComplete()){
            Log.i(TAG, "Reading BLE Characteristic to indicate end of transmission");
            BluetoothGattCharacteristic bgc = bluetoothGatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getCharacteristic(UUID.fromString(currentUUID));
            bluetoothGatt.readCharacteristic(bgc);
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

                bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
//                bluetoothGatt.requestMtu(512);


                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server, discovering services...");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server");
                }


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                lastConnected = getDateAndTime();

                Log.i(TAG, "Disconnected from GATT server.");
                Log.i(TAG, "Closing Gatt Server");


//                BLEScanner.startScan(MainActivity.reference);
//                con.stopService(new Intent(con, BLESend.class));
            } else {
                Log.i(TAG, "Other status change in BLE connection:" + newState);
            }

            MainActivity.updateStatusText();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Log.i(TAG, gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getUuid().toString());
            Log.i(TAG, "Obtained service");

            List<BluetoothGattCharacteristic> chars = gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getCharacteristics();

            for (int a = 0; a < chars.size(); a++) {
                if ((chars.get(a).getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    gatt.setCharacteristicNotification(chars.get(a), true);
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
                Intent i = new Intent(BLE_UPDATE);
                con.sendBroadcast(i);
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
                switch(charVal){
                    case "/notifications": {
                        currentMessage = new MessageClipper(MainActivity.notificationData, mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/calendar": {
                        currentMessage = new MessageClipper(CalendarReader.getDataFromEventTable(con), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/currentSong": {
                        currentMessage = new MessageClipper(MainActivity.sReceiver.getSongData(), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/time": {
                        currentMessage = new MessageClipper(getDateAndTime(), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/isPlaying": {
                        currentMessage = new MessageClipper(MainActivity.sReceiver.isPlaying(), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/play":
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
                        break;
                    case "/pause":
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
                        break;
                    case "/nextSong":
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
                        break;
                    case "/lastSong":
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                        break;

                    default:
                        Log.e(TAG, "Unrecognized command:" + charVal);
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

    class BLEUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Bluetooth update request received");
            update();
        }
    }

    //just presses a given key on the device, this allows external devices to control the android device
    public void pressMediaKey(int ke) {
        //referenced from https://stackoverflow.com/questions/5129027/android-application-to-pause-resume-the-music-of-another-music-player-app
        AudioManager mAudioManager = (AudioManager) MainActivity.reference.getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, ke);
        mAudioManager.dispatchMediaKeyEvent(event);
    }

    //returns a string representing the date and time
    public static String getDateAndTime() {
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        String formattedDate = df.format(c);
        return formattedDate;

    }
}
