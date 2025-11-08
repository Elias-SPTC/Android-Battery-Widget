package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Provedor principal para os widgets. Delega a lógica de atualização para o WidgetUpdater.
 */
class BatteryWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val repository: BatteryRepository by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            WidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        coroutineScope.launch {
            appWidgetIds.forEach { appWidgetId ->
                repository.deleteWidgetType(appWidgetId)
            }
        }
    }
}
