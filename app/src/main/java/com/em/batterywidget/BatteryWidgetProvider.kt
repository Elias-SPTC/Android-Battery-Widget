package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.em.batterywidget.R
import com.em.batterywidget.util.BatteryWidgetUtils
import com.em.batterywidget.util.SettingsUtils
import com.em.batterywidget.services.MonitorService
import java.util.*

class BatteryAppWidgetProvider : AppWidgetProvider() {

    // Ação para forçar a atualização manual
    companion object {
        private const val ACTION_UPDATE_CLICK = "com.em.batterywidget.UPDATE_CLICK"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Inicia o serviço de monitoramento se o monitoramento estiver ativado
        if (SettingsUtils.isMonitoringEnabled(context)) {
            MonitorService.startMonitoring(context)
        }

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_UPDATE_CLICK) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                BatteryAppWidgetProvider::class.java.getComponentName()
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun getPendingSelfIntent(context: Context): PendingIntent {
        val intent = Intent(context, javaClass).apply {
            action = ACTION_UPDATE_CLICK
        }
        // FIX: Usando FLAG_UPDATE_CURRENT e ajustando para o novo PendingIntent.
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val info = BatteryInfo.create(context)
        val prefs = SettingsUtils.loadPreferences(context, appWidgetId)

        // Configura o clique no botão de atualização
        views.setOnClickPendingIntent(R.id.refresh_button, getPendingSelfIntent(context))

        // Verifica se há dados da bateria disponíveis
        if (info.level != -1) {
            // 1. Atualizar Nível
            views.setTextViewText(R.id.text_level, "${info.level}%")

            // 2. Atualizar Detalhes
            val detailsText = BatteryWidgetUtils.getDetailsText(context, info, prefs)
            views.setTextViewText(R.id.widget_details, detailsText)

            // 3. Atualizar Ícone de Status (Carregamento/Descarregamento)
            val statusIconResId = BatteryWidgetUtils.getStatusIcon(info.status, info.plugged)
            // FIX: Removendo setImageViewResource(R.id.icon_status, statusIconResId) se não existir
            // Assumindo que o status do carregamento (o relâmpago) está em R.id.icon_status_charge
            views.setImageViewResource(R.id.icon_status_charge, statusIconResId)

            // Lógica de visibilidade - Assumindo que estes IDs existem e são importantes:
            // FIX: Removendo referências a IDs não resolvidas (text_charge_status, layout_details)
            views.setViewVisibility(R.id.text_level, View.VISIBLE)
            views.setViewVisibility(R.id.widget_details, if (detailsText.isNotEmpty()) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.text_no_data, View.GONE)

        } else {
            // Se os dados não estiverem disponíveis
            views.setTextViewText(R.id.text_no_data, context.getString(R.string.no_battery_data))
            views.setViewVisibility(R.id.text_no_level, View.GONE) // Assumindo R.id.text_level é para nível
            // FIX: Removendo referências a IDs não resolvidas (layout_details)
            views.setViewVisibility(R.id.text_no_data, View.VISIBLE)
        }

        // 4. Configurar Aparência Geral (Transparência/Cores) - Se necessário

        // Aplica as mudanças ao widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}