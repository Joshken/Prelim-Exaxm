/*
 * ESP32 Firebase IoT Project with WiFi Manager
 * Sends sensor data to Firebase Realtime Database
 * Compatible with Android app for real-time data synchronization
 * 
 * Required Libraries:
 * - Firebase ESP Client by mobizt
 * - ArduinoJson by Benoit Blanchon
 * - WebServer (built-in ESP32)
 * - EEPROM (built-in ESP32)
 * 
 * Instructions:
 * 1. Upload code to ESP32
 * 2. ESP32 will create WiFi hotspot "ESP32-Setup"
 * 3. Connect to hotspot and go to http://192.168.4.1
 * 4. Enter your WiFi credentials
 * 5. ESP32 will connect to your WiFi and start sending data to Firebase
 */

#include <WiFi.h>
#include <WebServer.h>
#include <EEPROM.h>
#include <Firebase_ESP_Client.h>
#include <ArduinoJson.h>

// WiFi Manager Configuration
#define AP_SSID "ESP32-Setup"
#define AP_PASSWORD "12345678"
#define AP_IP IPAddress(192, 168, 4, 1)
#define AP_GATEWAY IPAddress(192, 168, 4, 1)
#define AP_SUBNET IPAddress(255, 255, 255, 0)

// EEPROM addresses for storing WiFi credentials
#define EEPROM_SIZE 512
#define SSID_ADDR 0
#define PASS_ADDR 32

// Firebase Configuration - YOUR PROJECT DETAILS
#define FIREBASE_HOST "https://esp32-wifi-firebase-38e64-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "lkExJhc6Yr96cHRoiARskBUSOy3HunuxRrxEcFeD"

// Firebase Data object
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;


// Variables for data
unsigned long sendDataPrevMillis = 0;
int count = 0;
float sensorValue = 0;
bool signupOK = false;

void setup() {
  Serial.begin(115200);
  
  
  Serial.println("ESP32 Firebase IoT Project Starting...");
  Serial.println("=====================================");
  Serial.println("Firebase Host: " + String(FIREBASE_HOST));
  Serial.println("=====================================");
  
  // Connect to WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  
  Serial.println();
  Serial.println("WiFi Connected!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
  Serial.print("Signal Strength: ");
  Serial.print(WiFi.RSSI());
  Serial.println(" dBm");
  
  // Configure Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  
  // Initialize Firebase
  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("Firebase Authentication: OK");
    signupOK = true;
  } else {
    Serial.printf("Firebase Authentication: %s\n", config.signer.signupError.message.c_str());
  }
  
  // Assign the callback function for the long running token generation task
  config.token_status_callback = tokenStatusCallback;
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
  Serial.println("Firebase Initialized!");
  Serial.println("=====================================");
  Serial.println("System Ready - Sending data to Firebase...");
  Serial.println();
}

void loop() {
  // Send data every 5 seconds
  if (Firebase.ready() && signupOK && (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0)) {
    sendDataPrevMillis = millis();
    
    // Generate sample data
    sensorValue = random(0, 4095); // Random value between 0-4095
    
    // Send data to Firebase Realtime Database
    sendToRealtimeDatabase();
    
    count++;
  }
  
  // Check WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi connection lost! Attempting to reconnect...");
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    
    while (WiFi.status() != WL_CONNECTED) {
      Serial.print(".");
      delay(500);
    }
    Serial.println();
    Serial.println("WiFi reconnected!");
  }
  
  delay(1000);
}

// Send data to Firebase Realtime Database
void sendToRealtimeDatabase() {
  bool allDataSent = true;
  
  // Send sensor value
  if (Firebase.RTDB.setFloat(&fbdo, "/test/sensor_value", sensorValue)) {
    Serial.println("✓ Sensor value sent to Firebase");
  } else {
    Serial.println("✗ Failed to send sensor value");
    Serial.printf("Error: %s\n", fbdo.errorReason().c_str());
    allDataSent = false;
  }
  
  // Send count
  if (Firebase.RTDB.setInt(&fbdo, "/test/count", count)) {
    Serial.println("✓ Count sent to Firebase");
  } else {
    Serial.println("✗ Failed to send count");
    Serial.printf("Error: %s\n", fbdo.errorReason().c_str());
    allDataSent = false;
  }
  
  // Send device ID
  if (Firebase.RTDB.setString(&fbdo, "/test/device_id", "ESP32_001")) {
    Serial.println("✓ Device ID sent to Firebase");
  } else {
    Serial.println("✗ Failed to send device ID");
    Serial.printf("Error: %s\n", fbdo.errorReason().c_str());
    allDataSent = false;
  }
  
  // Send timestamp
  if (Firebase.RTDB.setString(&fbdo, "/test/timestamp", String(millis()))) {
    Serial.println("✓ Timestamp sent to Firebase");
  } else {
    Serial.println("✗ Failed to send timestamp");
    Serial.printf("Error: %s\n", fbdo.errorReason().c_str());
    allDataSent = false;
  }
  
  // Send WiFi signal strength
  if (Firebase.RTDB.setInt(&fbdo, "/test/wifi_signal", WiFi.RSSI())) {
    Serial.println("✓ WiFi signal sent to Firebase");
  } else {
    Serial.println("✗ Failed to send WiFi signal");
    Serial.printf("Error: %s\n", fbdo.errorReason().c_str());
    allDataSent = false;
  }
  
  // Send status
  if (Firebase.RTDB.setString(&fbdo, "/test/status", "active")) {
    Serial.println("✓ Status sent to Firebase");
  } else {
    Serial.println("✗ Failed to send status");
    Serial.printf("Error: %s\n", fbdo.errorReason().c_str());
    allDataSent = false;
  }
  
  // Display current data being sent
  Serial.println("-------------------------------------");
  Serial.printf("Data Sent - Count: %d, Sensor: %.2f, WiFi: %d dBm\n", 
                count, sensorValue, WiFi.RSSI());
  Serial.printf("Timestamp: %lu\n", millis());
  Serial.println("-------------------------------------");
  
  // Status feedback via Serial Monitor
  if (allDataSent) {
    Serial.println("✓ All data sent successfully!");
  } else {
    Serial.println("✗ Some data failed to send!");
  }
}

// Callback function for token status
void tokenStatusCallback(TokenInfo info) {
  if (info.status == token_status_error) {
    Serial.println("Token status: ERROR");
    Serial.printf("Token error: %s\n", info.error.message.c_str());
  } else {
    Serial.println("Token status: OK");
  }
}
