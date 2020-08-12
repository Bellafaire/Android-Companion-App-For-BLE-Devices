# Android Companion App For BLE Devices
Project forked off of my ESP32 Smartwatch project (https://github.com/Bellafaire/ESP32-Smart-Watch) to its own repo. 
This project is a simple Android app which uses BLE to send notification data and spotify song information to a connected BLE device. 
Also allows for other functionality such as song control with more features to come. 
This project is intended to work alongside an ESP32 based project, a short example Arduino sketch is included in the repo. 

<img src="https://github.com/Bellafaire/Android-Companion-App-For-BLE-Devices/blob/master/Images/appInterface.jpg?raw=true" height="400px" />

## Installation

Sources can be compiled in android studio once cloned from this repo. 
Compile the project to a .apk file and send to android device or upload directly from android studios. 
Be sure to grant notification permission and check "Device Broadcast Status" setting in spotify for all features to work properly in the app. 

## Usage example
Once the app is running on the android device an ESP32 can connect to it by matching the service and characteristic UUIDs in the example sketch. 
Calling initBLE() will connect the ESP32 to the BLE Gatt server and allow for commands to be issued and data to be received. 
Below is a list of commands that can be issued and their functionality. 

```c
    sendBLE("/notifications", true); //gets current android notifications as a string
    sendBLE("/isPlaying", true); //returns "true" or "false" indicating whether spotify is playing on the android device
    sendBLE("/currentSong", true); //returns the current song name and artist playing on spotify as one string
    sendBLE("/play", false); //hits the media play button on the android device
    sendBLE("/pause", false); //hits the media pause button on the android device
    sendBLE("/nextSong", false); //hits the media next song button on the android device
    sendBLE("/lastSong", false); //hits the media previous song button on the android device
```
The example sketch provided in the repo will connect to the Android app and print the current notifications present on the notification bar to the serial terminal.
If everything's configured correctly the output should look like this: 

<img src="https://github.com/Bellafaire/Android-Companion-App-For-BLE-Devices/blob/master/Images/ouputExample.png?raw=true" />
