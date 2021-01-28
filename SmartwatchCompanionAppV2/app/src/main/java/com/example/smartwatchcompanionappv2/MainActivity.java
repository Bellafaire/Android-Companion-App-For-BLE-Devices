package com.example.smartwatchcompanionappv2;

/* Referenced Material

I am not an android developer and as a result have limited knowledge in working with android. Here are some of the references I
Used that helped me greatly in creating this application.
NordicSemiconductor/Android-Scanner-Compat-Library: https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library
Background operation of BLE Library with Android 8 - Request for Example: https://devzone.nordicsemi.com/f/nordic-q-a/50642/background-operation-of-ble-library-with-android-8---request-for-example

 */

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    public static MainActivity reference;
    public static final String SERVICE_UUID = "5ac9bc5e-f8ba-48d4-8908-98b80b566e49";
    public static final String COMMAND_UUID = "bcca872f-1a3e-4491-b8ec-bfc93c5dd91a";
    public static final String NOTIFICATION_UUID = "921d9d4c-d833-4468-b2f0-ef1103018da7";
    public static final String TIME_UUID = "9a8eaee3-6c00-435a-b776-bbb9901e11c2";
    public static final String SPOTIFY_STATUS_UUID = "3683b4fc-7ced-4ec9-ae6c-bcb8d20e0d20";
    public static final String SPOTIFY_SONG_UUID = "8129b0cc-32bb-40a5-9fc2-a70e06f00cf1";
    public static final String CALENDAR_UUID = "cad2c1ad-4d70-413b-97da-5967a0c99b8a";

    private PendingIntent pendingIntent;
    private AlarmManager manager;

    public static String notificationData = "";

    private static String TAG = "Main";
    public BLEGATT blegatt;
    public static String[] tabText = {"First Tab", "Second Tab"};
    public static TextView txtView;

    private NotificationReceiver nReceiver;
    public static SpotifyReceiver sReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BLEScanner.startScan(this.getApplicationContext());
        blegatt = new BLEGATT(this.getApplicationContext(), (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE));

        try {
            int pid = android.os.Process.myUid();
            String whiteList = "logcat -P '" + pid + "'";
            Runtime.getRuntime().exec(whiteList).waitFor();
        } catch (Exception e) {
            Log.e(TAG, "COULD NOT WHITELIST APPLICATION IN LOGCAT");
        }
        setContentView(R.layout.activity_main);
        txtView = (TextView) findViewById(R.id.textView);
        txtView.setText("Test");
        reference = this;

        checkPermission(Manifest.permission.READ_CALENDAR, 10);
//        checkPermission(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE, 11);
        checkPermission(Manifest.permission.BLUETOOTH, 12);
        checkPermission(Manifest.permission.BLUETOOTH_ADMIN, 13);
//        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, 14);
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, 15);
//        checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, 16);
        checkPermission(Manifest.permission.FOREGROUND_SERVICE, 16);

        // Retrieve a PendingIntent that will perform a broadcast
        Intent alarmIntent = new Intent(this, BLESend.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        //init notification receiver
        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NLService.NOTIFICATION_ACTION);
        registerReceiver(nReceiver, filter);

        //get the current notifications by broadcasting an intent
        Intent i = new Intent(NLService.GET_NOTIFICATION_INTENT);
        i.putExtra("command", "list");
        sendBroadcast(i);

        //init Spotify receiver and register it's actions so it can be accessed
        sReceiver = new SpotifyReceiver();
        IntentFilter sfilter = new IntentFilter();
        sfilter.addAction("com.spotify.music.playbackstatechanged");
        sfilter.addAction("com.spotify.music.metadatachanged");
        sfilter.addAction("com.spotify.music.queuechanged");
        registerReceiver(sReceiver, sfilter);

        startAlarm();
    }

    public void startAlarm() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 100;

        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }

    // Function to check and request permission
    public void checkPermission(String permission, int requestCode) {

        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(
                MainActivity.this,
                permission)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            MainActivity.this,
                            new String[]{permission},
                            requestCode);
        } else {
            Toast.makeText(MainActivity.this,
                    "Permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public static void updateStatusText() {
        reference.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String statusText = BLEGATT.getStatusText()
                        + "\nNotification Data: \n" + notificationData
                        + "\n\nSpotify:\n" + sReceiver.getStatusText()
                        + "\n\nCalendar:\n" + CalendarReader.getDataFromEventTable();
                reference.txtView.setText(statusText);
            }
        });

    }

    public void sendBLE(View view) {
        reference.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean s = blegatt.write(blegatt.currentMessage, blegatt.currentUUID);
                if (!s) {
                    Log.e(TAG, "FAILED TO WRITE TO CHARACTERISTIC");
                }
            }
        });
    }

    public void sdt(View view) {
        Log.d(TAG, "sending notification data");
        blegatt.write(notificationData, blegatt.currentUUID);
    }

    void sendDateAndTime() {
        Log.v(TAG, "SENDING DATE AND TIME");
        blegatt.write(BLEGATT.getDateAndTime(), TIME_UUID);
    }

    //sends intent to obtain notification data and updates the textview
    //that the user sees along with the output data field. this version of the
    //function CANNOT be called from a static context
    public void updateText(View view) {
        Log.v(TAG, "Updating Notification Text");
        notificationData = BLEGATT.getDateAndTime() + "\n***";
        Intent i = new Intent(NLService.GET_NOTIFICATION_INTENT);
        i.putExtra("command", "list");
        sendBroadcast(i);
    }


    //receives the data from the NLService and updates fields in this class.
    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive method callback received " + intent.getStringExtra("notification_event"));
            String temp = intent.getStringExtra("notification_event");
            if (!notificationData.contains(temp)) {
                temp = intent.getStringExtra("notification_event") + "\n" + notificationData;
                notificationData = temp.replace("\n\n", "\n");
            }
            updateStatusText();
        }
    }


}