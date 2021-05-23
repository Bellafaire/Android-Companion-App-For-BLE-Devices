# Android Companion App For BLE Devices
Project forked off of my ESP32 Smartwatch project (https://github.com/Bellafaire/ESP32-Smart-Watch) to its own repo. 
This project is a simple Android app which uses BLE to send notification data and spotify song information to a connected BLE device. 
Also allows for other functionality such as song control and calendar reading. 
This project is intended to work alongside my ESP32 Smartwatch project but this could be useful for anyone working on a similar project. 

<img src="https://github.com/Bellafaire/Android-Companion-App-For-BLE-Devices/blob/master/Images/appInterface.jpg?raw=true" height="400px" />

## Installation

Sources can be compiled in android studio once cloned from this repo. 
There is a compiled .apk file available under the "releases" tab of this repo you can use that to install the app or compile the app yourself from source. 
Be sure to grant notification permission and check "Device Broadcast Status" setting in spotify for all features to work properly in the app. 

## Usage example
Once the app is running on the android device an ESP32 can connect to it by matching the service and characteristic UUIDs in the example sketch. 
Calling initBLE() will begin advertisements and allow the app to connect to the ESP32 device. 
Below is a list of commands that can be issued and their functionality. 
The initial connection can be lengthy sometimes, however once it is connected the device will automatically reconnect as long as the app is active. 

```c
      /notifications - gets current android notifications as a string format "appName,Title;ExtraText,ExtraInfoText,ExtraSubText,ExtraTitle;Description;"
      /calendar - returns a string of calender events for the next 24 hours in format "title;description;startDate;startTime;endTime;eventLocation;"
      /time - returns a string representing the time
      /isPlaying - returns "true" or "false" indicating whether spotify is playing on the android device
      /currentSong - returns the current song name and artist playing on spotify as one string
      /play - hits the media play button on the android device
      /pause - hits the media pause button on the android device
      /nextSong - hits the media next song button on the android device
      /lastSong - hits the media previous song button on the android device
```
The example sketch provided in the repo will connect to the Android app and print the current notifications present on the notification bar to the serial terminal.
If everything's configured correctly the output should look something like this for notification data: 

<img src="https://github.com/Bellafaire/Android-Companion-App-For-BLE-Devices/blob/master/Images/ouputExample.png?raw=true" />
