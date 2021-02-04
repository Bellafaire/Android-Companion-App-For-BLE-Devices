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
import android.bluetooth.BluetoothDevice;
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

    //service and command UUIDs, these are what will ultimately be used by the BLE device to be advertised
    //you can change these as you see fit just make sure that the device on the other side has a matching set of UUIDs
    //otherwise the connection cannot be established.
    public static final String SERVICE_UUID = "5ac9bc5e-f8ba-48d4-8908-98b80b566e49";
    public static final String COMMAND_UUID = "bcca872f-1a3e-4491-b8ec-bfc93c5dd91a";

    //current device found from scan
    public static BluetoothDevice currentDevice;

    public static String notificationData = "";
    private static String TAG = "Main";
    public static TextView txtView;

    //receivers used to obtain data from the android device
    private NotificationReceiver nReceiver;
    public static SpotifyReceiver sReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //begin background scan, this uses the nordic semiconductor Android Scanner Compat Library
        BLEScanner.startScan(this.getApplicationContext());

        //whitelist in logcat
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

        //request some permissions (There are better ways to do this)
        checkPermission(Manifest.permission.READ_CALENDAR, 10);
        checkPermission(Manifest.permission.BLUETOOTH, 12);
        checkPermission(Manifest.permission.BLUETOOTH_ADMIN, 13);
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, 15);
        checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, 16);
        checkPermission(Manifest.permission.FOREGROUND_SERVICE, 16);


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

        updateStatusText();

    }

    // Function to check and request permission
    public static void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(
                reference,
                permission)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            reference,
                            new String[]{permission},
                            requestCode);
        } else {
            Toast.makeText(reference,
                    "Permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public static void updateNotifications() {
        notificationData = "";
        Log.e(TAG, "Updating Notifications");
        Intent i = new Intent(NLService.GET_NOTIFICATION_INTENT);
        i.putExtra("command", "list");
        reference.sendBroadcast(i);
    }

    public static void updateStatusText() {
        try {
            reference.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    String statusText = BLEGATT.getStatusText()
                            + "\nNotification Data: \n" + notificationData
                            + "\n\nSpotify:\n" + sReceiver.getStatusText()
                            + "\n\nCalendar:\n" + CalendarReader.getDataFromEventTable(reference);
                    reference.txtView.setText(statusText);
                }
            });
        } catch (Exception e) {
            String statusText = "Connecting";
            reference.txtView.setText(statusText);
        }
    }

    //this is a stupid function i need to remove, it just turns bluetooth off altogether
    public void sdt(View view) {
        BluetoothAdapter.getDefaultAdapter().disable();
    }

    public void gotoSettings(View view) {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
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