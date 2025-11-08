// app/src/main/java/com/em/batterywidget/BatteryLog.kt
package com.em.batterywidget

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade do Room que representa um único registro de log da bateria no banco de dados.
 * Esta é a nossa
única fonte de verdade para os dados da bateria.
 */
@Entity(tableName = "battery_log")
data class BatteryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val level
    : Int, // Nível percentual (0-100)
    val status: Int, // Constantes do BatteryManager (e.g., BATTERY_STATUS_CHARGING)
    val health: Int, // Constantes do BatteryManager
    val plugged: Int, // Constantes do BatteryManager
    val temperature
    : Int, // Em décimos de grau Celsius
    val voltage: Int, // Em milivolts
    val technology: String
)
