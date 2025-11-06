package com.em.batterywidget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

/**
 * Worker periódico para forçar a atualização dos dados do widget
 * e agendar a coleta de dados pelo MonitorService.
 */
class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d("UpdateWorker", "Starting periodic update work...")

        // Inicializa utilitários
        UpdateServiceUtils.init(applicationContext)

        // 1. Inicia o serviço de monitorização, que irá recolher dados
        // e, em seguida, notificar todos os widgets.
        // O MonitorService é o principal responsável por garantir que o histórico
        // é gravado e os widgets são atualizados.
        UpdateServiceUtils.startBatteryMonitorService(applicationContext)

        // 2. Atualiza os widgets imediatamente após iniciar o serviço
        // (Isso também será feito dentro do MonitorService, mas forçar aqui garante a atualização rápida).
        UpdateServiceUtils.updateAllWidgets(applicationContext)
        UpdateServiceUtils.updateGraphWidgets(applicationContext)

        Log.d("UpdateWorker", "Periodic update work finished.")
        return Result.success()
    }
}