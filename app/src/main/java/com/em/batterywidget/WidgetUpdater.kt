package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.BatteryManager
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.em.batterywidget.WidgetType.TYPE_DETAILS_TABLE
import com.em.batterywidget.WidgetType.TYPE_GRAPH
import com.em.batterywidget.WidgetType.TYPE_ICON_DETAIL
import com.em.batterywidget.WidgetType.TYPE_TEXT_ONLY
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object WidgetUpdater : KoinComponent {

    private val repository: BatteryRepository by inject()

    suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Lê o tipo de widget que foi salvo no DataStore pela WidgetActivity.
        val widgetType = repository.widgetPreferencesFlow.first().widgetType[appWidgetId] ?: TYPE_DETAILS_TABLE
        val latestLog = repository.getLatestBatteryLog().first()

        val views = when (widgetType) {
            TYPE_GRAPH -> {
                val logs = repository.getHistory().first()
                createGraphView(context, logs)
            }
            TYPE_ICON_DETAIL -> createIconDetailView(context, latestLog)
            TYPE_TEXT_ONLY -> createTextOnlyView(context, latestLog)
            else -> createDetailsTableView(context, latestLog)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createGraphView(context: Context, logs: List<BatteryLog>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_graph_history)
        if (logs.size > 1) {
            views.setViewVisibility(R.id.graph_image_view, View.VISIBLE)
            val chartBitmap = createChartBitmap(context, logs)
            views.setImageViewBitmap(R.id.graph_image_view, chartBitmap)
            views.setTextViewText(R.id.graph_title, context.getString(R.string.graph_widget_title))
        } else {
            views.setViewVisibility(R.id.graph_image_view, View.GONE)
            views.setTextViewText(R.id.graph_title, context.getString(R.string.no_data_available))
        }
        views.setOnClickPendingIntent(R.id.widget_graph_root, getLaunchAppPendingIntent(context))
        return views
    }

    private fun createChartBitmap(context: Context, logs: List<BatteryLog>): Bitmap {
        val width = 500
        val height = 300
        val lineChart = LineChart(context).apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, measuredWidth, measuredHeight)
            description.isEnabled = false
            legend.isEnabled = false
            isDragEnabled = false
            setScaleEnabled(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.setDrawLabels(false)
            axisLeft.textColor = Color.WHITE
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisLeft.setLabelCount(5, true)
            axisRight.isEnabled = false
        }
        val entries = logs.reversed().mapIndexed { index, log -> Entry(index.toFloat(), log.level.toFloat()) }
        val dataSet = LineDataSet(entries, "Histórico").apply {
            color = Color.GREEN
            valueTextColor = Color.TRANSPARENT
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2.0f
            setDrawFilled(true)
            fillColor = Color.GREEN
            fillAlpha = 50
        }
        lineChart.data = LineData(dataSet)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        lineChart.draw(canvas)
        return bitmap
    }

    private fun createIconDetailView(context: Context, log: BatteryLog?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_icon_detail)
        if (log != null) {
            views.setTextViewText(R.id.battery_level_text, "${log.level}%")
            views.setProgressBar(R.id.battery_progress_bar_vertical, 100, log.level, false)
            views.setViewVisibility(R.id.charging_bolt_icon_vertical, if (log.status == BatteryManager.BATTERY_STATUS_CHARGING) View.VISIBLE else View.GONE)
        } else {
            views.setTextViewText(R.id.battery_level_text, "N/A")
        }
        views.setOnClickPendingIntent(R.id.widget_root_layout, getLaunchAppPendingIntent(context))
        return views
    }

    private fun createTextOnlyView(context: Context, log: BatteryLog?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_text_only)
        if (log != null) {
            views.setTextViewText(R.id.text_level_only, "${log.level}%")
            views.setProgressBar(R.id.battery_progress_bar, 100, log.level, false)
            views.setViewVisibility(R.id.charging_bolt_icon, if (log.status == BatteryManager.BATTERY_STATUS_CHARGING) View.VISIBLE else View.GONE)
        } else {
            views.setTextViewText(R.id.text_level_only, "N/A")
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
        }
        views.setOnClickPendingIntent(R.id.widget_details_container, getLaunchAppPendingIntent(context))
        return views
    }

    private fun getLaunchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
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
