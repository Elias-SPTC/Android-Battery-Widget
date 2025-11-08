package com.em.batterywidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Recebe eventos importantes do sistema (boot, carregador conectado/desconectado)
 * para acionar uma atualização imediata da bateria usando um OneTimeWorkRequest.
 */
class BatteryBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Quando o telemóvel reinicia, a App agenda o trabalho periódico novamente.
            // Esta lógica já está na classe App, então o BOOT_COMPLETED no manifesto garante que a App seja iniciada.
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Dispositivo reiniciado. O trabalho periódico será reagendado pela classe App.")
                // A classe App cuidará do agendamento.
            }

            // Quando o carregador é conectado ou desconectado, queremos uma atualização IMEDIATA.
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG, "Carregador conectado/desconectado. Acionando atualização imediata.")
                // Cria e enfileira um pedido de trabalho único para ser executado agora.
                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
                WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
            }
        }
    }
}
