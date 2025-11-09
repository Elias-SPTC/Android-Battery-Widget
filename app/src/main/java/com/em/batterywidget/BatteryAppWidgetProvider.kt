package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Provider para os widgets que NÃO são de gráfico.
 * Sua única responsabilidade é acionar o BatteryWorker.
 */
class BatteryAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Apenas enfileira o trabalho. O Worker fará o resto.
        val workRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
