/*****************************************************************************
  The MIT License (MIT)
  Copyright (c) 2021 Matthew James Bellafaire
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

// function signitures
void addData(String data);
void initBLE();
void startBLEAdvertising();
boolean sendBLE(String command, String *returnString, boolean blocking);
boolean sendBLE(String command);

void onNotificationEvent(String event){
  //event will have value 'posted' or 'removed' 
  Serial.printf("Notification Event '%s'\n", event);
}


void setup()
{
  Serial.begin(115200);
  Serial.println("Initializing Bluetooth");
  initBLE();
}

void loop()
{
  // available commands
  //   /notifications - gets current android notifications as a string format "appName,Title;ExtraText,ExtraInfoText,ExtraSubText,ExtraTitle;Description;"
  //   /calendar - returns a string of calender events for the next 24 hours in format "title;description;startDate;startTime;endTime;eventLocation;"
  //   /time - returns a string representing the time
  //   /isPlaying - returns "true" or "false" indicating whether spotify is playing on the android device
  //   /currentSong - returns the current song name and artist playing on spotify as one string
  //   /play - hits the media play button on the android device
  //   /pause - hits the media pause button on the android device
  //   /nextSong - hits the media next song button on the android device
  //   /lastSong - hits the media previous song button on the android device

  String data = "";

  /* A bluetooth command can be processed with the below code.
     commands can either be blocking or non-blocking (Preventing execution until data is fully recieved)
     A string pointer is passed in to the sendBLE function, if an operation is not in progress then the function will return TRUE
     and data will be placed in the data string.
     sendBLE will return false if a command is currently in progress.  */
  boolean requestSuccess = sendBLE(
                             "/notifications", //command
                             &data,            //pointer to string
                             true              //whether the request is blocking or not.
                           );
  /* Certain commands that do not request data, such as the media control commands,
     can use the below function
      boolean sendBLE(String command)
      which will return true if the command was successful and false if not.
  */

  if (requestSuccess)
  {
    Serial.println(data);
  }
  delay(5000);
}
