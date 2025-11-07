package com.em.batterywidget

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Repositório unificado que abstrai o acesso a dados da bateria (Room).
 * Este repositório combina a lógica de monitoramento de histórico e limpeza
 * com o acesso a logs em tempo real, usando a interface BatteryLogDao.
 *
 * @param batteryDao O Data Access Object unificado (BatteryLogDao) para operações com o banco de dados.
 */
class BatteryRepository(private val batteryDao: BatteryLogDao) {

    private val TAG = "BatteryRepository"

    companion object {
        // Define por quanto tempo os dados históricos devem ser mantidos (7 dias)
        val DATA_RETENTION_DAYS = 7L
    }

    /**
     * Insere um novo registro de bateria no histórico, usando a entidade BatteryLog completa.
     *
     * @param log O registro de dados da bateria a ser salvo.
     */
    suspend fun recordBatteryLog(log: BatteryLog) {
        try {
            // Chamada ao DAO
            batteryDao.insertLog(log)
            // Usamos 'log.timestamp' que é o nome do campo na Entidade
            Log.d(TAG, "Registro de bateria salvo: Nível=${log.level}%, Saúde=${log.health}, Tempo=${log.timestamp}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir registro de bateria", e)
        }
    }

    /**
     * Obtém o registro mais recente da bateria em tempo real (para o widget principal).
     */
    fun getLatestBatteryLog(): Flow<BatteryLog?> {
        // Chamada ao novo método do DAO
        return batteryDao.getLatestLog()
    }

    /**
     * Obtém o histórico completo dos registros de bateria para visualização completa.
     * Retorna um Flow de uma lista de BatteryLog.
     */
    fun getFullHistory(): Flow<List<BatteryLog>> {
        // Chamada ao novo método do DAO
        return batteryDao.getAllLogsHistory()
    }

    /**
     * Obtém o histórico dos últimos N dias, ideal para gráficos.
     *
     * @param days O número de dias de histórico a ser recuperado.
     * @return Um Flow de uma lista de BatteryLog.
     */
    fun getHistoryForLastDays(days: Long): Flow<List<BatteryLog>> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(days)

        Log.d(TAG, "Buscando histórico entre $startTime e $endTime ($days dias)")

        // Usa o DAO para filtrar registros dentro do intervalo de N dias.
        return batteryDao.getLogsBetween(startTime, endTime)
    }

    /**
     * Obtém registros desde um determinado timestamp (usado para inicialização de gráficos).
     *
     * @param sinceMillis O timestamp (em milissegundos) a partir do qual buscar.
     */
    fun getBatteryLogsSince(sinceMillis: Long): Flow<List<BatteryLog>> {
        // Chamada ao novo método do DAO
        return batteryDao.getLogsSince(sinceMillis)
    }

    /**
     * Tarefa de manutenção para limpar registros de bateria mais antigos que o período de retenção (7 dias).
     * Deve ser chamada periodicamente (por exemplo, em um Worker).
     */
    fun cleanupOldData() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DATA_RETENTION_DAYS)

        // Lançamento em CoroutineScope em Dispatchers.IO para não bloquear a thread principal.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Chamada ao novo método do DAO que retorna o número de entradas excluídas
                val deletedCount = batteryDao.cleanOldEntries(cutoffTime)
                Log.i(TAG, "Limpeza de dados concluída. Registros excluídos: $deletedCount")
            } catch (e: Exception) {
                Log.e(TAG, "Erro durante a limpeza de dados antigos", e)
            }
        }
    }
}