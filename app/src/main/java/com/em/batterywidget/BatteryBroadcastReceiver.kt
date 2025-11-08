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
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Dispositivo reiniciado. A classe App irá reagendar o trabalho periódico.")
                // A classe App cuida do agendamento periódico.
            }

            // Eventos que exigem uma atualização imediata.
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_CHANGED -> {
                Log.d(TAG, "Evento de bateria recebido (${intent.action}). Acionando o Worker.")
                // Cria e enfileira um pedido de trabalho único para ser executado agora.
                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
                WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
            }
        }
    }
}
