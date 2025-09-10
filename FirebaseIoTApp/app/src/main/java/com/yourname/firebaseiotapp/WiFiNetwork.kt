package com.yourname.firebaseiotapp

data class WiFiNetwork(
    val ssid: String,
    val bssid: String,
    val security: String,
    val signalLevel: Int,
    val frequency: Int
)
