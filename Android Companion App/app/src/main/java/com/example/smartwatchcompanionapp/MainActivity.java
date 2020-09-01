package com.example.smartwatchcompanionapp;

/*****************************************************************************
 The MIT License (MIT)

 Copyright (c) 2020 Matthew James Bellafaire

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 ******************************************************************************/

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends Activity {

    public static boolean bleServiceRunning = false;
    public static String outData = "";
    public static String logData = "Messages\n";

    public static TextView txtView;
    public static TextView status;
    private Button settings, toggleBT;
    private Button btn;
    private NotificationReceiver nReceiver;
    public static SpotifyReceiver sReceiver;

    public static TextView messages;
    public static MainActivity reference;

    public ArrayList<String> notificationHeaders, notificationText;

    String TAG = "inform";

    public static long dataAcquisitionStart = System.currentTimeMillis();


    /* onCreate
        everything that's important on startup happens here
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //save a reference of this instance, this allows for static
        //objects to access this instance
        reference = this;
        notificationHeaders = new ArrayList();
        notificationText = new ArrayList();

        //init UI
        Log.i(TAG, "application starting...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.statusInformationBar);
        status.setMovementMethod(new ScrollingMovementMethod());
        txtView = (TextView) findViewById(R.id.txt);
        btn = (Button) findViewById(R.id.button);
        settings = (Button) findViewById(R.id.settings);
        toggleBT = (Button) findViewById(R.id.sendButton);
        messages = (TextView) findViewById(R.id.messages);
        messages.append("\n");

        checkPermission(Manifest.permission.READ_CALENDAR);
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        //init Spotify receiver and register it's actions so it can be accessed
        sReceiver = new SpotifyReceiver();
        IntentFilter sfilter = new IntentFilter();
        sfilter.addAction("com.spotify.music.playbackstatechanged");
        sfilter.addAction("com.spotify.music.metadatachanged");
        sfilter.addAction("com.spotify.music.queuechanged");
        registerReceiver(sReceiver, sfilter);

        //init notification receiver
        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NLService.NOTIFICATION_ACTION);
        registerReceiver(nReceiver, filter);

        //get the current notifications by broadcasting an intent
        Intent i = new Intent(NLService.GET_NOTIFICATION_INTENT);
        i.putExtra("command", "list");
        sendBroadcast(i);

        //start BLE server as a service
        startService(new Intent(MainActivity.this, BLEServer.class));
    }


   public void checkPermission(String permission){
       if (ContextCompat.checkSelfPermission(MainActivity.this,
               permission) != PackageManager.PERMISSION_GRANTED){
           if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                   permission)){
               ActivityCompat.requestPermissions(MainActivity.this,
                       new String[]{permission}, 1);
           }else{
               ActivityCompat.requestPermissions(MainActivity.this,
                       new String[]{permission}, 1);
           }
       }

   }


    //make sure that the button on the UI reflects the current state of the BLE server
    public static void updateBLEStatus() {
        reference.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bleServiceRunning) {
                    reference.toggleBT.setText("Disable BLE Service");
                } else {
                    reference.toggleBT.setText("Enable BLE Service");
                }
            }
        });
    }

    //enable/disable the bleService
    public void toggleService(View view) {
        if (bleServiceRunning) {
            stopService(new Intent(MainActivity.this, BLEServer.class));
        } else {
            startService(new Intent(MainActivity.this, BLEServer.class));
        }
    }


    //opens the "grant notification permission" settings window
    public void gotoSettings(View view) {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
    }

    //sends intent to obtain notification data and updates the textview
    //that the user sees along with the output data field. this version of the
    //function can be called from a static context
    public static void updateText(Context context) {
        Log.d("inform", "update text function has been called");
        reference.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.dataAcquisitionStart = System.currentTimeMillis();
                outData = getDateAndTime() + "\n***";
                txtView.setText(getDateAndTime() + "\n***");
                Intent i = new Intent(NLService.GET_NOTIFICATION_INTENT);
                i.putExtra("command", "list");
                context.sendBroadcast(i);
                logData += "BLE Data Sent - " + getDateAndTime() + "\n";
                messages.setText(logData);
            }
        });
    }

    //sends intent to obtain notification data and updates the textview
    //that the user sees along with the output data field. this version of the
    //function CANNOT be called from a static context
    public void updateText(View view) {
        Log.i(TAG, "update text button has been pressed");
        MainActivity.dataAcquisitionStart = System.currentTimeMillis();
        logData += "Manually updated: " + getDateAndTime() + "\n";
        messages.setText(logData);
        outData = getDateAndTime() + "\n***";
        txtView.setText(getDateAndTime() + "\n***");
        Intent i = new Intent(NLService.GET_NOTIFICATION_INTENT);
        i.putExtra("command", "list");
        sendBroadcast(i);
    }

    //sets text on the UI, can be called from a static context
    public static void setUIText(Context context, String data) {
        Log.d("inform", "update text function has been called");
        reference.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                txtView.setText(data + "***");
                messages.setText(logData);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        messages.setText(logData);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nReceiver);
    }


    //returns a string representing the date and time
    public static String getDateAndTime() {
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        String formattedDate = df.format(c);
        return formattedDate;

    }

    //some data needs time to be acquired, we don't want the external device to be sent incomplete data
    // in these cases
    public static boolean dataIsReady() {
        return dataAcquisitionStart + 100 < System.currentTimeMillis();
    }

    //receives the data from the NLService and updates fields in this class.
    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onRecieve method callback received " + intent.getStringExtra("notification_event"));
            String temp = intent.getStringExtra("notification_event");
            if (!outData.contains(temp)) {
                temp = intent.getStringExtra("notification_event") + "\n" + outData;
                outData = temp.replace("\n\n", "\n");
            }
            if (!txtView.getText().toString().contains(temp)) {
                temp = intent.getStringExtra("notification_event") + "\n" + txtView.getText();
                txtView.setText(temp.replace("\n\n", "\n"));
            }
        }
    }


}
