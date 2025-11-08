package com.em.batterywidget.data

/**
 * Data class que representa as preferências de configuração do Widget.
 * Usada pelo DataStore.
 */
data class WidgetPreferences(
    val updateIntervalMinutes: Int = 15,
    val showGraph: Boolean = true,
    val backgroundColor: Int = 0xAA000000.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt()
)