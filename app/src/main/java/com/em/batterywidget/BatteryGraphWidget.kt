package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
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
            // CORRIGIDO: Usa o nome de método correto que existe no repositório.
            val history = repository.getHistory().first()
            val latestLog = repository.getLatestBatteryLog().first()

            val graphBitmap = drawGraphToBitmap(context, history)

            CoroutineScope(Dispatchers.Main).launch {
                val views = RemoteViews(context.packageName, R.layout.battery_graph_widget_layout)

                views.setImageViewBitmap(R.id.iv_battery_graph, graphBitmap)

                if (latestLog != null) {
                    val statusText = getStatusString(context, latestLog.status, latestLog.plugged)
                    views.setTextViewText(R.id.tv_graph_footer_info, context.getString(R.string.graph_status_latest, latestLog.level, statusText))
                } else {
                    views.setTextViewText(R.id.tv_graph_footer_info, context.getString(R.string.graph_no_data))
                }

                val intent = Intent(context, WidgetActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    action = "graph_widget_click_$appWidgetId"
                }
                val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                
                views.setOnClickPendingIntent(R.id.widget_graph_container, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun drawGraphToBitmap(context: Context, history: List<BatteryLog>): Bitmap {
        val width = 500
        val height = 250
        val graphView = BatteryGraphView(context).apply {
            setHistoryData(history)
        }
        graphView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        graphView.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        graphView.draw(canvas)
        return bitmap
    }

    private fun getStatusString(context: Context, status: Int, plugged: Int): String {
        val statusResource = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> {
                when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_USB -> R.string.status_charging_usb
                    BatteryManager.BATTERY_PLUGGED_AC -> R.string.status_charging_ac
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> R.string.status_charging_wireless
                    else -> R.string.status_charging
                }
            }
            BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.status_discharging
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.status_not_charging
            BatteryManager.BATTERY_STATUS_FULL -> R.string.status_full
            else -> R.string.status_unknown
        }
        return context.getString(statusResource)
    }
}
