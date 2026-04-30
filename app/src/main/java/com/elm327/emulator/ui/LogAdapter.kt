package com.elm327.emulator.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elm327.emulator.databinding.ItemLogBinding

class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val logMessages = mutableListOf<String>()
    private val maxMessages = 100

    fun addMessage(message: String) {
        logMessages.add(0, message)
        if (logMessages.size > maxMessages) {
            logMessages.removeAt(logMessages.lastIndex)
        }
        notifyItemInserted(0)
    }

    fun clear() {
        logMessages.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logMessages[position])
    }

    override fun getItemCount() = logMessages.size

    class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: String) {
            binding.logItemText.text = message
        }
    }
}
