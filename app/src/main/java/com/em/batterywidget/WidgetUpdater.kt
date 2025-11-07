package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Utilitário estático para gerenciar a persistência do tipo de widget e a lógica
 * de atualização dos diferentes layouts de widget.
 */
object WidgetUpdater {
    private const val TAG = "WidgetUpdater"

    // Chave para armazenar o tipo de layout do widget no DataStore, usando o ID do widget como sufixo.
    private fun widgetTypeKey(appWidgetId: Int) = intPreferencesKey("widget_type_$appWidgetId")

    // Nome do DataStore para as preferências de widget.
    private const val WIDGET_PREFS_NAME = "widget_preferences"

    // Inicialização do DataStore, necessário para persistência de dados no Android moderno.
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = WIDGET_PREFS_NAME)

    /**
     * Salva o tipo de layout configurado para um widget específico.
     * Deve ser chamado durante o processo de configuração (WidgetActivity).
     */
    fun saveWidgetType(context: Context, appWidgetId: Int, type: Int) = runBlocking {
        Log.d(TAG, "Salvando tipo $type para widget $appWidgetId")
        context.dataStore.edit { preferences ->
            preferences[widgetTypeKey(appWidgetId)] = type
        }
    }

    /**
     * Carrega o tipo de layout para um widget específico.
     */
    fun loadWidgetType(context: Context, appWidgetId: Int): Int? = runBlocking {
        // Assume-se que BatteryWidgetProvider.LAYOUT_ICON_DETAIL é o padrão (1)
        val key = widgetTypeKey(appWidgetId)
        return@runBlocking context.dataStore.data.map { preferences ->
            preferences[key]
        }.first()
    }

    /**
     * Exclui o tipo de layout (e a persistência) para um widget que foi removido.
     */
    fun deleteWidgetType(context: Context, appWidgetId: Int) = runBlocking {
        Log.d(TAG, "Excluindo tipo para widget $appWidgetId")
        context.dataStore.edit { preferences ->
            preferences.remove(widgetTypeKey(appWidgetId))
        }
    }

    // --- Lógica de Atualização de Layouts ---

    /**
     * Atualiza o widget com o layout de Ícone e Detalhe (widget_battery.xml).
     */
    fun updateIconDetailWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val batteryInfo = getLatestBatteryInfo(context)
        val views = RemoteViews(context.packageName, R.layout.widget_battery)

        // 1. Configurar o estado da bateria (ícone, texto, cor)
        views.setTextViewText(R.id.text_battery_level, "${batteryInfo.level}%")

        val statusText = getBatteryStatusText(context, batteryInfo)
        views.setTextViewText(R.id.text_battery_status, statusText)

        // 2. Renderização do ícone (simulando a mudança do ícone com o nível e status)
        val batteryIconRes = BatteryRenderer.getBatteryIcon(batteryInfo.level, batteryInfo.isCharging)
        views.setImageViewResource(R.id.battery_icon, batteryIconRes)
        views.setTextColor(
            R.id.text_battery_level,
            BatteryRenderer.getBatteryColor(batteryInfo.level, batteryInfo.isCharging)
        )

        // 3. Configurar a ação de clique (por exemplo, abrir a MainActivity)
        views.setOnClickPendingIntent(
            R.id.widget_container,
            BatteryWidgetUtils.getLaunchAppPendingIntent(context)
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * Atualiza o widget com o layout de Gráfico e Histórico (widget_battery_graph.xml).
     *
     * Este é o layout mais complexo, pois o gráfico precisa ser desenhado em um Bitmap
     * e depois definido na ImageView do RemoteViews.
     */
    fun updateGraphHistoryWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val batteryInfo = getLatestBatteryInfo(context)
        val views = RemoteViews(context.packageName, R.layout.widget_battery_graph)

        views.setTextViewText(R.id.text_graph_level, "${batteryInfo.level}%")
        views.setTextViewText(
            R.id.text_graph_status,
            getBatteryStatusText(context, batteryInfo)
        )

        // 1. Simulação do Desenho do Gráfico
        // Esta é uma parte CRÍTICA: RemoteViews não suporta Canvas ou desenho complexo.
        // Precisamos:
        // a) Obter os dados históricos (do BatteryDatabase/DataStore).
        // b) Desenhar o gráfico em um Bitmap usando a classe BatteryRenderer/GraphRenderer.
        // c) Converter o Bitmap em um objeto Bundle/Image.

        // Por enquanto, apenas atualizamos o texto para indicar que a lógica está aqui
        views.setTextViewText(R.id.text_graph_placeholder, "Gráfico Pendente: ${Date().time}")
        views.setTextColor(R.id.text_graph_level, Color.WHITE)

        // TODO: Chamar GraphRenderer.renderGraphBitmap(context, dataHistorico)
        // O bitmap resultante seria definido assim:
        // views.setImageViewBitmap(R.id.image_graph, bitmap)

        views.setOnClickPendingIntent(
            R.id.widget_graph_container,
            BatteryWidgetUtils.getLaunchAppPendingIntent(context)
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * Atualiza o widget com o layout Minimalista de Texto (widget_battery_text.xml).
     */
    fun updateTextOnlyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val batteryInfo = getLatestBatteryInfo(context)
        val views = RemoteViews(context.packageName, R.layout.widget_battery_text)

        // Configura o texto principal
        views.setTextViewText(R.id.text_level_only, "${batteryInfo.level}%")
        views.setTextColor(
            R.id.text_level_only,
            BatteryRenderer.getBatteryColor(batteryInfo.level, batteryInfo.isCharging)
        )

        // Configura o texto de status (se houver espaço e se o widget for grande o suficiente)
        val statusText = getBatteryStatusText(context, batteryInfo)
        views.setTextViewText(R.id.text_status_only, statusText)

        views.setOnClickPendingIntent(
            R.id.widget_text_container,
            BatteryWidgetUtils.getLaunchAppPendingIntent(context)
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // --- Funções de Suporte ---

    /**
     * Obtém as informações mais recentes da bateria (simuladas por enquanto).
     * Idealmente, isso chamaria BatteryMonitor para obter os dados reais.
     */
    private fun getLatestBatteryInfo(context: Context): BatteryInfo {
        // TODO: Substituir pela chamada real ao BatteryMonitor (que lê o Intent ACTION_BATTERY_CHANGED)
        // Por enquanto, retorna dados simulados.
        // Exemplo: Simular 85% e carregando
        return BatteryInfo(
            level = 85,
            isCharging = true,
            status = BatteryInfo.Status.CHARGING
        )
    }

    /**
     * Retorna uma string de status formatada.
     */
    private fun getBatteryStatusText(context: Context, info: BatteryInfo): String {
        return when (info.status) {
            BatteryInfo.Status.CHARGING -> if (info.level == 100) "Totalmente Carregado" else "Carregando"
            BatteryInfo.Status.DISCHARGING -> "Descarga"
            BatteryInfo.Status.FULL -> "Totalmente Carregado"
            BatteryInfo.Status.NOT_CHARGING -> "Não Carregando"
            BatteryInfo.Status.UNKNOWN -> "Desconhecido"
        }
    }
}