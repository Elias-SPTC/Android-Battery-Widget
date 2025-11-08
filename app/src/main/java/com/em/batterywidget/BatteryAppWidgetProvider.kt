package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Ponto de entrada ÚNICO para todos os widgets.
 * Na nova arquitetura, sua única responsabilidade é receber eventos do sistema (como onUpdate)
 * e delegar o trabalho pesado para o BatteryWorker, acionando-o sob demanda.
 */
class BatteryAppWidgetProvider : AppWidgetProvider() {

    /**
     * Chamado pelo sistema quando é necessária uma atualização do widget (ex: na criação inicial).
     * Ele simplesmente cria e enfileira uma tarefa única para o BatteryWorker fazer o trabalho.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Cria uma tarefa para ser executada imediatamente.
        val workRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()
        // Enfileira a tarefa.
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
