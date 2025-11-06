package com.em.batterywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.em.batterywidget.UpdateServiceUtils.getBatteryIntent
import com.em.batterywidget.UpdateServiceUtils.getBatteryLevel
import com.em.batterywidget.UpdateServiceUtils.getBatteryStatus

/**
 * AppWidgetProvider para o widget de bateria.
 * Responsável por receber eventos de broadcast do widget e atualizar a UI.
 */
class BatteryWidget : AppWidgetProvider() {

    // Ação de Intent para forçar a atualização do widget
    companion object {
        const val ACTION_REFRESH_WIDGET = "com.em.batterywidget.ACTION_REFRESH_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Inicializar o utilitário (necessário para o contexto do AppWidgetProvider)
        UpdateServiceUtils.init(context)

        // Iniciar o serviço de monitorização da bateria se ainda não estiver em execução
        UpdateServiceUtils.startBatteryMonitorService(context)

        // Atualizar cada instância do widget
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) return

        // Inicializar o utilitário
        UpdateServiceUtils.init(context)

        if (intent.action == ACTION_REFRESH_WIDGET) {
            // Se a ação for um pedido de atualização manual,
            // forçamos o AppWidgetManager a atualizar todos os widgets
            val componentName = ComponentName(context, BatteryWidget::class.java)
            val appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(componentName)
            onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds)
        }
    }

    // Chamado quando um widget é eliminado do ecrã principal
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        // Se todos os widgets forem removidos, o serviço de monitorização poderá ser parado
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, BatteryWidget::class.java)

        if (appWidgetManager.getAppWidgetIds(componentName).isEmpty()) {
            // O código para parar o serviço deve ser adicionado aqui, se implementado.
        }
    }

    // Função principal para construir e enviar a atualização do widget
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 1. Obter os dados atuais da bateria
        val batteryIntent = getBatteryIntent(context)
        val batteryLevel = getBatteryLevel(batteryIntent)
        val status = getBatteryStatus(batteryIntent)
        val isCharging = status.isCharging

        // 2. Criar a vista remota (RemoteViews)
        val views = RemoteViews(context.packageName, R.layout.widget_battery)

        // 3. Atualizar elementos da UI

        // Nível da Bateria
        views.setTextViewText(
            R.id.tv_battery_level,
            context.getString(R.string.battery_level_format, batteryLevel) // Assumindo R.string.battery_level_format existe
        )

        // Indicador de Carregamento/Ícone de Bateria (usando a cor para indicar o estado)
        views.setImageViewResource(
            R.id.iv_charging_indicator,
            if (isCharging) R.drawable.ic_charging_indicator // Assumindo ic_charging_indicator existe
            else R.drawable.ic_battery_default // Assumindo ic_battery_default existe
        )

        // Alterar a cor de fundo do indicador (opcional, pode depender do seu layout XML)
        val backgroundColor = if (batteryLevel <= 20 && !isCharging) {
            context.getColor(R.color.graph_low_battery_color) // Assumindo R.color.graph_low_battery_color existe
        } else {
            context.getColor(android.R.color.transparent)
        }
        views.setInt(
            R.id.iv_charging_indicator,
            "setBackgroundColor",
            backgroundColor
        )


        // Barra de Progresso
        views.setProgressBar(
            R.id.progress_bar,
            100,
            batteryLevel,
            false
        )

        // 4. Configurar Intent para abrir a WidgetActivity (Detalhes)
        val detailIntent = Intent(context, WidgetActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // Adicionar flags de segurança e evitar conflitos com a pilha de tarefas
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val detailPendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                appWidgetId,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                appWidgetId,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Definir ação de clique no layout principal para abrir a atividade de detalhes
        views.setOnClickPendingIntent(R.id.widget_container_layout, detailPendingIntent)

        // 5. Configurar Intent para Atualização Manual (Botão)
        val refreshIntent = Intent(context, BatteryWidget::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        // Configurar ação de clique no botão de atualização
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

        // 6. Finalmente, instruir o AppWidgetManager para executar a atualização
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}