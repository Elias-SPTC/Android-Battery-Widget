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

class BatteryWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val widgetUpdater: WidgetUpdater by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        coroutineScope.launch {
            appWidgetIds.forEach { appWidgetId ->
                widgetUpdater.updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // A lógica de deleção pode ser movida para o WidgetUpdater também
        // para manter a consistência.
    }
}
