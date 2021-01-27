package com.example.smartwatchcompanionappv2;

/* Referenced Material

I am not an android developer and as a result have limited knowledge in working with android. Here are some of the references I
Used that helped me greatly in creating this application.
NordicSemiconductor/Android-Scanner-Compat-Library: https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library
Background operation of BLE Library with Android 8 - Request for Example: https://devzone.nordicsemi.com/f/nordic-q-a/50642/background-operation-of-ble-library-with-android-8---request-for-example

 */

import android.Manifest;
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
    public static String serviceUUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    public static String charUUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    public static String notificationData = "";

    private static String TAG = "Main";
    public static BLEGATT blegatt;
    public static String[] tabText = {"First Tab", "Second Tab"};
    public static TextView txtView;

    private NotificationReceiver nReceiver;
    public static SpotifyReceiver sReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView = (TextView) findViewById(R.id.textView);
        txtView.setText("Test");
        reference = this;

        checkPermission(Manifest.permission.READ_CALENDAR, 1);
        checkPermission(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE, 2);
        checkPermission(Manifest.permission.BLUETOOTH, 3);
        checkPermission(Manifest.permission.BLUETOOTH_ADMIN, 4);
        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, 5);


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

        BLEScanner.startScan(this.getApplicationContext());
        blegatt = new BLEGATT(this.getApplicationContext(), (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE));
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
//            Toast.makeText(MainActivity.this,
//                    "Permission already granted",
//                    Toast.LENGTH_SHORT)000000
//                    .show();
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
                ;
                reference.txtView.setText(statusText);
            }
        });

    }

    public void sdt(View view) {
        sendDateAndTime();
    }

    void sendDateAndTime() {
        Log.d(TAG, "SENDING DATE AND TIME");
        blegatt.write(BLEGATT.getDateAndTime());
    }

    //sends intent to obtain notification data and updates the textview
    //that the user sees along with the output data field. this version of the
    //function CANNOT be called from a static context
    public void updateText(View view) {
        Log.d(TAG, "Updating Notification Text");
        notificationData = BLEGATT.getDateAndTime() + "\n***";
        Intent i = new Intent(NLService.GET_NOTIFICATION_INTENT);
        i.putExtra("command", "list");
        sendBroadcast(i);
    }


    //receives the data from the NLService and updates fields in this class.
    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive method callback received " + intent.getStringExtra("notification_event"));
            String temp = intent.getStringExtra("notification_event");
            if (!notificationData.contains(temp)) {
                temp = intent.getStringExtra("notification_event") + "\n" + notificationData;
                notificationData = temp.replace("\n\n", "\n");
            }
            updateStatusText();
        }
    }

    //gets the calender information we want in a string format
    //data format is "title;description;startDate;startTime;endTime;eventLocation;"
    public String getDataFromEventTable() {
        Log.d("calendar", "Obtaining calendar events");
        String ret = "";


        String[] INSTANCE_PROJECTION = new String[]{
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.END,
                CalendarContract.Instances.DTSTART,
                CalendarContract.Instances.DTEND,
                CalendarContract.Instances.EVENT_LOCATION
        };

        // Specify the date range you want to search for recurring
        // event instances


        //specifying date range here, we want to obtain all the events for the day, for that we use
        //the gregorian calendar, and set it to our timezone then give it the system time
        GregorianCalendar time = new GregorianCalendar();
        time.setTimeZone(TimeZone.getDefault());
        time.setTimeInMillis(System.currentTimeMillis());

        //now we want to set the start time range to the first second of the day
        time.clear(GregorianCalendar.MINUTE);
        time.clear(GregorianCalendar.HOUR_OF_DAY);
        time.set(GregorianCalendar.MINUTE, 0);
        time.set(GregorianCalendar.HOUR_OF_DAY, 0);
        long startMillis = time.getTimeInMillis();  //get the time in milliseconds since epoch

        //set the end time range to the last second of the day
        time.clear(GregorianCalendar.MINUTE);
        time.clear(GregorianCalendar.HOUR_OF_DAY);
        time.set(GregorianCalendar.MINUTE, 59);
        time.set(GregorianCalendar.HOUR_OF_DAY, 23);
        long endMillis = time.getTimeInMillis();   //get the time in milliseconds since epoch

        Log.d("calendar", "Looking for events from " + startMillis + " to " + endMillis);

        //create variables we'll need to query the calendar data
        Cursor cur = null;
        ContentResolver cr = getContentResolver();
        String selection = "";
        String[] selectionArgs = new String[]{};

        // Construct the query with the desired date range.
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        //query data and sort based on start time in descending order
        cur = cr.query(builder.build(),
                INSTANCE_PROJECTION,
                selection,
                selectionArgs,
                CalendarContract.Instances.BEGIN + " ASC"); //sort in ascending order (way easier to do on the phone end of things than the device)

        Log.d("calendar", "Found " + cur.getCount() + " Instances");

        if (cur.moveToFirst()) {
            do {

                //parse data out of the query that we want
                String title = cur.getString(cur.getColumnIndex(CalendarContract.Instances.TITLE)).replace("\n", " ");
                String description = cur.getString(cur.getColumnIndex(CalendarContract.Instances.DESCRIPTION)).replace("\n", " ");
                String end = cur.getString(cur.getColumnIndex(CalendarContract.Instances.END)).replace("\n", " ");
                String dtStart = cur.getString(cur.getColumnIndex(CalendarContract.Instances.DTSTART)).replace("\n", " ");
                String location = cur.getString(cur.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)).replace("\n", " ");

                //format date and time
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mma");

                Long startTimeStringSeconds, endTimeStringSeconds;
                String startTimeString, endTimeString;

                //convert calendar time from milliseconds since epoch to human readable time
                try {
                    startTimeStringSeconds = Long.parseLong(dtStart) / 1000;

                    startTimeString = Instant.ofEpochSecond(startTimeStringSeconds)
                            .atZone(ZoneId.of(TimeZone.getDefault().getID()))
                            .format(formatter);
                } catch (NumberFormatException e) {
                    startTimeString = "";
                    Log.d("calendar", "Could not parse start time due to error in event \"" + title + "\": " + e.getMessage());
                }
                try {
                    endTimeStringSeconds = Long.parseLong(end) / 1000;

                    endTimeString = Instant.ofEpochSecond(endTimeStringSeconds)
                            .atZone(ZoneId.of(TimeZone.getDefault().getID()))
                            .format(formatter);
                } catch (NumberFormatException e) {
                    endTimeString = "";
                    Log.d("calendar", "Could not parse end time due to error in event \"" + title + "\": " + e.getMessage());
                }

                //format for sending over BLE
                ret += title + ";" + description + ";" + /*Start date removed for the time being*/ ";" + startTimeString + ";" + endTimeString + ";" + location + ";\n";
                Log.d("calendar", "Found Event: " + title + ";" + description + ";" + ";" + startTimeString + ";" + endTimeString + ";" + location + ";\n");
            } while (cur.moveToNext());
        }

        return ret;
    }


}