package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
            val latestLog = getBatteryLog(applicationContext) ?: return Result.failure()
            repository.addBatteryLog(latestLog)
            updateAllWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val iconWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, BatteryAppWidgetProvider::class.java))
        
        iconWidgetIds.forEach { widgetId ->
            widgetUpdater.updateWidget(context, appWidgetManager, widgetId)
        }
    }

    // CORRIGIDO: Usa apenas o Intent "sticky" para garantir consistência de dados.
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
