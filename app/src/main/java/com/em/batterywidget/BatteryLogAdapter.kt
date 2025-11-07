package com.em.batterywidget.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.em.batterywidget.R
import com.em.batterywidget.data.BatteryLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter para exibir uma lista de objetos BatteryLog num RecyclerView.
 */
class BatteryLogAdapter : RecyclerView.Adapter<BatteryLogAdapter.BatteryLogViewHolder>() {

    // Lista de logs a serem exibidos. Usamos um setter simples para atualizar os dados.
    private var logs: List<BatteryLog> = emptyList()

    // Formatador de data e hora para exibição amigável
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    /**
     * Atualiza a lista de logs e notifica o RecyclerView para se redesenhar.
     */
    fun submitList(newLogs: List<BatteryLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_battery_log, parent, false)
        return BatteryLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatteryLogViewHolder, position: Int) {
        val currentLog = logs[position]
        holder.bind(currentLog)
    }

    override fun getItemCount(): Int = logs.size

    /**
     * ViewHolder para cada item da lista.
     */
    inner class BatteryLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val levelTextView: TextView = itemView.findViewById(R.id.tv_battery_level)
        private val statusTextView: TextView = itemView.findViewById(R.id.tv_charging_status)
        private val timestampTextView: TextView = itemView.findViewById(R.id.tv_timestamp)

        fun bind(log: BatteryLog) {
            // 1. Nível da Bateria
            levelTextView.text = "${log.level}%"

            // 2. Status do Carregamento
            val statusText = when (log.status) {
                1 -> "Não Carregando" // BATTERY_STATUS_UNKNOWN
                2 -> "A Carregar" // BATTERY_STATUS_CHARGING
                3 -> "Descarregando" // BATTERY_STATUS_DISCHARGING
                4 -> "Sem Carregamento" // BATTERY_STATUS_NOT_CHARGING
                5 -> "Completo" // BATTERY_STATUS_FULL
                else -> "Status Desconhecido"
            }
            // Adiciona a fonte (AC ou USB) ao status se estiver a carregar
            val sourceText = when (log.chargePlug) {
                1 -> " (AC)" // BATTERY_PLUGGED_AC
                2 -> " (USB)" // BATTERY_PLUGGED_USB
                4 -> " (Sem Fio)" // BATTERY_PLUGGED_WIRELESS
                else -> ""
            }
            statusTextView.text = statusText + if (log.status == 2) sourceText else ""

            // 3. Timestamp
            timestampTextView.text = "Registado em: ${dateFormat.format(Date(log.timestamp))}"
        }
    }
}