package com.em.batterywidget

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Entidade Room que representa um registro completo de estado da bateria.
 *
 * NOTA: Esta classe deve ser única. Se você tem BatteryLog.kt, remova seu conteúdo,
 * ou o renomeie para evitar o erro de 'Redeclaration'.
 */
@Entity(tableName = "battery_logs")
data class BatteryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val level: Int,
    val status: Int,
    val health: Int,
    val plugged: Int,
    val voltage: Int,
    val temperature: Int,
    // Novos campos que podem ter sido adicionados
    val technology: String? = null,
    val isCharging: Boolean = false
)

/**
 * Data Access Object (DAO) unificado para interagir com a tabela battery_logs.
 */
@Dao
interface BatteryLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatteryLog)

    @Query("SELECT * FROM battery_logs ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestLog(): Flow<BatteryLog?>

    @Query("SELECT * FROM battery_logs ORDER BY timestampMillis ASC")
    fun getAllLogsHistory(): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_logs WHERE timestampMillis BETWEEN :startTimeMillis AND :endTimeMillis ORDER BY timestampMillis ASC")
    fun getLogsBetween(startTimeMillis: Long, endTimeMillis: Long): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_logs WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis ASC")
    fun getLogsSince(sinceMillis: Long): Flow<List<BatteryLog>>

    @Query("DELETE FROM battery_logs WHERE timestampMillis < :cutoffTime")
    suspend fun cleanOldEntries(cutoffTime: Long): Int
}