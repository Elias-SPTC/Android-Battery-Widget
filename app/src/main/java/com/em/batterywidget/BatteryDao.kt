package com.em.batterywidget

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para a entidade BatteryLog.
 * Corrige a redeclaração de BatteryLogDao e os métodos de consulta ausentes.
 */
@Dao
interface BatteryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBatteryLog(log: BatteryLog)

    // Referenciado em BatteryRepository.kt
    @Query("SELECT * FROM battery_logs ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestLog(): Flow<BatteryLog?>

    // Referenciado em BatteryRepository.kt
    @Query("SELECT * FROM battery_logs ORDER BY timestampMillis DESC")
    fun getAllLogsHistory(): Flow<List<BatteryLog>>

    // Referenciado em BatteryRepository.kt
    @Query("SELECT * FROM battery_logs WHERE timestampMillis >= :startTime AND timestampMillis <= :endTime ORDER BY timestampMillis DESC")
    fun getLogsBetween(startTime: Long, endTime: Long): Flow<List<BatteryLog>>

    // Referenciado em BatteryRepository.kt
    @Query("SELECT * FROM battery_logs WHERE timestampMillis >= :startTime ORDER BY timestampMillis DESC")
    fun getLogsSince(startTime: Long): Flow<List<BatteryLog>>

    // Referenciado em BatteryViewModel.kt
    @Query("DELETE FROM battery_logs")
    suspend fun clearAllLogs()

    // Referenciado em BatteryRepository.kt para limpeza
    @Query("DELETE FROM battery_logs WHERE timestampMillis < :cutoffTime")
    suspend fun cleanOldEntries(cutoffTime: Long): Int
}