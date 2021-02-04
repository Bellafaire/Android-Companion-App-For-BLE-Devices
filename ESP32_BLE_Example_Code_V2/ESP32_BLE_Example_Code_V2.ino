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
void setup() {
  Serial.begin(115200);
  initBLE();
}

void loop() {
  //available commands
  //  sendBLE("/notifications", &string); //gets current android notifications as a string format "appName,Title;ExtraText,ExtraInfoText,ExtraSubText,ExtraTitle;Description;"
  //  sendBLE("/calendar", &string); // returns a string of calender events for the next 24 hours in format "title;description;startDate;startTime;endTime;eventLocation;"
  //  sendBLE("/time", &string); // returns a string representing the time
  //  sendBLE("/isPlaying", &string); //returns "true" or "false" indicating whether spotify is playing on the android device
  //  sendBLE("/currentSong", &string); //returns the current song name and artist playing on spotify as one string
  //  sendBLE("/play"); //hits the media play button on the android device
  //  sendBLE("/pause"); //hits the media pause button on the android device
  //  sendBLE("/nextSong"); //hits the media next song button on the android device
  //  sendBLE("/lastSong"); //hits the media previous song button on the android device

  Serial.println(sendBLE("/time"));
  delay(1000);
}
