package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import com.em.batterywidget.BatteryWidgetUtils.getBatteryIntent
import com.em.batterywidget.BatteryWidgetUtils.getBatteryLevel
import com.em.batterywidget.BatteryWidgetUtils.getBatteryStatus
import com.em.batterywidget.BatteryWidgetUtils.getStatusStringId
import com.em.batterywidget.BatteryWidgetUtils.init
import com.em.batterywidget.MonitorService.Companion.startBatteryMonitorService
import com.em.batterywidget.WidgetActivity.Companion.showGraphActivity

/**
 * AppWidgetProvider para o widget de gráfico de bateria.
 * Corrigido: Usando BatteryWidgetUtils em vez de UpdateServiceUtils.
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
        // Usa BatteryWidgetUtils.init
        init(context)
        startBatteryMonitorService(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) return

        // Usa BatteryWidgetUtils.init
        init(context)

        // Verifica se é um pedido de atualização apenas do gráfico (enviado pelo MonitorService)
        if (intent.action == ACTION_WIDGET_UPDATE_GRAPH_ONLY) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            appWidgetIds?.forEach { appWidgetId ->
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
        // Acesso ao Database (Singleton)
        val database = Database.getInstance(context)
        // Lógica real: obter o histórico dos últimos 100 registros (para fins de renderização)
        val historyData = database.getEntries()?.use { cursor ->
            val data = mutableListOf<Database.HistoryData>()
            // Não é a forma mais eficiente, mas converte o Cursor para uma lista de dados para simulação de gráfico
            while (cursor.moveToNext()) {
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Database.TIMESTAMP))
                val level = cursor.getInt(cursor.getColumnIndexOrThrow(Database.LEVEL))
                val voltage = cursor.getInt(cursor.getColumnIndexOrThrow(Database.VOLTAGE))
                // Apenas nível é usado para desenhar o gráfico, o restante é necessário para HistoryData
                data.add(Database.HistoryData(
                    id = 0, timestamp = timestamp, level = level, temperature = 0, voltage = voltage, plugged = 0, status = 0
                ))
            }
            data
        } ?: emptyList()


        val batteryIntent = getBatteryIntent(context)
        val batteryLevel = getBatteryLevel(batteryIntent)
        val status = getBatteryStatus(batteryIntent)
        val statusTextId = getStatusStringId(status)

        // Assumindo R.layout.widget_battery_graph
        val views = RemoteViews(context.packageName, R.layout.widget_battery_graph)

        // 1. Atualizar o Status/Último Nível
        val latestStatusText = if (batteryLevel > 0) {
            // Corrigido: Uso de getStatusStringId
            context.getString(R.string.graph_status_latest, batteryLevel, context.getString(statusTextId))
        } else {
            context.getString(R.string.graph_status_empty)
        }
        views.setTextViewText(R.id.tv_graph_status_latest, latestStatusText)

        // 2. Desenhar o gráfico
        val graphDataPoints = historyData.map { it.level } // Extrai apenas o nível para o gráfico
        val graphBitmap = if (graphDataPoints.isEmpty()) {
            drawEmptyGraph(context)
        } else {
            drawGraph(context, graphDataPoints)
        }

        // 3. Aplicar o Bitmap e o PendingIntent
        views.setImageViewBitmap(R.id.iv_graph_canvas, graphBitmap)

        // Configurar PendingIntent para abrir WidgetActivity ao clicar no gráfico
        val pendingIntent = showGraphActivity(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_graph_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // Funções simuladas para desenho (apenas devolvem um bitmap simples para evitar erros)
    private fun drawGraph(context: Context, data: List<Int>): Bitmap {
        val width = 500
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        canvas.drawColor(Color.parseColor("#F0F0F0")) // Fundo do gráfico

        // Desenhar a linha do gráfico
        paint.color = Color.parseColor("#3F51B5") // Cor principal (azul)
        paint.strokeWidth = 5f
        paint.isAntiAlias = true

        val maxLevel = 100f
        val numPoints = data.size
        if (numPoints > 1) {
            val stepX = width.toFloat() / (numPoints - 1)
            for (i in 0 until numPoints - 1) {
                val x1 = i * stepX
                val y1 = height - (data[i] / maxLevel) * height
                val x2 = (i + 1) * stepX
                val y2 = height - (data[i + 1] / maxLevel) * height

                canvas.drawLine(x1, y1, x2, y2, paint)
            }
        } else if (numPoints == 1) {
            // Desenhar um ponto se houver apenas um registro
            val x = width / 2f
            val y = height - (data[0] / maxLevel) * height
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, 8f, paint)
        }

        return bitmap
    }

    private fun drawEmptyGraph(context: Context): Bitmap {
        val width = 500
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F8F8F8")) // Fundo

        // Desenha uma moldura
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, width - 1f, height - 1f, borderPaint)

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.graph_no_data), width / 2f, height / 2f, textPaint)
        return bitmap
    }
}