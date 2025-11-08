package com.em.batterywidget

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Interface de Acesso a Dados (DAO) para a entidade BatteryLog.
 * Define todas as operações de banco de dados para os logs de bateria.
 */
@Dao
interface BatteryLogDao {

    /**
     * Insere um novo log de bateria. Se já existir um com o mesmo timestamp, ele será substituído.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBatteryLog(log: BatteryLog)

    /**
     * Retorna o log de bateria mais recente, observável através de um Flow.
     */
    @Query("SELECT * FROM battery_log ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestLog(): Flow<BatteryLog?>

    /**
     * Retorna os 100 logs mais recentes para o gráfico de histórico.
     */
    @Query("SELECT * FROM battery_log ORDER BY timestampMillis DESC LIMIT 100")
    fun getHistory(): Flow<List<BatteryLog>>

    /**
     * Retorna todos os logs de bateria, para a tela de histórico completo.
     */
    @Query("SELECT * FROM battery_log ORDER BY timestampMillis DESC")
    fun getAllLogs(): Flow<List<BatteryLog>>

    /**
     * Apaga todos os registros da tabela.
     */
    @Query("DELETE FROM battery_log")
    suspend fun clearAll()
}
