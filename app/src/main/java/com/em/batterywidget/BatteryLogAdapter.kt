package com.em.batterywidget

import android.os.BatteryManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatteryLogAdapter : ListAdapter<BatteryLog, BatteryLogAdapter.BatteryLogViewHolder>(BatteryLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_battery_log, parent, false)
        return BatteryLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatteryLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BatteryLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val levelTextView: TextView = itemView.findViewById(R.id.tv_battery_level)
        private val statusTextView: TextView = itemView.findViewById(R.id.tv_charging_status)
        private val timestampTextView: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        fun bind(log: BatteryLog) {
            levelTextView.text = "${log.level}%"
            timestampTextView.text = dateFormat.format(Date(log.timestampMillis))
            statusTextView.text = getStatusString(log)
        }

        private fun getStatusString(log: BatteryLog): String {
            val context = itemView.context
            val status = when (log.status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.status_charging)
                BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.status_discharging)
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.status_not_charging)
                BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.status_full)
                else -> context.getString(R.string.status_unknown)
            }
            val plugged = when (log.plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> " (AC)"
                BatteryManager.BATTERY_PLUGGED_USB -> " (USB)"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> " (Wireless)"
                else -> ""
            }
            return status + plugged
        }
    }
}

class BatteryLogDiffCallback : DiffUtil.ItemCallback<BatteryLog>() {
    override fun areItemsTheSame(oldItem: BatteryLog, newItem: BatteryLog): Boolean {
        return oldItem.timestampMillis == newItem.timestampMillis
    }

    override fun areContentsTheSame(oldItem: BatteryLog, newItem: BatteryLog): Boolean {
        return oldItem == newItem
    }
}
