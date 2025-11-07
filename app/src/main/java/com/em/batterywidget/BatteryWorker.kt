package com.em.batterywidget

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker do WorkManager para agendar e executar o log periódico do estado da bateria.
 */
class BatteryWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "BatteryLogWork"
        private const val TAG = "BatteryWorker"

        /**
         * Agenda a tarefa periódica para registrar o estado da bateria.
         * Esta tarefa será executada a cada 15 minutos.
         *
         * @param context O contexto da aplicação.
         */
        fun schedulePeriodicWork(context: Context) {
            Log.d(TAG, "Agendando trabalho periódico para log de bateria.")

            // Restrições: O trabalho deve ser executado mesmo sem conexão de rede (não requer rede)
            val constraints = Constraints.Builder()
                // Nenhuma restrição de rede, pois não estamos buscando dados externos
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Cria a solicitação de trabalho periódico
            // Período mínimo de repetição é de 15 minutos
            val workRequest = PeriodicWorkRequestBuilder<BatteryWorker>(
                15, TimeUnit.MINUTES, // Intervalo de repetição
                5, TimeUnit.MINUTES // Intervalo de flexibilidade (opcional)
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME) // Etiqueta para identificação
                .build()

            // Envia a solicitação ao WorkManager.
            // ExistingPeriodicWorkPolicy.KEEP garante que se a tarefa já estiver agendada,
            // ela não será substituída, mantendo o agendamento original.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i(TAG, "Trabalho periódico agendado com sucesso.")
        }
    }

    /**
     * Este método é chamado pelo WorkManager para executar a tarefa.
     */
    override suspend fun doWork(): Result {
        return try {
            // Chamamos a função de log que obtém o estado da bateria e salva no Room
            BatteryMonitor.logBatteryState(applicationContext)

            Log.d(TAG, "Tarefa do Worker concluída com sucesso.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Falha na execução do BatteryWorker.", e)
            // Se falhar, tentamos novamente
            Result.retry()
        }
    }
}