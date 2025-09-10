package com.yourname.firebaseiotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WiFiNetworkAdapter(
    private val networks: List<WiFiNetwork>,
    private val onConnectClick: (WiFiNetwork) -> Unit
) : RecyclerView.Adapter<WiFiNetworkAdapter.WiFiViewHolder>() {

    class WiFiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivWifiIcon: ImageView = itemView.findViewById(R.id.ivWifiIcon)
        val tvNetworkName: TextView = itemView.findViewById(R.id.tvNetworkName)
        val tvNetworkSecurity: TextView = itemView.findViewById(R.id.tvNetworkSecurity)
        val tvNetworkSignal: TextView = itemView.findViewById(R.id.tvNetworkSignal)
        val signalBar1: View = itemView.findViewById(R.id.signalBar1)
        val signalBar2: View = itemView.findViewById(R.id.signalBar2)
        val signalBar3: View = itemView.findViewById(R.id.signalBar3)
        val signalBar4: View = itemView.findViewById(R.id.signalBar4)
        val btnConnect: Button = itemView.findViewById(R.id.btnConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WiFiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_network, parent, false)
        return WiFiViewHolder(view)
    }

    override fun onBindViewHolder(holder: WiFiViewHolder, position: Int) {
        val network = networks[position]
        
        holder.tvNetworkName.text = network.ssid
        holder.tvNetworkSecurity.text = network.security
        holder.tvNetworkSignal.text = "${network.signalLevel} dBm"
        
        // Update signal strength bars
        updateSignalBars(holder, network.signalLevel)
        
        // Set connect button click listener
        holder.btnConnect.setOnClickListener {
            onConnectClick(network)
        }
        
        // Set security color
        when (network.security) {
            "Open" -> holder.tvNetworkSecurity.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            else -> holder.tvNetworkSecurity.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
        }
    }

    override fun getItemCount(): Int = networks.size
    
    private fun updateSignalBars(holder: WiFiViewHolder, signalLevel: Int) {
        // Reset all bars to gray
        val grayColor = holder.itemView.context.getColor(android.R.color.darker_gray)
        val greenColor = holder.itemView.context.getColor(android.R.color.holo_green_dark)
        
        holder.signalBar1.setBackgroundColor(grayColor)
        holder.signalBar2.setBackgroundColor(grayColor)
        holder.signalBar3.setBackgroundColor(grayColor)
        holder.signalBar4.setBackgroundColor(grayColor)
        
        // Set signal strength based on dBm
        when {
            signalLevel >= -50 -> {
                holder.signalBar1.setBackgroundColor(greenColor)
                holder.signalBar2.setBackgroundColor(greenColor)
                holder.signalBar3.setBackgroundColor(greenColor)
                holder.signalBar4.setBackgroundColor(greenColor)
            }
            signalLevel >= -60 -> {
                holder.signalBar1.setBackgroundColor(greenColor)
                holder.signalBar2.setBackgroundColor(greenColor)
                holder.signalBar3.setBackgroundColor(greenColor)
            }
            signalLevel >= -70 -> {
                holder.signalBar1.setBackgroundColor(greenColor)
                holder.signalBar2.setBackgroundColor(greenColor)
            }
            signalLevel >= -80 -> {
                holder.signalBar1.setBackgroundColor(greenColor)
            }
        }
    }
}
