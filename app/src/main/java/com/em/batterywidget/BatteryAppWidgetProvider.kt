package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BatteryAppWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val widgetUpdater: WidgetUpdater by inject()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            widgetUpdater.updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        widgetUpdater.deleteWidgetPreferences(appWidgetIds)
    }

    /**
     * CORRIGIDO: Recebe broadcasts para forçar a atualização dos widgets.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BatteryWorker.ACTION_WIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, BatteryAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
        super.onReceive(context, intent) // Garante que o onUpdate também seja chamado se necessário
    }
}
