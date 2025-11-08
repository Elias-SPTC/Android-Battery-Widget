package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.BatteryManager
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first // <-- IMPORT ADICIONADO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BatteryGraphWidget : AppWidgetProvider(), KoinComponent {

    private val repository: BatteryRepository by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        const val ACTION_WIDGET_UPDATE_GRAPH_ONLY = "com.em.batterywidget.ACTION_WIDGET_UPDATE_GRAPH_ONLY"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        coroutineScope.launch {
            val history = repository.getHistory().first().take(20)
            val latestLog = repository.getLatestBatteryLog().first()

            CoroutineScope(Dispatchers.Main).launch {
                val views = RemoteViews(context.packageName, R.layout.battery_graph_widget_layout)

                if (latestLog != null) {
                    views.setTextViewText(R.id.tv_current_level, "${latestLog.level}%")
                    views.setTextViewText(R.id.tv_current_status, getStatusString(context, latestLog.status))
                    views.setTextViewText(R.id.tv_current_health, "Saúde: ${getHealthString(context, latestLog.health)}")
                } else {
                    views.setTextViewText(R.id.tv_current_level, "N/A")
                    views.setTextViewText(R.id.tv_current_status, "Sem dados")
                }

                val barIds = getBarIds()
                for (i in 0 until barIds.size) {
                    val barId = barIds[i]
                    if (i < history.size) {
                        val log = history[i]
                        val barColor = if (log.level <= 20) Color.RED else Color.parseColor("#4CAF50")
                        views.setInt(barId, "setBackgroundColor", barColor)
                    } else {
                        views.setInt(barId, "setBackgroundColor", Color.TRANSPARENT)
                    }
                }

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_graph_container, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun getBarIds(): IntArray {
        return intArrayOf(
            R.id.bar_1, R.id.bar_2, R.id.bar_3, R.id.bar_4, R.id.bar_5, R.id.bar_6, R.id.bar_7, R.id.bar_8, R.id.bar_9, R.id.bar_10,
            R.id.bar_11, R.id.bar_12, R.id.bar_13, R.id.bar_14, R.id.bar_15, R.id.bar_16, R.id.bar_17, R.id.bar_18, R.id.bar_19, R.id.bar_20
        )
    }
    
    private fun getStatusString(context: Context, status: Int): String { 
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Completa"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Não Carrega"
            else -> "Desconhecido"
        }
     }
    private fun getHealthString(context: Context, health: Int): String { 
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Morta"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobretensão"
            BatteryManager.BATTERY_HEALTH_COLD -> "Fria"
            else -> "Desconhecida"
        }
     }
}
