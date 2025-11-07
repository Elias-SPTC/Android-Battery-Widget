package com.em.batterywidget

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representa uma única entrada de dados de bateria armazenada na base de dados (SQLite).
 *
 * NOTA DE CORREÇÃO: Adicionado o campo 'temperatureRaw' para consistência com as colunas
 * de BatteryDatabase (SQLite), onde a temperatura é armazenada como um valor bruto (Int em décimos de Celsius).
 */
data class DatabaseEntry(
    val timestamp: Long = System.currentTimeMillis(), // O tempo em milissegundos
    val level: Int = 0,               // Nível da bateria (0-100)
    val status: Int = 0,              // Código de status (ex: BATTERY_STATUS_CHARGING)
    val plugged: Int = 0,             // Código de plug (ex: BATTERY_PLUGGED_USB)
    val voltageMillivolts: Int = 0,   // Tensão em milivolts (mV)
    val temperatureRaw: Int = 0,      // Temperatura bruta em décimos de Celsius (Int)
    val health: Int = 1               // Código de saúde (ex: BATTERY_HEALTH_UNKNOWN)
) {
    // --- Construtores Auxiliares ---

    /**
     * Construtor auxiliar para inserção de dados de demonstração ou casos onde só o nível é importante.
     * Assume defaults para os demais: status=0, plugged=0, voltage=0, temperature=0, health=1 (BATTERY_HEALTH_UNKNOWN).
     */
    constructor(timestamp: Long, level: Int) : this(timestamp, level, 0, 0, 0, 0, 1)

    // --- Métodos de Conveniência (Properties calculadas) ---

    fun getTime(): Long {
        return timestamp // Alias para compatibilidade
    }

    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isCharging(): Boolean {
        // Plugged != 0 significa que está conectado a alguma fonte de energia (AC, USB, Wireless)
        return plugged != 0
    }

    /**
     * Retorna a temperatura formatada em Celsius (divide o valor bruto por 10).
     */
    fun getTemperatureFormatted(): String {
        val tempInC = if (temperatureRaw == 0) 0f else temperatureRaw / 10f
        return "${String.format("%.1f", tempInC)}°C"
    }

    /**
     * Retorna a voltagem formatada em Volts (divide o valor bruto por 1000).
     */
    fun getVoltageFormatted(): String {
        return "${String.format("%.2f", voltageMillivolts / 1000f)}V"
    }

    override fun toString(): String {
        return "DatabaseEntry [Time=${getFormattedTime()}, Level=$level%, Tensão=${voltageMillivolts}mV, Saúde=$health]"
    }

    // --- Constantes (Companion Object) ---

    companion object {
        // Colunas da base de dados (para uso no Cursor, embora o Database.kt já as defina)
        const val KEY_TIME = "time_stamp"
        const val KEY_LEVEL = "battery_level"
        const val KEY_STATUS = "status"
        const val KEY_PLUGGED = "plugged"
        const val KEY_VOLTAGE = "voltage" // Corresponde a voltageMillivolts
        const val KEY_TEMPERATURE = "temperature" // Corresponde a temperatureRaw
        const val KEY_HEALTH = "health"
    }
}