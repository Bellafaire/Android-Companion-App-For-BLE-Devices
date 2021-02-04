#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

//variables and defines used by BLEServer.ino
String* currentDataField;
#define SERVICE_UUID        "5ac9bc5e-f8ba-48d4-8908-98b80b566e49"
#define COMMAND_UUID        "bcca872f-1a3e-4491-b8ec-bfc93c5dd91a"
BLECharacteristic *commandCharacteristic;

//indicates connection state to the android device
boolean connected = false;

//indiciates whether or not a operation is currently in progress
boolean operationInProgress = false;

//function signitures
boolean sendBLE(String command, String* returnString); //sends command to the android device and puts returned data into the string indicated by a given pointer (returns true if command was issued successfully)
boolean sendBLE(String command); //sends command to the android device without return data (returns true if command was issued successfully)
void addData(String data);  //adds data to a current string, used within BLEServer.ino
void initBLE(); //initializes the BLE connection by starting advertising.


//strings declared for use in this example
String notificationData = "";
String timeData = "";
String spotifyStatus = "";
String spotifySong = "";
String calendarData = "";


void setup() {
  Serial.begin(115200);
  initBLE();
}

void loop() {
  //available commands
  //  sendBLE("/notifications", &string); //gets current android notifications as a string format "appName,Title;ExtraText,ExtraInfoText,ExtraSubText,ExtraTitle;Description;"
  //  sendBLE("/calendar", &string); // returns a string of calender events for the next 24 hours in format "title;description;startDate;startTime;endTime;eventLocation;"
  //  sendBLE("/isPlaying", &string); //returns "true" or "false" indicating whether spotify is playing on the android device
  //  sendBLE("/currentSong", &string); //returns the current song name and artist playing on spotify as one string
  //  sendBLE("/play"); //hits the media play button on the android device
  //  sendBLE("/pause"); //hits the media pause button on the android device
  //  sendBLE("/nextSong"); //hits the media next song button on the android device
  //  sendBLE("/lastSong"); //hits the media previous song button on the android device

  if (connected && !operationInProgress) {
    //commands submitted will take some time to be completed. In that time the data recieved from the android device is placed into the
    // string indicated by the pointer, other operations are allowed to happen during this time
    sendBLE("/calendar", &notificationData);
  }
  delay(1000);
  Serial.println(notificationData);
}
