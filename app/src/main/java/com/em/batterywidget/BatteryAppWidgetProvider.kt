package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Provider principal para o widget de monitoramento da bateria.
 * Gerencia a atualização do widget em resposta a eventos (como o broadcast receiver)
 * e o ciclo de vida do widget.
 */
class BatteryAppWidgetProvider : AppWidgetProvider(), KoinComponent {

    // Injeção de dependência do Repositório via Koin
    private val repository: BatteryRepository by inject()

    // Escopo de Coroutine para operações assíncronas (usamos Dispatchers.IO para leitura do BD)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        private const val TAG = "BatteryWidgetProvider"
    }

    /**
     * Chamado quando o widget é adicionado pela primeira vez, e em intervalos definidos.
     * Também é chamado pelo Broadcast Receiver quando dados são atualizados.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Itera sobre todos os widgets ativos
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Chamado quando o nosso ACTION_UPDATE_WIDGET customizado é recebido.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context != null && intent?.action == BatteryBroadcastReceiver.ACTION_UPDATE_WIDGET) {
            Log.d(TAG, "Recebido ACTION_UPDATE_WIDGET. Forçando a atualização dos widgets.")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, BatteryAppWidgetProvider::class.java)
            )
            // Chama onUpdate para todos os widgets para buscar os novos dados
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    /**
     * Lógica real de atualização de um widget individual.
     * Deve ser executada em um Job assíncrono, pois envolve uma leitura de base de dados.
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Lança uma coroutine para buscar os dados de forma segura
        coroutineScope.launch {
            try {
                // Busca o log mais recente. Usamos .first() para obter o valor e fechar o Flow.
                val latestLog = repository.getLatestBatteryLog().first()

                // O widget só pode ser atualizado na thread principal (UI)
                CoroutineScope(Dispatchers.Main).launch {
                    val views = RemoteViews(context.packageName, R.layout.battery_widget_layout)

                    if (latestLog != null) {
                        // 1. Nível da Bateria
                        views.setTextViewText(R.id.text_battery_level, "${latestLog.level}%")

                        // 2. Status do Carregamento (Exibe se está a carregar ou completo/descarregar)
                        val statusText = getStatusString(context, latestLog.status, latestLog.plugged)
                        views.setTextViewText(R.id.text_charge_status, statusText)

                        // 3. Informações Adicionais (Temperatura e Saúde)
                        views.setTextViewText(
                            R.id.text_details,
                            context.getString(
                                R.string.widget_details_format,
                                latestLog.temperature,
                                getHealthString(context, latestLog.health)
                            )
                        )
                        views.setViewVisibility(R.id.layout_details, View.VISIBLE) // Mostra os detalhes
                        views.setViewVisibility(R.id.text_no_data, View.GONE) // Esconde a mensagem de erro

                        // Define a cor de fundo com base no nível (simplificado)
                        val levelColor = if (latestLog.level > 20) R.color.battery_level_normal else R.color.battery_level_low
                        views.setInt(R.id.layout_widget_root, "setBackgroundResource", levelColor)

                    } else {
                        // Caso não haja dados no BD
                        views.setTextViewText(R.id.text_no_data, context.getString(R.string.no_battery_data))
                        views.setViewVisibility(R.id.layout_details, View.GONE)
                        views.setViewVisibility(R.id.text_no_data, View.VISIBLE)
                        views.setTextViewText(R.id.text_battery_level, "N/D")
                        views.setInt(R.id.layout_widget_root, "setBackgroundResource", R.color.battery_level_low)
                    }

                    // Define a ação de clique (por exemplo, abrir a App principal)
                    val intent = Intent(context, /* Sua Activity Principal Aqui */)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.layout_widget_root, pendingIntent)

                    // Pede ao gestor do widget para efetuar a atualização
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar dados do log da bateria para o widget: ${e.message}", e)
            }
        }
    }

    /**
     * Mapeia o código de status da bateria para uma string descritiva.
     */
    private fun getStatusString(context: Context, status: Int, plugged: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> {
                when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.status_charging_usb)
                    BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.status_charging_ac)
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.status_charging_wireless)
                    else -> context.getString(R.string.status_charging)
                }
            }
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.status_discharging)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.status_not_charging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.status_full)
            else -> context.getString(R.string.status_unknown)
        }
    }

    /**
     * Mapeia o código de saúde da bateria para uma string descritiva.
     */
    private fun getHealthString(context: Context, health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.health_over_voltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.health_failure)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.health_cold)
            else -> context.getString(R.string.health_unknown)
        }
    }
}