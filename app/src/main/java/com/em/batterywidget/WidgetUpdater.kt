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

    const val TYPE_ICON_DETAIL = 0
    const val TYPE_GRAPH = 1
    const val TYPE_TEXT_ONLY = 2
    const val TYPE_DETAILS_TABLE = 3

    suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val widgetType = repository.widgetPreferencesFlow.first().widgetType[appWidgetId] ?: TYPE_ICON_DETAIL
        val latestLog = repository.getLatestBatteryLog().first()

        val views = when (widgetType) {
            TYPE_ICON_DETAIL -> createIconDetailView(context, latestLog)
            TYPE_TEXT_ONLY -> createTextOnlyView(context, latestLog)
            TYPE_DETAILS_TABLE -> createDetailsTableView(context, latestLog)
            else -> createIconDetailView(context, latestLog)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createIconDetailView(context: Context, log: BatteryLog?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_icon_detail)
        if (log != null) {
            val isCharging = log.status == BatteryManager.BATTERY_STATUS_CHARGING

            views.setTextViewText(R.id.battery_level_text, "${log.level}%")
            views.setProgressBar(R.id.battery_progress_bar_vertical, 100, log.level, false)
            views.setViewVisibility(R.id.charging_bolt_icon_vertical, if (isCharging) View.VISIBLE else View.GONE)
        } else {
            views.setTextViewText(R.id.battery_level_text, "N/A")
            views.setProgressBar(R.id.battery_progress_bar_vertical, 100, 0, false)
            views.setViewVisibility(R.id.charging_bolt_icon_vertical, View.GONE)
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

    private fun createDetailsTableView(context: Context, log: BatteryLog?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_details_table)
        if (log != null) {
            views.setTextViewText(R.id.tv_details_level, "${log.level}%")
            views.setTextViewText(R.id.tv_details_status, getStatusString(context, log.status))
            views.setTextViewText(R.id.tv_details_health, getHealthString(context, log.health))
            views.setTextViewText(R.id.tv_details_temp, "${log.temperature / 10.0}°C")
            views.setTextViewText(R.id.tv_details_voltage, "${log.voltage / 1000.0} V")
            views.setTextViewText(R.id.tv_details_plugged, getPluggedString(context, log.plugged))
            views.setTextViewText(R.id.tv_details_technology, log.technology)
        } else {
            views.setTextViewText(R.id.tv_details_level, "N/A")
            views.setTextViewText(R.id.tv_details_status, "N/A")
            views.setTextViewText(R.id.tv_details_health, "N/A")
            views.setTextViewText(R.id.tv_details_temp, "N/A")
            views.setTextViewText(R.id.tv_details_voltage, "N/A")
            views.setTextViewText(R.id.tv_details_plugged, "N/A")
            views.setTextViewText(R.id.tv_details_technology, "N/A")
        }
        views.setOnClickPendingIntent(R.id.widget_details_container, getLaunchAppPendingIntent(context))
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

    private fun getStatusString(context: Context, status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.status_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.status_discharging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.status_full)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.status_not_charging)
            else -> context.getString(R.string.status_unknown)
        }
    }

    private fun getHealthString(context: Context, health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.health_over_voltage)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.health_cold)
            else -> context.getString(R.string.health_unknown)
        }
    }

    private fun getPluggedString(context: Context, plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Desconectado"
        }
    }
}
