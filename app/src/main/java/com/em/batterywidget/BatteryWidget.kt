package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.em.batterywidget.R // Assumindo R é o R gerado pelo Android Studio

/**
 * AppWidgetProvider para o widget de exibição do nível da bateria.
 * Responsável por receber eventos do sistema (onUpdate, onEnabled, etc.) e coordenar a renderização.
 */
class BatteryWidget : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // Instâncias do DataStore e Renderer inicializadas sob demanda
    private fun getDataStore(context: Context) = BatteryDataStoreManager(context)
    private fun getRenderer(context: Context) = BatteryRenderer(context)

    companion object {
        private const val TAG = "BatteryWidget"
    }

    /**
     * Chamado quando o widget é atualizado (periodicamente, sob demanda ou manualmente).
     *
     * @param context Contexto da aplicação.
     * @param appWidgetManager O gerenciador de widgets.
     * @param appWidgetIds IDs de todos os widgets a serem atualizados.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Inicializa o serviço de monitoramento se ainda não estiver rodando.
        // O monitoramento real deve ser iniciado aqui ou em onEnabled.
        startBatteryMonitorService(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Atualiza a UI de um widget específico.
     * Esta função é executada em uma corrotina para acessar o DataStore.
     */
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            try {
                // 1. Obter os dados atuais da bateria
                val dataStore = getDataStore(context)
                val batteryInfo = dataStore.batteryDataFlow.firstOrNull() ?: getDefaultBatteryInfo()

                // 2. Renderizar a UI
                val renderer = getRenderer(context)
                val views = renderer.render(batteryInfo)

                // 3. Configurar o PendingIntent de clique (abrir a Activity)
                val pendingIntent = createClickPendingIntent(context, appWidgetId)
                // Define o PendingIntent para ser chamado quando o widget for clicado
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                // 4. Aplicar o RemoteViews ao widget
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao atualizar o widget ID $appWidgetId", e)
                // Em caso de falha, tenta renderizar um estado de erro básico (opcional)
                val errorViews = RemoteViews(context.packageName, R.layout.widget_battery)
                errorViews.setTextViewText(R.id.text_battery_level, "ERRO")
                errorViews.setTextViewText(R.id.text_battery_status, "Dados indisponíveis")
                appWidgetManager.updateAppWidget(appWidgetId, errorViews)
            }
        }
    }

    /**
     * Cria um PendingIntent que abrirá a WidgetActivity.
     */
    private fun createClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WidgetActivity::class.java).apply {
            // Adicione extras se precisar passar informações específicas
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        // Define o PendingIntent com flags apropriadas
        return PendingIntent.getActivity(
            context,
            appWidgetId, // Usa o ID do widget como requestCode para exclusividade
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Retorna um objeto BatteryInfo com valores padrão em caso de falha de leitura do DataStore.
     */
    private fun getDefaultBatteryInfo(): BatteryInfo {
        return BatteryInfo(
            level = 0,
            status = BatteryManager.BATTERY_STATUS_UNKNOWN,
            health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
            plugged = 0,
            voltageMillivolts = 0,
            temperatureDeciCelsius = 0,
            technology = "N/A",
            timeRemaining = -1L
        )
    }

    /**
     * Chamado quando a primeira instância do widget é adicionada.
     * Usado para iniciar o serviço de monitoramento.
     */
    override fun onEnabled(context: Context) {
        Log.d(TAG, "onEnabled: Primeira instância do widget adicionada.")
        startBatteryMonitorService(context)
    }

    /**
     * Chamado quando a última instância do widget é removida.
     * Usado para parar o serviço de monitoramento, economizando recursos.
     */
    override fun onDisabled(context: Context) {
        Log.d(TAG, "onDisabled: Última instância do widget removida.")
        stopBatteryMonitorService(context)

        // Cancela todas as corrotinas pendentes
        job.cancel()
    }

    /**
     * Chamado quando uma instância específica do widget é excluída.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "onDeleted: Widget(s) excluído(s). IDs: ${appWidgetIds.joinToString()}")
    }

    /**
     * Inicia o serviço que monitora as alterações da bateria.
     * (Assumindo que existe uma classe BatteryMonitorService)
     */
    private fun startBatteryMonitorService(context: Context) {
        // Envia uma intent para iniciar o serviço (que deve ser configurado como foreground service)
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        try {
            // Em Android O (API 26) e superior, deve-se usar startForegroundService
            context.startForegroundService(serviceIntent)
            Log.d(TAG, "BatteryMonitorService iniciado via startForegroundService.")
        } catch (e: Exception) {
            // Em APIs mais antigas
            context.startService(serviceIntent)
            Log.d(TAG, "BatteryMonitorService iniciado via startService.")
        }
        // Usamos o DataStore para registrar que o serviço está rodando.
        scope.launch {
            getDataStore(context).setIsServiceRunning(true)
        }
    }

    /**
     * Para o serviço de monitoramento da bateria.
     * (Chamado apenas quando não há mais widgets ativos)
     */
    private fun stopBatteryMonitorService(context: Context) {
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        context.stopService(serviceIntent)
        Log.d(TAG, "BatteryMonitorService parado.")

        // Atualiza o DataStore para registrar que o serviço parou.
        scope.launch {
            getDataStore(context).setIsServiceRunning(false)
        }
    }
}