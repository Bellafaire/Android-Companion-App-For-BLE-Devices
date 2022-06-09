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

// library imports
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

/*******************************************************************
                              DEBUG
 *******************************************************************/
// prints debug information to the serial terminal when declared
// #define DEBUG

#ifdef DEBUG
#define printDebug(a) Serial.println(a)
#endif
#ifndef DEBUG
#define printDebug(a)
#endif

/********************************************************************
                           BLE Globals
 ********************************************************************/
// variables and defines used by BLEServer.ino
static String currentDataField;
static boolean blockingCommandInProgress = false;
static String *bleReturnString;
#define SERVICE_UUID "5ac9bc5e-f8ba-48d4-8908-98b80b566e49"
#define COMMAND_UUID "bcca872f-1a3e-4491-b8ec-bfc93c5dd91a"
#define CHARACTERISTIC_NOTIFICATION_UPDATE "bcca872f-1a3e-4491-b8ec-bfc93c5dd901"

BLECharacteristic *commandCharacteristic;
BLECharacteristic *notificationUpdateCharacteristic;
BLEService *pService;
BLEService *updateService;
BLEServer *pServer;
TaskHandle_t xBLE = NULL;

// indicates connection state to the android device
static boolean connected = false;

// indiciates whether or not a operation is currently in progress
static boolean operationInProgress = false;

void addData(String data)
{
  printDebug("Received:" + data);
  currentDataField += data;
  if (!blockingCommandInProgress)
  {
    *bleReturnString = currentDataField;
  }
}

class cb : public BLEServerCallbacks
{
  void onConnect(BLEServer *pServer)
  {
    connected = true;
    printDebug("BLE Device Connected");
  }
  void onDisconnect(BLEServer *pServer)
  {
    connected = false;
    printDebug("BLE Device Disconnected");
  }
};

class ccb : public BLECharacteristicCallbacks
{
  void onWrite(BLECharacteristic *pCharacteristic)
  {
    std::string rxValue = pCharacteristic->getValue();
    addData(String(pCharacteristic->getValue().c_str()));
  }
  void onRead(BLECharacteristic *pCharacteristic)
  {
    //      Serial.println("Characteristic Read");
    operationInProgress = false;
    printDebug("Complete Received String:\n" + currentDataField);
  }
};

class notification_update_callback : public BLECharacteristicCallbacks
{
  void onWrite(BLECharacteristic *pCharacteristic)
  {
    printDebug("Notification " + String(pCharacteristic->getValue().c_str()));
    onNotificationEvent(String(pCharacteristic->getValue().c_str()));
  }
};

void initBLE()
{
  BLEDevice::init("ESP32 Smartwatch");
  pServer = BLEDevice::createServer();
  pService = pServer->createService(SERVICE_UUID);

  // define the characteristics and how they can be used
  commandCharacteristic = pService->createCharacteristic(
      COMMAND_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY);

  notificationUpdateCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_NOTIFICATION_UPDATE,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY);

  // set all the callbacks
  commandCharacteristic->setCallbacks(new ccb());
  commandCharacteristic->setValue("");

  notificationUpdateCharacteristic->setCallbacks(new notification_update_callback());
  notificationUpdateCharacteristic->setValue("");

  // add server callback so we can detect when we're connected.
  pServer->setCallbacks(new cb());

  pService->start();
  startBLEAdvertising();
}

void startBLEAdvertising()
{
  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
}

// sends BLE command and returns data to a specific string. This function can be blocking (if you need it to perform a specific action) or non-blocking
// if you don't mind the data being used as its received.
boolean sendBLE(String command, String *returnString, boolean blocking)
{
  if (connected && !operationInProgress)
  {
    blockingCommandInProgress = blocking;
    operationInProgress = true;
    commandCharacteristic->setValue(command.c_str());
    commandCharacteristic->notify();

    printDebug("Sent BLE Command: " + command);

    if (blockingCommandInProgress)
    {

      currentDataField = "";

      unsigned long startTime = millis();
      while (operationInProgress && (startTime + 2000 > millis()))
        delay(25);

      operationInProgress = false;
      if (currentDataField.length() == 0)
      {
        return false;
      }
      else
      {
        *returnString = currentDataField;
        return true;
      }
    }
    else
    {
      currentDataField = "";
      bleReturnString = returnString;
      *returnString = currentDataField;

      unsigned long startTime = millis();
      while ((currentDataField.length() == 0) && (startTime + 1000 > millis()))
        delay(25);

      if (currentDataField.length() == 0)
      {
        operationInProgress = false;
        return false;
      }
      else
      {
        return true;
      }
    }
  }

  return false;
}

boolean sendBLE(String command)
{
  if (connected && !operationInProgress)
  {
    operationInProgress = true;
    commandCharacteristic->setValue(command.c_str());
    commandCharacteristic->notify();

    printDebug("Sent BLE Command: " + command);
    unsigned long startTime = millis();
    while (operationInProgress && (startTime + 200 > millis()))
      delay(25);
    return !operationInProgress;
  }
  return false;
}
