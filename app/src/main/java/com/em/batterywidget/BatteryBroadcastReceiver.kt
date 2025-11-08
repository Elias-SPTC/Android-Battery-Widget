package com.em.batterywidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * O único propósito deste Receiver é ouvir os eventos de energia e
 * acionar o BatteryWorker para uma atualização imediata.
 */
class BatteryBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PowerActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.d(TAG, "Evento de energia recebido: $action. Acionando o BatteryWorker.")
            
            // Cria e enfileira uma tarefa única e imediata.
            val workRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
