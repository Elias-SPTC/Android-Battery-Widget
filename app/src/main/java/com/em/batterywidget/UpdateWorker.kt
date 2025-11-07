package com.em.batterywidget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Worker responsável por realizar o monitoramento periódico da bateria:
 * 1. Coletar o estado atual.
 * 2. Salvar no banco de dados.
 * 3. Limpar dados antigos.
 * 4. Atualizar o widget na tela.
 */
class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "UpdateWorker"

    override suspend fun doWork(): Result {
        val context = applicationContext
        val dataManager = BatteryData(context)
        val monitor = BatteryMonitor(context)
        val settingsManager = BatteryDataStoreManager(context)

        try {
            // 1. Coletar o estado atual da bateria
            val batteryInfo = monitor.getBatteryInfo()

            // 2. Salvar o novo registro no banco de dados
            if (batteryInfo.level >= 0) {
                dataManager.saveBatteryEntry(batteryInfo)
            }

            // 3. Limpar dados antigos
            // Pega o número de dias de retenção das configurações
            val retentionDays = settingsManager.dataRetentionDays.firstOrNull() ?: BatteryDataStoreManager.DEFAULT_DATA_RETENTION_DAYS

            // Converte os dias em milissegundos
            val retentionMillis = TimeUnit.DAYS.toMillis(retentionDays.toLong())
            val cutoffTimeMillis = System.currentTimeMillis() - retentionMillis

            dataManager.cleanupOldEntries(cutoffTimeMillis)

            // 4. Atualizar o(s) widget(s) na tela
            WidgetUpdater.updateAllWidgets(context)

            // Sucesso na execução
            return Result.success()

        } catch (e: Exception) {
            // Em caso de qualquer falha, loga e tenta novamente mais tarde
            android.util.Log.e(TAG, "Erro na execução do UpdateWorker", e)
            return Result.retry()
        }
    }

    /**
     * Factory para o Worker, permitindo injeção de dependências (útil para testes).
     * Embora não usemos DI completa, essa estrutura é a mais recomendada.
     */
    class Factory(private val context: Context) : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): CoroutineWorker? {
            return when (workerClassName) {
                UpdateWorker::class.java.name -> UpdateWorker(appContext, workerParameters)
                else -> null
            }
        }
    }
}