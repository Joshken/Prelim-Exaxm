#include <WiFi.h>
#include <FirebaseESP32.h>

// Wi-Fi credentials (replace with your own)
#define WIFI_SSID "Chan 13"
#define WIFI_PASSWORD "chanb2323"

// Firebase project credentials (replace with your own)
#define FIREBASE_HOST "esp32-wifi-firebase-38e64-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "lkExJhc6Yr96cHRoiARskBUSOy3HunuxRrxEcFeD"

// Firebase object
FirebaseData fbdo;

// Variables
unsigned long sendDataPrevMillis = 0;
int count = 0;
float sensorValue = 0;

void setup() {
  Serial.begin(115200);

  // Connect to Wi-Fi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("Connected with IP: ");
  Serial.println(WiFi.localIP());
  Serial.println();

  // Firebase config
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.reconnectWiFi(true);

  // Buffer sizes for stability
  fbdo.setBSSLBufferSize(4096, 1024);
  fbdo.setResponseSize(2048);

  Serial.println("Firebase Initialized!");
}

void loop() {
  // Send data every 5 seconds
  if (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0) {
    sendDataPrevMillis = millis();
    sensorValue = random(0, 4095); // Simulated sensor value
    count++;

    // Upload values
    if (Firebase.setFloat(fbdo, "/test/sensor_value", sensorValue)) {
      Serial.println("✓ Sensor value sent");
    } else {
      Serial.println("✗ Sensor value error: " + fbdo.errorReason());
    }

    if (Firebase.setInt(fbdo, "/test/count", count)) {
      Serial.println("✓ Count sent");
    } else {
      Serial.println("✗ Count error: " + fbdo.errorReason());
    }

    if (Firebase.setString(fbdo, "/test/device_id", "ESP32_001")) {
      Serial.println("✓ Device ID sent");
    } else {
      Serial.println("✗ Device ID error: " + fbdo.errorReason());
    }

    if (Firebase.setString(fbdo, "/test/timestamp", String(millis()))) {
      Serial.println("✓ Timestamp sent");
    } else {
      Serial.println("✗ Timestamp error: " + fbdo.errorReason());
    }

    if (Firebase.setInt(fbdo, "/test/wifi_signal", WiFi.RSSI())) {
      Serial.println("✓ WiFi signal sent");
    } else {
      Serial.println("✗ WiFi signal error: " + fbdo.errorReason());
    }

    if (Firebase.setString(fbdo, "/test/status", "active")) {
      Serial.println("✓ Status sent");
    } else {
      Serial.println("✗ Status error: " + fbdo.errorReason());
    }

    Serial.println("-------------------------------------");
    Serial.printf("Data Sent - Count: %d, Sensor: %.2f, WiFi: %d dBm\n", count, sensorValue, WiFi.RSSI());
    Serial.printf("Timestamp: %lu\n", millis());
    Serial.println("-------------------------------------\n");
  }

  delay(1000);
}
