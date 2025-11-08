package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object WidgetUpdater : KoinComponent {

    private val repository: BatteryRepository by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    // CORRIGIDO: Constantes agora são públicas e acessíveis pela WidgetActivity
    const val TYPE_ICON_DETAIL = 0
    const val TYPE_GRAPH = 1
    const val TYPE_TEXT_ONLY = 2

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        coroutineScope.launch {
            val widgetType = repository.widgetPreferencesFlow.first().widgetType[appWidgetId] ?: TYPE_ICON_DETAIL
            val latestLog = repository.getLatestBatteryLog().first()

            CoroutineScope(Dispatchers.Main).launch {
                val views = when (widgetType) {
                    TYPE_ICON_DETAIL -> createIconDetailView(context, latestLog)
                    TYPE_TEXT_ONLY -> createTextOnlyView(context, latestLog)
                    else -> createIconDetailView(context, latestLog)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun createIconDetailView(context: Context, log: BatteryLog?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_icon_detail)
        if (log != null) {
            views.setTextViewText(R.id.battery_level_text, "${log.level}")
        } else {
            views.setTextViewText(R.id.battery_level_text, "N/A")
        }
        views.setOnClickPendingIntent(R.id.widget_root_layout, getLaunchAppPendingIntent(context))
        return views
    }

    private fun createTextOnlyView(context: Context, log: BatteryLog?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_text_only)
        if (log != null) {
            val isCharging = log.status == BatteryManager.BATTERY_STATUS_CHARGING

            views.setTextViewText(R.id.battery_level_text_only, "${log.level}%")
            
            views.setProgressBar(R.id.battery_progress_bar, 100, log.level, false)

            views.setViewVisibility(R.id.charging_bolt_icon, if (isCharging) View.VISIBLE else View.GONE)

        } else {
            views.setTextViewText(R.id.battery_level_text_only, "N/A")
            views.setProgressBar(R.id.battery_progress_bar, 100, 0, false)
            views.setViewVisibility(R.id.charging_bolt_icon, View.GONE)
        }
        views.setOnClickPendingIntent(R.id.widget_text_container, getLaunchAppPendingIntent(context))
        return views
    }

    fun deleteWidgetPreferences(appWidgetIds: IntArray) {
        coroutineScope.launch {
            appWidgetIds.forEach { repository.deleteWidgetType(it) }
        }
    }

    private fun getLaunchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
