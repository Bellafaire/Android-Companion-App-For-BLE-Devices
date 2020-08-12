//uses nkolban's ESP32 BLE library 
#include "BLEDevice.h"

//BLE related variables
//UUID's for the services used by the android app (change as you please if you're building this yourself, just match them in the android app)
static BLEUUID serviceUUID("d3bde760-c538-11ea-8b6e-0800200c9a66");
static BLEUUID    charUUID("d3bde760-c538-11ea-8b6e-0800200c9a67");

//important variables used to establish BLE communication
static BLERemoteCharacteristic* pRemoteCharacteristic;
static BLEAdvertisedDevice* myDevice;
static BLEClient*  pClient;
TaskHandle_t xConnect = NULL;
static boolean connected = false;
static boolean registeredForCallback = false;

//function signatures for functions in BluetoothReceive.ino
String sendBLE(String command, bool hasReturnData);
void initBLE();
void xFindDevice(void * pvParameters ) ;
void formConnection(void * pvParameters) ;


void setup() {
//  start serial communication
  Serial.begin(115200);

  //initalizes BLE connection in seperate thread
  //when connected will update the "connected" variable to true
  initBLE();
}

void loop() {
  //if we're connected then obtain the current notification data and print it to the serial terminal
  //commands are sent over the sendBLE function with the first parameter being the command and the second
  //being a boolean to indicate whether we want to read any resulting data from that command
  //some commands like media control do not require us to read any data back
  if (connected) {
    //available commands
    //    sendBLE("/notifications", true); //gets current android notifications as a string
    //    sendBLE("/isPlaying", true); //returns "true" or "false" indicating whether spotify is playing on the android device
    //    sendBLE("/currentSong", true); //returns the current song name and artist playing on spotify as one string
    //    sendBLE("/play", false); //hits the media play button on the android device
    //    sendBLE("/pause", false); //hits the media pause button on the android device
    //    sendBLE("/nextSong", false); //hits the media next song button on the android device
    //    sendBLE("/lastSong", false); //hits the media previous song button on the android device

    String notificationData = sendBLE("/notifications", true);
    Serial.println(notificationData);
  }
  delay(1000);
}
