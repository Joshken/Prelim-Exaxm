package com.yourname.firebaseiotapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.yourname.firebaseiotapp.databinding.ActivityWifiManagerBinding
import java.text.SimpleDateFormat
import java.util.*

class WiFiManagerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWifiManagerBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiAdapter: WiFiNetworkAdapter
    private val wifiNetworks = mutableListOf<WiFiNetwork>()
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        
        // Initialize WiFi Manager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        // Setup RecyclerView
        setupWiFiRecyclerView()
        
        // Setup click listeners
        setupClickListeners()
        
        // Start monitoring ESP32 status
        startESP32StatusListener()
        
        // Check permissions and scan for networks
        checkPermissionsAndScan()
    }
    
    private fun setupWiFiRecyclerView() {
        wifiAdapter = WiFiNetworkAdapter(wifiNetworks) { network ->
            showPasswordDialog(network)
        }
        binding.rvWifiNetworks.layoutManager = LinearLayoutManager(this)
        binding.rvWifiNetworks.adapter = wifiAdapter
    }
    
    private fun setupClickListeners() {
        binding.btnScanWifi.setOnClickListener {
            scanForWiFiNetworks()
        }
        
        binding.btnConfigureWifi.setOnClickListener {
            configureESP32WiFi()
        }
        
        binding.btnResetWifi.setOnClickListener {
            resetESP32WiFi()
        }
        
        binding.btnRestartEsp32.setOnClickListener {
            restartESP32()
        }
        
        binding.btnFactoryReset.setOnClickListener {
            factoryResetESP32()
        }
        
        binding.btnBackToMain.setOnClickListener {
            finish()
        }
    }
    
    private fun checkPermissionsAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            scanForWiFiNetworks()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanForWiFiNetworks()
            } else {
                Toast.makeText(this, "Location permission required for WiFi scanning", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun scanForWiFiNetworks() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable WiFi first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }
        
        wifiManager.startScan()
        val results = wifiManager.scanResults
        
        wifiNetworks.clear()
        results.forEach { scanResult ->
            val network = WiFiNetwork(
                ssid = scanResult.SSID,
                bssid = scanResult.BSSID,
                security = getSecurityType(scanResult),
                signalLevel = scanResult.level,
                frequency = scanResult.frequency
            )
            wifiNetworks.add(network)
        }
        
        // Sort by signal strength
        wifiNetworks.sortByDescending { it.signalLevel }
        
        wifiAdapter.notifyDataSetChanged()
        Toast.makeText(this, "Found ${wifiNetworks.size} networks", Toast.LENGTH_SHORT).show()
    }
    
    private fun getSecurityType(scanResult: ScanResult): String {
        return when {
            scanResult.capabilities.contains("WPA3") -> "WPA3"
            scanResult.capabilities.contains("WPA2") -> "WPA2"
            scanResult.capabilities.contains("WPA") -> "WPA"
            scanResult.capabilities.contains("WEP") -> "WEP"
            else -> "Open"
        }
    }
    
    private fun showPasswordDialog(network: WiFiNetwork) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_wifi_password, null)
        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etWifiPassword)
        val tvNetworkName = dialogView.findViewById<android.widget.TextView>(R.id.tvNetworkName)
        
        tvNetworkName.text = network.ssid
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<android.widget.Button>(R.id.btnConnect).setOnClickListener {
            val password = etPassword.text.toString()
            connectToWiFiNetwork(network, password)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun connectToWiFiNetwork(network: WiFiNetwork, password: String) {
        val wifiConfigRef = database.getReference("test/wifi_config")
        
        val wifiConfig = mapOf(
            "ssid" to network.ssid,
            "password" to password,
            "timestamp" to System.currentTimeMillis(),
            "command" to "configure_wifi"
        )
        
        wifiConfigRef.setValue(wifiConfig)
            .addOnSuccessListener {
                Toast.makeText(this, "WiFi configuration sent to ESP32", Toast.LENGTH_SHORT).show()
                updateCurrentWiFiConfig(network.ssid, "Configuring...")
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to send WiFi config: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun configureESP32WiFi() {
        val ssid = binding.etWifiSsid.text.toString().trim()
        val password = binding.etWifiPassword.text.toString().trim()
        
        if (ssid.isEmpty()) {
            Toast.makeText(this, "Please enter WiFi SSID", Toast.LENGTH_SHORT).show()
            return
        }
        
        val wifiConfigRef = database.getReference("test/wifi_config")
        
        val wifiConfig = mapOf(
            "ssid" to ssid,
            "password" to password,
            "timestamp" to System.currentTimeMillis(),
            "command" to "configure_wifi"
        )
        
        wifiConfigRef.setValue(wifiConfig)
            .addOnSuccessListener {
                Toast.makeText(this, "WiFi configuration sent to ESP32", Toast.LENGTH_SHORT).show()
                updateCurrentWiFiConfig(ssid, "Configuring...")
                binding.etWifiSsid.text?.clear()
                binding.etWifiPassword.text?.clear()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to send WiFi config: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun resetESP32WiFi() {
        AlertDialog.Builder(this)
            .setTitle("Reset WiFi Configuration")
            .setMessage("Are you sure you want to reset the ESP32 WiFi configuration?")
            .setPositiveButton("Reset") { _, _ ->
                val resetRef = database.getReference("test/wifi_config")
                val resetCommand = mapOf(
                    "command" to "reset_wifi",
                    "timestamp" to System.currentTimeMillis()
                )
                
                resetRef.setValue(resetCommand)
                    .addOnSuccessListener {
                        Toast.makeText(this, "WiFi reset command sent to ESP32", Toast.LENGTH_SHORT).show()
                        updateCurrentWiFiConfig("Not Set", "Resetting...")
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to send reset command: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun restartESP32() {
        AlertDialog.Builder(this)
            .setTitle("Restart ESP32")
            .setMessage("Are you sure you want to restart the ESP32?")
            .setPositiveButton("Restart") { _, _ ->
                val restartRef = database.getReference("test/esp32_control")
                val restartCommand = mapOf(
                    "command" to "restart",
                    "timestamp" to System.currentTimeMillis()
                )
                
                restartRef.setValue(restartCommand)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Restart command sent to ESP32", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to send restart command: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun factoryResetESP32() {
        AlertDialog.Builder(this)
            .setTitle("Factory Reset ESP32")
            .setMessage("Are you sure you want to factory reset the ESP32? This will erase all settings.")
            .setPositiveButton("Reset") { _, _ ->
                val factoryResetRef = database.getReference("test/esp32_control")
                val factoryResetCommand = mapOf(
                    "command" to "factory_reset",
                    "timestamp" to System.currentTimeMillis()
                )
                
                factoryResetRef.setValue(factoryResetCommand)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Factory reset command sent to ESP32", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to send factory reset command: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startESP32StatusListener() {
        val esp32Ref = database.getReference("test")
        
        esp32Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateESP32Status(true)
                
                // Update current WiFi configuration
                val deviceId = snapshot.child("device_id").getValue(String::class.java)
                val wifiSignal = snapshot.child("wifi_signal").getValue(Int::class.java)
                
                if (deviceId != null) {
                    updateCurrentWiFiConfig(deviceId, "Connected")
                }
                
                if (wifiSignal != null) {
                    updateWiFiStatus("Connected ($wifiSignal dBm)")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                updateESP32Status(false)
                updateWiFiStatus("Disconnected")
            }
        })
    }
    
    private fun updateESP32Status(isConnected: Boolean) {
        runOnUiThread {
            if (isConnected) {
                binding.esp32StatusIndicator.setBackgroundResource(R.drawable.circle_green)
                binding.tvEsp32Status.text = "ESP32 Connected"
                binding.tvEsp32Ip.text = "IP: Connected via Firebase"
            } else {
                binding.esp32StatusIndicator.setBackgroundResource(R.drawable.circle_red)
                binding.tvEsp32Status.text = "ESP32 Not Connected"
                binding.tvEsp32Ip.text = "IP: Not Available"
            }
        }
    }
    
    private fun updateCurrentWiFiConfig(ssid: String, status: String) {
        runOnUiThread {
            binding.tvCurrentSsid.text = ssid
            binding.tvWifiStatus.text = status
        }
    }
    
    private fun updateWiFiStatus(status: String) {
        runOnUiThread {
            binding.tvWifiStatus.text = status
        }
    }
}
