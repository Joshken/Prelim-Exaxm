package com.yourname.firebaseiotapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.yourname.firebaseiotapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private val historyList = mutableListOf<HistoryItem>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        
        // Setup RecyclerView for history
        setupHistoryRecyclerView()
        
        // Setup click listeners
        setupClickListeners()
        
        // Start listening to Firebase data
        startFirebaseListener()
        
        // Add initial history item
        addHistoryItem("App Started", "Application initialized successfully")
    }
    
    private fun setupHistoryRecyclerView() {
        historyAdapter = HistoryAdapter(historyList)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter
    }
    
    private fun setupClickListeners() {
        binding.btnSendMessage.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessageToFirebase(message)
                binding.etMessage.text?.clear()
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnSendCommand.setOnClickListener {
            sendTestCommand()
        }
        
        binding.btnWifiManager.setOnClickListener {
            val intent = Intent(this, WiFiManagerActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnClearHistory.setOnClickListener {
            historyList.clear()
            historyAdapter.notifyDataSetChanged()
            addHistoryItem("History Cleared", "All history items have been removed")
        }
    }
    
    private fun startFirebaseListener() {
        // Listen to ESP32 data
        val esp32Ref = database.getReference("test")
        
        esp32Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateConnectionStatus(true)
                
                // Update sensor value
                val sensorValue = snapshot.child("sensor_value").getValue(Double::class.java)
                if (sensorValue != null) {
                    binding.tvSensorValue.text = String.format("%.2f", sensorValue)
                }
                
                // Update count
                val count = snapshot.child("count").getValue(Int::class.java)
                if (count != null) {
                    binding.tvCount.text = count.toString()
                }
                
                // Update WiFi signal
                val wifiSignal = snapshot.child("wifi_signal").getValue(Int::class.java)
                if (wifiSignal != null) {
                    binding.tvWifiSignal.text = "$wifiSignal dBm"
                }
                
                // Update device ID
                val deviceId = snapshot.child("device_id").getValue(String::class.java)
                if (deviceId != null) {
                    binding.tvDeviceId.text = deviceId
                }
                
                // Update timestamp
                val timestamp = snapshot.child("timestamp").getValue(String::class.java)
                if (timestamp != null) {
                    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    binding.tvLastUpdate.text = currentTime
                }
                
                // Add to history
                addHistoryItem("Data Received", "Sensor: $sensorValue, Count: $count")
            }
            
            override fun onCancelled(error: DatabaseError) {
                updateConnectionStatus(false)
                addHistoryItem("Error", "Failed to read data: ${error.message}")
                Toast.makeText(this@MainActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun sendMessageToFirebase(message: String) {
        val messageRef = database.getReference("test/app_message")
        
        val messageData = mapOf(
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "sender" to "Android App"
        )
        
        messageRef.setValue(messageData)
            .addOnSuccessListener {
                addHistoryItem("Message Sent", "Message: $message")
                Toast.makeText(this, "Message sent successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                addHistoryItem("Send Failed", "Failed to send message: ${exception.message}")
                Toast.makeText(this, "Failed to send message: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun sendTestCommand() {
        val commandRef = database.getReference("test/app_command")
        
        val commandData = mapOf(
            "command" to "test_command",
            "timestamp" to System.currentTimeMillis(),
            "sender" to "Android App"
        )
        
        commandRef.setValue(commandData)
            .addOnSuccessListener {
                addHistoryItem("Command Sent", "Test command sent to ESP32")
                Toast.makeText(this, "Test command sent successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                addHistoryItem("Command Failed", "Failed to send command: ${exception.message}")
                Toast.makeText(this, "Failed to send command: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun updateConnectionStatus(isConnected: Boolean) {
        runOnUiThread {
            if (isConnected) {
                binding.statusIndicator.setBackgroundResource(R.drawable.circle_green)
                binding.tvConnectionStatus.text = "Connected"
            } else {
                binding.statusIndicator.setBackgroundResource(R.drawable.circle_red)
                binding.tvConnectionStatus.text = "Disconnected"
            }
        }
    }
    
    private fun addHistoryItem(type: String, message: String) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val historyItem = HistoryItem(type, message, currentTime)
        
        runOnUiThread {
            historyList.add(0, historyItem) // Add to beginning of list
            
            // Limit history to 50 items
            if (historyList.size > 50) {
                historyList.removeAt(historyList.size - 1)
            }
            
            historyAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up Firebase listeners if needed
    }
}
