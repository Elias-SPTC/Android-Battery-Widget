package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.room.Room
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * AppWidgetProvider para o widget de gráfico de histórico da bateria.
 * Carrega dados do Room e usa BatteryRenderer para desenhar o gráfico em um Bitmap.
 */
class BatteryGraphWidgetProvider : AppWidgetProvider() {

    private val TAG = "BatteryGraphWidgetProvider"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: BatteryDatabase // Inicializado em onUpdate/onEnabled
    private lateinit var renderer: BatteryRenderer

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        initializeComponents(context)
        Log.d(TAG, "onUpdate chamado para Widget de Gráfico: ${appWidgetIds.size} widgets.")

        // Garante que o serviço de monitoramento esteja ativo
        AppWidgetUtils.startMonitorService(context)

        for (appWidgetId in appWidgetIds) {
            updateGraphWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        initializeComponents(context)
        Log.i(TAG, "Widget de Gráfico Ativado.")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        scope.cancel()
        // Não paramos o serviço aqui, pois o BatteryWidgetProvider pode ainda estar ativo.
        // O MonitorService gerencia sua própria parada quando não há mais widgets.
        Log.i(TAG, "Widget de Gráfico Desativado.")
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Redesenha o gráfico se as dimensões mudarem
        updateGraphWidget(context, appWidgetManager, appWidgetId)
    }

    /**
     * Inicializa o banco de dados e o renderizador.
     */
    private fun initializeComponents(context: Context) {
        if (!::db.isInitialized) {
            db = Room.databaseBuilder(
                context.applicationContext,
                BatteryDatabase::class.java,
                BatteryDatabase.DATABASE_NAME
            ).build()
            renderer = BatteryRenderer(context)
        }
    }

    /**
     * Lógica principal de atualização: busca dados, desenha o Bitmap e atualiza o RemoteViews.
     */
    private fun updateGraphWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_battery_graph)

                // 1. Obter as dimensões atuais do widget
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                // Largura e altura mínima em DP
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                // Converter DP para PX
                val density = context.resources.displayMetrics.density
                val widthPx = (minWidth * density).roundToInt()
                val heightPx = (minHeight * density).roundToInt()

                // Dimensões do ImageView do gráfico dentro do padding de 8dp
                val graphMargin = (8 * density).roundToInt()
                // A altura do gráfico é aproximadamente a altura total menos o padding top/bottom (8*2)
                // e menos a altura do título e rodapé (aprox. 30dp)
                val graphHeightEstimate = heightPx - (2 * graphMargin) - (30 * density).roundToInt()

                // 2. Buscar Dados do Histórico (últimas 24h)
                val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                val history = db.batteryDao().getEntriesSince(yesterday)

                // 3. Desenhar o Gráfico (usando a largura total do widget e a altura estimada)
                val graphBitmap = renderer.drawGraph(
                    history = history,
                    width = widthPx,
                    height = graphHeightEstimate // Usamos a altura estimada para o desenho
                )

                // 4. Preencher a RemoteViews

                // Exibe o Bitmap desenhado
                views.setImageViewBitmap(R.id.image_battery_graph, graphBitmap)

                // Texto do Nível Atual (opcional, para conveniência)
                val latestInfo = BatteryManager.getBatteryInfo(context)
                views.setTextViewText(R.id.text_current_level_summary, "${latestInfo.level}%")

                // Carimbo de Data/Hora (rodapé)
                val timeFormat = SimpleDateFormat("dd/MM/yy - HH:mm:ss", Locale.getDefault())
                val lastTimestamp = history.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                views.setTextViewText(R.id.text_graph_timestamp, "Última leitura: ${timeFormat.format(Date(lastTimestamp))}")

                // Ação de clique para abrir a Atividade (aponta para o WidgetActivity)
                val intent = Intent(context, WidgetActivity::class.java)
                val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(context, appWidgetId + 1, intent, pendingIntentFlags)
                views.setOnClickPendingIntent(R.id.widget_graph_container, pendingIntent)

                // 5. Atualizar o widget
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao atualizar widget de gráfico $appWidgetId: ${e.message}", e)
            }
        }
    }
}