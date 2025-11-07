package com.em.batterywidget

/**
 * Classe de dados que representa o estado atual da bateria do dispositivo.
 *
 * Esta estrutura é usada por BatteryMonitor para coletar dados brutos do Intent
 * e por WidgetUpdater para renderizar o estado nos widgets.
 */
data class BatteryInfo(
    // Nível atual da bateria (0-100)
    val level: Int,
    // Estado do carregamento: true se o dispositivo estiver carregando (AC, USB, wireless)
    val isCharging: Boolean,
    // Status detalhado da bateria (Carregando, Descarga, Completo, etc.)
    val status: Status,
    // Fonte de energia (AC, USB, Wireless, Nenhuma)
    val chargeSource: ChargeSource = ChargeSource.NONE,
    // Temperatura da bateria em décimos de grau Celsius (e.g., 300 = 30.0°C)
    val temperature: Int = 0,
    // Voltagem da bateria em milivolts
    val voltage: Int = 0,
    // Saúde da bateria (Bom, Ruim, etc.)
    val health: Health = Health.UNKNOWN
) {

    /**
     * Enumeração dos possíveis estados de saúde da bateria.
     */
    enum class Health {
        UNKNOWN,
        GOOD,
        OVERHEAT,
        DEAD,
        OVER_VOLTAGE,
        UNSPECIFIED_FAILURE,
        COLD
    }

    /**
     * Enumeração dos possíveis status operacionais da bateria.
     */
    enum class Status {
        UNKNOWN,
        CHARGING,
        DISCHARGING,
        NOT_CHARGING,
        FULL
    }

    /**
     * Enumeração das possíveis fontes de carregamento.
     */
    enum class ChargeSource {
        NONE,
        AC,
        USB,
        WIRELESS,
        OTHER
    }

    /**
     * Retorna a temperatura formatada em graus Celsius.
     */
    fun getTemperatureCelsius(): String {
        // A temperatura é fornecida em décimos de grau Celsius
        return String.format("%.1f°C", temperature / 10f)
    }
}