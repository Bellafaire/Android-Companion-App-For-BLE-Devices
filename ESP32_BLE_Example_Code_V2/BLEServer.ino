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
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

//variables and defines used by BLEServer.ino
String currentDataField;
#define SERVICE_UUID        "5ac9bc5e-f8ba-48d4-8908-98b80b566e49"
#define COMMAND_UUID        "bcca872f-1a3e-4491-b8ec-bfc93c5dd91a"
BLECharacteristic *commandCharacteristic;

//indicates connection state to the android device
boolean connected = false;

//indiciates whether or not a operation is currently in progress
boolean operationInProgress = false;

//function signitures
String sendBLE(String command);
void addData(String data);  //adds data to a current string, used within BLEServer.ino
void initBLE(); //initializes the BLE connection by starting advertising.




void addData(String data) {
//  Serial.println("Adding:" + data);
  currentDataField += data;
}

class cb : public BLEServerCallbacks    {
    void onConnect(BLEServer* pServer) {
      connected = true;
    }
    void onDisconnect(BLEServer* pServer) {
      connected = false;
    }
};

class ccb : public BLECharacteristicCallbacks  {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      addData(String( pCharacteristic->getValue().c_str()));
    }
    void onRead(BLECharacteristic* pCharacteristic) {
      //      Serial.println("Characteristic Read");
      operationInProgress = false;
    }
};

void initBLE() {
  BLEDevice::init("ESP32 Smartwatch");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);

  //define the characteristics and how they can be used
  commandCharacteristic = pService->createCharacteristic(
                            COMMAND_UUID,
                            BLECharacteristic::PROPERTY_READ   |
                            BLECharacteristic::PROPERTY_WRITE  |
                            BLECharacteristic::PROPERTY_NOTIFY
                          );


  //set all the callbacks
  commandCharacteristic->setCallbacks(new ccb());
  commandCharacteristic->setValue("");

  //add server callback so we can detect when we're connected.
  pServer->setCallbacks(new cb());

  pService->start();


  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);  // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
}

String sendBLE(String command) {
  operationInProgress = true;
  commandCharacteristic->setValue(command.c_str());
  commandCharacteristic->notify();

  currentDataField = "";

  unsigned long startTime = millis();
  while (operationInProgress && (startTime + 2000 > millis()))
    delay(25);

  return currentDataField;

}
