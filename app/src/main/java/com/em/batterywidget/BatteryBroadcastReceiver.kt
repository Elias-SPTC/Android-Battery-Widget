package com.em.batterywidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BatteryBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PowerActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            // Inicia o serviço de monitoramento no boot (permitido pelo sistema)
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed. Iniciando o BatteryMonitorService.")
                startMonitorService(context)
            }
            
            // Também inicia o serviço e aciona o worker ao conectar/desconectar energia
            Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG, "Evento de energia recebido: $action. Acionando o BatteryWorker.")
                startMonitorService(context) // Garante que o serviço está rodando
                
                val workRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    private fun startMonitorService(context: Context) {
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
