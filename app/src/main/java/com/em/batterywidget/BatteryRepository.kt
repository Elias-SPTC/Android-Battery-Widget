// app/src/main/java/com/em/batterywidget/BatteryRepository.kt
package com.em.batterywidget

import kotlinx.coroutines.flow.Flow

/**
 * Repositório que gerencia todas as fontes de dados para a aplicação.
 * Ele abstrai o acesso ao banco de dados (através do DAO) e ao DataStore.
 */
class BatteryRepository(
    private val batteryLogDao: BatteryLogDao,
    private val dataStoreManager: BatteryDataStoreManager
) {

    /**
     * Adiciona um novo log de bateria ao banco de dados.
     */
    suspend fun addBatteryLog(log: BatteryLog) {
        batteryLogDao.addBatteryLog(log)
    }

    /**
     * Retorna o log de bateria mais recente, observável através de um Flow.
     */
    fun getLatestBatteryLog(): Flow<BatteryLog?> = batteryLogDao.getLatestLog()

    /**
     * Retorna os 100 logs mais recentes para o gráfico de histórico.
     */
    fun getHistory(): Flow<List<BatteryLog>> = batteryLogDao.getHistory()

    /**
     * Retorna todos os logs de bateria, para a tela de histórico completo.
     */
    fun getAllLogs(): Flow<List<BatteryLog>> = batteryLogDao.getAllLogs()

    /**
     * Apaga todos os registros da tabela de logs.
     */
    suspend fun clearAllLogs() {
        batteryLogDao.clearAll()
    }

    // --- Operações do DataStore ---

    val widgetPreferencesFlow: Flow<WidgetPreferences> = dataStoreManager.widgetPreferencesFlow

    suspend fun saveWidgetType(appWidgetId: Int, type: Int) {
        dataStoreManager.saveWidgetType(appWidgetId, type)
    }

    suspend fun deleteWidgetType(appWidgetId: Int) {
        dataStoreManager.deleteWidgetType(appWidgetId)
    }
}