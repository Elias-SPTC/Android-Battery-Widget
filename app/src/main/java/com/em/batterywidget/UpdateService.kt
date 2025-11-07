package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService // CORRIGIDO: Substitui IntentService por JobIntentService

/**
 * JobIntentService para realizar atualizações de widget em segundo plano.
 * Isso garante que o trabalho de desenho do gráfico e consulta ao DB seja feito fora da UI thread.
 *
 * Resolve as referências não resolvidas a ACTION_WIDGET_* e enqueueWork.
 */
class UpdateService : JobIntentService() {

    private val TAG = "UpdateService"

    companion object {
        // ID de Trabalho para JobIntentService (constante necessária)
        private const val JOB_ID = 1000

        // Ações de Intenção (constantes necessárias para o BatteryMonitor e BatteryWidget)
        const val ACTION_WIDGET_UPDATE_ALL = "com.em.batterywidget.ACTION_WIDGET_UPDATE_ALL"
        const val ACTION_WIDGET_UPDATE_SINGLE = "com.em.batterywidget.ACTION_WIDGET_UPDATE_SINGLE"

        /**
         * Método auxiliar para enfileirar o trabalho para este serviço.
         * Resolve o erro 'Unresolved reference: enqueueWork'.
         */
        fun enqueueWork(context: Context, intent: Intent) {
            JobIntentService.enqueueWork(
                context,
                UpdateService::class.java,
                JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Lidando com a intenção: $action")

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, BatteryWidget::class.java)

        when (action) {
            ACTION_WIDGET_UPDATE_ALL -> {
                // Atualiza todas as instâncias do widget
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                // Usamos o BatteryGraphWidget aqui apenas se você tiver um widget de gráfico separado.
                // Se o seu BatteryWidget for o único, chame ele.
                BatteryWidgetUtils.updateWidgets(this, appWidgetManager, appWidgetIds)
            }
            ACTION_WIDGET_UPDATE_SINGLE -> {
                // Atualiza apenas uma instância específica
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    BatteryWidgetUtils.updateWidgets(this, appWidgetManager, intArrayOf(appWidgetId))
                }
            }
            else -> {
                Log.w(TAG, "Ação desconhecida recebida: $action")
            }
        }

        // Limpa dados antigos do banco de dados a cada atualização
        // Assume que Database.kt tem o método cleanupOldData()
        Database.getInstance(this).cleanupOldData()
    }
}