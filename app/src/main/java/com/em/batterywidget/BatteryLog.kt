package com.em.batterywidget

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for logging battery status over time.
 * Corrige redeclaração de BatteryLog.
 */
@Entity(tableName = "battery_logs")
data class BatteryLog(
    // timestamp em milissegundos é a chave primária
    @PrimaryKey
    val timestampMillis: Long,
    val level: Int,
    val status: String, // Nome do Enum
    val plugged: String, // Nome do Enum
    val health: String, // Nome do Enum
    val temperature: Int, // deciCelsius
    val voltage: Int, // millivolts
    val technology: String
) {
    // Helper para converter BatteryInfo para BatteryLog para inserção
    companion object {
        fun fromBatteryInfo(info: BatteryInfo): BatteryLog {
            return BatteryLog(
                timestampMillis = info.timestamp,
                level = info.level,
                status = info.status.name,
                plugged = info.plugged.name,
                health = info.health.name,
                temperature = info.temperature,
                voltage = info.voltage,
                technology = info.technology
            )
        }
    }
}