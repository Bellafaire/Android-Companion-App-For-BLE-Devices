package com.example.smartwatchcompanionappv2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

//https://www.sitepoint.com/scheduling-background-tasks-android/

public class BLESend extends BroadcastReceiver {
    private PendingIntent pendingIntent;
    private AlarmManager manager;
    String TAG = "Alarm";

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        Log.d(TAG, "Alarm has run");

        Toast.makeText(arg0, "I'm running", Toast.LENGTH_SHORT).show();

    }

}