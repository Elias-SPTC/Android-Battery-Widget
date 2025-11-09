package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BatteryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: BatteryRepository by inject()
    private val widgetUpdater: WidgetUpdater by inject()

    override suspend fun doWork(): Result {
        return try {
            val latestLog = getBatteryLog(applicationContext)
            if (latestLog != null) {
                repository.addBatteryLog(latestLog)
            }
            updateAllWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("BatteryWorker", "Erro fatal no doWork", e)
            Result.failure()
        }
    }

    private suspend fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        val tableWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BatteryAppWidgetProvider::class.java))
        val graphWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BatteryGraphWidgetProvider::class.java))
        val iconWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BatteryIconWidgetProvider::class.java))
        val textWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BatteryTextWidgetProvider::class.java))

        (tableWidgetIds + graphWidgetIds + iconWidgetIds + textWidgetIds).forEach { widgetId ->
            try {
                widgetUpdater.updateWidget(context, appWidgetManager, widgetId)
            } catch (e: Exception) {
                Log.e("BatteryWorker", "Falha ao atualizar o widget ID $widgetId", e)
            }
        }
    }

    private fun getBatteryLog(context: Context): BatteryLog? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) return null

        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct: Int = if (level != -1 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1

        if (batteryPct == -1) return null

        return BatteryLog(
            level = batteryPct,
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
            health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1),
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1),
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "N/A",
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0),
            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        )
    }
}
