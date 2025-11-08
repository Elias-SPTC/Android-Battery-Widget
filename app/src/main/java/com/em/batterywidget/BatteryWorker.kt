package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Um Worker que periodicamente busca o estado da bateria e o salva no banco de dados.
 */
class BatteryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: BatteryRepository by inject()

    companion object {
        // Ação genérica para notificar widgets simples
        const val ACTION_WIDGET_UPDATE = "com.em.batterywidget.ACTION_WIDGET_UPDATE"
    }

    override suspend fun doWork(): Result {
        return try {
            captureAndLogBatteryState(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun captureAndLogBatteryState(context: Context) {
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Int = if (level != -1 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1

        if (batteryPct != -1) {
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
            val health: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
            val plugged: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val technology: String = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "N/A"
            val temperature: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val voltage: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

            val currentLog = BatteryLog(
                level = batteryPct, status = status, health = health, plugged = plugged,
                technology = technology, temperature = temperature, voltage = voltage
            )
            
            repository.addBatteryLog(currentLog)

            // CORRIGIDO: Envia broadcasts para TODOS os providers relevantes.
            sendUpdateBroadcasts(context)
        }
    }

    private fun sendUpdateBroadcasts(context: Context) {
        // Notifica o provider dos widgets simples (Ícone, Texto)
        Intent(context, BatteryAppWidgetProvider::class.java).also { intent ->
            intent.action = ACTION_WIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(android.content.ComponentName(context, BatteryAppWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }

        // Notifica o provider do widget de gráfico
        Intent(context, BatteryGraphWidgetProvider::class.java).also { intent ->
            intent.action = BatteryGraphWidgetProvider.ACTION_WIDGET_UPDATE_GRAPH_ONLY
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(android.content.ComponentName(context, BatteryGraphWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
