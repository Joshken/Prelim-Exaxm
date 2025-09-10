package com.yourname.firebaseiotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvType: TextView = itemView.findViewById(R.id.tvHistoryType)
        val tvMessage: TextView = itemView.findViewById(R.id.tvHistoryMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvHistoryTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvType.text = item.type
        holder.tvMessage.text = item.message
        holder.tvTime.text = item.timestamp
    }

    override fun getItemCount(): Int = historyList.size
}
