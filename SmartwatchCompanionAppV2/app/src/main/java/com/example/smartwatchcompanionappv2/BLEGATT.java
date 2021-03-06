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

    //status variables
    static private boolean mConnected = false;
    private boolean readyToSend = false;
    static private String lastConnected = "";
    boolean writeInProgress = false;
    int mtuSize = 16;
    public String currentUUID = MainActivity.COMMAND_UUID;

    //constants
    public static final String BLE_UPDATE = "com.companionApp.BLE_UPDATE";
    private static String TAG = "BLEGATT";

    //receiver
    private static BLEUpdateReceiver nReceiver;

    //current message
    public MessageClipper currentMessage = new MessageClipper("");

    //reference and context
    private static BluetoothGatt bluetoothGatt;
    private Context con;

    //constructor
    public BLEGATT(Context c) {
        con = c;

        //attach the receiver that's used to update the remote BLE device
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

    //returns string showing the status of the gatt server
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

    //connects to a bluetooth device and establishes a gatt server
    public void connect(BluetoothDevice sr) {
        Log.i(TAG, "attempting to connect to device: " + sr.getName() + " With Address " + sr.getAddress());
        bluetoothGatt = sr.connectGatt(con, true, gattCallback);
    }

    //updates the ble remote device with the current message.
    //this is called from the broadcast receiver which is called by the broadcast receiver
    //so that it can be triggered from within the gatt callbacks without binding the thread
    //this function is designed to be called as the condition of a while loop, although its not used
    //in that way any more
    public boolean update() {
        //if we're not connected return false
        if (!mConnected) {
            return false;
        }

        //return true if a write is currently in progress
        if (writeInProgress) {
            return true;
        }

        //if we have some message data to send and we're connected then send the data
        if (!currentMessage.messageComplete() && mConnected) {
            write(currentMessage.getNextMessage(), currentUUID);
            return true;

            //if the message is complete then read the BLE characteristic (this indicates that the message transmission has been completed)
        } else if (currentMessage.messageComplete()) {
            Log.i(TAG, "Reading BLE Characteristic to indicate end of transmission");
            BluetoothGattCharacteristic bgc = bluetoothGatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getCharacteristic(UUID.fromString(currentUUID));
            bluetoothGatt.readCharacteristic(bgc);
        }

        return false;
    }

    //writes a given string to a given UUID
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


    /* Bluetooth gatt callbacks, functionally everything important happens here

     */
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

            //device is connected
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                //set connection state
                mConnected = true;
                lastConnected = getDateAndTime();

                //request higher connection priority (increases service discovery speed)
                bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

                //discover services that the device has available
                gatt.discoverServices();

                gatt.requestMtu(256);

                //indicate we can write again
                writeInProgress = false;

                //device is disconnected
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //update status variables
                mConnected = false;
                lastConnected = getDateAndTime();

                //indicate we can write again
                writeInProgress = false;

                Log.i(TAG, "Disconnected from GATT server.");

            } else {
                //this isn't actually possible but whatever
                Log.i(TAG, "Other status change in BLE connection:" + newState);
            }

            //update the status text on the home screen of the app.
//            MainActivity.updateNotifications();
            MainActivity.updateStatusText();
        }

        @Override
        /* this callback is called when services are discovered, when we originally connect to the BLE device the android
        phone knows nothing about what characteristics or services it has, this will only be called after discoverServices() has been
        called and the services have been discovered
         */
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            //log spam
            Log.i(TAG, gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID)).getUuid().toString());
            Log.i(TAG, "Obtained service");

            //if any of the characteristics available are subscribeable then subscribe to them
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
        //called after write operation is complete (will indicate whether failed or not)
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            //indicate we can write again
            writeInProgress = false;

            //if success then try to send the next bit of data by sending a broadcast
            //to the receiver and triggering an update.
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "BLE Write success");
                Intent i = new Intent(BLE_UPDATE);
                con.sendBroadcast(i);
            } else {
                //print scary warning message if something goes wrong
                Log.e(TAG, "BLE Write failed");
            }
        }

        @Override
        /*When the BLE device wants data from the android device it will change the value of its own characteristic
        then notify the device, we use that notification to determine our next action

        all the data transmission to the device basically happens here, we load up a MessageClipper object and
        send a broadcast, the class will take care of the operation from there on out.
         */
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic.getUuid().equals(UUID.fromString(MainActivity.COMMAND_UUID))) {
                String charVal = new String(characteristic.getValue(), StandardCharsets.US_ASCII);
                Log.i(TAG, "Command characteristic changed to:" + charVal);
                switch (charVal) {
                    case "/notifications": {
                        //load message clipper with data, taking into account of MTU data
                        if(MainActivity.notificationData.length() <2) {
                            currentMessage = new MessageClipper("   ", mtuSize);
                            currentUUID = MainActivity.COMMAND_UUID;
                        }else{
                            Log.v(TAG, "Sending Notification data: \n" + MainActivity.notificationData);
                            currentMessage = new MessageClipper(MainActivity.notificationData, mtuSize);
                            currentUUID = MainActivity.COMMAND_UUID;
                        }
                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/calendar": {
                        //load message clipper with data, taking into account of MTU data
                        currentMessage = new MessageClipper(CalendarReader.getDataFromEventTable(con), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;

                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/currentSong": {
                        //load message clipper with data, taking into account of MTU data
                        currentMessage = new MessageClipper(MainActivity.sReceiver.getSongData(), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;

                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/time": {
                        //load message clipper with data, taking into account of MTU data
                        currentMessage = new MessageClipper(getDateAndTime(), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;

                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/isPlaying": {
                        //load message clipper with data, taking into account of MTU data
                        currentMessage = new MessageClipper(MainActivity.sReceiver.isPlaying(), mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;

                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);
                        break;
                    }
                    case "/play": {
                        //send keycode for play
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);


                        //load message clipper with data, taking into account of MTU data
                        //we still want to send a blank message when we're issuing a command so that
                        //the ESP32 knows the command has been completed
                        currentMessage = new MessageClipper("", mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);

                        break;
                    }
                    case "/pause": {
                        //send keycode for play
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);

                        //load message clipper with data, taking into account of MTU data
                        //we still want to send a blank message when we're issuing a command so that
                        //the ESP32 knows the command has been completed
                        currentMessage = new MessageClipper("", mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);

                        break;
                    }
                    case "/nextSong": {
                        //send keycode for play
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);

                        //load message clipper with data, taking into account of MTU data
                        //we still want to send a blank message when we're issuing a command so that
                        //the ESP32 knows the command has been completed
                        currentMessage = new MessageClipper("", mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);

                        break;
                    }
                    case "/lastSong": {
                        //send keycode for play
                        pressMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);

                        //load message clipper with data, taking into account of MTU data
                        //we still want to send a blank message when we're issuing a command so that
                        //the ESP32 knows the command has been completed
                        currentMessage = new MessageClipper("", mtuSize);
                        currentUUID = MainActivity.COMMAND_UUID;
                        //send broadcast to begin process
                        Intent i = new Intent(BLE_UPDATE);
                        con.sendBroadcast(i);

                        break;
                    }
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
        //if a higher MTU is requested we want to update our message clipper size so that we
        //can efficently use the newer size to send data more efficiently.
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "MTU changed to: " + mtu);
            mtuSize = mtu;
        }
    };

    /*
    Broadcast receiver used to update the remote device, functionally everything calls here.
    We use a broadcast receiver here to prevent binding up the thread responsible for the BLE
    callbacks (I think? this was the only way I could get this thing to work properly)     */
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
