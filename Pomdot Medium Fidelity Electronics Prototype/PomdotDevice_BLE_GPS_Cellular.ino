#include <IPAddress.h>
#include <SparkFun_u-blox_SARA-R5_Arduino_Library.h> //Click here to get the library: http://librarymanager/All#SparkFun_u-blox_SARA-R5_Arduino_Library
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include "BLE_Header.h"

// ThingSpeak via HTTP POST / GET
String myWriteAPIKey = "55GYJ6LV2C9H4M83"; // Change this to your API key
String serverName = "api.thingspeak.com"; // Domain Name for HTTP POST / GET

// SARA-R5
#define saraSerial Serial2
#define detachSensePin 18
#define bleRebootPin 19
#define gracefulExitPin 20

SARA_R5 mySARA;
PositionData gps;
SpeedData spd;
ClockData clk;
bool valid;
volatile bool detached;
volatile bool bleReboot;
int lastExit;
int lastBLEReboot;
int lastDetached;
int detachedField;
boolean gracefulExit;
unsigned long lastGpsPoll = 0;
bool _BLEClientConnected = false;
bool restartBLE = false;
const String pomdotTitle = "POMDOT!";
const int GPS_PERIOD = 1;  //Seconds
BLEServer *pServer;
BLEService *pPOMDOT;
float defaultLat = 33.7752778;
float defaultLon = -84.3995466;


//BLE
// POMDOT Service
BLECharacteristic POMDOTCharacteristic(BLEUUID((uint16_t) POMDOT_CHAR_UUID), BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor POMDOTDescriptor(BLEUUID((uint16_t) POMDOT_DESC_UUID));
// Detached Service
BLECharacteristic DetachedCharacteristic(DETACHED_CHAR_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor DetachedDescriptor(BLEUUID((uint16_t) DETACHED_DESC_UUID));
// Location Service
BLECharacteristic LocationLatCharacteristic(LOCATION_LAT_CHAR_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor LocationLatDescriptor(BLEUUID((uint16_t) LOCATION_LAT_DESC_UUID));

BLECharacteristic LocationLonCharacteristic(LOCATION_LON_CHAR_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor LocationLonDescriptor(BLEUUID((uint16_t) LOCATION_LON_DESC_UUID));
BLECharacteristic DateCharacteristic(DATE_CHAR_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor DateDescriptor(BLEUUID((uint16_t) DATE_DESC_UUID));
BLECharacteristic TimeCharacteristic(TIME_CHAR_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor TimeDescriptor(BLEUUID((uint16_t) TIME_DESC_UUID));

class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    _BLEClientConnected = true;
  };

  void onDisconnect(BLEServer* pServer) {
    _BLEClientConnected = false;
    restartBLE = false;
  }
};

void initBLE() {
  BLEDevice::init(BLE_GATT_SERVER_NAME);

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the POMDOT Service
  pPOMDOT = pServer->createService(POMDOTService);
  pPOMDOT->addCharacteristic(&POMDOTCharacteristic);
  POMDOTDescriptor.setValue("POMDOT Identifier Service");
  POMDOTCharacteristic.addDescriptor(&POMDOTDescriptor);
  POMDOTCharacteristic.addDescriptor(new BLE2902());
  pServer->getAdvertising()->addServiceUUID(POMDOTService);
  pPOMDOT->start();
  
  // Create the BLE DETACHED Service
  BLEService *pDetached = pServer->createService(DetachedService);
  pDetached->addCharacteristic(&DetachedCharacteristic);
  DetachedDescriptor.setValue("Detached Indicator");
  DetachedCharacteristic.addDescriptor(&DetachedDescriptor);
  DetachedCharacteristic.addDescriptor(new BLE2902());
  pServer->getAdvertising()->addServiceUUID(DetachedService);
  pDetached->start();

  // Create the BLE Location Service
  BLEService *pLocation = pServer->createService(LocationService);
  pLocation->addCharacteristic(&LocationLatCharacteristic);
  pLocation->addCharacteristic(&LocationLonCharacteristic);
  pLocation->addCharacteristic(&TimeCharacteristic);
  pLocation->addCharacteristic(&DateCharacteristic);
  
  LocationLatDescriptor.setValue("GPS Latitude Location");
  LocationLatCharacteristic.addDescriptor(&LocationLatDescriptor);
  LocationLatCharacteristic.addDescriptor(new BLE2902());
  LocationLonDescriptor.setValue("GPS Longitude Location");
  LocationLonCharacteristic.addDescriptor(&LocationLonDescriptor);
  LocationLonCharacteristic.addDescriptor(new BLE2902());
  TimeCharacteristic.addDescriptor(&TimeDescriptor);
  TimeCharacteristic.addDescriptor(new BLE2902());
  TimeDescriptor.setValue("GPS Time");
  DateCharacteristic.addDescriptor(&DateDescriptor);
  DateCharacteristic.addDescriptor(new BLE2902());
  DateDescriptor.setValue("GPS Date");
  pServer->getAdvertising()->addServiceUUID(LocationService);
  pLocation->start();

  // Start the server
  pServer->getAdvertising()->start();
  Serial.println("BLE Advertising");
}


// processHTTPcommandResult is provided to the SARA-R5 library via a 
// callback setter -- setHTTPCommandCallback. (See the end of setup())
void processHTTPcommandResult(int profile, int command, int result)
{
  Serial.println();
  Serial.print(F("HTTP Command Result:  profile: "));
  Serial.print(profile);
  Serial.print(F("  command: "));
  Serial.print(command);
  Serial.print(F("  result: "));
  Serial.print(result);
  if (result == 0)
    Serial.print(F(" (fail)"));
  if (result == 1)
    Serial.print(F(" (success)"));
  Serial.println();

  // Get and print the most recent HTTP protocol error
  int error_class;
  int error_code;
  mySARA.getHTTPprotocolError(0, &error_class, &error_code);
  Serial.print(F("Most recent HTTP protocol error:  class: "));
  Serial.print(error_class);
  Serial.print(F("  code: "));
  Serial.print(error_code);
  if (error_code == 0)
    Serial.print(F(" (no error)"));
  Serial.println();

  // Read and print the HTTP POST result
  String postResult = "";
  mySARA.getFileContents("post_response.txt", &postResult);
  Serial.print(F("HTTP command result was: "));
  Serial.println(postResult);

  Serial.println();
}

void setup()
{
//  pinMode(gracefulExitPin, INPUT);
  pinMode(detachSensePin, INPUT_PULLUP);
  detachedField = 0;
//  pinMode(bleRebootPin, INPUT);
//  attachInterrupt(digitalPinToInterrupt(gracefulExitPin), setGracefulExit, CHANGE);
  attachInterrupt(digitalPinToInterrupt(detachSensePin), setDetachSense, RISING);
//  attachInterrupt(digitalPinToInterrupt(bleRebootPin), executeBleReboot, RISING);
  gracefulExit = false;
  detached = false;
  lastExit = millis();
  lastDetached = millis();
  lastBLEReboot = millis();
  
  String currentOperator = "";

  Serial.begin(115200); // Start the serial console

  // Wait for user to press key to begin
  Serial.println(F("SARA-R5 Example"));

//  mySARA.enableDebugging(); // Uncomment this line to enable helpful debug messages on Serial

  // For the MicroMod Asset Tracker, we need to invert the power pin so it pulls high instead of low
  // Comment the next line if required
  mySARA.invertPowerPin(true); 

  // Initialize the SARA
  if (mySARA.begin(saraSerial, 9600)) {
    Serial.println(F("SARA-R5 connected!"));
  } else {
    Serial.println(F("Unable to communicate with the SARA."));
    Serial.println(F("Manually power-on (hold the SARA On button for 3 seconds) on and try again."));
    while (1) ; // Loop forever on fail
  }
  Serial.println();

  // First check to see if we're connected to an operator:
  while (mySARA.getOperator(&currentOperator) != SARA_R5_SUCCESS) {
    Serial.print(F("The SARA is not yet connected to an operator. Waiting for connection"));
    delay(1000);
  }
  Serial.print(F("Connected to: "));
  Serial.println(currentOperator);

  // Deactivate the PSD profile - in case one is already active
  if (mySARA.performPDPaction(0, SARA_R5_PSD_ACTION_DEACTIVATE) != SARA_R5_SUCCESS) {
    Serial.println(F("Warning: performPDPaction (deactivate profile) failed. Probably because no profile was active."));
  }

  // Load the PSD profile from NVM - these were saved by a previous example
  if (mySARA.performPDPaction(0, SARA_R5_PSD_ACTION_LOAD) != SARA_R5_SUCCESS){
    Serial.println(F("performPDPaction (load from NVM) failed! Freezing..."));
    while (1)
      ; // Do nothing more
  }

  // Activate the PSD profile
  if (mySARA.performPDPaction(0, SARA_R5_PSD_ACTION_ACTIVATE) != SARA_R5_SUCCESS)
  {
    Serial.println(F("performPDPaction (activate profile) failed! Freezing..."));
    while (1); // Do nothing more
  }

  // Reset HTTP profile 0
  mySARA.resetHTTPprofile(0);
  
  // Set the server name
  mySARA.setHTTPserverName(0, serverName);
  
  // Use HTTPS
  mySARA.setHTTPsecure(0, false); // Setting this to true causes the POST / GET to fail. Not sure why...

  // Set a callback to process the HTTP command result
  mySARA.setHTTPCommandCallback(&processHTTPcommandResult);

  // Clear the time offset
  mySARA.setUtimeConfiguration(); // Use default offset (offsetNanoseconds = 0, offsetSecs = 0)
  // Set the UTIME mode to pulse-per-second output using a best effort from GNSS and LTE
  mySARA.setUtimeMode(); // Use defaults (mode = SARA_R5_UTIME_MODE_PPS, sensor = SARA_R5_UTIME_SENSOR_GNSS_LTE)
  mySARA.gpsEnableRmc(); // Enable GPRMC messages

  // Initialize BLE Stuff
  initBLE();
  POMDOTCharacteristic.setValue("POMDOT!");
  POMDOTCharacteristic.notify();
}

void loop()
{
  if (detached) {
    detachedField++;
    detached = false;
    Serial.println("\n\nDEVICE DETACHED\n\n");
  }
  // Call (mySARA.gpsGetRmc to get coordinate, speed, and timing data
  while (mySARA.gpsGetRmc(&gps, &spd, &clk, &valid) != SARA_R5_SUCCESS) {};
  int t = clk.time.second + clk.time.minute * 100 + clk.time.hour * 10000;
  int d = clk.date.day * 10 + clk.date.month * 1000 + clk.date.year * 100000 + detachedField % 10;
  
  Serial.println(String(gps.lat, 7) + "\n" + String(gps.lon, 7));
  Serial.println(String(clk.date.month) + '/' + String(clk.date.day) + '/' + String(clk.date.year) + ' ' + String(clk.time.hour) + ":" + String(clk.time.minute) + ":" + String(clk.time.second) + "." + String(clk.time.ms));
  Serial.print(d); Serial.print(' '); Serial.println(t);

  if (abs(gps.lat) > 0.1) {
    Serial.println("Real GPS Location detected, transmitting");
    
    LocationLatCharacteristic.setValue(gps.lat);
    LocationLonCharacteristic.setValue(gps.lon);
    DetachedCharacteristic.setValue(detachedField);
    Serial.println("\t\t\t\t\t\t" + String(detachedField));
    DateCharacteristic.setValue(d);
    TimeCharacteristic.setValue(t);
    if (_BLEClientConnected) {
      DetachedCharacteristic.notify();
      LocationLatCharacteristic.notify();
      LocationLonCharacteristic.notify();
      DateCharacteristic.notify();
      TimeCharacteristic.notify();
      Serial.println("Using BLE");
    }
    
    // Send data using HTTP GET
    String path = "/update?api_key=" + myWriteAPIKey + "&field1=" + String(gps.lat, 7) + "&field2=" + String(gps.lon, 7) \
                + "&field3=" + String(t) + "&field4=" + String(d) + "&field5=" + String(detachedField % 10);
    // Send HTTP POST request to /update. The reponse will be written to post_response.txt in the SARA's file system
    mySARA.sendHTTPGET(0, path, "post_response.txt");
    Serial.println("Using Cellular");
  } else {
    LocationLatCharacteristic.setValue(defaultLat);
    LocationLonCharacteristic.setValue(defaultLon);
    DetachedCharacteristic.setValue(detachedField);
    Serial.println("\t\t\t\t\t\t" + String(detachedField));
    DateCharacteristic.setValue(d);
    if (_BLEClientConnected) {
      DetachedCharacteristic.notify();
      LocationLatCharacteristic.notify();
      LocationLonCharacteristic.notify();
      DateCharacteristic.notify();
    }
    String path = "/update?api_key=" + myWriteAPIKey + "&field5=" + String(detachedField % 10);
    mySARA.sendHTTPGET(0, path, "post_response.txt");
    Serial.println("GPS Location not yet detected");
  }

  // Wait for n seconds
  for (int i = 0; i < GPS_PERIOD * 1000; i++)
  {
    mySARA.poll(); // Keep processing data from the SARA so we can catch the HTTP command result
  }
  if (!restartBLE && !_BLEClientConnected) {
    pServer->getAdvertising()->start();
    restartBLE = true;
  }
  if (gracefulExit) {
    delay(5000);
    exit(0);
  }
}

void setDetachSense() {
  if (millis() - lastDetached > 1000) {
    lastDetached = millis();
    detached = true;
  }
}

//void setGracefulExit() {
//  if (millis() - lastExit > 1000) {
//    lastExit = millis();
//    gracefulExit = digitalRead(gracefulExitPin);
//  }
//}

//void executeBleReboot() {
//  if (millis() - lastBLEReboot > 1000) {
//    lastBLEReboot = millis();
//    initBLE();
//  }
//}
