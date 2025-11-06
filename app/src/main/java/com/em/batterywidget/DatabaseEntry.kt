package com.em.batterywidget

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representa uma única entrada de dados de bateria armazenada na base de dados (SQLite).
 *
 * Utiliza uma data class do Kotlin para concisão, fornecendo automaticamente:
 * - Getters para todas as propriedades (val).
 * - Métodos equals(), hashCode() e toString() baseados nas propriedades.
 */
data class DatabaseEntry(
    val timestamp: Long = System.currentTimeMillis(), // O tempo em milissegundos
    val level: Int = 0,               // Nível da bateria (0-100)
    val status: Int = 0,              // Código de status (ex: BATTERY_STATUS_CHARGING)
    val plugged: Int = 0,             // Código de plug (ex: BATTERY_PLUGGED_USB)
    val voltage: Int = 0,             // Tensão em mV
    val health: Int = 1               // Código de saúde (ex: BATTERY_HEALTH_UNKNOWN)
) {
    // --- Construtores Auxiliares ---

    /**
     * Construtor auxiliar para inserção de dados de demonstração ou casos onde só o nível é importante.
     * Assume defaults: status=0, plugged=0, voltage=0, health=1 (BATTERY_HEALTH_UNKNOWN).
     */
    constructor(timestamp: Long, level: Int) : this(timestamp, level, 0, 0, 0, 1)

    // --- Métodos de Conveniência (Properties calculadas) ---

    fun getTime(): Long {
        return timestamp // Alias para compatibilidade, mas 'timestamp' deve ser usado
    }

    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isCharging(): Boolean {
        // Plugged != 0 significa que está conectado a alguma fonte de energia (AC, USB, Wireless)
        return plugged != 0
    }

    // O método toString() é fornecido automaticamente pela data class.
    // Opcionalmente, pode-se sobrescrever para incluir a formatação extra:
    override fun toString(): String {
        return "DatabaseEntry [Time=${getFormattedTime()}, Level=$level%, Tensão=${voltage}mV, Saúde=$health]"
    }

    // --- Constantes (Companion Object) ---

    companion object {
        // Colunas da base de dados (são constantes, idealmente em um Companion Object)
        const val KEY_TIME = "time_stamp"
        const val KEY_LEVEL = "battery_level"
        const val KEY_STATUS = "status"
        const val KEY_PLUGGED = "plugged"
        const val KEY_VOLTAGE = "voltage"
        const val KEY_HEALTH = "health"
    }
}