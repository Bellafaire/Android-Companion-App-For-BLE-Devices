package com.example.smartwatchcompanionappv2;

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* Taken from the spotify developer api example
https://developer.spotify.com/documentation/android/guides/android-media-notifications/
 */


public class SpotifyReceiver extends BroadcastReceiver {
    static final class BroadcastTypes {
        static final String SPOTIFY_PACKAGE = "com.spotify.music";
        static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
        static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
        static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
    }

    //variables that will be updated when there is a detected change in spotify
    private String songData = "";
    public Boolean isPlaying = false;

    public String getStatusText(){
      return "currently playing: " + isPlaying + "\nCurrent Song: " + songData;
    };

    //return the song data as a string
    public String getSongData() {
        return songData;
    }

    //return play status as a string (easier to send over BLE and debug on the other end to just use a string)
    public String isPlaying() {
        if (isPlaying) {
            return "true";
        } else {
            return "false";
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This is sent with all broadcasts, regardless of type. The value is taken from
        // System.currentTimeMillis(), which you can compare to in order to determine how
        // old the event is.
        long timeSentInMs = intent.getLongExtra("timeSent", 0L);

        String action = intent.getAction();

        //when there's a change in the spotify song get it's data
        //there's a lot of stuff here that's not used in the final string
        // feel free to include or leave out whatever suits you
        if (action.equals(BroadcastTypes.METADATA_CHANGED)) {
            String trackId = intent.getStringExtra("id");
            String artistName = intent.getStringExtra("artist");
            String albumName = intent.getStringExtra("album");
            String trackName = intent.getStringExtra("track");
            int trackLengthInSec = intent.getIntExtra("length", 0);

            //if we are currently playing a song then we need to update the song name
            if (isPlaying) {
                songData = trackName + "-" + artistName;
                songData = songData.replaceAll("[^\\p{ASCII}]", ""); //remove all non-ascii characters, they don't play well with UTF-8 encoding
            }
            Log.v("spotify", "Meta data changed, current songData: " + songData);
        } else if (action.equals(BroadcastTypes.PLAYBACK_STATE_CHANGED)) {
            isPlaying = intent.getBooleanExtra("playing", false);
            // Do something with extracted information
        } else if (action.equals(BroadcastTypes.QUEUE_CHANGED)) {
            // Sent only as a notification, your app may want to respond accordingly.
        }
    }
}