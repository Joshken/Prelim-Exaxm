# Firebase IoT App

This Android application connects to Firebase Realtime Database to receive data from an ESP32 device and send commands back to it.

## Features

- **Real-time Data Display**: Shows sensor data, count, WiFi signal, and device ID from ESP32
- **Connection Status**: Visual indicator showing Firebase connection status
- **Send Messages**: Send custom messages to ESP32
- **Send Commands**: Send test commands to ESP32
- **Update History**: Track all data updates and actions
- **Bidirectional Communication**: ESP32 ↔ Android App

## Setup Instructions

### 1. Firebase Setup
1. Create a Firebase project at https://console.firebase.google.com
2. Enable Realtime Database
3. Download `google-services.json` and place it in the `app/` folder
4. Update database rules to allow read/write access

### 2. Android Studio Setup
1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run the project

### 3. ESP32 Integration
The app expects data from ESP32 in the following Firebase structure:
```
/test/
  ├── sensor_value: (number)
  ├── count: (number)
  ├── wifi_signal: (number)
  ├── device_id: (string)
  ├── timestamp: (string)
  └── status: (string)
```

## Usage

1. **Monitor Data**: The app automatically displays real-time data from ESP32
2. **Send Messages**: Type a message and tap "Send Message"
3. **Send Commands**: Tap "Send Test Command" to send a test command
4. **View History**: Scroll through the update history to see all activities
5. **Clear History**: Tap "Clear History" to remove all history items

## Requirements

- Android API 24+ (Android 7.0)
- Internet connection
- Firebase project with Realtime Database enabled

## Project Structure

- `MainActivity.kt`: Main activity with Firebase integration
- `HistoryAdapter.kt`: RecyclerView adapter for history display
- `HistoryItem.kt`: Data class for history items
- `activity_main.xml`: Main UI layout
- `item_history.xml`: History item layout

## Firebase Database Structure

```
/test/
  ├── sensor_value: ESP32 sensor reading
  ├── count: Incremental counter
  ├── wifi_signal: WiFi signal strength
  ├── device_id: ESP32 device identifier
  ├── timestamp: Last update time
  ├── status: Device status
  ├── app_message: Messages from Android app
  └── app_command: Commands from Android app
```
