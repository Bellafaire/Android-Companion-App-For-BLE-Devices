package com.example.smartwatchcompanionappv2;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class CalendarReader {

    //gets the calender information we want in a string format
    //data format is "title;description;startDate;startTime;endTime;eventLocation;"
    public static String getDataFromEventTable(Context context) {
        Log.v("calendar", "Obtaining calendar events");
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

        Log.v("calendar", "Looking for events from " + startMillis + " to " + endMillis);

        //create variables we'll need to query the calendar data
        Cursor cur = null;
        ContentResolver cr = context.getContentResolver();
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

        Log.v("calendar", "Found " + cur.getCount() + " Instances");

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
                    Log.v("calendar", "Could not parse start time due to error in event \"" + title + "\": " + e.getMessage());
                }
                try {
                    endTimeStringSeconds = Long.parseLong(end) / 1000;

                    endTimeString = Instant.ofEpochSecond(endTimeStringSeconds)
                            .atZone(ZoneId.of(TimeZone.getDefault().getID()))
                            .format(formatter);
                } catch (NumberFormatException e) {
                    endTimeString = "";
                    Log.v("calendar", "Could not parse end time due to error in event \"" + title + "\": " + e.getMessage());
                }

                //format for sending over BLE
                ret += title + ";" + description + ";" + /*Start date removed for the time being*/ ";" + startTimeString + ";" + endTimeString + ";" + location + ";\n";
                Log.v("calendar", "Found Event: " + title + ";" + description + ";" + ";" + startTimeString + ";" + endTimeString + ";" + location + ";\n");
            } while (cur.moveToNext());
        }

        return ret;
    }

}
