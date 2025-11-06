package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews

/**
 * AppWidgetProvider para o widget de gráfico de bateria.
 */
class BatteryGraphWidget : AppWidgetProvider() {

    companion object {
        // Ação de Intent para atualizar o widget apenas para o gráfico (usado pelo MonitorService)
        const val ACTION_WIDGET_UPDATE_GRAPH_ONLY = "com.em.batterywidget.ACTION_WIDGET_UPDATE_GRAPH_ONLY"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        UpdateServiceUtils.init(context)
        UpdateServiceUtils.startBatteryMonitorService(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) return

        UpdateServiceUtils.init(context)

        // Verifica se é um pedido de atualização apenas do gráfico (enviado pelo MonitorService)
        if (intent.action == ACTION_WIDGET_UPDATE_GRAPH_ONLY) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            appWidgetIds?.forEach { appWidgetId ->
                // O argumento forceFullUpdate=false é opcional, mas otimiza a atualização
                updateAppWidget(context, appWidgetManager, appWidgetId, false)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        forceFullUpdate: Boolean
    ) {
        // Lógica para carregar os dados do histórico de bateria
        // Esta lógica está omissa e simulada com dados vazios
        val historyData = emptyList<Int>() // Simulação de dados vazios

        val batteryIntent = UpdateServiceUtils.getBatteryIntent(context)
        val batteryLevel = UpdateServiceUtils.getBatteryLevel(batteryIntent)
        val status = UpdateServiceUtils.getBatteryStatus(batteryIntent)

        val views = RemoteViews(context.packageName, R.layout.widget_battery_graph)

        // 1. Atualizar o Status/Último Nível
        val latestStatusText = if (batteryLevel > 0) {
            context.getString(R.string.graph_status_latest, batteryLevel, status.statusText)
        } else {
            context.getString(R.string.graph_status_empty)
        }
        views.setTextViewText(R.id.tv_graph_status_latest, latestStatusText)

        // 2. Desenhar o gráfico (simulação)
        val graphBitmap = if (historyData.isEmpty()) {
            drawEmptyGraph(context)
        } else {
            drawGraph(context, historyData)
        }

        // 3. Aplicar o Bitmap e o PendingIntent
        views.setImageViewBitmap(R.id.iv_graph_canvas, graphBitmap)

        // Configurar PendingIntent para abrir WidgetActivity ao clicar no gráfico (opcional)
        // ...

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // Funções simuladas para desenho (apenas devolvem um bitmap simples para evitar erros)
    private fun drawGraph(context: Context, data: List<Int>): Bitmap {
        val width = 500
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = Color.BLUE
        paint.strokeWidth = 5f

        // Desenha uma linha de exemplo
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), 0f, paint)

        return bitmap
    }

    private fun drawEmptyGraph(context: Context): Bitmap {
        val width = 500
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY) // Fundo cinzento claro

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.graph_no_data), width / 2f, height / 2f, textPaint)
        return bitmap
    }
}