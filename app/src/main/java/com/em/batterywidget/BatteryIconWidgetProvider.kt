package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Provider para o widget de ícone (pilha em pé).
 */
class BatteryIconWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val workRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
